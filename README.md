<div align="center">

# Load Balancer

<img src="img.png" alt="Load Balancer Diagram" width="700" />

</div>

<div align="center">

## Overview

</div>

This project is a stateless HTTP load balancer built with Spring Boot.
It distributes incoming requests across multiple backend servers using a round-robin strategy.
The load balancer also performs active health checks and avoids routing traffic to unhealthy backends.
When all backends are unavailable, it returns `503 Service Unavailable`.

<div align="center">

## Functionality

</div>

- Routes incoming HTTP requests to healthy backend servers.
- Uses round-robin selection with atomic index updates.
- Runs periodic active health checks against each backend's `/health` endpoint.
- Automatically marks unhealthy backends and skips them during routing.
- Returns backend responses (status, headers, body) to clients.
- Returns `503 Service Unavailable` when no healthy backend exists.
- Returns `502 Bad Gateway` when proxy forwarding fails.

<div align="center">

## Basic Installation

</div>

Prerequisites:

- Java 21+
- Maven (or use the included Maven Wrapper)

Install dependencies and run tests:

```bash
./mvnw test
```

Run the application:

```bash
./mvnw spring-boot:run
```

The default backend configuration is in `src/main/resources/application.properties`.
Update `lb.backends[...]` entries to match your backend servers.
