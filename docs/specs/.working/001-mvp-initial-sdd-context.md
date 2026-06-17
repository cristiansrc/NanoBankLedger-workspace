# Shared Context — 001-mvp-initial

> **Increment:** 001-mvp-initial
> **Lifecycle status:** `implemented`
> **Fecha de creación:** 2026-06-16
> **Última actualización:** 2026-06-16

---

## Current status

`implemented`

MVP completado exitosamente el 2026-06-16. 44/44 tareas implementadas. Backend: 134 tests, 0 fallos, 93.94% cobertura. Frontend: build exitoso, sesión persistente en localStorage, layout responsive con sidebar. Infraestructura Docker: 3/3 contenedores UP. Correcciones: auth guard, migracion V2 ENUMs→VARCHAR, snake_case global, filtro type, categoría opcional, sesión persistente, sidebar + hamburger menu responsive, refresco de saldos.

---

## Canonical artifacts

| # | Artefacto | Ruta Absoluta | Tipo |
|---|---|---|---|
| 1 | Master Spec | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/master-spec.md` | Spec |
| 2 | Shared Context | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/.working/001-mvp-initial-sdd-context.md` | Context |
| 3 | OpenAPI Contract | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/openapi.yaml` | Contract |
| 4 | System Landscape | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/system-landscape.md` | Architecture |
| 5 | Context Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/context-map.md` | Architecture |
| 6 | Integration Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/integration-map.md` | Architecture |
| 7 | ADR-001 Monorepo | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-001-monorepo-structure.md` | ADR |
| 8 | ADR-002 Hexagonal | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-002-hexagonal-architecture.md` | ADR |
| 9 | ADR-003 JWT Refresh | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md` | ADR |
| 10 | Backend Project | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-backend/` | Project |
| 11 | Frontend Project | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-frontend/` | Project |
| 12 | Infrastructure Project | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-infrastructure/` | Project |
| 13 | Task Board | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/tasks/001-mvp-initial-task-board.md` | Task Board (pendiente) |

---

## Artifact evidence

| # | Artefacto | Hallazgo | Estado |
|---|---|---|---|
| 1 | Master Spec | Archivo creado con 12 secciones: propósito, stack, arquitectura, modelo de datos, reglas de negocio, contratos API, seguridad, testing, despliegue, criterios de aceptación, decomposition contract y referencias | `pass` |
| 2 | Shared Context | Este archivo. Contiene todos los headings obligatorios | `pass` |
| 3 | OpenAPI Contract | Archivo YAML creado con 16 endpoints, schemas de error (ApiErrorResponse, ApiErrorDetail), security scheme Bearer JWT, tags por BC, paginación, ejemplos | `pass` |
| 4 | System Landscape | Archivo existente (295 líneas). Define C4 Level 1-2, contenedores, matriz de comunicación, ownership de datos | `pass` |
| 5 | Context Map | Archivo existente (347 líneas). Define 4 BCs (Auth, Wallets, Transactions, Categories), relaciones DDD, lenguaje ubicuo | `pass` |
| 6 | Integration Map | Archivo existente (482 líneas). Define 4 integraciones (INT-001 a INT-004), flujos detallados, resiliencia | `pass` |
| 7 | ADR-001 | Archivo existente (195 líneas). Monorepo con 3 proyectos hermanos | `pass` |
| 8 | ADR-002 | Archivo existente (299 líneas). Arquitectura Hexagonal con 4 BCs como hexágonos independientes | `pass` |
| 9 | ADR-003 | Archivo existente (335 líneas). JWT dual con refresh token rotativo y detección de robo | `pass` |
| 10 | Backend Project | Directorio existente. Sin código implementado aún (MVP greenfield) | `pass` |
| 11 | Frontend Project | Directorio existente. MVP completo: 13 tareas (T29-T41) implementadas, 168 tests, 86.77% cobertura. Auth guard fix aplicado: rutas protegidas con canActivate, raíz y wildcard redirigen a /auth/login. | `pass` |
| 12 | Infrastructure Project | Directorio existente. Docker completado: Dockerfiles multi-stage, docker-compose con 3 servicios (postgres, backend, frontend), nginx.conf, scripts init-db/reset-db. 3/3 contenedores UP. | `pass` |
| 13 | Task Board | Archivo existente. 44/44 tareas completadas. | `pass` |
| 14 | Migraciones Flyway | V1__initial_schema.sql y V2__fix_enum_columns.sql existentes. V2 corrige ENUMs nativos a VARCHAR. | `pass` |
| 15 | Runtime OpenAPI copy | No existe aún (`src/main/resources/openapi.yaml`). Se sincronizará durante implementación | `not applicable` |

---

## Spec Validator Approval

```
verdict: ready
reviewed_at: 2026-06-16T19:30:00Z
validator_agent: spec-validator
artifact_set_reviewed:
  - /home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/master-spec.md
  - /home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/.working/001-mvp-initial-sdd-context.md
  - /home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/openapi.yaml
  - /home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/integration-map.md
  - /home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/context-map.md
