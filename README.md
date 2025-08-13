# FinTrack Backend 🚀

> **A production-ready Spring Boot microservice that delivers enterprise-grade RESTful APIs for personal financial portfolio management with security, performance, and scalability.**

The **FinTrack Backend** is a **high-performance Java microservice** built with Spring Boot 3.2 and Java 21, demonstrating modern backend development practices, enterprise architecture patterns, and production-ready features. It provides a robust foundation for financial data management with comprehensive security, monitoring, and scalability capabilities.

---

## 🎯 **What This Backend Showcases**

- **Modern Java Development**: Java 21 with latest language features and Spring Boot 3.2
- **Enterprise Architecture**: Microservices design with clean separation of concerns
- **Security Excellence**: JWT authentication, role-based access control, and input validation
- **Performance Optimization**: Connection pooling, caching strategies, and query optimization
- **API Design**: RESTful APIs with OpenAPI 3.0 specification and comprehensive documentation
- **Database Design**: PostgreSQL with Flyway migrations and optimized schemas
- **Testing Strategy**: Comprehensive unit and integration testing with TestContainers
- **DevOps Ready**: Docker containerization, health checks, and monitoring endpoints

---

## 🏗️ **Technical Architecture**

### **Layered Architecture Pattern**
```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   REST API      │  │   WebSocket     │  │   Security  │ │
│  │   Controllers   │  │   Handlers      │  │   Filters   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                     Business Layer                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Services      │  │   Validators    │  │   Business  │ │
│  │   (Core Logic)  │  │   & Rules      │  │   Rules     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Data Access Layer                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Repositories  │  │   Data Models   │  │   Database  │ │
│  │   (JPA/Hibernate)│  │   (Entities)    │  │   Migrations│ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### **Package Structure**
```
src/main/java/com/fintrack/
├── controller/                    # REST API endpoints
│   ├── auth/                     # Authentication controllers
│   ├── finance/                  # Financial data controllers
│   ├── user/                     # User management controllers
│   └── market/                   # Market data controllers
├── service/                      # Business logic layer
│   ├── auth/                     # Authentication services
│   ├── finance/                  # Financial calculation services
│   ├── user/                     # User management services
│   └── market/                   # Market data services
├── repository/                   # Data access layer
│   ├── auth/                     # Authentication repositories
│   ├── finance/                  # Financial data repositories
│   └── user/                     # User data repositories
├── model/                        # Data models and DTOs
│   ├── entity/                   # JPA entities
│   ├── dto/                      # Data transfer objects
│   └── enums/                    # Enumerations
├── config/                       # Configuration classes
│   ├── security/                 # Security configuration
│   ├── database/                 # Database configuration
│   └── web/                      # Web configuration
├── component/                    # Reusable components
│   ├── chart/                    # Chart generation components
│   ├── email/                    # Email service components
│   └── notification/             # Notification components
├── exception/                    # Custom exception classes
├── util/                         # Utility classes
└── security/                     # Security utilities
```

---

## 🚀 **Core Features & Capabilities**

### **🔐 Authentication & Security**
- **JWT Token Management**: Secure stateless authentication with refresh tokens
- **Firebase Integration**: Third-party authentication provider support
- **Role-Based Access Control**: Granular permissions and authorization
- **Password Security**: BCrypt hashing with salt and complexity validation
- **Input Validation**: Comprehensive request validation and sanitization
- **CORS Configuration**: Secure cross-origin resource sharing
- **Rate Limiting**: API rate limiting to prevent abuse

### **📊 Financial Portfolio Management**
- **Asset Tracking**: Comprehensive portfolio management with real-time updates
- **Category Management**: Hierarchical asset categorization and subcategorization
- **Performance Analytics**: ROI calculations, portfolio allocation, and trend analysis
- **Data Validation**: Financial data integrity and consistency checks
- **Audit Logging**: Complete audit trail for all financial transactions
- **Multi-Currency Support**: Base currency conversion and exchange rate management

### **🌐 API Design & Documentation**
- **RESTful APIs**: Standard HTTP methods with proper status codes
- **OpenAPI 3.0**: Comprehensive API documentation with Swagger UI
- **Request/Response Validation**: Input validation and error handling
- **API Versioning**: Backward-compatible API evolution
- **Rate Limiting**: Configurable API usage limits
- **Health Checks**: Application health monitoring endpoints

### **📈 Performance & Scalability**
- **Connection Pooling**: HikariCP for optimal database connections
- **Caching Strategy**: Redis integration for data caching
- **Query Optimization**: Database query tuning and indexing
- **Async Processing**: Non-blocking operations for better performance
- **Load Balancing**: Horizontal scaling support
- **Monitoring**: Actuator endpoints for application metrics

---

## 🛠️ **Technology Stack**

### **Core Framework**
| **Technology** | **Version** | **Purpose** |
|----------------|-------------|-------------|
| **Java** | 21 | Latest LTS with modern language features |
| **Spring Boot** | 3.2.12 | Application framework and auto-configuration |
| **Spring Framework** | 6.1.20 | Core Spring functionality |
| **Spring Data JPA** | 3.2.12 | Data access and persistence |
| **Spring Security** | 3.2.12 | Security and authentication |

### **Database & Persistence**
| **Technology** | **Version** | **Purpose** |
|----------------|-------------|-------------|
| **PostgreSQL** | 17.5 | Primary relational database |
| **Hibernate** | 6.2.18.Final | JPA implementation and ORM |
| **Flyway** | 9.22.3 | Database migration management |
| **HikariCP** | Built-in | High-performance connection pooling |

### **Security & Authentication**
| **Technology** | **Version** | **Purpose** |
|----------------|-------------|-------------|
| **JWT** | 0.12.6 | JSON Web Token implementation |
| **Firebase Admin** | 9.5.0 | Third-party authentication |
| **BCrypt** | Built-in | Password hashing and validation |
| **Spring Security** | 3.2.12 | Security framework integration |

### **Messaging & Integration**
| **Technology** | **Version** | **Purpose** |
|----------------|-------------|-------------|
| **Apache Kafka** | 4.0.0 | Message queuing and streaming |
| **Spring Kafka** | 4.0.0-M3 | Kafka integration |
| **WebSocket** | Planned | Real-time communication (in development) |
| **STOMP** | Planned | WebSocket messaging protocol (in development) |

### **Testing & Quality**
| **Technology** | **Version** | **Purpose** |
|----------------|-------------|-------------|
| **JUnit 5** | 6.0.0-M2 | Unit testing framework |
| **TestContainers** | 1.21.3 | Integration testing with containers |
| **Mockito** | Built-in | Mocking framework |
| **Spring Boot Test** | 3.2.12 | Integration testing support |

---

## 🔧 **Development Setup**

### **Prerequisites**
- **Java**: Version 21.0.0 or higher (OpenJDK or Oracle JDK)
- **Maven**: Version 3.8.0 or higher
- **PostgreSQL**: Version 17.0 or higher
- **Docker**: Version 20.10 or higher (for containerized development)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### **Quick Start** ⚡

```bash
# Clone the repository
git clone https://github.com/fintrack-project/financial-tracker-backend.git
cd financial-tracker-backend

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Access the application
# 🌐 Backend API: http://localhost:8080
# 📊 Swagger UI: http://localhost:8080/swagger-ui.html
# 🔍 Actuator: http://localhost:8080/actuator
```

### **Environment Configuration**
Create `application-local.properties` for local development:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/financial_tracker
spring.datasource.username=admin
spring.datasource.password=secure_password_123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Security Configuration
jwt.secret=your-secret-key-here
jwt.expiration=86400000
firebase.project-id=your-firebase-project-id

# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Logging Configuration
logging.level.com.fintrack=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## 🐳 **Docker Development**

### **Build Docker Image**
```bash
# Build the application
./mvnw clean package -DskipTests

