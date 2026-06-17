# ADR-002: Arquitectura Hexagonal (Ports & Adapters) para el Backend

> **Estado:** Accepted
> **Fecha:** 2026-06-16
> **Owner:** Enterprise Architect + Solution Architect
> **Decisión:** Adoptar Arquitectura Hexagonal (Ports & Adapters) con Kotlin + Spring Boot 3.x como patrón arquitectónico para el Backend de NanoBank Ledger.
> **Última revisión:** 2026-06-16

---

## 1. Contexto

El Backend de NanoBank Ledger es el componente central del sistema: gestiona la lógica de negocio financiera (billeteras, transacciones, saldos), la autenticación JWT y la persistencia en PostgreSQL. Las siguientes características del dominio exigen una arquitectura robusta:

### 1.1 Factores Decisivos

- **Consistencia financiera:** Las operaciones de saldo (ingresos, gastos, transferencias) requieren transaccionalidad ACID y lógica de negocio libre de efectos secundarios técnicos.
- **Testabilidad:** La lógica de negocio debe poder probarse sin dependencias de frameworks, bases de datos o infraestructura.
- **Evolución del dominio:** Se esperan nuevos casos de uso (presupuestos, metas de ahorro, reportes) que no deben requerir cambios en la infraestructura.
- **Desacoplamiento de frameworks:** Spring Boot, JPA y PostgreSQL son decisiones técnicas que no deben contaminar el modelo de dominio.
- **Múltiples bounded contexts:** Auth, Wallets, Transactions y Categories deben tener boundaries claros para evitar acoplamiento inadvertido.

### 1.2 Requisitos No Funcionales

| Requisito | Implicación Arquitectónica |
|---|---|
| Código testeable | El dominio no debe depender de Spring, JPA ni HTTP |
| Bajo acoplamiento | Cambiar la BD (ej: PostgreSQL → MySQL) no debe afectar la lógica de negocio |
| Alto cohesión | Cada bounded context debe ser autocontenido |
| Mantenibilidad | Nuevos desarrolladores deben entender rápidamente dónde va cada tipo de código |
| Escalabilidad futura | Posibilidad de extraer bounded contexts a microservicios sin reescribir el dominio |

---

## 2. Decisión

Adoptar **Arquitectura Hexagonal (Ports & Adapters)** organizada en capas concéntricas con dependencias pointing inward, implementada en Kotlin con Spring Boot 3.x como framework de soporte.

### 2.1 Estructura de Capas

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADAPTERS (Driving / Primary)                  │
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │  REST Controllers │  │  JWT Filter      │  │  CLI Commands │  │
│  │  (Spring MVC)     │  │  (Security)      │  │  (si aplica)  │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬────────┘  │
│           │                      │                    │           │
│           ▼                      ▼                    ▼           │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   PORTS (Driving / Primary)                  │ │
│  │                                                              │ │
│  │  Use Cases / Application Services                            │ │
│  │  ┌──────────────────────────────────────────────────────┐   │ │
│  │  │  CreateWallet, ListWallets, CreateTransaction,       │   │ │
│  │  │  TransferTransaction, AuthenticateUser, ...          │   │ │
│  │  └──────────────────────┬───────────────────────────────┘   │ │
│  │                         │                                    │ │
│  │                         ▼                                    │ │
│  │  ┌──────────────────────────────────────────────────────┐   │ │
│  │  │              DOMAIN (Centro del Hexágono)             │   │ │
│  │  │                                                      │   │ │
│  │  │  Entities: Wallet, Transaction, Category, User       │   │ │
│  │  │  Value Objects: Money, TransactionType, DateRange    │   │ │
│  │  │  Domain Services: BalanceCalculator, ...             │   │ │
│  │  │  Domain Events: TransactionCreated, ...              │   │ │
│  │  │  Port Interfaces (Driven): WalletRepository, ...     │   │ │
│  │  │                                                      │   │ │
│  │  └──────────────────────────────────────────────────────┘   │ │
│  │                                                              │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                         │                                        │
│                         ▼                                        │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   PORTS (Driven / Secondary)                 │ │
│  │                                                              │ │
│  │  WalletRepository, TransactionRepository,                    │ │
│  │  UserRepository, RefreshTokenRepository, ...                 │ │
│  │                                                              │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│                              │                                    │
│                              ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   ADAPTERS (Driven / Secondary)              │ │
│  │                                                              │ │
│  │  ┌──────────────────┐  ┌──────────────────┐                 │ │
│  │  │  JPA Repositories │  │  External APIs   │                 │ │
│  │  │  (Spring Data JPA)│  │  (si aplica)     │                 │ │
│  │  └──────────────────┘  └──────────────────┘                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Reglas de Dependencia

