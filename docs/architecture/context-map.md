# Context Map — NanoBank Ledger

> **Versión:** 1.0.0
> **Estado:** Accepted
> **Fecha:** 2026-06-16
> **Owner:** Enterprise Architect
> **Última revisión:** 2026-06-16

---

## 1. Propósito del Documento

Este documento define los **Bounded Contexts** del sistema NanoBank Ledger utilizando principios DDD (Domain-Driven Design). Establece el lenguaje ubicuo de cada contexto, las relaciones upstream/downstream, los patrones de integración entre contextos y los owners responsables.

Este documento complementa al `system-landscape.md` y debe consultarse junto con el `integration-map.md` para entender las fronteras de comunicación.

---

## 2. Vista General del Context Map

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        NANOBANK LEDGER — CONTEXT MAP                        │
│                                                                             │
│  ┌──────────────────┐    Customer-Supplier    ┌──────────────────────────┐  │
│  │                  │◀────────────────────────│                          │  │
│  │  BC: Auth        │    (tokens, user id)    │  BC: Wallets             │  │
│  │                  │────────────────────────▶│                          │  │
│  │  [Upstream]      │    Published Language   │  [Downstream]            │  │
│  └────────┬─────────┘                         └────────────┬─────────────┘  │
│           │                                                │                │
│           │ Customer-Supplier                              │ Customer-Supplier│
│           │ (user identity)                                │ (wallet context) │
│           ▼                                                ▼                │
│  ┌──────────────────┐                         ┌──────────────────────────┐  │
│  │                  │    Conformist           │                          │  │
│  │  BC: Categories  │◀────────────────────────│  BC: Transactions        │  │
│  │                  │    (category id)        │                          │  │
│  │  [Upstream]      │────────────────────────▶│  [Downstream]            │  │
│  └──────────────────┘                         └──────────────────────────┘  │
│           ▲                                                │                │
│           │ Customer-Supplier                              │                │
│           └────────────────────────────────────────────────┘                │
│                (category reference)                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Bounded Contexts — Detalle

### 3.1 BC: Auth (Autenticación y Autorización)

| Atributo | Valor |
|---|---|
| **Nombre** | Auth |
| **Propósito** | Gestionar la identidad de los usuarios, emisión y validación de tokens de acceso, rotación de refresh tokens y sesiones |
| **Owner Funcional** | Backend Team |
| **Owner Técnico** | Backend Developer Lead |
| **Patrón** | Published Language (expone contratos de tokens) |
| **Modelo de Consistencia** | Strong (ACID sobre tabla `users` y `refresh_tokens`) |

#### 3.1.1 Lenguaje Ubicuo

| Término | Definición | No debe reutilizarse como |
|---|---|---|
| **Usuario (User)** | Entidad que representa una identidad registrada con credenciales | No confundir con "persona" o "cuenta bancaria" |
| **Credencial (Credential)** | Par email/password almacenado de forma segura (hash) | No es un "token" |
| **Access Token** | JWT de corta duración (15 min) que prueba la identidad en cada request | No es un "session id" |
| **Refresh Token** | Token de larga duración (7 días) usado exclusivamente para renovar access tokens | No es un "access token" |
| **Rotación** | Proceso de invalidar un refresh token usado y emitir uno nuevo en su lugar | No es "renovación simple" |
| **Sesión** | Período de actividad del usuario definido por la validez del refresh token | No es una "sesión HTTP" |
| **Login** | Proceso de autenticación que genera un par access/refresh token | No es "registro" |
| **Logout** | Invalidación del refresh token activo | No es "cerrar navegador" |
| **Registro (Register)** | Creación de una nueva identidad de usuario con credenciales | No es "login" |

#### 3.1.2 Responsabilidades

- Validar credenciales (email + password) contra hash almacenado.
- Generar Access Token (JWT, 15 min) con claims: `sub` (user ID), `email`, `exp`, `iat`.
- Generar Refresh Token (UUID seguro, 7 días) y almacenarlo en `refresh_tokens`.
- Rotar Refresh Token: al usar un refresh token, invalidarlo y emitir uno nuevo.
- Invalidar refresh tokens en logout.
- Exponer el user ID al contexto downstream (Wallets, Transactions) vía el claim `sub` del JWT.

#### 3.1.3 Interfaces Públicas (Puertos)

