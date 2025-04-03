FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /app

# Copy the Maven build output
COPY target/fintrack-backend.jar fintrack-backend.jar

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "fintrack-backend.jar"]