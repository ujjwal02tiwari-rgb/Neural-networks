# =========================
# 1. Build nnfs-java-core
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS core-build
WORKDIR /app

# Copy core module
COPY nnfs-java-core ./nnfs-java-core

# Build and install in local Maven repo
RUN mvn -f nnfs-java-core/pom.xml clean install -DskipTests


# =========================
# 2. Build api-java-spring
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS spring-build
WORKDIR /app

# Copy Spring Boot module + core module
COPY api-java-spring ./api-java-spring
COPY --from=core-build /root/.m2 /root/.m2

# Build Spring Boot app
RUN mvn -f api-java-spring/pom.xml clean package -DskipTests


# =========================
# 3. Runtime
# =========================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy Spring Boot jar
COPY --from=spring-build /app/api-java-spring/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
