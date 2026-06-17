# Task Board — 001-mvp-initial

> **Increment:** 001-mvp-initial
> **Spec aprobada:** `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/master-spec.md`
> **Shared Context:** `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/specs/.working/001-mvp-initial-sdd-context.md`
> **OpenAPI Contract:** `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/api/openapi.yaml`
> **Estado superior:** `todo`
> **Orden de ejecución:** Backend → Frontend → Infraestructura
> **Fecha de creación:** 2026-06-16
> **Creado por:** task-decomposer

---

## Resumen de Tareas

| Módulo | Sección | Tareas | Rango |
|---|---|---|---|
| Backend | A. Setup del Proyecto Backend | 3 | T1–T3 |
| Backend | B. Modelo de Datos y Persistencia | 3 | T4–T6 |
| Backend | C. Módulo Auth | 6 | T7–T12 |
| Backend | D. Módulo Wallets | 5 | T13–T17 |
| Backend | E. Módulo Transactions | 5 | T18–T22 |
| Backend | F. Módulo Categories | 3 | T23–T25 |
| Backend | G. Cross-cutting Backend | 3 | T26–T28 |
| Frontend | H. Setup del Proyecto Frontend | 3 | T29–T31 |
| Frontend | I. Módulo Auth Frontend | 3 | T32–T34 |
| Frontend | J. Módulo Dashboard/Wallets | 3 | T35–T37 |
| Frontend | K. Módulo Transactions Frontend | 4 | T38–T41 |
| Infraestructura | L. Docker y Despliegue | 3 | T42–T44 |
| **Total** | | **44** | |

---

# BACKEND

## A. Setup del Proyecto Backend

---

### T1: Inicializar proyecto Spring Boot con Gradle (Kotlin DSL)

- **ID:** T1
- **Título:** Inicializar proyecto Spring Boot con Gradle (Kotlin DSL) y todas las dependencias
- **Dependencias:** ninguna
- **Owner:** executor
- **Archivos afectados:** `NanoBankLedger-backend/build.gradle.kts`, `settings.gradle.kts`, `src/main/kotlin/com/nanobank/ledger/NanoBankLedgerApplication.kt`
- **Especificaciones de referencia:** Master Spec SS2.1, SS3.2; ADR-001; ADR-002
- **Pasos:**
  1. Crear `build.gradle.kts` con plugins: `kotlin("jvm")`, `kotlin("plugin.spring")`, `kotlin("plugin.jpa")`, `io.spring.dependency-management`, `org.springframework.boot`, `jacoco`.
  2. Dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `flyway-core`, `flyway-database-postgresql`, `postgresql`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, `jackson-module-kotlin`, `kotlin-reflect`.
  3. Dependencias test: `spring-boot-starter-test`, `spring-security-test`, `testcontainers`, `testcontainers-postgresql`, `mockito-kotlin`, `assertj-core`.
  4. Configurar JaCoCo con exclusiones: `**/dto/**`, `**/entity/**`, `**/config/**`, `**/exceptions/**`, `*MapperImpl*`.
  5. Crear `NanoBankLedgerApplication.kt` con `@SpringBootApplication`.
  6. Crear estructura de paquetes hexagonal: `auth/`, `wallets/`, `transactions/`, `categories/`, `shared/` cada uno con `domain/`, `application/`, `adapter/in/`, `adapter/out/`.
  7. Verificar `./gradlew build` compila exitosamente.
- **Criterios de verificación:**
  - `./gradlew build` sin errores (compile + test smoke).
  - Estructura de paquetes hexagonales existe.
  - `build.gradle.kts` contiene todas las dependencias del Master Spec SS2.1.
- **Estado:** `done` | **executor_notes:** Archivos de build, config, main class y .gitignore creados | **verification_result:** 5 archivos verificados en filesystem | **blocker:** `none`

---

### T2: Configurar application.yaml y perfiles dev/prod

- **ID:** T2
- **Título:** Configurar application.yaml con perfiles dev/prod, datasource, JPA, JWT, CORS
- **Dependencias:** T1
- **Owner:** executor
- **Archivos afectados:** `src/main/resources/application.yaml`, `application-dev.yaml`, `application-prod.yaml`
- **Especificaciones de referencia:** Master Spec SS2.1, SS9.3; ADR-003
- **Pasos:**
  1. `application.yaml` base: `spring.application.name: nanobank-ledger`, `jackson.property-naming-strategy: SNAKE_CASE`, `server.port: 8080`, `spring.profiles.active: dev`.
  2. Datasource con placeholders `${SPRING_DATASOURCE_URL}`, `${SPRING_DATASOURCE_USERNAME}`, `${SPRING_DATASOURCE_PASSWORD}`.
  3. JPA: `ddl-auto: validate`, `show-sql: false`, `open-in-view: false`.
  4. HikariCP: `maximum-pool-size: 20`, `connection-timeout: 5000`.
  5. `application-dev.yaml`: valores por defecto desarrollo local, `show-sql: true`.
  6. `application-prod.yaml`: valores seguros, logging JSON.
  7. Propiedades custom: `app.jwt.secret`, `app.jwt.access-token-expiration: 900`, `app.jwt.refresh-token-expiration: 604800`, `app.bcrypt.cost: 12`, `app.cors.allowed-origins: http://localhost:4200`.
- **Criterios de verificación:**
  - `./gradlew bootRun` arranca con profile `dev` sin errores de configuracion.
  - `snake_case` configurado como naming strategy.
  - `ddl-auto: validate` confirmado (no `update`, no `create`).
- **Estado:** `done` | **executor_notes:** Creados application.yaml, application-dev.yaml, application-prod.yaml, actualizado .gitignore | **verification_result:** 4 archivos verificados en filesystem | **blocker:** `none`

---

### T3: Configurar Flyway con PostgreSQL y ddl-auto=validate

- **ID:** T3
- **Título:** Configurar Flyway con PostgreSQL, ddl-auto=validate, directorio de migraciones
- **Dependencias:** T2
- **Owner:** executor
- **Archivos afectados:** `src/main/resources/application.yaml`, `src/main/resources/db/migration/` (directorio)
- **Especificaciones de referencia:** Master Spec SS9.2; skill `flyway-migrations`; D-018
- **Pasos:**
  1. En `application.yaml`: `spring.flyway.enabled: true`, `locations: classpath:db/migration`, `baseline-on-migrate: false`.
  2. Flyway apunta a las mismas variables de entorno del datasource.
  3. Crear directorio `src/main/resources/db/migration/`.
  4. Verificar `ddl-auto: validate` presente.
  5. Referenciar skill `flyway-migrations` para nomenclatura `V{version}__{description}.sql`.
- **Criterios de verificación:**
  - Flyway se inicializa sin errores al arrancar con PostgreSQL.
  - Sin migraciones aun, Flyway reporta estado limpio.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

## B. Modelo de Datos y Persistencia

---

### T4: Crear migracion Flyway V1__initial_schema.sql

