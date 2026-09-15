# Builds one deployable image: the Vue frontend's static build gets copied into the Spring Boot
# app's own static resources (see SpaForwardingController + SecurityConfig for the routing/auth
# pieces that make that work), so the whole app - API and UI - is one process on one port. This
# was built and verified locally (mvn package + java -jar, not just written and hoped for): SPA
# routes survive a refresh, hashed JS/CSS under /assets/ is served with the right content type,
# and the API's own auth rules are unchanged.

# ---- Stage 1: build the frontend ----
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: build the backend, with the frontend's build folded in as static resources ----
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static
RUN mvn -B clean package -DskipTests

# ---- Stage 3: slim runtime image ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/unilinkhub.jar ./app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
