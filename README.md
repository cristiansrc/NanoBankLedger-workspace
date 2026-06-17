# NanoBank Ledger

Sistema de gestión financiera con wallets, transacciones, categorías, autenticación JWT y drag & drop entre wallets.

**Evaluación:** FS-SR-2026-002

| Aspecto | Detalle |
|---|---|
| **Backend** | Spring Boot 3.4.0 + Kotlin 2.0.21 + Java 21 |
| **Frontend** | Angular 17.3+ (Standalone Components + Signals) |
| **Base de Datos** | PostgreSQL 16 con Flyway (ddl-auto=validate) |
| **Autenticación** | JWT dual (access 15min + refresh 7d con rotación) |
| **Infraestructura** | Docker Compose (3 servicios: postgres, backend, frontend) |
| **Tests Backend** | JUnit 5, Mockito, JaCoCo (94% cobertura) |
| **Tests Frontend** | Jasmine, Karma (86.76% cobertura) |

---

## Requisitos cumplidos

### Requisitos Funcionales

| # | Requisito | Estado | Descripción |
|---|---|---|---|
| 1 | **Dashboard de Billeteras** | ✅ Implementado | CRUD completo de wallets en Angular, vista dashboard con resumen de saldo total, cantidad de billeteras y tipo más usado. Backend con endpoints REST protegidos por JWT. |
| 2 | **Gestión de Transacciones** | ✅ Implementado | Creación, edición, eliminación de transacciones tipo INCOME/EXPENSE por wallet. Actualización atómica de saldos con CHECK constraints en BD. |
| 3 | **Interfaz Dinámica** | ✅ Implementado | Filtros en tiempo real por categoría, tipo y fecha. Drag & Drop entre wallets con optimistic UI y rollback en caso de error. |
| 4 | **Seguridad JWT** | ✅ Implementado | JWT dual con access token (15 min) + refresh token (7 días) con rotación y detección de robo. |

### Requisitos Técnicos

#### A. Arquitectura y Escalabilidad

| Aspecto | Estado |
|---|---|
| SOLID: Arquitectura Hexagonal (Ports & Adapters) con dominio puro | ✅ |
| DTOs en todas las capas de transporte | ✅ |
| Global Exception Handler con 15 handlers y códigos de error estables | ✅ |
| Base de datos PostgreSQL: justificación ACID para transacciones financieras | ✅ |

#### B. Calidad y Pruebas

| Aspecto | Resultado |
|---|---|
| Backend: 134 tests, 0 fallos, **94% cobertura** (JUnit 5 / Mockito / JaCoCo) | ✅ |
| Frontend: 168 tests, 0 fallos, **86.76% cobertura** (Jasmine / Karma) | ✅ |
| Tests de lógica de saldos y transferencia entre billeteras | ✅ |

#### C. IA Mastery

| Aspecto | Estado |
|---|---|
| Ciclo SDD (Spec-Driven Development) con agentes especializados | ✅ |
| Supervisión humana en gates de validación | ✅ |
| Skills cargados: 23 skills del ecosistema config-ai | ✅ |



## Justificación de Base de Datos

### ¿Por qué PostgreSQL?

Se eligió PostgreSQL como motor de base de datos por las siguientes razones:

| Requisito | Beneficio de PostgreSQL |
|---|---|
| **ACID compliance** | Las transacciones financieras requieren atomicidad y consistencia. PostgreSQL garantiza que cada movimiento de dinero (ingreso, gasto, transferencia) se ejecute como una transacción atómica. |
| **CHECK constraints** | Permite validar a nivel de base de datos que `balance >= 0` y `amount > 0`, evitando inconsistencias incluso si hay errores en la lógica de aplicación. |
| **Integridad referencial** | Las claves foráneas entre wallets, transacciones, categorías y usuarios aseguran que no queden huérfanos. |
| **Escalabilidad vertical** | PostgreSQL maneja eficientemente millones de registros con índices adecuados, siendo suficiente para un MVP. Una migración futura a sharding o read replicas es posible si se requiere escalabilidad horizontal. |

### Estructura del modelo de datos

