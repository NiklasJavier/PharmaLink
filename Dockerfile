# Stage 1: Build the application with Gradle
FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/src

# Copy the entire project directory into the container.
COPY . .

# Make the Gradle wrapper executable and build the Spring Boot fat JAR without the daemon.
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

# Rename the generated JAR artifact for the runtime stage.
RUN mv build/libs/*.jar app.jar


# Stage 2: Create the final, lean production image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR artifact from the build stage.
COPY --from=build /home/gradle/src/app.jar .

# External Spring configuration location override.
ENV SPRING_CONFIG_LOCATION=optional:file:/etc/pharmalink/application.yaml

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]