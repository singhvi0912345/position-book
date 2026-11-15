# Position Book API

A Spring Boot REST service that maintains the real-time net position of a traded security held by a trading account. It accepts trade events (BUY, SELL and CANCEL), keeps every position in memory, and exposes the results through a documented REST API with Swagger UI.

Nothing is stored in a database or on the file system. All state lives in the JVM's memory and is lost when the application stops.

**Storage requirement.** The system keeps all data in memory, as the specification requires. The build has no database or persistence dependency. `pom.xml` contains only Spring Boot web, validation, actuator, springdoc (Swagger UI) and test libraries, and the application writes no files. The assumptions and their rationale are recorded in `readme.txt`.

---

## Contents

1. [Features](#1-features)
2. [Technology stack](#2-technology-stack)
3. [Prerequisites](#3-prerequisites)
4. [Getting the code](#4-getting-the-code)
5. [Build and test](#5-build-and-test)
6. [Run the application](#6-run-the-application)
7. [Explore the API with Swagger UI](#7-explore-the-api-with-swagger-ui)
8. [API reference](#8-api-reference)
9. [Worked walkthrough of the four examples](#9-worked-walkthrough-of-the-four-examples)
10. [Business rules and assumptions](#10-business-rules-and-assumptions)
11. [Validation rules](#11-validation-rules)
12. [Error responses](#12-error-responses)
13. [Configuration](#13-configuration)
14. [Project structure](#14-project-structure)
15. [Testing](#15-testing)
16. [Troubleshooting](#16-troubleshooting)
17. [Limitations](#17-limitations)

---

## 1. Features

- Process BUY, SELL and CANCEL trade events.
- Net position per (trading account, security) pair: BUY adds, SELL subtracts.
- CANCEL reverses the effect of an earlier BUY or SELL that has the same event ID.
- Retrieve one position with its full drilldown of events, including cancelled originals and CANCEL records.
- List all positions, optionally filtered by trading account.
- Input validation at the boundary, with RFC 7807 problem responses for every error.
- Thread-safe under concurrent submissions.
- Interactive API documentation through Swagger UI and an OpenAPI 3 document.
- Health endpoint through Spring Boot Actuator.

## 2. Technology stack

| Concern | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot (web MVC, validation, actuator) | 3.3.5 |
| API documentation | springdoc-openapi (Swagger UI and OpenAPI 3) | 2.6.0 |
| Build tool | Apache Maven | 3.8 or later |
| Testing | JUnit 5, AssertJ, Spring MockMvc, Hamcrest | Managed by Spring Boot |
| Coverage | JaCoCo | 0.8.12 |
| Storage | In-process memory (HashMap guarded by a ReentrantReadWriteLock) | none |

The project uses no external database. An in-memory store is part of the application itself, so there is nothing to install or start.

## 3. Prerequisites

You need the following installed before you build or run the project.

**Java Development Kit 17.** Any 17.x distribution works, such as Eclipse Temurin, Amazon Corretto or Oracle JDK. Newer versions such as 21 also run the code, but this project targets 17.

Check the installed version:

```
java -version
```

The output must show version 17. If it shows a different version, install JDK 17 and point `JAVA_HOME` at it (see [Troubleshooting](#16-troubleshooting)).

**Apache Maven 3.8 or later.** Check with:

```
mvn -version
```

Maven must report that it uses Java 17. If you use an IDE with its own bundled Maven, that is fine as long as the IDE's project JDK is 17.

**Internet access on the first build.** Maven downloads the Spring Boot, springdoc and test libraries from Maven Central. Later builds work offline from the local repository in `~/.m2`.

**Port 8080 free on your machine.** The application listens on 8080 by default. You can change this (see [Configuration](#13-configuration)).

**A way to send HTTP requests.** You can use a web browser for Swagger UI, or `curl` from a terminal. `curl` ships with macOS, Linux and recent Windows versions.

**Optional: an IDE.** IntelliJ IDEA, Eclipse with the m2e plugin, or VS Code with the Java extension pack all import Maven projects directly.

Installation references for each platform:

- JDK 17: https://adoptium.net (Temurin) or your organisation's standard distribution
- Maven: https://maven.apache.org/install.html

## 4. Getting the code

If you received a zip file, extract it and open a terminal in the extracted `position-book` folder. The folder must contain `pom.xml` at its top level.

If you clone from a repository instead:

```
git clone <repository-url> position-book
cd position-book
```

Every command in this README runs from that project folder.

## 5. Build and test

Compile the code, run all tests, and check coverage in one step:

```
mvn clean verify
```

A successful run ends with `BUILD SUCCESS`. The build fails if:

- any test fails, or
- line coverage falls below 90 percent, or
- branch coverage falls below 85 percent.

The coverage report is written to `target/site/jacoco/index.html`. Open it in a browser to see coverage by class and by line.

To run only the tests without the coverage check:

```
mvn test
```

To run one test class:

```
mvn -Dtest=PositionBookTest test
```

To build the executable jar without running the tests:

```
mvn clean package -DskipTests
```

The jar is created at `target/position-book-1.0.0.jar`.

## 6. Run the application

Choose one of the following.

**Option A: Maven plugin (development).** This starts the application from the source tree and reloads when you restart the command:

```
mvn spring-boot:run
```

**Option B: executable jar.** Build first, then run the jar:

```
mvn clean package -DskipTests
java -jar target/position-book-1.0.0.jar
```

When the application starts, the console shows a line similar to:

```
Tomcat started on port 8080 (http)
Started PositionBookApplication in ... seconds
```

Stop the application with `Ctrl+C`.

Once it is running, these addresses are available:

| What | Address |
|---|---|
| Swagger UI (interactive documentation) | http://localhost:8080/swagger-ui.html |
| OpenAPI 3 document (JSON) | http://localhost:8080/v3/api-docs |
| Health check | http://localhost:8080/actuator/health |

Confirm the service is up:

```
curl http://localhost:8080/actuator/health
```

Expected response: `{"status":"UP"}`

## 7. Explore the API with Swagger UI

1. Start the application (section 6).
2. Open http://localhost:8080/swagger-ui.html in a browser. It redirects to the interactive page.
3. Expand a group, such as **Trade events** or **Positions**.
4. Click an operation, then **Try it out**.
5. Edit the request body or the parameters, then click **Execute**.
6. Read the response code, the response body, and the equivalent `curl` command shown under **Curl**.

To try the examples from section 9 in Swagger UI, submit the events in order using **POST /api/v1/trade-events**, then query the result with **GET /api/v1/positions/{tradingAccount}/{securityId}**.

The state is shared by every request made to the running application. Restart the application to start with an empty Position Book.

## 8. API reference

Base path: `/api/v1`. All request and response bodies are JSON.

### 8.1 Submit a trade event

```
POST /api/v1/trade-events
```

Request body for BUY or SELL:

| Field | Type | Required | Rule |
|---|---|---|---|
| id | integer (long) | yes | Positive. A CANCEL reuses the ID of the event it cancels. |
| eventType | string | yes | `BUY`, `SELL` or `CANCEL`. Case-sensitive. |
| tradingAccount | string | yes for BUY and SELL | 1 to 20 characters from letters, digits, `-` and `_`. Optional for CANCEL. |
| securityId | string | yes for BUY and SELL | Same format as `tradingAccount`. Optional for CANCEL. |
| quantity | integer (long) | yes for BUY and SELL | 1 to 1,000,000,000. Ignored for CANCEL. |

Responses:

| Status | Meaning |
|---|---|
| 201 Created | Event accepted. The body echoes the accepted event. |
| 400 Bad Request | Validation failed, the JSON is malformed, or `eventType` is not one of the allowed values. |
| 404 Not Found | A CANCEL refers to an ID that does not exist. |
| 409 Conflict | A BUY or SELL reuses an existing ID, or a CANCEL targets an event that is already cancelled. |
| 422 Unprocessable Entity | A CANCEL supplies an account or security that differs from its target. |

Example, a BUY:

```
curl -X POST http://localhost:8080/api/v1/trade-events \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "eventType": "BUY", "tradingAccount": "ACC1", "securityId": "SEC1", "quantity": 100}'
```

Response (201):

```
{"id":1,"type":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":100,"cancelled":false}
```

Example, a CANCEL:

```
curl -X POST http://localhost:8080/api/v1/trade-events \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "eventType": "CANCEL"}'
```

Response (201):

```
{"id":1,"type":"CANCEL","tradingAccount":"ACC1","securityId":"SEC1","quantity":0,"cancelled":false}
```

The account and security in the response come from the cancelled event, even when the request omitted them.

### 8.2 Get one position

```
GET /api/v1/positions/{tradingAccount}/{securityId}
```

Path parameters:

| Parameter | Rule |
|---|---|
| tradingAccount | 1 to 20 characters from letters, digits, `-` and `_` |
| securityId | Same format as `tradingAccount` |

Responses:

| Status | Meaning |
|---|---|
| 200 OK | Position found. |
| 400 Bad Request | A path parameter has an invalid format. |
| 404 Not Found | No event has ever referenced this account and security. |

A pair that has events but a net quantity of zero returns 200 with `"quantity": 0`.

```
curl http://localhost:8080/api/v1/positions/ACC1/SEC1
```

Response (200):

```
{
  "tradingAccount": "ACC1",
  "securityId": "SEC1",
  "quantity": 150,
  "events": [
    {"id":1,"type":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":100,"cancelled":false},
    {"id":2,"type":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":50,"cancelled":false}
  ]
}
```

In `events`, entries appear in arrival order. A cancelled original has `"cancelled": true`. Each CANCEL appears as its own entry with quantity 0.

### 8.3 List positions

```
GET /api/v1/positions
GET /api/v1/positions?tradingAccount=ACC1
```

Returns an array of positions sorted by trading account, then by security ID. The optional `tradingAccount` parameter restricts the list to one account. An account with no positions returns an empty array, not an error.

```
curl "http://localhost:8080/api/v1/positions?tradingAccount=ACC1"
```

### 8.4 Health and documentation endpoints

| Method and path | Purpose |
|---|---|
| GET /actuator/health | Liveness and health status |
| GET /actuator/info | Application information |
| GET /v3/api-docs | OpenAPI 3 document in JSON |
| GET /swagger-ui.html | Swagger UI |


### 8.5 Mapping to the high-level design

The HLD (section 4.3) defines three endpoints. Each one is implemented as follows.

| HLD endpoint | Purpose | Controller method | Service method | Domain method |
|---|---|---|---|---|
| POST /api/v1/trade-events | Submit BUY, SELL or CANCEL | TradeEventController.submitTradeEvent | TradeEventService.submitTradeEvent | PositionBook.applyTradeEvent |
| GET /api/v1/positions/{tradingAccount}/{securityId} | Retrieve one position with drilldown | PositionController.getPosition | PositionQueryService.getPosition | PositionBook.find |
| GET /api/v1/positions?tradingAccount={account} | List positions | PositionController.listPositions | PositionQueryService.listPositions | PositionBook.findAll |

The POST endpoint returns 201 Created with the accepted event. Both GET endpoints return 200 OK, and the single-position GET returns 404 for an unknown pair.

Two intentional differences from the HLD are noted here. First, the HLD describes a separate `PositionBookStore` interface and `InMemoryPositionBookStore` class. In the implementation, the store and the business rules are combined in `PositionBook`, which keeps the locking next to the rules it protects. Second, the HLD's error mapping is implemented as `GlobalExceptionHandler`, using RFC 7807 Problem Details.

## 9. Worked walkthrough of the four examples

These steps reproduce the four scenarios from the exercise specification. Run them in order against a freshly started application, so that no earlier data is present.

**Example 1: buying the same security twice.**

```
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":1,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":100}'
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":2,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":50}'
curl localhost:8080/api/v1/positions/ACC1/SEC1
```

Expected: quantity 150, with events 1 and 2.

**Example 2: different securities and accounts.**

```
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":3,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":12}'
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":4,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SECXYZ","quantity":50}'
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":5,"eventType":"BUY","tradingAccount":"ACC2","securityId":"SECXYZ","quantity":33}'
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":6,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":20}'
curl "localhost:8080/api/v1/positions"
```

Expected: ACC1/SEC1 = 32 (events 3 and 6), ACC1/SECXYZ = 50, ACC2/SECXYZ = 33.

**Example 3: buying and selling.**

```
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":10,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":100}'
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":11,"eventType":"SELL","tradingAccount":"ACC1","securityId":"SEC1","quantity":50}'
curl localhost:8080/api/v1/positions/ACC1/SEC1
```

Expected: quantity 50, with a BUY and a SELL.

**Example 4: cancelling an event.** This example uses IDs 21 and 22, which do not clash with the earlier examples. Run it in the same application as the others, or restart the application first to start from an empty Position Book. Then run:

```
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":21,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":100}'
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":21,"eventType":"CANCEL","tradingAccount":"ACC1","securityId":"SEC1","quantity":0}'
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" -d '{"id":22,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":5}'
curl localhost:8080/api/v1/positions/ACC1/SEC1
```

Expected: quantity 5, with three entries: the cancelled BUY 21, the CANCEL 21 with quantity 0, and BUY 22.

The same four scenarios run automatically as tests, so you do not need to repeat them by hand to verify the code (see [Testing](#15-testing)).

## 10. Business rules and assumptions

The exercise specification leaves some behaviour open. The complete assumptions, with rationale and the code that enforces each one, are in `readme.txt` at the project root. The table below summarises them.

| # | Topic | Behaviour |
|---|---|---|
| A1 | Event ID | Positive integer. BUY and SELL IDs are unique. A CANCEL reuses its target's ID. |
| A2 | Duplicate ID | A BUY or SELL with an ID already in use returns 409. The ID stays reserved after cancellation. |
| A3 | Cancel target | Must exist, must be a BUY or SELL, and must not already be cancelled. |
| A4 | Cancel of a cancel | Not possible. |
| A5 | Cancel account and security | Quantity is ignored. If supplied, the account and security must match the target (otherwise 422). If omitted, the target's values are used. |
| A6 | Negative positions | Allowed. A SELL may exceed holdings and produce a short position. |
| A7 | Quantity | Integer from 1 to 1,000,000,000 for BUY and SELL. |
| A8 | Unknown position | 404 when no event has referenced the pair. A flat position returns 200 with quantity 0. |
| A9 | Drilldown | Every event for the pair in arrival order, including cancelled originals and CANCEL records. |
| A10 | Identifiers | 1 to 20 characters from letters, digits, `-` and `_`. Case-sensitive. |
| A11 | Ordering | Events are applied in arrival order. No timestamps are needed. |
| A12 | Deployment | Single instance. A restart discards all state. |

If any of these choices does not match what you expected, change the corresponding code and tests. The rules are in `engine/PositionBook.java` and `model/Position.java`.

## 11. Validation rules

Validation happens before any state changes. A request that fails validation has no effect on the Position Book.

| Input | Rule |
|---|---|
| id | Required, greater than 0 |
| eventType | Required, one of BUY, SELL, CANCEL |
| tradingAccount | Required for BUY and SELL. Optional for CANCEL. 1 to 20 characters from `[A-Za-z0-9_-]`. |
| securityId | Required for BUY and SELL. Optional for CANCEL. 1 to 20 characters from `[A-Za-z0-9_-]`. |
| quantity | Required for BUY and SELL, from 1 to 1,000,000,000. Ignored for CANCEL. |
| Path and query identifiers | 1 to 20 characters from `[A-Za-z0-9_-]` |

## 12. Error responses

Every error is returned as an RFC 7807 problem document with the content type `application/problem+json`. A validation failure looks like this:

```
curl -X POST localhost:8080/api/v1/trade-events -H "Content-Type: application/json" \
  -d '{"id":0,"eventType":"BUY","tradingAccount":"ACC1","securityId":"SEC1","quantity":10}'
```

Response (400):

```
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/trade-events",
  "errors": ["id: must be greater than 0"]
}
```

A business-rule failure, such as a duplicate ID, returns the same shape with its own `status`, `title` and `detail`. Unexpected failures return 500 with a generic message. The stack trace is written to the application log, not to the response.

## 13. Configuration

Settings live in `src/main/resources/application.yml`:

```
spring:
  application:
    name: position-book
server:
  port: 8080
management:
  endpoints:
    web:
      exposure:
        include: health,info
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
logging:
  level:
    com.bank.positionbook: INFO
```

Any value can be overridden without editing the file, which is useful for running a second copy on another port:

```
java -jar target/position-book-1.0.0.jar --server.port=9090
```

Or with an environment variable:

```
SERVER_PORT=9090 java -jar target/position-book-1.0.0.jar
```

To see more detail from the application code, set the logging level to `DEBUG`:

```
java -jar target/position-book-1.0.0.jar --logging.level.com.bank.positionbook=DEBUG
```

## 14. Project structure

```
position-book/
├── pom.xml                              Maven build, dependencies, JaCoCo gate
├── README.md                            This file
├── readme.txt                           Assumptions and rationale
├── docs/NAMING_CONVENTIONS.md           Naming rules for classes, methods, variables and APIs
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/bank/positionbook/
    │   │   ├── PositionBookApplication.java   Spring Boot entry point
    │   │   ├── api/                           REST controllers
    │   │   │   ├── ApiConstants.java              All API constants: paths, rules, messages, OpenAPI metadata
    │   │   │   ├── TradeEventController.java  POST /api/v1/trade-events
    │   │   │   ├── PositionController.java    GET  /api/v1/positions...
    │   │   │   ├── dto/                       Request and response records, cross-field validation
    │   │   │   └── error/GlobalExceptionHandler.java  Maps errors to ProblemDetail
    │   │   ├── service/                       Application services (DTO to domain and back)
    │   │   ├── model/                         Domain objects and value types, no Spring dependencies
    │   │   │   ├── Position.java              Net quantity and drilldown per pair
    │   │   │   ├── TrackedTrade.java          BUY or SELL event that can be cancelled
    │   │   │   ├── TradeEvent.java, EventType.java, PositionKey.java, DrilldownEntry.java, PositionSnapshot.java
    │   │   │   └── PositionBookException.java Business failures, with a errorType mapped to HTTP by the API
    │   │   ├── engine/                        Applies events and answers queries; holds the business rules
    │   │   │   └── PositionBook.java          Aggregate root, delegates all state to the store
    │   │   ├── store/                         Where state lives
    │   │   │   ├── PositionBookStore.java     Storage contract (interface)
    │   │   │   └── InMemoryPositionBookStore.java  Maps and read/write lock, in memory
    │   │   └── config/                        Bean wiring and OpenAPI metadata
    │   └── resources/application.yml
    └── test/java/com/bank/positionbook/
        ├── engine/                            Unit tests for business rules and concurrency
        └── api/                               Validation tests and HTTP integration tests
```


## 15. Testing

Run the full suite with `mvn clean verify`. The suite contains these groups:

| Test class | What it checks |
|---|---|
| PositionBookTest | All four examples from the specification, plus duplicate IDs, cancel rules, short positions, sorting and filtering. |
| PositionBookConcurrencyTest | 32 threads each submitting 1,000 BUYs give exactly 32,000. Racing duplicate IDs accept exactly one event. |
| TradeEventRequestValidationTest | Every field rule and every cross-field rule, including boundary values. |
| PositionBookApiIntegrationTest | Full HTTP round trips for the four examples, error status codes, problem responses, path and query validation, the OpenAPI document and Swagger UI. |

Each HTTP integration test starts with an empty Position Book, because the application context is rebuilt after each test method.

The coverage gate (90 percent line, 85 percent branch) is set in `pom.xml`. If you add code, add tests with it. Coverage figures are visible in `target/site/jacoco/index.html` after `mvn verify`.

## 16. Troubleshooting

**`java -version` shows a version other than 17.** Install JDK 17 and set `JAVA_HOME` to its folder. On macOS and Linux:

```
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
```

On Windows PowerShell:

```
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

**`release version 17 not supported` during the build.** Maven is running on an older JDK. Confirm with `mvn -version` and correct `JAVA_HOME` as above.

**`Port 8080 was already in use`.** Another process holds the port. Either stop it or start this application on a different port, as shown in section 13.

**The build cannot download dependencies.** Check your internet connection and any corporate proxy. Maven reads proxy settings from `~/.m2/settings.xml`. Ask your IT team for the proxy host and port if needed.

**Swagger UI shows a blank page or 404.** Make sure the application has started successfully, then use http://localhost:8080/swagger-ui.html (not `/swagger-ui/index.html`, which is the page the redirect lands on). If the page still fails, check the OpenAPI document at http://localhost:8080/v3/api-docs.

**Positions disappear after a restart.** This is expected. The store is in memory only, as the specification requires.

**Tests fail with a coverage message even though all tests pass.** The JaCoCo gate failed. Look for the lines reported as uncovered in `target/site/jacoco/index.html` and add tests for them.

## 17. Limitations

- All state is lost when the application stops.
- A single instance only. Two running copies do not share data.
- Writes are serialised by one lock. This is simple and correct, but it limits throughput under very heavy write load.
- There is no authentication or authorisation. Add Spring Security before exposing the service beyond a trusted network.
- Timestamps are not recorded, so the drilldown reflects arrival order only.
