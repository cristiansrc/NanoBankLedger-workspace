# Re-Validation Report — 001-mvp-initial

> **Validator:** spec-validator
> **Fecha:** 2026-06-16
> **Incremento:** 001-mvp-initial
> **Ronda:** 2 (re-validacion post-correcciones)
> **Veredicto:** `not ready`

---

## 1. Resumen Ejecutivo

De los 11 findings originales (1 BLOCKER, 6 HIGH, 3 MEDIUM, 1 LOW), **10 fueron corregidos exitosamente**. Sin embargo, persisten **2 findings HIGH** que impiden la aprobacion:

1. **F-001 remanente (HIGH):** El Integration Map aun tiene 2 endpoints de transactions con paths incorrectos (lineas 76-77): `/api/v1/transactions` en lugar de `/api/v1/wallets/{walletId}/transactions`.
2. **F-012 nuevo (HIGH):** El Context Map (lineas 165-166) define el lenguaje ubicuo de "Monto" como "negativo para gastos", contradiciendo directamente la RN-003 de la Master Spec ("el monto siempre es positivo") y el CHECK constraint `amount > 0`.

---

## 2. Artefactos Revisados

| # | Artefacto | Ruta Absoluta | Lineas |
|---|---|---|---|
| 1 | Master Spec | `docs/specs/master-spec.md` | 714 |
| 2 | Shared Context | `docs/specs/.working/001-mvp-initial-sdd-context.md` | 194 |
| 3 | OpenAPI Contract | `docs/api/openapi.yaml` | 1462 |
| 4 | Integration Map | `docs/architecture/integration-map.md` | 486 |
| 5 | Context Map | `docs/architecture/context-map.md` | 339 |
| 6 | ADR-003 JWT Refresh | `docs/architecture/decision-records/ADR-003-jwt-refresh-rotation.md` | 335 |

---

## 3. Tabla de Hallazgos Re-verificados

