# Master Spec — NanoBank Ledger

> **Lifecycle status:** `awaiting-human-plan-approval`
> **Versión:** 1.0.0
> **Fecha:** 2026-06-16
> **Owner:** Planner
> **Última revisión:** 2026-06-16

---

## 1. Propósito del Sistema

NanoBank Ledger es un MVP de gestión financiera personal que permite a los usuarios administrar billeteras virtuales, registrar transacciones de ingresos y gastos, categorizar movimientos y visualizar dashboards con filtros en tiempo real. El sistema soporta drag & drop de transacciones entre billeteras con actualización atómica de saldos en el backend.

### 1.1 Alcance Funcional (In-Scope)

| # | Funcionalidad | Descripción |
|---|---|---|
| F-001 | Dashboard de Billeteras | Crear, listar, editar y eliminar billeteras virtuales con saldo |
| F-002 | Gestión de Transacciones | Registrar ingresos y gastos vinculados a billeteras y categorías |
| F-003 | Filtros en Tiempo Real | Filtrar transacciones por categoría, fecha y tipo (ingreso/gasto) |
| F-004 | Drag & Drop | Mover transacciones entre billeteras con actualización atómica de saldos |
| F-005 | Autenticación JWT | Registro, login, refresh token rotativo y logout |
| F-006 | Catálogo de Categorías | Listar categorías predefinidas para clasificar transacciones |

### 1.2 Fuera de Alcance (Out-of-Scope)

- Integración con sistemas bancarios externos
- Notificaciones push o email
- Multi-tenancy
- Procesamiento de pagos
- Reportes avanzados o exportación
- Multi-moneda (una sola moneda por billetera en MVP)

---

## 2. Stack Tecnológico

### 2.1 Backend

| Componente | Tecnología | Versión | Justificación |
|---|---|---|---|
| Framework | Spring Boot | 3.x (última estable) | Soporte nativo Kotlin, Virtual Threads, observabilidad |
| Lenguaje | Kotlin | 2.x | Nulo-safe, data classes, coroutines |
| ORM | Spring Data JPA / Hibernate | 7.x (incluido en Spring Boot 3.x) | Mapeo objeto-relacional |
| Base de Datos | PostgreSQL | 16 | ACID, JSONB, UUID nativo |
| Migraciones | Flyway | 10.x | Versionado inmutable de esquema |
| Seguridad | Spring Security + JWT (jjwt) | 6.x (incluido en Spring Boot 3.x) | JWT dual con refresh rotation |
| Build | Gradle (Kotlin DSL) | 8.x | Build system nativo Kotlin |
| API Spec | OpenAPI | 3.1 | Contract-first |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers | — | Pirámide completa |
| Cobertura | JaCoCo | — | 80% mínimo global |

### 2.2 Frontend

| Componente | Tecnología | Versión | Justificación |
|---|---|---|---|
| Framework | Angular | 17+ | Standalone Components, Signals |
| Lenguaje | TypeScript | 5.x | Tipado estático |
| Estado | Angular Signals | 17+ | Reactividad granular sin RxJS excesivo |
| UI | Minimalist UI premium utilitario | — | Ver skill `minimalist-ui` |
| Testing | Jasmine + Karma | — | Testing unitario y de integración |
| Build | Angular CLI | 17+ | Build system oficial |

### 2.3 Infraestructura

| Componente | Tecnología | Versión | Justificación |
|---|---|---|---|
| Contenedores | Docker | 24+ | Reproducibilidad |
| Orquestación | Docker Compose | 2.x (V2) | Desarrollo local |
| Servidor Web | Nginx | 1.25+ | Servir SPA Angular |
| Red | Docker Network | — | Comunicación interna entre contenedores |

---

## 3. Arquitectura General

### 3.1 Estructura del Monorepo (ADR-001)

El proyecto sigue una estructura de monorepo con tres directorios hermanos dentro del workspace:

```
NanoBankLedger-workspace/
├── docs/
│   ├── architecture/          ← Documentación macro (C4, ADRs, Context Map)
│   ├── api/                   ← Contratos OpenAPI (fuente de verdad)
│   ├── specs/                 ← Especificaciones SDD
│   │   ├── .working/          ← Shared contexts activos
│   │   ├── increments/        ← Delta specs
│   │   └── tasks/             ← Task boards
│   └── requerimientos.pdf
├── NanoBankLedger-backend/    ← Spring Boot + Kotlin
├── NanoBankLedger-frontend/   ← Angular 17+
└── NanoBankLedger-infrastructure/ ← Docker + scripts BD
```

**Referencia:** [ADR-001-monorepo-structure.md](docs/architecture/decision-records/ADR-001-monorepo-structure.md)

