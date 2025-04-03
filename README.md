# FinTrack Backend

FinTrack is a Personal Financial Tracker designed to help individuals manage their financial assets effectively. This backend component is built using Spring Boot and provides RESTful APIs for asset management.

## Project Structure

- **src/main/java/com/fintrack/controller**: Contains controllers for handling HTTP requests related to asset management.
- **src/main/java/com/fintrack/model**: Contains model classes representing the data structure for assets, users, and transactions.
- **src/main/java/com/fintrack/repository**: Contains repository interfaces for database interactions using Spring Data JPA.
- **src/main/java/com/fintrack/service**: Contains service classes that implement the business logic for asset management.
- **src/main/resources/application.properties**: Configuration properties for the Spring Boot application, including database connection settings.
- **src/main/resources/schema.sql**: Defines the initial database schema for PostgreSQL.

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven
- PostgreSQL
- Docker (for containerization)

### Installation

1. Clone the repository:
   ```
   git clone <repository-url>
   cd FinTrack/backend
   ```

2. Update the `application.properties` file with your PostgreSQL database connection settings.

3. Build the project using Maven:
   ```
   mvn clean install
   ```

4. Run the application:
   ```
   mvn spring-boot:run
   ```

### Docker

To build and run the backend application in a Docker container, use the following command:
```
docker build -t fintrack-backend .
```

### API Documentation

Refer to the API documentation for details on available endpoints and their usage.

## Contributing

Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.