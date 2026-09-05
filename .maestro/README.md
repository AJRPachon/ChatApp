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
    pdf_viewer_navigation.yaml
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
    app_lock_toggle_roundtrip.yaml
    backup_screen_navigation.yaml
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
    group_text_message_roundtrip.yaml
    group_sender_attribution_navigation.yaml
    group_info_member_management_navigation.yaml
    group_image_message_roundtrip.yaml
    group_file_message_roundtrip.yaml
    group_camera_photo_roundtrip.yaml
    group_video_capture_roundtrip.yaml
    group_audio_message_roundtrip.yaml
    group_location_message_roundtrip.yaml
    group_contact_message_roundtrip.yaml
    group_poll_message_roundtrip.yaml
    setup/                  # setup ÚNICO, fuera de la suite normal — ver más abajo
      create_maestro_group.yaml
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
    delete_selected_message.yaml  # parametrizado: env MESSAGE_SELECTOR
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
    delete_selected_message.yaml         # parametrizado: env MESSAGE_SELECTOR
    delete_selected_media_message.yaml   # parametrizado: env POINT
    create_group.yaml              # parametrizado: env GROUP_NAME, MEMBER_USERNAME
```

Siempre se ejecuta apuntando a `flows/` (archivo o carpeta), nunca a `.maestro`
a secas — así nunca se corre un subflow suelto (le faltaría contexto: espera
partir de una pantalla concreta, no del arranque de la app).

**`maestro test .maestro/flows` (modo carpeta) solo escanea archivos `.yaml`
de primer nivel dentro de `flows/`, nunca desciende a subcarpetas** —
verificado directamente con una carpeta de prueba mínima: un archivo
sin listar en `flowsOrder` pero suelto directamente en `flows/` sí se
ejecutó igualmente (`flowsOrder` solo determina el ORDEN de los que sí
lista, no filtra los que no), mientras que uno idéntico dentro de una
subcarpeta nunca llegó a correr. Por eso tanto `flows/realtime/` como
`flows/setup/` (ver debajo) viven en su propia subcarpeta en vez de como
archivos sueltos con un prefijo `_` — un prefijo no basta para excluirlos de
una pasada de la suite completa.

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
`CreateGroupScreen`, `SessionAuditScreen`...); `chat_mic_button`
(`ChatInputBar`'s `NormalInputBar` — el icono de micrófono, antes sin
`testTag`), `chat_recording_stop_button` (`RecordingBar`'s botón de stop),
`chat_audio_discard_button` / `chat_audio_send_button` (`AudioPreviewBar`'s
descartar/enviar — los cuatro añadidos para `group_audio_message_roundtrip.yaml`,
ver "Grupo real de prueba" más abajo); `profile_app_lock_switch`,
`profile_backup_row` (`ProfileScreen`), `applock_screen` (`AppLockScreen`,
root `Column`), `backup_make_backup_button` (`BackupScreen`) — los cuatro
añadidos para `app_lock_toggle_roundtrip.yaml`/`backup_screen_navigation.yaml`,
ninguno tenía `testTag` antes.

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

## Grupo real de prueba y sus flows de adjuntos

Decisión explícita del usuario, que sustituye la cautela anterior de esta
misma suite (ver el antiguo punto "Crear grupo de verdad..." en "Pendiente"
más abajo, ahora resuelto): existe un grupo real y permanente llamado
**"Maestro Test Group"**, creado una sola vez con `@claudeqa` como creador y
`@claudeqa2` como único otro miembro. Aparece para siempre en la lista de
chats de ambas cuentas — `GroupRepositoryImpl.leaveGroup` no borra la fila
de la conversación en Supabase aunque ambas cuentas salgan del grupo, así
que salir no sería una limpieza real, solo perder acceso — y eso es
aceptado a propósito. Todos los flows `flows/group_*.yaml` asumen que este
grupo ya existe y lo abren por nombre (`open_conversation.yaml` con
`CONTACT_NAME: "Maestro Test Group"`), igual que cualquier chat 1:1.

**Creación (ya hecha, no repetir)**: `flows/setup/create_maestro_group.yaml`
documenta los pasos exactos y usa el nuevo `subflows/create_group.yaml`
(parametrizado por `GROUP_NAME`/`MEMBER_USERNAME`, extraído del wizard que
`create_group_navigation.yaml` ya recorría sin nunca tocar "Create"). Crear
un grupo no es idempotente como un send+delete — volver a correrlo crearía
OTRO grupo cada vez — así que este archivo vive en su propia subcarpeta
`flows/setup/`, no como un archivo suelto en `flows/`: ver la nota en
"Estructura" más arriba sobre por qué un `.yaml` de primer nivel sin listar
en `flowsOrder` **igualmente se ejecutaría** en una pasada de la suite
completa, y solo una subcarpeta (como ya hacía `flows/realtime/`) lo evita
de verdad. Queda en el repo solo como referencia/documentación de cómo se
hizo.

### Sembrar medios para el picker

`group_image_message_roundtrip.yaml` y `group_file_message_roundtrip.yaml`
requieren un archivo ya sembrado en el dispositivo — Maestro no puede
invocar `adb` desde dentro de un flow. Comandos reales verificados
(**en Git Bash/MSYS, no `cmd.exe`**: usar doble barra al inicio de la ruta
del dispositivo, `//sdcard/...`, o MSYS reescribe silenciosamente
`/sdcard/...` como una ruta de Windows tipo `C:/Program Files/Git/sdcard/...`
y el push falla o aterriza en el sitio equivocado — verificado directamente,
ver historial de comandos):

