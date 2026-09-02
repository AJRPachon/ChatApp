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

## Ejecutar

```bash
# Smoke test (no requiere credenciales)
maestro test .maestro/flows/smoke_launch.yaml

# Flujo de login (requiere QA_EMAIL / QA_PASSWORD)
cp .maestro/.env.example .maestro/.env   # y rellena las credenciales reales
maestro test .maestro/flows/login_email.yaml --env-file .maestro/.env

# Todos los flows en orden (ver config.yaml)
maestro test .maestro
```

`maestro studio` abre un inspector interactivo para explorar el árbol de la UI
y grabar pasos nuevos.

## Credenciales de la cuenta QA

Este repo es público — nunca se commitea `.maestro/.env` (está en `.gitignore`).
Las credenciales reales de `@claudeqa` viven solo en la memoria local de
Claude Code / el usuario, no en el repo.

## Convenciones

- Los flows seleccionan por `id` (`testTag` de Compose) cuando el texto no es
  único o único de forma fiable (p. ej. la pestaña "Iniciar sesión" y el botón
  de submit comparten literalmente el mismo texto). Al añadir un flow nuevo,
  añade un `Modifier.testTag("...")` al elemento en vez de depender de índices
  de texto ambiguos.
- Un flow por archivo, nombrado por lo que verifica (`login_email.yaml`, no
  `test1.yaml`).
- No incluir datos que muten estado compartido de forma destructiva (usar
  siempre la cuenta QA, nunca `@ajrpachon`).

## Pendiente / ideas

- Flow de registro (`sign up`) con limpieza posterior de la cuenta creada.
- Flow de envío de mensaje entre `@claudeqa` y `@claudeqa2` (dos instancias).
- Integrar como job opcional de CI (requiere un emulador headless en el
  runner; de momento se ejecuta solo en local).