### 3.2 Arquitectura Hexagonal (ADR-002)

El Backend implementa Arquitectura Hexagonal (Ports & Adapters) con 4 Bounded Contexts:

```
com.nanobank.ledger
├── auth/                       ← BC: Auth
│   ├── domain/                 ← Entidades puras, puertos (interfaces)
│   ├── application/            ← Use Cases, DTOs de aplicación
│   └── adapter/
│       ├── in/                 ← AuthController, JwtAuthenticationFilter
│       └── out/                ← JPA Repositories, Entities, Mappers
├── wallets/                    ← BC: Wallets
│   ├── domain/
│   ├── application/
│   └── adapter/
├── transactions/               ← BC: Transactions
│   ├── domain/
│   ├── application/
│   └── adapter/
├── categories/                 ← BC: Categories
│   ├── domain/
│   ├── application/
│   └── adapter/
└── shared/                     ← Cross-cutting (config, security, exceptions)
```

**Reglas de dependencia:**
- `domain/` — CERO dependencias externas (sin Spring, sin JPA, sin Jackson)
- `application/` — Depende solo de `domain/`
- `adapter/` — Depende de `application/` y `domain/`
- Los DTOs de transporte NO cruzan hacia el dominio
- Las entidades JPA viven exclusivamente en `adapter/out/`

**Referencia:** [ADR-002-hexagonal-architecture.md](docs/architecture/decision-records/ADR-002-hexagonal-architecture.md)

### 3.3 API-First (Contract-First)

- El contrato OpenAPI 3.1 vive en `docs/api/openapi.yaml` (fuente de verdad de diseño)
- Se copia a `NanoBankLedger-backend/src/main/resources/openapi.yaml` (copia runtime)
- Los controllers implementan interfaces generadas desde OpenAPI (`interfaceOnly: true`)
- Los DTOs de transporte se generan desde el contrato OpenAPI
- Cualquier cambio visible al cliente se refleja primero en OpenAPI

### 3.4 Bounded Contexts (Context Map)

| BC | Propósito | Tablas | Relación DDD |
|---|---|---|---|
| **Auth** | Identidad, tokens, sesiones | `users`, `refresh_tokens` | Upstream (Published Language) |
| **Wallets** | Billeteras y saldos | `wallets` | Downstream de Auth, Upstream de Transactions |
| **Transactions** | Ingresos, gastos, transferencias | `transactions` | Downstream de Auth, Wallets, Categories |
| **Categories** | Catálogo de categorías | `categories` | Upstream de Transactions (Conformist) |

**Referencia:** [context-map.md](docs/architecture/context-map.md)

---

## 4. Modelo de Datos Conceptual

### 4.1 Entidad: User (BC: Auth)

| Campo | Tipo | Null | Unique | Index | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | NO | PK | — | Identificador único (gen_random_uuid()) |
| `name` | `VARCHAR(100)` | NO | — | — | Nombre completo del usuario |
| `email` | `VARCHAR(255)` | NO | YES | B-Tree | Email único (login) |
| `password_hash` | `VARCHAR(255)` | NO | — | — | Hash BCrypt (cost 12) |
| `created_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de creación |
| `updated_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de última actualización |

**Constraints:**
- `email` debe ser un email válido (validación en dominio + BD)
- `password_hash` nunca se expone fuera del BC Auth

### 4.2 Entidad: RefreshToken (BC: Auth)