| Regla | Justificación |
|---|---|
| **El Domain NO depende de nada externo** | El dominio puro (entidades, value objects, domain services) no importa ni Spring, ni JPA, ni nada de infraestructura |
| **Los Puertos (Ports) definen interfaces en el Domain** | Los contratos de repositorios se definen como interfaces en la capa de dominio |
| **Los Adapters implementan los Puertos** | JPA Repositories, REST Controllers y JWT Filters implementan las interfaces definidas en el dominio |
| **Los Use Cases orquestan el flujo** | Los Application Services coordinan el dominio y los puertos, pero no contienen lógica de negocio |
| **Los DTOs viven en los Adapters** | Los objetos de transporte (request/response HTTP) no entran al dominio; se mapean en los adapters |
| **Las entidades de persistencia (JPA) viven en los Adapters** | Las `@Entity` classes son detalles de infraestructura, no del dominio |

### 2.3 Mapeo de Paquetes Kotlin

```
com.nanobank.ledger
├── auth/                          ← Bounded Context: Auth
│   ├── domain/
│   │   ├── model/                 ← User, RefreshToken (entidades de dominio puras)
│   │   ├── port/                  ← UserRepository, RefreshTokenRepository (interfaces)
│   │   └── service/               ← PasswordHasher (domain service)
│   ├── application/
│   │   ├── usecase/               ← AuthenticateUser, RegisterUser, RefreshAccessToken
│   │   └── dto/                   ← LoginRequest, LoginResponse, TokenResponse
│   └── adapter/
│       ├── in/                    ← AuthController (REST), JwtAuthenticationFilter
│       └── out/                   ← JpaUserRepository, JpaRefreshTokenRepository, BcryptPasswordHasher
│
├── wallets/                       ← Bounded Context: Wallets
│   ├── domain/
│   │   ├── model/                 ← Wallet, Balance (value object)
│   │   ├── port/                  ← WalletRepository
│   │   └── service/               ← BalanceCalculator
│   ├── application/
│   │   ├── usecase/               ← CreateWallet, ListWallets, UpdateWalletBalance
│   │   └── dto/                   ← CreateWalletRequest, WalletResponse
│   └── adapter/
│       ├── in/                    ← WalletController
│       └── out/                   ← JpaWalletRepository, WalletEntity, WalletMapper
│
├── transactions/                  ← Bounded Context: Transactions
│   ├── domain/
│   │   ├── model/                 ← Transaction, TransactionType, Money
│   │   ├── port/                  ← TransactionRepository
│   │   └── service/               ← TransferService
│   ├── application/
│   │   ├── usecase/               ← CreateTransaction, TransferTransaction, ListTransactions
│   │   └── dto/                   ← CreateTransactionRequest, TransferRequest
│   └── adapter/
│       ├── in/                    ← TransactionController
│       └── out/                   ← JpaTransactionRepository, TransactionEntity, TransactionMapper
│
├── categories/                    ← Bounded Context: Categories
│   ├── domain/
│   │   ├── model/                 ← Category, CategoryType
│   │   ├── port/                  ← CategoryRepository
│   │   └── service/
│   ├── application/
│   │   ├── usecase/               ← ListCategories, CreateCategory
│   │   └── dto/                   ← CategoryResponse
│   └── adapter/
│       ├── in/                    ← CategoryController
│       └── out/                   ← JpaCategoryRepository, CategoryEntity, CategoryMapper
│
└── shared/                        ← Cross-cutting concerns
    ├── config/                    ← Spring configuration classes
    ├── security/                  ← JWT utilities, SecurityConfig
    ├── exception/                 ← Global exception handlers
    └── common/                    ← Shared value objects, utilities
```

### 2.4 Principios de Implementación

