# ADR-001: Estructura Monorepo para NanoBank Ledger

> **Estado:** Accepted
> **Fecha:** 2026-06-16
> **Owner:** Enterprise Architect
> **Decisión:** Usar monorepo con directorios separados para los 3 proyectos del ecosistema NanoBank Ledger.
> **Última revisión:** 2026-06-16

---

## 1. Contexto

NanoBank Ledger es un MVP de gestión financiera compuesto por tres proyectos técnicamente diferenciados:

1. **NanoBankLedger-backend** — API REST en Spring Boot 3.x + Kotlin con Arquitectura Hexagonal.
2. **NanoBankLedger-frontend** — SPA en Angular 17+ con Standalone Components y Signals.
3. **NanoBankLedger-infrastructure** — Configuración Docker, Docker Compose y scripts de base de datos.

Estos tres proyectos forman una **unidad de despliegue cohesiva** en la fase MVP: el Frontend consume exclusivamente el Backend, el Backend usa exclusivamente PostgreSQL, y la Infrastructure orquesta todos los contenedores.

### 1.1 Factores Decisivos

- **Equipo pequeño:** 1-3 desarrolladores que trabajan en todos los componentes.
- **Fase MVP:** Velocidad de iteración es prioritaria sobre gobernanza enterprise compleja.
- **Alta cohesión:** Los tres proyectos cambian juntos en la mayoría de los features.
- **Workspace de solución:** Se requiere un lugar centralizado para documentación de arquitectura macro (system landscape, context map, ADRs).

---

## 2. Decisión

Adoptar una **estructura de monorepo** donde los tres proyectos coexisten como directorios hermanos dentro de un workspace común (`NanoBankLedger-workspace/`), con documentación de arquitectura centralizada en `docs/architecture/`.

### 2.1 Estructura Resultante

```
NanoBankLedger-workspace/
├── docs/
│   ├── architecture/
│   │   ├── system-landscape.md          ← Visión macro C4
│   │   ├── context-map.md               ← Bounded Contexts DDD
│   │   ├── integration-map.md           ← Contratos de integración
│   │   └── decision-records/
│   │       ├── ADR-001-monorepo-structure.md
│   │       ├── ADR-002-hexagonal-architecture.md
│   │       └── ADR-003-jwt-refresh-rotation.md
│   ├── api/                             ← Contratos OpenAPI
│   ├── specs/                           ← Especificaciones SDD
│   └── requerimientos.pdf
├── NanoBankLedger-backend/
│   └── src/
│       ├── main/
│       └── test/
├── NanoBankLedger-frontend/
│   └── src/
│       ├── app/
│       ├── assets/
│       └── environments/
├── NanoBankLedger-infrastructure/
│   ├── docker-compose/
│   ├── docker/
│   └── scripts/
└── desarrollo-log.md
```

### 2.2 Reglas del Monorepo

| Regla | Justificación |
|---|---|
| Cada proyecto mantiene su propia estructura interna independiente | Permite que cada proyecto evolucione su build, tests y dependencias sin afectar a los demás |
| La documentación de arquitectura macro vive en la raíz del workspace | Fuente de verdad centralizada para decisiones que afectan múltiples proyectos |
| Los contratos OpenAPI viven en `docs/api/` en la raíz | Contract-first: el contrato es compartido por Frontend y Backend |
| Cada proyecto puede tener su propio `.gitignore`, `README`, y configuración de build | Autonomía técnica por proyecto |
| La Infrastructure es responsable de orquestar todos los contenedores | Punto único de configuración para el entorno de ejecución |

---

## 3. Alternativas Consideradas

### 3.1 Alternativa A: Multi-repo con Git Submodules

**Descripción:** Cada proyecto en su propio repositorio Git independiente, agregados al workspace principal mediante Git submodules.

**Ventajas:**
- Aislamiento total de repositorios (permisos, CI/CD independientes).
- Cada proyecto tiene su propio historial de commits limpio.
- Facilita la transición futura a equipos separados.

**Desventajas:**
- Complejidad significativa en la gestión de submodules (init, update, sync).
- Dificultad para hacer cambios atómicos cross-proyecto (ej: cambiar un contrato API y actualizar Frontend + Backend en un solo commit).
- Curva de aprendizaje alta para desarrolladores nuevos.
- Herramientas de IDE y CI/CD tienen soporte inconsistente para submodules.
- Overhead operacional: mantener versiones sincronizadas entre submodules.

**Razón de rechazo:** La complejidad operacional de los submodules no se justifica para un equipo pequeño en fase MVP. Los cambios atómicos cross-proyecto son frecuentes en esta etapa.

### 3.2 Alternativa B: Multi-repo Independientes (sin submodules)

**Descripción:** Cada proyecto en su propio repositorio Git, sin relación estructural en el filesystem. La documentación de arquitectura vive en un repositorio separado o en un wiki.

**Ventajas:**
- Máxima independencia por proyecto.
- CI/CD completamente aislados.
- Permisos granulares por repositorio.