summary: "Ronda 3: Los 12 findings (F-001 a F-012) corregidos. F-001: Integration Map L76-77 ahora tiene paths correctos /api/v1/wallets/{walletId}/transactions. F-012: Context Map L165-166 ahora define montos siempre positivos alineados con RN-003. Todos los artefactos son consistentes cross-artefacto y estan listos para descomposicion."
invalidated_by_changes_since: none
```

---

## Decisions locked

| # | Decisión | Fuente | Justificación |
|---|---|---|---|
| D-001 | Monorepo con 3 proyectos hermanos | ADR-001 | Equipo pequeño, MVP, alta cohesión |
| D-002 | Arquitectura Hexagonal (Ports & Adapters) | ADR-002 | Dominio financiero requiere separación estricta |
| D-003 | 4 Bounded Contexts: Auth, Wallets, Transactions, Categories | Context Map | Separación de responsabilidades por dominio |
| D-004 | JWT dual: Access Token 15min + Refresh Token 7d con rotación | ADR-003 | Seguridad para datos financieros + UX fluida |
| D-005 | API-First con OpenAPI 3.1 como fuente de verdad de diseño | Master Spec §3.3 | Contract-first para alinear Frontend y Backend |
| D-006 | PostgreSQL 16 con Flyway para migraciones | System Landscape | ACID para operaciones financieras |
| D-007 | `snake_case` para todos los campos JSON | Master Spec §6.3, restful-standard | Consistencia con estándar REST |
| D-008 | Error contract: `ApiErrorResponse` con `code` estable | springboot-kotlin-rest-error-response-standards | Código estable para frontend/i18n |
| D-009 | Paginación 0-indexed con `page`, `size`, `total_elements`, `total_pages`, `content` | restful-standard | Estándar REST para paginación |
| D-010 | UUIDs como identificadores públicos | ADR-002, PostgreSQL standard | Descentralización, seguridad (no exponer IDs secuenciales) |
| D-011 | BCrypt cost 12 para password hashing | ADR-003, security-standards | Balance seguridad/performance |
| D-012 | Soft delete no aplicado en MVP (DELETE físico con CASCADE) | Master Spec §4.3-4.5 | Simplificación para MVP; revisar si se necesita auditoría |
| D-013 | `PATCH` para actualizaciones parciales (no `PUT`) | Master Spec §6, restful-standard | Semántica REST correcta para partial updates |
| D-014 | Endpoint de drag & drop: `PATCH /api/v1/transactions/{id}/move` | Master Spec §6.1 | El usuario prefiere `/move` sobre `/transfer` |
| D-015 | Transacciones anidadas bajo wallets: `/wallets/{walletId}/transactions` | Master Spec §6.1 | Expresa pertenencia real (transacción pertenece a wallet) |
| D-016 | Cobertura mínima 80% global, 85% por archivo testable | springboot-stack, Master Spec §8 | Calidad de código garantizada |
| D-017 | Docker Compose para desarrollo local | System Landscape §7.3 | Reproducibilidad del entorno |
| D-018 | `ddl-auto=validate` — Flyway es dueño del esquema | flyway-migrations, Master Spec §9.2 | Integridad del esquema |
| D-019 | Tokens almacenados en Signals (memoria), no en localStorage | ADR-003, security-standards | Inmunidad parcial a XSS |
| D-020 | Saldo de wallet como `NUMERIC(15,2)` con CHECK `>= 0` | Master Spec §4.3, RN-001 | Precisión financiera + constraint de no-negatividad |

---

## Validator findings

| ID | Severidad | Artefacto | Descripcion | Reporte |
|---|---|---|---|---|
| (sin findings pendientes) | — | — | Todos los findings fueron resueltos | — |

---

## Resolved findings

| ID | Severidad | Artefacto | Correccion aplicada |
|---|---|---|---|
| F-001 | BLOCKER → HIGH (resuelto) | Integration Map | Corregidos 12 de 12 sub-issues: PUT→PATCH, /transfer→/move, camelCase→snake_case, error contract, Idempotency-Key eliminado, POST/PUT/DELETE categories eliminados, FK user_id en categories eliminada, soft delete→fisico, paths L76-77 corregidos a `/api/v1/wallets/{walletId}/transactions` |
| F-001 (parcial) | BLOCKER → HIGH | Integration Map | Corregidos 10 de 12 sub-issues: PUT→PATCH, /transfer→/move, camelCase→snake_case, error contract, Idempotency-Key eliminado, POST/PUT/DELETE categories eliminados, FK user_id en categories eliminada, soft delete→fisico. **Pendiente:** lineas 76-77 aun tienen paths `/api/v1/transactions` en lugar de `/api/v1/wallets/{walletId}/transactions` |
| F-002 | HIGH | Context Map | Soft delete reemplazado por DELETE fisico en L125, L137, L178, L189 |
| F-003 | HIGH | Context Map | Categorias personalizadas eliminadas; BC Categories acotado a solo lectura (ListCategories) en L207, L224-225, L231 |
| F-004 | HIGH | Master Spec | Agregado `401` en todos los 12 endpoints protegidos en §6.1 (L346-367) |
| F-005 | HIGH | OpenAPI | Pattern de amount corregido a `^(?!0+(\.0{1,2})?$)\d+(\.\d{1,2})?$` + `minimum: 0.01` en CreateTransactionRequest (L1198-1199) y UpdateTransactionRequest (L1224-1225) |
| F-006 | HIGH | Master Spec + ADR-003 | Agregadas columnas `issued_at` y `updated_at` en §4.2 RefreshToken (L181, L186), diagrama ER (L250, L255) y Decomposition Contract §11.3 (L670) |
| F-007 | HIGH | Integration Map | FK user_id eliminada de categories en L160: `categories | Categories | — | Strong` |
| F-008 | MEDIUM | Shared Context | Termino `validated-not-executed` reemplazado por `awaiting-human-plan-approval` en L54 |
| F-009 | MEDIUM | OpenAPI | Schema independiente `CategoryType` creado (L1328-1334); `CategoryResponse.type` ahora referencia `CategoryType` (L1349) |
| F-010 | MEDIUM | Master Spec | `PaginatedResponse<T>` generico reemplazado por `PaginatedTransactionResponse` especifico en §11.2 (L661) |
| F-011 | LOW | Master Spec | Nota explicita agregada en §4.3 (L206) indicando que `initial_balance` es opcional con default `"0.00"` |
| F-012 | HIGH | Context Map | Lenguaje ubicuo de "Monto" (L165-166) corregido: ahora define montos siempre positivos, alineado con RN-003 y CHECK constraint `amount > 0` |

---

## Open questions

| # | Pregunta | Impacto | Estado | Decisión temporal |
|---|---|---|---|---|
| OQ-001 | ¿Cómo se manejan wallets con múltiples monedas? | Modelo de datos: actualmente una sola moneda implícita por wallet | Abierto | MVP: moneda única implícita (USD). Se agregará campo `currency` si se requiere multi-moneda |
| OQ-002 | ¿Hay límite de transacciones por página? | Paginación | Resuelto | Sí: `size` default 20, máximo 100. Ver OpenAPI y Master Spec §6.2 |
| OQ-003 | ¿Se necesitan categorías personalizadas por usuario en MVP? | BC Categories: actualmente solo categorías del sistema | Abierto | MVP: solo categorías del sistema (predefinidas). Se evaluará agregar categorías personalizadas en incremento posterior |
| OQ-004 | ¿Se requiere soft delete para entidades de negocio? | Modelo de datos: actualmente DELETE físico con CASCADE | Abierto | MVP: DELETE físico. Si se requiere auditoría, se agregará `deleted_at` en incremento posterior |
| OQ-005 | ¿Cómo se maneja la concurrencia en transferencias simultáneas sobre la misma wallet? | Consistencia de saldos | Abierto | MVP: `@Transactional` con `READ_COMMITTED` + CHECK constraint `balance >= 0`. Si se detectan problemas de concurrencia, se evaluará `SELECT FOR UPDATE` o optimistic locking |
| OQ-006 | ¿Se requiere rate limiting en MVP? | Seguridad | Abierto | MVP: rate limiting básico en `/auth/login` (5 intentos/minuto/IP). Se evaluará agregar rate limiting global si se requiere |

---

## Stale terms guard

Los siguientes términos NO deben usarse en specs, código, OpenAPI ni comunicación técnica de este incremento:

| Término prohibido | Razón | Reemplazo correcto |
|---|---|---|
| `account` | No es cuenta bancaria; es billetera virtual | `wallet` |
| `movement` | No es término del dominio | `transaction` |
| `/transfer` (como endpoint path) | El endpoint usa `/move` | `/move` |
| `targetWalletId` (camelCase en JSON) | La API usa `snake_case` | `target_wallet_id` |
| `error_code` | El error contract usa `code` | `code` |
| `success` (flag en response) | El status HTTP indica éxito/error; no se usa wrapper | Retornar recurso directamente |
| `data` (wrapper en response) | No se usa envelope/wrapper | Retornar recurso directamente |
| `PUT` (para actualizaciones parciales) | La semántica correcta es PATCH | `PATCH` |
| `session` (como sesión HTTP) | No hay sesiones server-side; es un período de actividad definido por refresh token | `sesión` (en lenguaje ubicuo de Auth) |
| `balance` como `DOUBLE PRECISION` | Se usa NUMERIC para precisión financiera | `NUMERIC(15,2)` |
| `id` como `SERIAL`/`BIGSERIAL` | Se usa UUID para identificadores públicos | `UUID DEFAULT gen_random_uuid()` |
| `ddl-auto=update` | Flyway es dueño del esquema | `ddl-auto=validate` |
| `localStorage` (para tokens) | Los tokens van en Signals (memoria) | `Signal` (Angular) |

## Human Plan Approval

```
approved_by_user: true
approved_at: 2026-06-16
approved_by: Cristian (usuario)
note: Plan aprobado. Orden de ejecución: backend → frontend → infraestructura.
```

---

## Next action

MVP completado. 44/44 tareas ejecutadas. Pendiente: revision humana del incremento completo, aprobacion de cierre, y planificacion del proximo incremento.

---

## Change log

| Fecha | Autor | Cambio |
|---|---|---|
| 2026-06-16 | Planner | Creación inicial del shared context para incremento 001-mvp-initial |
| 2026-06-16 | spec-validator | Primera validacion: verdict `not ready`. 11 findings (1 BLOCKER, 6 HIGH, 3 MEDIUM, 1 LOW). Estado cambiado a `revision-needed`. Ver `001-mvp-initial-validator-report.md` |
| 2026-06-16 | Planner | Correccion de findings F-004, F-005, F-006, F-008, F-009, F-010, F-011. 7 findings resueltos (3 HIGH, 3 MEDIUM, 1 LOW). Pendiente: F-001 (BLOCKER), F-002 (HIGH), F-003 (HIGH), F-007 (HIGH) |
| 2026-06-16 | Enterprise Architect + Planner | Correccion de findings F-001 (parcial), F-002, F-003, F-007. Integration Map reconciliado (PUT→PATCH, /transfer→/move, snake_case, error contract, categories solo GET, sin FK user_id). Context Map corregido (DELETE fisico, categorias solo lectura) |
| 2026-06-16 | Planner | Correccion de findings F-001 (remanente) y F-012. Integration Map L76-77 corregido con paths `/api/v1/wallets/{walletId}/transactions`. Context Map L165-166 corregido con lenguaje ubicuo de montos siempre positivos |
| 2026-06-16 | spec-validator | Validacion final (ronda 3): verdict `ready`. Los 12 findings (F-001 a F-012) corregidos. Estado cambiado a `awaiting-human-plan-approval`. Pendiente aprobacion humana del plan |
| 2026-06-16 | spec-validator | Re-validacion (ronda 2): verdict `not ready`. 10 de 11 findings originales corregidos. Persisten 2 findings HIGH: F-001 remanente (Integration Map L76-77 paths incorrectos) y F-012 nuevo (Context Map L165-166 monto negativo contradice RN-003) |
| 2026-06-16 | executor | Infraestructura Docker completada (T42-T44). 3 contenedores UP. Dockerfiles multi-stage, docker-compose, nginx.conf, scripts init-db/reset-db creados. |
| 2026-06-16 | executor | Frontend fix: authGuard aplicado a rutas protegidas en app.routes.ts. Raíz y wildcard redirigen a /auth/login (no /dashboard). |
| 2026-06-16 | executor | Flyway V2__fix_enum_columns.sql creado. Columnas ENUM nativas convertidas a VARCHAR (wallet_type→VARCHAR(20), transaction_type→VARCHAR(10), category_type→VARCHAR(10)). POST/PUT wallets funciona sin error 500. |
| 2026-06-16 | test-architect | Functional test plan creado (docs/functional-testing/functional-test-plan.md). 25 escenarios (TS_001 a TS_025). |
| 2026-06-16 | executor | Fix snake_case global (D-007). Jackson property-naming-strategy SNAKE_CASE en backend. Modelos, servicios, componentes y tests frontend actualizados para snake_case. `initial_balance` funciona correctamente. |
| 2026-06-16 | executor | Fix response type transacciones: backend retorna array, no paginated. Eliminado error `Cannot read properties of undefined (reading 'length')`. |
| 2026-06-16 | executor | Sesión persistente en localStorage. Tokens sobreviven entre pestañas. Interceptor maneja 401+403 con redirección a login. |
| 2026-06-16 | executor | Refresco de saldos de wallets después de crear/eliminar/mover transacciones. Ocultado balance del selector de billetera. |
| 2026-06-16 | executor | Wallet ID desde ruta URL: TransactionListComponent lee `:id` de `/wallets/{id}/transactions`. Cada billetera muestra sus transacciones. |
| 2026-06-16 | executor | Rediseño layout: sidebar izquierdo (Dashboard, Billeteras, Transacciones, Cerrar sesión) en desktop. Menú hamburguesa con dropdown en mobile. Navbar superior con brand y username. Responsive <768px. |
| 2026-06-17 | executor | Fix query params snake_case en controller (category_id, date_from, date_to). Fix error 500 en filtro de fechas con COALESCE. |
| 2026-06-17 | executor | Validación de token JWT al iniciar sesión: isTokenExpired() decodifica payload y verifica exp. Navbar no se muestra con token vencido. |
| 2026-06-17 | executor | Layout: navbar alineado con sidebar (220px). Cerrar sesión movido debajo de Transacciones en sidebar. |
| 2026-06-17 | executor | Fix iconos billeteras (emoji text). Error 409 de eliminación se muestra en UI. Categorías sin iconos ni círculos de colores. |
