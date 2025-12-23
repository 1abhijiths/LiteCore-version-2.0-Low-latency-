LiteCore v2.0 – Low Latency Java Core Server

LiteCore v2.0 is a lightweight, low-latency Java server core built from scratch without heavy frameworks.
It is designed to handle HTTP requests efficiently with minimal overhead, making it suitable for learning, experimentation, and performance-critical systems.

This project focuses on raw socket handling, custom request parsing, middleware flow, and fast response writing, inspired by how modern backend frameworks work internally.

Features

Low latency architecture (no Spring, no heavy abstractions)

Built using Java Sockets

Modular design (Server, Request, Response, Middleware)

Basic HTTP request and response handling

Middleware support similar to Express.js and Spring Filters

JAR executable support

Educational project demonstrating how backend frameworks work internally

Project Structure
LiteCore-version-2.0-Low-latency-
│
├── src/
│   ├── Main.java          # Entry point
│   ├── LiteCore.java      # Core server engine
│   ├── Request.java       # HTTP request parser
│   ├── Response.java      # HTTP response builder
│   ├── Middleware.java    # Middleware interface / logic
│   ├── Pool.java          # Thread / connection pooling
│
├── lib/                   # External libraries (if any)
├── demo/                  # Sample usage / demo code
├── *.jar                  # Executable JAR files
└── README.md

Architecture Overview

LiteCore follows a layered, event-driven server architecture optimized for low latency.

1. Server Core (LiteCore)

Opens a ServerSocket

Listens for incoming client connections

Hands off each connection to a thread pool

Keeps the main server thread lightweight and non-blocking

Why this helps latency:
Connections are delegated immediately, avoiding long blocking operations in the main thread.

2. Thread / Connection Pool (Pool)

Manages reusable worker threads

Each request is processed by a worker thread

Avoids overhead from creating a new thread per request

Benefits:

Faster execution

Better scalability

Controlled CPU and memory usage

3. Request Handling (Request)

Parses raw HTTP data from the socket input stream

Extracts:

HTTP method (GET, POST, etc.)

Request path

Headers

Body (if present)

Key idea:
Direct interaction with raw HTTP avoids framework overhead and improves performance.

4. Middleware Layer (Middleware)

Executes before the final response is sent

Can be used for:

Logging

Authentication

Validation

Rate limiting

Flow:

Request → Middleware → Response


Inspired by Express.js middleware and Spring filters.

5. Response Builder (Response)

Writes raw HTTP responses directly to the output stream

Supports:

Plain text responses

JSON responses

Headers are manually constructed for full control

Example response format:

HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: ...


Why this matters:
Direct stream writing reduces overhead and improves response time.

Request Lifecycle
Client
  ↓
ServerSocket
  ↓
Thread Pool
  ↓
Request Parser
  ↓
Middleware Chain
  ↓
Response Writer
  ↓
Client

Why LiteCore Is Low Latency

No reflection

No dependency injection containers

No ORM

No annotation scanning

Direct socket and stream handling

Minimal object creation

This results in predictable performance and fast response times.

How to Run
Compile
javac *.java

Run
java Main

Run using JAR
java -jar LiteCore.jar
