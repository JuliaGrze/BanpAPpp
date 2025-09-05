# syntax=docker/dockerfile:1.7

# --- build ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# twardy cache npm (szybciej, mniej dziwnych stanów)
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
ENV NODE_OPTIONS=--max-old-space-size=4096
RUN --mount=type=cache,target=/root/.npm npm run build -- --configuration production

# --- run ---
FROM nginx:stable-alpine
# SPA fallback
RUN printf 'server {\n\
  listen 80;\n\
  server_name _;\n\
  root /usr/share/nginx/html;\n\
  index index.html;\n\
  location / { try_files $uri $uri/ /index.html; }\n\
}\n' > /etc/nginx/conf.d/default.conf
RUN rm -rf /usr/share/nginx/html/*
# U Ciebie outputPath = dist/client (bez "browser")
COPY --from=build /app/dist/client/browser /usr/share/nginx/html/
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