| Puerto | Tipo | Descripción |
|---|---|---|
| `AuthenticateUser` | Use Case (Command) | Login con email/password → retorna tokens |
| `RegisterUser` | Use Case (Command) | Registro de nuevo usuario |
| `RefreshAccessToken` | Use Case (Command) | Renovar access token usando refresh token (con rotación) |
| `RevokeRefreshToken` | Use Case (Command) | Logout: invalidar refresh token |
| `ValidateToken` | Use Case (Query) | Validar y decodificar un access token |

---

### 3.2 BC: Wallets (Gestión de Billeteras)

| Atributo | Valor |
|---|---|
| **Nombre** | Wallets |
| **Propósito** | Gestionar el ciclo de vida de las billeteras virtuales: creación, consulta, edición, eliminación y cálculo de saldos |
| **Owner Funcional** | Backend Team |
| **Owner Técnico** | Backend Developer Lead |
| **Patrón** | Customer-Supplier (consume identidad de Auth, provee contexto de billetera a Transactions) |
| **Modelo de Consistencia** | Strong (ACID sobre tabla `wallets`) |

#### 3.2.1 Lenguaje Ubicuo

| Término | Definición | No debe reutilizarse como |
|---|---|---|
| **Billetera (Wallet)** | Contenedor virtual con nombre, descripción y saldo que agrupa transacciones | No es una "cuenta bancaria" real |
| **Saldo (Balance)** | Suma algebraica de ingresos menos gastos de una billetera | No es "saldo bancario" |
| **Saldo Inicial (Initial Balance)** | Monto con el que se crea la billetera | No es un "depósito" |
| **Moneda (Currency)** | Unidad monetaria de la billetera (ej: USD, COP, EUR) | No es un "tipo de cambio" |
| **Billetera Activa** | Billetera que existe en el sistema (no ha sido eliminada físicamente) | No es "billetera con saldo positivo" |

#### 3.2.2 Responsabilidades

- Crear billeteras con nombre, descripción, moneda y saldo inicial.
- Listar billeteras del usuario autenticado (filtrado por `user_id` del JWT).
- Actualizar datos de una billetera (nombre, descripción).
- Eliminar billeteras (DELETE físico). Solo si no tienen transacciones asociadas (RN-017).
- Calcular saldo actual de una billetera (saldo inicial + Σ ingresos - Σ gastos).
- Actualizar saldo atómicamente cuando se mueve una transacción (drag & drop).

#### 3.2.3 Interfaces Públicas (Puertos)

| Puerto | Tipo | Descripción |
|---|---|---|
| `CreateWallet` | Use Case (Command) | Crear nueva billetera |
| `ListWallets` | Use Case (Query) | Listar billeteras del usuario |
| `GetWalletById` | Use Case (Query) | Obtener detalle de una billetera |
| `UpdateWallet` | Use Case (Command) | Actualizar datos de billetera |
| `DeleteWallet` | Use Case (Command) | Eliminar billetera (DELETE físico). Rechazado si tiene transacciones asociadas (RN-017) |
| `UpdateWalletBalance` | Use Case (Command) | Ajustar saldo (usado internamente por Transactions en drag & drop) |

#### 3.2.4 Dependencias

| Depende de (Upstream) | Relación | Qué consume |
|---|---|---|
| BC: Auth | Customer-Supplier | `user_id` desde el claim `sub` del JWT para filtrar billeteras por usuario |

---

### 3.3 BC: Transactions (Gestión de Transacciones)

| Atributo | Valor |
|---|---|
| **Nombre** | Transactions |
| **Propósito** | Gestionar el registro de movimientos financieros (ingresos/gastos), su asociación a billeteras y categorías, y la operación de transferencia entre billeteras |
| **Owner Funcional** | Backend Team |
| **Owner Técnico** | Backend Developer Lead |
| **Patrón** | Conformist (respeta categorías de Categories) + Customer-Supplier con Wallets |
| **Modelo de Consistencia** | Strong (ACID — las operaciones de saldo y movimiento son transaccionales) |

#### 3.3.1 Lenguaje Ubicuo

| Término | Definición | No debe reutilizarse como |
|---|---|---|
| **Transacción (Transaction)** | Registro atómico de un movimiento financiero (ingreso o gasto) en una billetera | No es una "transacción de base de datos" |
| **Ingreso (Income)** | Transacción con monto positivo que incrementa el saldo de la billetera | No es un "depósito bancario" |
| **Gasto (Expense)** | Transacción con monto positivo (el tipo EXPENSE indica que decrementa el saldo) de la billetera | No es un "retiro" |
| **Monto (Amount)** | Valor numérico de la transacción, siempre positivo; el tipo INCOME/EXPENSE indica la dirección del efecto en el saldo | No es "saldo" |
| **Fecha de Transacción (Transaction Date)** | Fecha en que se registró el movimiento | No es "fecha de creación del registro" |
| **Descripción (Description)** | Texto libre que describe el propósito de la transacción | No es un "comentario" |
| **Transferencia (Transfer)** | Operación que mueve una transacción de una billetera origen a una billetera destino, actualizando ambos saldos | No es una "transacción bancaria" |
| **Drag & Drop** | Interacción de usuario que desencadena una transferencia entre billeteras | No es una "animación" |

