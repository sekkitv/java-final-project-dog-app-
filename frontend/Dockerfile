# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Build-time override for the backend origin (rarely needed since the app
# uses relative URLs; left here for future flexibility)
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- Runtime stage ----
FROM nginx:1.27-alpine
RUN rm -rf /usr/share/nginx/html/*
# Place as a template so the nginx entrypoint substitutes ${BACKEND_URL}
# at container start from the environment.
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

ENV BACKEND_URL=http://127.0.0.1:8080
ENV PORT=80

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