El modelo se diseñó con 4 tablas principales y escalabilidad en mente:

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│   users     │     │    wallets      │     │  transactions    │
├─────────────┤     ├─────────────────┤     ├──────────────────┤
│ id (UUID)   │←────│ user_id (FK)    │←────│ wallet_id (FK)   │
│ name        │     │ id (UUID)       │     │ id (UUID)        │
│ email (UQ)  │     │ name            │     │ amount (NUMERIC) │
│ password_ha │     │ type (VARCHAR)  │     │ type (VARCHAR)   │
│ created_at  │     │ balance(NUMERIC)│     │ category_id(FK)  │
└─────────────┘     │ created_at      │     │ date             │
                    │ updated_at      │     │ description      │
                    └─────────────────┘     │ created_at       │
                                            │ updated_at       │
                                            └──────────────────┘
                                                    │
                                            ┌───────┴────────┐
                                            │   categories   │
                                            ├────────────────┤
                                            │ id (UUID)      │
                                            │ name           │
                                            │ type (VARCHAR) │
                                            │ icon           │
                                            │ color          │
                                            └────────────────┘
```

**Claves de escalabilidad:**
- **UUIDs como identificadores**: Permiten generación distribuida sin conflictos y no exponen IDs secuenciales.
- **NUMERIC(15,2) para montos**: Precisión exacta para operaciones financieras, sin errores de redondeo.
- **Índices por wallet_id + fecha**: Optimizan las consultas de listado de transacciones, que son las más frecuentes.
- **CHECK constraints**: `balance >= 0` y `amount > 0` como red de seguridad a nivel BD.
- **Flyway migrations**: Las migraciones versionadas permiten evolucionar el esquema de forma controlada y reproducible en cualquier entorno.

---

## Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.4.0 + Kotlin 2.0.21 + Java 21 |
| Frontend | Angular 17.3+ (Standalone Components + Signals) |
| Base de Datos | PostgreSQL 16 con Flyway (ddl-auto=validate) |
| Autenticación | JWT dual (access 15min + refresh 7d con rotación) |
| Infraestructura | Docker Compose (3 servicios: postgres, backend, frontend) |
| Tests Backend | JUnit 5, Mockito, JaCoCo (94% cobertura) |
| Tests Frontend | Jasmine, Karma (86.76% cobertura) |
---

## Arquitectura

- **Hexagonal (Ports & Adapters)** en 4 Bounded Contexts: Auth, Wallets, Transactions, Categories.
- **API-First**: OpenAPI 3.1 contrato con 16 endpoints.
- **Frontend**: Layout responsive con sidebar en desktop, menú hamburguesa en mobile.
- **Sesión persistente** en localStorage con validación de expiración JWT.

---

## SDLC Workflow (Spec-Driven Development)

El ciclo de vida se basó en SDD (Spec-Driven Development) utilizando agentes de IA especializados del ecosistema [config-ai](https://github.com/cristiansrc/config-ai):

```
 1. REQUISITOS      → requirements-analyst      → docs/requerimientos.pdf
 2. PLANIFICACIÓN   → planner                    → Master Spec + OpenAPI + Shared Context
 3. VALIDACIÓN      → spec-validator             → 3 rondas, 12 findings corregidos
    ─── GATE HUMANO: Aprobación del plan ───
 4. DESCOMPOSICIÓN  → task-decomposer            → 44 tareas atómicas
 5. EJECUCIÓN BACKEND  → executor (T1-T28)       → 28 tareas
 6. EJECUCIÓN FRONTEND → executor (T29-T41)      → 13 tareas
 7. EJECUCIÓN INFRA    → devops-architect(T42-T44)→ 3 tareas
 8. PRUEBAS         → test-architect             → 302 tests totales
 9. PRUEBAS FUNCIONALES → functional-test-planner → 25 escenarios E2E
10. CORRECCIONES    → bug-fixing-workflow        → 6 ciclos de fixes
    ─── GATE HUMANO: Aprobación QA ───