```bash
# Imagen — Galería usa el Photo Picker moderno (ACTION_PICK_IMAGES), que
# solo ve archivos indexados por MediaStore, no cualquier archivo en /sdcard.
adb -s emulator-5554 push maestro_test_image.jpg //sdcard/Pictures/maestro_test_image.jpg
adb -s emulator-5554 shell content insert --uri content://media/external/images/media \
  --bind _data:s://sdcard/Pictures/maestro_test_image.jpg --bind mime_type:s:image/jpeg
adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d file://sdcard/Pictures/maestro_test_image.jpg

# Archivo — "Archivo" usa ACTION_OPEN_DOCUMENT (el selector de archivos del
# sistema), que sí ve archivos recientes sin necesitar indexado de MediaStore.
adb -s emulator-5554 push maestro_test_file.txt //sdcard/Download/maestro_test_file.txt
```

En el picker de fotos, el archivo sembrado aparece con `content-desc="Photo
taken on <fecha>"` (dinámico) — seleccionable con `tapOn: "Photo taken.*"`
(regex, ya que el selector de texto exige coincidencia completa del nodo).
El botón para confirmar la selección se llama **"Done"**, no "Add" — verificado
directamente contra el picker real; la documentación/suposición inicial de
"Add" no correspondía a la build actual.

### "Video" en el menú de adjuntar es captura de cámara real, no una galería

A diferencia de lo que su posición junto a "Galería" sugiere, el botón
"Video" del `AttachmentBottomSheet` dispara
`ActivityResultContracts.CaptureVideo()` (ver `ChatScreen.kt`) — abre la
app de cámara del sistema en modo vídeo y graba de verdad, igual que
"Cámara" para fotos. No consulta la galería/MediaStore en ningún momento,
así que sembrar un MP4 ahí (como si fuera para un picker) no serviría de
nada. `group_video_capture_roundtrip.yaml` conduce la app de cámara AOSP
del emulador (mismo flujo que `group_camera_photo_roundtrip.yaml`, solo que
en modo vídeo): `tapOn: "Shutter"` inicia la grabación, `tapOn: "Shutter"`
otra vez la detiene, luego la pantalla de revisión (`Play`/`Done`/`Cancel`,
un fotograma fijo con controles) — `tapOn: "Done"` confirma. Ambos flows de
captura real funcionan de punta a punta contra la cámara virtual del
emulador (un patrón de prueba estático, una casa con ventanas).

### Grabar y enviar un audio es tap-to-start/tap-to-stop, no mantener pulsado