# Build Docker image
docker build -t fintrack-backend:latest .

# Run with Docker Compose (recommended)
cd ../financial-tracker-infra
docker-compose -f docker-compose.yml -f docker-compose.local.yml up -d
```

### **Individual Container**
```bash
# Run backend container
docker run -d \
  --name fintrack-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/financial_tracker \
  fintrack-backend:latest
```

---

## 📚 **API Documentation**

### **Swagger UI Access**
Once the application is running, access the interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### **API Endpoints Overview**

#### **Authentication Endpoints**
```
POST   /api/auth/login              # User login
POST   /api/auth/register           # User registration
POST   /api/auth/refresh            # Refresh JWT token
POST   /api/auth/logout             # User logout
```

#### **User Management Endpoints**
```
GET    /api/user/profile            # Get user profile
PUT    /api/user/profile            # Update user profile
POST   /api/user/password-reset     # Request password reset
PUT    /api/user/password-reset     # Reset password
```

#### **Financial Data Endpoints**
```
GET    /api/finance/holdings        # Get user holdings
POST   /api/finance/holdings        # Add new holding
PUT    /api/finance/holdings/{id}   # Update holding
DELETE /api/finance/holdings/{id}   # Delete holding
GET    /api/finance/portfolio       # Get portfolio summary
GET    /api/finance/analytics       # Get portfolio analytics
```

#### **Category Management Endpoints**
```
GET    /api/categories              # Get all categories
POST   /api/categories              # Create new category
PUT    /api/categories/{id}         # Update category
DELETE /api/categories/{id}         # Delete category
```

### **Request/Response Examples**

#### **Create Holding Request**
```json
{
  "assetName": "Apple Inc.",
  "symbol": "AAPL",
  "quantity": 100,
  "assetType": "STOCK",
  "categoryId": 1,
  "subcategoryId": 2,
  "purchasePrice": 150.00,
  "purchaseDate": "2024-01-15"
}
```

#### **Portfolio Response**
```json
{
  "totalValue": 25000.00,
  "baseCurrency": "USD",
  "holdings": [
    {
      "id": 1,
      "assetName": "Apple Inc.",
      "symbol": "AAPL",
      "currentValue": 15000.00,
      "percentage": 60.0
    }
  ],
  "categories": [
    {
      "name": "Technology",
      "value": 15000.00,
      "percentage": 60.0
    }
  ]
}
```

---

## 🧪 **Testing Strategy**

### **Testing Pyramid**
```
    🔺 E2E Tests (Few)
   🔺🔺 Integration Tests
  🔺🔺🔺 Unit Tests (Many)