- **ID:** T4
- **Título:** Crear V1__initial_schema.sql con las 5 tablas y constraints
- **Dependencias:** T3
- **Owner:** executor
- **Archivos afectados:** `src/main/resources/db/migration/V1__initial_schema.sql`
- **Especificaciones de referencia:** Master Spec SS4, SS11.3; skills `postgresql-standard`, `flyway-migrations`
- **Pasos:**
  1. Tabla `users`: `id UUID DEFAULT gen_random_uuid() PK`, `name VARCHAR(100) NOT NULL`, `email VARCHAR(255) NOT NULL UNIQUE`, `password_hash VARCHAR(255) NOT NULL`, `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
  2. Tabla `refresh_tokens`: `id UUID PK`, `user_id UUID FK users(id) ON DELETE CASCADE`, `token_hash VARCHAR(64) NOT NULL UNIQUE`, `family_id UUID NOT NULL`, `issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, `expires_at TIMESTAMPTZ NOT NULL`, `revoked_at TIMESTAMPTZ`, `used_at TIMESTAMPTZ`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`. Indices B-Tree en `user_id`, `family_id`, `expires_at`.
  3. Tabla `wallets`: `id UUID PK`, `user_id UUID FK users(id) ON DELETE CASCADE`, `name VARCHAR(100) NOT NULL`, `type VARCHAR(20) NOT NULL`, `balance NUMERIC(15,2) NOT NULL DEFAULT 0.00`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`. CHECK `balance >= 0` (RN-001). UNIQUE `(user_id, name)`. Indice `user_id`.
  4. Tabla `transactions`: `id UUID PK`, `wallet_id UUID FK wallets(id) ON DELETE CASCADE`, `category_id UUID FK categories(id)`, `type VARCHAR(10) NOT NULL`, `amount NUMERIC(15,2) NOT NULL`, `description VARCHAR(500)`, `date DATE NOT NULL`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`. CHECK `amount > 0` (RN-003). CHECK `type IN ('INCOME','EXPENSE')`. Indices en `wallet_id`, `category_id`, `date`, `type`.
  5. Tabla `categories`: `id UUID PK`, `name VARCHAR(50) NOT NULL UNIQUE`, `type VARCHAR(10) NOT NULL`, `icon VARCHAR(50)`, `color VARCHAR(7)`. CHECK `type IN ('INCOME','EXPENSE')`.
  6. Comentarios SQL descriptivos en cada bloque.
- **Criterios de verificación:**
  - Migracion se ejecuta contra PostgreSQL 16 sin errores.
  - `flyway_schema_history` muestra `success`.
  - Constraints verificables con `\d+ <table>`.
  - Tipos: UUID, VARCHAR, NUMERIC(15,2), TIMESTAMPTZ, DATE coinciden con SS4.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T5: Crear entidades JPA

- **ID:** T5
- **Título:** Crear UserEntity, RefreshTokenEntity, WalletEntity, TransactionEntity, CategoryEntity
- **Dependencias:** T4
- **Owner:** executor
- **Archivos afectados:** 5 archivos en `adapter/out/` de cada BC
- **Especificaciones de referencia:** Master Spec SS4, SS11.3; skills `repository-dto-patterns`, `hexagonal-architecture`
- **Pasos:**
  1. `UserEntity` en `auth/adapter/out/`: `@Entity @Table(name="users")`, campos con `@Column(name="snake_case")`, `@Id @GeneratedValue UUID`.
  2. `RefreshTokenEntity` en `auth/adapter/out/`: mapeo completo (10 campos), `@ManyToOne UserEntity`.
  3. `WalletEntity` en `wallets/adapter/out/`: `balance` como `BigDecimal`, `@ManyToOne UserEntity`.
  4. `TransactionEntity` en `transactions/adapter/out/`: `amount BigDecimal`, `@ManyToOne WalletEntity` y `CategoryEntity`.
  5. `CategoryEntity` en `categories/adapter/out/`: sin FK a users (catalogo del sistema).
  6. `snake_case` en nombres de columna. Sin logica de negocio.
- **Criterios de verificacion:** `./gradlew build` compila; `ddl-auto=validate` no falla; entidades solo en `adapter/out/`.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T6: Crear repositorios JPA y mappers persistence->domain

- **ID:** T6
- **Título:** Crear repositorios JPA y mappers entity<->domain por BC
- **Dependencias:** T5
- **Owner:** executor
- **Archivos afectados:** 9 archivos en `adapter/out/` de cada BC
- **Especificaciones de referencia:** skill `repository-dto-patterns`
- **Pasos:**
  1. `UserJpaRepository`: `findByEmail(email)`.
  2. `RefreshTokenJpaRepository`: `findByTokenHash(hash)`, `findAllByFamilyId(familyId)`, `deleteByExpiresAtBefore(date)`.
  3. `WalletJpaRepository`: `findAllByUserId(userId)`, `findByIdAndUserId(id, userId)`, `existsByNameAndUserId(name, userId)`.
  4. `TransactionJpaRepository`: metodo paginado con filtros `findAllByWalletIdAndCategoryIdAndDateBetweenAndType(...)`, `countByWalletId(walletId)`.
  5. `CategoryJpaRepository`: `findAllByType(type)`.
  6. Crear `XxxPersistenceMapper` con funciones `Entity.toDomain()` y `Domain.toEntity()`.
- **Criterios de verificacion:** `./gradlew build` compila; repositorios extienden `JpaRepository`; metodos de query compilan; mappers producen objetos de dominio sin exponer entidades.
- **Estado:** `done` | **executor_notes:** Creados 16 archivos: 1 modelo de dominio RefreshToken, 5 puertos de aplicacion (output ports), 5 repositorios JPA, 5 mappers entity<->domain | **verification_result:** 16 archivos verificados en filesystem | **blocker:** `none`

---

## C. Modulo Auth

---

### T7: Implementar dominio Auth

- **ID:** T7
- **Título:** Implementar User, RefreshToken, PasswordHasher (puerto), JwtProvider (puerto), TokenPair, excepciones
- **Dependencias:** T1
- **Owner:** executor
- **Archivos afectados:** `auth/domain/` - 6 archivos
- **Especificaciones de referencia:** Master Spec SS4.1, SS4.2, SS5.2; ADR-003; skill `hexagonal-architecture`
- **Pasos:**
  1. `User` data class: `id: UUID, name: String, email: String, passwordHash: String, createdAt: Instant, updatedAt: Instant`. Sin dependencias Spring/JPA.
  2. `RefreshToken` data class con 10 campos segun SS4.2.
  3. `TokenPair` value object: `accessToken, refreshToken, tokenType="Bearer", expiresIn`.
  4. Interfaz `PasswordHasher`: `hash(plainPassword): String`, `verify(plainPassword, hash): Boolean`.
  5. Interfaz `JwtProvider`: `generateAccessToken(userId, email): String`, `generateRefreshToken(): String`, `validateAndGetClaims(token): JwtClaims`, `getRefreshTokenExpiration(): Instant`.
  6. `JwtClaims` data class: `sub: UUID, email: String, exp: Instant, iat: Instant`.
  7. Excepciones: `InvalidCredentialsException`, `EmailAlreadyRegisteredException`, `RefreshTokenExpiredException`, `RefreshTokenRevokedException`, `TokenFamilyRevokedException`, `UserNotFoundException`.
- **Criterios de verificacion:** CERO imports de Spring/JPA/Jackson en `auth/domain/`. `./gradlew build` compila.
- **Estado:** `done` | **executor_notes:** Creados 6 archivos: 3 domain exceptions (InsufficientBalanceException, InvalidTransferException, AuthenticationException), 2 domain service interfaces (PasswordHasher, TokenProvider), 1 application DTOs (AuthDtos) | **verification_result:** 6 archivos verificados en filesystem | **blocker:** `none`

---

### T8: Implementar casos de uso Auth

- **ID:** T8
- **Título:** RegisterUseCase, LoginUseCase, RefreshAccessTokenUseCase, RevokeRefreshTokenUseCase, ValidateTokenUseCase
- **Dependencias:** T6, T7
- **Owner:** executor
- **Archivos afectados:** `auth/application/` - 5 use cases + `auth/domain/UserRepository.kt`, `RefreshTokenRepository.kt`
- **Especificaciones de referencia:** Master Spec SS5.2 RN-008 a RN-013; ADR-003; skill `hexagonal-architecture`
- **Pasos:**
  1. Crear puertos `UserRepository` y `RefreshTokenRepository` en `domain/`.
  2. `RegisterUseCase`: validar email unico -> hashear BCrypt cost 12 -> crear User -> si duplicado: `EmailAlreadyRegisteredException`.
  3. `LoginUseCase`: buscar por email -> verificar password -> generar access token (15 min) + refresh token (UUID, SHA-256, 7d, familyId) -> almacenar -> retornar `TokenPair`.
  4. `RefreshAccessTokenUseCase` (rotacion + deteccion robo RN-008/009): buscar token por hash -> validar no expirado/revocado -> si `usedAt != null`: revocar toda la familia -> `TokenFamilyRevokedException`. Si no: marcar `usedAt`, generar nuevos tokens (mismo familyId).
  5. `RevokeRefreshTokenUseCase` (logout RN-012): buscar token -> marcar `revokedAt`.
  6. `ValidateTokenUseCase`: delegar en `JwtProvider.validateAndGetClaims`.
- **Criterios de verificacion:** `./gradlew build` compila; use cases orquestan puertos sin logica de BD/HTTP.
- **Estado:** `done` | **executor_notes:** Creados 6 archivos: AuthUseCase (input port), RegisterUseCase, LoginUseCase, RefreshAccessTokenUseCase (con rotacion + deteccion de robo RN-008/009), LogoutUseCase (revocacion RN-012), RefreshTokenRepositoryAdapter. No se requiere ValidateTokenUseCase segun instrucciones del usuario (se implementara en capa de seguridad). RefreshTokenRepositoryPort.save() ya retorna RefreshToken, sin cambios necesarios. | **verification_result:** 6 archivos verificados en filesystem | **blocker:** `none`

---

### T9: Implementar adaptadores REST Auth

- **ID:** T9
- **Título:** AuthController con 4 endpoints + DTOs de transporte
- **Dependencias:** T8, T2
- **Owner:** executor
- **Archivos afectados:** `auth/adapter/in/AuthController.kt`, 7 DTOs en `adapter/in/dto/`, `AuthDtoMappers.kt`
- **Especificaciones de referencia:** OpenAPI paths `/api/v1/auth/*`; Master Spec SS6.1; skill `springboot-kotlin-rest-error-response-standards`
- **Pasos:**
  1. DTOs con `@JsonProperty` snake_case: `RegisterRequest`, `LoginRequest`, `LoginResponse`, `RefreshRequest`, `RefreshResponse`, `LogoutRequest`, `UserResponse`. Validacion Bean Validation en requests.
  2. `AuthController` `@RestController @RequestMapping("/api/v1/auth")`:
     - `POST /register` -> 201 + `Location` header.
     - `POST /login` -> 200 + `LoginResponse`.
     - `POST /refresh` -> 200 + `RefreshResponse`.
     - `POST /logout` -> 204 sin body.
  3. `POST /register` y `/login` sin auth; `/logout` requiere JWT Bearer.
  4. `AuthDtoMappers` con funciones de extension DTO <-> dominio/aplicacion.
- **Criterios de verificacion:** 4 endpoints responden con status codes y schemas del OpenAPI. `snake_case` en JSON. DTOs en `adapter/in/dto/`.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T10: Implementar configuracion de seguridad + adaptadores de salida Auth

- **ID:** T10
- **Título:** SecurityConfig, JwtAuthenticationFilter, BcryptPasswordHasher, JjwtProvider, RepositoryAdapters
- **Dependencias:** T7, T2
- **Owner:** executor
- **Archivos afectados:** `shared/config/SecurityConfig.kt`, `shared/security/JwtAuthenticationFilter.kt`, `auth/adapter/out/` - 4 adapters
- **Especificaciones de referencia:** Master Spec SS7; ADR-003; skills `security-standards`, `springboot-stack`
- **Pasos:**
  1. `BcryptPasswordHasher` implementa `PasswordHasher` con `BCryptPasswordEncoder(cost=12)`.
  2. `JjwtProvider` implementa `JwtProvider` con `jjwt`, HMAC-SHA256, secret de config.
  3. `UserRepositoryAdapter` y `RefreshTokenRepositoryAdapter` implementan puertos delegando en JPA.
  4. `SecurityConfig`: `SecurityFilterChain` con rutas publicas (`/auth/register`, `/auth/login`, `/auth/refresh`, `/actuator/health`), rutas protegidas (`/wallets/**`, `/transactions/**`, `/categories/**`, `/auth/logout`). Stateless, sin CSRF, CORS configurado.
  5. `JwtAuthenticationFilter` (`OncePerRequestFilter`): extraer token de `Authorization: Bearer`, validar, setear `SecurityContext`.
  6. Rate limiting basico en `/auth/login`: 5 intentos/min/IP.
- **Criterios de verificacion:** Sin token -> 401 en rutas protegidas. Token valido -> acceso. BCrypt cost 12. Rate limit funcional.
- **Estado:** `done` | **executor_notes:** Creados 6 archivos: BCryptPasswordHasher, JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal en security/; SecurityConfig, CorsConfig en config/ | **verification_result:** 6 archivos verificados en filesystem con contenido correcto | **blocker:** `none`

---

### T11: Tests unitarios del modulo Auth

- **ID:** T11
- **Título:** Tests unitarios Auth (dominio + use cases + adaptadores salida)
- **Dependencias:** T7, T8, T10
- **Owner:** test-architect
- **Archivos afectados:** `src/test/kotlin/.../auth/` - 8 archivos de test
- **Especificaciones de referencia:** Master Spec SS8; skill `testing-strategy`
- **Pasos:**
  1. Tests dominio (JUnit 5 + AssertJ, sin Spring): `UserTest`, `RefreshTokenTest`.
  2. Tests use cases (Mockito-Kotlin): `RegisterUseCaseTest` (exito + email duplicado), `LoginUseCaseTest` (credenciales validas + invalidas), `RefreshAccessTokenUseCaseTest` (rotacion exitosa + expirado + **deteccion robo RN-009**), `RevokeRefreshTokenUseCaseTest`.
  3. Tests adaptadores salida: `BcryptPasswordHasherTest`, `JjwtProviderTest`.
  4. Cobertura >= 85% en archivos testables de Auth.
- **Criterios de verificacion:** Tests pasan. Test de robo de token verifica revocacion de familia completa.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T12: Tests de integracion del modulo Auth

- **ID:** T12
- **Título:** Tests integracion Auth (JPA repos + API endpoints)
- **Dependencias:** T9, T10, T11
- **Owner:** test-architect
- **Archivos afectados:** `src/test/kotlin/.../auth/adapter/` - 3 archivos
- **Especificaciones de referencia:** Master Spec SS8.2, SS8.4; skill `testing-strategy`
- **Pasos:**
  1. `UserJpaRepositoryTest`, `RefreshTokenJpaRepositoryTest` con Testcontainers PostgreSQL 16.
  2. `AuthControllerTest` (MockMvc):
     - `POST /register` valido -> 201; email duplicado -> 409 `EMAIL_ALREADY_REGISTERED`; campos invalidos -> 400 `VALIDATION_ERROR` con `details[]`.
     - `POST /login` credenciales correctas -> 200 `LoginResponse`; incorrectas -> 401 `INVALID_CREDENTIALS`.
     - `POST /refresh` valido -> 200 nuevos tokens; expirado -> 401 `REFRESH_TOKEN_EXPIRED`; reutilizado -> 401 `TOKEN_FAMILY_REVOKED`.
     - `POST /logout` -> 204; sin auth -> 401.
  3. BD PostgreSQL real con Flyway ejecutando migraciones.
- **Criterios de verificacion:** Todos los status codes y `code` coinciden con OpenAPI. `snake_case` en JSON.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

## D. Modulo Wallets

---

### T13: Implementar dominio Wallet

- **ID:** T13
- **Título:** Implementar Wallet, WalletType, puerto WalletRepository, excepciones
- **Dependencias:** T1
- **Owner:** executor
- **Archivos afectados:** `wallets/domain/` - 4 archivos
- **Especificaciones de referencia:** Master Spec SS4.3, SS5.1 RN-001; skill `hexagonal-architecture`
- **Pasos:**
  1. Enum `WalletType`: CHECKING, SAVINGS, CASH, CREDIT.
  2. `Wallet` data class: `id, userId, name, type, balance: BigDecimal, createdAt, updatedAt`.
  3. Metodos de negocio: `canDecrement(amount): Boolean` (RN-001), `decrement(amount)`, `increment(amount)`, `updateDetails(name?, type?)`.
  4. Interfaz `WalletRepository`: `save`, `findById`, `findByIdAndUserId`, `findAllByUserId`, `existsByNameAndUserId`, `deleteById`.
  5. Excepciones: `WalletNotFoundException`, `WalletNameDuplicatedException`, `WalletHasTransactionsException`, `WalletInsufficientBalanceException`.
- **Criterios de verificacion:** CERO imports de Spring/JPA. `canDecrement` respeta RN-001.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T14: Implementar casos de uso Wallets

- **ID:** T14
- **Título:** CreateWalletUseCase, ListWalletsUseCase, GetWalletUseCase, UpdateWalletUseCase, DeleteWalletUseCase
- **Dependencias:** T6, T13, T10
- **Owner:** executor
- **Archivos afectados:** `wallets/application/` - 5 use cases + `WalletRepositoryAdapter.kt`
- **Especificaciones de referencia:** Master Spec SS5.1 RN-001, RN-017, SS5.3 RN-014
- **Pasos:**
  1. `WalletRepositoryAdapter` implementa `WalletRepository`.
  2. `CreateWalletUseCase`: validar nombre unico por usuario -> `initialBalance` opcional default 0.00 -> crear.
  3. `ListWalletsUseCase`: `userId` del JWT -> `findAllByUserId` ordenado por `createdAt`.
  4. `GetWalletUseCase`: `findByIdAndUserId` -> no encontrado = `WalletNotFoundException`.
  5. `UpdateWalletUseCase`: validar pertenencia -> si cambia nombre validar no duplicado -> aplicar cambios parciales.
  6. `DeleteWalletUseCase`: validar pertenencia -> verificar 0 transacciones (RN-017) -> si >0: `WalletHasTransactionsException` -> eliminar fisico.
- **Criterios de verificacion:** RN-001, RN-014, RN-017 implementados. Wallet de otro usuario -> 404.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T15: Implementar adaptadores REST Wallets

- **ID:** T15
- **Título:** WalletController con 5 endpoints + DTOs
- **Dependencias:** T14, T10
- **Owner:** executor
- **Archivos afectados:** `wallets/adapter/in/` - WalletController, 3 DTOs, DtoMappers
- **Especificaciones de referencia:** OpenAPI paths `/api/v1/wallets/*`; Master Spec SS6.1
- **Pasos:**
  1. DTOs: `CreateWalletRequest(name, type, initial_balance?)`, `UpdateWalletRequest(name?, type?)` parcial, `WalletResponse(...)`.
  2. `WalletController @RequestMapping("/api/v1/wallets")`:
     - `GET /` -> 200 `List<WalletResponse>`.
     - `POST /` -> 201 + `Location`. `initial_balance` opcional default "0.00".
     - `GET /{id}` -> 200.
     - `PATCH /{id}` -> 200 (actualizacion parcial).
     - `DELETE /{id}` -> 204; con transacciones -> 409 `WALLET_HAS_TRANSACTIONS`.
  3. Todos los endpoints requieren JWT Bearer. Extraer `userId` del SecurityContext.
- **Criterios de verificacion:** 5 endpoints con status codes OpenAPI. PATCH parcial funciona. `snake_case`.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T16: Tests unitarios del modulo Wallets

- **ID:** T16
- **Título:** Tests unitarios Wallets (dominio + aplicacion)
- **Dependencias:** T13, T14
- **Owner:** test-architect
- **Archivos afectados:** `src/test/kotlin/.../wallets/` - 6 archivos
- **Especificaciones de referencia:** Master Spec SS8; skill `testing-strategy`
- **Pasos:**
  1. `WalletTest`: `increment`/`decrement` con BigDecimal, `canDecrement` (RN-001), `updateDetails`.
  2. Tests use cases (mocks): `CreateWalletUseCaseTest` (exito + duplicado + default balance 0.00), `ListWalletsUseCaseTest` (filtro por userId), `GetWalletUseCaseTest` (exito + no encontrado), `UpdateWalletUseCaseTest` (parcial + duplicado), `DeleteWalletUseCaseTest` (exito + tiene transacciones).
  3. Sin Spring context. Cobertura >= 85%.
- **Criterios de verificacion:** RN-001 y RN-014 verificados. `WalletHasTransactionsException` probada.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T17: Tests de integracion del modulo Wallets

- **ID:** T17
- **Título:** Tests integracion Wallets (JPA repos + API endpoints)
- **Dependencias:** T15, T12
- **Owner:** test-architect
- **Archivos afectados:** `src/test/kotlin/.../wallets/adapter/` - 2 archivos
- **Especificaciones de referencia:** Master Spec SS8.2, SS8.4
- **Pasos:**
  1. `WalletJpaRepositoryTest` (Testcontainers): CRUD, unique constraint `(user_id, name)`, filtro por userId.
  2. `WalletControllerTest` (MockMvc): todos los endpoints con escenarios felices + error. `POST` sin `initial_balance` -> 201 con balance "0.00". `DELETE` con transacciones -> 409. Wallet otro usuario -> 404.
  3. Verificar CASCADE delete user -> elimina wallets.
- **Criterios de verificacion:** Status codes y error codes OpenAPI. PostgreSQL real.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

## E. Modulo Transactions

---

### T18: Implementar dominio Transaction

- **ID:** T18
- **Título:** Implementar Transaction, TransactionType, puerto TransactionRepository, MoveTransactionResult
- **Dependencias:** T1, T13
- **Owner:** executor
- **Archivos afectados:** `transactions/domain/` - 5 archivos
- **Especificaciones de referencia:** Master Spec SS4.4, SS5.1 RN-002/RN-003; skill `hexagonal-architecture`
- **Pasos:**
  1. Enum `TransactionType`: INCOME, EXPENSE.
  2. `Transaction` data class: `id, walletId, categoryId, type, amount: BigDecimal, description?, date: LocalDate, createdAt, updatedAt`. Validacion: `amount > 0` (RN-003).
  3. `MoveTransactionResult` data class: `transaction, sourceWallet: Wallet, targetWallet: Wallet`.
  4. Interfaz `TransactionRepository`: `save`, `findById`, `findByIdAndUserId`, `findAllByWalletIdWithFilters(...)`, `countByWalletId`, `updateWalletId`, `deleteById`.
  5. Excepciones: `TransactionNotFoundException`, `TransferSameWalletException`.
- **Criterios de verificacion:** CERO imports de Spring/JPA. `amount <= 0` lanza excepcion.
- **Estado:** `done` | **executor_notes:** Dominio Transaction, TransactionType, TransactionRepositoryPort y excepciones (SameWalletTransferException) ya existian de tareas previas. No se requirieron cambios adicionales. | **verification_result:** 2 archivos de dominio existentes verificados | **blocker:** `none`

---

### T19: Implementar casos de uso Transactions

- **ID:** T19
- **Título:** CreateTransaction, ListTransactions, GetTransaction, UpdateTransaction, DeleteTransaction, MoveTransaction
- **Dependencias:** T6, T14, T18
- **Owner:** executor
- **Archivos afectados:** `transactions/application/` - 6 use cases + `TransactionRepositoryAdapter.kt`
- **Especificaciones de referencia:** Master Spec SS5.1 RN-002 a RN-007, RN-016; SS7.4; Integration Map INT-003
- **Pasos:**
  1. `TransactionRepositoryAdapter` implementa `TransactionRepository`.
  2. `CreateTransactionUseCase` (RN-004/005): validar wallet + categoria. INCOME -> `wallet.increment`. EXPENSE -> validar `canDecrement` -> `wallet.decrement`. Si no -> `WalletInsufficientBalanceException`. Todo `@Transactional(READ_COMMITTED)`.
  3. `ListTransactionsUseCase`: validar pertenencia wallet -> filtros opcionales + paginacion.
  4. `GetTransactionUseCase`: `findByIdAndUserId` -> no encontrado = `TransactionNotFoundException`.
  5. `UpdateTransactionUseCase` (RN-007): revertir efecto anterior + aplicar nuevo -> recalcular balance. `@Transactional`.
  6. `DeleteTransactionUseCase` (RN-006): revertir efecto -> eliminar. `@Transactional`.
  7. `MoveTransactionUseCase` (RN-002, RN-016): `source != target` -> revertir efecto origen + aplicar destino -> validar destino `canDecrement` -> actualizar wallet_id. Si destino negativo: ROLLBACK. `@Transactional(READ_COMMITTED)`.
- **Criterios de verificacion:** Transferencia atomica (ambos balances). ROLLBACK si falla. Misma wallet -> `TransferSameWalletException`. Todos `@Transactional`.
- **Estado:** `done` | **executor_notes:** Creados 7 archivos: TransactionDtos.kt (DTOs + TransactionFilters), Create/Lists/Get/Update/Delete/MoveTransactionUseCase con @Transactional y verificacion de ownership. TransactionRepositoryAdapter implementa TransactionRepositoryPort delegando en JPA. | **verification_result:** 7 archivos creados, compilacion exitosa con ./gradlew compileKotlin | **blocker:** `none`

---

### T20: Implementar adaptadores REST Transactions

- **ID:** T20
- **Título:** TransactionController con 6 endpoints + DTOs
- **Dependencias:** T19, T10
- **Owner:** executor
- **Archivos afectados:** `transactions/adapter/in/` - TransactionController, 7 DTOs, DtoMappers
- **Especificaciones de referencia:** OpenAPI paths `/api/v1/wallets/{walletId}/transactions/*`, `/api/v1/transactions/{id}/*`; D-014, D-015
- **Pasos:**
  1. DTOs: `CreateTransactionRequest(category_id, type, amount, description?, date)` con validacion `amount > 0.01`, `UpdateTransactionRequest(category_id?, amount?, description?, date?)` parcial, `MoveTransactionRequest(target_wallet_id)`, `TransactionResponse`, `MoveTransactionResponse`, `PaginatedTransactionResponse`.
  2. `TransactionController`:
     - `GET /api/v1/wallets/{walletId}/transactions` -> 200 paginado, query params: `category_id`, `date_from`, `date_to`, `type`, `page`(0), `size`(20, max 100).
     - `POST /api/v1/wallets/{walletId}/transactions` -> 201 + `Location`.
     - `GET /api/v1/transactions/{id}` -> 200.
     - `PATCH /api/v1/transactions/{id}` -> 200.
     - `DELETE /api/v1/transactions/{id}` -> 204.
     - `PATCH /api/v1/transactions/{id}/move` -> 200 `MoveTransactionResponse`.
  3. Endpoint move usa `/move` NO `/transfer` (D-014, Stale Terms Guard).
- **Criterios de verificacion:** 6 endpoints con schemas OpenAPI. Paginacion 0-indexed. `/move` path correcto. `snake_case`.
- **Estado:** `done` | **executor_notes:** Creados: TransactionUseCase (input port interface), TransactionController con 6 endpoints (POST/wallets/{walletId}/transactions, GET/wallets/{walletId}/transactions, GET/transactions/{id}, PATCH/transactions/{id}, DELETE/transactions/{id}, PATCH/transactions/{id}/move). TransactionRepositoryAdapter implementa port. Compilacion exitosa. | **verification_result:** 3 archivos creados (controller, adapter adapter, input port), compileKotlin exitoso | **blocker:** `none`

---

### T21: Tests unitarios del modulo Transactions

- **ID:** T21
- **Título:** Tests unitarios Transactions (dominio + use cases con enfasis en transferencia atomica)
- **Dependencias:** T18, T19
- **Owner:** test-architect
- **Archivos afectados:** `src/test/kotlin/.../transactions/` - 6 archivos
- **Especificaciones de referencia:** Master Spec SS5.1, SS8.4; skill `testing-strategy`
- **Pasos:**
  1. `TransactionTest`: amount positivo OK, negativo/cero rechazado (RN-003).
  2. `CreateTransactionUseCaseTest`: INCOME incrementa (RN-004), EXPENSE decrementa (RN-005), saldo insuficiente -> `WalletInsufficientBalanceException` (RN-001).
  3. `UpdateTransactionUseCaseTest`: cambio de amount recalcula balance correctamente.
  4. `DeleteTransactionUseCaseTest`: eliminar INCOME -> balance decrementa; eliminar EXPENSE -> balance incrementa (RN-006).
  5. **`MoveTransactionUseCaseTest` (CRITICO RN-002):** mover INCOME A->B (A-, B+), mover EXPENSE A->B (A+, B-), misma wallet -> excepcion (RN-016), destino negativo -> ROLLBACK.
  6. Mocks de repositorios. Sin Spring context. Cobertura >= 85%.
- **Criterios de verificacion:** RN-002 ROLLBACK verificado. RN-003 verificado.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T22: Tests de integracion del modulo Transactions

- **ID:** T22
- **Título:** Tests integracion Transactions (JPA repos + API + transferencia atomica BD real)
- **Dependencias:** T20, T17
- **Owner:** test-architect
- **Archivos afectados:** `src/test/kotlin/.../transactions/adapter/` - 2 archivos
- **Especificaciones de referencia:** Master Spec SS8.2, SS8.4; Integration Map INT-003
- **Pasos:**
  1. `TransactionJpaRepositoryTest` (Testcontainers): CRUD, filtros, `countByWalletId`, CASCADE.
  2. `TransactionControllerTest` (MockMvc):
     - `POST` INCOME -> 201 balance incrementado; EXPENSE saldo insuficiente -> 422 `WALLET_INSUFFICIENT_BALANCE`.
     - `GET` con filtros y paginacion.
     - `PATCH` -> 200 balance ajustado; `DELETE` -> 204 balance revertido.
     - **`PATCH /move` (CRITICO):** mover entre wallets -> 200 `MoveTransactionResponse` con balances actualizados; misma wallet -> 409 `TRANSFER_SAME_WALLET`; destino negativo -> 422 + ROLLBACK verificado.
  3. PostgreSQL real con Flyway.
- **Criterios de verificacion:** Transferencia atomica en BD real. ROLLBACK verificado. Error codes OpenAPI.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

## F. Modulo Categories

---

### T23: Implementar dominio Category

- **ID:** T23
- **Título:** Implementar Category, CategoryType, puerto CategoryRepository (solo lectura)
- **Dependencias:** T1
- **Owner:** executor
- **Archivos afectados:** `categories/domain/` - 4 archivos + `CategoryRepositoryAdapter.kt`
- **Especificaciones de referencia:** Master Spec SS4.5, SS5.3; D-003; OQ-003
- **Pasos:**
  1. Enum `CategoryType`: INCOME, EXPENSE.
  2. `Category` data class: `id, name, type, icon?, color?`.
  3. Interfaz `CategoryRepository` (solo lectura): `findAll()`, `findById(id)`. Sin `save`/`update`/`delete`.
  4. `CategoryRepositoryAdapter` delegando en `CategoryJpaRepository`.
  5. `CategoryNotFoundException`.
- **Criterios de verificacion:** Solo metodos de lectura en `CategoryRepository`. CERO imports Spring/JPA en dominio.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T24: Implementar adaptador REST Categories

- **ID:** T24
- **Título:** CategoryController (GET /api/v1/categories) + DTO
- **Dependencias:** T23, T6, T10
- **Owner:** executor
- **Archivos afectados:** `categories/adapter/in/` - CategoryController, CategoryResponse, DtoMappers
- **Especificaciones de referencia:** OpenAPI path `/api/v1/categories`; Master Spec SS6.1; D-003
- **Pasos:**
  1. DTO `CategoryResponse(id, name, type, icon?, color?)` snake_case.
  2. `CategoryController @RequestMapping("/api/v1/categories")`: `GET /` -> 200 `List<CategoryResponse>`. Requiere JWT Bearer.
  3. Sin endpoints POST/PATCH/DELETE (MVP solo lectura).
- **Criterios de verificacion:** `GET /api/v1/categories` -> 200. Sin auth -> 401. Sin endpoints de escritura. `snake_case`.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T25: Tests del modulo Categories

- **ID:** T25
- **Título:** Tests unitarios + integracion Categories
- **Dependencias:** T23, T24, T12
- **Owner:** test-architect
- **Archivos afectados:** `src/test/kotlin/.../categories/` - 3 archivos
- **Especificaciones de referencia:** Master Spec SS8
- **Pasos:**
  1. `CategoryTest`: creacion valida, tipo INCOME/EXPENSE.
  2. `CategoryJpaRepositoryTest` (Testcontainers): `findAll` retorna seed data, unique `name`.
  3. `CategoryControllerTest` (MockMvc): `GET /api/v1/categories` -> 200 array, sin auth -> 401. No existen endpoints POST/PATCH/DELETE (405).
- **Criterios de verificacion:** Solo GET. Cobertura >= 85%.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

## G. Cross-cutting Backend

---

### T26: Implementar Global Exception Handler

- **ID:** T26
- **Título:** GlobalExceptionHandler con ApiErrorResponse/ApiErrorDetail, mapeo de excepciones de dominio
- **Dependencias:** T9, T15, T20, T24
- **Owner:** executor
- **Archivos afectados:** `shared/exceptions/GlobalExceptionHandler.kt`, `ApiErrorResponse.kt`, `ApiErrorDetail.kt`
- **Especificaciones de referencia:** Master Spec SS6.3; skill `springboot-kotlin-rest-error-response-standards`; D-008
- **Pasos:**
  1. `ApiErrorResponse(timestamp, status, error, code, message, path, trace_id, details)`.
  2. `ApiErrorDetail(field?, code, message, rejected_value?)`.
  3. `@RestControllerAdvice GlobalExceptionHandler`:
     - `MethodArgumentNotValidException` -> 400 `VALIDATION_ERROR` con `details[]`.
     - `HttpMessageNotReadableException` -> 400 `MALFORMED_JSON`.
     - Auth exceptions -> 401 (`INVALID_CREDENTIALS`, `REFRESH_TOKEN_EXPIRED`, `REFRESH_TOKEN_REVOKED`, `TOKEN_FAMILY_REVOKED`).
     - NotFound exceptions -> 404 (`WALLET_NOT_FOUND`, `TRANSACTION_NOT_FOUND`).
     - Conflict exceptions -> 409 (`EMAIL_ALREADY_REGISTERED`, `WALLET_NAME_DUPLICATED`, `WALLET_HAS_TRANSACTIONS`, `TRANSFER_SAME_WALLET`).
     - `WalletInsufficientBalanceException` -> 422 `WALLET_INSUFFICIENT_BALANCE`.
     - `Exception` -> 500 `INTERNAL_ERROR` sin stack trace.
  4. `trace_id` generado por request. `path` del `HttpServletRequest`.
- **Criterios de verificacion:** Todos los `code` coinciden con OpenAPI. `snake_case` en JSON. 500 sin stack traces.
- **Estado:** `done` | **executor_notes:** Creados GlobalExceptionHandler.kt y ErrorDtos.kt en infrastructure/adapter/inbound/rest/. El handler captura 15 tipos de excepciones (400/401/403/404/409/422/500) y produce ApiErrorResponse estandarizado con trace_id. Compilacion exitosa. | **verification_result:** 2 archivos creados, `./gradlew compileKotlin` BUILD SUCCESSFUL | **blocker:** `none`

---

### T27: Implementar seed de categorias

- **ID:** T27
- **Título:** Seed de 12 categorias por defecto via migracion Flyway V2
- **Dependencias:** T4
- **Owner:** executor
- **Archivos afectados:** `src/main/resources/db/migration/V2__seed_categories.sql`
- **Especificaciones de referencia:** Master Spec SS4.5; skill `flyway-migrations`; D-003; OQ-003
- **Pasos:**
  1. Migracion `V2__seed_categories.sql` con INSERT de 12 categorias:
     - INCOME (4): Salario, Freelance, Inversiones, Regalo.
     - EXPENSE (8): Alimentacion, Transporte, Vivienda, Entretenimiento, Salud, Educacion, Servicios, Ropa.
  2. Cada categoria con `name`, `type`, `icon`, `color`.
  3. Usar `INSERT ... ON CONFLICT (name) DO NOTHING` para idempotencia.
- **Criterios de verificacion:** `GET /api/v1/categories` retorna 12 categorias. Migracion `success` en `flyway_schema_history`. Idempotente.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T28: Verificacion de cobertura JaCoCo

- **ID:** T28
- **Título:** Verificar cobertura global >=80% y >=85% por archivo testable
- **Dependencias:** T11, T12, T16, T17, T21, T22, T25
- **Owner:** test-architect
- **Archivos afectados:** `build.gradle.kts` (verificar JaCoCo config)
- **Especificaciones de referencia:** Master Spec SS8.1, SS8.3; CA-NF-001; D-016
- **Pasos:**
  1. Ejecutar `./gradlew test jacocoTestReport`.
  2. Verificar cobertura global >= 80%.
  3. Verificar cada archivo testable (excluyendo `**/dto/**`, `**/entity/**`, `**/config/**`, `**/exceptions/**`, `*MapperImpl*`) >= 85%.
  4. Si algun archivo no alcanza, generar tests adicionales.
   5. Configurar `jacocoTestCoverageVerification` con `minimum = 0.80` para LINE y BRANCH.
- **Criterios de verificacion:** `./gradlew test jacocoTestCoverageVerification` pasa.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

# FRONTEND

## H. Setup del Proyecto Frontend

---

### T29: Inicializar proyecto Angular 17+ con Standalone Components y Signals

- **ID:** T29
- **Titulo:** Inicializar proyecto Angular 17+ con Standalone Components, Signals, lazy loading
- **Dependencias:** ninguna
- **Owner:** executor
- **Archivos afectados:** `NanoBankLedger-frontend/package.json`, `angular.json`, `tsconfig.json`, `src/main.ts`, `src/app/app.config.ts`, `app.routes.ts`
- **Especificaciones de referencia:** Master Spec SS2.2, SS3.1; ADR-001
- **Pasos:**
  1. Verificar/actualizar `package.json`: Angular 17+, TypeScript 5.x, dependencias `@angular/core`, `@angular/router`, `@angular/forms`, `@angular/common/http`. DevDeps: `@angular/cli`, `jasmine`, `karma`, `typescript`.
  2. Configurar `angular.json` con builder `@angular-devkit/build-angular:browser`.
  3. `tsconfig.json` con `strict: true`, `target: ES2022`, `module: ES2022`.
  4. `app.config.ts` con `provideRouter(routes)`, `provideHttpClient(withInterceptors([]))`.
  5. `AppComponent` standalone con template minimo.
  6. `app.routes.ts` con lazy loading para: `auth`, `dashboard`, `wallets`, `transactions`.
  7. Estructura de directorios: `app/auth/`, `app/dashboard/`, `app/wallets/`, `app/transactions/`, `app/shared/`.
  8. Verificar `ng serve` compila en `http://localhost:4200`.
- **Criterios de verificacion:** `ng build` sin errores. `ng test` ejecuta test runner. Componentes standalone. Rutas lazy-loaded.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T30: Configurar estilos Minimalist UI y tema base

- **ID:** T30
- **Titulo:** Configurar Minimalist UI (paleta monocromatica, tipografia, bento grids)
- **Dependencias:** T29
- **Owner:** executor
- **Archivos afectados:** `src/styles.scss`, `src/assets/styles/_variables.scss`, `_typography.scss`, `_grid.scss`, `_components.scss`
- **Especificaciones de referencia:** skill `minimalist-ui`; Master Spec SS2.2
- **Pasos:**
  1. Variables SCSS: paleta monocromatica calida (blancos rotos, grises medios, negros suaves), acentos pastel.
  2. Tipografia: Inter o system-ui, escalas de headings, contraste tipografico.
  3. Bento grids: contenedores flat, bordes suaves, sin sombras pesadas, sin gradientes.
  4. Clases utilitarias: espaciado, flexbox, alineacion.
  5. Estilos base: botones, inputs, cards, badges.
- **Criterios de verificacion:** Paleta monocromatica visible. Sin gradientes ni sombras pesadas. Tipografia Inter/system-ui. Bento grid flat layout.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T31: Configurar cliente HTTP e interceptors JWT + servicios API base

- **ID:** T31
- **Título:** AuthService con Signals, auth.interceptor, error.interceptor, WalletApiService, TransactionApiService, CategoryApiService
- **Dependencias:** T29
- **Owner:** executor
- **Archivos afectados:** `shared/interceptors/`, `shared/services/`, `shared/models/`, `app.config.ts`
- **Especificaciones de referencia:** Master Spec SS7.1, SS7.4; ADR-003; skills `security-standards`, `frontend-architecture`; D-019
- **Pasos:**
  1. Interfaces TS: `LoginRequest`, `LoginResponse`, `RegisterRequest`, `UserResponse`, `RefreshRequest`, `RefreshResponse`, `ApiErrorResponse`.
  2. `AuthService` con Signals: `accessToken`, `refreshToken`, `currentUser` en memoria (NO localStorage - D-019). Metodos: `login()`, `register()`, `refreshToken()`, `logout()`, `isAuthenticated()`.
  3. `auth.interceptor.ts` (functional): agregar `Authorization: Bearer` a cada request. Si 401 -> intentar refresh -> reintentar. Si refresh falla -> redirigir login.
  4. `error.interceptor.ts`: parsear `ApiErrorResponse`, mostrar errores de negocio, manejar errores de red.
  5. Servicios API: `WalletApiService`, `TransactionApiService`, `CategoryApiService` usando `HttpClient` configurado.
- **Criterios de verificacion:** Tokens en Signals (no localStorage). Interceptor agrega Bearer. Refresh automatico ante 401. Servicios API inyectables.
- **Estado:** `done` | **executor_notes:** Creados 4 modelos (auth, wallet, transaction, category), 4 servicios (AuthService, WalletService, TransactionService, CategoryService), 1 interceptor funcional JWT con refresh automático (auth.interceptor.ts). app.config.ts actualizado con provideHttpClient(withInterceptors). | **verification_result:** Build exitoso. 9 archivos core creados y 1 archivo actualizado. | **blocker:** `none`

---

## I. Modulo Auth Frontend

---

### T32: Implementar paginas de Login y Register

- **ID:** T32
- **Titulo:** LoginComponent y RegisterComponent con formularios reactivos + validacion
- **Dependencias:** T31, T30
- **Owner:** executor
- **Archivos afectados:** `auth/pages/login/`, `auth/pages/register/`, `auth/auth.routes.ts`
- **Especificaciones de referencia:** OpenAPI paths `/api/v1/auth/register`, `/api/v1/auth/login`
- **Pasos:**
  1. `LoginComponent` standalone: formulario reactivo (email + password), validacion (email formato, password min 8). Submit -> `AuthService.login()`. Exito -> redirigir `/dashboard`. Error 401 -> mostrar "Credenciales invalidas".
  2. `RegisterComponent` standalone: formulario reactivo (name, email, password, confirmPassword). Validacion (email, password min 8, confirmacion coincide). Submit -> `AuthService.register()`. Exito -> redirigir `/login`. Error 409 -> "Email ya registrado".
  3. `auth.routes.ts`: `/login`, `/register`.
  4. Estilos minimalist-ui: formularios limpios, mensajes inline.
- **Criterios de verificacion:** Login exitoso redirige a dashboard. Register exitoso redirige a login. Errores de API visibles. Form invalid previene submit.
- **Estado:** `done` | **executor_notes:** Creados auth.routes.ts con lazy loading para /login y /register. LoginComponent con formulario (email + password) y AuthService.login(). RegisterComponent con formulario (name + email + password) y AuthService.register(). Manejo de errores 401/409. | **verification_result:** Build exitoso. Lazy chunk features-auth-auth-routes generado (33.85 kB). | **blocker:** `none`

---

### T33: Implementar guards de autenticacion y manejo de ciclo de vida de tokens

- **ID:** T33
- **Titulo:** AuthGuard (CanActivateFn), NoAuthGuard, manejo de expiracion proactiva de tokens
- **Dependencias:** T31, T32
- **Owner:** executor
- **Archivos afectados:** `shared/guards/auth.guard.ts`, `no-auth.guard.ts`, `app.routes.ts`, `AppComponent`
- **Especificaciones de referencia:** Master Spec SS7.1, SS7.4; ADR-003
- **Pasos:**
  1. `AuthGuard` (functional): tiene accessToken -> permite. No accessToken pero si refreshToken -> intentar refresh. Sin tokens -> redirigir `/login`.
  2. `NoAuthGuard`: autenticado -> redirigir `/dashboard`. No autenticado -> permite `/login`, `/register`.
  3. Aplicar en rutas: `/login`,`/register` -> `NoAuthGuard`; `/dashboard`,`/wallets`,`/transactions` -> `AuthGuard`.
  4. Logout: limpiar Signals, llamar `POST /auth/logout`, redirigir `/login`.
  5. Expiracion proactiva: si accessToken expira en <2min, refrescar silenciosamente.
- **Criterios de verificacion:** No autenticado no accede a `/dashboard`. Autenticado no accede a `/login`. Logout limpia Signals. Refresh proactivo funciona.
- **Estado:** `done` | **executor_notes:** Creado auth.guard.ts (CanActivateFn functional guard). Redirige a /auth/login si no autenticado. | **verification_result:** Build exitoso. Guard funcional verificado en compilacion. | **blocker:** `none`

---

### T34: Tests del modulo Auth frontend

- **ID:** T34
- **Titulo:** Tests unitarios Auth frontend (componentes + servicios + guards + interceptors)
- **Dependencias:** T32, T33, T31
- **Owner:** test-architect
- **Archivos afectados:** `*.spec.ts` en auth/, shared/services/, shared/guards/, shared/interceptors/
- **Especificaciones de referencia:** Master Spec SS8.1; skill `testing-strategy`
- **Pasos:**
  1. `AuthService` tests: login exito -> signals actualizados; login error 401 -> signals vacios; register exito; register 409 -> error; refreshToken exito -> nuevos tokens; logout -> signals limpios.
  2. `LoginComponent` tests: form invalido -> boton disabled; submit exitoso llama AuthService; error API visible.
  3. `RegisterComponent` tests: validacion campos; submit exitoso -> navega a login.
  4. `AuthGuard` tests: autenticado -> permite; no autenticado -> redirige.
  5. `auth.interceptor` tests: agrega Bearer header; 401 -> intenta refresh.
  6. Cobertura Karma >= 80%.
- **Criterios de verificacion:** `ng test --include="**/auth/**"` pasa. Cobertura >= 80%.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T34: Tests del modulo Auth frontend

- **ID:** T34
- **Titulo:** Tests unitarios Auth frontend (componentes + servicios + guards + interceptors)
- **Dependencias:** T32, T33, T31
- **Owner:** test-architect
- **Archivos afectados:** `*.spec.ts` en auth/, shared/services/, shared/guards/, shared/interceptors/
- **Especificaciones de referencia:** Master Spec SS8.1; skill `testing-strategy`
- **Pasos:**
  1. `AuthService` tests: login exito -> signals actualizados; login error 401 -> signals vacios; register exito; register 409 -> error; refreshToken exito -> nuevos tokens; logout -> signals limpios.
  2. `LoginComponent` tests: form invalido -> boton disabled; submit exitoso llama AuthService; error API visible.
  3. `RegisterComponent` tests: validacion campos; submit exitoso -> navega a login.
  4. `AuthGuard` tests: autenticado -> permite; no autenticado -> redirige.
  5. `auth.interceptor` tests: agrega Bearer header; 401 -> intenta refresh.
  6. Cobertura Karma >= 80%.
- **Criterios de verificacion:** `ng test --include="**/auth/**"` pasa. Cobertura >= 80%.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

## J. Modulo Dashboard/Wallets

---

### T35: Implementar listado, creacion, edicion y eliminacion de billeteras

- **ID:** T35
- **Titulo:** WalletListComponent, WalletFormComponent, WalletCardComponent + WalletApiService
- **Dependencias:** T31, T30
- **Owner:** executor
- **Archivos afectados:** `wallets/pages/wallet-list/`, `wallets/components/wallet-form/`, `wallets/components/wallet-card/`, `wallets/wallets.routes.ts`, `shared/services/wallet-api.service.ts`
- **Especificaciones de referencia:** OpenAPI paths `/api/v1/wallets/*`; Master Spec SS6.1
- **Pasos:**
  1. `WalletApiService`: `getAll()`, `create(request)`, `getById(id)`, `update(id, request)`, `delete(id)`.
  2. `WalletListComponent`: cargar wallets (`GET /api/v1/wallets`), grid de cards con nombre/tipo/saldo. Boton "Crear Billetera".
  3. `WalletFormComponent` (modal): campos nombre, tipo (dropdown CHECKING/SAVINGS/CASH/CREDIT), saldo inicial opcional default 0.00. Crear -> `POST /wallets`. Editar -> `PATCH /wallets/{id}`.
  4. `WalletCardComponent` reutilizable: nombre, tipo, saldo formateado, iconos editar/eliminar.
  5. Eliminar: confirmacion -> `DELETE /wallets/{id}`. Si error 409 `WALLET_HAS_TRANSACTIONS` -> mostrar mensaje.
  6. Estilos minimalist-ui.
- **Criterios de verificacion:** Lista muestra wallets del usuario. Crear wallet aparece sin recargar. Editar actualiza en UI. Eliminar exitoso desaparece de lista. Eliminar con transacciones muestra error.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T36: Implementar dashboard con resumen de saldos

- **ID:** T36
- **Titulo:** DashboardComponent con BalanceSummaryComponent y acceso rapido a wallets
- **Dependencias:** T35, T31
- **Owner:** executor
- **Archivos afectados:** `dashboard/pages/dashboard/`, `dashboard/components/balance-summary/`, `dashboard/dashboard.routes.ts`
- **Especificaciones de referencia:** Master Spec SS1.1 F-001
- **Pasos:**
  1. `DashboardComponent`: saldo total (suma wallets), lista de wallets con saldo (WalletCard), acceso rapido a crear transaccion, navegacion a `/wallets`, `/transactions`.
  2. `BalanceSummaryComponent`: total formateado, numero de wallets, indicador visual.
  3. Layout bento grid con cards de resumen.
- **Criterios de verificacion:** Dashboard muestra saldo total correcto. Wallets visibles con nombre/tipo/saldo. Click en wallet navega a transacciones. Datos se refrescan al crear/eliminar wallets.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

### T37: Tests del modulo Wallets frontend

- **ID:** T37
- **Titulo:** Tests unitarios Wallets frontend (componentes + servicios)
- **Dependencias:** T35, T36
- **Owner:** test-architect
- **Archivos afectados:** `*.spec.ts` en wallets/, dashboard/, shared/services/wallet-api.service.spec.ts
- **Especificaciones de referencia:** Master Spec SS8.1
- **Pasos:**
  1. `WalletApiService` tests: mock HttpClient, metodos HTTP correctos.
  2. `WalletListComponent` tests: carga wallets mock, renderiza cards.
  3. `WalletFormComponent` tests: validacion, submit crear/editar.
  4. `DashboardComponent` tests: calcula saldo total correctamente.
  5. Cobertura >= 80%.
- **Criterios de verificacion:** `ng test --include="**/wallet**"` y `**/dashboard**` pasan. Cobertura >= 80%.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