`ChatAudioRecordingDelegate.kt` (`StartRecording`/`StopRecording`) confirma
que no hay gesto de mantener-pulsado ni de deslizar-para-cancelar en este
código — un toque en el micrófono empieza a grabar (`RecordingBar` con
botón de stop), otro toque lo detiene y pasa a una vista previa
(`AudioPreviewBar` con descartar/enviar). Mucho más simple de automatizar
de lo que el encargo original anticipaba. `group_audio_message_roundtrip.yaml`
graba ~2 segundos reales (con micrófono del emulador, sin necesitar audio
de verdad — un archivo `.m4a` silencioso/de ruido de fondo es válido) y
lo envía. Para borrarlo después, ver la sección de multi-select más abajo —
el reproductor de audio (`RemoteAudioPlayer`/`AudioPlayerRow`) no tiene
ningún texto estable que seleccionar salvo la propia etiqueta de duración
(p. ej. `"0:03"`), que nunca coincide con una hora de reloj en pantalla
porque esta usa `"%d:%02d"` (minutos sin cero a la izquierda: `"0:XX"`,
nunca `"00:XX"`) mientras que un reloj siempre usa `"%02d:%02d"`.

### Borrado universal por selección múltiple, y tres bugs de long-press reales encontrados y arreglados

`delete_own_message.yaml` (long-press sobre el texto → "Eliminar" del menú
contextual) solo sirve para mensajes con `content` no vacío — ese menú
contextual vive dentro de un `if (message.content.isNotBlank())` en
`ChatBubbleContent.kt`. Un mensaje de imagen/audio/vídeo/archivo puro (sin
texto) nunca entra en esa rama, así que no tiene ese menú. Todo tipo de
mensaje comparte en cambio un mecanismo distinto: `MessageBubble.kt` envuelve
cada burbuja en un contenedor con `onLongClick = { onToggleSelect() }` —
mantener pulsado activa el modo de selección múltiple, y la barra superior
muestra entonces un botón de papelera ("Eliminar seleccionados") que abre un
`AlertDialog` de confirmación. `subflows/delete_selected_message.yaml`
formaliza esto (parametrizado por `MESSAGE_SELECTOR`) y es lo que usan todos
los flows `group_*` para tipos sin texto — image, file, video, audio,
contact, poll, location y las capturas de cámara.

Construir esto encontró (y arregló, en `MessageBubble.kt`/
`ChatFileBubbles.kt`/`ChatBubbleContent.kt`) tres casos reales donde
mantener pulsado no llegaba nunca a ese `onLongClick` del contenedor
exterior: la imagen (`AsyncImage`), el archivo genérico
(`GenericFileBubble`) y el vídeo (`VideoBubble`) tenían cada uno su propio
`Modifier.clickable { ... }` (abrir el visor, abrir el archivo, reproducir
el vídeo) — y un `clickable` normal en Compose dispara `onClick` en el
`ACTION_UP` sin importar cuánto duró la pulsación, así que un long-press
sobre esa zona hacía justo lo mismo que un tap corto (abría el visor/
archivo/vídeo) y JAMÁS llegaba a activar la selección — verificado
directamente probando `longPressOn` sobre la imagen enviada: en vez de
"1 selected", abría el visor de imágenes de pantalla completa. Arreglado
cambiando esos tres `clickable` a `combinedClickable(onClick = ..., onLongClick
= onToggleSelect)`, con `onToggleSelect`/`onLongPress` ahora enhebrado como
parámetro hasta cada composable. Se aplicó el mismo arreglo a
`LocationMessageCard`, que además tenía un problema estructural adicional:
su propio `clickable` (abrir Maps) vivía como **hermano**, no hijo, del
`Box` que contiene el `DropdownMenu` de borrado en `MessageFooterContent` —
ni siquiera estando dentro de ese `Box`, así que un mensaje de ubicación no
tenía NINGÚN camino de borrado alcanzable antes de este fix, pese a que su
`content` (`"📍 Mi ubicación: <url>"`) no está vacío. `StickerBubble` y
`ContactBubble` no necesitaron el mismo arreglo porque nunca tuvieron su
propio `clickable` — el long-press siempre cayó limpiamente al contenedor
exterior.

### Sembrar un contacto de prueba