```

### **Unit Testing**
```java
@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {
    
    @Mock
    private HoldingRepository holdingRepository;
    
    @InjectMocks
    private HoldingService holdingService;
    
    @Test
    void shouldCreateHolding() {
        // Given
        CreateHoldingRequest request = new CreateHoldingRequest();
        request.setAssetName("Apple Inc.");
        request.setSymbol("AAPL");
        
        Holding expectedHolding = new Holding();
        expectedHolding.setId(1L);
        expectedHolding.setAssetName("Apple Inc.");
        
        when(holdingRepository.save(any(Holding.class)))
            .thenReturn(expectedHolding);
        
        // When
        Holding result = holdingService.createHolding(request);
        
        // Then
        assertThat(result.getAssetName()).isEqualTo("Apple Inc.");
        verify(holdingRepository).save(any(Holding.class));
    }
}
```

### **Integration Testing**
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HoldingControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldReturnHoldings() {
        // When
        ResponseEntity<Holding[]> response = restTemplate
            .getForEntity("/api/finance/holdings", Holding[].class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
```

### **Test Coverage Targets**
- **Unit Tests**: 90%+ coverage
- **Integration Tests**: 80%+ coverage
- **API Tests**: All endpoints covered
- **Security Tests**: Authentication and authorization verified

---

## 🔐 **Security Implementation**

### **Authentication Flow**
```
1. User Login → Validate Credentials → Generate JWT Token
2. API Request → Extract JWT Token → Validate Token → Check Permissions
3. Token Refresh → Validate Refresh Token → Generate New JWT Token
```

### **Security Configuration**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, 
                           UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### **JWT Token Management**
