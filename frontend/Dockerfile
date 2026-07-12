# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime stage ----
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Overridable at `docker run -e` / docker-compose / ECS task definition time.
# Defaults to the docker-compose service name for local orchestration.
ENV BACKEND_HOST=backend:8080

EXPOSE 80