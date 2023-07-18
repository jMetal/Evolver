# Base image for Maven build
FROM maven:3.8.3-openjdk-17 AS builder

# Set the working directory inside the container
WORKDIR /evolver

# Copy the Maven project file(s) to the container
COPY pom.xml .

# Download project dependencies (cached by Docker layer)
RUN mvn dependency:go-offline -B

# Copy the source code to the container
COPY src/ ./src/

# Build the Maven project
RUN mvn package

# Barebones Java 17 image to run the JAR
FROM amazoncorretto:17-alpine

# Set the working directory inside the container
WORKDIR /evolver

# Copy the JAR file from the builder stage
COPY --from=builder /evolver/target/Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar .

# Copy the resources folder
COPY resources/ ./resources/

# Set the entrypoint script
COPY docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod +x ./docker-entrypoint.sh

# Specify the command to execute when the container starts
ENTRYPOINT ["./docker-entrypoint.sh"]