`group_contact_message_roundtrip.yaml` necesita un contacto llamado
"MaestroContact" ya existente en el dispositivo antes de correr. El camino
obvio — `adb shell am start -a android.intent.action.INSERT ... -e name
"Maestro Contact"` seguido de tocar "Save" en la UI del sistema — resultó
poco fiable: el botón "Save" de "Add to contacts" apareció visualmente
deshabilitado (sin ninguna cuenta de dispositivo configurada en este
emulador) y el contacto nunca llegó a guardarse pese a que `tapOn`
reportaba éxito. Además, un nombre con espacio en el propio comando `adb
shell am start -e name "Maestro Contact"` se corrompe por cómo Android
tokeniza el comando shell recibido — usar un nombre sin espacios
(`MaestroContact`) evita ese problema aparte. La alternativa que sí
funciona de forma reproducible: insertar directamente vía el
`ContentProvider` de contactos, sin pasar por ninguna UI:

```bash
adb -s emulator-5554 shell content insert --uri content://com.android.contacts/raw_contacts \
  --bind account_name:n: --bind account_type:n:
# usar el _id devuelto (consultar con `content query --uri content://com.android.contacts/raw_contacts --projection _id`)
adb -s emulator-5554 shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:<id> --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:MaestroContact
adb -s emulator-5554 shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:<id> --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
  --bind data1:s:5550100 --bind data2:i:2
