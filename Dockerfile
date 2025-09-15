# =========================
# 1. Build stage (with Maven)
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom and fetch dependencies (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Package application (skip tests to speed up CI)
RUN mvn clean package -DskipTests


# =========================
# 2. Runtime stage (JDK only, smaller image)
# =========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Start app
ENTRYPOINT ["java","-jar","app.jar"]
