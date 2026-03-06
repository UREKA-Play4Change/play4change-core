<div align="center">
<img width="419" height="120" alt="image" src="https://github.com/user-attachments/assets/4e7d3235-673f-4b6c-b761-df755dc6d46d" />


# Play4Change

**A Multiplatform Gamified Educational Platform for Sustainability and Digital Education**

[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Mistral AI](https://img.shields.io/badge/AI-Mistral%20API-FF7000?logo=ai&logoColor=white)](https://mistral.ai)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![ISEL](https://img.shields.io/badge/ISEL-Engenharia%20Informática-red)](https://www.isel.pt)

> *Serious games that make sustainability and digital literacy measurable, engaging, and scalable — powered by AI-generated daily challenges.*

</div>

---

## 🎯 Overview

U!REKA Play4Change is a **multiplatform serious game** in which players receive AI-generated daily challenges aligned with **sustainability** and **digital education** themes. Players earn points and badges for completing tasks, and lose points for inactivity — creating a powerful engagement loop grounded in established game-design research.

The platform is designed to be **fully configurable**: subject domains, difficulty levels, and audience profiles are driven by configuration rather than code, making the system reusable across the entire U!REKA institution network and beyond.

### Core Engineering Goals

| Goal | Approach |
|------|----------|
| 📱 Multiplatform delivery | Kotlin Multiplatform + Compose Multiplatform (Android & iOS) |
| 🤖 Adaptive AI content | LangChain4j + Mistral API (EU-hosted, GDPR-compliant) |
| 🎮 Sustained engagement | Server-side scoring engine with rewards & penalties |
| ⚡ Zero inference latency | Nightly batch pre-generation + offline-first client sync |

---

## 🏗️ Architecture

> See the Wiki page for the full annotated diagram and design decisions.

The system follows a **clean architecture** pattern on both client and server:

- **Client** — Kotlin Multiplatform with Compose Multiplatform UI; Decompose for navigation; Koin for dependency injection. Offline-first with background sync.
- **Server** — Spring Boot RESTful API with layered architecture (Web → Services → Repository → Domain). Spring `@Scheduled` drives nightly AI batch jobs.
- **Database** — PostgreSQL for persistent state; `pgvector` extension for semantic deduplication of AI-generated tasks.
- **AI Pipeline** — LangChain4j orchestrates task generation via the EU-hosted Mistral API. All generated content is schema-validated before storage.
- **Observability** — Prometheus metrics exposed via Spring Boot Actuator, visualised in Grafana.

---

## 🛠️ Tech Stack

### Client
| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform |
| Navigation | Decompose |
| DI | Koin |
| Networking | Ktor Client |
| Targets | Android (primary), iOS (extended) |

### Server
| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.x |
| Language | Kotlin (JVM) |
| Persistence | PostgreSQL + pgvector |
| AI Orchestration | LangChain4j |
| LLM Provider | Mistral AI (EU-hosted) |
| Scheduling | Spring `@Scheduled` |
| Observability | Prometheus + Grafana |
| Containerisation | Docker Compose |

---

## 🎓 Academic Context

This project is developed as part of the **Projecto e Seminário** course unit in the **Licenciatura em Engenharia Informática e de Computadores** at [ISEL – Instituto Superior de Engenharia de Lisboa](https://www.isel.pt).

**Author:** Radesh Ilesh Gamanbhai Govind (A51620)  
**Supervisors:**
- Prof. Nuno Miguel Soares Datia — [nuno.datia@isel.pt](mailto:nuno.datia@isel.pt)
- Prof. António Serrador — [antonio.serrador@isel.pt](mailto:antonio.serrador@isel.pt)
- Prof. Michel Vorenhout