| Principio | Descripción |
|---|---|
| **Dominio puro en Kotlin** | Las entidades de dominio son `data class` o clases inmutables sin anotaciones de framework |
| **Value Objects para conceptos del dominio** | `Money`, `Balance`, `TransactionType`, `DateRange` son value objects, no primitivos |
| **Puertos como interfaces Kotlin** | Los repositorios son interfaces en `domain/port/`, no heredan de `JpaRepository` |
| **Adapters JPA separados** | Las `@Entity` classes y los `JpaRepository` viven en `adapter/out/` |
| **Mappers explícitos** | Conversión entre entidades de dominio y entidades JPA mediante mappers dedicados |
| **Use Cases como Spring `@Service`** | Los Application Services son beans de Spring que inyectan puertos (interfaces) |
| **Controllers como thin adapters** | Los REST Controllers solo mapean HTTP a Use Cases; no contienen lógica de negocio |

---

## 3. Alternativas Consideradas

### 3.1 Alternativa A: MVC Tradicional (Model-View-Controller)

**Descripción:** Estructura clásica con paquetes por tipo técnico: `controllers/`, `services/`, `repositories/`, `models/`.

**Ventajas:**
- Ampliamente conocido; curva de aprendizaje mínima.
- Menos archivos y clases para funcionalidad simple.
- Soporte nativo y abundante en Spring Boot.

**Desventajas:**
- **Acoplamiento dominio-infraestructura:** Los `@Entity` son simultáneamente el modelo de dominio y el de persistencia. Cambiar la BD o agregar validaciones de dominio complejas requiere modificar las mismas clases.
- **Services god-object:** Los `@Service` tienden a acumular lógica de negocio, validación, persistencia y orquestación en una sola clase.
- **Testabilidad limitada:** Probar la lógica de negocio requiere mockear el framework (Spring, JPA) o levantar un contexto completo.
- **Boundaries difusos:** No hay separación clara entre bounded contexts; todo tiende a un modelo anémico compartido.
- **Difícil extracción a microservicios:** El acoplamiento entre capas hace costoso separar bounded contexts.

**Razón de rechazo:** El dominio financiero de NanoBank Ledger (saldos, transferencias atómicas, consistencia) requiere separación estricta entre lógica de negocio e infraestructura. MVC tradicional no ofrece esta garantía.

### 3.2 Alternativa B: Arquitectura en Capas Tradicional (N-Tier)

**Descripción:** Capas horizontales: `Presentation → Business → Data Access → Database`, donde cada capa depende de la inferior.

**Ventajas:**
- Separación más clara que MVC por tipo técnico.
- Fácil de entender para equipos acostumbrados a enterprise Java.
- Permite cierta independencia de la capa de presentación.

**Desventajas:**
- **Dependencias hacia abajo:** La capa de negocio depende de la capa de acceso a datos, creando acoplamiento con JPA/Hibernate.
- **Modelo anémico:** La capa de negocio tiende a ser un passthrough sin lógica real; las entidades son solo getters/setters.
- **Fronteras de bounded context difusas:** No hay un mecanismo natural para separar Auth de Wallets de Transactions.
- **Testabilidad intermedia:** Mejor que MVC pero peor que Hexagonal; aún se necesita mockear la capa de datos.

**Razón de rechazo:** La dependencia de la capa de negocio hacia la capa de datos viola el principio de inversión de dependencias. Para un dominio con reglas financieras críticas, se necesita que el dominio sea independiente de la infraestructura.

### 3.3 Alternativa C: Clean Architecture (Robert C. Martin)

**Descripción:** Variante de Arquitectura Hexagonal con énfasis en Use Cases como organizador principal y Entities como centro absoluto.

**Ventajas:**
- Separación aún más estricta que Hexagonal.
- Fuerte énfasis en Use Cases como concepto de primer nivel.
- Excelente testabilidad del dominio.

**Desventajas:**
- Mayor número de archivos y clases (cada Use Case es una clase separada).
- Puede ser excesivo para un MVP con un equipo pequeño.
- La distinción entre "Clean" y "Hexagonal" es a menudo cosmética; la diferencia práctica es mínima.

**Razón de no-adopción (parcial):** Se adoptan los principios de Clean Architecture (dependencias hacia adentro, dominio puro) pero se usa la nomenclatura y organización de Hexagonal por ser más pragmática en el ecosistema Kotlin/Spring Boot. Los Use Cases se agrupan por bounded context en lugar de una clase por operación.