```

Verificado con `content query --uri content://com.android.contacts/contacts
--projection display_name`. El Activity del picker de contactos
(`com.google.android.contacts`) además renderiza como una captura de
pantalla completamente negra vía `adb shell screencap` en este emulador —
confirmado con `uiautomator dump` que el contenido real (la lista de
contactos) está presente e interactuable debajo; es una rareza de
screencap/hardware-layer de ese Activity concreto, no un problema real de
la app ni de Maestro para `tapOn`/`assertVisible` por texto.

### Ubicación: ahora sí se cubre, con permiso y GPS falso ya preparados de antemano

Corrige la decisión anterior documentada en "Pendiente" (más abajo, ahora
resuelta): con `ACCESS_FINE_LOCATION` ya concedido de antemano
(`adb shell pm grant`) y una posición falsa ya fijada
(`adb emu geo fix -3.7038 40.4168`, Madrid) antes de correr la suite, enviar
una ubicación real dentro de `group_location_message_roundtrip.yaml` es
seguro y repetible — el permiso, una vez concedido, ya no puede
revocarse desde dentro de un flow, así que ya no hay nada que "arriesgar":
cada ejecución futura enviará una ubicación real de todas formas,
igual que antes de este flow existir. Se borra en el mismo run vía
selección múltiple.

### Sticker: paquetes no disponibles, no cubierto por falta de datos (no de código)

`sticker_picker_navigation.yaml` sigue siendo solo de navegación. Investigado
a fondo si se podía extender a un envío real: la pestaña "Stickers" del
picker (`StickerTab`, paquetes de imágenes reales vía `StickerPackViewModel`)
muestra "Instala packs desde la tienda →" porque no hay ningún paquete
instalado; abrir la tienda in-app (`StickerStoreSheet`, alcanzable con
`tapOn: "\\+"` en la pestaña) confirma que tampoco hay ninguno **disponible**
para instalar — "No hay packs disponibles". A diferencia de GIFs (que
necesitan una clave de API de Giphy y red externa, fuera de alcance por
diseño), esto es simplemente un catálogo vacío en el backend de Supabase de
este proyecto — no hay ninguna fila en la tabla de paquetes de stickers
disponibles. Sembrar una no es como sembrar un archivo en `/sdcard/`: es
contenido de catálogo compartido, no datos de la cuenta QA, así que está
fuera del alcance de "mutar libremente las cuentas QA" y no se hizo sin
confirmación explícita. Documentado aquí en vez de forzarlo.

De paso: `StickerBubble` renderiza `message.stickerUrl` como texto (fontSize
64sp) tras pasarlo por `StickerValidation.sanitize()`, que descarta
cualquier valor de más de 10 caracteres — pensado para una secuencia de
emoji corta (el flujo `SendSticker(emoji: String)` original), pero el
selector de paquetes de imágenes (`onStickerSelected(url)`) envía una URL
real de bastante más de 10 caracteres por ese mismo intent. Si alguna vez
se siembra un paquete de verdad, un sticker de imagen elegido ahí
probablemente se renderizaría como una burbuja casi vacía (solo la hora),
no la imagen — un bug de UI real pero no verificable sin datos de catálogo
para reproducirlo; queda anotado para cuando se investigue el catálogo de
stickers.

## Hallazgos de UX reales encontrados (y su estado)

- **`inputText: " "` + `Backspace` NO evita de forma fiable que el teclado
  predictivo sustituya el texto tecleado (arreglado en el flow, no en la
  app)**: `profile_display_name_roundtrip.yaml` documentaba desde antes un
  workaround para el autocompletado de Gboard (escribir "claudeqa" tras
  sesiones repitiendo "claudeqa2" hacía que un `pressKey: Enter` a secas
  aceptara la sugerencia en vez del texto literal) — pero el propio
  workaround (tecleaba un espacio, luego lo borraba con Backspace, y
  entonces sí pulsaba Enter) resultó **no funcionar**: verificado paso a
  paso con capturas de pantalla que el simple hecho de teclear el espacio
  ya sustituía el contenido del campo por la sugerencia resaltada
  ("claudeqa2"), antes incluso de llegar al Backspace/Enter. La sustitución
  ocurre al escribir el espacio, no al confirmar. El arreglo real: usar
  `eraseText` para vaciar el campo y un único `inputText` seguido
  directamente de `pressKey: Enter` — sin espacio de por medio — que sí
  respeta el texto tecleado literalmente en todas las repeticiones
  probadas. Esto tuvo una consecuencia real y no trivial: el propio bug del
  workaround había dejado el `display_name` de la cuenta `@claudeqa` en el
  backend permanentemente corrompido a `"claudeqa2"` (en vez de
  `"claudeqa"`) tras una ejecución anterior de la versión antigua del
  flow — ver el siguiente punto.
- **Atribución de remitente en grupo, aparentemente incorrecta (en realidad,
  dato de cuenta QA corrompido, ya arreglado)**:
  `group_sender_attribution_navigation.yaml` (verificar que
  `MessageBubble`'s cabecera de remitente muestra el nombre correcto al ver
  un mensaje ajeno en un grupo) inicialmente parecía encontrar un bug de
  atribución real: un mensaje enviado desde `@claudeqa` se mostraba, visto
  desde `@claudeqa2`, con la cabecera **"claudeqa2"** — el propio nombre del
  espectador, no el del remitente real. Investigado a fondo con
  `MessageRepositoryImpl.observeMessages` (`senderMap[dbo.senderId]
  ?.displayName`, correcto — usa el `senderId` real de cada fila, no el
  usuario actual) antes de descartarlo como bug de la app: la Pantalla de
  Perfil de `@claudeqa`, abierta directamente, confirmó que su propio
  `display_name` en el backend literalmente decía `"claudeqa2"` — dato de
  cuenta corrompido, no un bug de renderizado. Causa raíz encontrada:
  el bug del punto anterior (`inputText: " "` + `Backspace` no siempre
  evita la sustitución predictiva) había hecho que una ejecución anterior
  de `profile_display_name_roundtrip.yaml` (posiblemente del otro agente
  compartiendo `@claudeqa` en `emulator-5556`, o de una ejecución previa
  propia) "restaurara" el nombre a "claudeqa2" en vez de "claudeqa" al
  final de su propio roundtrip. Arreglado corrigiendo el dato directamente
  (vía la propia UI de Perfil, con el `pressKey: Enter` sin espacio) en vez
  de dejarlo como hallazgo sin resolver, ya que es dato de una cuenta QA
  explícitamente mutable, no un bug de código — y arreglando también el
  propio `profile_display_name_roundtrip.yaml` para que no vuelva a
  corromperlo. Recordatorio para el futuro: con dos agentes compartiendo
  las mismas dos cuentas QA en dos emuladores a la vez, una ejecución
  concurrente de cualquier flow que mute un perfil compartido (nombre,
  username, avatar...) puede pisar el trabajo del otro — no hay forma de
  evitar esto por diseño mientras ambos agentes usen las mismas cuentas.
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

- **`AppLockScreen` es evitable con el botón/gesto "atrás" del sistema (no
  arreglado, solo reportado)**: construyendo `app_lock_toggle_roundtrip.yaml`
  se confirmó que `NavDisplay`'s `onBack` en `MainActivity.kt`
  (`if (backStack.size > 1) backStack.removeLastOrNull() else finish()`)
  no tiene ninguna excepción para `AppLockRoute` — un `back` normal la
  saca de la pila igual que cualquier otra pantalla, revelando lo que
  hubiera debajo (aquí, `ProfileScreen`) sin pasar nunca por
  `AppLockIntent.AuthSucceeded`/`AppLockEffect.Authenticated`. Es decir,
  el bloqueo de app actual se puede saltar por completo con el botón
  atrás, sin biometría ni PIN. El propio flow se apoya en este hueco a
  propósito para volver a `ProfileScreen` y desactivar el toggle al
  terminar (no hay ninguna otra vía de UI para salir de `AppLockScreen`
  sin pasar una biometría/PIN real) — si esto se arregla alguna vez
  (añadiendo una excepción para `AppLockRoute` en `onBack`), el paso de
  limpieza de ese flow necesitará una vía distinta.
- **`BackupScreen` puede disparar el OAuth de Google solo con abrirla, no
  solo al tocar "Hacer copia"**: `BackupViewModel.init` llama siempre a
  `loadLastBackupInfo()`, que llama a `backupRepository.getLatestBackupInfo()`
  para rellenar la tarjeta "Última copia" — y esa función usa el mismo
  `AccountManager.blockingGetAuthToken(account, DRIVE_SCOPE, true)` que
  `backup()`/`restore()`. Está envuelta en `runCatching { }.getOrNull()`,
  así que en un dispositivo/emulador sin ninguna cuenta de Google añadida
  (`accounts.firstOrNull() ?: error(...)`) falla en silencio y la pantalla
  simplemente muestra su estado vacío ("No backups in Google Drive") — sin
  diálogo ni crash, verificado leyendo `BackupRepositoryImpl.kt`. Pero en
  un dispositivo que sí tenga una cuenta de Google configurada, entrar a
  esta pantalla podría bastar para disparar un diálogo real de
  consentimiento de cuenta, antes de tocar ningún botón.
  `backup_screen_navigation.yaml` asume el escenario sin cuenta de Google
  (el que describe el propio encargo para la cuenta QA) y por eso se
  queda en navegación pura.

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

Los flows `group_*.yaml` siguen el mismo principio para tipos con texto
(text, poll — vía `delete_own_message.yaml`/selección múltiple) y lo
extienden a los que no tienen texto (image, file, video, audio, contact,
location, capturas de cámara) vía `delete_selected_message.yaml` — ver
"Grupo real de prueba" más arriba. La única excepción real y documentada es
el propio grupo "Maestro Test Group": la conversación en sí es permanente
por diseño (decisión explícita del usuario), no algo que cada flow cree y
borre.

## `BroadcastListScreen` y `UsageStatsScreen`: implementadas, pero sin ningún punto de entrada real en la app

Ambas pantallas están completamente implementadas (`ui/broadcast/BroadcastListScreen.kt`,
`ui/usagestats/UsageStatsScreen.kt`), tienen su propia ruta `@Serializable`
(`BroadcastListRoute`, `UsageStatsRoute`) y su propio `NavEntry` registrado en
`NavRoutes.kt` (`miscNavEntry`/`profileNavEntry` respectivamente) — es decir,
Navigation 3 sabe perfectamente cómo renderizarlas si algún `NavKey` de ese
tipo llega a aparecer en el backstack. El problema es que nada en la app
llega jamás a añadir ese `NavKey`: verificado exhaustivamente con
`grep -rn "onUsageStats\|onBroadcastList\|UsageStatsRoute\|BroadcastListRoute"`
sobre todo `app/src/main/java` que la única aparición de ambas rutas fuera de
sus propias pantallas y de `NavRoutes.kt` es su definición y su `NavEntry` —
ningún composable en toda la app llama nunca a `backStack.add(BroadcastListRoute)`
ni a `backStack.add(UsageStatsRoute)`. Se leyó `ProfileScreen.kt` completo (la
pantalla candidata más obvia, dado que `UsageStatsRoute` vive en
`profileNavEntry` junto a `SessionAuditRoute`/`BackupRoute`, que sí tienen
fila propia) y no existe ninguna fila/botón para ninguna de las dos — su
firma (`onBack`, `onSignOut`, `onBackup`, `onSessionAudit`) ni siquiera tiene
un parámetro `onUsageStats`/`onBroadcastList` que se pudiera invocar.
`ConversationListScreen.kt` (candidato para "Listas de difusión", ya que
conceptualmente encaja junto a "Nuevo grupo"/"Nuevo chat") tampoco tiene
ningún FAB/ítem de menú para ello. Tampoco hay deep link (`MainActivity.kt`
solo reconoce `chatapp://chat/...` y el callback de auth de Supabase).

