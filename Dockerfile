# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/testmanagement-0.0.1-SNAPSHOT.jar ./app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-Xmx384m", "-jar", "app.jar", "--spring.profiles.active=prod"]