| Campo | Tipo | Null | Unique | Index | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | NO | PK | — | Identificador único |
| `user_id` | `UUID` | NO | — | B-Tree | FK → `users.id` ON DELETE CASCADE |
| `token_hash` | `VARCHAR(64)` | NO | YES | B-Tree | SHA-256 del refresh token |
| `family_id` | `UUID` | NO | — | B-Tree | Familia de tokens (rotación) |
| `issued_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de emisión (default CURRENT_TIMESTAMP) |
| `expires_at` | `TIMESTAMPTZ` | NO | — | B-Tree | Expiración (7 días desde emisión) |
| `revoked_at` | `TIMESTAMPTZ` | YES | — | — | NULL = activo, NOT NULL = revocado |
| `used_at` | `TIMESTAMPTZ` | YES | — | — | Marca cuándo fue usado (detección robo) |
| `created_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de creación (default CURRENT_TIMESTAMP) |
| `updated_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de última actualización (default CURRENT_TIMESTAMP) |

**Referencia:** [ADR-003-jwt-refresh-rotation.md](docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md)

### 4.3 Entidad: Wallet (BC: Wallets)

| Campo | Tipo | Null | Unique | Index | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | NO | PK | — | Identificador único |
| `user_id` | `UUID` | NO | — | B-Tree | FK → `users.id` ON DELETE CASCADE |
| `name` | `VARCHAR(100)` | NO | — | — | Nombre de la billetera |
| `type` | `VARCHAR(20)` | NO | — | — | Tipo: `CHECKING`, `SAVINGS`, `CASH`, etc. |
| `balance` | `NUMERIC(15,2)` | NO | — | — | Saldo actual (default 0.00) |
| `created_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de creación |
| `updated_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de última actualización |

**Constraints:**
- `balance >= 0` (CHECK constraint — los saldos no pueden ser negativos)
- `(user_id, name)` debe ser único por usuario (nombre de billetera no repetido)

**Nota sobre `initial_balance` en creación de wallet:** El campo `initial_balance` del `CreateWalletRequest` es **opcional** con valor default `"0.00"`. Si el cliente no lo envía, la wallet se crea con saldo cero. El OpenAPI lo define como propiedad no requerida con `default: "0.00"`.

### 4.4 Entidad: Transaction (BC: Transactions)

| Campo | Tipo | Null | Unique | Index | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | NO | PK | — | Identificador único |
| `wallet_id` | `UUID` | NO | — | B-Tree | FK → `wallets.id` ON DELETE CASCADE |
| `category_id` | `UUID` | NO | — | B-Tree | FK → `categories.id` |
| `type` | `VARCHAR(10)` | NO | — | B-Tree | `INCOME` o `EXPENSE` |
| `amount` | `NUMERIC(15,2)` | NO | — | — | Monto (siempre positivo; el tipo indica dirección) |
| `description` | `VARCHAR(500)` | YES | — | — | Descripción libre |
| `date` | `DATE` | NO | — | B-Tree | Fecha de la transacción |
| `created_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de creación |
| `updated_at` | `TIMESTAMPTZ` | NO | — | — | Fecha de última actualización |

**Constraints:**
- `amount > 0` (CHECK constraint — el monto siempre es positivo)
- `type IN ('INCOME', 'EXPENSE')` (CHECK constraint)

### 4.5 Entidad: Category (BC: Categories)

| Campo | Tipo | Null | Unique | Index | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | NO | PK | — | Identificador único |
| `name` | `VARCHAR(50)` | NO | — | — | Nombre de la categoría |
| `type` | `VARCHAR(10)` | NO | — | B-Tree | `INCOME` o `EXPENSE` |
| `icon` | `VARCHAR(50)` | YES | — | — | Identificador de icono (ej: `utensils`, `car`) |
| `color` | `VARCHAR(7)` | YES | — | — | Código hex de color (ej: `#FF5733`) |

**Constraints:**
- `name` único a nivel de catálogo del sistema
- `type IN ('INCOME', 'EXPENSE')` (CHECK constraint)

### 4.6 Diagrama de Relaciones

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐
│    users     │       │  refresh_tokens   │       │     wallets      │
│──────────────│       │──────────────────│       │──────────────────│
│ id (PK)      │◀──┐   │ id (PK)          │       │ id (PK)          │
│ name         │   │   │ user_id (FK)─────│───┐   │ user_id (FK)─────│──┐
│ email (UQ)   │   │   │ token_hash (UQ)  │   │   │ name             │  │
│ password_hash│   │   │ family_id        │   │   │ type             │  │
│ created_at   │   │   │ issued_at        │   │   │ balance          │  │
│ updated_at   │   │   │ expires_at       │   │   │ created_at       │  │
└──────────────┘   │   │ revoked_at       │   │   │ updated_at       │  │
       │           │   │ used_at          │   │   └──────────────────┘  │
       │           │   │ created_at       │   │            │            │
       │           │   │ updated_at       │   │            │            │
       │           │   └──────────────────┘   │            ▼            │
       │           │                          │   ┌──────────────────┐  │
       │           │                          │   │  transactions    │  │
       │           │                          │   │──────────────────│  │
       │           │                          │   │ id (PK)          │  │
       │           │                          │   │ wallet_id (FK)───│──┘
       │           │                          │   │ category_id (FK)─│──┐
       │           │                          │   │ type             │  │
       │           │                          │   │ amount           │  │
       │           │                          │   │ description      │  │
       │           │                          │   │ date             │  │
       │           │                          │   │ created_at       │  │
       │           │                          │   │ updated_at       │  │
       │           │                          │   └──────────────────┘  │
       │           │                          │            │            │
       │           │                          │            │            │
       │           │                          │            ▼            │
       │           │                          │   ┌──────────────────┐  │
       │           │                          └──▶│   categories     │  │
       │           │                              │──────────────────│  │
       │           │                              │ id (PK)          │  │
       │           │                              │ name (UQ)        │  │
       │           │                              │ type             │  │
       │           │                              │ icon             │  │
       │           │                              │ color            │  │
       │           │                              └──────────────────┘  │
       │           │                                                    │
       └───────────┘ (FK user_id en wallets y refresh_tokens)          │
                                                                    │
       (category_id en transactions referencia categories) ◀────────┘
