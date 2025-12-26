🚀 LiteCore v2.0 – Low Latency Java Core Server

LiteCore v2.0 is a lightweight, low-latency Java HTTP server core built from scratch without heavy frameworks.
It is designed to demonstrate how modern backend frameworks work internally, with a strong focus on performance, simplicity, and learning.

The project includes a minimal frontend built using HTML, CSS, and Vanilla JavaScript to interact with the server and visualize responses.

✨ Key Features

⚡ Low-latency HTTP server using raw Java sockets

🧩 Custom HTTP request parsing (no Spring / Netty)

🔁 Thread & connection pooling for concurrent requests

🧠 Middleware-style request flow

📤 Efficient response writing (text & JSON)

🌐 Frontend interface built with HTML, CSS, and JavaScript

🛠️ Designed for learning backend internals & performance tuning

🏗️ Tech Stack

Backend

Java

Raw Sockets

Custom HTTP Parser

Thread Pooling

Frontend

HTML

CSS

Vanilla JavaScript

📂 Project Structure
LiteCore-version-2.0-Low-latency-
│
├── src/
│   ├── Main.java          # Server entry point
│   ├── LiteCore.java      # Core server engine
│   ├── Request.java       # HTTP request parser
│   ├── Response.java      # HTTP response builder
│   ├── Middleware.java   # Middleware logic
│   ├── Pool.java          # Thread / connection pool
│
├── frontend/
│   ├── index.html         # UI to interact with the server
│   ├── style.css          # Styling
│   └── script.js          # Client-side logic
│
├── demo/                  # Sample usage
├── *.jar                  # Executable JARs
└── README.md

🎯 Purpose of the Project

Understand how HTTP servers work under the hood

Learn latency optimization and request lifecycle

Explore threading models & middleware patterns

Bridge backend systems with a simple frontend
