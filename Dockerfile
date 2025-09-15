
# =========================
# 1. Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy full repo (needed for multi-module build)
COPY . .

# Build all modules, skip tests
RUN mvn clean install -DskipTests


# =========================
# 2. Runtime stage (Spring Boot app only)
# =========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy Spring Boot JAR from api-java-spring
COPY --from=build /app/api-java-spring/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