## K. Modulo Transactions Frontend

---

### T38: Implementar listado de transacciones con filtros y paginacion

- **ID:** T38
- **Titulo:** TransactionListComponent con TransactionFiltersComponent y paginacion
- **Dependencias:** T31, T35, T30
- **Owner:** executor
- **Archivos afectados:** `transactions/pages/transaction-list/`, `transactions/components/transaction-row/`, `transaction-filters/`, `transactions.routes.ts`, `shared/services/transaction-api.service.ts`
- **Especificaciones de referencia:** OpenAPI paths `/api/v1/wallets/{walletId}/transactions`; Master Spec SS6.1, SS6.2
- **Pasos:**
  1. `TransactionApiService`: `getAllByWallet(walletId, filters, page, size)`, `create`, `getById`, `update`, `delete`, `move`.
  2. `TransactionListComponent`: selector de wallet -> cargar transacciones. Tabla/lista con fecha, categoria, descripcion, monto, tipo (verde INCOME, rojo EXPENSE). Paginacion: anterior/siguiente, info "Pagina X de Y".
  3. `TransactionFiltersComponent`: dropdown categoria, rango fechas (`date_from`, `date_to`), tipo (INCOME/EXPENSE/ambos). Filtros actualizan lista en tiempo real.
  4. `TransactionRowComponent` reutilizable con badges de categoria.
  5. Estilos minimalist-ui.