Esto bloquea escribir `broadcast_list_navigation.yaml` y
`usage_stats_navigation.yaml` tal como se pidieron (abrir la pantalla desde
un punto de entrada real): no existe ninguno que abrir, y Maestro solo puede
conducir la UI real de la app — no puede inyectar un `NavKey` arbitrario en
el backstack de Navigation 3 saltándose la UI. Siguiendo el mismo principio
que el resto de esta sección (documentar en vez de forzar: ver 2FA, tienda
de stickers, wallpaper picker), no se creó ningún flow simulado para estas
dos pantallas — habría sido un flow que nunca podría pasar de verdad, o que
solo probaría un botón que no existe en el código de producción.
Es un bug real (dos pantallas terminadas y registradas en el grafo de
navegación pero completamente inalcanzables por cualquier usuario real),
no una limitación de esta suite — repórtese y arréglese añadiendo el punto
de entrada que falta (lo más probable: una fila en `ProfileScreen.kt` para
"Estadísticas de uso" junto a "Sesiones activas", y un punto de entrada para
"Listas de difusión" en `ConversationListScreen.kt` o en el propio
`ProfileScreen.kt`) antes de que estos dos flows se puedan escribir de
verdad.

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
- **Sticker (paquetes de imagen reales)**: no cubierto por falta de datos
  de catálogo (ni instalados ni disponibles en la tienda in-app de este
  backend de Supabase), no por limitación técnica de Maestro — ver "Grupo
  real de prueba" más arriba para el detalle completo, incluyendo un bug de
  UI real (`StickerValidation.sanitize`) encontrado de paso pero no
  verificable sin esos datos.
