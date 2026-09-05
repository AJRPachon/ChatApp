# Política de Privacidad de ChatApp

**Última actualización: [fecha de publicación]**

Esta política describe qué datos recoge ChatApp ("la App", "nosotros"), cómo se usan, con quién se comparten y cómo se protegen. Se basa en un análisis del código fuente real de la aplicación en la fecha indicada; no es una plantilla genérica.

---

## 1. Quiénes somos

ChatApp es una aplicación de mensajería instantánea para Android (chats 1:1, grupos, llamadas de voz/vídeo) desarrollada por [nombre del desarrollador/empresa]. Para consultas sobre privacidad, contacta a **[email de soporte]**.

---

## 2. Datos que recogemos

### 2.1 Datos de cuenta e identificación
- **Correo electrónico y contraseña, o cuenta de Google** — usados para crear tu cuenta y autenticarte. El inicio de sesión con Google usa Google Sign-In (`androidx.credentials` + `GoogleIdTokenCredential`, ver `AuthViewModel.kt`, `AuthRepositoryImpl.kt`); las credenciales se validan contra **Supabase Auth**.
- **Nombre de usuario, nombre para mostrar, foto de perfil (avatar)** — que tú introduces y que otros usuarios de la App pueden ver.
- **Identificador interno de usuario (UUID)** generado por Supabase Auth.
- **Clave pública de cifrado (E2EE)** — se genera en tu dispositivo y se sube a tu perfil (`profiles.public_key`) para que otros usuarios puedan cifrar mensajes dirigidos a ti (ver `E2EEKeyManager.kt`). La clave privada correspondiente **nunca sale del dispositivo**: se genera y almacena en el Android Keystore.
- **Estado de "última conexión" / "en línea"**, configurable por el usuario (`updateShowOnlineStatus`).

### 2.2 Contactos del dispositivo
Con tu permiso (`READ_CONTACTS`), la App:
- Lee los **correos electrónicos** almacenados en tu agenda de contactos del dispositivo (`ContactSyncManager.kt`) para sugerirte contactos que ya usan ChatApp. Esos correos se envían a nuestro backend (Supabase) para buscar coincidencias con usuarios registrados (`UserRepositoryImpl.searchUsersByEmails`, `NewChatViewModel.kt`). Solo se transmiten las direcciones de correo, no el resto de la ficha de contacto.
- Lee **nombre y número de teléfono** de contactos individuales cuando eliges expresamente a alguien desde tu agenda para iniciar un chat o enviar una invitación (`ContactRepositoryImpl.kt`, `SendInvitationUseCase.kt`). Esta lectura es puntual y activada por el usuario, no una sincronización periódica en segundo plano.

Si no concedes el permiso de contactos, estas funciones (sugerencias de contactos, invitar por teléfono) simplemente no están disponibles.

### 2.3 Contenido de las conversaciones
- **Mensajes de texto, respuestas, menciones, reacciones y ediciones.**
- **Archivos multimedia**: imágenes, vídeos, notas de voz, archivos adjuntos, GIFs y stickers que envías o recibes.
- **Metadatos de mensajes**: hora de envío, estado de lectura, si el mensaje fue editado/eliminado, si tiene expiración (mensajes efímeros).
- **Encuestas (polls)** creadas dentro de los chats y tus respuestas a ellas.

### 2.4 Ubicación
Cuando pulsas el botón de "compartir ubicación" dentro de un chat, la App obtiene tu **última ubicación conocida** por GPS o red (`ChatViewModel.fetchAndSendLocation`, permisos `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) y la envía como un enlace de Google Maps dentro del mensaje. **La App no accede a tu ubicación salvo que pulses ese botón explícitamente**; no hay rastreo de ubicación en segundo plano ni ubicación continua.

### 2.5 Audio y vídeo de llamadas
Para llamadas de voz/vídeo 1:1 y en grupo (permisos `CAMERA`, `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `BLUETOOTH_CONNECT`), la App captura audio de tu micrófono y vídeo de tu cámara y los transmite en tiempo real a través de **LiveKit** (infraestructura WebRTC), incluyendo, si la usas, la función de compartir pantalla. Estos flujos son de señal en vivo (streaming); la App no los almacena en el dispositivo salvo que actives explícitamente una grabación de llamada, si esa función está habilitada.