```

---

## 5. Reglas de Negocio Globales

### 5.1 Reglas Financieras

| # | Regla | Implementación | Consecuencia |
|---|---|---|---|
| RN-001 | **Los saldos de las wallets no pueden ser negativos** | CHECK constraint `balance >= 0` en BD + validación en dominio antes de decrementar | Si un gasto o transferencia causaría saldo negativo, se rechaza con `422 WALLET_INSUFFICIENT_BALANCE` |
| RN-002 | **Las transferencias entre wallets deben ser atómicas** | `@Transactional` con `isolation = READ_COMMITTED` en el Use Case `TransferTransaction`. Descontar origen + incrementar destino + mover transacción en una sola transacción DB | Si cualquier paso falla, ROLLBACK completo. El Frontend revierte el estado visual (optimistic UI rollback) |
| RN-003 | **El monto de una transacción siempre es positivo** | CHECK constraint `amount > 0` en BD + validación en dominio | El campo `type` (`INCOME`/`EXPENSE`) determina la dirección del efecto sobre el saldo |
| RN-004 | **Al crear un ingreso, el saldo de la wallet se incrementa** | Use Case `CreateTransaction` → `wallet.balance += transaction.amount` | Operación atómica dentro de la transacción DB |
| RN-005 | **Al crear un gasto, el saldo de la wallet se decrementa** | Use Case `CreateTransaction` → `wallet.balance -= transaction.amount` | Si `wallet.balance - transaction.amount < 0`, se rechaza (RN-001) |
| RN-006 | **Al eliminar una transacción, se revierte su efecto sobre el saldo** | Use Case `DeleteTransaction` → si era INGRESO: `wallet.balance -= amount`; si era GASTO: `wallet.balance += amount` | Operación atómica |
| RN-007 | **Al actualizar una transacción, se recalcula el saldo** | Use Case `UpdateTransaction` → revertir efecto anterior + aplicar nuevo efecto | Solo si cambia `amount` o `type` |

### 5.2 Reglas de Autenticación (ADR-003)

| # | Regla | Implementación | Consecuencia |
|---|---|---|---|
| RN-008 | **Refresh Token rotation: al usar un refresh token, se invalida el anterior y se emite uno nuevo (misma familia)** | Use Case `RefreshAccessToken` → marcar `used_at` del token actual + generar nuevo token con mismo `family_id` | Cada refresh token solo puede usarse UNA vez |
| RN-009 | **Detección de robo: si un refresh token ya usado se reutiliza, se invalidan TODOS los tokens de la familia** | Use Case `RefreshAccessToken` → si `used_at IS NOT NULL`, revocar todos los tokens con ese `family_id` | Retorna `401 TOKEN_FAMILY_REVOKED` y fuerza re-login |
| RN-010 | **Access Token tiene duración de 15 minutos** | JWT claim `exp = iat + 900` | Frontend debe renovar antes de expiración |
| RN-011 | **Refresh Token tiene duración de 7 días** | Campo `expires_at = created_at + 7 days` | Después de 7 días sin actividad, el usuario debe re-login |
| RN-012 | **Logout invalida el refresh token activo** | Use Case `RevokeRefreshToken` → marcar `revoked_at` | El token no puede reutilizarse |
| RN-013 | **Password hashing con BCrypt cost 12** | `BcryptPasswordHasher` en adapter out | Nunca almacenar passwords en texto plano |

### 5.3 Reglas de Integridad

| # | Regla | Implementación |
|---|---|---|
| RN-014 | **Un usuario solo puede acceder a sus propias wallets** | Todos los queries de wallets filtran por `user_id` extraído del JWT claim `sub` |
| RN-015 | **Un usuario solo puede acceder a sus propias transacciones** | Las transacciones se listan vía `wallet_id`, y las wallets ya están filtradas por `user_id` |
| RN-016 | **No se puede mover una transacción a la misma wallet** | Use Case `TransferTransaction` valida `source_wallet_id != target_wallet_id` → `409 TRANSFER_SAME_WALLET` |
| RN-017 | **No se puede eliminar una wallet con transacciones asociadas** | Use Case `DeleteWallet` verifica `transactions.count == 0` → `409 WALLET_HAS_TRANSACTIONS` |
| RN-018 | **Email de usuario debe ser único** | UNIQUE constraint en BD + validación en dominio → `409 EMAIL_ALREADY_REGISTERED` |

---

## 6. Contratos de API

### 6.1 Resumen de Endpoints

**Fuente de verdad:** `docs/api/openapi.yaml`

#### Auth (público — sin autenticación)

| Método | Path | Descripción | Status Codes |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Registrar nuevo usuario | `201`, `400`, `409` |
| `POST` | `/api/v1/auth/login` | Iniciar sesión (devuelve access + refresh token) | `200`, `400`, `401` |
| `POST` | `/api/v1/auth/refresh` | Refrescar access token (rotación) | `200`, `400`, `401` |
| `POST` | `/api/v1/auth/logout` | Cerrar sesión (invalida refresh token) | `204`, `401` |

#### Wallets (protegido — JWT Bearer)

| Método | Path | Descripción | Status Codes |
|---|---|---|---|
| `GET` | `/api/v1/wallets` | Listar billeteras del usuario autenticado | `200`, `401` |
| `POST` | `/api/v1/wallets` | Crear nueva billetera | `201`, `400`, `401`, `409` |
| `GET` | `/api/v1/wallets/{id}` | Obtener detalle de billetera (con saldo) | `200`, `401`, `404` |
| `PATCH` | `/api/v1/wallets/{id}` | Actualizar billetera (nombre, tipo) | `200`, `400`, `401`, `404`, `409` |
| `DELETE` | `/api/v1/wallets/{id}` | Eliminar billetera | `204`, `401`, `404`, `409` |

#### Transactions (protegido — JWT Bearer)

| Método | Path | Descripción | Status Codes |
|---|---|---|---|
| `GET` | `/api/v1/wallets/{walletId}/transactions` | Listar transacciones con filtros | `200`, `401`, `404` |
| `POST` | `/api/v1/wallets/{walletId}/transactions` | Crear transacción (ingreso o gasto) | `201`, `400`, `401`, `404`, `422` |
| `GET` | `/api/v1/transactions/{id}` | Obtener detalle de transacción | `200`, `401`, `404` |
| `PATCH` | `/api/v1/transactions/{id}` | Actualizar transacción | `200`, `400`, `401`, `404`, `422` |
| `DELETE` | `/api/v1/transactions/{id}` | Eliminar transacción | `204`, `401`, `404` |
| `PATCH` | `/api/v1/transactions/{id}/move` | Mover transacción a otra wallet (Drag & Drop) | `200`, `400`, `401`, `404`, `409`, `422` |

#### Categories (protegido — JWT Bearer)

| Método | Path | Descripción | Status Codes |
|---|---|---|---|
| `GET` | `/api/v1/categories` | Listar categorías disponibles | `200`, `401` |

### 6.2 Paginación

- `page`: 0-indexed (default `0`)
- `size`: default `20`, máximo `100`
- Respuesta paginada incluye: `content` (lista de items), `page` (página actual), `size` (tamaño de página), `total_elements` (total de registros), `total_pages` (total de páginas)

### 6.3 Error Contract

Todos los errores siguen el schema `ApiErrorResponse` definido en `springboot-kotlin-rest-error-response-standards`:

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

**Campos:**
- `timestamp` (string, ISO 8601 UTC) — Instante del error
- `status` (integer) — HTTP status code
- `error` (string) — Nombre del status HTTP
- `code` (string) — Código estable de negocio/técnico (no el número HTTP)
- `message` (string) — Mensaje descriptivo seguro (sin detalles internos)
- `path` (string) — Path del request que causó el error
- `trace_id` (string) — Identificador de traza para correlación
- `details` (array de `ApiErrorDetail`) — Detalles de validación (puede ser vacío)

**ApiErrorDetail:**
- `field` (string, nullable) — Campo que causó el error
- `code` (string) — Código del error de campo
- `message` (string) — Mensaje descriptivo
- `rejected_value` (any, nullable) — Valor rechazado

---

## 7. Estrategia de Seguridad

### 7.1 Autenticación JWT Dual (ADR-003)

| Token | Tipo | Duración | Almacenamiento Frontend | Rotación |
|---|---|---|---|---|
| **Access Token** | JWT (HS256) | 15 minutos | Signal en memoria (no localStorage) | No |
| **Refresh Token** | UUID v4 opaco | 7 días | Signal en memoria | Sí (rotación en cada uso) |

### 7.2 Endpoints Públicos vs Protegidos

| Categoría | Endpoints | Auth |
|---|---|---|
| **Públicos** | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` | Sin autenticación |
| **Refresh** | `POST /api/v1/auth/refresh` | Refresh Token en body |
| **Protegidos** | Todos los demás (`/wallets/**`, `/transactions/**`, `/categories/**`, `POST /api/v1/auth/logout`) | JWT Bearer Token en header `Authorization` |