- **GIFs** (pestaña GIFs del picker de stickers): necesita una clave de API
  de Giphy configurada y red externa — fuera de alcance por diseño, sin
  investigar más a fondo.
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
- ~~Backup a Google Drive~~ (Profile): **parcialmente cubierto** —
  `backup_screen_navigation.yaml` cubre solo la navegación (entrar,
  confirmar que renderiza, volver), sin tocar "Hacer copia"/"Restaurar" ni
  completar ningún OAuth real — sigue siendo cierto que un backup/restore
  de extremo a extremo necesitaría una cuenta de Google real añadida al
  emulador, como ya anticipaba el encargo original. Ver "Hallazgos" más
  abajo por un matiz real encontrado: incluso solo *abrir* la pantalla
  puede disparar el mismo `blockingGetAuthToken` que el propio botón.
- ~~Crear grupo de verdad + Group info + salir del grupo~~: **resuelto** —
  decisión explícita del usuario de crear un grupo real y permanente pese
  a la suciedad de backend que eso implica. Ver "Grupo real de prueba y sus
  flows de adjuntos" más arriba. `Group info`/salir del grupo en sí siguen
  sin flow propio (fuera del alcance de este encargo, centrado en tipos de
  mensaje/adjuntos), pero ya no por la razón original.
- **Borrar una encuesta de verdad**: `poll_create_send_roundtrip.yaml`
  borra el *mensaje* `poll:<id>` (soft-delete, igual que cualquier otro
  tipo de mensaje), pero no existe ninguna acción de UI que borre la
  fila de la propia encuesta/sus opciones/votos en Supabase —
  `ChatPollDelegate` solo tiene `createPoll`/`votePoll`, sin
  `deletePoll`. Mismo tipo de huella permanente de bajo impacto que
  "Crear grupo de verdad" de arriba, aceptado por la misma razón: es
  metadata sin contenido legible sensible, no un mensaje de texto de
  prueba.
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