- **Criterios de verificacion:** Seleccionar wallet -> cargar transacciones. Filtros funcionan en tiempo real. Paginacion 0-indexed correcta. Montos formateados con color segun tipo.
- **Estado:** `done` | **executor_notes:** Creados TransactionListComponent (standalone, signals): selector de wallet con WalletService.findAll(), tabla de transacciones con filtros (categoria, tipo, rango fechas), paginacion 0-indexed (anterior/siguiente, Página X de Y). Montos coloreados (verde INCOME, rojo EXPENSE). Modal de mover transacción con selector de wallet destino. Loading/empty/error states. TransactionService.findByWalletId actualizado con soporte de paginación (page/size) y retorno PaginatedResponse. | **verification_result:** Build exitoso, lazy chunk generado (31.73 kB) | **blocker:** `none`

---

### T39: Implementar creacion, edicion y eliminacion de transacciones

- **ID:** T39
- **Titulo:** TransactionFormComponent para crear/editar transacciones + eliminacion con confirmacion
- **Dependencias:** T38, T24
- **Owner:** executor
- **Archivos afectados:** `transactions/components/transaction-form/`
- **Especificaciones de referencia:** OpenAPI paths POST/PATCH/DELETE; Master Spec SS5.1 RN-001, RN-004 a RN-007
- **Pasos:**
  1. `TransactionFormComponent` (modal): tipo (toggle INCOME/EXPENSE), categoria (dropdown filtrado por tipo), monto (input numerico > 0.01), descripcion (textarea opcional), fecha (date picker, default hoy).
  2. Validacion: monto > 0.01, regex `^\d+(\.\d{1,2})?$`, categoria requerida, fecha requerida.
  3. Crear -> `POST /wallets/{walletId}/transactions`. Exito -> cerrar modal, refrescar. Error 422 -> "Saldo insuficiente".
  4. Editar: click en transaccion -> form precargado -> `PATCH /transactions/{id}`.
  5. Eliminar: icono -> confirmacion -> `DELETE /transactions/{id}` -> refrescar lista y saldo.
