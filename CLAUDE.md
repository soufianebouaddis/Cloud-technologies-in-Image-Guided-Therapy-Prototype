# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build (Maven wrapper)
./mvnw clean install
./mvnw clean install -DskipTests

# Run a single service
cd <service-dir> && ../mvnw spring-boot:run

# Run tests
./mvnw test                              # all modules
cd <service-dir> && ../mvnw test         # single module
```

## Project Context

Cloud-native microservices platform for **Philips Image-Guided Therapy (IGT)**. The system processes real-time DICOM fluoroscopy streams: denoises frames via a C++ kernel, streams results back to the client, archives raw data, and runs AI analysis (MedGemma) for clinical findings.

## Architecture & Data Flow

```
Simulator (DCM frames)
    │
    ▼ (streams DICOM)
imaging-service ──gRPC──▶ cpp-service (bilateral filter denoising)
    │                          │
    │◀─── denoised frame ──────┘
    │
    ├──▶ WebSocket → Simulator client (denoised frame, real-time)
    ├──▶ Kafka: raw-frames (raw frame for audit archiving)
    └──▶ Kafka: processed-frames (denoised frame → triggers AI)
              │
              ▼
         ai-service
         (consumes processed-frames, calls Ollama medgemma:4b via REST)
              │
              ▼
         Kafka: analysed-frames {frameId, finding, confidence, inferenceMs}
              │
              ├──▶ imaging-service → WebSocket → client (AI finding)
              └──▶ metadata-service → MongoDB (full procedure record:
                     frame metadata, denoising time, stream latency, AI finding)
```

All three imaging-service outputs (WebSocket push, Kafka raw-frames, Kafka processed-frames) happen **in parallel, non-blocking**.

## Services

| Service | Port | Role |
|---------|------|------|
| **discovery-service** | 8761 | Eureka server — all services register here, gateway routes by name |
| **config-service** | 8888 | Spring Cloud Config Server backed by Git repo |
| **apigateway-service** | — | Spring Cloud Gateway (reactive/WebFlux) |
| **imaging-service** | — | Core pipeline: reads DICOM (dcm4che), calls cpp-service via gRPC, pushes to WebSocket & Kafka |
| **ai-service** | — | Consumes processed-frames from Kafka, calls Ollama medgemma:4b, publishes to analysed-frames |
| **metadata-service** | — | Consumes analysed-frames, persists full records to MongoDB (ReactiveMongoRepository) |
| **cpp-service** | — | C++20 gRPC server: bilateral filter denoising with OpenMP |
| **simulator** | — | Streams DICOM frames to imaging-service, receives denoised frames + AI findings via WebSocket |

## Key Technologies

- **Java 21**, Spring Boot 3.5.11, Spring Cloud 2025.0.1
- **Kafka** — event streaming (topics: `raw-frames`, `processed-frames`, `analysed-frames`)
- **MongoDB** — procedure record persistence (reactive)
- **gRPC + Protobuf** — imaging-service ↔ cpp-service communication (`cpp-service/proto/imaging.proto`)
- **WebSocket** — real-time frame + finding delivery to simulator client
- **dcm4che** — DICOM image reading in imaging-service
- **Ollama (medgemma:4b)** — local AI inference on macOS host, called via REST from ai-service
- **C++20, CMake 3.15+, OpenMP** — high-performance denoising kernel

## Configuration

- Centralized config via Config Server pulling from external Git repo
- Required env vars: `GIT_USERNAME`, `GIT_TOKEN`, `EUREKA_SERVER_ADDRESS` (defaults to `http://localhost:8761/eureka`)
- Services import config with `spring.config.import: optional:configserver:http://localhost:8888`

## Infrastructure (local dev)

- Kafka: installed locally
- MongoDB: installed locally
- Ollama with medgemma:4b: running natively on macOS

## C++ Service Build

```bash
cd cpp-service
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build
```

Requires gRPC, Protocol Buffers, and OpenMP. Uses `-O3 -march=native` optimization.