### 7.3 Medidas de Seguridad Adicionales

| Medida | Implementación |
|---|---|
| **Password hashing** | BCrypt con cost 12 |
| **Rate limiting en /auth/login** | Máximo 5 intentos por minuto por IP |
| **CORS restrictivo** | Solo el origen del Frontend |
| **HTTPS** | Obligatorio en producción (TLS 1.3). En desarrollo local se acepta HTTP |
| **Content Security Policy** | Headers CSP para prevenir XSS |
| **No exponer datos sensibles** | Nunca retornar `password_hash`, `token_hash` ni stack traces |
| **Limpieza de tokens expirados** | Job periódico que elimina `refresh_tokens` expirados/revocados > 30 días |

### 7.4 Flujo de Refresh Token con Detección de Robo

```
1. Frontend detecta 401 o Access Token próximo a expirar
2. POST /api/v1/auth/refresh con Refresh Token en body
3. Backend busca token por hash en BD
4. Backend verifica: no expirado + no revocado
5. ROTACIÓN:
   a. Marcar token actual con used_at = NOW()
   b. Generar nuevo Access Token (15 min)
   c. Generar nuevo Refresh Token (UUID, 7 días, mismo family_id)
   d. Almacenar hash del nuevo Refresh Token
6. Retornar nuevo par de tokens
7. DETECCIÓN DE ROBO:
   a. Si token ya tiene used_at NOT NULL → reutilización detectada
   b. Revocar TODOS los tokens de la familia (family_id)
   c. Retornar 401 TOKEN_FAMILY_REVOKED
   d. Frontend redirige a login
```

