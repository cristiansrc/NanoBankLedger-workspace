# ADR-003: Autenticación JWT con Refresh Token Rotativo

> **Estado:** Accepted
> **Fecha:** 2026-06-16
> **Owner:** Enterprise Architect + Security Lead
> **Decisión:** Implementar autenticación basada en JWT con Access Token de corta duración (15 min) y Refresh Token de larga duración (7 días) con rotación obligatoria en cada uso.
> **Última revisión:** 2026-06-16

---

## 1. Contexto

NanoBank Ledger es una SPA (Single Page Application) Angular que se comunica con un Backend Spring Boot vía HTTP/REST. La autenticación debe cumplir con los siguientes requisitos:

### 1.1 Factores Decisivos

- **SPA sin cookies de sesión:** El Frontend es una Angular SPA que no puede depender de cookies de sesión server-side (CORS, CSRF, escalabilidad).
- **Datos financieros sensibles:** El sistema maneja información financiera personal que requiere protección adecuada.
- **Experiencia de usuario fluida:** El usuario no debe ser forzado a re-login frecuentemente, pero la seguridad no puede comprometerse.
- **Sin estado en el Backend (stateless API):** El Access Token debe ser auto-contenido (JWT) para evitar sesiones server-side en cada request.
- **Protección contra robo de tokens:** Si un token es comprometido, la ventana de exposición debe ser mínima y detectable.

### 1.2 Requisitos de Seguridad

| Requisito | Justificación |
|---|---|
| Tokens de corta duración | Minimizar ventana de exposición si un token es interceptado |
| Renovación transparente | El usuario no debe notar la renovación del token |
| Detección de robo | Si un refresh token es reutilizado, se debe detectar y revocar toda la sesión |
| Sin almacenamiento persistente de tokens en el navegador | Prevenir ataques XSS que roben tokens de localStorage |
| Rotación de refresh tokens | Limitar la vida útil de cada refresh token individual |

### 1.3 Restricciones Técnicas

- **No hay servidor de identidad externo (Keycloak)** en la fase MVP — la autenticación es self-hosted.
- **El Frontend es una SPA pura** — no hay server-side rendering que pueda usar cookies httpOnly.
- **El Backend es stateless** — no mantiene sesiones HTTP.

---

## 2. Decisión

Implementar un esquema de **dual token JWT** con las siguientes características:

### 2.1 Esquema de Tokens

| Token | Tipo | Duración | Contenido | Almacenamiento |
|---|---|---|---|---|
| **Access Token** | JWT (JSON Web Token) | 15 minutos | `sub` (user ID), `email`, `iat`, `exp` | Signal en memoria (Angular) |
| **Refresh Token** | UUID v4 opaco | 7 días | Identificador aleatorio (sin información del usuario) | Signal en memoria (Angular) + BD (hasheado) |

