FROM node:22-bookworm-slim AS frontend
WORKDIR /build
COPY frontend/package*.json ./frontend/
RUN cd frontend && npm ci
COPY frontend ./frontend
COPY src/main/resources/static ./src/main/resources/static
RUN mkdir -p src/main/resources/static/js/vendor
RUN cd frontend && npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY --from=frontend /build/src/main/resources/static ./src/main/resources/static
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /build/target/HospitalApp-1.0.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar"]
