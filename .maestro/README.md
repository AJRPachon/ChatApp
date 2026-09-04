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
    new_chat_search.yaml
    global_search.yaml
    archive_unarchive_roundtrip.yaml
    mute_unmute_roundtrip.yaml
    theme_toggle_roundtrip.yaml
    send_message.yaml
    message_reaction_roundtrip.yaml
    send_image_message.yaml
    send_camera_photo_message.yaml
    send_video_message.yaml
    send_file_message.yaml
    send_audio_message.yaml
    send_location_message.yaml
    send_contact_message.yaml
    background_resume_chat_list.yaml
    background_resume_chat_screen.yaml
    background_resume_profile.yaml
    contact_info_navigation.yaml
    chat_media_gallery_navigation.yaml
    in_chat_search_navigation.yaml
    incognito_mode_roundtrip.yaml
    session_audit_navigation.yaml
    invitations_screen_navigation.yaml
    edit_message_roundtrip.yaml
    sticker_picker_navigation.yaml
    chat_theme_picker_navigation.yaml
    disappearing_mode_navigation.yaml
    create_group_navigation.yaml
    notification_sound_picker_navigation.yaml
    sign_out_all_devices_cancel.yaml
    profile_display_name_roundtrip.yaml
    profile_online_status_roundtrip.yaml
    my_qr_code_navigation.yaml
    forward_message_dialog_navigation.yaml
    ephemeral_message_dialog_navigation.yaml
    mute_duration_dialog_navigation.yaml
    chat_wallpaper_picker_navigation.yaml
    poll_create_navigation.yaml
    poll_create_send_roundtrip.yaml
    ai_assistant_navigation.yaml
    clear_chat_roundtrip.yaml
    status_compose_navigation.yaml
    realtime/              # flujo multi-dispositivo, fuera de la suite de un solo device
      01_recipient_wait.yaml
      02_sender_send.yaml
      03_recipient_verify.yaml
      04_sender_cleanup.yaml
      run.sh
  subflows/               # bloques reutilizables, solo vía `runFlow`, nunca standalone
    login_qa.yaml               # parametrizado: env LOGIN_EMAIL / LOGIN_PASSWORD
    logout.yaml
    open_conversation.yaml      # parametrizado: env CONTACT_NAME
    delete_own_message.yaml     # parametrizado: env MESSAGE_TEXT
    send_chat_message.yaml      # parametrizado: env MESSAGE_TEXT
    react_to_message.yaml       # parametrizado: env MESSAGE_TEXT, EMOJI
    background_and_resume.yaml  # sin parámetros
    open_chat_menu_item.yaml    # parametrizado: env MENU_ITEM_TEXT
    open_new_chat_search.yaml     # parametrizado: env QUERY
    global_search.yaml             # parametrizado: env QUERY
    archive_conversation.yaml     # parametrizado: env CONTACT_NAME
    unarchive_conversation.yaml   # parametrizado: env CONTACT_NAME
    mute_conversation.yaml        # parametrizado: env CONTACT_NAME
    unmute_conversation.yaml      # parametrizado: env CONTACT_NAME
    set_theme.yaml                 # parametrizado: env THEME_OPTION
    delete_selected_message.yaml         # parametrizado: env SELECT_TARGET
    delete_selected_media_message.yaml   # parametrizado: env POINT
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

## Setup previo para los flows de adjuntos

Siete flows envían un adjunto real (`send_image_message.yaml`,
`send_camera_photo_message.yaml`, `send_video_message.yaml`,
`send_file_message.yaml`, `send_audio_message.yaml`,
`send_location_message.yaml`, `send_contact_message.yaml`) y necesitan
estado del dispositivo que ningún paso de Maestro puede crear (no existe
un comando de shell dentro de un flow). A diferencia de las credenciales
QA, este estado es duradero — sobrevive a `launchApp: clearState` (que
solo limpia `com.ajrpachon.chatapp`, no el resto del dispositivo) y a
sucesivas ejecuciones — así que se hace una sola vez por emulador, no
antes de cada run:

```bash
# Permisos (sin diálogo del sistema que estos flows puedan pulsar)
adb -s emulator-5556 shell pm grant com.ajrpachon.chatapp android.permission.CAMERA
adb -s emulator-5556 shell pm grant com.ajrpachon.chatapp android.permission.RECORD_AUDIO
adb -s emulator-5556 shell pm grant com.ajrpachon.chatapp android.permission.READ_CONTACTS
adb -s emulator-5556 shell pm grant com.ajrpachon.chatapp android.permission.ACCESS_FINE_LOCATION
adb -s emulator-5556 shell pm grant com.ajrpachon.chatapp android.permission.ACCESS_COARSE_LOCATION

# Ubicación fija y determinista (persiste hasta reiniciar el emulador)
adb -s emulator-5556 emu geo fix -3.7038 40.4168

# Imagen semilla para el Photo Picker (Attach → Galería)
adb -s emulator-5556 push seed.jpg /sdcard/Pictures/maestro_seed.jpg
adb -s emulator-5556 shell content insert --uri content://media/external/images/media --bind _data:s:/sdcard/Pictures/maestro_seed.jpg
adb -s emulator-5556 shell am force-stop com.google.android.photopicker
adb -s emulator-5556 shell am force-stop com.google.android.providers.media.module

# Archivo semilla para el selector de documentos (Attach → Archivo)
adb -s emulator-5556 push seed.txt /sdcard/Download/maestro_seed.txt

# Un contacto de dispositivo para Attach → Contacto (ver
# send_contact_message.yaml para por qué los extras -e name/-e phone de
# `am start` no rellenaron el formulario de forma fiable en esta imagen
# de emulador, y hubo que escribirlos a mano/por adb después)
adb -s emulator-5556 shell am start -a android.intent.action.INSERT -t vnd.android.cursor.dir/person
# … completar "First name"/"Phone" y pulsar "Save" (a mano o con adb input) …
```

`send_image_message.yaml` deja documentado un matiz importante: cualquier
`adb shell screencap` directo a almacenamiento del dispositivo (incluida
la raíz de `/sdcard`, no solo `Pictures`/`DCIM`) se auto-indexa como foto
y contamina el Photo Picker en ejecuciones futuras — usar
`adb exec-out screencap -p > local.png` en su lugar para depurar, que
nunca toca el almacenamiento del dispositivo.

## Credenciales de la cuenta QA

Este repo es público — nunca se commitea `.maestro/.env` (está en `.gitignore`).
Las credenciales reales de `@claudeqa` viven solo en la memoria local de
Claude Code / el usuario, no en el repo.

## Los selectores de texto exigen coincidencia completa, no subcadena