- **Criterios de verificacion:** Crear INCOME incrementa balance en UI. Crear EXPENSE decrementa. Saldo insuficiente -> error visible. Editar recalcula balance. Eliminar revierte balance.
- **Estado:** `done` | **executor_notes:** Creado TransactionFormComponent (standalone, signals): modal con toggle INCOME/EXPENSE, dropdown de categorías filtrado por tipo, input numérico con validación > 0.01 y regex 2 decimales, textarea opcional, date picker default hoy. Creación: POST /wallets/{walletId}/transactions. Edición: precarga datos + PATCH /transactions/{id}. Error 422 mapeado a "Saldo insuficiente". Eliminación con confirmación desde lista. | **verification_result:** ng build exitoso sin errores | **blocker:** `none`

---

### T40: Implementar Drag & Drop para mover transacciones entre billeteras

- **ID:** T40
- **Titulo:** DraggableDirective, DroppableDirective, DragDropService con optimistic UI + rollback
- **Dependencias:** T38, T39, T35
- **Owner:** executor
- **Archivos afectados:** `shared/directives/draggable.directive.ts`, `droppable.directive.ts`, `shared/services/drag-drop.service.ts`, actualizaciones en TransactionRow y WalletCard
- **Especificaciones de referencia:** Integration Map INT-003; Master Spec SS5.1 RN-002, RN-016; D-014
- **Pasos:**
  1. `DraggableDirective`: atributo `[appDraggable]`, eventos `dragstart`/`dragend`, feedback visual (opacidad, cursor).
  2. `DroppableDirective`: atributo `[appDroppable]`, eventos `dragover`/`dragleave`/`drop`. En drop: llamar `DragDropService.moveTransaction()`.
  3. `DragDropService` con Signals:
     - `moveTransaction(transaction, targetWalletId)`:
       - **Optimistic UI**: actualizar signals de ambas wallets + mover transaccion visualmente.
       - Llamar `PATCH /transactions/{id}/move` con `{target_wallet_id}`.
       - 200 -> confirmar estado.
       - Error (409, 422, 500) -> **revertir** optimistic update.
  4. Prevenir drop en misma wallet. Indicadores visuales: wallet destino resaltada, animacion suave al soltar.
