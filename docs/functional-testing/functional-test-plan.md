# Plan de Pruebas Funcionales — NanoBank Ledger Frontend

**Proyecto:** NanoBank Ledger
**Fecha de Creación:** 2026-06-16
**Diseñador:** functional-test-planner

---

## 1. Alcance de las Pruebas

Este plan cubre la validación funcional del Frontend Angular 17+ (Standalone + Signals) de NanoBank Ledger, corriendo en `http://localhost:4200` con backend en `http://localhost:8080/api/v1`.

Se verifican las siguientes vistas y funcionalidades:

| Funcionalidad | Vistas/Rutas | Operaciones |
|---|---|---|
| **Auth** | `/auth/login`, `/auth/register` | Registro, Login, Logout, Protección de rutas |
| **Dashboard** | `/dashboard` | Resumen de saldo total, listado de wallets, enlace a movimientos |
| **Wallets** | `/wallets` | CRUD completo con modal, tipos (SAVINGS/CHECKING/INVESTMENT/CASH), saldo inicial |
| **Transactions** | `/wallets/:id/transactions`, `/transactions` | CRUD con modal, filtros por categoría/tipo/fecha, paginación |
| **Drag & Drop** | Vista de transacciones | Arrastrar transacción entre wallets, Optimistic UI, rollback en error |
| **Categories** | Precargadas en formularios | Listado de categorías solo lectura |

---

## 2. Escenarios de Prueba

---

### 2.1 Autenticación (Auth)

#### [TS_001] - Registro de nuevo usuario exitoso
- **Objetivo:** Validar que un usuario puede registrarse con nombre, email y password, y es redirigido al dashboard.
- **Criterio de Aceptación:** CA-001 (Registro + login exitoso)
- **Precondiciones:**
  - App corriendo en `http://localhost:4200`
  - No existe un usuario con el email a registrar
  - Backend operativo en `http://localhost:8080`
- **Pasos:**
  1. Navegar a `http://localhost:4200/auth/register`
  2. Escribir "Juan Pérez" en `input#name`
  3. Escribir "juan.test@ejemplo.com" en `input#email`
  4. Escribir "TestPass123!" en `input#password`
  5. Click en `button[type="submit"]` que contiene "Crear cuenta"
- **Resultado Esperado:**
  - Redirección a `/dashboard`
  - Se muestra el mensaje "Aun no tienes billeteras."
  - No hay errores en consola JS
- **Selectores UI:** `input#name`, `input#email`, `input#password`, `button[type="submit"]`

#### [TS_002] - Login con credenciales válidas
- **Objetivo:** Validar que un usuario registrado puede iniciar sesión y acceder al dashboard.
- **Criterio de Aceptación:** CA-001 (Login exitoso)
- **Precondiciones:**
  - Usuario registrado con email `juan.test@ejemplo.com` y password `TestPass123!`
  - Sesión previa cerrada (no hay token en memoria)
- **Pasos:**
  1. Navegar a `http://localhost:4200/auth/login`
  2. Escribir "juan.test@ejemplo.com" en `input#email`
  3. Escribir "TestPass123!" en `input#password`
  4. Click en `button[type="submit"]` que contiene "Iniciar sesión"
- **Resultado Esperado:**
  - Redirección a `/dashboard`
  - Se muestra el resumen con "Saldo Total", "Billeteras" y "Tipo mas usado"
  - Las billeteras del usuario se listan en "Tus Billeteras"
- **Selectores UI:** `input#email`, `input#password`, `button[type="submit"]`

#### [TS_003] - Login con credenciales inválidas
- **Objetivo:** Validar que el formulario muestra mensaje de error al ingresar credenciales incorrectas.
- **Criterio de Aceptación:** Manejo de error 401 con mensaje "Email o contraseña incorrectos"
- **Precondiciones:** App corriendo, sin sesión activa
- **Pasos:**
  1. Navegar a `http://localhost:4200/auth/login`
  2. Escribir "inexistente@email.com" en `input#email`
  3. Escribir "WrongPassword1" en `input#password`
  4. Click en `button[type="submit"]` que contiene "Iniciar sesión"
