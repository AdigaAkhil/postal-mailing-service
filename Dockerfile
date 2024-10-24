# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/postal-mailing-service.jar postal-mailing-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "postal-mailing-service.jar"]