---

## 8. Estrategia de Testing

### 8.1 Herramientas

| Capa | Herramienta | Propósito |
|---|---|---|
| Unit tests (dominio) | JUnit 5 + AssertJ | Probar lógica de negocio sin Spring |
| Unit tests (use cases) | JUnit 5 + Mockito | Probar orquestación con puertos mockeados |
| Integration tests (adapters) | Spring Boot Test + Testcontainers | Probar adapters con BD real en contenedor |
| API tests (endpoints) | MockMvc / TestRestTemplate | Probar contratos HTTP completos |
| Frontend unit tests | Jasmine + Karma | Probar componentes, servicios, guards |
| Cobertura backend | JaCoCo | 80% mínimo global, 85% por archivo testable |
| Cobertura frontend | Karma coverage reporter | 80% mínimo |

### 8.2 Pirámide de Tests

```
                    ┌─────────────┐
                    │  E2E Tests  │  ← Smoke tests (login, CRUD wallet, drag & drop)
                    │   (5-10)    │
                   ┌┴─────────────┴┐
                   │  API Tests    │  ← Todos los endpoints (status codes, schemas, auth)
                   │   (30-50)     │
                  ┌┴───────────────┴┐
                  │ Integration     │  ← Adapters con BD real (Testcontainers)
                  │ Tests (20-30)   │
                 ┌┴─────────────────┴┐
                 │  Unit Tests       │  ← Dominio + Use Cases (la mayoría)
                 │   (80-120)        │
                 └───────────────────┘
```

### 8.3 Exclusiones JaCoCo

Los siguientes paquetes se excluyen del cálculo de cobertura:
- `**/dto/**` — DTOs de transporte (generados desde OpenAPI)
- `**/entity/**` — Entidades JPA (solo datos, sin lógica)
- `**/config/**` — Configuración de Spring
- `**/exceptions/**` — Clases de excepciones (solo datos)
- `*MapperImpl*` — Implementaciones generadas por MapStruct

### 8.4 Tests Obligatorios por Escenario

| Escenario | Tipo | Descripción |
|---|---|---|
| Crear ingreso → saldo incrementa | Unit + Integration | Verificar RN-004 |
| Crear gasto → saldo decrementa | Unit + Integration | Verificar RN-005 |
| Gasto que causaría saldo negativo → rechazado | Unit + API | Verificar RN-001 → `422 WALLET_INSUFFICIENT_BALANCE` |
| Transferencia atómica → ambos saldos actualizados | Integration | Verificar RN-002 |
| Transferencia con rollback → nada cambia | Integration | Verificar RN-002 (fallo parcial) |
| Refresh token rotation → token anterior invalidado | Integration | Verificar RN-008 |
| Reutilización de refresh token → familia revocada | Integration | Verificar RN-009 |
| Acceso a wallet de otro usuario → 404 | API | Verificar RN-014 |
| DTO inválido → `400 VALIDATION_ERROR` con details | API | Verificar error contract |
| Token expirado → `401 TOKEN_EXPIRED` | API | Verificar seguridad |

---

## 9. Estrategia de Despliegue

### 9.1 Desarrollo Local (Docker Compose)