- **Resultado Esperado:**
  - No hay redirección (permanece en `/auth/login`)
  - Se muestra el mensaje "Email o contraseña incorrectos" en `.error-message`
  - El botón de submit vuelve a estado habilitado
- **Selectores UI:** `input#email`, `input#password`, `button[type="submit"]`, `.error-message`

#### [TS_004] - Registro con email ya existente
- **Objetivo:** Validar que el registro con un email duplicado muestra error 409 con mensaje descriptivo.
- **Criterio de Aceptación:** RN-018 (Email único) + manejo de error 409
- **Precondiciones:**
  - Usuario con email `juan.test@ejemplo.com` ya registrado
- **Pasos:**
  1. Navegar a `http://localhost:4200/auth/register`
  2. Escribir "Otro Usuario" en `input#name`
  3. Escribir "juan.test@ejemplo.com" en `input#email`
  4. Escribir "OtraPass123!" en `input#password`
  5. Click en `button[type="submit"]`
- **Resultado Esperado:**
  - No hay redirección (permanece en `/auth/register`)
  - Se muestra el mensaje "Este email ya está registrado" en `.error-message`
- **Selectores UI:** `input#name`, `input#email`, `input#password`, `button[type="submit"]`, `.error-message`

#### [TS_005] - Protección de rutas sin autenticación
- **Objetivo:** Validar que las rutas protegidas redirigen a `/auth/login` cuando no hay token JWT.
- **Criterio de Aceptación:** authGuard redirige a `/auth/login`
- **Precondiciones:** Sin sesión activa (tokens eliminados)
- **Pasos:**
  1. Navegar directamente a `http://localhost:4200/dashboard`
  2. Navegar directamente a `http://localhost:4200/wallets`
  3. Navegar directamente a `http://localhost:4200/transactions`
- **Resultado Esperado:**
  - En todos los casos, redirección a `http://localhost:4200/auth/login`
  - No se renderiza contenido del dashboard, wallets ni transacciones
  - No hay llamadas a la API protegida (solo a auth)
- **Selectores UI:** Verificar que `.auth-card` está presente y `.page-container` no

#### [TS_006] - Logout
- **Objetivo:** Validar que el usuario puede cerrar sesión y es redirigido al login.
- **Criterio de Aceptación:** RN-012 (Logout invalida refresh token) + redirección
- **Precondiciones:**
  - Usuario autenticado (sesión activa en memoria)
- **Pasos:**
  1. Navegar a `/dashboard` estando autenticado
  2. Click en botón/ícono de Logout (según navbar implementado)
  3. Esperar respuesta de logout
- **Resultado Esperado:**
  - Redirección a `/auth/login`
  - Al intentar navegar a `/dashboard` manualmente, se redirige nuevamente a `/auth/login`
  - (Si no hay navbar visible, verificar que el token se limpia — comprobación indirecta)
- **Selectores UI:** Se requiere identificar el botón de logout en el layout compartido (navbar). Selector sugerido: `button.logout-btn`, `a[routerLink="/auth/login"]` o selector de logout.

---

### 2.2 Dashboard

#### [TS_007] - Dashboard con billeteras vacías
- **Objetivo:** Validar que el dashboard muestra el estado vacío cuando el usuario no tiene billeteras.
- **Criterio de Aceptación:** Dashboard empty state
- **Precondiciones:**
  - Usuario autenticado
  - Usuario sin billeteras creadas
- **Pasos:**
  1. Navegar a `/dashboard`
  2. Observar las tarjetas de resumen
  3. Observar la sección "Tus Billeteras"
- **Resultado Esperado:**
  - Tarjeta "Saldo Total": `$0.00`
  - Tarjeta "Billeteras": `0`
  - Sección "Tus Billeteras" muestra: "Aun no tienes billeteras." con enlace "Crear Billetera"
- **Selectores UI:** `.summary-card`, `.summary-value`, `.empty-state`, `a[routerLink="/wallets"]`

