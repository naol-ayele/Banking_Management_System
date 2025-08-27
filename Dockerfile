# Use official OpenJDK 21 base image
FROM openjdk:21-jdk-slim

WORKDIR /Banking_app

# Copy the built jar
COPY ./target/*.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
