# Integration Map — NanoBank Ledger

> **Versión:** 1.0.0
> **Estado:** Accepted
> **Fecha:** 2026-06-16
> **Owner:** Enterprise Architect
> **Última revisión:** 2026-06-16

---

## 1. Propósito del Documento

Este documento define todas las **integraciones** entre contenedores y bounded contexts del sistema NanoBank Ledger. Para cada integración se especifica: tipo (sync/async), protocolo, autenticación, timeouts, idempotencia, contratos y flujos detallados.

Este documento complementa al `system-landscape.md` (visión macro) y al `context-map.md` (boundaries DDD).

---

## 2. Resumen de Integraciones

| # | Integración | Tipo | Protocolo | Auth | Dirección |
|---|---|---|---|---|---|
| INT-001 | Frontend ↔ Backend API | Sync | HTTP/REST + JSON | JWT Bearer | Bidireccional (request-response) |
| INT-002 | Backend ↔ PostgreSQL | Sync | JDBC/JPA + SQL | Credenciales DB | Backend → Database |
| INT-003 | Drag & Drop Flow (End-to-End) | Sync | HTTP PATCH → JDBC transaccional | JWT Bearer | Frontend → Backend → Database |
| INT-004 | Auth Flow (Login/Refresh) | Sync | HTTP POST → JDBC | Credenciales DB | Frontend → Backend → Database |

---

## 3. INT-001: Frontend ↔ Backend API

### 3.1 Descripción

El Frontend (Angular SPA) se comunica con el Backend (Spring Boot) exclusivamente vía HTTP/REST con payloads JSON. Todas las APIs protegidas requieren un JWT Bearer Token en el header `Authorization`.

### 3.2 Especificación Técnica

| Atributo | Valor |
|---|---|
| **Tipo** | Sync — Request-Response |
| **Protocolo** | HTTP/1.1 (upgradeable a HTTP/2) |
| **Formato** | JSON (`Content-Type: application/json`) |
| **Auth** | JWT Bearer Token (`Authorization: Bearer <access_token>`) |
| **Contract** | OpenAPI 3.1 (contract-first) |
| **Timeout sugerido** | 30 segundos (general), 60 segundos (operaciones de transferencia) |
| **Retry Policy** | Frontend: máximo 2 reintentos con backoff exponencial para errores 5xx y timeouts. No reintentar 4xx. |
| **Idempotencia** | GET, PATCH, DELETE son idempotentes por diseño. POST no es idempotente (creación de recursos). |
| **CORS** | Backend permite origen del Frontend (`Access-Control-Allow-Origin`) |
| **Compresión** | gzip para responses > 1KB |

### 3.3 Endpoints por Bounded Context

#### Auth Endpoints

| Método | Endpoint | Descripción | Auth | Idempotente |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | Registro de usuario | No | Sí (email único) |
| POST | `/api/v1/auth/login` | Autenticación | No | Sí (mismas credenciales) |
| POST | `/api/v1/auth/refresh` | Renovar access token | Refresh Token | No (rotación) |
| POST | `/api/v1/auth/logout` | Invalidar refresh token | JWT Bearer | Sí |

#### Wallets Endpoints

| Método | Endpoint | Descripción | Auth | Idempotente |
|---|---|---|---|---|
| POST | `/api/v1/wallets` | Crear billetera | JWT Bearer | Sí (nombre único por usuario) |
| GET | `/api/v1/wallets` | Listar billeteras | JWT Bearer | Sí (GET) |
| GET | `/api/v1/wallets/{id}` | Obtener billetera | JWT Bearer | Sí (GET) |
| PATCH | `/api/v1/wallets/{id}` | Actualizar billetera | JWT Bearer | Sí (PATCH) |
| DELETE | `/api/v1/wallets/{id}` | Eliminar billetera | JWT Bearer | Sí (DELETE) |

#### Transactions Endpoints

| Método | Endpoint | Descripción | Auth | Idempotente |
|---|---|---|---|---|
| POST | `/api/v1/wallets/{walletId}/transactions` | Crear transacción | JWT Bearer | No |
| GET | `/api/v1/wallets/{walletId}/transactions` | Listar transacciones (con filtros) | JWT Bearer | Sí (GET) |
| GET | `/api/v1/transactions/{id}` | Obtener transacción | JWT Bearer | Sí (GET) |
| PATCH | `/api/v1/transactions/{id}` | Actualizar transacción | JWT Bearer | Sí (PATCH) |
| DELETE | `/api/v1/transactions/{id}` | Eliminar transacción | JWT Bearer | Sí (DELETE) |
| PATCH | `/api/v1/transactions/{id}/move` | Mover transacción entre billeteras (drag & drop) | JWT Bearer | Sí (transaction_id) |