```java
@Component
public class JwtTokenProvider {
    
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

---

## 📊 **Performance & Monitoring**

### **Health Check Endpoints**
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Disk space
curl http://localhost:8080/actuator/health/diskSpace

# Custom health indicators
curl http://localhost:8080/actuator/health/custom
```

### **Metrics & Monitoring**
```bash
# Application metrics
curl http://localhost:8080/actuator/metrics

# HTTP request metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### **Performance Optimization**
- **Connection Pooling**: HikariCP with optimized settings
- **Query Optimization**: Database indexing and query tuning
- **Caching Strategy**: Redis integration for frequently accessed data
- **Async Processing**: Non-blocking operations for I/O intensive tasks
- **Load Balancing**: Horizontal scaling support

---

## 🚀 **Deployment & Production**

### **Production Configuration**
```properties
# Production profile
spring.profiles.active=production

# Database (use environment variables)
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# Security
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}

# Logging
logging.level.com.fintrack=INFO
logging.level.org.springframework.security=WARN
```

### **Docker Production Build**
```dockerfile
# Multi-stage build for optimization
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM openjdk:21-jre-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### **Environment Variables**
```bash
# Required environment variables
export DATABASE_URL="jdbc:postgresql://db:5432/financial_tracker"
export DATABASE_USERNAME="admin"
export DATABASE_PASSWORD="secure_password_123"
export JWT_SECRET="your-super-secret-jwt-key"
export JWT_EXPIRATION="86400000"
export FIREBASE_PROJECT_ID="your-firebase-project-id"
```

---

## 🔮 **Future Enhancements**

### **Planned Features**
- **GraphQL API**: Advanced querying and data fetching
- **Real-time Updates**: Kafka integration for live data (WebSocket planned)
- **Advanced Analytics**: Machine learning-powered insights
- **Microservices**: Service decomposition for scalability
- **Event Sourcing**: Event-driven architecture for audit trails

### **Technical Improvements**
- **Docker Compose**: Container orchestration and scaling
- **Service Mesh**: Advanced networking with Istio
- **Observability**: Distributed tracing and monitoring
- **API Gateway**: Centralized API management
- **Message Queuing**: Advanced event processing

---

## 🤝 **Contributing to Backend**

### **Development Guidelines**
- **Code Style**: Follow Google Java Style Guide
- **Testing**: Comprehensive test coverage for all new features
- **Documentation**: Javadoc comments and API documentation
- **Security**: Security-first approach for all implementations
- **Performance**: Consider performance implications of all changes

### **Pull Request Process**
1. **Fork** the repository
2. **Create** a feature branch
3. **Implement** your changes with tests
4. **Update** documentation and README
5. **Submit** a pull request
6. **Code Review** and iteration

---

## 📚 **Additional Resources**

### **Documentation**
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/site/docs/current/reference/html5/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### **Learning Resources**
- **Spring Framework**: Modern Java development practices
- **JPA/Hibernate**: Database persistence and ORM
- **Security Best Practices**: Authentication and authorization
- **Performance Tuning**: Application and database optimization

---

## 📞 **Support & Community**

- **GitHub Issues**: Bug reports and feature requests
- **Discussions**: Community forum for questions
- **Documentation**: Comprehensive guides and examples
- **Contributing**: Guidelines for contributors

---

## 🏆 **Why This Backend Stands Out**

This backend demonstrates **enterprise-grade Java development** with:

- **Latest Technologies**: Java 21, Spring Boot 3.2, modern frameworks
- **Security Focus**: Comprehensive authentication and authorization
- **Performance**: Optimized database queries and caching strategies
- **Scalability**: Microservices architecture and horizontal scaling
- **Quality**: Comprehensive testing and code coverage
- **DevOps Ready**: Docker containerization and monitoring

**FinTrack Backend** represents a **production-ready financial service** that showcases the ability to build robust, secure, and scalable backend systems while following industry best practices and modern development standards.

---

*Built with ❤️ using Java 21, Spring Boot 3.2, and enterprise-grade technologies*