| ID | Severidad | Artefacto | Estado | Evidencia de correccion (o persistencia) |
|---|---|---|---|---|
| F-001 | BLOCKER → **HIGH** (remanente) | Integration Map | **❌ Parcialmente corregido** | La mayoria de sub-issues fueron corregidos (PUT→PATCH L69, /transfer→/move L81, camelCase→snake_case L255/272, error contract L93-103, Idempotency-Key eliminado, POST/PUT/DELETE categories eliminados L87, FK user_id en categories eliminada L160). **PERO** lineas 76-77 aun dicen `POST /api/v1/transactions` y `GET /api/v1/transactions` cuando deberian ser `POST /api/v1/wallets/{walletId}/transactions` y `GET /api/v1/wallets/{walletId}/transactions`. Ver Double-Check: Integration Map L76 string exacto: `\| POST \| \`/api/v1/transactions\`` vs Master Spec L357 string exacto: `\| \`POST\` \| \`/api/v1/wallets/{walletId}/transactions\``. OpenAPI L518 confirma: `/api/v1/wallets/{walletId}/transactions:`. |
| F-002 | HIGH | Context Map | **✅ Resuelto** | Context Map L125: "Eliminar billeteras (DELETE fisico)". L137: "Eliminar billetera (DELETE fisico)". L178: "Eliminar una transaccion (DELETE fisico)". L189: "Eliminar transaccion (DELETE fisico)". |
| F-003 | HIGH | Context Map | **✅ Resuelto** | Context Map L207: "solo lectura en el MVP". L224-225: Solo responsabilidades de listar. L231: Solo puerto `ListCategories`. |
| F-004 | HIGH | Master Spec | **✅ Resuelto** | Master Spec L346-367: Todos los 12 endpoints protegidos ahora incluyen `401` en sus status codes. |
| F-005 | HIGH | OpenAPI | **✅ Resuelto** | OpenAPI L1198: `pattern: '^(?!0+(\.0{1,2})?$)\d+(\.\d{1,2})?$'` + L1199: `minimum: 0.01`. Mismo pattern en L1224-1225 para UpdateTransactionRequest. |
| F-006 | HIGH | Master Spec + ADR-003 | **✅ Resuelto** | Master Spec L181: `issued_at TIMESTAMPTZ NOT NULL`. L186: `updated_at TIMESTAMPTZ NOT NULL`. ADR-003 L150: `issued_at TIMESTAMP NOT NULL DEFAULT NOW()`. L155: `updated_at TIMESTAMP NOT NULL DEFAULT NOW()`. Diagrama ER L250/255 incluye ambas. Decomposition Contract L670 incluye ambas. |
| F-007 | HIGH | Integration Map | **✅ Resuelto** | Integration Map L160: `categories \| Categories \| — \| Strong` (sin FK). |
| F-008 | MEDIUM | Shared Context | **✅ Resuelto** | Shared Context L54: "Se creara despues de `awaiting-human-plan-approval`". Termino `validated-not-executed` ya no aparece. |
| F-009 | MEDIUM | OpenAPI | **✅ Resuelto** | OpenAPI L1328-1334: Schema `CategoryType` independiente. L1349: `CategoryResponse.type` referencia `$ref: '#/components/schemas/CategoryType'`. |
| F-010 | MEDIUM | Master Spec | **✅ Resuelto** | Master Spec L661: `PaginatedTransactionResponse` especifico con nota explicativa. |
| F-011 | LOW | Master Spec | **✅ Resuelto** | Master Spec L206: Nota explicita "El campo `initial_balance` del `CreateWalletRequest` es **opcional** con valor default `\"0.00\"`." OpenAPI L1113: `required: [name, type]` (initial_balance no requerido). L1127: `default: "0.00"`. |
| **F-012** | **HIGH** (nuevo) | Context Map | **❌ Nuevo hallazgo** | Context Map L165 string exacto: `Transaccion con monto negativo que decrementa el saldo de la billetera`. L166 string exacto: `Valor numerico de la transaccion (positivo para ingresos, negativo para gastos)`. Contradice Master Spec L216: `Monto (siempre positivo; el tipo indica direccion)`. L223: `amount > 0 (CHECK constraint — el monto siempre es positivo)`. L298 (RN-003): `El monto de una transaccion siempre es positivo`. OpenAPI L1199: `minimum: 0.01`. **Executor risk:** Alto — el Executor podria implementar el modelo de dominio con montos negativos para gastos, violando el CHECK constraint `amount > 0` y la RN-003, causando bugs en produccion. |

---

## 4. Consistencia Cross-Artefacto (Ronda 2)

| Verificacion | Resultado | Evidencia |
|---|---|---|
| Endpoints Auth: Master Spec ↔ OpenAPI ↔ Integration Map | **PASS** | 4 endpoints auth consistentes en los 3 artefactos |
| Endpoints Wallets: Master Spec ↔ OpenAPI ↔ Integration Map | **PASS** | 5 endpoints wallets consistentes (GET, POST, GET/{id}, PATCH/{id}, DELETE/{id}) |
| Endpoints Transactions: Master Spec ↔ OpenAPI ↔ Integration Map | **FAIL** | Master Spec L356-357 y OpenAPI L518 usan `/api/v1/wallets/{walletId}/transactions` para crear y listar. Integration Map L76-77 usa `/api/v1/transactions` (sin prefijo walletId). |
| Endpoints Categories: Master Spec ↔ OpenAPI ↔ Integration Map | **PASS** | Solo GET /api/v1/categories en los 3 artefactos |
| Error contract: Master Spec ↔ OpenAPI ↔ Integration Map | **PASS** | ApiErrorResponse con timestamp, status, error, code, message, path, trace_id, details consistente |
| Schemas/DTOs: Master Spec ↔ OpenAPI | **PASS** | 18 schemas consistentes incluyendo PaginatedTransactionResponse y CategoryType |
| Modelo de datos: Master Spec ↔ ADR-003 | **PASS** | Columnas de refresh_tokens ahora incluyen issued_at y updated_at en ambos |
| DELETE fisico: Master Spec ↔ Context Map ↔ Integration Map | **PASS** | Los 3 artefactos confirman DELETE fisico con CASCADE |
| snake_case: OpenAPI ↔ Integration Map | **PASS** | Todos los campos JSON usan snake_case |
| Lenguaje ubicuo: Context Map ↔ Master Spec (reglas de negocio) | **FAIL** | Context Map L165-166 define "monto negativo" para gastos; Master Spec RN-003 dice "monto siempre positivo" |
| Stale terms guard en Integration Map | **PASS** | No se encontraron terminos prohibidos (PUT, /transfer, targetWalletId, error_code, camelCase) |
| Shared Context headings obligatorios | **PASS** | Todos los headings presentes y no duplicados |
| Canonical artifacts existen en disco | **PASS** | Los 9 artefactos documentales + 3 directorios de proyecto existen |