Las notas de voz (mensajes de audio, no llamadas) también usan `RECORD_AUDIO` y se guardan como archivo adjunto del mensaje.

### 2.6 Identificadores técnicos y datos de diagnóstico
- **Token de notificaciones push (FCM)**: Firebase Cloud Messaging asigna un token a tu instalación de la App; ese token se guarda en nuestro backend (Supabase, tabla de tokens) para poder enviarte notificaciones push (`FcmTokenManager.kt`, función Edge `send-fcm-notification`). No incluye el contenido del mensaje, solo sirve para enrutar la notificación a tu dispositivo.
- **Verificación de integridad del dispositivo/app (Play Integrity API)**: en determinados flujos, la App solicita a Google Play un token de integridad del dispositivo y la instalación, que se valida en nuestro backend (`IntegrityChecker.kt`, función Edge `verify-integrity`) para detectar apps modificadas o dispositivos no confiables. Esto no comparte tu identidad personal con Google más allá de lo que el propio servicio de Play Integrity requiere.
- **Registros técnicos básicos** generados por el uso normal de Supabase (por ejemplo, IP de conexión a la API, marcas de tiempo) como parte del funcionamiento de cualquier backend en la nube.

- **Analítica de uso (Firebase Analytics)**: registramos eventos de uso de funciones (por ejemplo, inicio de sesión, mensaje enviado, llamada iniciada/finalizada, grupo creado, invitación enviada) junto con metadatos del tipo de evento (por ejemplo, el *tipo* de mensaje — texto, imagen, audio — nunca su contenido) para entender qué funciones se usan y detectar problemas (`FirebaseAnalyticsTracker.kt`, `AnalyticsEvents.kt`). Tu identificador interno de usuario se asocia a estos eventos mientras tienes sesión iniciada. **No recogemos el contenido de tus mensajes, ni el identificador de publicidad de tu dispositivo** (la app no declara el permiso `AD_ID`, por lo que Firebase Analytics no lo recoge).
- **Diagnóstico de fallos (Firebase Crashlytics)**: si la App falla o encuentra un error inesperado, se envía un informe técnico (traza de la excepción, modelo/versión de Android, tu identificador interno de usuario) para poder solucionarlo (`FirebaseCrashReporter.kt`). En algunos casos ese informe incluye identificadores internos de usuario relacionados con el fallo (por ejemplo, si falla el cifrado E2EE de un mensaje), pero **nunca el contenido del mensaje en sí**.

### 2.7 Búsquedas de GIFs y stickers
Cuando buscas un GIF dentro de un chat, el **término de búsqueda** (o una petición de GIFs "trending" si no escribes nada) se envía a la API de **Giphy** para obtener resultados (`GiphyClient.kt`). Giphy puede recibir esa consulta de búsqueda; consulta la política de privacidad de Giphy para más detalle sobre su tratamiento.

### 2.8 Copia de seguridad en Google Drive
Si activas la función de copia de seguridad (Ajustes → Copia de seguridad), la App sube tu **historial completo de mensajes** (incluyendo adjuntos referenciados por URL, no los binarios) como un archivo JSON a **tu propia cuenta de Google Drive** (carpeta de la app, alcance `drive.file`), usando el token de la cuenta de Google ya vinculada al dispositivo (`BackupManager.kt`). Ese archivo solo es accesible por ti desde tu Google Drive; nosotros no tenemos acceso a esa copia.

Para los mensajes 1:1 cifrados de extremo a extremo, el contenido guardado en el dispositivo (y por tanto en la copia de seguridad) está **cifrado (ciphertext)**, no en texto plano — ver sección 4. Los mensajes de grupo y los metadatos de adjuntos (URLs, nombres de archivo) no están cifrados de extremo a extremo y se incluyen tal cual en la copia de seguridad.

### 2.9 Traducción de mensajes
La traducción de mensajes se realiza **en el propio dispositivo** mediante Google ML Kit Translation (`TranslationManager.kt`). El texto que traduces no se envía a un servidor de traducción; solo se descarga un modelo de idioma desde los servidores de Google la primera vez que usas un idioma nuevo.

---

## 3. Con quién compartimos datos

No vendemos tus datos. Los compartimos únicamente con los proveedores necesarios para operar la App:

| Proveedor | Qué datos recibe | Para qué |
|---|---|---|
| **Supabase** (Auth, Postgrest, Realtime, Storage, Edge Functions) | Cuenta, perfil, mensajes, archivos multimedia, claves públicas E2EE, tokens push, correos de contactos consultados | Backend principal: autenticación, base de datos, almacenamiento de archivos, sincronización en tiempo real, funciones de servidor (notificaciones, verificación de integridad) |
| **LiveKit** | Audio/vídeo de llamadas, señalización WebRTC | Infraestructura de llamadas de voz/vídeo en tiempo real |
| **Firebase Cloud Messaging (Google)** | Token de dispositivo | Entrega de notificaciones push |
| **Firebase Analytics (Google)** | Eventos de uso de funciones, tipo de evento, tu identificador interno de usuario (mientras tienes sesión iniciada) | Entender el uso de funciones de la App; nunca recibe el contenido de tus mensajes ni tu identificador de publicidad |
| **Firebase Crashlytics (Google)** | Trazas de errores/fallos, tu identificador interno de usuario, modelo y versión de Android | Diagnóstico y solución de fallos de la App; nunca recibe el contenido de tus mensajes |
| **Google (Sign-In / Credential Manager)** | Identidad de tu cuenta de Google (si eliges iniciar sesión con Google) | Autenticación alternativa al email/contraseña |
| **Google Play Integrity** | Token de integridad del dispositivo/app | Detección de manipulación de la App o dispositivos no confiables |
| **Google Drive** (tu propia cuenta) | Historial de mensajes exportado como JSON | Copia de seguridad opcional, controlada y accesible solo por ti |
| **Giphy** | Términos de búsqueda de GIFs | Resultados de búsqueda de GIFs dentro del chat |
| **Google ML Kit** | Ninguno de tu contenido (traducción on-device); solo descarga de modelos de idioma | Traducción de mensajes en el dispositivo |

No compartimos tus datos con terceros con fines publicitarios ni los usamos para perfilado comercial.

---

## 4. Cómo protegemos tus datos

- **Cifrado de extremo a extremo (E2EE) en chats 1:1**: los mensajes de texto entre dos usuarios se cifran en el dispositivo usando intercambio de claves **ECDH (curva secp256r1)** + derivación **HKDF (HMAC-SHA256)** + cifrado **AES-256-GCM** (ver `E2EEKeyManager.kt`). Las claves privadas se generan y almacenan en el **Android Keystore** y nunca se transmiten a nuestros servidores. Cuando el cifrado es aplicable, ni Supabase ni nosotros podemos leer el contenido de esos mensajes.
  - *Alcance actual*: el E2EE cubre mensajes de texto 1:1. Los mensajes de grupo, adjuntos multimedia (imágenes, audio, vídeo, archivos) y mensajes de llamada **no** están cubiertos por este cifrado de extremo a extremo (ver `MessageRepositoryImpl.kt`, líneas donde se decide `isEncrypted` solo para mensajes de texto 1:1 sin adjuntos).
- **Base de datos local cifrada**: la base de datos Room del dispositivo está cifrada con **SQLCipher** (AES-256), con la contraseña de cifrado protegida a su vez por una clave del Android Keystore (`DatabaseKeyProvider.kt`).
- **Bloqueo de aplicación con biometría**: puedes activar un bloqueo adicional con huella/reconocimiento facial o PIN del dispositivo mediante `BiometricPrompt` (`AppLockScreen.kt`, `AppLockRepository.kt`). Los datos biométricos los gestiona el sistema operativo Android; la App nunca accede a ellos directamente.
- **Cifrado en tránsito**: toda la comunicación de red usa HTTPS/TLS; el tráfico sin cifrar (HTTP) está bloqueado a nivel de sistema (`network_security_config.xml`). Además, se aplica *certificate pinning* para las conexiones a Supabase (`*.supabase.co`) y LiveKit (`*.livekit.cloud`).
- **Cifrado en reposo en el backend**: Supabase cifra los datos en reposo según sus propias garantías de infraestructura (Postgres gestionado, Storage).
- **Tokens sensibles**: los tokens de sesión y credenciales locales se guardan con `EncryptedSharedPreferences` (`SessionGuard`), respaldadas por el Android Keystore.

---

## 5. Retención y borrado de datos