#### 3.3.2 Responsabilidades

- Registrar ingresos en una billetera (incrementa saldo).
- Registrar gastos en una billetera (decrementa saldo).
- Listar transacciones de una billetera con filtros (categoría, fecha, tipo).
- Actualizar una transacción existente (editar monto, categoría, descripción).
- Eliminar una transacción (DELETE físico, revierte el efecto sobre el saldo).
- Ejecutar transferencias (drag & drop): mover transacción de billetera origen a destino con actualización atómica de ambos saldos.

#### 3.3.3 Interfaces Públicas (Puertos)

| Puerto | Tipo | Descripción |
|---|---|---|
| `CreateTransaction` | Use Case (Command) | Registrar nueva transacción (ingreso/gasto) |
| `ListTransactions` | Use Case (Query) | Listar transacciones con filtros (billetera, categoría, fecha) |
| `GetTransactionById` | Use Case (Query) | Obtener detalle de una transacción |
| `UpdateTransaction` | Use Case (Command) | Actualizar datos de una transacción |
| `DeleteTransaction` | Use Case (Command) | Eliminar transacción (DELETE físico) y revertir saldo |
| `TransferTransaction` | Use Case (Command) | Mover transacción entre billeteras (drag & drop) |

#### 3.3.4 Dependencias

| Depende de (Upstream) | Relación | Qué consume |
|---|---|---|
| BC: Auth | Customer-Supplier | `user_id` desde el claim `sub` del JWT |
| BC: Wallets | Customer-Supplier | `wallet_id` para asociar transacciones; `UpdateWalletBalance` para ajustar saldos en transferencias |
| BC: Categories | Conformist | `category_id` para clasificar transacciones (acepta el modelo de categorías tal cual) |

---

### 3.4 BC: Categories (Catálogo de Categorías)

| Atributo | Valor |
|---|---|
| **Nombre** | Categories |
| **Propósito** | Proveer el catálogo de categorías del sistema para clasificar transacciones (solo lectura en el MVP) |
| **Owner Funcional** | Backend Team |
| **Owner Técnico** | Backend Developer Lead |
| **Patrón** | Conformist (catálogo de referencia solo lectura; Transactions acepta las categorías tal cual) |
| **Modelo de Consistencia** | Strong (ACID sobre tabla `categories`) |

#### 3.4.1 Lenguaje Ubicuo

| Término | Definición | No debe reutilizarse como |
|---|---|---|
| **Categoría (Category)** | Etiqueta clasificatoria para transacciones (ej: Alimentación, Transporte, Entretenimiento) | No es un "tag" o "label" genérico |
| **Nombre de Categoría** | Texto descriptivo de la categoría | No es una "descripción de transacción" |
| **Color** | Código de color asociado a la categoría para visualización en el dashboard | No es un "tema" |
| **Categoría del Sistema** | Categoría predefinida por el sistema (solo lectura en el MVP) | No es una "categoría personalizada" |

#### 3.4.2 Responsabilidades

- Proveer catálogo de categorías del sistema (predefinidas).
- Listar categorías disponibles para clasificar transacciones.

#### 3.4.3 Interfaces Públicas (Puertos)

| Puerto | Tipo | Descripción |
|---|---|---|
| `ListCategories` | Use Case (Query) | Listar todas las categorías disponibles |

---

## 4. Matriz de Relaciones entre Bounded Contexts

| Upstream | Downstream | Relación DDD | Protocolo | Qué se comparte | Dirección del cambio |
|---|---|---|---|---|---|
| **Auth** | **Wallets** | Customer-Supplier | JWT claim `sub` | User ID | Auth define el formato del token; Wallets se adapta (Conformist al contrato) |
| **Auth** | **Transactions** | Customer-Supplier | JWT claim `sub` | User ID | Auth define el formato del token; Transactions se adapta |
| **Auth** | **Categories** | Customer-Supplier | JWT claim `sub` | User ID | Auth define el formato del token; Categories se adapta |
| **Wallets** | **Transactions** | Customer-Supplier | Internal API (puerto) | Wallet ID, Balance updates | Wallets define la interfaz de billeteras; Transactions consume |
| **Categories** | **Transactions** | Conformist | Internal API (puerto) | Category ID | Categories define el catálogo; Transactions lo acepta sin modificar |

