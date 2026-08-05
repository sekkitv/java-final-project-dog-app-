package com.zuzdog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * AuthenticationFilter is the single gate that protects every /api/** endpoint.
 *
 * What it is, framework-wise:
 *   We extend OncePerRequestFilter (from spring-web), which is a Servlet Filter
 *   guaranteed to run exactly once per request even with async dispatches and
 *   forwards. Spring Boot auto-registers any Filter that is a @Component bean
 *   into the embedded Tomcat filter chain  so we don't need to declare a
 *   FilterRegistrationBean, define a SecurityFilterChain, or pull in
 *   spring-boot-starter-security. This is the "custom security" approach the
 *   project plan mandates.
 *
 * Why request attribute and not Spring's SecurityContextHolder:

 * Note on once-per-request:
 *   OncePerRequestFilter stores a request attribute to make sure doFilterInternal
 *   isn't called twice for the same request. We don't manage that ourselves —
 *   the base class does.
 */

// @Component makes Spring detect it during component scan and register it as a
// servlet Filter. That registration is what puts it in the request chain.
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
     * Paths that must NOT require a token.
     *   /auth/**  — register and login obviously can't require a token yet.
     *   /health   — basic  check 
     *   /error    — Spring routes failed requests here; protecting it produces
     *               confusing double-401 responses, so we skip it.
     *   /favicon.ico — browsers ask for this unconditionally; a 401 noise.
     */


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/auth/")
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

        // Pull the Authorization header. Per RFC 7235 it's case-insensitive in
        // name, case-sensitive in the scheme token ("Bearer"), so we read with
        // getHeader (transparent) and compare "Bearer" with startsWith on a
        // canonical prefix. We tolerate any whitespace after "Bearer".
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            // No header or wrong scheme → not authenticated. We STOP here so
            // the protected controller never executes.
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