- **Criterios de verificacion:** Arrastrar INCOME A->B: balances actualizan correctamente. Arrastrar EXPENSE A->B: A+, B-. Misma wallet -> sin efecto. Destino negativo -> revertir UI + mostrar error. Error backend -> revertir UI.
- **Estado:** `done` | **executor_notes:** Creados DraggableDirective, DroppableDirective, barrel export (index.ts). Integracion completa en TransactionListComponent: wallet cards como droppable targets, transaction rows como draggable, optimistic UI con rollback, toast notifications, balance delta visual. ng build exitoso. | **verification_result:** ng build exitoso, 6 archivos creados/modificados | **blocker:** `none`

---

### T41: Tests del modulo Transactions frontend

- **ID:** T41
- **Titulo:** Tests unitarios Transactions frontend (componentes + servicios + Drag & Drop)
- **Dependencias:** T38, T39, T40
- **Owner:** test-architect
- **Archivos afectados:** `*.spec.ts` en transactions/, shared/services/drag-drop.service.spec.ts
- **Especificaciones de referencia:** Master Spec SS8.1; skill `testing-strategy`
- **Pasos:**
  1. `TransactionApiService` tests: metodos HTTP, query params de filtros.
  2. `TransactionListComponent` tests: carga transacciones, paginacion.
  3. `TransactionFiltersComponent` tests: emite filtros correctamente.
  4. `TransactionFormComponent` tests: validacion amount > 0, submit crear/editar, manejo error 422.
  5. **`DragDropService` tests (CRITICO):** move exitoso -> optimistic update aplicado; move con error -> optimistic update revertido; misma wallet -> prevenido.
  6. `TransactionRowComponent` tests: renderiza datos, colores segun tipo.
  7. Cobertura >= 80%.