#### [TS_008] - Dashboard con billeteras y saldo
- **Objetivo:** Validar que el dashboard muestra correctamente el resumen y las billeteras del usuario.
- **Criterio de Aceptación:** CA-003 (Listar wallets con saldo)
- **Precondiciones:**
  - Usuario autenticado
  - Al menos 2 billeteras creadas (ej: "Principal" CHECKING con $1000, "Ahorro" SAVINGS con $500)
- **Pasos:**
  1. Navegar a `/dashboard`
  2. Observar las tarjetas de resumen
  3. Hacer clic en "Ver movimientos →" de una billetera
- **Resultado Esperado:**
  - "Saldo Total" muestra la suma de saldos (ej: `$1,500.00`)
  - "Billeteras" muestra el número (ej: `2`)
  - Cada billetera se muestra con su tipo, nombre y saldo
  - Al hacer clic en "Ver movimientos →", navega a `/wallets/{id}/transactions`
- **Selectores UI:** `.summary-card`, `.wallet-mini-card`, `.wallet-mini-balance`, `a.wallet-link`

---

### 2.3 Wallets (CRUD)

#### [TS_009] - Crear billetera con saldo inicial
- **Objetivo:** Validar que se puede crear una billetera con nombre, tipo y saldo inicial.
- **Criterio de Aceptación:** CA-002 (POST /wallets → 201)
- **Precondiciones:**
  - Usuario autenticado
  - No existe billetera con nombre "Ahorro Test"
- **Pasos:**
  1. Navegar a `/wallets`
  2. Click en `button` "+ Nueva Billetera"
  3. En el modal, escribir "Ahorro Test" en `input#name`
  4. Seleccionar "Ahorros" (value `SAVINGS`) en `select#type`
  5. Escribir "500.00" en `input#initialBalance`
  6. Click en `button[type="submit"]` "Crear Billetera"
- **Resultado Esperado:**
  - El modal se cierra
  - La billetera "Ahorro Test" aparece en la lista con tipo "SAVINGS" y saldo `$500.00`
  - No hay errores en consola
- **Selectores UI:** `button.btn-primary` (nueva billetera), `input#name`, `select#type`, `input#initialBalance`, `.wallet-card`

#### [TS_010] - Crear billetera con saldo inicial cero (omisión)
- **Objetivo:** Validar que se puede crear una billetera sin especificar saldo inicial (default 0.00).
- **Criterio de Aceptación:** CreateWalletRequest con initial_balance opcional (default "0.00")
- **Precondiciones:** Usuario autenticado
- **Pasos:**
  1. Navegar a `/wallets`
  2. Click en "+ Nueva Billetera"
  3. Escribir "Efectivo Diario" en `input#name`
  4. Seleccionar "Efectivo" (value `CASH`) en `select#type`
  5. Dejar `input#initialBalance` vacío
  6. Click en "Crear Billetera"
- **Resultado Esperado:**
  - La billetera se crea con saldo `$0.00`
  - Aparece en la lista de billeteras
- **Selectores UI:** `input#name`, `select#type`, `button[type="submit"]`

#### [TS_011] - Crear billetera con nombre duplicado (error 409)
- **Objetivo:** Validar que el frontend muestra error si se intenta crear una billetera con nombre ya existente.
- **Criterio de Aceptación:** 409 WALLET_NAME_DUPLICATED
- **Precondiciones:**
  - Existe billetera con nombre "Ahorro Test"
- **Pasos:**
  1. Navegar a `/wallets`
  2. Click en "+ Nueva Billetera"
  3. Escribir "Ahorro Test" en `input#name`
  4. Seleccionar tipo cualquiera
  5. Click en "Crear Billetera"
- **Resultado Esperado:**
  - El modal permanece abierto
  - Se muestra mensaje de error (dependiendo del backend: error de nombre duplicado)
  - La lista de billeteras no cambia
- **Selectores UI:** `.error-message`, `input#name`, `button[type="submit"]`

