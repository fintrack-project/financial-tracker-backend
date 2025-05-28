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

# Stage 2: Development environment
FROM eclipse-temurin:21-jdk as dev

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw* ./
COPY pom.xml ./
COPY .mvn .mvn

# Copy source code and resources
COPY src src

# Create application.properties from template if it doesn't exist
RUN if [ ! -f src/main/resources/application.properties ]; then \
    cp src/main/resources/application.properties.template src/main/resources/application.properties; \
    fi

EXPOSE 8080

# The command will be overridden by docker-compose
CMD ["./mvnw", "spring-boot:run"]

# Stage 3: Production environment
FROM eclipse-temurin:21-jre as prod

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]