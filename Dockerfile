# Build stage
FROM gradle:8.1.1-jdk17 AS build
COPY . .
RUN ./gradlew clean build -x test

# Final stage
FROM openjdk:17.0.1-jdk-slim
COPY --from=build /home/gradle/build/libs/SmartContactManager-0.0.1-SNAPSHOT.jar /SmartContactManager.jar
EXPOSE 8080

# Set environment variables for the Spring Boot application
ENV SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/smart_manager
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=priotY28@
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update
ENV SPRING_JPA_SHOW_SQL=true
ENV SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL=true
ENV SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.MySQL8Dialect

ENTRYPOINT ["java", "-jar", "SmartContactManager.jar"]