#### [TS_012] - Editar billetera
- **Objetivo:** Validar que se puede editar el nombre y tipo de una billetera existente.
- **Criterio de Aceptación:** PATCH /wallets/{id} → 200
- **Precondiciones:**
  - Existe billetera "Ahorro Test" (SAVINGS)
- **Pasos:**
  1. Navegar a `/wallets`
  2. Sobre la billetera "Ahorro Test", hacer click en botón editar (icono ✏️)
  3. En el modal, cambiar nombre a "Ahorro Editado"
  4. Cambiar tipo a "Corriente" (value `CHECKING`)
  5. Click en "Guardar Cambios"
- **Resultado Esperado:**
  - El modal se cierra
  - La billetera ahora muestra "Ahorro Editado" con tipo "CHECKING"
  - El saldo no se modifica
- **Selectores UI:** `button.btn-icon` (editar por wallet), `input#name`, `select#type`, `button[type="submit"]`

#### [TS_013] - Eliminar billetera sin transacciones
- **Objetivo:** Validar que se puede eliminar una billetera que no tiene transacciones asociadas.
- **Criterio de Aceptación:** DELETE /wallets/{id} → 204
- **Precondiciones:**
  - Existe billetera "Efectivo Diario" sin transacciones
- **Pasos:**
  1. Navegar a `/wallets`
  2. Sobre la billetera "Efectivo Diario", hacer click en botón eliminar (icono 🗑️)
  3. Confirmar en el cuadro de diálogo `confirm()` (Aceptar)
- **Resultado Esperado:**
  - La billetera desaparece de la lista
  - El total de billeteras se reduce en 1
  - No hay errores en consola
- **Selectores UI:** `button.btn-icon` (eliminar por wallet)

#### [TS_014] - Eliminar billetera con transacciones (error 409)
- **Objetivo:** Validar que no se puede eliminar una billetera que tiene transacciones asociadas.
- **Criterio de Aceptación:** RN-017 + DELETE → 409 WALLET_HAS_TRANSACTIONS
- **Precondiciones:**
  - Existe billetera "Ahorro Editado" con al menos una transacción asociada
- **Pasos:**
  1. Navegar a `/wallets`
  2. Sobre la billetera con transacciones, hacer click en botón eliminar
  3. Confirmar en el cuadro de diálogo `confirm()` (Aceptar)
- **Resultado Esperado:**
  - Se muestra alerta: "No se puede eliminar: la billetera tiene transacciones."
  - La billetera permanece en la lista
- **Selectores UI:** `button.btn-icon` (eliminar por wallet)

---

### 2.4 Transactions (CRUD + Filtros + Paginación)

#### [TS_015] - Crear transacción INCOME (ingreso)
- **Objetivo:** Validar que se puede crear un ingreso y el saldo de la billetera se incrementa.
- **Criterio de Aceptación:** RN-004 (INCOME incrementa saldo) + CA-004
- **Precondiciones:**
  - Usuario autenticado
  - Billetera "Ahorro Editado" con saldo $500.00
  - Categorías precargadas en el sistema
- **Pasos:**
  1. Navegar a `/wallets/Ahorro-Editado-id/transactions` o seleccionar la billetera desde `/transactions`
  2. Click en `button` "+ Nueva Transacción"
  3. En el modal, hacer click en `button.toggle-btn.toggle-income` "Ingreso"
  4. Seleccionar una categoría de INCOME en `select#category` (ej: "Salario")
  5. Escribir "200.00" en `input#amount`
  6. Escribir "Ingreso de prueba" en `textarea#description`
  7. Seleccionar fecha de hoy en `input#date`
  8. Click en `button[type="submit"]` "Crear Transacción"
- **Resultado Esperado:**
  - El modal se cierra
  - La transacción aparece en la tabla con tipo "Ingreso" y monto `+$200.00`
  - El saldo de la billetera se incrementa (verificar al regresar a wallets o en el selector)
- **Selectores UI:** `button.btn-primary` (nueva transacción), `button.toggle-btn.toggle-income`, `select#category`, `input#amount`, `textarea#description`, `input#date`, `button[type="submit"]`

