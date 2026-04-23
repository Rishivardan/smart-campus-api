# Smart Campus Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey)** and **Grizzly HTTP Server** to manage Rooms and Sensors across a university smart campus. All data is stored in-memory using `ConcurrentHashMap`.

---

## Table of Contents

- [API Overview](#api-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [How to Build & Run](#how-to-build--run)
- [API Endpoints](#api-endpoints)
- [Sample curl Commands](#sample-curl-commands)
- [Report – Question Answers](#report--question-answers)

---

## API Overview

The API is versioned and accessible under `/api/v1`. It exposes three core resources:

| Resource | Base Path |
|---|---|
| Discovery | `GET /api/v1` |
| Rooms | `/api/v1/rooms` |
| Sensors | `/api/v1/sensors` |
| Sensor Readings | `/api/v1/sensors/{sensorId}/readings` |

---

## Technology Stack

- **Java 17**
- **JAX-RS** via Jersey 3.1.5
- **Grizzly HTTP Server** (embedded, no external server needed)
- **Jackson** for JSON serialization
- **Maven** for build management
- **In-memory storage** using `ConcurrentHashMap` (no database)

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/example/smartcampus/
    ├── Main.java                          # Entry point, starts Grizzly server
    ├── config/
    │   └── SmartCampusApplication.java   # JAX-RS Application, @ApplicationPath("/api/v1")
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java                # Shared in-memory ConcurrentHashMap store
    ├── resource/
    │   ├── DiscoveryResource.java        # GET /api/v1
    │   ├── RoomResource.java             # /api/v1/rooms
    │   ├── SensorResource.java           # /api/v1/sensors
    │   └── SensorReadingResource.java    # Sub-resource: /sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   └── SensorUnavailableException.java
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java         # 409
    │   ├── LinkedResourceNotFoundExceptionMapper.java # 422
    │   ├── SensorUnavailableExceptionMapper.java    # 403
    │   └── GlobalExceptionMapper.java               # 500 catch-all
    ├── filter/
    │   └── LoggingFilter.java            # Request & response logging
    └── dto/
        └── ErrorResponse.java            # Standard JSON error body
```

---

## How to Build & Run

### Prerequisites

- Java 17+
- Maven 3.6+

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```

**2. Build the project**
```bash
mvn clean package
```

**3. Run the server**
```bash
mvn exec:java -Dexec.mainClass="com.example.smartcampus.Main"
```

The server will start at:
```
http://localhost:8080/api/v1
```

Press `ENTER` to stop the server.

---

## API Endpoints

### Discovery
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1` | Returns API metadata and resource links |

### Rooms
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/rooms` | Get all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (fails if sensors exist) |

### Sensors
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/sensors` | Get all sensors (supports `?type=` filter) |
| POST | `/api/v1/sensors` | Register a new sensor |
| GET | `/api/v1/sensors?type=CO2` | Filter sensors by type |

### Sensor Readings
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a new reading (updates sensor's currentValue) |

---

## Sample curl Commands

### 1. Get API Discovery Info
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CS-101",
    "name": "Computer Science Lab",
    "capacity": 40
  }'
```

### 3. Get All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Create a Sensor (linked to an existing room)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CO2-001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 400.0,
    "roomId": "CS-101"
  }'
```

### 5. Get All Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 6. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 450.5
  }'
```

### 7. Get All Readings for a Sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/CO2-001/readings
```

### 8. Delete a Room (will fail if sensors are assigned)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/CS-101
```

### 9. Attempt to Create a Sensor with an Invalid Room (triggers 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-999",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.0,
    "roomId": "INVALID-ROOM"
  }'
```

### 10. Get a Specific Room by ID
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

---

## Report – Question Answers

### Q1 – JAX-RS Resource Lifecycle (Part 1.1)

In JAX-RS, resource classes follow a **per-request lifecycle** by default, meaning a new instance of the resource class is created for every incoming HTTP request rather than being shared as a singleton.

This ensures that each request is handled independently, avoiding unintended shared state within resource instances. However, since this project stores data in shared structures (`ConcurrentHashMap`) across all requests, concurrent access must be handled carefully. This implementation uses `ConcurrentHashMap` for all data stores (`rooms`, `sensors`, `sensorReadings`), which provides thread-safe read and write operations and prevents race conditions or data corruption under concurrent load.

---

### Q2 – HATEOAS (Part 1.2)

HATEOAS (Hypermedia as the Engine of Application State) is considered a hallmark of advanced RESTful design because it allows API responses to include links to related resources and available actions, making the API self-descriptive.

Instead of relying on static documentation, clients can dynamically discover available operations through links embedded in responses. This benefits client developers in several ways: they do not need to hardcode URLs, the API guides navigation through valid states, and structural changes to the API do not break client implementations as easily. The Discovery endpoint at `GET /api/v1` in this project is a direct example of HATEOAS — it returns links to all primary resource collections, allowing clients to navigate without prior knowledge of the URL structure.

---

### Q3 – Returning IDs vs Full Room Objects (Part 2.1)

Returning only IDs reduces the response payload size, improving network efficiency and reducing bandwidth usage. However, it forces the client to make additional API calls to retrieve full details for each room, which can result in the N+1 request problem.

Returning full room objects provides all necessary data in a single response, reducing round-trips and simplifying client-side processing. However, it increases response size, which may impact performance with large datasets.

The optimal choice depends on the use case. For dashboard listings, full objects are more convenient. For lightweight lookups or references in other resources, returning IDs is more efficient.

---

### Q4 – DELETE Idempotency (Part 2.2)

Yes, the DELETE operation is idempotent in this implementation. Idempotency means that making the same request multiple times produces the same final system state.

In this implementation, the first DELETE request successfully removes the room from the data store. Any subsequent DELETE request for the same room ID will find no matching room and return `404 Not Found`. Although the HTTP response differs between the first and subsequent calls, the **system state remains unchanged** — the room is still absent — which satisfies the formal definition of idempotency.

---

### Q5 – @Consumes(APPLICATION_JSON) Mismatch (Part 3.1)

The `@Consumes(MediaType.APPLICATION_JSON)` annotation specifies that the endpoint only accepts requests with a `Content-Type: application/json` header.

If a client sends data in a different format such as `text/plain` or `application/xml`, JAX-RS will reject the request and return an **HTTP 415 Unsupported Media Type** response automatically, without invoking the resource method. This ensures that the server only processes data in the expected format and prevents deserialization errors caused by incompatible content types.

---

### Q6 – @QueryParam vs Path-Based Filtering (Part 3.2)

Using `@QueryParam` (e.g., `/sensors?type=CO2`) is preferred for filtering because it aligns with RESTful best practices for collection queries. Query parameters:

- Allow optional filtering — the base path `/sensors` still returns all sensors when no parameter is provided
- Support multiple simultaneous conditions (e.g., `/sensors?type=CO2&status=ACTIVE`)
- Keep the resource path clean and semantically consistent

In contrast, encoding the filter in the path (e.g., `/sensors/type/CO2`) implies a distinct resource rather than a filtered view of a collection, is harder to extend for multiple filters, and can conflict with other path parameters. Query parameters are therefore better suited for searching and filtering collections.

---

### Q7 – Sub-Resource Locator Pattern (Part 4.1)

The Sub-Resource Locator pattern allows nested resources to be delegated to separate, dedicated classes rather than defining all nested paths in a single large controller. This improves code organization by following the **Single Responsibility Principle** — each class is responsible for one resource context.

In this project, `SensorResource` delegates `/sensors/{sensorId}/readings` to a `SensorReadingResource` instance. This keeps `SensorResource` focused on sensor management and `SensorReadingResource` focused on reading management. The pattern also improves reusability and scalability — if readings logic grows in complexity, it can be extended independently without touching the parent resource class. A monolithic controller handling all nested paths would quickly become difficult to maintain and test.

---

### Q8 – HTTP 422 vs 404 (Part 5.2)

HTTP **422 Unprocessable Entity** is more semantically accurate than 404 in this scenario because the issue is not that the endpoint or requested resource is missing — it is that the **request payload references a resource that does not exist**.

A `404 Not Found` signals that the URL itself or the target resource cannot be found. In contrast, when a client POSTs a new sensor with a `roomId` that does not exist, the endpoint is valid and the JSON is syntactically correct, but the data is semantically invalid. HTTP 422 correctly communicates that the server understood the request but could not process it due to invalid content, providing clearer and more actionable feedback to the client.

---

### Q9 – Security Risks of Stack Traces (Part 5.4)

Exposing internal Java stack traces to external API consumers is a significant security risk because they reveal sensitive implementation details, including:

- **Internal class and package names** — revealing application structure
- **File paths** — exposing server directory layout
- **Framework and library versions** — allowing attackers to look up known CVEs for those specific versions
- **Database query fragments or table names** — if a persistence exception propagates

Attackers can use this information to craft targeted exploits. For example, a stack trace showing `org.glassfish.jersey` with a version number immediately narrows the attack surface. The correct approach is to return generic error messages to clients (as implemented via `GlobalExceptionMapper`) while logging full stack trace details internally on the server only.

---

### Q10 – JAX-RS Filters vs Manual Logging (Part 5.5)

Using JAX-RS filters for logging is advantageous because logging is a **cross-cutting concern** that applies uniformly to all endpoints. Implementing it via a filter (implementing `ContainerRequestFilter` and `ContainerResponseFilter`) provides:

- **Centralized logic** — logging is defined once in `LoggingFilter` and applies to every request and response automatically
- **Consistency** — every endpoint is guaranteed to be logged the same way
- **Reduced code duplication** — no need to insert `Logger.info()` calls in every resource method
- **Separation of Concerns** — resource methods focus purely on business logic

Manually inserting logging statements in each method is error-prone, risks inconsistency, and violates the DRY (Don't Repeat Yourself) principle. Filters follow an AOP (Aspect-Oriented Programming) approach, making cross-cutting concerns manageable and maintainable.