# Maestro E2E flows

Pruebas de UI end-to-end (caja negra, sobre un APK real en emulador/dispositivo)
con [Maestro](https://maestro.dev). Complementan, no sustituyen, los tests
unitarios (`app/src/test`) e instrumentados (`app/src/androidTest`).

## Requisitos

- CLI de Maestro instalado (`maestro --version`). Instalación:
  `curl -Ls "https://get.maestro.mobile.dev" | bash` (Linux/macOS/WSL) o ver
  [docs de instalación en Windows](https://docs.maestro.dev/getting-started/installing-maestro).
- Un emulador corriendo o un dispositivo conectado (`adb devices`).
- La app instalada en debug: `./gradlew installDebug --no-daemon`.

## Estructura

```
.maestro/
  config.yaml           # orden de ejecución de los flows "runnables"
  flows/                 # flows ejecutables directamente (uno = un escenario)
    smoke_launch.yaml
    login_email.yaml
    login_logout_roundtrip.yaml
    send_message.yaml
  subflows/               # bloques reutilizables, solo vía `runFlow`, nunca standalone
    login_qa.yaml         # parametrizado: env LOGIN_EMAIL / LOGIN_PASSWORD
    logout.yaml
    open_conversation.yaml   # parametrizado: env CONTACT_NAME
    delete_own_message.yaml  # parametrizado: env MESSAGE_TEXT
```

Siempre se ejecuta apuntando a `flows/` (archivo o carpeta), nunca a `.maestro`
a secas — así nunca se corre un subflow suelto (le faltaría contexto: espera
partir de una pantalla concreta, no del arranque de la app).

## Ejecutar

```bash
# Smoke test (no requiere credenciales)
maestro test .maestro/flows/smoke_launch.yaml

# Flujo de login (requiere QA_EMAIL / QA_PASSWORD)
cp .maestro/.env.example .maestro/.env   # y rellena las credenciales reales
set -a && source .maestro/.env && set +a
maestro test .maestro/flows/login_email.yaml -e QA_EMAIL="$QA_EMAIL" -e QA_PASSWORD="$QA_PASSWORD"

# Todos los flows ejecutables, en el orden de config.yaml
maestro test .maestro/flows -e QA_EMAIL="$QA_EMAIL" -e QA_PASSWORD="$QA_PASSWORD"
```

`maestro studio` abre un inspector interactivo para explorar el árbol de la UI
y grabar pasos nuevos.

## Credenciales de la cuenta QA

Este repo es público — nunca se commitea `.maestro/.env` (está en `.gitignore`).
Las credenciales reales de `@claudeqa` viven solo en la memoria local de
Claude Code / el usuario, no en el repo.

## Selectores: por qué `id` y no texto

No hay `testTag` por defecto en Compose expuesto a UiAutomator/Maestro; hay
que activarlo explícitamente. Se hace una única vez en la raíz del contenido
(`MainActivity.kt`, `Box` que envuelve `NavDisplay`):

```kotlin
Modifier.semantics { testTagsAsResourceId = true }
```

A partir de ahí, cualquier `Modifier.testTag("algo")` en un composable
aparece como `resource-id` y se puede seleccionar con `id: "algo"` en un
flow. Preferir `id` sobre texto salvo que el texto sea:

- único en la pantalla, **y**
- idéntico en `values/strings.xml` (es) y `values-en/strings.xml` (en) —
  el emulador arranca en inglés por defecto, así que un flow que busque
  "Correo electrónico" falla contra "Email". "Chats" y "ChatApp" son
  ejemplos de textos seguros porque no se tradujeron.

IDs ya disponibles: `auth_email_field`, `auth_password_field`,
`auth_submit_button` (`AuthScreen`), `conversation_list_profile_button`
(`ConversationListScreen`), `profile_sign_out_button` (`ProfileScreen`),
`chat_input_field`, `chat_send_button` (`ChatInputBar`).

**Excepciones donde se usa texto en vez de `id`:**

- Contenido generado por el usuario (nombre de un contacto, texto de un
  mensaje): no tiene ni puede tener un `testTag` estático
  (`open_conversation.yaml` hace `tapOn: ${CONTACT_NAME}`). Los nombres
  de usuario no se traducen, así que siguen siendo seguros entre locales.
- **`DropdownMenu` (Compose Material3)**: sus items se renderizan en un
  `Popup` (ventana Android separada), y `testTagsAsResourceId` no se
  propaga a esa ventana — comprobado en `chat_message_delete_menu_item`:
  el `testTag()` estaba puesto pero Maestro nunca lo encontraba, aunque
  el menú sí estaba abierto en la captura de pantalla. Dentro de un
  `DropdownMenu`, selecciona por texto (`delete_own_message.yaml`) y
  comprueba que el texto sea único en pantalla en ese momento concreto
  del flow, ya que puede repetirse en otra parte de la UI que no esté
  visible simultáneamente.

## Subflows reutilizables

Bloques de pasos, no escenarios en sí — se componen desde un flow con
`runFlow`. Un subflow que necesita datos de quien lo llama declara sus
propias variables de entorno (p. ej. `LOGIN_EMAIL`/`LOGIN_PASSWORD`,
`CONTACT_NAME`) y el flow se las pasa con la forma larga de `runFlow`:

```yaml
appId: com.ajrpachon.chatapp
---
- runFlow:
    file: ../subflows/login_qa.yaml
    env:
      LOGIN_EMAIL: ${QA_EMAIL}
      LOGIN_PASSWORD: ${QA_PASSWORD}
- runFlow:
    file: ../subflows/open_conversation.yaml
    env:
      CONTACT_NAME: claudeqa2
- runFlow: ../subflows/logout.yaml   # sin params, forma corta
```

Al añadir un flow nuevo que necesite partir de una sesión iniciada,
reutiliza `login_qa.yaml` en vez de repetir los pasos de login. Si un paso
nuevo (navegar a una pantalla, abrir un diálogo) se va a repetir en más de
un flow, extráelo a `subflows/` siguiendo el mismo patrón — parametrízalo
con `env` en vez de hardcodear datos concretos, para que otro flow lo
pueda reutilizar con otra cuenta/contacto/valor.

## Convenciones

- Un flow ejecutable por archivo en `flows/`, nombrado por lo que verifica
  (`login_email.yaml`, no `test1.yaml`). Los bloques compartidos van en
  `subflows/`.
- Al añadir un elemento nuevo a seleccionar, añade
  `Modifier.testTag("pantalla_elemento")` en el composable en vez de
  depender de texto o de índices ambiguos.
- No incluir datos que muten estado compartido de forma destructiva (usar
  siempre la cuenta QA, nunca `@ajrpachon`). `login_logout_roundtrip`
  deja la app deslogueada al terminar, igual que al empezar.

## Higiene de datos

`send_message.yaml` borra el mensaje que envía nada más verificarlo (vía
`subflows/delete_own_message.yaml`), así que se puede correr en bucle sin
ir acumulando mensajes de prueba **con contenido legible** en la
conversación de QA. El borrado en la app es un soft-delete — deja un
placeholder ("This message was deleted"/"Se eliminó este mensaje") en
vez de desaparecer del todo — así que la conversación sí acumula esos
placeholders con cada ejecución; es ruido inofensivo, no un dato
sensible ni de prueba con contenido.

Sigue el mismo principio en cualquier flow nuevo que escriba datos: si el
flow los crea, el propio flow los borra al final.

## Pendiente / ideas

- Flow de registro (`sign up`) con limpieza posterior de la cuenta creada.
- Flow de creación de contacto/chat nuevo (`send_message.yaml` asume que
  la conversación `@claudeqa` ↔ `@claudeqa2` ya existe).
- Integrar como job opcional de CI (requiere un emulador headless en el
  runner; de momento se ejecuta solo en local).
