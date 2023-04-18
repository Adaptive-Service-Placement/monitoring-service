FROM maven:3.8.4-openjdk-17 as maven-builder
COPY src /app/src
COPY pom.xml /app

ENV MIGRATION_INTERVAL=24

RUN mvn -f /app/pom.xml clean package -DskipTests
FROM openjdk:17-alpine

COPY --from=maven-builder app/target/monitoring-service.jar /app-service/monitoring-service
WORKDIR /app-service

EXPOSE 8080
ENTRYPOINT ["java","-jar","monitoring-service"]