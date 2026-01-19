# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /build

# Copy parent pom first
COPY pom.xml ./

# Copy all modules
COPY shared-models ./shared-models
COPY document-search-service ./document-search-service
COPY document-management-service ./document-management-service
COPY indexer-worker ./indexer-worker

# Build from the root to resolve parent POM and all dependencies
RUN mvn clean install -DskipTests -pl shared-models -am
RUN mvn clean package -DskipTests -pl document-search-service -am

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /build/document-search-service/target/*.jar app.jar

# Expose the application port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