`assertVisible`/`assertNotVisible`/`tapOn` con una cadena de texto la
tratan como un regex de Java evaluado con `matches()` — tiene que
coincidir con **todo** el texto del nodo, no basta con que el nodo lo
contenga. Verificado directamente: contra un botón mostrando
literalmente "Next (1 selected)", ni `assertVisible: "selected"` ni
`assertVisible: "Next"` (ninguno de los dos con nada especial de regex)
encontraron el elemento — solo funcionó el texto completo, con los
paréntesis escapados (`"Next \\(1 selected\\)"`; sin escapar, un
paréntesis literal se interpreta como grupo de captura, no como
carácter). Esto también explica por qué algunas aserciones de esta
suite que parecían comprobar un mensaje más largo ("no se encontró
usuario con...") en realidad solo confirman que el campo de búsqueda
sigue mostrando el texto tecleado (que sí es el texto completo de
*ese* nodo) — funcionan, pero no verifican lo que el comentario
original decía.

Al escribir una aserción nueva contra un texto con partes dinámicas
incrustadas en una frase más larga (p. ej. un contador: "Next (N
selected)"), o bien conoces el texto completo exacto y lo pones entero
(escapando regex especiales como paréntesis), o usas `.*` alrededor de
la parte que no controlas (`".*selected.*"`) — nunca asumas que un
fragmento suelto bastará.

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
`auth_submit_button` (`AuthScreen`); `conversation_list_profile_button`,
`conversation_list_new_chat_fab`, `conversation_list_archived_button`,
`conversation_list_search_button` (`ConversationListScreen`);
`profile_sign_out_button`, `profile_theme_system_option` / `_light_` /
`_dark_option` (`ProfileScreen`); `newchat_search_field`
(`NewChatScreen`); `global_search_field` (`GlobalSearchScreen`);
`chat_input_field`, `chat_send_button` (`ChatInputBar`);
`chat_top_bar_title`, `chat_top_bar_menu_button` (`ChatTopBar`);
`chat_search_field` (`ChatSearchOverlay`); `profile_session_audit_row`,
`profile_sign_out_all_button`, `profile_display_name_field`,
`profile_online_status_switch`, `profile_my_qr_code_button`
(`ProfileScreen`); `conversation_list_invitations_button`,
`conversation_list_new_group_fab` (`ConversationListScreen`);
`create_group_search_field`, `create_group_next_button`
(`CreateGroupScreen`); `chat_app_top_bar_back_button` (`ChatAppTopBar`,
compartido por varias pantallas: `NewChatScreen`, `InvitationsScreen`,
`CreateGroupScreen`, `SessionAuditScreen`...).

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
  `DropdownMenu`/`ModalBottomSheet`, selecciona por texto
  (`delete_own_message.yaml`, `react_to_message.yaml`) y comprueba que
  el texto sea único en pantalla en ese momento concreto del flow, ya
  que puede repetirse en otra parte de la UI que no esté visible
  simultáneamente. Un emoji (`"😀"`) es texto normal para Compose/Maestro
  y se selecciona igual — sin equivalente de `id` posible ahí.
- **Un elemento visible en pantalla puede no existir en el árbol que lee
  Maestro, sin que sea un problema de accesibilidad real**: el banner de
  "Modo incógnito" del chat se ve perfectamente en una captura de
  pantalla, pero una búsqueda de su texto en el volcado de jerarquía de
  Maestro (`screen-hierarchy/*.json`) da cero resultados. Verificado que
  **no** es un bug de accesibilidad de la app: un `adb shell uiautomator
  dump` directo sobre la misma pantalla, en el mismo momento, sí
  encuentra el texto (`text="Incognito mode — messages are not
  saved"`) — es decir, TalkBack y el resto de servicios de accesibilidad
  reales lo leen bien. Es una inconsistencia del propio driver de
  Maestro (probablemente por ser edge-to-edge sobre la status bar) frente
  al UiAutomator del sistema, no algo que arreglar en el código de la
  app. Cuando algo así pase, primero descarta con un `uiautomator dump`
  directo si es un problema de Maestro o de la app, y si es de Maestro
  busca un efecto secundario alcanzable en su lugar (aquí: el propio
  texto del ítem del menú cambia de "Modo incógnito" a "Desactivar
  incógnito", y eso sí es seleccionable).
- **Un campo de búsqueda puede colisionar con su propio resultado**: en
  "Nuevo grupo", buscar "claudeqa2" deja ese texto tanto en el campo de
  búsqueda como en la fila de resultado — `tapOn: "claudeqa2"` es
  ambiguo entre ambos y puede tocar el campo en vez de la fila (probado:
  dejó "0 selected" en vez de seleccionar al usuario). Selecciona por
  algo que solo esté en la fila, como el `@username` del subtítulo
  (`tapOn: "@claudeqa2"`), no por el nombre a secas.
- **`ModalBottomSheet` tampoco propaga `testTagsAsResourceId`**, no solo
  `DropdownMenu`: `poll_create_send_roundtrip.yaml` añadió
  `Modifier.testTag("poll_create_button")` al botón de
  `CreatePollSheetContent` y Maestro nunca lo encontró por `id`, aunque
  el propio texto del botón sí era visible en pantalla en ese momento —
  se revirtió el `testTag` (no aporta nada aquí) y en su lugar se
  selecciona por texto con `index: 1`, ya que "Crear encuesta"/
  "Create poll" aparece dos veces a la vez (título de la hoja + botón) y
  el texto por sí solo es ambiguo. Confirma que la excepción de arriba
  aplica a *cualquier* contenido de un `ModalBottomSheet`, no solo a los
  `DropdownMenu`.
- **Un `Modifier.clickable` propio en el contenido de un mensaje se come
  el long-press antes de que llegue al selector múltiple de la fila**:
  visto en `send_image_message.yaml` (la `AsyncImage` de una imagen sin
  texto tiene `.clickable { onImageClick(...) }` para abrir el visor a
  pantalla completa), y de nuevo en `send_file_message.yaml`,
  `send_location_message.yaml`, `send_contact_message.yaml` y
  `send_camera_photo_message.yaml`/`send_video_message.yaml` — todos con
  su propio `clickable` de un solo toque (abrir imagen/archivo/Maps/
  intent de contacto/reproductor). Un long-press real y sostenido sobre
  ese contenido se resuelve como un simple toque (abre el visor/intent)
  en vez de disparar `onLongClick = onToggleSelect` en la fila que lo
  envuelve — verificado con una pulsación larga real, no una suposición
  de lectura de código. `subflows/delete_selected_media_message.yaml`
  evita esto pulsando el margen en blanco junto a la burbuja (la fila
  completa ocupa el 100% del ancho pero la burbuja está capada al 85% y
  pegada al borde saliente, dejando un margen sin `clickable` propio en
  el lado "interior") en vez del contenido de la burbuja en sí. El audio
  (`send_audio_message.yaml`) es la excepción: su forma de onda es un
  `Canvas` puro sin gesto propio, así que un long-press directo sobre
  ella sí llega al selector múltiple sin necesidad de este rodeo.

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

## Background / resume

`subflows/background_and_resume.yaml` manda la app a segundo plano
(tecla Home) y la trae de vuelta **sin matar el proceso**
(`launchApp: stopApp: false`), para comprobar que la pantalla actual
sobrevive a un ciclo de segundo plano → primer plano — no que la app
simplemente arranca bien desde cero.

**Trampa real que encontré**: `launchApp` normal siempre hace
force-stop de la app antes de lanzarla (comportamiento por defecto de
Maestro, visible en el log como "Stopping app during launch" incluso
sin `clearState`). Si usas `launchApp` a secas después de un `pressKey:
Home` para "volver" a la app, en realidad la estás matando y
reiniciando — el test pasaría igual, pero estarías probando un cold
start, no un resume real, y no detectarías una regresión donde volver
de segundo plano resetea la navegación. Hay que pasar `stopApp: false`
explícitamente.

Tres flows usan este subflow para tres pantallas distintas:
`background_resume_chat_list.yaml` (lista de chats),
`background_resume_chat_screen.yaml` (un chat abierto — la pantalla más
profunda en el back stack, y la más fácil de perder si algo reconstruye
mal el `NavBackStack`), `background_resume_profile.yaml` (perfil).

## Flujo multi-dispositivo (Realtime)

`flows/realtime/` prueba algo que ningún flow de un solo dispositivo puede
probar: que un mensaje llega **por Supabase Realtime**, no solo que se
guarda. Necesita dos emuladores/dispositivos corriendo a la vez, uno por
cada cuenta QA, y no encaja en `config.yaml`/`maestro test .maestro/flows`
(que asume un solo dispositivo) — por eso vive en su propia carpeta,
fuera de esa suite.

Son 4 pasos secuenciales, cada uno apuntado a un dispositivo concreto con
`--device`:

1. **`01_recipient_wait.yaml`** (dispositivo receptor): login, abre la
   conversación con el emisor y se queda ahí — deliberadamente sin volver
   a lanzar la app después de esto.
2. **`02_sender_send.yaml`** (dispositivo emisor): login, abre la
   conversación, envía "Maestro realtime ping".
3. **`03_recipient_verify.yaml`** (mismo dispositivo receptor, **sin
   `launchApp`**): comprueba que el mensaje ya está visible. Al no
   relanzar la app, esto prueba una recepción en vivo, no una carga al
   abrir pantalla.
4. **`04_sender_cleanup.yaml`** (mismo dispositivo emisor, sin
   `launchApp`): borra el mensaje de prueba.

`run.sh` orquesta los 4 pasos con un solo comando:

```bash
bash .maestro/flows/realtime/run.sh \
  <sender_device> <sender_email> <sender_password> <sender_target_username> \
  <recipient_device> <recipient_email> <recipient_password> <recipient_target_username>

# Ejemplo real con las dos cuentas QA:
set -a && source .maestro/.env && set +a
bash .maestro/flows/realtime/run.sh \
  emulator-5554 "$QA_EMAIL" "$QA_PASSWORD" claudeqa2 \
  emulator-5556 claude.qa2.chatapp@gmail.com "$QA_PASSWORD" claudeqa
```

Verificado end-to-end con dos emuladores reales (`Pixel9ProXL_API36_A` +
`_B`): el mensaje enviado en el emisor apareció en el receptor sin
recargar ni relanzar la app.

## Hallazgos de UX reales encontrados (y su estado)

- **Cursor al editar un mensaje (arreglado)**: `edit_message_roundtrip.yaml`
  encontró que al pulsar "Editar" en un mensaje propio, el campo de texto
  se rellenaba con el contenido original pero el cursor quedaba en la
  posición 0 en vez de al final — cualquier edición insertaba al
  principio en vez de añadir/reemplazar al final (verificado:
  `"Maestro edit updatedMaestro edit original"`). Arreglado en
  `ChatAppTextField` (ver historial de git); el flow ya no necesita el
  workaround de "Select all" para funcionar, aunque lo sigue usando
  porque es más explícito sobre la intención del test.
- **`BackHandler` ausente en pantallas con navegación interna propia**:
  el botón de sistema/gesto "atrás" de Android puede comportarse
  distinto del botón "atrás" dibujado en pantalla cuando la pantalla
  gestiona su propio estado de navegación interna sin un `BackHandler`
  que intercepte el back del sistema. Visto dos veces en pantallas no
  relacionadas: `ChatSearchOverlay` (el back del sistema cierra todo el
  chat en vez de solo la búsqueda) y `CreateGroupScreen` (el back del
  sistema en el paso "info del grupo" sale directamente a la lista de
  chats, descartando la selección de miembros, en vez de volver al paso
  de selección como sí hace la flecha de la topbar). Ningún flow de esta
  suite usa `- back` de Maestro en ninguna de las dos pantallas por esta
  razón — usan el `id` del botón/icono en pantalla en su lugar. Podría
  valer la pena una auditoría general de pantallas con pasos/wizards
  internos para ver si el patrón se repite en más sitios.
- **"Forward" no hacía nada observable (arreglado)**:
  `forward_message_dialog_navigation.yaml` encontró que tocar "Reenviar"
  en el menú de un mensaje no abría ningún diálogo — sin diálogo, sin
  snackbar visible, sin crash. Causa raíz real, encontrada añadiendo un
  `AppLogger.e` temporal en el `onFailure` de
  `ChatForwardDelegate.showForwardDialog` (que antes tragaba la
  excepción en silencio sin loguearla — por eso "sin error en logcat"
  era literalmente cierto): `conversationRepository.observeConversations(uid)`
  abre varios canales de Supabase Realtime con un topic **fijo** por
  usuario (`"participants:$userId"`, `"messages:list:$userId"`). Si esa
  Flow ya tiene un colector vivo para ese mismo usuario en el momento de
  tocar "Reenviar" — típicamente `ConversationListViewModel`, que sigue
  vivo en el back stack de Navigation 3 mientras el chat está abierto
  encima — el segundo `.channel(...)` con el mismo nombre de topic
  devuelve el mismo `RealtimeChannel` ya unido (`supabase-kt` busca
  canales por topic), y su `postgresChangeFlow(...)` lanza
  `IllegalStateException: You cannot call postgresChangeFlow after
  joining the channel`. `catchResult` atrapaba esa excepción y el
  `onFailure` solo ponía `state.error` (un snackbar fácil de perder en
  una prueba manual rápida) sin tocar `state.forward.showDialog` — así
  que el diálogo, poblado o vacío, nunca se llegaba a mostrar. El propio
  código de `ForwardConversationDialog` sí contemplaba una lista vacía
  correctamente (nunca fue un problema de `ChatDialogHost` ni de
  observación de estado); el fallo ocurría antes, en el propio
  repositorio. Arreglado en `ConversationRepositoryImpl.observeConversations`
  dándole a los cuatro canales un sufijo único por suscripción
  (`${System.nanoTime()}`) — patrón que ya se usaba en dos de los cuatro
  canales (`conversationsUpdateChannel`, `profilesChannel`) pero no en
  los otros dos (`participantsChannel`, `messagesChannel`), que eran los
  que colisionaban. De paso, `showForwardDialog`/
  `showForwardSelectionDialog` ahora loguean el error con `AppLogger.e`
  en vez de tragarlo en silencio, para que un fallo futuro sea
  diagnosticable desde logcat sin tener que añadir logging temporal.
  `forward_message_dialog_navigation.yaml` ahora cubre las dos ramas de
  `ForwardConversationDialog` con las dos cuentas QA: `@claudeqa2` (una
  sola conversación total, con `@claudeqa`) ejercita la rama vacía
  ("No hay otras conversaciones"), y `@claudeqa` (conversaciones con
  `@claudeqa2` y con la cuenta real `@ajrpachon`, preexistente) ejercita
  la rama poblada. La rama poblada solo comprueba que el diálogo
  renderiza con un destino seleccionable y es cancelable — nunca llega a
  tocar esa fila, porque el único destino disponible es `@ajrpachon`, y
  la regla de higiene de datos de este README es no escribir datos de
  prueba en esa cuenta real. Un reenvío real de extremo a extremo
  (confirmar que el mensaje llega al destino) no es probable de forma
  segura solo con las dos cuentas QA desechables, ya que solo se tienen
  la una a la otra como destino.
- **"Clear chat" no tiene diálogo de confirmación**: a diferencia de
  "Sign out on all devices" (que sí muestra un `AlertDialog` de
  confirmación — ver `sign_out_all_devices_cancel.yaml`), el
  `DropdownMenuItem` de `conversations_menu_clear_chat` en
  `ConversationListScreen.kt` llama a `onClearChat` directamente al
  tocarlo, sin ningún paso intermedio — un toque accidental borra el
  historial local al instante. Verificado que el impacto real es menor
  de lo que parece (ver `clear_chat_roundtrip.yaml` y "Higiene de
  datos" más abajo: `clearChat` solo toca la caché local, no el
  backend), pero la ausencia de confirmación en una acción con la
  palabra "Clear"/destructiva en el nombre sigue siendo inconsistente
  con el resto de la app y merece una segunda mirada fuera de esta
  suite.
- **`StatusIntent.DeleteStatus` no está conectado a ninguna UI**:
  `StatusViewModel.deleteStatus()` existe y funciona (llama a
  `statusRepository.deleteStatus`), pero `StatusScreen.kt` no lo
  dispara desde ningún sitio — no hay botón/menú para borrar el propio
  estado una vez publicado. Es la razón por la que
  `status_compose_navigation.yaml` no publica un estado real: no habría
  forma de limpiarlo después a través de la app.
- **El Photo Picker (`com.google.android.photopicker`) no indexa un
  archivo recién insertado hasta que se reinicia**: un `adb push` +
  `content insert` en MediaStore deja la imagen consultable por
  `content query` de inmediato, pero el propio Picker (que mantiene su
  índice propio, separado de MediaStore) seguía mostrando "No photos
  yet" hasta hacer `am force-stop` de `com.google.android.photopicker`
  **y** `com.google.android.providers.media.module` — verificado
  directamente probando ambos por separado. No es un bug de ChatApp (el
  Picker es un componente del sistema fuera de su control), documentado
  aquí porque `send_image_message.yaml` depende de este paso de setup.
- **`ACTION_OPEN_DOCUMENT` recuerda la última carpeta/raíz explorada
  entre lanzamientos, sobreviviendo a `launchApp: clearState`**: la
  primera vez que se abre "Archivo" en un emulador nuevo, el picker
  (`com.google.android.documentsui`) aterriza en su pestaña "Recent"
  (vacía para un archivo recién insertado — mismo tipo de lag de índice
  que el Photo Picker); a partir de ahí, recuerda haber navegado a
  "Downloads" y abre directamente ahí en cada ejecución siguiente —
  porque ese estado vive en las preferencias de DocumentsUI, un paquete
  aparte, no en `com.ajrpachon.chatapp`. `send_file_message.yaml` maneja
  ambos casos con `runFlow: when: notVisible` en vez de asumir uno fijo.
- **El picker de contactos del sistema renderiza en negro sólido en este
  AVD, pero el contenido y la interacción funcionan igual**: al abrir
  Attach → Contacto, la pantalla de `com.google.android.contacts`
  (`ContactPickerActivity`) se ve completamente negra — verificado con
  `uiautomator dump` que la fila del contacto ("MaestroContact") sí
  existe en el árbol de accesibilidad, con sus bounds reales, mientras
  la pantalla no pinta nada encima. Como Maestro conduce el árbol de
  accesibilidad y no los píxeles, `tapOn`/`send_contact_message.yaml`
  funcionan sin cambios pese a que un humano viendo la pantalla no vería
  nada que tocar. Es un glitch de rendering de esa build de la app de
  Contactos de Google en esta imagen de emulador concreta, no algo
  atribuible a ChatApp ni a Maestro.
- **La vista previa de "último mensaje" en la lista de chats no se
  actualiza al borrar ese mensaje**: tras enviar y borrar un mensaje de
  audio con `send_audio_message.yaml`, `ConversationListScreen` siguió
  mostrando "🎤 Audio (0:06)" como último mensaje de `@claudeqa2` en vez
  de reflejar el borrado — la fila de la propia conversación abierta sí
  se actualiza correctamente al placeholder "This message was deleted".
  Probablemente un campo `lastMessage` desnormalizado en la fila de
  conversación que no se refresca en el flujo de borrado. No investigado
  a fondo (no bloquea ningún flow: el contenido del chat en sí es
  correcto) — mencionado aquí por si aparece de nuevo al depurar otro
  flow y resulta desconcertante.
- **La tienda de stickers no tiene ningún pack disponible en esta
  instalación**: `Attach → Stickers → +` abre `StickerStoreSheet` y
  muestra "No hay packs disponibles" — investigado hasta
  `StickerPackRepositoryImpl`/`StickerPackDao`: los packs se leen de una
  tabla Room puramente local, sin ningún seed/prepopulate en
  `DatabaseBuilder.kt` ni en ningún otro sitio del código. Es decir, no
  hay ningún sticker instalable a través de la UI en una instalación
  nueva, con ninguna cuenta. Por eso `sticker_picker_navigation.yaml`
  sigue siendo solo de navegación (abre el picker y cancela) — no se
  extendió a un envío real porque no hay ningún sticker que seleccionar,
  no por una limitación de Maestro.

## Higiene de datos

`send_message.yaml` y `message_reaction_roundtrip.yaml` borran el mensaje
que envían nada más verificarlo (vía `subflows/delete_own_message.yaml`),
así que se pueden correr en bucle sin ir acumulando mensajes de prueba
**con contenido legible** en la conversación de QA. El borrado en la app es un soft-delete — deja un
placeholder ("This message was deleted"/"Se eliminó este mensaje") en
vez de desaparecer del todo — así que la conversación sí acumula esos
placeholders con cada ejecución; es ruido inofensivo, no un dato
sensible ni de prueba con contenido.

Sigue el mismo principio en cualquier flow nuevo que escriba datos: si el
flow los crea, el propio flow los borra al final. Los siete flows de
adjuntos (`send_image_message.yaml` y el resto listado en "Setup previo
para los flows de adjuntos") siguen el mismo patrón: cada uno borra su
mensaje justo después de verificarlo, vía
`subflows/delete_selected_media_message.yaml` (o
`delete_selected_message.yaml` para la encuesta) en vez de
`delete_own_message.yaml` — ver la sección de selectores más arriba para
por qué. Los propios *archivos/contacto semilla* en el dispositivo
(`maestro_seed.jpg`, `maestro_seed.txt`, el contacto "MaestroContact")
no se borran entre ejecuciones a propósito: son datos de setup del
dispositivo de prueba, reutilizados en cada run, no datos de prueba
generados dentro de la conversación — la distinción que importa para
esta política es qué termina visible en la conversación de QA, no qué
existe en el disco del emulador.

`clear_chat_roundtrip.yaml` usa la misma cuenta y conversación compartidas,
pero su "borrado" (`ConversationRepositoryImpl.clearChat` →
`messageDao.deleteByConversation`) es local-only: no llama al backend, así
que no afecta a `@claudeqa2` ni a la fila de Supabase. `ChatViewModel.init`
vuelve a llamar a `messageRepository.syncRemote(...)` cada vez que se abre
la conversación, así que reabrirla re-descarga y re-inserta todo el
historial remoto en la caché local recién vaciada — el flow explota
justamente esto para verificar el comportamiento (envía un mensaje único,
vacía el chat, reabre, confirma que el mensaje reaparece) y borra ese
mensaje de prueba al final igual que `send_message.yaml`.

## Pendiente / ideas

- Flow de registro (`sign up`) con limpieza posterior de la cuenta creada
  (no cubierto: requiere verificar el email de forma asíncrona).
- Flow de invitación/alta de contacto nuevo de verdad (enviar invitación
  desde "New chat" a un usuario no conectado). No cubierto todavía:
  toca el área donde hay un bug de deserialización conocido al conectar
  cuentas — ver memoria del proyecto — y necesitaría una tercera cuenta
  QA desechable (vía Supabase admin, no vía sign-up en la app) para no
  ensuciar la relación `@claudeqa` ↔ `@claudeqa2` — requiere confirmación
  antes de crearla, ya que toca el proyecto Supabase directamente.
- Bloquear/desbloquear un usuario (`NewChatScreen`'s `BlockUser`/
  `UnblockUser`): solo tiene botón de bloqueo para contactos con
  relación `NONE`, y las únicas cuentas disponibles (`@claudeqa`,
  `@claudeqa2`) ya están conectadas entre sí — necesitaría la misma
  cuenta QA #3 desechable que la invitación de contacto.
- Wallpaper picker: probablemente requiere un Intent de galería externo
  (fuera del sandbox de Compose/Maestro) para elegir una imagen — no
  investigado a fondo.
- **Compartir ubicación** (adjuntar → "Ubicación"): ~~no cubierto~~ →
  cubierto por `send_location_message.yaml`. La razón original para
  descartarlo (una vez concedido `ACCESS_FINE_LOCATION`,
  `FetchAndSendLocation` envía la ubicación real sin paso intermedio, y
  el flow no puede revocar el permiso entre ejecuciones) seguía siendo
  válida — lo que cambió es reconocer que en el emulador la "ubicación
  real del dispositivo" es en sí un valor fijo y determinista via
  `adb emu geo fix`, no algo incontrolable. El flow asume permiso y geo
  fix ya establecidos (ver "Setup previo para los flows de adjuntos"
  arriba) en vez de gestionarlos él mismo.
- **Compartir contacto** (adjuntar → "Contacto"): ~~no cubierto~~ →
  cubierto por `send_contact_message.yaml`, sembrando un contacto real
  del dispositivo primero (`am start -a android.intent.action.INSERT`).
  Resultó automatizable pese a ser un Intent real a la app de Contactos
  del sistema — el precedente de "fuera del sandbox de Compose/Maestro"
  no era, por sí solo, motivo suficiente para descartarlo (a diferencia
  del wallpaper picker de abajo, que sigue sin investigarse a fondo). El
  único hallazgo real en el camino fue un glitch de rendering (ver
  "Hallazgos" arriba) que no impidió automatizarlo.
- **Activar verificación en dos pasos** (Profile → "Activate"):
  investigado a fondo, no cubierto — tratado con la cautela extra que
  pedía el encargo. Tocar "Activate" llama a
  `ProfileViewModel.enroll2FA()`, que ejecuta
  `authRepository.enrollTotp()`: una llamada real a Supabase Auth que
  crea un factor TOTP pendiente en la cuenta. Verificado en
  `ProfileScreen.kt` que cerrar la hoja de enrolamiento
  (`Dismiss2FASheet`) solo oculta la UI — no hay ninguna acción que
  desenrole ese factor sin verificar, porque el botón "Deactivate" solo
  aparece cuando `state.twoFactor.isEnrolled == true`, y eso solo pasa
  tras verificar un código TOTP real. Es decir: abrir la hoja dejaría un
  factor huérfano sin verificar en la cuenta QA en cada ejecución, sin
  ninguna vía de vuelta atrás a través de la propia app — exactamente el
  riesgo que el encargo pedía descartar si no se podía verificar una
  salida limpia. Se necesitaría limpiar el factor vía Supabase admin
  (fuera del alcance de un flow de Maestro) para que esto fuera seguro
  de automatizar.
- **Backup a Google Drive** (Profile): no investigado a fondo más allá
  de confirmar que `onBackup` existe — descartado directamente por
  requerir casi con certeza un inicio de sesión real de Google, como ya
  anticipaba el encargo.
- **Crear grupo de verdad + Group info + salir del grupo**: investigado
  a fondo, se decide NO cubrirlo con datos reales (más allá de la
  navegación del wizard que ya cubre `create_group_navigation.yaml`).
  `GroupRepositoryImpl.leaveGroup` solo hace
  `remoteSource.removeMember` + borra la fila local del miembro — no
  borra ni marca la conversación en sí. Aunque ambas cuentas QA
  (`@claudeqa` y `@claudeqa2`) salieran del grupo al terminar el test,
  la fila de esa conversación de grupo (y sus mensajes) seguiría
  existiendo para siempre en Supabase, inalcanzable desde la UI de
  cualquiera de las dos cuentas pero presente como suciedad permanente
  en el backend compartido — el mismo tipo de huella que la razón
  original para no crear el grupo en `create_group_navigation.yaml`,
  solo que sin siquiera la mitigación de "sigue siendo visible para
  @claudeqa2".
- **Borrar una encuesta de verdad**: `poll_create_send_roundtrip.yaml`
  borra el *mensaje* `poll:<id>` (soft-delete, igual que cualquier otro
  tipo de mensaje), pero no existe ninguna acción de UI que borre la
  fila de la propia encuesta/sus opciones/votos en Supabase —
  `ChatPollDelegate` solo tiene `createPoll`/`votePoll`, sin
  `deletePoll`. Mismo tipo de huella permanente de bajo impacto que
  "Crear grupo de verdad" más abajo, aceptado por la misma razón: es
  metadata sin contenido legible sensible, no un mensaje de texto de
  prueba.
- **Stickers reales**: no cubierto — ver "Hallazgos" arriba
  (`No hay packs disponibles` en la tienda; no es una limitación de
  Maestro sino que no hay ningún pack sembrado en la tabla Room local en
  ninguna parte del código). Si en el futuro se añade un seed de packs
  por defecto, `sticker_picker_navigation.yaml` sería el punto de
  partida para extenderlo a un envío real, siguiendo el mismo patrón que
  `poll_create_send_roundtrip.yaml`.
- **`send_video_message.yaml`/`send_camera_photo_message.yaml`
  dependen de coordenadas de pantalla fijas** para el obturador/confirmar
  de la app de Cámara del sistema (sin texto/`content-description`
  seleccionable en su árbol de accesibilidad en este AVD) — es el flow
  más frágil ante un cambio de imagen de emulador/API level de toda esta
  suite. Si se rompe, la solución es volver a capturar la pantalla de la
  Cámara con `adb exec-out screencap` y remedir los puntos, no reescribir
  el flow.
- Integrar como job opcional de CI (requiere un emulador headless en el
  runner; de momento se ejecuta solo en local).
