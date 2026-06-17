# System Landscape — NanoBank Ledger

> **Versión:** 1.0.0
> **Estado:** Accepted
> **Fecha:** 2026-06-16
> **Owner:** Enterprise Architect
> **Última revisión:** 2026-06-16

---

## 1. Propósito del Documento

Este documento define el **System Landscape** del ecosistema NanoBank Ledger utilizando el Modelo C4 (Niveles 1 y 2). Establece la visión macro de arquitectura, los contenedores del sistema, sus responsabilidades, owners y las dependencias principales.

Este documento es la **fuente de verdad** para la arquitectura global del workspace y debe consultarse antes de cualquier decisión que afecte la estructura del sistema.

---

## 2. Visión General del Sistema

**NanoBank Ledger** es un MVP de gestión financiera personal que permite a los usuarios administrar billeteras virtuales, registrar transacciones de ingresos y gastos, categorizar movimientos y visualizar dashboards con filtros en tiempo real. El sistema soporta drag & drop de transacciones entre billeteras con actualización atómica de saldos.

### 2.1 Alcance del Sistema (In-Scope)

| Funcionalidad | Descripción |
|---|---|
| Gestión de Billeteras | Crear, listar, editar y eliminar billeteras virtuales con saldo inicial |
| Gestión de Transacciones | Registrar ingresos y gastos asociados a una billetera |
| Categorización | Clasificar transacciones por categorías predefinidas |
| Dashboard | Visualización de saldos, movimientos recientes y filtros por categoría/fecha |
| Drag & Drop | Mover transacciones entre billeteras con recálculo automático de saldos |
| Autenticación | Registro, login, refresh token rotativo y logout |

### 2.2 Fuera de Alcance (Out-of-Scope)

- Integración con sistemas bancarios externos
- Notificaciones push o email
- Multi-tenancy
- Procesamiento de pagos
- Reportes avanzados o exportación

---

## 3. Modelo C4 — Nivel 1: System Context

### 3.1 Diagrama de Contexto

```
┌─────────────────────────────────────────────────────────────────────┐
│                        NANOBANK LEDGER SYSTEM                       │
│                                                                     │
│  ┌──────────────┐         ┌──────────────────────┐                  │
│  │              │  HTTP   │                      │                  │
│  │   Usuario    │────────▶│   NanoBank Ledger    │                  │
│  │   Final      │  (SPA)  │                      │                  │
│  │              │◀────────│                      │                  │
│  └──────────────┘  JSON   └──────────────────────┘                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

  Actores:
  ┌──────────────────────────────────────────────────────────────────┐
  │ Usuario Final: Persona que usa la aplicación para gestionar     │
  │ sus finanzas personales. Accede vía navegador web.              │
  └──────────────────────────────────────────────────────────────────┘

  Sistemas Externos:
  ┌──────────────────────────────────────────────────────────────────┐
  │ (Ninguno en la fase MVP)                                        │
  └──────────────────────────────────────────────────────────────────┘
```

### 3.2 Actores del Sistema

| Actor | Descripción | Canal | Autenticación |
|---|---|---|---|
| **Usuario Final** | Persona que gestiona sus finanzas personales mediante billeteras virtuales y transacciones | Navegador Web (SPA Angular) | JWT Bearer Token |

### 3.3 Sistemas Externos

| Sistema | Tipo | Propósito | Estado |
|---|---|---|---|
| *(Ninguno)* | — | — | N/A en fase MVP |

---

## 4. Modelo C4 — Nivel 2: Containers

### 4.1 Diagrama de Contenedores

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          NANOBANK LEDGER SYSTEM                             │
│                                                                             │
│  ┌─────────────────────┐     HTTP/JSON      ┌─────────────────────────┐    │
│  │                     │     (REST API)      │                         │    │
│  │  NanoBank Ledger    │────────────────────▶│  NanoBank Ledger        │    │
│  │  Frontend           │     JWT Bearer      │  Backend                │    │
│  │                     │◀────────────────────│                         │    │
│  │  [Angular 17+ SPA]  │                     │  [Spring Boot 3.x]     │    │
│  │                     │                     │  [Kotlin]               │    │
│  └─────────────────────┘                     │  [Hexagonal Arch]      │    │
│         │                                    └───────────┬─────────────┘    │
│         │                                                │                  │
│         │ Navegador Web                                  │ JDBC/JPA         │
│         │ (Chrome, Firefox, Edge)                        │ (HikariCP)       │
│         │                                                │                  │
│         │                                    ┌───────────▼─────────────┐    │
│         │                                    │                         │    │
│         │                                    │  NanoBank Ledger        │    │
│         │                                    │  Database               │    │
│         │                                    │                         │    │
│         │                                    │  [PostgreSQL 16]       │    │
│         │                                    │  [Flyway Migrations]   │    │
│         │                                    │                         │    │
│         │                                    └─────────────────────────┘    │
│         │                                                                   │
│  ┌──────▼──────────────────────────────────────────────────────────────┐    │
│  │  NanoBank Ledger Infrastructure                                     │    │
│  │                                                                     │    │
│  │  [Docker + Docker Compose]                                          │    │
│  │  Orquesta: Backend, Frontend (nginx), PostgreSQL                    │    │
│  │  Scripts de BD, variables de entorno, redes                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Detalle de Contenedores

