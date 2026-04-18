# Smart Campus API — 5COSC022W Coursework
**Student:** Uthsara Kasthurirathne

**Student ID:** 20231548 / w2120708

---

## API Overview

This is a RESTful API built using JAX-RS (Jersey) for the University's Smart Campus initiative. It manages two core resources, Rooms and Sensors - and supports historical sensor readings via a sub-resource pattern. The API follows RESTful principles including proper HTTP status codes, JSON responses, resource nesting, and robust error handling.

**Base URL:** `http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1`

**Resources:**
- `GET /api/v1` — Discovery endpoint
- `/api/v1/rooms` — Room management
- `/api/v1/sensors` — Sensor management
- `/api/v1/sensors/{sensorId}/readings` — Sensor reading history (sub-resource)

---

## Requirements

- JDK 25
- GlassFish Server 8.0.0
- Apache Maven (bundled with NetBeans)
- Apache NetBeans IDE 29+

---

## Build & Run Instructions

### Step 1 — Clone the Repository

```bash
git clone https://github.com/uths4r/Smart_Campus_API_20231548_CSA_CW.git
cd Smart_Campus_API_20231548_CSA_CW
```

### Step 2 — Start GlassFish Server

```bash
/path/to/glassfish8/bin/asadmin start-domain
```

### Step 3 — Build the Project

```bash
mvn clean install
```

### Step 4 — Deploy to GlassFish

Open the project in NetBeans and click **Run**, or deploy manually:

```bash
/path/to/glassfish8/bin/asadmin deploy target/Smart_Campus_API_20231548_CSA_CW-1.0-SNAPSHOT.war
```

### Step 5 — Access the API

```
http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1
```

---

## Sample curl Commands

**1. Discovery endpoint**
```bash
curl -X GET http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1
```

**2. Create a Room**
```bash
curl -X POST http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "LIB-301", "name": "Library Quiet Study", "capacity": 50}'
```

**3. Create a Sensor**
```bash
curl -X POST http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-001", "type": "Temperature", "status": "ACTIVE", "currentValue": 22.5, "roomId": "LIB-301"}'
```

**4. Get all Sensors filtered by type**
```bash
curl -X GET "http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1/sensors?type=Temperature"
```

**5. Post a Sensor Reading**
```bash
curl -X POST http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.3}'
```

**6. Get all Readings for a Sensor**
```bash
curl -X GET http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1/sensors/TEMP-001/readings
```

**7. Attempt to delete a Room that has Sensors (409 Conflict)**
```bash
curl -X DELETE http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1/rooms/LIB-301
```

---

## Report — Answers to Coursework Questions

---

### Part 1.1 — JAX-RS Resource Class Lifecycle

By default, JAX-RS creates a new instance of each resource class for every incoming HTTP request. This is known as the per-request lifecycle. The runtime instantiates the resource class, handles the request, and then discards the instance. It does not reuse the same object across requests.

This has a direct impact on how in-memory data is managed. Because each request gets a fresh resource instance, any instance level fields (non-static variables) would be lost between requests. To preserve data across requests, the data store must be held in static fields, which are shared across all instances and persist for the lifetime of the application. In this implementation, the `DataStore` class uses `static final` maps (`LinkedHashMap`) for rooms, sensors, and readings, ensuring all requests read from and write to the same shared data structures regardless of which resource instance handles them.

However, static shared state introduces the risk of race conditions when multiple requests access or modify the data concurrently. In a production system this would be addressed using thread-safe collections such as `ConcurrentHashMap` or explicit synchronization blocks. For this coursework, which operates in a single user development environment, the standard `LinkedHashMap` is sufficient.

---

### Part 1.2 — HATEOAS and Hypermedia

HATEOAS (Hypermedia as the Engine of Application State) is considered a hallmark of advanced RESTful design because it makes an API self describing and navigable. Rather than requiring clients to rely on external documentation to know what actions are available, a HATEOAS compliant API embeds relevant links directly in its responses, guiding clients to the next possible actions from any given state.

For example, a response to a room creation request could include a link to `GET /api/v1/rooms/LIB-301` and `DELETE /api/v1/rooms/LIB-301`, so the client knows exactly what it can do next without consulting any documentation. This reduces the coupling between the client and server. If a URL changes, the server updates the link in the response and the client adapts automatically.

Compared to static documentation, this approach benefits client developers by reducing the risk of broken integrations when the API evolves, making discovery more dynamic, and simplifying client logic since navigation is driven by the server rather than hardcoded paths.

---

### Part 2.1 — Full Objects vs IDs in Room List Responses

Returning only IDs in a list response is bandwidth-efficient, the payload is minimal and fast to transfer. However, it forces the client to make additional HTTP requests for each ID to retrieve the details it actually needs. This results in what is known as the N+1 problem, where one list request spawns N subsequent detail requests, increasing latency and server load.

Returning full room objects in the list response is heavier in terms of payload size, but it gives the client everything it needs in a single round trip. This is generally preferable when the client is likely to need the full data, as it reduces total latency and simplifies client side logic. The trade off depends on the use case. For large collections where only IDs are needed, returning IDs is appropriate, but for typical campus management views where room details are always displayed, returning full objects is the better design choice.

---

### Part 2.2 — Idempotency of DELETE

The DELETE operation in this implementation is not fully idempotent in the strict HTTP sense. The first DELETE request for a room that exists and has no sensors will successfully remove it and return `204 No Content`. A second identical DELETE request for the same room will find it no longer exists and return `404 Not Found`.