#### Categories Endpoints

| Método | Endpoint | Descripción | Auth | Idempotente |
|---|---|---|---|---|
| GET | `/api/v1/categories` | Listar categorías | JWT Bearer | Sí (GET) |

### 3.4 Error Contract

Todos los errores siguen un formato estándar:

```json
{
  "timestamp": "2026-06-16T10:30:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "WALLET_INSUFFICIENT_BALANCE",
  "message": "La billetera no tiene saldo suficiente para esta operación.",
  "path": "/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/transactions",
  "trace_id": "abc-123-def-456",
  "details": []
}
```

### 3.5 JWT Token Lifecycle

```
┌──────────────┐     POST /auth/login      ┌──────────────┐
│              │ ─────────────────────────▶ │              │
│   Frontend   │ ◀───────────────────────── │   Backend    │
│   (Angular)  │   { access_token,          │              │
│              │     refresh_token }        │              │
│              │                            │              │
│  [Signal:    │     GET /wallets           │  [JWT        │
│   authToken] │ ──── Authorization: ─────▶ │   Filter]    │
│              │     Bearer <access_token>  │              │
│              │ ◀───────────────────────── │              │
│              │   [wallets data]           │              │
│              │                            │              │
│              │     POST /auth/refresh     │              │
│              │ ──── { refresh_token } ──▶ │              │
│              │ ◀───────────────────────── │              │
│              │   { new_access_token,      │              │
│              │     new_refresh_token }    │              │
└──────────────┘                            └──────────────┘
```

---

## 4. INT-002: Backend ↔ PostgreSQL

### 4.1 Descripción

El Backend se comunica con PostgreSQL mediante JDBC a través de HikariCP connection pool y JPA/Hibernate como ORM. El esquema de base de datos es gestionado exclusivamente por migraciones Flyway.

### 4.2 Especificación Técnica

| Atributo | Valor |
|---|---|
| **Tipo** | Sync — Query-Response |
| **Protocolo** | TCP/IP (PostgreSQL Wire Protocol) |
| **Driver** | JDBC (PostgreSQL JDBC Driver) |
| **ORM** | JPA/Hibernate (Spring Data JPA) |
| **Connection Pool** | HikariCP (default: 10 conexiones, max: 20) |
| **Auth** | Credenciales DB (usuario/password vía variables de entorno) |
| **Timeout sugerido** | 30 segundos (query timeout), 5 segundos (connection timeout) |
| **Migraciones** | Flyway (versionadas, ejecutadas al inicio del Backend) |
| **Transacciones** | Spring `@Transactional` con isolation level READ_COMMITTED |
| **Consistencia** | Strong (ACID) — todas las operaciones financieras son transaccionales |

### 4.3 Esquema de Base de Datos (Propiedad por Bounded Context)

| Tabla | Bounded Context Owner | Clave Foránea | Consistencia |
|---|---|---|---|
| `users` | Auth | — | Strong |
| `refresh_tokens` | Auth | `user_id → users.id` | Strong |
| `wallets` | Wallets | `user_id → users.id` | Strong |
| `transactions` | Transactions | `wallet_id → wallets.id`, `category_id → categories.id` | Strong |
| `categories` | Categories | — | Strong |

### 4.4 Reglas de Acceso

| Regla | Justificación |
|---|---|
| Solo el Backend escribe en la BD | El Frontend nunca accede directamente a PostgreSQL |
| Cada BC accede solo a sus tablas | Wallets no lee `transactions` directamente; usa puertos internos |
| Las migraciones Flyway son inmutables | Una vez aplicada, una migración no se modifica; se crean nuevas |
| DELETE físico con CASCADE para wallets y transactions | D-012 y RN-017: eliminación real con CASCADE en BD; no soft delete |

---

## 5. INT-003: Drag & Drop Flow (End-to-End)

### 5.1 Descripción

Este es el flujo más complejo del sistema. Cuando un usuario arrastra una transacción de una billetera a otra en el Frontend, se desencadena una secuencia de operaciones que debe garantizar consistencia de saldos en ambas billeteras.

### 5.2 Flujo Detallado

