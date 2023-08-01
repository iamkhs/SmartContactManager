# Build stage
FROM gradle:8.1.1-jdk17 AS build
COPY . .
RUN ./gradlew clean build -x test

# Final stage
FROM openjdk:17.0.1-jdk-slim
COPY --from=build /home/gradle/build/libs/SmartContactManager-0.0.1-SNAPSHOT.jar /SmartContactManager.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "SmartContactManager.jar"]
