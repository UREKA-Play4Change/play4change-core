# 🎮🌍 U!REKA Play4Change

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-Backend-6DB33F?logo=spring&logoColor=white)
![Cloudflare Workers](https://img.shields.io/badge/Cloudflare-Edge_AI-F38020?logo=cloudflare&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white)

**Play4Change** is a multiplatform serious game focused on **sustainability, digital skill development, and behavioral transformation**.

Developed as a BSc final-year project at **ISEL (Instituto Superior de Engenharia de Lisboa)** in collaboration with the **U!REKA European University Alliance** and the **University of Amsterdam of Applied Sciences**.

---

## ✨ Key Game Features

### 🎯 Daily Adaptive Tasks
Players receive sustainability and digital education challenges tailored to their progression.

### 🤖 AI-Powered Contextual Hints
Edge AI analyzes progression using IRT models to generate adaptive hints in real time.

### 📊 Progression & Behavioral Tracking
- User identification & progression tracking
- Reward mechanisms for task completion
- Penalty mechanisms for non-compliance
- Behavioral analytics

### 📡 Offline-First Architecture
Custom `SyncManager`:
- Offline state persistence
- Conflict resolution
- Automatic backend synchronization

---

## 🏗 Architecture Overview

This project follows a **scalable, enterprise-grade monorepo architecture**, applying:

- Domain-Driven Design (DDD)
- Single Responsibility Principle (SRP)
- Clean Architecture
- Unidirectional Data Flow (UDF)

---

## 📂 Repository Structure

```text
.
├── ai-agent/         # Cloudflare Worker, Hono API Gateway, LLM integration
├── composeApp/       # KMP mobile application (Android/iOS shared UI)
├── core/             # Shared functional domain, value classes, Result wrappers
├── infra/            # Docker compose, Prometheus config, Grafana dashboards
├── server/           # Spring Boot application, PostgreSQL entities
└── build.gradle.kts  # Monorepo build configuration
```

---

## 📦 Core Modules

### 📱 `composeApp` — Client (Kotlin Multiplatform)

- Built with **Kotlin Multiplatform (KMP)**
- UI powered by **Jetpack Compose**
- Lifecycle-aware routing using **Decompose**
- Enforces deterministic state management (UDF)
- Shared UI logic across Android & iOS
- Memory-safe state handling

---

### ⚙️ `server` — Backend (Spring Boot)

- Kotlin + Spring Boot
- RESTful API architecture
- PostgreSQL persistence
- JPA / Hibernate ORM
- OAuth 2.0 & Magic Link authentication
- 12-Factor compliant design

---

### 🧠 `core` — Shared Domain Layer

The functional heart of the application.

- Shared across client & server
- Kotlin value classes for strong domain modeling
- Custom Monadic Comprehension pattern
  - `Result` wrappers
  - `.bind()` operators
- Boilerplate-free, type-safe error handling
- Framework-agnostic pure business logic

---

### ⚡ `ai-agent` — Edge AI Inference

Deployed using Cloudflare Workers.

- Built with Hono (TypeScript)
- Vector database integration
- LLaMA model inference
- Processes Item Response Theory (IRT) data
- Generates low-latency contextual hints

---

### 🐳 `infra` — DevOps & Observability

- Fully containerized with Docker
- Docker Compose orchestration
- Prometheus metrics
- Grafana dashboards
- Centralized telemetry & monitoring

---

## 🚀 Getting Started

### 📋 Prerequisites

- JDK 17+
- Docker & Docker Compose
- Node.js & npm (Cloudflare Wrangler)
- Android Studio / Xcode

---

## 🛡 Engineering Principles

- Test-Driven Development (TDD)
- Clean Architecture
- Strict concurrency discipline (Coroutines, Flow, Channels)
- 12-Factor methodology
- Stateless services
- Observability-first design

---

## 🤝 Collaborators

- **Radesh Govind** — Computer Science Engineering Student  
- **Prof. Nuno Miguel Soares Datia (ISEL)** — Project Orientator  
- **U!REKA European Alliance** — Project Sponsor  

---

## 📄 License

This project is developed for academic and research purposes.