**Desventajas:**
- Imposibilidad de hacer cambios atómicos cross-proyecto.
- Documentación de arquitectura desacoplada del código (drift frecuente).
- Dificultad para mantener contratos OpenAPI sincronizados.
- Overhead de gestión de múltiples repos para un equipo pequeño.
- No hay un "lugar natural" para la visión macro del sistema.

**Razón de rechazo:** Para un MVP con un equipo pequeño y alta cohesión entre componentes, el costo de coordinación supera los beneficios del aislamiento.

### 3.3 Alternativa C: Monorepo con Build System Unificado (Bazel/Nx/Turborepo)

**Descripción:** Monorepo con un sistema de build que orquesta todas las compilaciones, tests y despliegues desde un punto central.

**Ventajas:**
- Build caching inteligente (solo reconstruye lo afectado).
- Dependency graph automático entre proyectos.
- Comandos unificados (`build all`, `test all`).

**Desventajas:**
- Overhead de configuración y mantenimiento del build system.
- Curva de aprendizaje para herramientas como Bazel o Nx.
- Excesivo para un MVP con solo 3 proyectos.
- Spring Boot y Angular tienen sus propios build systems maduros (Gradle/Maven, Angular CLI).

**Razón de rechazo:** El overhead de un build system unificado no se justifica para 3 proyectos con tecnologías heterogéneas (Kotlin/Gradle, Angular/CLI, Docker). Cada proyecto usa su build system nativo.

---

## 4. Consecuencias

### 4.1 Ventajas (Consecuencias Positivas)

| Ventaja | Impacto |
|---|---|
| **Atomicidad de cambios cross-proyecto** | Un feature que requiere cambios en Frontend + Backend + API contract puede desarrollarse y versionarse en un solo flujo de trabajo |
| **Simplicidad operacional** | Un solo directorio para clonar, navegar y trabajar. Sin submodules ni múltiples repos que sincronizar |
| **Documentación co-locada con el código** | Los ADRs, system landscape y contratos OpenAPI viven junto al código que implementan, reduciendo drift |
| **Visibilidad holística** | Fácil entender la arquitectura completa del sistema al tener todos los componentes visibles |
| **Onboarding simplificado** | Un nuevo desarrollador clona un solo repositorio y tiene todo el ecosistema |
| **Cambios atómicos en contratos** | Si cambia un endpoint, se puede actualizar el OpenAPI spec, el Backend y el Frontend en un solo commit |

### 4.2 Desventajas (Consecuencias Negativas)

| Desventaja | Mitigación |
|---|---|
| **Repositorio más grande** | El tamaño es manejable para un MVP. Si crece significativamente, evaluar migrar a multi-repo |
| **CI/CD potencialmente más lento** | Configurar pipelines que solo ejecuten tests del proyecto afectado (cambio detectado por directorio) |
| **Acoplamiento percibido** | Cada proyecto mantiene su estructura interna independiente. El monorepo es una conveniencia de organización, no un acoplamiento técnico |
| **Permisos menos granulares** | Para un equipo pequeño, todos tienen acceso a todo. Si el equipo crece, evaluar multi-repo |
| **Historial de commits mezclado** | Usar convenciones de commits con prefijos (`feat(backend):`, `feat(frontend):`) para claridad |

### 4.3 Condiciones para Revisar la Decisión

Esta decisión debe revisarse si:

1. **El equipo crece a más de 5 desarrolladores** con especialización por proyecto (Frontend team vs Backend team).
2. **Los proyectos necesitan ciclos de release independientes** (ej: Backend se despliega semanalmente, Frontend mensualmente).
3. **Se requiere aislamiento de permisos** (diferentes equipos con acceso a diferentes proyectos).
4. **El repositorio supera los 5GB** o los tiempos de clone/checkout son problemáticos.
5. **Se integran servicios externos** que requieren repositorios separados por razones de seguridad o compliance.

---

## 5. Relación con Otros ADRs

| ADR | Relación |
|---|---|
| ADR-002 (Hexagonal Architecture) | La arquitectura hexagonal se aplica dentro del Backend, independientemente de la estructura de repositorio |
| ADR-003 (JWT Refresh Rotation) | La decisión de seguridad es independiente de la estructura del repositorio |

---

## 6. Evidencia

### Artefactos Creados

| # | Archivo | Ruta Absoluta | Estado |
|---|---|---|---|
| 1 | ADR-001 | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/decision-records/ADR-001-monorepo-structure.md` | ✅ Creado |

### Estructura del Workspace (Evidencia de la Decisión)

| Directorio | Ruta Absoluta | Rol |
|---|---|---|
| Workspace Root | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/` | Raíz del monorepo |
| Backend | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-backend/` | Proyecto Backend |
| Frontend | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-frontend/` | Proyecto Frontend |
| Infrastructure | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/NanoBankLedger-infrastructure/` | Proyecto Infrastructure |
| Architecture Docs | `/home/cristiansrc/Documentos/Proyectos/NanoBankLedger-workspace/docs/architecture/` | Documentación macro centralizada |
