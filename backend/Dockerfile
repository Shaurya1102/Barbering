# Multi-stage Docker build for Barbering Backend (Spring Boot 3.5 + Java 21)
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Maven wrapper and POM first to take advantage of Docker layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source and package jar
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