11. COMMIT          → git-executor               → 3 commits semánticos
```

Flujo completo documentado en [desarrollo-log.md](./desarrollo-log.md).


## Skills utilizados (desde config-ai)

Skills cargados desde [config-ai](https://github.com/cristiansrc/config-ai):

| Skill | Descripción |
|---|---|
| [spec-driven-development](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/spec-driven-development) | Ciclo de vida Master Spec e Incrementos |
| [hexagonal-architecture](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/hexagonal-architecture) | Puertos y Adaptadores |
| [openapi-first](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/openapi-first) | Diseño de APIs basado en contratos |
| [springboot-stack](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/springboot-stack) | Estándares Spring Boot |
| [frontend-architecture](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/frontend-architecture) | Arquitectura limpia para SPAs |
| [flyway-migrations](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/flyway-migrations) | Gestión de esquemas multi-motor |
| [testing-strategy](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/testing-strategy) | Estrategia de pruebas |
| [security-standards](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/security-standards) | JWT, OAuth2, RBAC |
| [minimalist-ui](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/minimalist-ui) | UI minimalista (warm monochrome) |
| [postgresql-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/postgresql-standard) | Estándares PostgreSQL |
| [restful-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/restful-standard) | Convenciones REST |
| [repository-dto-patterns](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/repository-dto-patterns) | Separación de capas |
| [model-tier-routing](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/model-tier-routing) | Política de escalamiento de modelos |
| [context-pinning](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/context-pinning) | Gestión de contexto crítico |
| [documentation-lifecycle](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/documentation-lifecycle) | Ciclo de vida de documentación |
| [documentation-standards](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/documentation-standards) | Estándares de documentación |
| [git-ops](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/git-ops) | Automatización Git |
| [functional-testing-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/functional-testing-standard) | Pruebas funcionales E2E |
| [design-patterns-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/design-patterns-standard) | Patrones de diseño |
| [enterprise-architecture-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/enterprise-architecture-standard) | Macro-arquitectura |
| [code-review-checklist](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/code-review-checklist) | Revisión de código |
| [pre-flight-check](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/pre-flight-check) | Validación pre-commit |
| [bug-fixing-workflow](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/bug-fixing-workflow) | Protocolo de resolución de errores |

---

## Agentes utilizados

Agentes orquestados desde [config-ai](https://github.com/cristiansrc/config-ai):

| Agente | Rol |
|---|---|
| [master-orchestrator](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/master-orchestrator.md) | Orquestador principal |
| [planner](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/planner.md) | Planificación y diseño técnico |
| [executor](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/executor.md) | Implementación de código |
| [test-architect](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/test-architect.md) | Diseño de pruebas automatizadas |
| [devops-architect](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/devops-architect.md) | Infraestructura Docker |
| [documentation](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/documentation.md) | Documentación técnica |
| [git-executor](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/git-executor.md) | Operaciones Git |
| [spec-validator](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/spec-validator.md) | Validación de especificaciones |
| [enterprise-architect](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/enterprise-architect.md) | Visión macro del sistema |
| [reviewer](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/reviewer.md) | Revisión de código |
| [task-decomposer](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/task-decomposer.md) | Descomposición de tareas |
| [functional-test-planner](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/functional-test-planner.md) | Plan de pruebas funcionales |
| [general-helper](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/general-helper.md) | Soporte operativo |
| [final-validation](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/final-validation.md) | Validación final |

---

## Métricas finales

| Métrica | Backend | Frontend |
|---|---|---|
| Tests | 134 | 168 |
| Fallos | 0 | 0 |
| Cobertura | 94% | 86.76% |
| Tareas | 28 (T1-T28) | 13 (T29-T41) |
| Infraestructura | Docker Compose 3 servicios UP | |

---

## Cómo ejecutar

```bash
cd NanoBankLedger-infrastructure/docker-compose
docker compose up -d
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| Health Check | http://localhost:8080/api/v1/health |

---

## Estructura del proyecto

