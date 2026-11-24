# Suite E2E de ChatApp (Maestro)

Pruebas de extremo a extremo con [Maestro](https://maestro.dev) (CLI 2.x) sobre la app real
(debug) contra el Supabase real, con las cuentas QA. Cubre la app sección por sección, en un
dispositivo y en dos a la vez, en español e inglés.

## Estructura

```
.maestro/
  config.yaml          # suite de un dispositivo: flows/** (excluye multidevice, destructive, known-bug)
  .env.example         # copia a .env (no se versiona) y rellena
  assets/              # imagen, vídeo y PDF de prueba (setup-emulators.ps1 los copia al dispositivo)
  subflows/            # bloques reutilizables (login, abrir chat, enviar texto, selector de fotos…)
  flows/NN_<sección>/  # un flow = un comportamiento
  multidevice/<escenario>/NN_A_*.yaml, NN_B_*.yaml   # pasos alternos en dos emuladores
  scripts/             # PowerShell: preparar emuladores, idioma, ejecutar, cuentas desechables, TOTP
  out/                 # resultados (no se versiona)
```

| Sección | Qué cubre |
|---|---|
| 01_launch | arranque limpio, aviso de root, deep link inválido, atrás en la raíz |
| 02_auth | login, contraseña incorrecta, validación, pestañas, mostrar contraseña, logout |
| 03_conversations | abrir chat, silenciar, archivar, sonido, banner sin conexión, vaciar/salir (known-bug) |
| 04_status | publicar texto/imagen, cancelar, menú añadir, visor |
| 05_chat_send | texto, imagen, cámara, vídeo, PDF + visor, audio, ubicación, contacto, stickers/GIF, encuesta, programar, IA |
| 06_chat_message_actions | responder, editar, borrar, copiar, traducir, reaccionar, autodestrucción, multiselección, reenviar, buscar |
| 07_chat_menu | silenciar con duración, exportar, temporales, incógnito, multimedia, info de contacto, menú de grupo |
| 08_newchat_invitations | buscar usuario, invitar/QR, permiso de contactos denegado, pestañas de invitaciones |
| 09_groups | crear, validar, editar, enlace, admin, añadir miembro (qa3), salir |
| 10_profile | nombre, en línea, tema, QR, sesiones, diálogos peligrosos, backup/restore, bloqueo de app |
| 11_search | búsqueda global |
| 12_lifecycle | segundo plano y vuelta, rotación |
| 13_debug_menu | menú QA/Debug (solo debug): listas de difusión y estadísticas de uso |
| multidevice/ | mensajería en tiempo real, invitaciones, estados, llamadas, notificaciones, sesiones |

## Puesta en marcha

1. Emuladores `Pixel9ProXL_API36_A` (emulator-5554) y `_B` (emulator-5556), imagen con Google Play.
2. `.maestro/.env` a partir de `.env.example` (credenciales de `@claudeqa`, `@claudeqa2`, `@claudeqa3`;
   `SUPABASE_URL` + `SUPABASE_SERVICE_ROLE_KEY` solo para los flows destructivos — **nunca a CI**).
3. APK y preparación (PIN 1234 para el bloqueo de app, contacto "QA Contact", assets, APK):

```powershell
./gradlew assembleDebug
.maestro/scripts/setup-emulators.ps1 -InstallApk
```

## Ejecutar

```powershell
.maestro/scripts/run-suite.ps1                                  # toda la suite de un dispositivo, en español
.maestro/scripts/run-suite.ps1 -Locale en                       # la misma en inglés
.maestro/scripts/run-suite.ps1 -Path flows/05_chat_send         # una sección
.maestro/scripts/run-suite.ps1 -IncludeTags smoke               # lo que corre CI
.maestro/scripts/run-suite.ps1 -IncludeDestructive -IncludeTags destructive  # registro, username, 2FA, cerrar sesiones, borrar cuenta
.maestro/scripts/run-multidevice.ps1                            # escenarios de dos dispositivos
.maestro/scripts/run-multidevice.ps1 -IncludeDestructive        # + invitaciones y sesiones (cuentas desechables)
```

### En un móvil físico

```powershell
.maestro/scripts/run-suite.ps1 -Device <serial> -Physical
```

`-Physical` no escribe el PIN del emulador ni cambia el idioma ni el teclado del sistema (exige
que ya esté en el idioma pedido y desbloqueado) y excluye:

- `emulator-only`: el bloqueo de app (habría que escribir el PIN real del móvil, cosa que la
  automatización no hace) y el nombre visible (necesita desactivar la autocorrección de Gboard).
- `personal-data`, salvo con `-AllowPersonalData`: galería, ficheros, cámara, vídeo, ubicación,
  contacto, estado con imagen y la copia de seguridad (va al Google Drive de la cuenta del móvil).
  Con el interruptor, antes de empezar copia `.maestro/assets` a `Download` como lo más reciente
  (los selectores eligen esos ficheros y no fotos del dueño) y crea un contacto local sin
  sincronizar «QA Contact»; al terminar borra ambos. La ubicación y la cámara sí son las reales.

```powershell
.maestro/scripts/run-suite.ps1 -Device <serial> -Physical -AllowPersonalData -Path flows/04_status,flows/05_chat_send,flows/10_profile
```
 Los flows con estado limpio hacen `clearState`, así que
**borra los datos de ChatApp del móvil** (habrá que volver a iniciar sesión).
Además desactiva el servicio de autocompletar del móvil solo durante la ejecución y lo restaura al
terminar: si no, el Gestor de contraseñas de Google ofrece guardar las credenciales QA en la cuenta
del dueño tras cada login, en una hoja que la automatización no puede ver. También fija la
pantalla en vertical durante la ejecución (con la rotación automática, si el móvil está apoyado de
lado el teclado tapa los resultados) y restaura después la configuración de rotación.

Para ejecutar solo algunas secciones o flows, `-Path` acepta varias rutas separadas por comas:
`-Path "flows/08_newchat_invitations,flows/09_groups/create_and_manage_group.yaml"`.

Los resultados (junit, capturas, logcat) quedan en `.maestro/out/<RUN_ID>-<idioma>/`, junto con
`report.html`: un informe autocontenido (resumen, secciones, fallos con el paso fallido y su
captura, y flows no ejecutados) que `run-suite.ps1` genera al terminar. Para regenerarlo:

```powershell
.maestro/scripts/make-report.ps1 -RunDir .maestro/out/<RUN_ID>-es
```

## Convenciones

- **Arranque**: `subflows/ensure_logged_in.yaml` reabre la app conservando la sesión del flow
  anterior y solo hace el login completo (`login.yaml`: `clearState` + credenciales) si no llega a
  la lista de chats. Ahorra ~50 s por flow. Usan `login.yaml` directamente los flows que necesitan
  la app recién instalada: arranque, autenticación, logout y los que tocan ajustes persistentes
  (bloqueo de app, nombre visible, tema, copia de seguridad). Como los datos se conservan, un flow
  no puede dar por hecho un estado vacío: `open_chat_with_qa2.yaml` vacía el borrador del chat.

- **Selectores por `id`** (testTag de Compose expuesto como resource-id). Texto solo para *datos*
  (lo que el propio flow escribió, usernames), para que la suite funcione en es y en.
- Tags dentro de diálogos, menús y bottom sheets usan `Modifier.qaTag()` (`ui/common/QaTag.kt`):
  esas ventanas no heredan `testTagsAsResourceId` de `MainActivity`.
- Todo dato creado lleva `${RUN_ID}` y es **una sola palabra**: el teclado autocorrige palabras
  seguidas de espacio ("Tmp 123" acabó siendo "Temp 123").
- Para salir de un chat: `tapOn: id: chat_back_button` (un `back` con el teclado abierto solo lo cierra).
- Nombres de flow sin `: / \ * ? < > |` (Maestro crea carpetas con ellos y en Windows falla).
- **Selectores solo en ASCII**: en Windows Maestro lee mal emojis y acentos del YAML ("👍" llegaba
  como "?"). Para emojis se usa `emoji_item_<n>`; en regex, `.` en lugar de la letra acentuada.
- Tags transversales: `smoke`, `slow`, `multidevice`, `destructive`, `known-bug`, `emulator-only`, `personal-data`.
- No compiles con Gradle mientras corre la suite: un host saturado hace que los emuladores den ANR
  al arrancar la app.

## Peculiaridades del entorno

- **Idioma**: `clearState` (pm clear) borra el idioma por app de Android 13+, así que se usa el
  del sistema. `set-locale.ps1` lo cambia por Ajustes (las imágenes con Google Play no son root e
  ignoran `-change-locale`).
- **Animaciones activas**: el temporizador del visor de estados es una animación; con
  `animator_duration_scale 0` cada historia termina al instante.
- **mobile-mcp y Maestro no conviven**: ambos usan la única conexión de UiAutomation (y también
  bloquea `uiautomator dump`, que usan `set-locale.ps1` y `setup-emulators.ps1`). Los scripts
  paran el agente de mobile-mcp (`com.mobilenext.mobilecli.DeviceServer`) antes de ejecutar.
- **Teclado**: `setup-emulators.ps1` desactiva la autocorrección, mayúsculas, redacción
  inteligente y sugerencias de Gboard. Con ellas, `inputText` + Enter podía guardar una
  *sugerencia* ("claudeqa" acabó como "claudeqa2") y las palabras con espacio se corregían.
- **PIN del dispositivo**: el emulador arranca bloqueado; los scripts lo desbloquean (`Unlock-Device`).
- **Play Integrity**: no hay bypass. En emuladores el check suele dar `Error` (la app continúa);
  si diera `Failed`, `launch_clean` falla con un mensaje explícito.
- **Supabase pausado**: si el proyecto se pausa por inactividad, todos los logins fallan con
  "Unable to resolve host". Reactívalo desde el dashboard.

## Bugs encontrados por la suite

| Estado | Bug |
|---|---|
| Arreglado | `NetworkMonitor` no detectaba el modo avión (re-consultaba `activeNetwork` en `onLost`) |
| Arreglado | `MainActivity.onCreate` leía disco en el hilo principal (detección de root + prefs): StrictMode lo marcaba y en un dispositivo lento provocaba ANR en el primer frame |
| Arreglado | El banner "Sin conexión" se pintaba debajo de la barra de estado |
| Arreglado | El aviso de modo incógnito del chat también se pintaba debajo de la barra de estado |
| Arreglado | El visor de estados se cerraba solo al abrirlo mientras cargaba |
| Arreglado | Las zonas táctiles del visor tapaban el texto del estado para TalkBack/automatización |
| Arreglado | Textos de la hoja de adjuntos y del selector de stickers/GIF sin traducir |
| Arreglado | Paging del chat: tras cualquier invalidación de Room desaparecían los ~29 mensajes más nuevos (`getRefreshKey` con receta de páginas sobre claves de offset) |
| Arreglado | El chat no seguía al mensaje más reciente tras la sincronización inicial ni al enviar con la ventana de Paging llena (las burbujas recién enviadas y las encuestas, "Cargando encuesta…", quedaban fuera de vista u obsoletas) |
| Arreglado (migración aplicada) | Hacer admin / quitar admin en grupos nunca funcionaba: faltaba una política RLS de `UPDATE` para admins en `conversation_participants`, y el `update` de 0 filas pasaba como éxito (ahora la app lo detecta) |
| Arreglado (migración `20261124094500` aplicada) | Hacer admin / quitar admin seguía fallando con un 500: la política `allow_self_promote_when_no_admin` (creada fuera de las migraciones) consultaba `conversation_participants` dentro de una política de esa misma tabla (recursión infinita, 42P17), y `minimal_grants` nunca concedió `UPDATE` sobre la tabla. Ahora la comprobación va en `group_has_admin()` (`SECURITY DEFINER`) y hay `GRANT UPDATE (role)` |
| Arreglado | Al salir de un grupo desde "Info del grupo" la app volvía al chat (vacío) del grupo abandonado en lugar de a la lista |
| Arreglado | Salir de un grupo no lo quitaba de la lista: solo se borraba el miembro en Room, no la conversación ni sus mensajes |
| Abierto | El diálogo de error de la copia de seguridad muestra el mensaje crudo de la excepción, en inglés ("Failed to get Google access token", `BackupRepositoryImpl`) |
| Abierto (`known-bug`) | "Vaciar chat" solo borra Room: al abrir el chat se re-sincroniza y vuelve todo |
| Abierto | Las encuestas son solo locales (`PollRepositoryImpl` no tiene backend): el otro participante ve "Cargando encuesta…" para siempre y los votos no se comparten. Fuera de `group_realtime` hasta que se implemente |
| Observación | Cancelar una invitación enviada la marca como `rejected` (`InvitationRepositoryImpl.cancelSentInvitation`): en "Enviadas" sigue apareciendo, como "Rechazada", no como cancelada |
| Por confirmar | Los estados con imagen se ven en blanco en el visor |

## Huecos conocidos (no automatizables o sin UI)

- Intents sin UI: fijar/guardar mensaje, reintentar envío, transcribir audio, filtros/orden/búsqueda
  de la lista, tema y fondo del chat (ocultos), borrar estado y color del estado, resumir con IA
  (`ChatIntent.AiSummarize` no tiene botón en la hoja de IA).
- Google Sign-In (hoja del sistema de Credential Manager).
- Las acciones del menú de la lista (borrar, vaciar, salir) no piden confirmación: observación de
  producto, no fallo de test.
- En "Nuevo grupo", el *atrás* del sistema en el paso 2 sale de la pantalla en lugar de volver al
  paso 1 (la flecha de la barra sí vuelve): observación de UX.
