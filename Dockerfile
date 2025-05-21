# Stage 1: Build the application
FROM eclipse-temurin:21-jdk as builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better cache
COPY mvnw* ./
COPY pom.xml ./
COPY .mvn .mvn

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src src

# Package the application
RUN ./mvnw package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]