#### [TS_016] - Crear transacción EXPENSE (gasto)
- **Objetivo:** Validar que se puede crear un gasto y el saldo de la billetera se decrementa.
- **Criterio de Aceptación:** RN-005 (EXPENSE decrementa saldo) + CA-005
- **Precondiciones:**
  - Billetera con saldo suficiente (ej: $700.00 después del INCOME anterior)
- **Pasos:**
  1. Navegar a la vista de transacciones de la misma billetera
  2. Click en "+ Nueva Transacción"
  3. Asegurar que el toggle muestra "Gasto" (valor por defecto)
  4. Seleccionar categoría de EXPENSE (ej: "Alimentación")
  5. Escribir "50.00" en `input#amount`
  6. Escribir "Compra de prueba" en `textarea#description`
  7. Click en "Crear Transacción"
- **Resultado Esperado:**
  - La transacción aparece en la tabla con tipo "Gasto" y monto `-$50.00`
  - El saldo de la billetera se decrementa
- **Selectores UI:** `button.toggle-btn.toggle-expense` (por defecto activo), `select#category`, `input#amount`, `button[type="submit"]`

#### [TS_017] - Crear transacción EXPENSE con saldo insuficiente (error 422)
- **Objetivo:** Validar que el frontend muestra error al intentar un gasto mayor al saldo disponible.
- **Criterio de Aceptación:** RN-001 + CA-006 → 422 WALLET_INSUFFICIENT_BALANCE
- **Precondiciones:**
  - Billetera con saldo conocido (ej: $650.00 después de operaciones previas)
- **Pasos:**
  1. Navegar a la vista de transacciones de la billetera
  2. Click en "+ Nueva Transacción"
  3. Mantener tipo "Gasto"
  4. Seleccionar categoría EXPENSE
  5. Escribir "999999.99" en `input#amount` (monto mayor al saldo)
  6. Click en "Crear Transacción"
- **Resultado Esperado:**
  - El modal permanece abierto
  - Se muestra mensaje de error: "Saldo insuficiente en la billetera." en `.error-message`
  - La transacción no se agrega a la tabla
  - El saldo de la billetera no cambia
- **Selectores UI:** `.error-message`, `input#amount`, `button[type="submit"]`

#### [TS_018] - Listar transacciones con filtros
- **Objetivo:** Validar que los filtros por categoría, tipo y rango de fechas funcionan correctamente.
- **Criterio de Aceptación:** CA-009 (Filtros funcionan)
- **Precondiciones:**
  - Al menos 3 transacciones de diferentes tipos/categorías/fechas en la billetera
- **Pasos:**
  1. Navegar a la vista de transacciones con una billetera seleccionada
  2. En `select#filter-type`, seleccionar "Ingreso"
  3. Verificar que solo se muestran transacciones INCOME
  4. En `select#filter-category`, seleccionar una categoría específica
  5. Verificar el filtrado combinado
  6. En `input#filter-date-from` y `input#filter-date-to`, establecer un rango de fechas
  7. Click en `button` "Limpiar filtros" (si aparece)
- **Resultado Esperado:**
  - Al aplicar filtro por tipo: solo transacciones del tipo seleccionado
  - Al aplicar filtro por categoría: solo transacciones de esa categoría
  - Al aplicar filtro por fecha: solo transacciones dentro del rango
  - Al combinar filtros: se aplican todos simultáneamente
  - "Limpiar filtros" restablece la vista completa
- **Selectores UI:** `select#filter-type`, `select#filter-category`, `input#filter-date-from`, `input#filter-date-to`, `button` (Limpiar filtros)

#### [TS_019] - Paginación de transacciones
- **Objetivo:** Validar que la paginación permite navegar entre páginas de transacciones.
- **Criterio de Aceptación:** Endpoint paginado con page/size/total_elements/total_pages
- **Precondiciones:**
  - Billetera con más de 20 transacciones (o usar size pequeño via test)