#### 4.2.1 NanoBank Ledger Frontend

| Atributo | Valor |
|---|---|
| **Tipo** | Single Page Application (SPA) |
| **Tecnología** | Angular 17+, Standalone Components, Signals |
| **Responsabilidad** | Interfaz de usuario: dashboard de billeteras, formularios de transacciones, drag & drop, autenticación visual, filtros en tiempo real |
| **Comunicación** | HTTP/REST → Backend (JSON sobre HTTPS) |
| **Autenticación** | Almacena JWT en memoria (signal), envía Bearer Token en cada request |
| **Despliegue** | Servido por Nginx en contenedor Docker |
| **Owner Funcional** | Frontend Team |
| **Owner Técnico** | Frontend Developer Lead |
| **Ruta en Workspace** | `NanoBankLedger-frontend/` |
| **Razón de Existir** | Proveer la experiencia de usuario interactiva para la gestión financiera. Separa la presentación de la lógica de negocio. |

#### 4.2.2 NanoBank Ledger Backend

| Atributo | Valor |
|---|---|
| **Tipo** | API REST / Servicio de Aplicación |
| **Tecnología** | Spring Boot 3.x, Kotlin, Arquitectura Hexagonal (Ports & Adapters) |
| **Responsabilidad** | Lógica de negocio, validación de dominio, autenticación/autorización JWT, persistencia de datos financieros |
| **Comunicación** | Recibe HTTP/REST del Frontend; envía queries/commands a PostgreSQL vía JDBC/JPA |
| **Autenticación** | Emite y valida JWT (Access Token + Refresh Token rotativo) |
| **Despliegue** | JAR ejecutable en contenedor Docker (JVM con Virtual Threads) |
| **Owner Funcional** | Backend Team |
| **Owner Técnico** | Backend Developer Lead |
| **Ruta en Workspace** | `NanoBankLedger-backend/` |
| **Razón de Existir** | Centraliza la lógica de negocio, garantiza consistencia de datos financieros y expone una API contract-first. |

#### 4.2.3 NanoBank Ledger Database

| Atributo | Valor |
|---|---|
| **Tipo** | Base de Datos Relacional |
| **Tecnología** | PostgreSQL 16 |
| **Responsabilidad** | Almacenamiento persistente de usuarios, billeteras, transacciones, categorías y tokens de refresh |
| **Gestión de Esquema** | Flyway (migraciones versionadas) |
| **Comunicación** | JDBC vía HikariCP connection pool desde el Backend |
| **Owner Funcional** | Backend Team (fuente de verdad de datos) |
| **Owner Técnico** | DBA / Backend Developer Lead |
| **Ruta en Workspace** | `NanoBankLedger-infrastructure/scripts/` (migraciones) |
| **Razón de Existir** | Fuente de verdad única para todos los datos financieros. Garantiza ACID para operaciones de saldo. |

#### 4.2.4 NanoBank Ledger Infrastructure

| Atributo | Valor |
|---|---|
| **Tipo** | Orquestación de Contenedores |
| **Tecnología** | Docker, Docker Compose |
| **Responsabilidad** | Definir y orquestar el entorno de ejecución: redes, volúmenes, variables de entorno, health checks |
| **Comunicación** | Red interna Docker entre contenedores |
| **Owner Funcional** | DevOps / Platform Team |
| **Owner Técnico** | DevOps Engineer |
| **Ruta en Workspace** | `NanoBankLedger-infrastructure/` |
| **Razón de Existir** | Reproducibilidad del entorno, facilitación del desarrollo local y despliegue consistente. |

---

## 5. Matriz de Comunicación entre Contenedores

| Origen | Destino | Protocolo | Formato | Auth | Patrón |
|---|---|---|---|---|---|
| Frontend (Angular) | Backend (Spring Boot) | HTTP/1.1 REST | JSON | JWT Bearer Token | Request-Response (Sync) |
| Backend (Spring Boot) | PostgreSQL | TCP/JDBC | SQL | Credenciales DB | Query-Response (Sync) |
| Infrastructure (Docker) | Todos los contenedores | Docker Network | — | — | Orquestación |