---

## 5. Diagrama de Relaciones DDD (Detalle)

```
                    ┌─────────────────────────────────────┐
                    │           BC: Auth                   │
                    │                                     │
                    │  Published Language:                │
                    │  - JWT Token Contract               │
                    │  - User ID (sub claim)              │
                    │  - Refresh Token Contract           │
                    └──────────┬──────────────────────────┘
                               │
                    Customer-Supplier (3 downstream)
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                 ▼
   ┌──────────────┐  ┌──────────────────┐  ┌────────────────┐
   │ BC: Wallets  │  │ BC: Transactions │  │ BC: Categories │
   │              │  │                  │  │                │
   │ Consume:     │  │ Consume:         │  │ Consume:       │
   │ - user_id    │  │ - user_id        │  │ - user_id      │
   │              │  │ - wallet_id      │  │                │
   │ Provee:      │  │ - category_id    │  │ Provee:        │
   │ - wallet_id  │  │                  │  │ - category_id  │
   │ - balance    │  │                  │  │                │
   └──────┬───────┘  └──────────────────┘  └───────┬────────┘
          │                                         │
          │  Customer-Supplier                      │  Conformist
          │  (wallet context)                       │  (category catalog)
          ▼                                         │
   ┌──────────────────┐                             │
   │ BC: Transactions │◀────────────────────────────┘
   │                  │
   │ Consume:
   │ - wallet_id (de Wallets)
   │ - category_id (de Categories)
   │ - user_id (de Auth)
   └──────────────────┘
```

---

## 6. Reglas de Frontera entre Contextos

### 6.1 Reglas de No-Compartición

| Regla | Justificación |
|---|---|
| **No compartir entidades JPA entre contextos** | Cada BC tiene su propio modelo de persistencia. Si Transactions necesita datos de Wallets, los obtiene vía puertos, no vía joins directos. |
| **No compartir tablas de BD entre contextos** | Cada BC es dueño de sus tablas. `wallets` pertenece a Wallets; `transactions` pertenece a Transactions. |
| **No exponer DTOs internos entre contextos** | La comunicación entre BCs usa contratos explícitos (puertos). Los DTOs internos de un BC no son visibles fuera. |
| **User ID viaja solo como claim JWT** | Los BCs downstream no consultan la tabla `users`; solo extraen el `user_id` del token. |

### 6.2 Reglas de Evolución de Contratos

| Regla | Justificación |
|---|---|
| **Auth puede cambiar el formato del JWT sin notificar** | Los downstream solo dependen del claim `sub` (user ID), que es estable. |
| **Wallets debe notificar a Transactions si cambia la interfaz de balance** | Relación Customer-Supplier: el supplier (Wallets) debe mantener compatibilidad. |
| **Categories puede agregar categorías sin afectar a Transactions** | Conformist: Transactions solo consume `category_id`, no depende de la estructura interna. |

---

## 7. Anti-Corruption Layers (ACL)

En la fase MVP, **no se requieren Anti-Corruption Layers** explícitas porque:

1. Todos los Bounded Contexts viven dentro del mismo Backend (monolito modular).
2. La comunicación entre contextos es interna (llamadas a puertos dentro del mismo proceso).
3. No hay traducción de modelos externos.

**Nota:** Si en el futuro los BCs se separan en microservicios independientes, se deberán implementar ACLs en cada adaptador de comunicación inter-servicio para traducir entre modelos de dominio independientes.

---

## 8. Evidencia

### Artefactos Relacionados

| Artefacto | Ruta Absoluta | Relación con este documento |
|---|---|---|
| System Landscape | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/system-landscape.md` | Define los contenedores que albergan estos BCs |
| Integration Map | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/integration-map.md` | Detalla los protocolos de comunicación entre BCs |
| ADR-002 Hexagonal | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-002-hexagonal-architecture.md` | Justifica la arquitectura que soporta los boundaries de los BCs |

### Estructura de Bounded Contexts en el Código

| Bounded Context | Paquete esperado en Backend | Tablas en PostgreSQL |
|---|---|---|
| Auth | `com.nanobank.ledger.auth.*` | `users`, `refresh_tokens` |
| Wallets | `com.nanobank.ledger.wallets.*` | `wallets` |
| Transactions | `com.nanobank.ledger.transactions.*` | `transactions` |
| Categories | `com.nanobank.ledger.categories.*` | `categories` |