- **Pasos:**
  1. Navegar a la vista de transacciones
  2. Verificar que aparece el control de paginación
  3. Click en "Siguiente →"
  4. Click en "← Anterior"
- **Resultado Esperado:**
  - La paginación muestra "Página X de Y (Z transacciones)"
  - "Siguiente" carga la página siguiente
  - "Anterior" vuelve a la página anterior
  - Los botones se deshabilitan en los extremos (Anterior en página 1, Siguiente en última página)
- **Selectores UI:** `.pagination`, `button` (Anterior/Siguiente), `.pagination-info`

#### [TS_020] - Editar transacción
- **Objetivo:** Validar que se puede editar una transacción existente.
- **Criterio de Aceptación:** PATCH /transactions/{id} → 200
- **Precondiciones:**
  - Existe al menos una transacción en la billetera
- **Pasos:**
  1. Navegar a la vista de transacciones
  2. Sobre una transacción, hacer click en `button` "Editar"
  3. En el modal, cambiar el monto (ej: de "200.00" a "250.00")
  4. Modificar la descripción
  5. Click en "Guardar Cambios"
- **Resultado Esperado:**
  - El modal se cierra
  - La transacción actualizada aparece en la tabla con los nuevos valores
  - El saldo de la billetera se recalcula (si cambió el monto)
- **Selectores UI:** `button` (Editar en fila), `input#amount`, `textarea#description`, `button[type="submit"]`

#### [TS_021] - Eliminar transacción
- **Objetivo:** Validar que se puede eliminar una transacción y se revierte su efecto sobre el saldo.
- **Criterio de Aceptación:** RN-006 (DELETE revierte efecto)
- **Precondiciones:**
  - Existe al menos una transacción en la billetera
- **Pasos:**
  1. Navegar a la vista de transacciones
  2. Sobre una transacción INCOME, hacer click en `button` "Eliminar"
  3. Confirmar en el cuadro de diálogo `confirm()` (Aceptar)
- **Resultado Esperado:**
  - La transacción desaparece de la tabla
  - El saldo de la billetera se revierte (se decrementa si era INCOME, se incrementa si era EXPENSE)
- **Selectores UI:** `button.btn-sm.btn-danger-outline` (Eliminar en fila)

---

### 2.5 Drag & Drop

#### [TS_022] - Drag & Drop: mover transacción a otra wallet
- **Objetivo:** Validar que se puede arrastrar una transacción de una billetera a otra y que los saldos se actualizan atómicamente.
- **Criterio de Aceptación:** CA-007 (Transferencia atómica) + Optimistic UI visual
- **Precondiciones:**
  - Al menos 2 billeteras: "Origen" (saldo $500.00) y "Destino" (saldo $1000.00)
  - Al menos 1 transacción en "Origen" (ej: EXPENSE $50.00)
- **Pasos:**
  1. Navegar a la vista de transacciones
  2. Seleccionar la billetera "Origen" en `select#wallet-select`
  3. Iniciar arrastre de una fila de transacción (con atributo `draggable`)
  4. Soltar sobre la tarjeta "Destino" en `.wallet-drop-grid`
- **Resultado Esperado:**
  - **Optimistic UI:** Las tarjetas de wallet muestran el badge con delta inmediato (ej: `+$50.00` en destino, `-$50.00` en origen)
  - Se muestra toast "Moviendo transacción..."
  - Al completarse: toast "Transacción movida exitosamente"
  - La transacción ya no aparece en la lista de "Origen"
  - Al seleccionar "Destino", la transacción aparece en su lista
  - Los saldos reflejan el cambio final
- **Selectores UI:** `tr.drag-transaction-row`, `.wallet-drop-card[appDroppable]`, `.toast`, `.optimistic-badge`

#### [TS_023] - Drag & Drop: misma wallet (error 409)
- **Objetivo:** Validar que arrastrar una transacción a la misma wallet muestra error sin llamar al API.
- **Criterio de Aceptación:** RN-016 (TRANSFER_SAME_WALLET) + validación frontend temprana
- **Precondiciones:**
  - Al menos 2 billeteras
  - Transacción en la billetera actual
