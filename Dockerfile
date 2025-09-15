# =========================
# 1. Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy full repo (root pom + modules)
COPY . .

# Build core + spring modules, skip tests
RUN mvn clean install -DskipTests -pl api-java-spring,nnfs-java-core -am


# =========================
# 2. Runtime stage
# =========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the Spring Boot JAR
COPY --from=build /app/api-java-spring/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
