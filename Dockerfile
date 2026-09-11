# Stage 1: Build application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY rachaplus-api /app
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