- Tus mensajes y archivos permanecen almacenados mientras tu cuenta esté activa, salvo que tú o el destinatario los eliminéis (eliminación o expiración de mensajes efímeros).
- **Puedes eliminar tu cuenta de forma permanente** desde Perfil → Eliminar cuenta, con una confirmación explícita de que la acción es irreversible. Al confirmar, se ejecuta de inmediato lo siguiente:
  - Se borran tu perfil, tu clave pública E2EE, tu foto de perfil y el resto de tus archivos (fotos, audios, vídeos, stickers de tus mensajes) de nuestros servidores.
  - Se borran tus invitaciones, tus bloqueos de otros usuarios y tu pertenencia a cada conversación y grupo (equivalente a abandonarlos todos).
  - Tus mensajes en conversaciones 1:1 y de grupo se **anonimizan**: el contenido y los adjuntos se eliminan y se muestran como "mensaje eliminado", pero la fila del mensaje se conserva para que el hilo de conversación siga siendo coherente para el resto de participantes — no se le atribuyen a tu nombre ni a tu cuenta.
  - Tu cuenta se elimina de nuestro sistema de autenticación (Supabase Auth) y se cierra tu sesión en todos los dispositivos.
  - Esta operación **no se puede deshacer**.
- El token de notificaciones push se elimina de nuestro backend al cerrar sesión o eliminar la cuenta (`FcmTokenManager.deleteToken`).
- Las copias de seguridad en Google Drive son responsabilidad y propiedad del usuario: puedes eliminarlas en cualquier momento desde tu propia cuenta de Google Drive; nosotros no las controlamos ni las eliminamos por ti. Eliminar tu cuenta en la App **no borra** copias de seguridad que ya hayas subido a tu Google Drive.

---

## 6. Tus derechos

Dependiendo de tu jurisdicción (por ejemplo, RGPD en la UE/EEE), puedes tener derecho a:
- Acceder a los datos que tenemos sobre ti.
- Rectificar datos incorrectos (nombre, foto de perfil, etc., editables desde tu perfil).
- Solicitar la eliminación de tu cuenta y tus datos.
- Oponerte o limitar ciertos tratamientos.
- Portabilidad de tus datos.
- Retirar tu consentimiento a permisos concedidos (contactos, ubicación, cámara, micrófono) en cualquier momento desde los ajustes de Android, sin que ello afecte a funciones que no dependen de ese permiso.

Para ejercer cualquiera de estos derechos, contacta con **[email de soporte]**.

---

## 7. Permisos de Android y para qué se usan

| Permiso | Uso real en la App |
|---|---|
| `INTERNET` | Conexión con Supabase, LiveKit, Firebase, Google, Giphy |
| `POST_NOTIFICATIONS` | Mostrar notificaciones push de mensajes/llamadas |
| `READ_CONTACTS` | Sugerir contactos que ya usan la App e invitar contactos por teléfono/email (secciones 2.2) |
| `CAMERA` | Videollamadas y captura de fotos/vídeo para enviar en el chat |
| `RECORD_AUDIO` | Llamadas de voz/vídeo y notas de voz |
| `MODIFY_AUDIO_SETTINGS` | Gestión del audio durante llamadas (altavoz, volumen) |
| `BLUETOOTH_CONNECT` | Uso de auriculares/manos libres Bluetooth durante llamadas |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Compartir tu ubicación puntual en un chat, solo si pulsas ese botón (sección 2.4) |

---

## 8. Menores de edad

ChatApp no está dirigida a menores de 13 años (o la edad mínima aplicable en tu país) y no recogemos conscientemente datos de menores. Si crees que un menor nos ha proporcionado datos personales, contacta con **[email de soporte]** para solicitar su eliminación.

---

## 9. Cambios en esta política

Podemos actualizar esta política cuando cambien las funciones de la App. Notificaremos cambios relevantes dentro de la propia App o por otros medios razonables antes de que entren en vigor.

---

## 10. Contacto

Para cualquier consulta sobre privacidad, protección de datos o para ejercer tus derechos: **[email de soporte]**.

---

⚠️ **Este es un borrador técnico basado en el código actual.** Debe ser revisado por un abogado/asesor legal antes de publicarse, y debe alojarse en una URL pública (no en el repo) para poder pegarla en Play Console.
