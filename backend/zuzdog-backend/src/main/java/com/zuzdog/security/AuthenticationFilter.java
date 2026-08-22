package com.zuzdog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * the one gate in front of every /api/** endpoint.
 *
 * we extend OncePerRequestFilter from spring-web, which runs once per request.
 * spring boot picks up any Filter that is a @Component and puts it in the
 * chain by itself, so we do not need spring-boot-starter-security here. this
 * is the custom security the project plan asks for.
 *
 * the base class also makes sure doFilterInternal is not called twice for the
 * same request, we do not handle that ourselves.
 */

// @Component is what makes spring find this class and register it as a filter
@org.springframework.stereotype.Component
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ID_ATTR = "authenticatedUserId";

    private final SessionService sessionService;

    // Constructor injection is the recommended way to get Spring beans into your
    // own beans. Spring will find the SessionService bean and pass it in here.
    public AuthenticationFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * paths that do not need a token.
     *   /auth/**     - register and login, you have no token yet
     *   /health      - basic check
     *   /error       - spring sends failed requests here, guarding it gives a
     *                  confusing second 401
     *   /favicon.ico - the browser always asks for it, a 401 is just noise
     */


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/auth/")
                || path.startsWith("/uploads/")
                || path.equals("/auth")
                || path.equals("/api/auth")
                || path.equals("/health")
                || path.equals("/error")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // read the Authorization header. the header name is not case sensitive
        // but "Bearer" is, so we check it with startsWith
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            // no header or wrong type, so we stop here and the controller
            // never runs
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return;
        }

        // Strip the "Bearer " prefix (7 chars) and ignore surrounding whitespace.
        String token = header.substring(7).trim();

        // Ask the session store who this token belongs to. resolveUserId
        // returns empty if the token is unknown OR expired (it also removes
        // expired entries lazily).
        Optional<Long> userId = sessionService.resolveUserId(token);

        if (userId.isEmpty()) {
            // Token presented but invalid/expired - 401, do not continue.
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired session token");
            return;
        }

        // Success: publish the authenticated user id on the request so
        // controllers downstream can read it without re-parsing the header.
        request.setAttribute(AUTHENTICATED_USER_ID_ATTR, userId.get());

        // Hand control to the next filter / the DispatcherServlet.
        filterChain.doFilter(request, response);
    }
}