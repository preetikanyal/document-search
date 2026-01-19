FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy parent pom and shared-models first (for dependency resolution)
COPY pom.xml .
COPY shared-models ./shared-models

# Copy api-gateway
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

# Build
RUN mvn -f api-gateway/pom.xml clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/api-gateway/target/*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
