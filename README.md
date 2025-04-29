# FinTrack Backend

FinTrack is a Personal Financial Tracker designed to help individuals manage their financial assets effectively. This backend component is built using Spring Boot and provides RESTful APIs for asset management.

## Project Structure

- **src/main/java/com/fintrack/controller**: Contains controllers for handling HTTP requests related to asset management.
- **src/main/java/com/fintrack/model**: Contains model classes representing the data structure for assets, users, and transactions.
- **src/main/java/com/fintrack/repository**: Contains repository interfaces for database interactions using Spring Data JPA.
- **src/main/java/com/fintrack/service**: Contains service classes that implement the business logic for asset management.
- **src/main/java/com/fintrack/component/chart**: Contains chart-related components for generating visual representations of financial data.
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
   ```bash
   git clone <repository-url>
   cd FinTrack/backend
   ```

2. Update the `application.properties` file with your PostgreSQL database connection settings:
   ```properties
   spring.datasource.url=jdbc:postgresql://<host>:<port>/<database>
   spring.datasource.username=<username>
   spring.datasource.password=<password>
   ```

3. Build the project using Maven:
   ```bash
   mvn clean install
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Docker

To build and run the backend application in a Docker container:

1. Build the Docker image:
   ```bash
   docker build -t fintrack-backend .
   ```

2. Run the Docker container:
   ```bash
   docker run -p 8080:8080 --name fintrack-backend fintrack-backend
   ```

### API Documentation

The API documentation is available via Swagger. Once the application is running, navigate to:
```
http://localhost:8080/swagger-ui.html
```

This documentation provides details on available endpoints, request/response formats, and usage examples.

## Testing

To run the tests, use the following Maven command:
```bash
mvn test
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add your message here"
   ```
4. Push to your forked repository:
   ```bash
   git push origin feature/your-feature-name
   ```
5. Submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

## Contact

For any questions or support, please contact the project maintainers at:
- **Email**: support@fintrack.com
- **GitHub Issues**: [Open an issue](https://github.com/<repository-url>/issues)