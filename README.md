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
- Apache NetBeans IDE 28+

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

### Part 1: Service Architecture & Setup

#### 1. Project & Application Configuration

**Question:** In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

**Answer:** Request-Scoped is the default lifecycle of a JAX-RS Resource class in a typical Jakarta EE deployment such as GlassFish 8. This implies that the runtime will create a new object each time an individual incoming HTTP request is received and will destroy it once the response has been delivered.

Since these cases are temporary, they cannot contain data locally. To resolve this, I used a centralized DataStore class. As the API supports multiple threads dealing with concurrent requests, The recommended approach for this architecture would be to use ConcurrentHashMap as the main collections, which permits thread-safe concurrent reads and localized write locking, rather than a standard LinkedHashMap. The design avoids race conditions and data loss in case two or more campus sensors update the state at the same time, and maintains the integrity of the in-memory data structures on the M4 system without the performance bottleneck of global synchronization.

---

#### 2. The "Discovery" Endpoint

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

**Answer:** The aspect of hypermedia is the Engine of Application State (HATEOAS), which is a characteristic of high-level design due to the fact that the API is self-descriptive. The API is made dynamic and navigable, not just a collection of hard-coded URLs by including hypermedia links in the DiscoveryResource.

To client developers this greatly decreases coupling. In case the backend resource paths are refactored (e.g., /api/v1/rooms to /api/v1/spaces), a client that relies on the Discovery links will still work with no code changes. It enables developers to learn about the capabilities of the API at runtime, and the integration is therefore far more robust to updates on the server-side than using static PDF documentation that can become obsolete.

---

### Part 2: Room Management

#### 1. Room Resource Implementation

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

**Answer:** Sending back only IDs is very efficient in network bandwidth since it reduces the amount of payload to be sent per request. Nonetheless, the design introduces a major N+1 problem to the client; the developer has to make a single initial call to receive the list and then N other API calls to receive the information about a particular room. This adds to the overall latency and imposes an additional load on the server at a given time.

In my implementation, I send back the entire room objects. Although this does slightly add to the initial payload size, it optimizes client-side processing since all the metadata (name, floor, etc.) is delivered in one round-trip. In the case of a Smart Campus dashboard, it is the best design option since it can be rendered to the UI in real-time without additional network calls, effectively trading off bandwidth consumption with a much more favorable user experience and a much lower server overhead.

---

#### 2. Room Deletion & Safety Logic

**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

**Answer:** Yes, the DELETE operation in my implementation is idempotent. By definition, an idempotent operation is one where multiple identical requests have the same effect on the server state as a single request.

In my implementation, when a client sends the first `DELETE /rooms/{roomId}` request, the server locates the room in the DataStore, verifies it has no active sensors, and removes it, returning an HTTP `204 No Content` status. If the client mistakenly sends the exact same request again, the room no longer exists in the collection. Consequently, the API will return an HTTP `404 Not Found`.

Although the HTTP status codes differ (204 vs. 404), the resultant state of the server remains identical: the room is gone, and no further changes occur to the data. Since the server state does not change further after the first successful execution, the implementation satisfies the core requirement of idempotency.

---

### Part 3: Sensor Operations & Linking

#### 1. Sensor Resource & Integrity