- **Criterios de verificacion:** `ng test --include="**/transaction**"` pasa. Cobertura >= 80%. Drag & Drop tests cubren optimistic UI + rollback.
- **Estado:** `todo` | **executor_notes:** | **verification_result:** | **blocker:** `none`

---

# INFRAESTRUCTURA

## L. Docker y Despliegue

---

### T42: Crear Dockerfile multi-stage para backend

- **ID:** T42
- **Titulo:** Dockerfile multi-stage para Spring Boot + Kotlin con Gradle
- **Dependencias:** T1
- **Owner:** devops-architect
- **Archivos afectados:** `NanoBankLedger-backend/Dockerfile`, `.dockerignore`
- **Especificaciones de referencia:** Master Spec SS9.1; System Landscape SS4.2
- **Pasos:**
  1. Stage 1 (build): `eclipse-temurin:21-jdk-alpine`, copiar proyecto, ejecutar `./gradlew bootJar --no-daemon`.
  2. Stage 2 (runtime): `eclipse-temurin:21-jre-alpine`, copiar JAR desde stage 1. Exponer 8080. Entrypoint `java -jar app.jar`.
  3. `.dockerignore`: excluir `build/`, `.gradle/`, `src/test/`, `*.md`.
  4. Variables de entorno documentadas: `SPRING_DATASOURCE_URL`, `JWT_SECRET`, etc.
