# Smart Campus API testing notes

## What was found
- Framework: Jakarta EE / JAX-RS (Jersey)
- Packaging: WAR
- Base path: `http://localhost:8080/Smart_Campus_API_20231548_CSA_CW/api/v1`
- Resources:
  - `GET /`
  - `GET /rooms`
  - `POST /rooms`
  - `GET /rooms/{roomId}`
  - `DELETE /rooms/{roomId}`
  - `GET /sensors`
  - `POST /sensors`
  - `GET /sensors/{sensorId}`
  - `GET /sensors/{sensorId}/readings`
  - `POST /sensors/{sensorId}/readings`

## Current test state
- No Java test classes were found under `src/test`.
- `mvn test` completes successfully, but there are effectively no automated tests yet.

## Suggested key test cases

### Happy path
1. Discovery endpoint returns API metadata and resource links.
2. Create room with valid payload returns `201 Created` and `Location` header.
3. Get all rooms returns an array including the created room.
4. Get room by ID returns `200 OK` and expected room fields.
5. Create sensor with valid existing `roomId` returns `201 Created`.
6. Sensor created without `status` defaults to `ACTIVE`.
7. Get all sensors returns created sensor.
8. Filter sensors by `type` returns matching sensor(s) only.
9. Create reading for active sensor returns `201 Created` and generated `id`/`timestamp` when omitted.
10. Get readings returns array containing posted reading.
11. Posting a reading updates parent sensor `currentValue`.
12. Delete empty room returns `204 No Content`.

### Edge and validation
1. Create room without `id` returns `400 Bad Request`.
2. Create duplicate room ID returns `409 Conflict`.
3. Get unknown room returns `404 Not Found`.
4. Delete unknown room returns `404 Not Found`.
5. Delete room that still has sensors returns `409 Conflict`.
6. Create sensor without `id` returns `400 Bad Request`.
7. Create duplicate sensor ID returns `409 Conflict`.
8. Create sensor with missing or unknown `roomId` returns mapped validation error (README suggests `422 Unprocessable Entity`; verify actual implementation).
9. Get unknown sensor returns `404 Not Found`.
10. Get readings for unknown parent sensor returns `404 Not Found`.
11. Post reading to unknown parent sensor returns `404 Not Found`.
12. Post invalid content type to JSON-only endpoints returns `415 Unsupported Media Type`.
13. Send malformed JSON returns `400` or framework-level parsing error; verify actual response shape.

### Business rules
1. Room cannot be deleted while `sensorIds` is not empty.
2. Sensor in `MAINTENANCE` cannot accept readings and should return mapped error status.
3. Reading with omitted `id` gets generated automatically.
4. Reading with omitted `timestamp` gets generated automatically.
5. Sensor filter should be case-insensitive for `type`.

### Security / robustness
1. Error bodies should not expose Java stack traces.
2. Unexpected server errors should return generic JSON error response.
3. Verify only JSON is accepted on POST endpoints.
4. Very large payloads or unexpected fields should be handled safely.
5. Concurrent creation with same IDs should not create duplicates.

## Recommended execution order in Postman
1. Discovery
2. Create room `LIB-301`
3. Create empty room `LAB-EMPTY`
4. Create sensor `TEMP-001` in `LIB-301`
5. Get sensor / filter sensors
6. Create reading for `TEMP-001`
7. Get readings
8. Attempt delete `LIB-301` -> expect `409`
9. Delete `LAB-EMPTY` -> expect `204`

## CI and monitoring suggestions
- Add collection-level tests, then run them with Newman in CI.
- Add environments for local/dev/prod with `baseUrl` variable.
- Add a monitor for smoke tests: discovery, list rooms, list sensors.
- Track regressions on status codes and error payload shapes.
