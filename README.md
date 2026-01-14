# Hotel Booking System

A microservices-based hotel booking platform built with Spring Boot 4.0.1 and Spring Cloud 2025.1.0.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client Applications                            │
│                         (Web/Mobile - localhost:3000)                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          API Gateway (Port 8080)                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌────────────────────────┐ │
│  │   Routing   │ │    CORS     │ │  Circuit    │ │   Load Balancing       │ │
│  │             │ │   Handling  │ │  Breaker    │ │   (Eureka-aware)       │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
           │                                                    │
           │  /api/auth/*                                       │  /api/hotels/*
           │  /api/bookings/*                                   │  /api/rooms/*
           ▼                                                    ▼
┌──────────────────────────────┐              ┌──────────────────────────────┐
│   Booking Service (8082)     │   OpenFeign  │    Hotel Service (8081)      │
│  ┌────────────────────────┐  │◄────────────►│  ┌────────────────────────┐  │
│  │  User Authentication   │  │              │  │   Hotel Management     │  │
│  │  JWT Token Generation  │  │              │  │   Room Management      │  │
│  │  Booking Management    │  │              │  │   Availability Check   │  │
│  └────────────────────────┘  │              │  └────────────────────────┘  │
│  ┌────────────────────────┐  │              │  ┌────────────────────────┐  │
│  │      H2 Database       │  │              │  │      H2 Database       │  │
│  │    (Users, Bookings)   │  │              │  │   (Hotels, Rooms)      │  │
│  └────────────────────────┘  │              │  └────────────────────────┘  │
└──────────────────────────────┘              └──────────────────────────────┘
           │                                                    │
           └────────────────────┬───────────────────────────────┘
                                │ Service Registration
                                ▼
                 ┌──────────────────────────────┐
                 │   Eureka Server (8761)       │
                 │  ┌────────────────────────┐  │
                 │  │  Service Discovery     │  │
                 │  │  Health Monitoring     │  │
                 │  └────────────────────────┘  │
                 └──────────────────────────────┘
```

### Service Communication Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         JWT Authentication Flow                            │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  1. User registers/logs in via Booking Service                             │
│  2. Booking Service generates JWT (signed with private.pem)                │
│  3. Client includes JWT in Authorization header                            │
│  4. Hotel Service validates JWT (using public.pem)                         │
│                                                                            │
│  ┌──────────┐      ┌─────────────────┐      ┌───────────────┐              │
│  │  Client  │─────►│ Booking Service │      │ Hotel Service │              │
│  └──────────┘      │   (private.pem) │      │  (public.pem) │              │
│       │            └─────────────────┘      └───────────────┘              │
│       │                    │                        ▲                      │
│       │                    │    Service Token       │                      │
│       │                    └────────────────────────┘                      │
│       │                                                                    │
│       └─────────────────► API Gateway ─────────────────►                   │
│                         (JWT passthrough)                                  │
└────────────────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
.
├── pom.xml                              # Parent POM (module aggregator)
├── README.md
├── CLAUDE.md
│
├── eureka-server/                       # Service Discovery (port 8761)
│   ├── pom.xml
│   └── src/main/
│       ├── java/mephi/eureka/
│       │   ├── EurekaServerApplication.java
│       │   └── config/EurekaSecurityConfig.java
│       └── resources/application.yml
│
├── api-gateway/                         # API Gateway (port 8080)
│   ├── pom.xml
│   └── src/main/
│       ├── java/mephi/gateway/
│       │   ├── ApiGatewayApplication.java
│       │   ├── controller/FallbackController.java
│       │   └── filter/
│       │       ├── LoggingFilter.java
│       │       └── TracingFilter.java
│       └── resources/application.yml
│
├── booking-service/                     # Booking & Auth Service (port 8082)
│   ├── pom.xml
│   └── src/
│       ├── main/java/mephi/bookingservice/
│       │   ├── client/                  # OpenFeign clients for Hotel Service
│       │   ├── config/                  # Security, Feign, OpenAPI configs
│       │   ├── controller/              # AuthController, BookingController
│       │   ├── dto/                     # Request/Response DTOs
│       │   ├── entity/                  # User, Booking, Role, BookingStatus
│       │   ├── exception/               # Custom exceptions & global handler
│       │   ├── mapper/                  # MapStruct mappers
│       │   ├── repository/              # Spring Data JPA repositories
│       │   └── service/                 # BookingService, UserService, JwtService
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── data.sql                 # Initial data (admin user)
│       │   └── keys/                    # RSA keys (private.pem, public.pem)
│       └── test/                        # Unit & integration tests
│
└── hotel-service/                       # Hotel & Room Service (port 8081)
    ├── pom.xml
    └── src/
        ├── main/java/mephi/hotelservice/
        │   ├── config/                  # Security, OpenAPI configs
        │   ├── controller/              # HotelController, RoomController
        │   ├── dto/                     # Request/Response DTOs
        │   ├── entity/                  # Hotel, Room, RoomType
        │   ├── exception/               # Custom exceptions & global handler
        │   ├── mapper/                  # MapStruct mappers
        │   ├── repository/              # Spring Data JPA repositories
        │   └── service/                 # HotelService, RoomService
        ├── main/resources/
        │   ├── application.yml
        │   ├── data.sql                 # Initial data (hotels, rooms)
        │   └── keys/                    # RSA public key (public.pem)
        └── test/                        # Unit & integration tests
```

## Architecture Decision Records (ADR)

### ADR-001: Microservices Architecture

**Context:** Need a scalable hotel booking system that can handle independent scaling of different components.

**Decision:** Implement a microservices architecture with separate services for booking management and hotel/room management.

**Consequences:**
- (+) Services can be scaled independently based on load
- (+) Technology choices can vary per service
- (+) Failure isolation - one service failure doesn't bring down the entire system
- (-) Increased operational complexity
- (-) Network latency between services

### ADR-002: Service Discovery with Netflix Eureka

**Context:** Services need to discover and communicate with each other without hardcoded URLs.

**Decision:** Use Netflix Eureka for service discovery and registration.

**Consequences:**
- (+) Dynamic service discovery enables horizontal scaling
- (+) Built-in health checking and instance management
- (+) Seamless integration with Spring Cloud ecosystem
- (-) Additional infrastructure component to maintain

### ADR-003: API Gateway Pattern

**Context:** Clients need a single entry point to the system with cross-cutting concerns handled centrally.

**Decision:** Implement Spring Cloud Gateway as the API gateway.

**Consequences:**
- (+) Single entry point simplifies client integration
- (+) Centralized CORS, routing, and rate limiting
- (+) Circuit breaker integration for fault tolerance
- (-) Gateway becomes a potential single point of failure

### ADR-004: JWT-based Authentication with RSA

**Context:** Need stateless authentication that works across multiple services.

**Decision:** Use JWT tokens signed with RSA-256. Booking Service holds the private key (token generation), Hotel Service has the public key (validation only).

**Consequences:**
- (+) Stateless - no session storage needed
- (+) Asymmetric keys allow validation without sharing signing credentials
- (+) Token contains user claims, reducing database lookups
- (-) Token revocation requires additional mechanisms (not implemented)

### ADR-005: Circuit Breaker Pattern with Resilience4j

**Context:** Inter-service communication can fail; need graceful degradation.

**Decision:** Implement circuit breakers using Resilience4j on all inter-service calls.

**Consequences:**
- (+) Prevents cascade failures
- (+) Provides fallback responses when services are unavailable
- (+) Configurable failure thresholds and recovery times
- (-) Adds complexity to service communication

### ADR-006: H2 In-Memory Database

**Context:** Development and testing environment needs simple database setup.

**Decision:** Use H2 in-memory database with PostgreSQL compatibility mode.

**Consequences:**
- (+) Zero configuration for development
- (+) Fast test execution
- (+) PostgreSQL-compatible SQL for production migration
- (-) Data is not persisted between restarts
- (-) Not suitable for production

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- OpenSSL (for key generation)

### Generate RSA Keys

The system uses RSA key pairs for JWT signing. Generate them before first run:

```bash
# Create key directories
mkdir -p booking-service/src/main/resources/keys
mkdir -p booking-service/src/test/resources/keys
mkdir -p hotel-service/src/main/resources/keys
mkdir -p hotel-service/src/test/resources/keys

# Generate private key (PKCS#8 format)
openssl genpkey -algorithm RSA -out booking-service/src/main/resources/keys/private.pem -pkeyopt rsa_keygen_bits:2048

# Extract public key
openssl rsa -pubout -in booking-service/src/main/resources/keys/private.pem -out booking-service/src/main/resources/keys/public.pem

# Copy public key to hotel-service (for JWT validation)
cp booking-service/src/main/resources/keys/public.pem hotel-service/src/main/resources/keys/

# Copy keys for tests
cp booking-service/src/main/resources/keys/private.pem booking-service/src/test/resources/keys/
cp booking-service/src/main/resources/keys/public.pem booking-service/src/test/resources/keys/
cp booking-service/src/main/resources/keys/public.pem hotel-service/src/test/resources/keys/
```

Key locations:
- `booking-service/src/main/resources/keys/private.pem` - Signs JWT tokens
- `booking-service/src/main/resources/keys/public.pem` - Validates tokens in booking-service
- `hotel-service/src/main/resources/keys/public.pem` - Validates tokens in hotel-service

### Starting the System

Services must be started in order due to dependencies:

```bash
# 1. Build all modules
mvn clean install

# 2. Start Eureka Server (wait for it to be ready)
java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar

# 3. Start API Gateway
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar

# 4. Start Hotel Service
java -jar hotel-service/target/hotel-service-1.0.0-SNAPSHOT.jar

# 5. Start Booking Service
java -jar booking-service/target/booking-service-1.0.0-SNAPSHOT.jar
```

### Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Eureka Dashboard | http://localhost:8761 | Service registry (eureka/eureka-secret) |
| API Gateway | http://localhost:8080 | Main entry point |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |
| Booking Service | http://localhost:8082 | Direct access |
| Hotel Service | http://localhost:8081 | Direct access |

### Initial Data

The system is pre-populated with demo data via `data.sql` files that run on startup.

**Default Users** (password for all: `password`):

| Username   | Email              | Role  |
|------------|--------------------|-------|
| admin      | admin@hotel.com    | ADMIN |
| john_doe   | john@example.com   | USER  |
| jane_smith | jane@example.com   | USER  |
| bob_wilson | bob@example.com    | USER  |

**Pre-loaded Hotels:**

| Hotel              | City     | Stars | Rooms                              |
|--------------------|----------|-------|------------------------------------|
| Grand Plaza Hotel  | New York | 5     | 6 rooms (Standard, Deluxe, Suite)  |
| Seaside Resort     | Miami    | 4     | 5 rooms (Standard, Deluxe, Suite)  |
| Mountain View Lodge| Denver   | 3     | 4 rooms (Standard, Deluxe)         |
| City Center Inn    | Chicago  | 3     | 3 rooms (Standard, Deluxe)         |
| Royal Palace Hotel | Las Vegas| 5     | 5 rooms (Deluxe, Suite, Presidential) |

**Sample Bookings:** 4 bookings in various statuses (COMPLETED, CONFIRMED, CANCELLED)

## Using the System

All API requests go through the API Gateway at `http://localhost:8080`.

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secret123",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9...",
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER"
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secret123"
  }'
```

### 3. Create a Hotel (Admin Only)

```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "name": "Grand Hotel",
    "address": "123 Main Street",
    "city": "Moscow",
    "country": "Russia",
    "starRating": 5
  }'
```

### 4. Create a Room (Admin Only)

```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "hotelId": 1,
    "roomNumber": "101",
    "roomType": "DELUXE",
    "pricePerNight": 150.00,
    "maxOccupancy": 2,
    "available": true
  }'
```

Room types: `STANDARD`, `DELUXE`, `SUITE`, `PRESIDENTIAL`

### 5. Search Available Rooms

```bash
# Get all available rooms
curl http://localhost:8080/api/rooms/available

# Filter by hotel
curl "http://localhost:8080/api/rooms/available?hotelId=1"

# Filter by type
curl "http://localhost:8080/api/rooms/available?roomType=DELUXE"

# Filter by capacity
curl "http://localhost:8080/api/rooms/available?guestCount=2"
```

### 6. Create a Booking

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "roomId": 1,
    "hotelId": 1,
    "checkInDate": "2025-06-01",
    "checkOutDate": "2025-06-05",
    "guestCount": 2,
    "specialRequests": "Late check-in"
  }'
```

### 7. View My Bookings

```bash
curl http://localhost:8080/api/bookings/my \
  -H "Authorization: Bearer <token>"
```

### 8. Cancel a Booking

```bash
curl -X POST "http://localhost:8080/api/bookings/1/cancel?reason=Change%20of%20plans" \
  -H "Authorization: Bearer <token>"
```

## API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/register` | No | Register new user |
| POST | `/login` | No | Authenticate user |
| GET | `/me` | Yes | Get current user info |
| POST | `/admin/create` | Admin | Create admin user |

### Hotels (`/api/hotels`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | No | List all hotels |
| GET | `/{id}` | No | Get hotel by ID |
| GET | `/search?name=` | No | Search hotels by name |
| GET | `/city/{city}` | No | Get hotels by city |
| GET | `/country/{country}` | No | Get hotels by country |
| GET | `/stars/{minStars}` | No | Get hotels by minimum rating |
| POST | `/` | Admin | Create hotel |
| PUT | `/{id}` | Admin | Update hotel |
| DELETE | `/{id}` | Admin | Delete hotel |

### Rooms (`/api/rooms`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | No | List all rooms |
| GET | `/{id}` | No | Get room by ID |
| GET | `/hotel/{hotelId}` | No | Get rooms by hotel |
| GET | `/available` | No | Get available rooms (with filters) |
| GET | `/recommend` | No | Get recommended rooms |
| POST | `/` | Admin | Create room |
| PUT | `/{id}` | Admin | Update room |
| DELETE | `/{id}` | Admin | Delete room |

### Bookings (`/api/bookings`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/` | Yes | Create booking |
| GET | `/my` | Yes | Get user's bookings |
| GET | `/{id}` | Yes | Get booking by ID |
| GET | `/reference/{ref}` | Yes | Get booking by reference |
| POST | `/{id}/cancel` | Yes | Cancel booking |
| GET | `/` | Admin | List all bookings |
| GET | `/status/{status}` | Admin | Get bookings by status |
| GET | `/rooms/recommend` | Yes | Get room recommendations |
| GET | `/rooms/{roomId}` | Yes | Get room details |

Booking statuses: `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`, `FAILED`

## Testing

### Run All Tests

```bash
mvn clean test
```

### Run Tests for Specific Service

```bash
# Hotel Service tests
mvn clean test -pl hotel-service

# Booking Service tests
mvn clean test -pl booking-service
```

### Run Specific Test Class

```bash
mvn test -Dtest=AuthControllerTest
mvn test -Dtest=BookingServiceTest
mvn test -Dtest=HotelControllerTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=AuthControllerTest#shouldRegisterUser
```

### Test Coverage

The project includes unit and integration tests for:

**Booking Service:**
- `AuthControllerTest` - Authentication endpoints
- `BookingControllerTest` - Booking endpoints
- `UserServiceTest` - User service logic
- `BookingServiceTest` - Booking service logic
- `JwtServiceTest` - JWT token operations

**Hotel Service:**
- `HotelControllerTest` - Hotel endpoints
- `RoomControllerTest` - Room endpoints
- `HotelServiceTest` - Hotel service logic
- `RoomServiceTest` - Room service logic

### Test Configuration

Tests use:
- H2 in-memory database (fresh instance per test)
- Disabled Eureka discovery
- Test RSA keys in `src/test/resources/keys/`