```yaml
# NanoBankLedger-infrastructure/docker-compose/docker-compose.yml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: nanobank_ledger
      POSTGRES_USER: nanobank
      POSTGRES_PASSWORD: nanobank_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U nanobank"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: ../../NanoBankLedger-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/nanobank_ledger
      SPRING_DATASOURCE_USERNAME: nanobank
      SPRING_DATASOURCE_PASSWORD: nanobank_dev
      JWT_SECRET: dev-secret-key-change-in-production
    depends_on:
      postgres:
        condition: service_healthy

  frontend:
    build: ../../NanoBankLedger-frontend
    ports:
      - "4200:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

### 9.2 Migraciones Flyway

- Ubicación: `NanoBankLedger-backend/src/main/resources/db/migration/`
- Nomenclatura: `V{version}__{description}.sql` (ej: `V1.0.0__create_users_table.sql`)
- `ddl-auto=validate` — JPA solo valida que el esquema coincide con las entidades
- Flyway es el único dueño del esquema de BD
- Las migraciones son inmutables una vez aplicadas

### 9.3 Variables de Entorno

| Variable | Descripción | Default (dev) |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL de conexión JDBC | `jdbc:postgresql://localhost:5432/nanobank_ledger` |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD | `nanobank` |
| `SPRING_DATASOURCE_PASSWORD` | Password BD | `nanobank_dev` |
| `JWT_SECRET` | Clave secreta para firmar JWT (HS256) | `dev-secret-key-change-in-production` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Duración Access Token (segundos) | `900` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Duración Refresh Token (segundos) | `604800` (7 días) |
| `BCRYPT_COST` | Cost de BCrypt para password hashing | `12` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para CORS | `http://localhost:4200` |

---

## 10. Criterios de Aceptación Globales

### 10.1 Funcionales

| # | Criterio | Verificación |
|---|---|---|
| CA-001 | Un usuario puede registrarse con email + password y recibir confirmación | Test E2E: registro → login exitoso |
| CA-002 | Un usuario autenticado puede crear una wallet con nombre, tipo y saldo inicial | Test API: POST /wallets → 201 |
| CA-003 | Un usuario autenticado puede listar sus wallets con saldo actualizado | Test API: GET /wallets → 200 con balances |
| CA-004 | Un usuario puede crear un ingreso y el saldo de la wallet se incrementa | Test Integration: balance antes/después |
| CA-005 | Un usuario puede crear un gasto y el saldo de la wallet se decrementa | Test Integration: balance antes/después |
| CA-006 | Un gasto que causaría saldo negativo es rechazado con 422 | Test API: POST → 422 WALLET_INSUFFICIENT_BALANCE |
| CA-007 | Un usuario puede mover una transacción a otra wallet (drag & drop) y ambos saldos se actualizan atómicamente | Test Integration: transfer atómica |
| CA-008 | Si la transferencia falla, nada cambia (rollback completo) | Test Integration: rollback |
| CA-009 | Los filtros por categoría, fecha y tipo funcionan correctamente | Test API: GET con query params |
| CA-010 | El refresh token rota en cada uso y se detecta la reutilización | Test Integration: rotation + theft detection |

### 10.2 No Funcionales

| # | Criterio | Verificación |
|---|---|---|
| CA-NF-001 | Cobertura backend >= 80% global | JaCoCo report |
| CA-NF-002 | Cobertura frontend >= 80% | Karma coverage report |
| CA-NF-003 | Todos los endpoints documentados en OpenAPI 3.1 | Validación del YAML |
| CA-NF-004 | Todos los errores siguen el schema `ApiErrorResponse` | Tests de error contract |
| CA-NF-005 | El dominio no depende de Spring, JPA ni HTTP | Tests unitarios sin contexto Spring |
| CA-NF-006 | Las migraciones Flyway son inmutables y versionadas | Revisión de archivos de migración |
| CA-NF-007 | Docker Compose levanta todo el stack con un solo comando | `docker-compose up` funcional |

---

## 11. Decomposition Contract

