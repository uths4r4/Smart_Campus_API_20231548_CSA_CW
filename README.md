# Smart Campus REST API

**Student Name** Uthsara Kasthurirathne
**Student ID:** 20231548 / w2120708
**Module:** CSA Coursework  
**Tech Stack:** Java 25 · Jakarta EE 10 (JAX-RS) · GlassFish 8 · Maven

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Build & Run Instructions](#2-build--run-instructions)
3. [API Documentation & Sample Commands](#3-api-documentation--sample-commands)
4. [Conceptual Report](#4-conceptual-report)

---

## 1. Project Overview

The **Smart Campus API** is a RESTful web service built as part of the CSA coursework. It is designed to support a smart campus initiative by providing a structured, programmatic interface for managing physical infrastructure — specifically **Rooms**, **Sensors** (e.g., CO2, Temperature), and **Sensor Readings**.

The API is designed around REST principles, including resource-oriented URIs, stateless communication, proper HTTP verb semantics, and structured JSON responses. It additionally demonstrates advanced JAX-RS concepts such as sub-resource locators, exception mappers, and container filters.

### Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 25 |
| Application Server | Eclipse GlassFish 8 |
| API Standard | Jakarta EE 10 — JAX-RS (Jersey 3.1.3) |
| Build Tool | Apache Maven 3.9.x |
| Data Format | JSON (via Jackson) |

---

## 2. Build & Run Instructions

### Prerequisites

- JDK 25 installed and configured
- Apache Maven 3.9.x available on the system path (or use the downloaded binary)
- Eclipse GlassFish 8 installed

### Step 1 — Build the WAR File

Navigate to the project directory and run:

```bash
mvn clean package
```

This compiles the source code and packages it into a `.war` file located at:

```
target/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT.war
```

### Step 2 — Deploy to GlassFish

**Option A — Autodeploy (Recommended)**

Copy the `.war` file directly into the GlassFish autodeploy folder:

```bash
cp target/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT.war \
   ~/GlassFish_Server/glassfish/domains/domain1/autodeploy/
```

GlassFish will detect and deploy the application automatically within a few seconds.

**Option B — Deploy via `asadmin` CLI**

Start the domain and deploy using the administration tool:

```bash
# Start GlassFish
~/GlassFish_Server/bin/asadmin start-domain

# Deploy the WAR
~/GlassFish_Server/bin/asadmin deploy \
  target/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT.war
```

### Step 3 — Verify Deployment

Once deployed, the API is accessible at:

```
http://localhost:8080/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT/api/v1
```

The GlassFish Admin Console is available at:

```
http://localhost:4848
```

---

## 3. API Documentation & Sample Commands

### Base URL

```
http://localhost:8080/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT/api/v1
```

### Endpoint Reference

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1` | Discovery — returns API metadata and resource map |
| `GET` | `/api/v1/rooms` | Retrieve all rooms |
| `POST` | `/api/v1/rooms` | Create a new room |
| `GET` | `/api/v1/rooms/{roomId}` | Retrieve a specific room |
| `DELETE` | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors are assigned → 409) |
| `GET` | `/api/v1/sensors` | Retrieve all sensors (supports `?type=` filter) |
| `POST` | `/api/v1/sensors` | Register a new sensor (validates room existence → 422) |
| `GET` | `/api/v1/sensors/{sensorId}` | Retrieve a specific sensor |
| `GET` | `/api/v1/sensors/{sensorId}/readings` | Retrieve all readings for a sensor |
| `POST` | `/api/v1/sensors/{sensorId}/readings` | Add a reading (blocked if sensor is MAINTENANCE → 403) |

---

### Sample `curl` Commands

#### 1. GET — Discovery Endpoint

Returns the API version, administrative contact, and a map of all primary resource collections.

```bash
curl -s http://localhost:8080/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT/api/v1
```

**Expected Response:**
```json
{
  "api": "Smart Campus API",
  "version": "v1",
  "contact": "20231548@student.westminster.ac.uk",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

#### 2. POST — Create a New Room

Creates a room resource. The server returns `201 Created` with a `Location` header pointing to the new resource.

```bash
curl -s -X POST \
  http://localhost:8080/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "R001", "name": "Lab A", "capacity": 30}'
```

**Expected Response (`201 Created`):**
```json
{
  "id": "R001",
  "name": "Lab A",
  "capacity": 30,
  "sensorIds": []
}
```

---

#### 3. POST — Register a New Sensor (Linked to a Room)

Registers a sensor and associates it with an existing room. If the `roomId` does not exist, the server returns `422 Unprocessable Entity`.

```bash
curl -s -X POST \
  http://localhost:8080/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "S001", "type": "CO2", "roomId": "R001"}'
```

**Expected Response (`201 Created`):**
```json
{
  "id": "S001",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "R001"
}
```

---

#### 4. POST — Add a Sensor Reading (Updates Parent Sensor)

Appends a new reading to a sensor's historical log. As a **side effect**, the parent sensor's `currentValue` field is updated to reflect the latest reading. If the sensor status is `MAINTENANCE`, the server returns `403 Forbidden`.

```bash
curl -s -X POST \
  http://localhost:8080/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT/api/v1/sensors/S001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 450.5}'
```

**Expected Response (`201 Created`):**
```json
{
  "id": "a3f2e1b0-...",
  "value": 450.5,
  "timestamp": 1713371400000
}
```

---

#### 5. GET — Filter Sensors by Type

Retrieves all sensors matching a specific `type` using an optional query parameter. If the `type` parameter is omitted, all sensors are returned.

```bash
curl -s \
  "http://localhost:8080/smart_campus_api_20231548_csa_cw-1.0-SNAPSHOT/api/v1/sensors?type=CO2"
```

**Expected Response (`200 OK`):**
```json
[
  {
    "id": "S001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 450.5,
    "roomId": "R001"
  }
]
```

---

## 4. Conceptual Report

### Part 1.1 — JAX-RS Resource Lifecycle & Data Management

JAX-RS follows a **request-scoped** lifecycle by default: the runtime instantiates a new instance of each resource class for every incoming HTTP request. This behaviour ensures thread isolation at the resource level, but it introduces a critical implication for state management — **any instance variables declared within a resource class are destroyed at the end of each request**.

To prevent data loss across requests, this implementation uses a dedicated `DataStore` class that holds all in-memory data structures — rooms, sensors, and readings — as **static fields backed by `LinkedHashMap` and `ArrayList`**. Because `static` fields belong to the class (not the instance), they persist for the entire lifecycle of the application, surviving across multiple request-scoped resource instantiations.

In a production system, concurrency would require additional synchronisation (e.g., `ConcurrentHashMap` or database-backed persistence) to prevent race conditions. For the scope of this coursework, the static in-memory store provides a sufficient and predictable solution.

---

### Part 1.2 — HATEOAS and the Value of Hypermedia

**HATEOAS** (Hypermedia As The Engine Of Application State) is a REST constraint that mandates API responses include navigational links, allowing clients to discover and traverse available actions dynamically rather than relying on out-of-band documentation.

In this implementation, the discovery endpoint at `GET /api/v1` returns a JSON object containing a `resources` map that explicitly lists the URIs of all primary collections (e.g., `"rooms": "/api/v1/rooms"`). Furthermore, `POST` operations on rooms and sensors return a `Location` header in the HTTP response, pointing directly to the newly created resource.

This approach confers several advantages over static documentation: it makes the API **self-documenting**, reduces the risk of clients hardcoding stale URLs, and enables clients to navigate the API programmatically. As the server evolves and URIs change, clients that follow hypermedia links remain functional without requiring code updates.

---

### Part 2.1 — Returning IDs vs. Full Objects in Collections

When designing the `GET /api/v1/rooms` endpoint, a key architectural decision is whether to return a list of **resource IDs only** or **full room objects**.

Returning only IDs minimises the response payload size, which benefits scenarios with large datasets and bandwidth-constrained clients. However, it forces clients to make a separate `GET /api/v1/rooms/{roomId}` request for each ID they wish to inspect — a pattern known as the **N+1 request problem** — significantly increasing latency and server load.

This implementation returns **full room objects** in the collection response. This trades a marginally larger payload for a substantial reduction in the number of HTTP round-trips required, which is preferable for client-side rendering and API usability in most campus management scenarios.

---

### Part 2.2 — Idempotency of the DELETE Operation

The `DELETE /api/v1/rooms/{roomId}` operation is **idempotent** by design. An operation is idempotent if applying it multiple times produces the same server state as applying it once.

If a client sends `DELETE /api/v1/rooms/R001`, the server removes the room and returns `204 No Content`. If the same request is sent again, the server returns `404 Not Found` because the room no longer exists. Although the **HTTP status code differs** between the first and subsequent calls, the **server state is identical** — the resource is absent in both cases. No additional data is modified or deleted on repeated calls. This satisfies the formal definition of idempotency and makes DELETE safe to retry in the event of network failures or uncertain delivery.

---

### Part 3.1 — Effect of `@Consumes(MediaType.APPLICATION_JSON)`

Annotating a resource method with `@Consumes(MediaType.APPLICATION_JSON)` instructs the JAX-RS runtime to only match incoming requests whose `Content-Type` header is `application/json`. If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, the runtime rejects the request before it reaches the method body and automatically returns **`415 Unsupported Media Type`**.

This is a declarative content negotiation mechanism built into JAX-RS. It eliminates the need for manual content-type validation within business logic, making the code cleaner and ensuring that the server never receives data in an unexpected format it cannot parse.

---

### Part 3.2 — Query Parameters vs. Path Parameters for Filtering

The filtering capability on `GET /api/v1/sensors?type=CO2` is implemented using `@QueryParam` rather than embedding the filter value in the URI path (e.g., `/api/v1/sensors/type/CO2`).

This distinction is architecturally significant. **Path parameters** are part of the resource identifier and imply a fixed, hierarchical resource. **Query parameters** are supplementary and optional, used to refine or search within a collection without altering the identity of the resource being addressed.

Using `@QueryParam` for filtering is the industry-standard approach because: it is optional (the endpoint degrades gracefully to returning all sensors when omitted), it supports multiple simultaneous filters (e.g., `?type=CO2&status=ACTIVE`), and it correctly communicates to clients and intermediaries that the result is a filtered view of a collection rather than a distinct resource.

---

### Part 4.1 — The Sub-Resource Locator Pattern

In `SensorResource`, the path `/{sensorId}/readings` is handled not by a method annotated with `@GET` or `@POST`, but by a **sub-resource locator** — a method annotated only with `@Path`. This method returns an instance of `SensorReadingResource`, to which JAX-RS delegates all further request processing.

This pattern offers meaningful advantages for managing complexity in large APIs. By delegating logic to a dedicated, specialised class (`SensorReadingResource`), each class maintains a **single responsibility**. This is preferable to a monolithic resource class that handles every nested path, which would become difficult to maintain and test as the API grows. The locator also allows the sub-resource instance to be constructed with contextual state (in this case, the `sensorId`), enabling clean parameterisation of behaviour without polluting the parent class.

---

### Part 5.2 — Why 422 Unprocessable Entity is Semantically Preferable to 404

When a client attempts to register a sensor with a `roomId` that does not exist, returning `404 Not Found` would be misleading. The 404 status implies that the **resource being requested** was not found, which is incorrect — the sensor endpoint (`/api/v1/sensors`) was found and processed successfully.

**`422 Unprocessable Entity`** is semantically more precise in this context. It communicates that the request was syntactically valid JSON and was received correctly, but the **business logic could not be fulfilled** because the payload contained an unresolvable reference. This distinction is valuable to API consumers: a 404 might cause a client to retry the same request endpoint, while a 422 clearly signals that the payload itself must be corrected (i.e., the `roomId` must reference an existing room).

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a serious security vulnerability. A stack trace can reveal:

- **Internal file paths** (e.g., `/Users/uthsarak/GlassFish_Server/...`), which disclose server directory structure
- **Library names and versions** (e.g., `Jersey 3.1.3`, `Jackson 2.x`), enabling **dependency-targeted exploits** against known CVEs
- **Internal class and method names**, exposing business logic and application architecture to reverse engineering
- **Database query fragments**, which in persistence-layer exceptions may reveal table names or column structures susceptible to SQL injection

The `GlobalExceptionMapper<Throwable>` in this project intercepts all unhandled exceptions and returns a generic `500 Internal Server Error` JSON response with no internal details. The actual exception is logged **server-side only** using `java.util.logging.Logger`, ensuring that diagnostic information is retained for developers without being exposed to potentially malicious clients.

---

### Part 5.5 — Advantage of JAX-RS Filters for Cross-Cutting Concerns

Cross-cutting concerns such as logging, authentication, and performance monitoring affect every request and response in an API, regardless of which resource handles them. Implementing these concerns directly inside each resource method (e.g., calling `Logger.info()` at the start and end of every `@GET` and `@POST`) violates the **DRY (Don't Repeat Yourself)** principle and couples infrastructure concerns with business logic.

JAX-RS **container filters** (`ContainerRequestFilter` and `ContainerResponseFilter`) provide a clean separation of these concerns. The `LoggingFilter` in this project intercepts every request and response at the framework level, logging the HTTP method, URI, and response status code without any modification to individual resource classes. This approach ensures that logging is **consistent**, **centralised**, and **maintainable** — adding or modifying logging behaviour requires changes to only one class, not every resource in the application.