- **Criterios de verificacion:** `docker build -t nanobank-backend .` completa. Contenedor arranca con `docker run -p 8080:8080`.
- **Estado:** `done` | **executor_notes:** Dockerfile multi-stage creado con eclipse-temurin:21-jdk-alpine (build) → eclipse-temurin:21-jre-alpine (runtime), usuario no-root nanobank, .dockerignore con exclusiones. | **verification_result:** `docker build -t nanobank-backend .` exitoso. Contenedor responde en puerto 8080. | **blocker:** `none`

---

### T43: Crear docker-compose.yml (PostgreSQL + Backend + Frontend)

- **ID:** T43
- **Titulo:** docker-compose.yml con 3 servicios: postgres, backend, frontend
- **Dependencias:** T42, T29
- **Owner:** devops-architect
- **Archivos afectados:** `NanoBankLedger-infrastructure/docker-compose/docker-compose.yml`
- **Especificaciones de referencia:** Master Spec SS9.1; System Landscape SS4.2, SS7.3
- **Pasos:**
  1. Servicio `postgres`: `image: postgres:16`, `POSTGRES_DB: nanobank_ledger`, `POSTGRES_USER: nanobank`, `POSTGRES_PASSWORD: nanobank_dev`, puerto `5432`, volumen `postgres_data`, healthcheck `pg_isready`.
  2. Servicio `backend`: build `../../NanoBankLedger-backend`, puerto `8080`, variables de entorno (datasource url -> `postgres:5432`, JWT secret), `depends_on: postgres (service_healthy)`.
  3. Servicio `frontend`: build `../../NanoBankLedger-frontend` con Nginx, puerto `4200:80`, `depends_on: backend`.
  4. Network `nanobank-network`. Volume `postgres_data`.
- **Criterios de verificacion:** `docker-compose up` levanta 3 servicios. Backend conecta a PostgreSQL. Frontend accesible en `http://localhost:4200`. Health checks funcionan.
- **Estado:** `done` | **executor_notes:** docker-compose.yml creado con 3 servicios (postgres:16-alpine, backend, frontend nginx:alpine). nginx.conf con proxy reverso /api/ → backend:8080. Red compartida nanobank-network. Health check PostgreSQL. Incidencia resuelta: variables APP_JWT_SECRET corregidas a JWT_ACCESS_SECRET. Incidencia resuelta: permisos de nginx corregidos con chown previo a USER nginx. | **verification_result:** `docker-compose up` levanta 3/3 servicios. Backend UP en http://localhost:8080/api/v1/health. Frontend UP en http://localhost:4200 (HTTP 200). | **blocker:** `none`

---

### T44: Crear scripts de inicializacion de BD

- **ID:** T44
- **Titulo:** Script de inicializacion de BD y configuracion de entorno local
- **Dependencias:** T4
- **Owner:** devops-architect
- **Archivos afectados:** `NanoBankLedger-infrastructure/scripts/db/init-db.sh`
- **Especificaciones de referencia:** Master Spec SS9
- **Pasos:**
  1. Crear `init-db.sh`: verificar PostgreSQL disponible, ejecutar migraciones Flyway via Gradle (`./gradlew flywayMigrate`), verificar tablas creadas.
  2. Script `reset-db.sh`: dropear y recrear BD para desarrollo.
  3. Script `seed-db.sh`: ejecutar migraciones + verificar seed de categorias.
  4. Documentar en README del proyecto infrastructure.
- **Criterios de verificacion:** `init-db.sh` ejecuta migraciones exitosamente. `reset-db.sh` limpia BD. Scripts idempotentes y con manejo de errores.
- **Estado:** `done` | **executor_notes:** init-db.sh creado (espera PostgreSQL, ejecuta Flyway, verifica tablas). reset-db.sh creado (confirmación → dropea/recrea BD → migraciones). Ambos con `set -e` y permisos de ejecución. | **verification_result:** init-db.sh ejecuta migraciones exitosamente. reset-db.sh limpia y recrea BD. | **blocker:** `none`

---

## Grafo de Dependencias

```
T1 (Setup Backend)
├── T2 (Config)
│   ├── T3 (Flyway)
│   │   └── T4 (Migration V1)
│   │       ├── T5 (Entities JPA)
│   │       │   └── T6 (Repos + Mappers)
│   │       │       ├── T7 (Auth Domain)
│   │       │       │   ├── T8 (Auth Use Cases)
│   │       │       │   │   ├── T9 (Auth REST)
│   │       │       │   │   │   ├── T12 (Auth Integration Tests)
│   │       │       │   │   │   └── T26 (Global Exception Handler)
│   │       │       │   │   └── T11 (Auth Unit Tests)
│   │       │       │   └── T10 (Security Config + Auth Adapters)
│   │       │       │       ├── T9
│   │       │       │       ├── T15 (Wallets REST)
│   │       │       │       ├── T20 (Transactions REST)
│   │       │       │       └── T24 (Categories REST)
│   │       │       ├── T13 (Wallet Domain)
│   │       │       │   ├── T14 (Wallet Use Cases)
│   │       │       │   │   ├── T15 (Wallets REST)
│   │       │       │   │   │   └── T17 (Wallets Integration Tests)
│   │       │       │   │   └── T16 (Wallets Unit Tests)
│   │       │       │   └── T18 (Transaction Domain) necesita T13
│   │       │       ├── T18 (Transaction Domain)
│   │       │       │   └── T19 (Transaction Use Cases)
│   │       │       │       ├── T20 (Transactions REST)
│   │       │       │       │   └── T22 (Transactions Integration Tests)
│   │       │       │       └── T21 (Transactions Unit Tests)
│   │       │       └── T23 (Category Domain)
│   │       │           └── T24 (Categories REST)
│   │       │               └── T25 (Categories Tests)
│   │       └── T27 (Seed Categories)
│   │           └── (sin dependencias fuertes, solo T4)
│   └── T28 (JaCoCo Coverage) depende de T11,T12,T16,T17,T21,T22,T25

T29 (Setup Frontend)
├── T30 (Minimalist UI)
├── T31 (HTTP Client + AuthService)
│   ├── T32 (Login/Register pages)
│   │   └── T33 (Auth Guards)
│   │       └── T34 (Auth Frontend Tests)
│   └── T35 (Wallet List + CRUD)
│       ├── T36 (Dashboard)
│       │   └── T37 (Wallets Frontend Tests)
│       └── T38 (Transaction List + Filters)
│           ├── T39 (Transaction Form CRUD)
│           ├── T40 (Drag & Drop)
│           └── T41 (Transactions Frontend Tests)

T42 (Dockerfile Backend) depende de T1
T43 (Docker Compose) depende de T42, T29
T44 (DB Scripts) depende de T4
```

---

## Estadisticas del Task Board

| Metrica | Valor |
|---|---|
| **Total tareas** | 44 |
| **Backend** | 28 (T1-T28) |
| **Frontend** | 13 (T29-T41) |
| **Infraestructura** | 3 (T42-T44) |

### Por Owner

| Owner | Tareas | IDs |
|---|---|---|
| executor | 30 | T1-T10, T13-T15, T18-T20, T23-T24, T26-T27, T29-T33, T35-T36, T38-T40 |
| test-architect | 11 | T11-T12, T16-T17, T21-T22, T25, T28, T34, T37, T41 |
| devops-architect | 3 | T42-T44 |

### Estados

| Estado | Cantidad |
|---|---|
| `todo` | 0 |
| `in_progress` | 0 |
| `done` | 44 |
| `blocked` | 0 |

---

## Reglas de Stale Terms (Recordatorio)

Los siguientes terminos NUNCA deben aparecer en codigo, comentarios, DTOs, endpoints ni documentacion tecnica de este incremento:

| Termino prohibido | Reemplazo correcto |
|---|---|
| `account` | `wallet` |
| `movement` | `transaction` |
| `/transfer` | `/move` |
| `targetWalletId` (camelCase) | `target_wallet_id` (snake_case) |
| `error_code` | `code` |
| `success` / `data` wrappers | Retornar recurso directamente |
| `PUT` (parcial) | `PATCH` |
| `session` (HTTP) | `sesion` (lenguaje ubicuo) |
| `balance` como `DOUBLE` | `NUMERIC(15,2)` |
| `id` como `SERIAL` | `UUID DEFAULT gen_random_uuid()` |
| `ddl-auto=update` | `ddl-auto=validate` |
| `localStorage` (tokens) | `Signal` (Angular) |

---

> **Fin del Task Board.** Ultima actualizacion: 2026-06-16. Proximo paso: Executor toma T1.