**Question:** We explicitly use the `@Consumes (MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

**Answer:** The `@Consumes(MediaType.APPLICATION_JSON)` annotation is a rigid declarative JAX-RS runtime filter. When a client tries to send data in a format like `text/plain` or `application/xml`, the JAX-RS implementation (Jersey/GlassFish) will intercept the request prior to it actually invoking the resource method logic.

The technical impact is an immediate end of the request-response cycle. JAX-RS manages this mismatch by defaulting to sending an HTTP `415 Unsupported Media Type` status code to the client. This in-built mechanism guarantees that the internal data-binding logic (JSON-B / Jackson) of the application is safeguarded against erroneous or inappropriate input, preserving the strictness of data and avoiding superfluous server-side processing of malformed payloads.

---

#### 2. Filtered Retrieval & Search

**Question:** You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?

**Answer:** The basic distinction is between resource identity and resource attributes. A path parameter (e.g., `/sensors/type/CO2`) means that type is a hierarchical, unique aspect of the identity of a resource, which implies a fixed hierarchy of folders. Conversely, a query parameter (e.g. `sensors?type=CO2`) is a parameter that is treated as an attribute to slice or filter a collection.

The query parameter methodology is believed to be the best in terms of filtering and searching due to three technical reasons:

- **Combinatorial Flexibility:** Query parameters can be easily combined with a series of filters (e.g., `/sensors?type=CO2&status=ACTIVE&floor=3`). This would be complicated to implement using path parameters, which have complex, inflexible URL patterns that are hard to maintain and not standard.
- **Semantic Accuracy:** The path in the RESTful design will be the what (the resource), and the query string will be the how (how to display or filter that resource). Filtering does not change the location of the resource, but is a transformation of the view.
- **Optionality:** Query parameters are optional. With the help of `@QueryParam`, the same endpoint can be used to provide the entire collection and a sub-set filtered without a separate method mapping or managing empty path segments, resulting in a more concise and scalable API design.

---

### Part 4: Deep Nesting with Sub-Resources

#### 1. The Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?

**Answer:** Sub-Resource Locator pattern encourages Separation of Concerns and modularity in the API. The parent class is only involved in locating the particular sensor by using a locator technique in SensorResource to get an instance of SensorReadingResource. It then leaves all the logic further on historical data to a special class.

This strategy handles complexity in three major aspects:

- **Avoids "God Classes":** In a large API, specifying all nested paths in a single controller results in an unmaintainable God Class with hundreds of methods. Delegation ensures that individual classes are small, readable, and that they are focused on a single resource context.
- **Encapsulates Context:** The sub-resource class (e.g., `SensorReadingResource`) can be created with particular state, e.g., `sensorId`. It implies that all methods in that class have the context of the parent by default without having to re-extract it out of the path each and every method.
- **Better Reusability:** Sub-resource classes can be reused in other sections of the API. In case there was another resource that needed to be managed by reading, the same `SensorReadingResource` might be returned, giving the same business logic and eliminating the need to duplicate the code across the system.

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging

#### 2. Dependency Validation (422 Unprocessable Entity)

**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:** In RESTful architecture, an HTTP `404 Not Found` response is supposed to indicate that the URI is invalid and does not correspond to any resource on the server. A `404` response to a request that contains a missing `roomId` within a JSON body is ambiguous, with the client unable to tell the difference between a broken link and a data error.

Conversely, HTTP `422 Unprocessable Entity` is more semantically correct since it specifically states that:

1. The server knows the `Content-Type` of the request.
2. The syntax of the JSON payload is correct.
3. The instructions are not handled by the server because of semantic errors, e.g., a foreign key reference that does not exist (e.g., a `roomId` that is not in the system).

The API gives the developer accurate feedback with `422`: the endpoint is correct and the JSON is well-formed, but the business logic cannot be satisfied due to the data provided being logically inconsistent with the existing state of the server. This strict separation makes debugging client-side developers easier and keeps the architecture on a high level.

---

#### 4. The Global Safety Net (500)

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

**Answer:** Leaking internal Java stack traces to external consumers is a serious Information Disclosure vulnerability. Cybersecurity-wise, a raw stack trace will make the reconnaissance stage of an attack easier since it gives the attacker a blueprint of the internal environment of the server. This greatly reduces the entry barrier to more advanced exploits.

A trace can provide an attacker with several high-value pieces of information that are specific:

1. **Software Stack and Versioning:** The trace shows the version of the JAX-RS implementation (Jersey), the application server (GlassFish), and the Java Runtime (JDK) in use. These versions can be cross-referenced with Common Vulnerabilities and Exposures (CVE) databases by attackers to locate known unpatched exploits.
2. **Internal Business Logic and Code Structure:** The trace shows the names of the specific classes, names of the methods and line numbers where an error took place. This enables an attacker to trace the internal logic of the application, and determine possible weak points where input can be injected or logical bypassed.
3. **Physical Directory Structure:** Stack traces can contain absolute file paths on the host machine. Such information is crucial to attackers trying to execute Path Traversal or Local File Inclusion (LFI) attacks, since they do not need to make guesses about the directory structure of the server.
4. **Backend Dependencies:** Traces may indicate the existence of certain libraries or third-party integrations (e.g., database drivers or security frameworks). In case any of these dependencies are known to be weak, the attacker can shift their attention to exploit these secondary targets.

Using a global `ExceptionMapper<Throwable>`, I can be sure that all unforeseen exceptions are caught and substituted with a generic HTTP `500 Internal Server Error` body. This spills no technical data, keeping a fail-safe security posture.

---

#### 5. API Request & Response Logging Filters

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?

**Answer:** Cross-cutting concerns such as logging should be implemented with JAX-RS filters, rather than manually, due to the DRY (Don't Repeat Yourself) principle and separation of concerns. With the introduction of `ContainerRequestFilter` and `ContainerResponseFilter`, the logic of logging is no longer replicated in dozens of resource methods, but is concentrated in one component.

This design has a number of architectural benefits:

- **Guaranteed Coverage:** A filter is a global interceptor of the JAX-RS pipeline. This makes sure that all incoming requests and outgoing responses are automatically logged, even new endpoints which are added in the future. Manual logging is prone to errors, because developers may easily forget to include `Logger.info()` statements in new methods.
- **Maintainability:** In case the logging format or the destination (e.g. switching console logs to a file or external service) must change, it only needs to change in a single location. In the case of manual logging, it would be necessary to search and replace code throughout the codebase.
- **Code Cleanliness:** Resource methods are expected to be business-oriented, i.e. dealing with rooms or sensors. Isolating infrastructure issues such as logging out of the resource logic enhances readability and simplifies testing of the code.
- **Pipeline Integration:** Filters are connected to certain points of the request-response lifecycle. The request filter intercepts the URI and method prior to the resource logic running, and the response filter intercepts the status code at the end of the logic. This organized implementation gives a uniform and precise audit trail of the API performance and conduct.