---

## 5. Findings Pendientes de Resolucion

### F-001 remanente (HIGH) — Integration Map: paths de transactions incorrectos

**Artefacto afectado:** `docs/architecture/integration-map.md`

**Seccion:** §3.3 Transactions Endpoints, lineas 76-77

**Cambio requerido:**
- Linea 76: Cambiar `| POST | \`/api/v1/transactions\` | Crear transaccion |` → `| POST | \`/api/v1/wallets/{walletId}/transactions\` | Crear transaccion |`
- Linea 77: Cambiar `| GET | \`/api/v1/transactions\` | Listar transacciones (con filtros) |` → `| GET | \`/api/v1/wallets/{walletId}/transactions\` | Listar transacciones (con filtros) |`

**Executor risk:** El Executor implementaria los endpoints de creacion y listado de transacciones en paths incorrectos (`/api/v1/transactions` en lugar de `/api/v1/wallets/{walletId}/transactions`), rompiendo la jerarquia de recursos y la consistencia con OpenAPI.

---

### F-012 (HIGH) — Context Map: lenguaje ubicuo de "monto" contradice RN-003

**Artefacto afectado:** `docs/architecture/context-map.md`

**Seccion:** §3.3.1 Lenguaje Ubicuo, lineas 165-166

**Cambio requerido:**
- Linea 165: Cambiar `Transaccion con monto negativo que decrementa el saldo de la billetera` → `Transaccion con monto positivo que decrementa el saldo de la billetera (el tipo EXPENSE indica la direccion)`
- Linea 166: Cambiar `Valor numerico de la transaccion (positivo para ingresos, negativo para gastos)` → `Valor numerico de la transaccion (siempre positivo; el tipo INCOME/EXPENSE indica la direccion del efecto sobre el saldo)`

**Executor risk:** El Executor podria implementar el modelo de dominio permitiendo montos negativos para gastos, lo cual violaria el CHECK constraint `amount > 0` de la BD y la RN-003, causando errores de integridad en produccion.

---

## 6. Veredicto Final

### `not ready`

**Razon:** Persisten 2 findings HIGH sin resolver:
1. F-001 remanente: 2 endpoints de transactions con paths incorrectos en Integration Map (lineas 76-77).
2. F-012 nuevo: Lenguaje ubicuo de "monto" en Context Map contradice RN-003 y CHECK constraint de BD.

Ambos findings tienen alto riesgo de implementacion incorrecta por parte del Executor.

**Next action:** `Planner corrections`

### Acciones requeridas del Planner

1. **[HIGH]** Corregir Integration Map lineas 76-77: agregar prefijo `/wallets/{walletId}` a los endpoints de crear y listar transacciones.
2. **[HIGH]** Corregir Context Map lineas 165-166: alinear lenguaje ubicuo de "monto" con RN-003 (siempre positivo).

---

*Reporte de re-validacion generado por spec-validator. Este archivo es un artefacto de validacion; no modifica specs, contratos ni decisiones de arquitectura.*