---

## 6. Matriz de Ownership de Datos

| Dato | Fuente de Verdad | Contenedor Owner | Consistencia |
|---|---|---|---|
| Usuarios (credenciales) | PostgreSQL `users` | Backend | Strong (ACID) |
| Refresh Tokens | PostgreSQL `refresh_tokens` | Backend | Strong (ACID) |
| Billeteras | PostgreSQL `wallets` | Backend | Strong (ACID) |
| Transacciones | PostgreSQL `transactions` | Backend | Strong (ACID) |
| Categorías | PostgreSQL `categories` | Backend | Strong (ACID) |
| Estado de UI (sesión) | Navegador (Signals) | Frontend | Eventual (por diseño) |

---

## 7. Cross-Cutting Concerns

### 7.1 Seguridad

| Concern | Estrategia | Owner |
|---|---|---|
| Autenticación | JWT con Access Token (15 min) + Refresh Token (7 días, rotativo) | Backend |
| Autorización | Bearer Token en cada request API | Backend + Frontend |
| Transporte | HTTPS (TLS 1.3 en producción) | Infrastructure |
| Secrets | Variables de entorno en Docker Compose | Infrastructure |
| CORS | Configurado en Backend para origen del Frontend | Backend |

### 7.2 Observabilidad

| Concern | Estrategia | Estado |
|---|---|---|
| Logs | Structured logging (JSON) en Backend | MVP: básico |
| Métricas | Health checks en Backend y DB | MVP: básico |
| Trazabilidad | Correlation ID por request (pendiente de implementación avanzada) | Futuro |

### 7.3 Despliegue

| Environment | Estrategia | Herramienta |
|---|---|---|
| Desarrollo local | Docker Compose con hot-reload | Docker Compose |
| Producción | *(No definido en MVP)* | TBD |

---

## 8. Restricciones y Suposiciones

### 8.1 Restricciones

- El sistema es un MVP; no se requiere alta disponibilidad ni escalado horizontal.
- Un solo usuario por instancia (no multi-tenancy en MVP).
- No hay integración con sistemas bancarios externos.
- La base de datos es PostgreSQL exclusivamente.

### 8.2 Suposiciones

- Los usuarios tienen acceso a un navegador web moderno (Chrome, Firefox, Edge).
- La infraestructura de despliegue soporta Docker y Docker Compose.
- El volumen de datos es bajo (< 100K transacciones por usuario).
- No se requiere procesamiento en tiempo real de eventos externos.

---

## 9. Glosario (Ubiquitous Language Global)

| Término | Definición |
|---|---|
| **Billetera (Wallet)** | Contenedor virtual de fondos con un saldo asociado |
| **Transacción (Transaction)** | Registro de movimiento financiero (ingreso o gasto) en una billetera |
| **Categoría (Category)** | Clasificación asignada a una transacción (ej: alimentación, transporte) |
| **Ingreso (Income)** | Transacción que incrementa el saldo de una billetera |
| **Gasto (Expense)** | Transacción que decrementa el saldo de una billetera |
| **Transferencia (Transfer)** | Movimiento de una transacción de una billetera a otra (drag & drop) |
| **Access Token** | JWT de corta duración (15 min) para autenticar requests |
| **Refresh Token** | Token de larga duración (7 días) con rotación para renovar access tokens |
| **Dashboard** | Vista principal con resumen de billeteras y transacciones recientes |

---

## 10. Evidencia

### Artefactos Creados

| # | Archivo | Ruta Absoluta | Estado |
|---|---|---|---|
| 1 | System Landscape | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/system-landscape.md` | ✅ Creado |
| 2 | Context Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/context-map.md` | ✅ Creado |
| 3 | Integration Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/integration-map.md` | ✅ Creado |
| 4 | ADR-001 Monorepo | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-001-monorepo-structure.md` | ✅ Creado |
| 5 | ADR-002 Hexagonal | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-002-hexagonal-architecture.md` | ✅ Creado |
| 6 | ADR-003 JWT Refresh | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md` | ✅ Creado |

### Referencias a Estructura del Workspace

| Proyecto | Ruta Absoluta | Rol en el System Landscape |
|---|---|---|
| Backend | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-backend/` | API REST + Lógica de Negocio |
| Frontend | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-frontend/` | SPA Angular |
| Infrastructure | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-infrastructure/` | Docker + DB Scripts |