```
NanoBankLedger-workspace/
│
├── docs/                              # Documentación del proyecto
│   ├── specs/                         # Especificaciones SDD
│   │   ├── master-spec.md             # Spec maestro con modelo de datos, RN, contratos
│   │   └── tasks/                     # Tablero de tareas del incremento
│   ├── api/                           # Contrato OpenAPI 3.1
│   │   └── openapi.yaml               # 16 endpoints, 4 tags
│   ├── architecture/                  # Documentación arquitectónica
│   │   ├── system-landscape.md        # C4 Level 1-2
│   │   ├── context-map.md             # 4 Bounded Contexts
│   │   ├── integration-map.md         # Integraciones entre BCs
│   │   └── decision-records/          # ADRs (Monorepo, Hexagonal, JWT)
│   └── functional-testing/            # Pruebas funcionales
│       └── functional-test-plan.md    # 25 escenarios E2E
│
├── NanoBankLedger-backend/            # Backend Spring Boot + Kotlin
│   ├── src/main/kotlin/               # Código fuente (Arquitectura Hexagonal)
│   │   ├── domain/                    # Entidades puras, reglas de negocio
│   │   ├── application/               # Puertos, DTOs, Use Cases
│   │   └── infrastructure/            # REST controllers, JPA, Security, Config
│   ├── src/main/resources/            # Configuración y migraciones Flyway
│   └── src/test/                      # Tests (134 tests, 94% cobertura)
│
├── NanoBankLedger-frontend/           # Frontend Angular 17+
│   ├── src/app/                       # Código fuente
│   │   ├── core/                      # Servicios, modelos, guards, interceptors
│   │   ├── features/                  # Módulos funcionales (auth, wallets, transactions, dashboard)
│   │   └── shared/                    # Directivas compartidas (drag & drop)
│   └── src/                           # Tests (168 tests, 86.76% cobertura)
│
├── NanoBankLedger-infrastructure/     # Infraestructura Docker
│   ├── docker-compose/                # Docker Compose + Nginx config
│   │   ├── docker-compose.yml         # 3 servicios: postgres, backend, frontend
│   │   └── nginx.conf                 # Proxy reverso /api/ → backend
│   └── scripts/                       # Scripts de base de datos
│
├── desarrollo-log.md                  # Bitácora completa de desarrollo
└── README.md                          # Este archivo
```

---

## OpenCode API - Plataforma de IA Multimodelo

Este proyecto fue desarrollado utilizando [OpenCode](https://opencode.ai), una plataforma que unifica múltiples proveedores de IA en un solo lugar.

### Ventajas de OpenCode API

- **Multimodelo**: Acceso a Claude (Anthropic), ChatGPT (OpenAI), Gemini (Google), Qwen (Alibaba) y DeepSeek desde una misma interfaz.
- **Facturación unificada**: Un solo proveedor para todos los modelos, sin necesidad de cuentas separadas.
- **Modelos utilizados en este proyecto**:
  - `opencode-go/deepseek-v4-flash` — Agentes de ejecución, documentación y orquestación
  - `opencode-go/qwen3.7-plus` — Agentes de planificación, validación y revisión

### Ecosistema de Desarrollo (config-ai)

Este proyecto se desarrolló bajo el flujo **Spec-Driven Development (SDD)** utilizando el ecosistema de agentes y skills disponible en [config-ai](https://github.com/cristiansrc/config-ai).

El ciclo de vida fue el siguiente:

1. **Requerimientos** → Se analizó el PDF de requisitos
2. **Planificación** → `planner` creó la Master Spec, contrato OpenAPI y Shared Context
3. **Validación** → `spec-validator` revisó consistencia (3 rondas, 12 findings corregidos)
4. **Aprobación humana** → Gate de validación del plan
5. **Descomposición** → `task-decomposer` generó 44 tareas atómicas
6. **Ejecución** → `executor` implementó backend (T1-T28), frontend (T29-T41) e infraestructura (T42-T44)
7. **Pruebas** → `test-architect` generó 134 tests backend + 168 tests frontend
8. **Correcciones** → Fixes iterativos: snake_case, sesión persistente, filtros, layout responsive
9. **Commit** → `git-executor` realizó los commits semánticos

### Agentes utilizados en esta sesión

Ver sección [Agentes utilizados](#agentes-utilizados) para el listado detallado con enlaces a cada agente.

### Skills utilizados en esta sesión

Ver sección [Skills utilizados](#skills-utilizados-desde-config-ai) para el listado detallado con enlaces a cada skill.