---

## 4. Consecuencias

### 4.1 Ventajas (Consecuencias Positivas)

| Ventaja | Impacto |
|---|---|
| **Dominio testeable en aislamiento** | Los tests unitarios del dominio no requieren Spring, BD ni HTTP. Tests rápidos (< 10ms cada uno) |
| **Independencia de framework** | Si se decide migrar de Spring Boot a otro framework, el dominio no cambia |
| **Independencia de persistencia** | Cambiar de PostgreSQL a MySQL o MongoDB solo afecta los adapters `out/`, no el dominio |
| **Boundaries explícitos** | Cada bounded context tiene su propio hexágono con dominio, puertos y adapters independientes |
| **Onboarding predecible** | Un nuevo desarrollador sabe exactamente dónde va cada tipo de código |
| **Preparado para microservicios** | Si un bounded context necesita extraerse, el dominio ya está aislado |
| **Value Objects ricos** | Conceptos como `Money` y `Balance` encapsulan reglas de negocio (ej: saldo no puede ser negativo) |

### 4.2 Desventajas (Consecuencias Negativas)

| Desventaja | Mitigación |
|---|---|
| **Mayor número de archivos** | Para una operación CRUD simple, hay ~8-10 archivos (Controller, DTO, Use Case, Port, Entity, Repository, Mapper, Domain Model). Aceptado como costo de la separación |
| **Curva de aprendizaje inicial** | Los desarrolladores nuevos deben entender las reglas de dependencia. Mitigado con este ADR y documentación |
| **Mappers explícitos** | Convertir entre Domain ↔ JPA ↔ DTO requiere código de mapeo. Mitigado con mappers dedicados y tests |
| **Overhead para CRUDs triviales** | Para operaciones muy simples (ej: listar categorías), la arquitectura puede sentirse pesada. Aceptado como costo de consistencia |
| **Configuración de Spring más compleja** | Se necesita configurar la inyección de dependencias entre capas correctamente. Mitigado con `@Configuration` classes centralizadas |

### 4.3 Reglas de Bloqueo

| Regla | Consecuencia de violación |
|---|---|
| El dominio NO puede importar Spring | Si lo hace, el código no compila en tests unitarios sin contexto Spring |
| Los Controllers NO pueden contener lógica de negocio | Code review debe rechazar lógica en adapters `in/` |
| Las entidades JPA NO pueden exponerse fuera de `adapter/out/` | Los DTOs son la única forma de comunicar datos entre capas |
| Los Use Cases NO pueden conocer HTTP ni SQL | Solo conocen puertos (interfaces) del dominio |

---

## 5. Relación con Otros ADRs y Estándares

| Referencia | Relación |
|---|---|
| `hexagonal-architecture` (skill) | Define los principios detallados de implementación de Ports & Adapters |
| ADR-001 (Monorepo) | La estructura hexagonal se aplica dentro del Backend, independientemente del monorepo |
| ADR-003 (JWT Refresh) | La lógica de rotación de tokens vive en el dominio de Auth, los adapters solo implementan el almacenamiento |
| `context-map.md` | Los bounded contexts definidos en el Context Map se implementan como hexágonos independientes |
| `repository-dto-patterns` (skill) | Define los patrones de mapeo entre Domain, DTO y JPA entities |

---

## 6. Evidencia

### Artefactos Creados

| # | Archivo | Ruta Absoluta | Estado |
|---|---|---|---|
| 1 | ADR-002 | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-002-hexagonal-architecture.md` | ✅ Creado |

### Estructura Esperada en el Backend

| Capa | Ruta Esperada | Contenido |
|---|---|---|
| Domain | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/*/domain/` | Entidades puras, Value Objects, interfaces de puertos |
| Application | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/*/application/` | Use Cases, DTOs de aplicación |
| Adapter In | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/*/adapter/in/` | REST Controllers, Filters |
| Adapter Out | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/*/adapter/out/` | JPA Entities, JPA Repositories, Mappers |
| Tests Unitarios | `NanoBankLedger-backend/src/test/kotlin/com/nanobank/ledger/*/domain/` | Tests del dominio sin Spring |
| Tests Integración | `NanoBankLedger-backend/src/test/kotlin/com/nanobank/ledger/*/adapter/` | Tests con Spring context + TestContainers |