```
┌──────────────┐                    ┌──────────────┐                    ┌──────────────┐
│   Frontend   │                    │   Backend    │                    │  PostgreSQL  │
│   (Angular)  │                    │ (Spring Boot)│                    │              │
│              │                    │              │                    │              │
│  1. Usuario  │                    │              │                    │              │
│  drag & drop │                    │              │                    │              │
│  transacción │                    │              │                    │              │
│  de Wallet A │                    │              │                    │              │
│  a Wallet B  │                    │              │                    │              │
│              │                    │              │                    │              │
│  2. Signal   │                    │              │                    │              │
│  update:     │                    │              │                    │              │
│  optimistic  │                    │              │                    │              │
│  UI move     │                    │              │                    │              │
│              │                    │              │                    │              │
│  3. PATCH    │                    │              │                    │              │
│  /api/v1/    │                    │              │                    │              │
│  transactions│                    │              │                    │              │
│  /{id}/      │───────────────────▶│              │                    │              │
│  move        │  { target_wallet_id } │  4. Validar  │                    │              │
│              │                    │  JWT +       │                    │              │
│              │                    │  permisos    │                    │              │
│              │                    │              │                    │              │
│              │                    │  5. BEGIN    │                    │              │
│              │                    │  TX          │───────────────────▶│              │
│              │                    │              │                    │              │
│              │                    │  6. Mover    │                    │              │
│              │                    │  transacción │  UPDATE transactions│             │
│              │                    │  (wallet_id  │  SET wallet_id = B │              │
│              │                    │   = B)       │  WHERE id = TX_ID  │              │
│              │                    │              │───────────────────▶│              │
│              │                    │              │                    │              │
│              │                    │  7. Ajustar  │                    │              │
│              │                    │  saldo       │  UPDATE wallets    │              │
│              │                    │  Wallet A    │  SET balance =     │              │
│              │                    │  (-amount)   │  balance - amount  │              │
│              │                    │              │  WHERE id = A      │              │
│              │                    │              │───────────────────▶│              │
│              │                    │              │                    │              │
│              │                    │  8. Ajustar  │                    │              │
│              │                    │  saldo       │  UPDATE wallets    │              │
│              │                    │  Wallet B    │  SET balance =     │              │
│              │                    │  (+amount)   │  balance + amount  │              │
│              │                    │              │  WHERE id = B      │              │
│              │                    │              │───────────────────▶│              │
│              │                    │              │                    │              │
│              │                    │  9. COMMIT   │                    │              │
│              │                    │  TX          │───────────────────▶│              │
│              │                    │              │                    │              │
│  10. Response│                    │              │                    │              │
│  200 OK     │◀───────────────────│              │                    │              │
│  { updated  │  { transaction,    │              │                    │              │
│  transaction,│    walletA,       │              │                    │              │
│  wallets }  │    walletB }       │              │                    │              │
│              │                    │              │                    │              │
│  11. Signal  │                    │              │                    │              │
│  update:     │                    │              │                    │              │
│  confirm     │                    │              │                    │              │
│  balances    │                    │              │                    │              │
│              │                    │              │                    │              │
│  12. Error?  │                    │              │                    │              │
│  Rollback    │                    │              │                    │              │
│  UI state    │                    │              │                    │              │
└──────────────┘                    └──────────────┘                    └──────────────┘
```

### 5.3 Especificación de la Integración

| Atributo | Valor |
|---|---|
| **Tipo** | Sync — Request-Response (transaccional) |
| **Método HTTP** | `PATCH` |
| **Endpoint** | `/api/v1/transactions/{id}/move` |
| **Request Body** | `{ "target_wallet_id": "uuid" }` |
| **Auth** | JWT Bearer Token |
| **Timeout** | 60 segundos (operación compleja con múltiples writes) |
| **Idempotencia** | Sí — basado en `transaction_id` + `target_wallet_id`. Si la transacción ya está en la wallet destino, retorna 200 sin cambios. |
| **Consistencia** | Strong — toda la operación es una transacción ACID en el Backend |
| **Compensación** | Si falla cualquier paso, ROLLBACK automático de la transacción DB. El Frontend revierte el estado visual (optimistic UI rollback). |
| **Retry** | Frontend reintenta 1 vez con backoff de 1s si recibe 5xx o timeout. No reintenta 4xx. |

### 5.4 Request/Response Contract