- **Pasos:**
  1. Navegar a vista de transacciones
  2. Seleccionar billetera "Origen"
  3. Iniciar arrastre de una transacción
  4. Soltar sobre la tarjeta que indica "Billetera actual" (misma wallet)
- **Resultado Esperado:**
  - Se muestra toast informativo: "La transacción ya está en esta billetera"
  - No se realiza ninguna llamada al API `/move`
  - Los saldos no se modifican
  - No hay cambios en la lista de transacciones
- **Selectores UI:** `tr.drag-transaction-row`, `.wallet-same-wallet`, `.drop-hint-current`, `.toast`

#### [TS_024] - Drag & Drop: error API con rollback visual
- **Objetivo:** Validar que si la API falla (422 saldo insuficiente en destino), el frontend revierte el optimistic update.
- **Criterio de Aceptación:** CA-008 (Rollback en fallo) + Optimistic UI rollback
- **Precondiciones:**
  - Billetera destino con saldo `$0.00`
  - Transacción EXPENSE de `$50.00` en billetera origen con saldo `$500.00`
- **Pasos:**
  1. Navegar a vista de transacciones con billetera origen seleccionada
  2. Arrastrar transacción EXPENSE hacia la tarjeta de la billetera destino (con saldo 0)
  3. Observar optimistic update
  4. Esperar respuesta del API
- **Resultado Esperado:**
  - **Fase 1 (Optimistic):** Las tarjetas muestran deltas visuales inmediatos
  - **Fase 2 (Rollback):** Al recibir error 422, los badges de delta desaparecen
  - Se muestra toast error: "La billetera destino no tiene saldo suficiente"
  - La transacción permanece en la billetera origen
  - Los saldos visuales vuelven a su estado original
- **Selectores UI:** `.optimistic-badge`, `.toast.toast-error`, `tr.drag-transaction-row`, `.wallet-drop-card`

---

### 2.6 Categorías

#### [TS_025] - Listar categorías en el formulario de transacción
- **Objetivo:** Validar que las categorías se cargan y filtran correctamente según el tipo de transacción (INCOME/EXPENSE).
- **Criterio de Aceptación:** GET /categories → 200 + filtro por tipo en frontend
- **Precondiciones:**
  - Usuario autenticado
  - Backend con categorías precargadas
- **Pasos:**
  1. Navegar a la vista de transacciones de cualquier billetera
  2. Click en "+ Nueva Transacción"
  3. Con tipo "Gasto" activo, abrir `select#category`
  4. Hacer click en "Ingreso" en el toggle
  5. Abrir `select#category` nuevamente
- **Resultado Esperado:**
  - Con tipo "Gasto": solo categorías EXPENSE (ej: Alimentación, Transporte, Entretenimiento)
  - Con tipo "Ingreso": solo categorías INCOME (ej: Salario)
  - Cada opción muestra su icono y nombre
- **Selectores UI:** `select#category`, `button.toggle-btn.toggle-expense`, `button.toggle-btn.toggle-income`

---

## 3. Resumen de Escenarios