### 2.2 Flujo de Autenticación

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    FLUJO COMPLETO DE AUTENTICACIÓN                          │
│                                                                             │
│  FASE 1: LOGIN                                                              │
│  ───────────                                                                │
│  1. Usuario envía email + password → POST /api/v1/auth/login                │
│  2. Backend valida credenciales contra hash en BD                           │
│  3. Backend genera Access Token (JWT, 15 min) + Refresh Token (UUID, 7d)    │
│  4. Backend almacena hash del Refresh Token en BD (tabla refresh_tokens)    │
│  5. Backend retorna ambos tokens al Frontend                                │
│  6. Frontend almacena tokens en Signals (memoria)                           │
│                                                                             │
│  FASE 2: REQUESTS AUTENTICADOS                                              │
│  ───────────────────────────                                                │
│  7. Frontend incluye Access Token en header Authorization: Bearer <token>   │
│  8. Backend valida JWT (firma, expiración) en JwtAuthenticationFilter       │
│  9. Si válido, extrae user_id del claim `sub` y continúa el request         │
│  10. Si expirado, retorna 401 → Frontend inicia FASE 3                     │
│                                                                             │
│  FASE 3: RENOVACIÓN (REFRESH)                                               │
│  ──────────────────────────                                                 │
│  11. Frontend detecta 401 o Access Token próximo a expirar                  │
│  12. Frontend envía POST /api/v1/auth/refresh con Refresh Token             │
│  13. Backend busca el Refresh Token en BD (por hash)                        │
│  14. Backend verifica: no expirado + no revocado                            │
│  15. ROTACIÓN:                                                              │
│      a. Backend revoca el Refresh Token actual (marca como usado)           │
│      b. Backend genera nuevo Access Token (15 min)                          │
│      c. Backend genera nuevo Refresh Token (UUID, 7d)                       │
│      d. Backend almacena hash del nuevo Refresh Token en BD                 │
│  16. Backend retorna nuevo par de tokens                                    │
│  17. Frontend actualiza Signals con nuevos tokens                           │
│  18. Frontend reintenta el request original con nuevo Access Token          │
│                                                                             │
│  FASE 4: DETECCIÓN DE ROBO                                                  │
│  ──────────────────────────                                                 │
│  19. Si un atacante usa un Refresh Token ya revocado:                       │
│      a. Backend detecta que el token fue usado previamente                  │
│      b. Backend revoca TODA la familia de tokens del usuario                │
│      c. Backend retorna 401                                                 │
│      d. Frontend redirige al login                                          │
│                                                                             │
│  FASE 5: LOGOUT                                                             │
│  ────────────                                                               │
│  20. Frontend envía POST /api/v1/auth/logout con Access Token               │
│  21. Backend revoca el Refresh Token activo en BD                           │
│  22. Frontend limpia Signals (tokens eliminados de memoria)                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Especificación del Access Token (JWT)

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "usuario@ejemplo.com",
  "iat": 1718539200,
  "exp": 1718540100
}
```

| Claim | Descripción |
|---|---|
| `sub` | User ID (UUID) — fuente de identidad para todos los bounded contexts |
| `email` | Email del usuario — para logging y auditoría |
| `iat` | Issued At (timestamp Unix) |
| `exp` | Expiration (iat + 900 segundos = 15 minutos) |

### 2.4 Especificación del Refresh Token

| Atributo | Valor |
|---|---|
| **Formato** | UUID v4 (128 bits, criptográficamente aleatorio) |
| **Duración** | 7 días desde la emisión |
| **Almacenamiento en BD** | Hash SHA-256 del token (no el token en texto plano) |
| **Almacenamiento en Frontend** | Signal en memoria (nunca en localStorage/sessionStorage) |
| **Rotación** | Obligatoria en cada uso — el token usado se invalida y se emite uno nuevo |
| **Revocación** | Campo `revoked_at` en la tabla `refresh_tokens` |

### 2.5 Modelo de Datos para Refresh Tokens

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 del token
    family_id       UUID NOT NULL,                 -- Identifica la familia de tokens (para revocación en cascada)
    issued_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP NOT NULL,
    revoked_at      TIMESTAMP NULL,                -- NULL = activo, NOT NULL = revocado
    used_at         TIMESTAMP NULL,                -- Marca cuándo fue usado (para detección de reutilización)
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### 2.6 Reglas de Rotación

| # | Regla | Justificación |
|---|---|---|
| 1 | Cada refresh token solo puede usarse UNA vez | Previene replay attacks |
| 2 | Al usar un refresh token, se emite uno nuevo con un nuevo `family_id` compartido | Permite rastrear la cadena de tokens |
| 3 | Si se detecta reutilización de un token ya usado (campo `used_at` no null), se revoca toda la familia | Detecta robo: si alguien más usa el token, toda la sesión se invalida |
| 4 | El token usado se marca con `used_at` inmediatamente | Permite detectar reutilización posterior |
| 5 | Logout revoca el token activo sin generar uno nuevo | Termina la sesión limpiamente |

---

## 3. Alternativas Consideradas

### 3.1 Alternativa A: Sesiones Server-Side (Spring Session + Cookie)

**Descripción:** El Backend mantiene sesiones HTTP server-side (en memoria o Redis). El Frontend recibe una cookie de sesión httpOnly.

**Ventajas:**
- Revocación inmediata (eliminar sesión del servidor).
- No requiere gestión de tokens en el Frontend.
- Cookies httpOnly son inmunes a XSS para robo de session ID.

**Desventajas:**
- **Stateful:** El Backend debe mantener estado de sesión, violando el principio stateless de la API REST.
- **CORS complejo:** Las cookies cross-origin requieren configuración adicional (`Access-Control-Allow-Credentials`, `SameSite`).
- **CSRF vulnerable:** Las cookies son enviadas automáticamente por el navegador, requiriendo protección CSRF adicional.
- **Escalabilidad limitada:** Sesiones en memoria no escalan horizontalmente sin sticky sessions o Redis.
- **No nativo para SPA:** Las SPAs modernas prefieren tokens sobre cookies para APIs REST.

**Razón de rechazo:** El requisito de API stateless y la naturaleza SPA del Frontend hacen que las sesiones server-side sean una solución incómoda. Además, agregaría complejidad de infraestructura (Redis para sesiones distribuidas).

### 3.2 Alternativa B: JWT Simple (sin Refresh Token)

**Descripción:** Solo Access Token JWT con duración extendida (ej: 24 horas o más). Sin mecanismo de renovación.

**Ventajas:**
- Implementación más simple (un solo token).
- Sin necesidad de tabla de refresh tokens en BD.
- Sin lógica de rotación.

**Desventajas:**
- **Ventana de exposición grande:** Si el token es robado, es válido por 24+ horas.
- **Sin revocación:** No hay forma de invalidar un JWT antes de su expiración (sin blacklists, que reintroducen estado).
- **UX pobre:** Para reducir la ventana de exposición, se necesitaría duración corta, forzando re-login frecuente.
- **No detecta robo:** Sin rotación, no hay mecanismo para detectar si un token es usado por un atacante.

**Razón de rechazo:** La combinación de ventana de exposición grande y ausencia de revocación es inaceptable para un sistema que maneja datos financieros.

### 3.3 Alternativa C: OAuth2 / OIDC con Keycloak

**Descripción:** Delegar la autenticación a un servidor de identidad dedicado (Keycloak) usando el flujo Authorization Code con PKCE.

**Ventajas:**
- Estándar de la industria (OAuth2 / OIDC).
- Keycloak maneja toda la complejidad de tokens, rotación, revocación.
- Preparado para SSO, federación de identidad, MFA.
- Escalable a múltiples aplicaciones y microservicios.

**Desventajas:**
- **Overhead operacional significativo:** Keycloak es una aplicación Java pesada que requiere su propia BD, mantenimiento, actualizaciones y monitoreo.
- **Excesivo para un MVP:** Para un sistema con un solo cliente (SPA) y autenticación simple (email/password), Keycloak es overkill.
- **Complejidad de desarrollo:** Configurar realms, clients, scopes, roles y flujos OAuth2 agrega complejidad innecesaria en fase MVP.
- **Recursos de infraestructura:** Keycloak requiere al menos 512MB-1GB de RAM y una base de datos dedicada.
- **Curva de aprendizaje:** El equipo debe aprender Keycloak administration.

**Razón de no-adopción (fase MVP):** Keycloak es la solución correcta para un sistema en producción con múltiples clientes, SSO o federación de identidad. Para el MVP de NanoBank Ledger, el overhead no se justifica. **Esta decisión debe revisarse cuando el sistema escale a múltiples aplicaciones o requiera SSO.**

### 3.4 Alternativa D: BFF (Backend-for-Frontend) con Cookies httpOnly

**Descripción:** Un servidor intermedio (BFF) maneja los tokens y los almacena en cookies httpOnly. El Frontend solo se comunica con el BFF.

**Ventajas:**
- Tokens nunca expuestos al JavaScript del navegador.
- Cookies httpOnly protegen contra XSS.
- Separación de concerns: el BFF maneja auth, el API Backend es stateless.

**Desventajas:**
- **Componente adicional:** Requiere un servidor intermedio (Node.js, Nginx + Lua, etc.).
- **Complejidad de despliegue:** Un contenedor adicional en Docker Compose.
- **Excesivo para MVP:** Agrega complejidad innecesaria para un sistema con un solo Frontend.
- **Latencia adicional:** Cada request pasa por el BFF.

**Razón de rechazo:** Para un MVP con un solo Frontend, el BFF agrega complejidad sin beneficio proporcional. Es una evolución natural si el sistema crece.

---

## 4. Consecuencias

### 4.1 Ventajas (Consecuencias Positivas)

| Ventaja | Impacto |
|---|---|
| **API stateless** | El Backend no mantiene sesiones; escala horizontalmente sin sticky sessions |
| **Ventana de exposición mínima** | Access Token de 15 min limita el daño si es comprometido |
| **Detección de robo** | La rotación de refresh tokens permite detectar reutilización maliciosa |
| **Revocación posible** | Los refresh tokens pueden revocarse individualmente o por familia |
| **UX fluida** | La renovación es transparente para el usuario; sesión de 7 días sin re-login |
| **Sin dependencias externas** | No requiere Keycloak ni servicios de identidad externos en MVP |
| **Preparado para evolución** | Si se necesita Keycloak en el futuro, el contrato de tokens (JWT con `sub`) es compatible |
| **Almacenamiento seguro** | Tokens en memoria (Signals) son inmunes a ataques que leen localStorage |

### 4.2 Desventajas (Consecuencias Negativas)

| Desventaja | Mitigación |
|---|---|
| **Complejidad de implementación** | La rotación y detección de robo agregan ~30% más código que un JWT simple. Mitigado con el BC Auth bien definido |
| **Tokens vulnerables a XSS (en memoria)** | Los tokens en memoria JS son accesibles por XSS si la aplicación tiene una vulnerabilidad. Mitigado con CSP headers, sanitización de inputs y Angular's built-in XSS protection |
| **Tokens vulnerables a XSS (en memoria) durante refresh** | Durante el request de refresh, el refresh token viaja en el body. Mitigado con HTTPS obligatorio |
| **Tabla adicional en BD** | `refresh_tokens` requiere espacio y queries. Mitigado con índices y limpieza periódica de tokens expirados |
| **Lógica de rotación compleja** | La detección de reutilización y revocación en cascada requiere código cuidadoso. Mitigado con tests exhaustivos del BC Auth |
| **No protege contra todos los ataques** | Si el navegador está comprometido (malware), los tokens en memoria son accesibles. Esto es inherente a cualquier SPA |

### 4.3 Consideraciones de Seguridad Adicionales

| Medida | Implementación |
|---|---|
| **HTTPS obligatorio** | Todos los requests de auth deben ser sobre TLS. En desarrollo local, se acepta HTTP pero se documenta como no-producción |
| **Rate limiting en /auth/login** | Máximo 5 intentos por minuto por IP para prevenir brute force |
| **Password hashing** | BCrypt con costo 12 (configurable) para almacenar contraseñas |
| **CORS restrictivo** | Solo el origen del Frontend puede hacer requests a endpoints de auth |
| **Content Security Policy** | CSP headers para prevenir XSS |
| **Limpieza de tokens expirados** | Job periódico que elimina refresh_tokens expirados y revocados antiguos (> 30 días) |
| **Auditoría de eventos de auth** | Log de logins exitosos, fallidos, refreshes y logouts |

### 4.4 Condiciones para Migrar a Keycloak

Esta decisión debe revisarse y potencialmente migrar a OAuth2/OIDC con Keycloak si:

1. **Se agregan múltiples aplicaciones cliente** (ej: app móvil, admin panel, API pública).
2. **Se requiere SSO** (Single Sign-On) entre aplicaciones.
3. **Se necesita federación de identidad** (Google, GitHub, enterprise SSO).
4. **Se requiere MFA** (Multi-Factor Authentication).
5. **El sistema escala a múltiples microservicios** que necesitan validación de tokens independiente.
6. **Se requieren scopes y roles granulares** más allá de lo que un JWT simple puede ofrecer.

---

## 5. Relación con Otros ADRs y Documentos

| Referencia | Relación |
|---|---|
| `context-map.md` (BC: Auth) | Este ADR detalla la implementación del Bounded Context de Auth |
| `integration-map.md` (INT-004) | Este ADR define el flujo técnico; el Integration Map define el contrato de integración |
| `system-landscape.md` (§7.1 Seguridad) | Este ADR es la implementación detallada de la estrategia de seguridad del System Landscape |
| ADR-001 (Monorepo) | La implementación de auth vive en el Backend, dentro del monorepo |
| ADR-002 (Hexagonal) | La lógica de auth sigue Arquitectura Hexagonal: dominio puro (User, RefreshToken), puertos (UserRepository), adapters (JPA, JWT) |
| `security-standards` (skill) | Este ADR debe alinearse con los estándares de seguridad del workspace |

---

## 6. Evidencia

### Artefactos Creados

| # | Archivo | Ruta Absoluta | Estado |
|---|---|---|---|
| 1 | ADR-003 | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md` | ✅ Creado |

### Artefactos de Implementación (Pendientes)

| Artefacto | Ubicación Esperada | Estado |
|---|---|---|
| Tabla `refresh_tokens` (Flyway migration) | `NanoBankLedger-infrastructure/scripts/V{version}__create_refresh_tokens_table.sql` | Pendiente |
| BC Auth (Domain) | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/auth/domain/` | Pendiente |
| BC Auth (Application) | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/auth/application/` | Pendiente |
| BC Auth (Adapter In) | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/auth/adapter/in/` | Pendiente |
| BC Auth (Adapter Out) | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/auth/adapter/out/` | Pendiente |
| JWT Filter | `NanoBankLedger-backend/src/main/kotlin/com/nanobank/ledger/shared/security/JwtAuthenticationFilter.kt` | Pendiente |
| Angular Auth Service | `NanoBankLedger-frontend/src/app/core/services/auth.service.ts` | Pendiente |
| Angular Auth Guard | `NanoBankLedger-frontend/src/app/core/guards/auth.guard.ts` | Pendiente |
| Angular Token Interceptor | `NanoBankLedger-frontend/src/app/core/interceptors/token.interceptor.ts` | Pendiente |