**Request:**
```http
PATCH /api/v1/transactions/{transactionId}/move HTTP/1.1
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "target_wallet_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (200 OK):**
```json
{
  "transaction": {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "wallet_id": "550e8400-e29b-41d4-a716-446655440000",
    "category_id": "770e8400-e29b-41d4-a716-446655440000",
    "type": "INCOME",
    "amount": "50000.00",
    "description": "Salario",
    "date": "2026-06-16",
    "created_at": "2026-06-16T10:00:00Z",
    "updated_at": "2026-06-16T10:30:00Z"
  },
  "source_wallet": {
    "id": "440e8400-e29b-41d4-a716-446655440000",
    "name": "Billetera Principal",
    "balance": "150000.00"
  },
  "target_wallet": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Ahorro",
    "balance": "250000.00"
  }
}
```

**Response (409 Conflict — misma billetera):**
```json
{
  "timestamp": "2026-06-16T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "TRANSFER_SAME_WALLET",
  "message": "No se puede mover una transacción a la misma billetera.",
  "path": "/api/v1/transactions/{id}/move",
  "trace_id": "abc-123-def-456",
  "details": []
}
```

### 5.5 Manejo de Errores en el Frontend

| Escenario | Acción del Frontend |
|---|---|
| **200 OK** | Confirmar optimistic update, actualizar signals de saldos |
| **400 Bad Request** | Mostrar error de validación, revertir UI |
| **401 Unauthorized** | Intentar refresh token; si falla, redirect a login |
| **403 Forbidden** | Mostrar error de permisos, revertir UI |
| **404 Not Found** | Transacción o wallet destino no existe; revertir UI y mostrar notificación |
| **409 Conflict** | Misma wallet; revertir drag & drop |
| **500 Internal Server Error** | Reintentar 1 vez; si persiste, revertir UI y mostrar error |
| **Timeout (>60s)** | Reintentar 1 vez; si persiste, revertir UI y mostrar error de timeout |

---

## 6. INT-004: Auth Flow (Login / Refresh)

### 6.1 Descripción

Flujo de autenticación completo: login inicial, uso de access token, renovación vía refresh token con rotación, y logout.

### 6.2 Flujo de Login

```
┌──────────────┐                         ┌──────────────┐                         ┌──────────────┐
│   Frontend   │                         │   Backend    │                         │  PostgreSQL  │
│              │                         │              │                         │              │
│  1. POST     │                         │              │                         │              │
│  /auth/login │────────────────────────▶│  2. Validar  │                         │              │
│  { email,    │                         │  credenciales│  3. SELECT user         │              │
│   password } │                         │              │────────────────────────▶│              │
│              │                         │              │◀────────────────────────│              │
│              │                         │              │                         │              │
│              │                         │  4. Verificar│                         │              │
│              │                         │  password    │                         │              │
│              │                         │  hash        │                         │              │
│              │                         │              │                         │              │
│              │                         │  5. Generar  │                         │              │
│              │                         │  JWT +       │  6. INSERT refresh_token│              │
│              │                         │  Refresh     │────────────────────────▶│              │
│              │                         │  Token       │                         │              │
│              │                         │              │                         │              │
│  7. Response │                         │              │                         │              │
│  200 OK     │◀────────────────────────│              │                         │              │
│  { accessToken,                        │              │                         │              │
│    refreshToken,                       │              │                         │              │
│    expiresIn }                         │              │                         │              │
│              │                         │              │                         │              │
│  8. Almacenar│                         │              │                         │              │
│  tokens en   │                         │              │                         │              │
│  Signals     │                         │              │                         │              │
└──────────────┘                         └──────────────┘                         └──────────────┘
```

### 6.3 Flujo de Refresh Token con Rotación

```
┌──────────────┐                         ┌──────────────┐                         ┌──────────────┐
│   Frontend   │                         │   Backend    │                         │  PostgreSQL  │
│              │                         │              │                         │              │
│  1. Detecta  │                         │              │                         │              │
│  access_token│                         │              │                         │              │
│  expirado    │                         │              │                         │              │
│              │                         │              │                         │              │
│  2. POST     │                         │              │                         │              │
│  /auth/      │────────────────────────▶│  3. Validar  │  4. SELECT refresh_token│              │
│  refresh     │  { refreshToken }       │  refresh     │────────────────────────▶│              │
│  {refreshToken}                        │  token       │◀────────────────────────│              │
│              │                         │              │                         │              │
│              │                         │  5. Verificar│                         │              │
│              │                         │  no revocado │                         │              │
│              │                         │  + no        │                         │              │
│              │                         │  expirado    │                         │              │
│              │                         │              │                         │              │
│              │                         │  6. ROTACIÓN:│  7. UPDATE: revocar     │              │
│              │                         │  - Revocar   │     token actual        │              │
│              │                         │    actual    │────────────────────────▶│              │
│              │                         │  - Generar   │                         │              │
│              │                         │    nuevo par │  8. INSERT nuevo        │              │
│              │                         │              │     refresh_token       │              │
│              │                         │              │────────────────────────▶│              │
│              │                         │              │                         │              │
│  9. Response │                         │              │                         │              │
│  200 OK     │◀────────────────────────│              │                         │              │
│  { newAccessToken,                     │              │                         │              │
│    newRefreshToken,                    │              │                         │              │
│    expiresIn }                         │              │                         │              │
│              │                         │              │                         │              │
│  10. Actualizar│                       │              │                         │              │
│  Signals con │                         │              │                         │              │
│  nuevos      │                         │              │                         │              │
│  tokens      │                         │              │                         │              │
│              │                         │              │                         │              │
│  11. Reintentar│                       │              │                         │              │
│  request     │                         │              │                         │              │
│  original    │                         │              │                         │              │
│  con nuevo   │                         │              │                         │              │
│  access_token│                         │              │                         │              │
└──────────────┘                         └──────────────┘                         └──────────────┘
```

### 6.4 Especificación de la Integración

| Atributo | Valor |
|---|---|
| **Tipo** | Sync — Request-Response |
| **Auth (Login)** | Sin auth (credenciales en body) |
| **Auth (Refresh)** | Refresh Token en body (no en header) |
| **Timeout** | 15 segundos |
| **Idempotencia** | Login: Sí (mismas credenciales producen tokens diferentes pero válidos). Refresh: No (cada refresh invalida el token anterior — rotación). |
| **Seguridad adicional** | Rate limiting en /auth/login (5 intentos por minuto por IP). Refresh token revocado no puede reutilizarse. |

### 6.5 Token Specifications

| Token | Tipo | Duración | Almacenamiento Frontend | Rotación |
|---|---|---|---|---|
| Access Token | JWT (HS256/RS256) | 15 minutos | Signal en memoria (no localStorage) | No |
| Refresh Token | UUID v4 opaco | 7 días | Signal en memoria | Sí (rotación en cada uso) |

### 6.6 Refresh Token Rotation — Reglas

| Regla | Justificación |
|---|---|
| Cada uso de refresh token genera uno nuevo | Limita la ventana de exposición si un token es comprometido |
| El refresh token usado se invalida inmediatamente | Previene reutilización (replay attack) |
| Si se detecta reutilización de un token revocado, invalidar toda la familia de tokens | Detecta robo de tokens; fuerza re-login |
| El refresh token se envía solo en el body del request /auth/refresh | No en headers ni cookies para simplificar la arquitectura SPA |

---

## 7. Matriz de Resiliencia

| Integración | Timeout | Retry | Circuit Breaker | Fallback | Idempotencia |
|---|---|---|---|---|---|
| Frontend → Backend (general) | 30s | 2x con backoff exponencial | N/A (Frontend) | Mostrar error al usuario | Depende del método HTTP |
| Frontend → Backend (transfer) | 60s | 1x con backoff 1s | N/A (Frontend) | Revertir optimistic UI | Sí (transaction_id) |
| Frontend → Backend (auth) | 15s | 1x | N/A (Frontend) | Redirect a login | Sí (login) / No (refresh) |
| Backend → PostgreSQL | 30s (query) / 5s (conn) | HikariCP retry automático | N/A (pool) | Error 500 al cliente | N/A (transaccional) |

---

## 8. Observabilidad de Integraciones

| Concern | Estrategia | Implementación |
|---|---|---|
| **Correlation ID** | Header `X-Request-Id` generado por Frontend, propagado por Backend | MDC en logs del Backend |
| **Logging** | Backend loguea cada request con método, path, status, duración | Structured JSON logs |
| **Métricas** | Contadores de requests por endpoint, tiempos de respuesta, errores | Spring Boot Actuator (MVP) |
| **Health Checks** | `/actuator/health` expone estado de DB y servicios | Docker health checks |

---

## 9. Evidencia

### Artefactos Relacionados

| Artefacto | Ruta Absoluta | Relación |
|---|---|---|
| System Landscape | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/system-landscape.md` | Define los contenedores que se integran |
| Context Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/context-map.md` | Define los bounded contexts que participan en las integraciones |
| ADR-003 JWT Refresh | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md` | Justifica la decisión de refresh token rotation |

### Contratos de API (Pendientes de Crear)

| Contrato | Ruta Esperada | Estado |
|---|---|---|
| OpenAPI Spec (Auth) | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/auth-api.yaml` | Pendiente |
| OpenAPI Spec (Wallets) | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/wallets-api.yaml` | Pendiente |
| OpenAPI Spec (Transactions) | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/transactions-api.yaml` | Pendiente |
| OpenAPI Spec (Categories) | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/categories-api.yaml` | Pendiente |