| ID | Escenario | Funcionalidad | Tipo | Prioridad |
|---|---|---|---|---|
| TS_001 | Registro de nuevo usuario exitoso | Auth | Happy Path | Alta |
| TS_002 | Login con credenciales válidas | Auth | Happy Path | Alta |
| TS_003 | Login con credenciales inválidas | Auth | Edge Case | Alta |
| TS_004 | Registro con email existente | Auth | Edge Case | Alta |
| TS_005 | Protección de rutas sin autenticación | Auth | Edge Case | Alta |
| TS_006 | Logout | Auth | Happy Path | Alta |
| TS_007 | Dashboard con billeteras vacías | Dashboard | Edge Case | Media |
| TS_008 | Dashboard con billeteras y saldo | Dashboard | Happy Path | Alta |
| TS_009 | Crear billetera con saldo inicial | Wallets | Happy Path | Alta |
| TS_010 | Crear billetera sin saldo inicial | Wallets | Edge Case | Alta |
| TS_011 | Crear billetera con nombre duplicado | Wallets | Edge Case | Media |
| TS_012 | Editar billetera | Wallets | Happy Path | Alta |
| TS_013 | Eliminar billetera sin transacciones | Wallets | Happy Path | Alta |
| TS_014 | Eliminar billetera con transacciones | Wallets | Edge Case | Alta |
| TS_015 | Crear transacción INCOME | Transactions | Happy Path | Alta |
| TS_016 | Crear transacción EXPENSE | Transactions | Happy Path | Alta |
| TS_017 | Crear EXPENSE con saldo insuficiente | Transactions | Edge Case | Alta |
| TS_018 | Listar transacciones con filtros | Transactions | Happy Path | Alta |
| TS_019 | Paginación de transacciones | Transactions | Happy Path | Media |
| TS_020 | Editar transacción | Transactions | Happy Path | Alta |
| TS_021 | Eliminar transacción | Transactions | Happy Path | Alta |
| TS_022 | Drag & Drop: mover a otra wallet | Drag & Drop | Happy Path | Alta |
| TS_023 | Drag & Drop: misma wallet (error) | Drag & Drop | Edge Case | Alta |
| TS_024 | Drag & Drop: error API + rollback | Drag & Drop | Edge Case | Alta |
| TS_025 | Categorías en formulario | Categories | Happy Path | Media |

---

## 4. Estrategia de Ejecución

### 4.1 Orden Sugerido
1. Ejecutar TS_001 a TS_006 (Auth) en orden secuencial
2. Ejecutar TS_007 y TS_008 (Dashboard)
3. Ejecutar TS_009 a TS_014 (Wallets) en orden
4. Ejecutar TS_015 a TS_021 (Transactions) en orden
5. Ejecutar TS_022 a TS_024 (Drag & Drop)
6. Ejecutar TS_025 (Categories)

### 4.2 Dependencias entre Escenarios
- TS_002 (Login) depende de TS_001 (Register) o datos precargados
- TS_008 (Dashboard con wallets) depende de TS_009 (Crear wallet)
- TS_012 (Editar wallet) depende de TS_009
- TS_013/TS_014 (Eliminar wallet) dependen de TS_009
- TS_015 a TS_021 (Transactions) dependen de tener al menos una wallet
- TS_022 a TS_024 (Drag & Drop) dependen de tener al menos 2 wallets y 1 transacción
- TS_025 (Categories) es independiente

### 4.3 Consideraciones de Automatización
- Los `confirm()` y `alert()` nativos del navegador deben manejarse con `page.on('dialog')` en Puppeteer/Playwright
- Las interacciones de Drag & Drop requieren simular eventos nativos del DOM (`dragstart`, `drop`, `dragend`)
- Los modales son de Angular (no nativos) y se cierran emitiendo eventos; verificar que el DOM del modal desaparezca
- Los tokens se almacenan en Signals en memoria (no localStorage/sessionStorage): limpiar recargando la página para resetear estado de autenticación
- La respuesta paginada incluye `content`, `page`, `size`, `total_elements`, `total_pages`

---

## 5. Mapeo de Criterios de Aceptación

| CA | Escenario(s) | Verificación |
|---|---|---|
| CA-001 | TS_001, TS_002 | Registro + login exitoso, redirección a /dashboard |
| CA-002 | TS_009 | POST /wallets → 201 |
| CA-003 | TS_008 | GET /wallets → 200 con balances |
| CA-004 | TS_015 | INCOME: saldo se incrementa |
| CA-005 | TS_016 | EXPENSE: saldo se decrementa |
| CA-006 | TS_017 | Gasto con saldo negativo → 422 |
| CA-007 | TS_022 | Transfer atómica exitosa |
| CA-008 | TS_024 | Rollback en fallo |
| CA-009 | TS_018 | Filtros funcionan correctamente |

---

*Fin del Plan de Pruebas Funcionales*
