# =========================
# 1. Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy core module and build it
COPY nnfs-java-core ./nnfs-java-core
RUN mvn -f nnfs-java-core/pom.xml clean install -DskipTests

# Copy API module and build it (resolves nnfs-java-core from local repo)
COPY api-java-spring ./api-java-spring
RUN mvn -f api-java-spring/pom.xml clean package -DskipTests


# =========================
# 2. Runtime stage
# =========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the Spring Boot JAR
COPY --from=build /app/api-java-spring/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
