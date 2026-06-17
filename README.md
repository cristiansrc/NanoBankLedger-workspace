# NanoBank Ledger

Sistema de gestión financiera con wallets, transacciones, categorías, autenticación JWT y drag & drop entre wallets.

**Desarrollado por:** Cristhiam Reina (con asistencia de IA)
**Evaluación:** FS-SR-2026-002

---

## 📑 Tabla de Contenido

- [Stack Tecnológico](#stack-tecnológico)
- [Justificación de Base de Datos](#justificación-de-base-de-datos)
- [Requisitos Cumplidos](#requisitos-cumplidos)
- [Uso Estratégico de IA](#uso-estratégico-de-ia)
  - [OpenCode API](#opencode-api)
  - [Ecosistema config-ai](#ecosistema-config-ai)
  - [Ciclo de Vida SDLC](#ciclo-de-vida-sdlc)
  - [Graphify - Grafos de Conocimiento](#graphify---grafos-de-conocimiento)
  - [Agentes Utilizados](#agentes-utilizados)
  - [Skills Utilizados](#skills-utilizados)
- [Bitácora de Prompts](#bitácora-de-prompts)
- [Criterio Senior](#criterio-senior)
- [Arquitectura](#arquitectura)
- [Métricas Finales](#métricas-finales)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Cómo Ejecutar](#cómo-ejecutar)
> **🔗 Navegación del documento:** Este README contiene enlaces a lo largo de todo el documento. Los enlaces locales (ej: `[Stack Tecnológico](#stack-tecnológico)`) te llevan a secciones dentro de este mismo archivo. Los enlaces a archivos del proyecto (ej: `[desarrollo-log.md](./desarrollo-log.md)`) apuntan a artefactos dentro del repositorio. Los enlaces externos (ej: `[config-ai](https://github.com/cristiansrc/config-ai)`) te llevan a recursos fuera del proyecto. Todos los agentes, skills y artefactos mencionados tienen su enlace directo.

---

## Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.4.0 + Kotlin 2.0.21 + Java 21 |
| Frontend | Angular 17.3+ (Standalone Components + Signals) |
| Base de Datos | PostgreSQL 16 con Flyway (ddl-auto=validate) |
| Autenticación | JWT dual (access 15min + refresh 7d con rotación) |
| Infraestructura | Docker Compose (3 servicios: postgres, backend, frontend) |
| Tests Backend | JUnit 5, Mockito, JaCoCo (93.94% cobertura) |
| Tests Frontend | Jasmine, Karma (86.77% cobertura) |

---

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

## Requisitos Cumplidos

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
| Backend: 134 tests, 0 fallos, **93.94% cobertura** (JUnit 5 / Mockito / JaCoCo) | ✅ |
| Frontend: 168 tests, 0 fallos, **86.77% cobertura** (Jasmine / Karma) | ✅ |
| Tests de lógica de saldos y transferencia entre billeteras | ✅ |

#### C. IA Mastery

| Aspecto | Estado |
|---|---|
| Ciclo SDD (Spec-Driven Development) con agentes especializados | ✅ |
| Supervisión humana en gates de validación | ✅ |
| Skills cargados: 23 skills del ecosistema config-ai | ✅ |

---

## Uso Estratégico de IA

Este proyecto fue desarrollado por **Cristhiam Reina** con asistencia estratégica de Inteligencia Artificial, utilizando un ecosistema de agentes especializados y múltiples modelos de lenguaje.

### OpenCode API

Se utilizó [OpenCode](https://opencode.ai) como plataforma de IA multimodelo, que permite acceder a **Claude (Anthropic), ChatGPT (OpenAI), Gemini (Google), Qwen (Alibaba) y DeepSeek** desde una sola interfaz con facturación unificada.

**Modelos utilizados en este proyecto:**
- `opencode-go/deepseek-v4-flash` — Agentes de ejecución, documentación y orquestación
- `opencode-go/qwen3.7-plus` — Agentes de planificación, validación y revisión

### Ecosistema config-ai

El desarrollo se apoyó en [config-ai](https://github.com/cristiansrc/config-ai), un ecosistema de **agentes y skills de IA** creado por Cristhiam Reina para estandarizar y acelerar el desarrollo de software siguiendo el flujo **Spec-Driven Development (SDD)**.

> **Nota:** config-ai es un repositorio independiente que centraliza la configuración de agentes y skills. Puedes consultar su [README completo](https://github.com/cristiansrc/config-ai) para entender la arquitectura completa del ecosistema.

### Ciclo de Vida SDLC

El flujo de desarrollo implementado fue:

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
11. COMMIT          → git-executor               → commits semánticos
```

Cada etapa del flujo utiliza agentes especializados que cargan skills específicos para garantizar calidad y consistencia. Todo el proceso está documentado en [desarrollo-log.md](./desarrollo-log.md).

### Graphify - Grafos de Conocimiento

Durante el desarrollo se utilizó **Graphify**, una herramienta de grafos de conocimiento que permite:

- **Navegación inteligente del código fuente**: Consultas como "encuentra la relación entre el controlador de transacciones y el repositorio JPA" devuelven subgrafos específicos sin necesidad de leer archivos completos.
- **Detección de comunidades**: Identifica agrupaciones lógicas de archivos (por bounded context, por capa arquitectónica) para entender la estructura del proyecto.
- **Actualización automática**: Cada vez que se modifica código, Graphify actualiza el grafo en segundo plano (solo AST, sin costo de API).

Graphify fue particularmente útil para mantener el contexto arquitectónico durante los 6 ciclos de corrección, permitiendo al orquestador entender rápidamente el impacto de cada cambio sin releer toda la base de código.

### Agentes Utilizados

| Agente | Rol |
|---|---|
| [master-orchestrator](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/master-orchestrator.md) | Orquestador principal - coordinó todo el flujo SDD |
| [planner](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/planner.md) | Planificación y diseño técnico |
| [spec-validator](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/spec-validator.md) | Validación de especificaciones |
| [enterprise-architect](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/enterprise-architect.md) | Visión macro del sistema |
| [task-decomposer](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/task-decomposer.md) | Descomposición de tareas |
| [executor](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/executor.md) | Implementación de código backend y frontend |
| [devops-architect](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/devops-architect.md) | Infraestructura Docker |
| [test-architect](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/test-architect.md) | Diseño de pruebas automatizadas |
| [functional-test-planner](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/functional-test-planner.md) | Plan de pruebas funcionales |
| [reviewer](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/reviewer.md) | Revisión de código |
| [documentation](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/documentation.md) | Documentación técnica |
| [git-executor](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/git-executor.md) | Operaciones Git |
| [general-helper](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/general-helper.md) | Soporte operativo |
| [final-validation](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/agents/final-validation.md) | Validación final |

### Skills Utilizados

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
| [git-ops](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/git-ops) | Automatización Git |
| [functional-testing-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/functional-testing-standard) | Pruebas funcionales E2E |
| [design-patterns-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/design-patterns-standard) | Patrones de diseño |
| [enterprise-architecture-standard](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/enterprise-architecture-standard) | Macro-arquitectura |
| [code-review-checklist](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/code-review-checklist) | Revisión de código |
| [pre-flight-check](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/pre-flight-check) | Validación pre-commit |
| [bug-fixing-workflow](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/bug-fixing-workflow) | Protocolo de resolución de errores |
| [graphify](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/graphify) | Grafos de conocimiento para navegación de código |
| [documentation-standards](https://github.com/cristiansrc/config-ai/tree/main/active/opencode/skills/documentation-standards) | Estándares de documentación |

---

## Bitácora de Prompts

> **📖 Para revisar el proceso completo:** El archivo [desarrollo-log.md](./desarrollo-log.md) contiene el registro detallado de todas las interacciones entre el agente `master-orchestrator`, los agentes especializados y Cristhiam Reina. Allí encontrarás el prompt inicial de alineación, cada una de las 44 tareas ejecutadas, los 6 ciclos de corrección y las decisiones tomadas durante todo el desarrollo del MVP.


Registro de los prompts clave utilizados durante el desarrollo del MVP:

| # | Prompt | Agente | Propósito |
|---|---|---|---|---|
| 1 | *"Tengo este PDF con los requerimientos del proyecto. Necesito que analices los requisitos y coordines la ejecución del MVP NanoBank Ledger. Es un sistema de gestión financiera con billeteras, transacciones, categorías, autenticación JWT y drag & drop entre billeteras. El stack es Spring Boot + Kotlin para backend, Angular 17+ para frontend y PostgreSQL como base de datos. Debe tener arquitectura Hexagonal, cobertura de tests >=80% y desplegarse con Docker Compose."* | master-orchestrator | Prompt inicial de Cristhiam Reina al orquestador para iniciar el proyecto |
| 2 | *"Define alignment decisions for NanoBank Ledger MVP considering PostgreSQL vs NoSQL, Hexagonal Architecture, JWT rotation, monorepo structure, API-First, testing strategy, UI framework, deployment and migration strategy."* | enterprise-architect | Alineación inicial del proyecto (9 decisiones locked) |
| 3 | *"Crear la Master Spec para NanoBank Ledger siguiendo el estándar spec-driven-development. Incluir: propósito, stack técnico, arquitectura Hexagonal, modelo de datos (PostgreSQL), reglas de negocio, contratos API (OpenAPI 3.1), seguridad (JWT rotation), estrategia de testing (80% cobertura), despliegue (Docker Compose) y criterios de aceptación."* | planner | Creación de la Master Spec |
| 4 | *"Crear el contrato OpenAPI 3.1 para NanoBank Ledger con 4 tags (Auth, Wallets, Transactions, Categories), 16 endpoints, snake_case, respuestas paginadas, contrato ApiErrorResponse y esquema de seguridad Bearer JWT."* | planner | Contrato OpenAPI |
| 5 | *"Validar el conjunto completo de artefactos de especificación (Master Spec, OpenAPI, Integration Map, Context Map) por ambigüedad, inconsistencia, riesgo arquitectónico y restricciones faltantes. Proporcionar veredicto y hallazgos."* | spec-validator | Validación de especificaciones (3 rondas, 12 findings) |
| 6 | *"Descomponer el incremento MVP en tareas de ingeniería atómicas con dependencias, criterios de aceptación, rutas de archivo y pasos de verificación."* | task-decomposer | Descomposición en 44 tareas |
| 7 | *"Implementar T1-T28: Proyecto Spring Boot 3.4 + Kotlin 2.0 + Java 21 con Arquitectura Hexagonal. Entidades de dominio, puertos de aplicación, adaptadores (JPA, REST, Security), migraciones Flyway, autenticación JWT con rotación, Global Exception Handler."* | executor | Implementación del backend completo |
| 8 | *"Implementar T29-T41: Componentes Angular 17+ standalone con Signals, lazy loading, interceptor de autenticación, guards, CRUD de billeteras, CRUD de transacciones con filtros, drag & drop con optimistic UI, tema minimalist UI."* | executor | Implementación del frontend completo |
| 9 | *"Crear T42-T44: Docker Compose con 3 servicios (postgres:16-alpine, backend Java 21, frontend Nginx), Dockerfiles multi-stage, configuración nginx proxy, scripts de inicialización/reseteo de BD."* | devops-architect | Infraestructura Docker |
| 10 | *"Generar suites de tests para todos los casos de uso del backend y servicios del frontend. Incluir tests unitarios, tests de integración, casos borde (saldo insuficiente, mover a misma wallet, aislamiento de ownership). Mínimo 80% de cobertura."* | test-architect | Generación de tests (302 tests totales) |
| 11 | *"Corregir: El frontend esperaba PaginatedResponse pero el backend retorna un array. Error: Cannot read properties of undefined (reading 'length')."* | executor | Fix response type transactions |
| 12 | *"Implementar persistencia de sesión entre pestañas usando localStorage. Manejar errores 403. Validar expiración JWT al iniciar la aplicación."* | executor | Sesión persistente + validación JWT |
| 13 | *"Rediseñar el layout: sidebar con navegación en desktop, menú hamburguesa en mobile. Mover Cerrar sesión debajo de Transacciones. Alinear la marca con el sidebar."* | executor | Layout responsive con sidebar |

---

## Criterio Senior

Durante el desarrollo, varias sugerencias de la IA fueron modificadas o rechazadas por no cumplir con principios SOLID, ser ineficientes o no alinearse con los requisitos. A continuación se documentan las decisiones más relevantes:

### Decisiones donde se corrigió/modificó a la IA

| # | Sugerencia de la IA | Decisión tomada | Justificación |
|---|---|---|---|
| 1 | **Arquitectura pragmática** (mezclar capas) | **Hexagonal puro** (Ports & Adapters) | Se insistió en separación estricta de capas: dominio puro sin dependencias de frameworks, aplicando SOLID correctamente. El dominio no debe importar Spring, JPA ni HTTP. |
| 2 | **PUT para actualizaciones** | **PATCH** | La IA sugirió PUT para actualizar wallets/transactions. Se corrigió a PATCH por semántica REST correcta (actualización parcial, no reemplazo completo). |
| 3 | **Endpoint `/transfer`** | **`/move`** | La IA propuso `/api/v1/transactions/{id}/transfer`. Se cambió a `/move` por ser el término del lenguaje ubicuo del dominio. |
| 4 | **camelCase en JSON** | **snake_case** | La IA usó camelCase en los DTOs. Se configuró Jackson con `SNAKE_CASE` global y se actualizó todo el frontend para cumplir con el contrato OpenAPI y el estándar REST. |
| 5 | **Soft delete para entidades** | **DELETE físico con CASCADE** | La IA sugirió soft delete con `deleted_at`. Se optó por DELETE físico para simplificar el MVP. Se documentó como decisión a revisar si se requiere auditoría. |
| 6 | **Tokens en localStorage** | **Tokens en Signals (memoria) + localStorage** | La IA sugirió localStorage únicamente. Se implementó almacenamiento dual: Signals para reactividad y localStorage para persistencia entre pestañas, con validación de expiración JWT al cargar. |
| 7 | **Idempotency-Key en transacciones** | **Eliminado** | La IA sugirió header `Idempotency-Key` para POST. Se eliminó por complejidad innecesaria para el MVP; no hay casos de uso que lo requieran. |
| 8 | **CRUD completo de categorías** | **Solo lectura** | La IA generó endpoints POST/PUT/DELETE para categorías. Se corrigió a solo GET, ya que las categorías son predefinidas del sistema (seed data). |
| 9 | **PaginatedResponse en backend** | **Lista plana** | La IA diseñó paginación en el backend pero no la implementó. Se ajustó el frontend para recibir array plano hasta que se implemente paginación real. |
| 10 | **Filtro por type como String** | **Enum TransactionType** | La IA pasaba `type` como String a la JPQL causando error 500. Se corrigió tipando como `TransactionType` enum en toda la cadena: controller → use case → repository. |

### Patrones de diseño aplicados correctamente (sin corrección)

| Patrón | Ubicación | Beneficio |
|---|---|---|
| **Global Exception Handler** | Backend (15 handlers) | Errores consistentes con código estable, trazabilidad vía trace_id |
| **DTOs en todas las capas** | Backend (request/response) | Aislamiento entre dominio y transporte |
| **Optimistic UI** | Frontend (drag & drop) | Feedback instantáneo con rollback automático |
| **Signals + Computed** | Frontend (estado reactivo) | Rendimiento y detección de cambios precisa |
| **JWT Rotation + Theft Detection** | Backend (Auth) | Seguridad: si un refresh token se reusa, toda la familia se invalida |

### Lecciones aprendidas

- **El 100% de los hallazgos del spec-validator (12 findings) fueron corregidos antes de escribir código**, lo que demuestra la efectividad del flujo SDD con gates de validación temprana.
- **3 de los 6 bugs detectados en pruebas fueron causados por desincronización entre capas** (frontend esperando PaginatedResponse, query params en camelCase, type como String vs enum), lo que refuerza la importancia de mantener contratos consistentes.
- **El uso de IA generativa aceleró el desarrollo ~3x**, pero cada sugerencia fue revisada y ajustada con criterio senior, especialmente en decisiones de arquitectura y seguridad.

---

## Arquitectura

- **Hexagonal (Ports & Adapters)** en 4 Bounded Contexts: Auth, Wallets, Transactions, Categories.
- **API-First**: OpenAPI 3.1 contrato con 16 endpoints.
- **Frontend**: Layout responsive con sidebar en desktop, menú hamburguesa en mobile.
- **Sesión persistente** en localStorage con validación de expiración JWT.

---

## Métricas Finales

| Métrica | Backend | Frontend |
|---|---|---|
| Tests | 134 | 168 |
| Fallos | 0 | 0 |
| Cobertura | 93.94% | 86.77% |
| Tareas | 28 (T1-T28) | 13 (T29-T41) |
| Infraestructura | Docker Compose 3 servicios UP | |

---

## Estructura del Proyecto

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
│   └── src/test/                      # Tests (134 tests, 93.94% cobertura)
│
├── NanoBankLedger-frontend/           # Frontend Angular 17+
│   ├── src/app/                       # Código fuente
│   │   ├── core/                      # Servicios, modelos, guards, interceptors
│   │   ├── features/                  # Módulos funcionales (auth, wallets, transactions, dashboard)
│   │   └── shared/                    # Directivas compartidas (drag & drop)
│   └── src/                           # Tests (168 tests, 86.77% cobertura)
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

## Cómo Ejecutar

```bash
cd NanoBankLedger-infrastructure/docker-compose
docker compose up -d
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| Health Check | http://localhost:8080/api/v1/health |