### 11.1 Canonical Endpoint Paths

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/wallets
POST   /api/v1/wallets
GET    /api/v1/wallets/{id}
PATCH  /api/v1/wallets/{id}
DELETE /api/v1/wallets/{id}
GET    /api/v1/wallets/{walletId}/transactions
POST   /api/v1/wallets/{walletId}/transactions
GET    /api/v1/transactions/{id}
PATCH  /api/v1/transactions/{id}
DELETE /api/v1/transactions/{id}
PATCH  /api/v1/transactions/{id}/move
GET    /api/v1/categories
```

### 11.2 Canonical DTO/Schema Names

| Schema OpenAPI | Descripción |
|---|---|
| `RegisterRequest` | Body de registro (name, email, password) |
| `LoginRequest` | Body de login (email, password) |
| `LoginResponse` | Respuesta de login (access_token, refresh_token, token_type, expires_in) |
| `RefreshRequest` | Body de refresh (refresh_token) |
| `RefreshResponse` | Respuesta de refresh (access_token, refresh_token, token_type, expires_in) |
| `CreateWalletRequest` | Body de crear wallet (name, type, initial_balance) |
| `WalletResponse` | Respuesta de wallet (id, user_id, name, type, balance, created_at, updated_at) |
| `UpdateWalletRequest` | Body de actualizar wallet (name, type) — parcial |
| `CreateTransactionRequest` | Body de crear transacción (category_id, type, amount, description, date) |
| `TransactionResponse` | Respuesta de transacción (id, wallet_id, category_id, type, amount, description, date, created_at, updated_at) |
| `UpdateTransactionRequest` | Body de actualizar transacción (category_id, amount, description, date) — parcial |
| `MoveTransactionRequest` | Body de mover transacción (target_wallet_id) |
| `MoveTransactionResponse` | Respuesta de mover (transaction, source_wallet, target_wallet) |
| `CategoryResponse` | Respuesta de categoría (id, name, type, icon, color) |
| `PaginatedTransactionResponse` | Respuesta paginada de transacciones (content, page, size, total_elements, total_pages). Nombre específico usado en OpenAPI; para otros recursos paginados se seguirá el mismo patrón con sufijo del recurso |
| `ApiErrorResponse` | Error contract (timestamp, status, error, code, message, path, trace_id, details) |
| `ApiErrorDetail` | Detalle de error de campo (field, code, message, rejected_value) |

### 11.3 Canonical DB Tables/Columns/Enums

| Tabla | Columnas clave | Enums |
|---|---|---|
| `users` | `id`, `name`, `email` (UQ), `password_hash` | — |
| `refresh_tokens` | `id`, `user_id` (FK), `token_hash` (UQ), `family_id`, `issued_at`, `expires_at`, `revoked_at`, `used_at`, `created_at`, `updated_at` | — |
| `wallets` | `id`, `user_id` (FK), `name`, `type`, `balance` | `type`: `CHECKING`, `SAVINGS`, `CASH`, `CREDIT` |
| `transactions` | `id`, `wallet_id` (FK), `category_id` (FK), `type`, `amount`, `description`, `date` | `type`: `INCOME`, `EXPENSE` |
| `categories` | `id`, `name` (UQ), `type`, `icon`, `color` | `type`: `INCOME`, `EXPENSE` |

### 11.4 Stale Terms Guard (Prohibidos)

| Término prohibido | Reemplazar con |
|---|---|
| `account` | `wallet` (no es cuenta bancaria) |
| `movement` | `transaction` |
| `transfer` (como endpoint) | `move` (el endpoint es `/move`, no `/transfer`) |
| `targetWalletId` (camelCase) | `target_wallet_id` (snake_case en JSON) |
| `error_code` | `code` (según error contract estándar) |
| `success` (flag en response) | No usar; el status HTTP indica éxito/error |
| `data` (wrapper) | No usar; retornar el recurso directamente |
| `PUT` (para actualizaciones parciales) | `PATCH` (semántica correcta) |

### 11.5 Archivos Autoritativos

| Artefacto | Ruta Absoluta |
|---|---|
| Master Spec | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/master-spec.md` |
| Shared Context | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/.working/001-mvp-initial-sdd-context.md` |
| OpenAPI Contract | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/openapi.yaml` |
| System Landscape | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/system-landscape.md` |
| Context Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/context-map.md` |
| Integration Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/integration-map.md` |
| ADR-001 Monorepo | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-001-monorepo-structure.md` |
| ADR-002 Hexagonal | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-002-hexagonal-architecture.md` |
| ADR-003 JWT Refresh | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md` |

---

## 12. Referencias Cruzadas

| Documento | Ruta | Relación |
|---|---|---|
| System Landscape | `docs/architecture/system-landscape.md` | Visión macro C4 Level 1-2 |
| Context Map | `docs/architecture/context-map.md` | Bounded Contexts DDD |
| Integration Map | `docs/architecture/integration-map.md` | Contratos de integración y flujos |
| ADR-001 | `docs/architecture/decision-records/ADR-001-monorepo-structure.md` | Estructura de monorepo |
| ADR-002 | `docs/architecture/decision-records/ADR-002-hexagonal-architecture.md` | Arquitectura Hexagonal |
| ADR-003 | `docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md` | JWT con Refresh Token rotativo |
| OpenAPI | `docs/api/openapi.yaml` | Contrato de API (fuente de verdad) |