Strictly speaking, idempotency means the same request produces the same server state every time it is applied. After the first DELETE, the room is gone. Subsequent requests do not change the state further, which satisfies the idempotency condition from a state perspective. However, the HTTP response code differs between the first and second call, which some implementations treat as a violation of idempotency. In this implementation, returning `404` on a repeated delete is the more semantically honest choice, as it accurately communicates to the client that the resource no longer exists rather than silently returning `204` again.

---

### Part 3.1 — @Consumes and Content-Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that a method only accepts requests with a `Content-Type` header of `application/json`. If a client sends data with a different format such as `text/plain` or `application/xml`, the JAX-RS runtime will not match the incoming request to the annotated method. Instead, it will automatically return an HTTP `415 Unsupported Media Type` response before the method body is even reached.

This is handled entirely at the framework level. The developer does not need to write any code to reject mismatched content types. The annotation acts as a contract, and Jersey enforces it automatically. This protects the application from attempting to deserialize incompatible data formats and ensures that only well-formed JSON bodies are passed to the resource method.

---

### Part 3.2 — @QueryParam vs Path-Based Filtering

Using `@QueryParam` for filtering (e.g., `GET /api/v1/sensors?type=CO2`) is the more appropriate design for several reasons. Query parameters are semantically intended for optional, non-hierarchical filtering of a collection. They can be omitted entirely, allowing the same endpoint to return all sensors or a filtered subset depending on whether the parameter is present.

In contrast, embedding the filter in the URL path (e.g., `/api/v1/sensors/type/CO2`) implies that `type` is a fixed structural level of the resource hierarchy, which it is not. It also creates ambiguity — the runtime might attempt to match `type` against a `{sensorId}` path parameter, causing routing conflicts. Additionally, path-based filtering makes it harder to combine multiple filters, whereas query parameters compose naturally (e.g., `?type=CO2&status=ACTIVE`).

Query parameters are the standard industry approach for search and filtering operations on collections, as defined in REST conventions and supported by tools like Swagger and OpenAPI.

---

### Part 4.1 — Sub-Resource Locator Pattern Benefits

The Sub-Resource Locator pattern allows a resource class to delegate handling of a nested path to a separate, dedicated class. In this implementation, `SensorResource` delegates all requests under `/{sensorId}/readings` to `SensorReadingResource`, without using an HTTP method annotation on the locator method itself.

The key architectural benefit is separation of concerns. Each resource class has a single, well-defined responsibility  `SensorResource` manages sensors, and `SensorReadingResource` manages readings. This makes each class smaller, easier to understand, and easier to maintain independently.

In contrast, defining every nested path in a single massive controller (e.g., handling `GET /sensors`, `POST /sensors`, `GET /sensors/{id}/readings`, `POST /sensors/{id}/readings` all in one class) leads to a monolithic structure that becomes increasingly difficult to navigate and test as the API grows. The sub-resource pattern also improves testability, as each class can be unit tested in isolation, and it aligns with the Single Responsibility Principle in object-oriented design.

---

### Part 5.2 — HTTP 422 vs 404 for Missing References

HTTP `404 Not Found` is semantically designed for situations where the requested URL itself does not point to an existing resource. When a client sends a POST request to `/api/v1/sensors` with a valid JSON body containing a `roomId` that does not exist, the URL `/api/v1/sensors` is perfectly valid. The endpoint exists and processed the request successfully up to the point of validation.

The issue is not that the URL is wrong, but that the content of the payload contains a reference to a resource that cannot be resolved. HTTP `422 Unprocessable Entity` is the more semantically accurate status code in this case because it signals that the server understood the request, understood the content type, and was able to parse the body, but the data inside failed a business logic validation rule. Returning `404` here would mislead the client into thinking the endpoint itself does not exist, rather than pointing to the real problem. A broken foreign key reference inside an otherwise valid request.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers represents a significant security risk for several reasons. A stack trace reveals the internal structure of the application including class names, method names, file names, and line numbers. An attacker can use this information to map out the application's architecture and identify specific frameworks, libraries, and their versions in use.

With version information, an attacker can cross-reference known vulnerabilities in those libraries (via databases such as the CVE registry) and craft targeted exploits. Stack traces may also reveal database query structures, file system paths, or configuration details that could be leveraged in injection attacks or directory traversal attempts. In some cases, error messages within stack traces expose business logic details that were intended to remain internal.

The Global Exception Mapper in this implementation addresses this by catching all unhandled `Throwable` instances and returning a generic, non-revealing `500 Internal Server Error` JSON response, while logging the full details server-side where only authorised personnel can access them.

---

### Part 5.5 — JAX-RS Filters vs Manual Logging

Using JAX-RS filters for cross cutting concerns like logging is superior to inserting `Logger.info()` calls manually in every resource method for several reasons.

Filters implement the DRY (Don't Repeat Yourself) principle — logging logic is written once in a single class and applied automatically to every request and response without touching any resource method. If the logging format needs to change, only the filter needs to be updated.

Manual logging in every method is error-prone, as it is easy to forget to add the log statement to a new method, or to accidentally log inconsistently. It also pollutes the resource methods with infrastructure concerns that have nothing to do with the business logic they implement, reducing readability and maintainability.

Filters in JAX-RS are applied in a well defined execution pipeline. The request filter runs before any resource method executes, and the response filter runs after the response is built. This guarantees consistent logging coverage across the entire API regardless of how many endpoints are added in the future, without any additional developer effort.
