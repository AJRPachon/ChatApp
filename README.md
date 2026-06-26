 > [!WARNING]
> **Este proyecto se encuentra actualmente en desarrollo.** Algunas funcionalidades pueden estar incompletas o sujetas a cambios.

<div align="center">

# 💬 ChatApp

**Aplicación de mensajería instantánea para Android**

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.06.00-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3.5.0-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![LiveKit](https://img.shields.io/badge/LiveKit-2.26.0-E5363B?style=for-the-badge&logo=webrtc&logoColor=white)
![CI](https://img.shields.io/github/actions/workflow/status/AJRPachon/ChatApp/ci.yml?style=for-the-badge&label=CI&logo=githubactions&logoColor=white)

</div>

---

Proyecto personal para poner en práctica lo aprendido en desarrollo Android nativo. El 80% del código y las decisiones de arquitectura son de mi autoría; usé IA (Claude) como guía puntual para cosas concretas: testing, algunas integraciones, la configuración de Gitflow, el pipeline de CI y este mismo README son buenos ejemplos de ello.

---

## ✨ Funcionalidades

| | Funcionalidad |
|---|---|
| 🔐 | Registro e inicio de sesión con **email/contraseña** y **Google Sign-In** |
| 💬 | Chat individual y grupal con **texto, imágenes, audio, GIFs y stickers** |
| 📞 | **Llamadas de voz y videollamadas** entre usuarios via LiveKit WebRTC |
| 🔔 | **Notificaciones push** con Firebase Cloud Messaging |
| 👥 | Gestión completa de grupos: crear, editar, roles, añadir/expulsar miembros |
| 🤝 | Sistema de **invitaciones de amistad** con bloqueo de usuarios |
| 🖼️ | Perfil de usuario con **avatar** e información editable |
| ⚡ | Lista de conversaciones **en tiempo real** via Supabase Realtime |

---

## 🏗️ Arquitectura

El proyecto sigue **Clean Architecture** con tres capas bien definidas y el patrón **MVI** en presentación.

```
com.ajrpachon.chatapp/
│
├── 🟣 domain/                     ← Kotlin puro, sin dependencias Android (KMP-ready)
│   ├── model/                        UserBO, MessageBO, ConversationBO, CallBO…
│   │                                 MediaUrlValidator, MessageLimits, StickerValidation,
│   │                                 InputValidation — validación pura sin imports Android
│   ├── repository/                   Interfaces (contratos)
│   └── usecase/                      Un caso de uso por archivo
│
├── 🔵 data/                       ← Implementa las interfaces del dominio
│   ├── local/
│   │   ├── entity/                   Entidades Room (DBO)
│   │   ├── dao/                      DAOs de acceso a la BD
│   │   ├── ChatDatabase.kt           Base de datos Room (versión 11, cifrada con SQLCipher)
│   │   ├── DatabaseBuilder.kt        Migraciones v1 → v11
│   │   └── DatabaseKeyProvider.kt    Clave AES-256 en Android KeyStore
│   ├── remote/
│   │   ├── dto/                      Data Transfer Objects de Supabase
│   │   └── source/                   Fuentes remotas (Supabase, FCM tokens)
│   ├── repository/                   Coordinan caché local ↔ Supabase remoto
│   ├── mapper/                       DBO ↔ BO  /  DTO → DBO
│   └── session/                      Gestión de sesión de autenticación
│
├── 🟢 ui/                         ← Jetpack Compose + MVI
│   ├── auth/                         Login, registro e IntegrityBlockedScreen
│   ├── conversations/                Lista de conversaciones
│   ├── chat/                         Chat (+ GiphyClient, StickerPicker)
│   ├── call/                         Llamada en curso + overlay de entrante
│   ├── newchat/                      Buscar usuario / importar contactos
│   ├── group/                        Crear grupo y gestión de miembros
│   ├── invitations/                  Invitaciones de amistad
│   ├── profile/                      Perfil propio
│   ├── userinfo/                     Perfil de otro usuario
│   ├── components/                   Avatar, Button, TextField, Shimmer
│   └── theme/                        Color, Shape, Theme (Material3 pastel)
│
├── 🔴 service/                    ← Servicios en background
│   ├── ChatFirebaseMessagingService.kt
│   ├── FcmTokenManager.kt
│   ├── FcmMessageHandler.kt
│   └── ActiveChatTracker.kt
│
├── di/                            ← Módulos Koin (AppModule, SharedModules)
├── utils/                         ← AppLogger, catchResult, E2EEKeyManager,
│                                     OkHttpProvider, SessionGuard, RootDetector,
│                                     ClipboardProtection, IntegrityChecker
├── MainActivity.kt                ← NavDisplay + todas las rutas (Navigation 3)
└── ChatApplication.kt             ← Inicialización de Koin y Supabase

supabase/
├── functions/
│   ├── send-fcm-notification/     ← Edge Function: envía push via FCM v1
│   ├── livekit-token/             ← Edge Function: genera JWT de LiveKit (secreto nunca en cliente)
│   ├── verify-integrity/          ← Edge Function: valida Play Integrity token
│   └── assetlinks/                ← Edge Function: sirve /.well-known/assetlinks.json
└── migrations/                    ← Migraciones SQL del esquema
```

### Patrón MVI por pantalla

```kotlin
val state: StateFlow<FooState>    // estado observable
val effects: Flow<FooEffect>      // efectos de un solo uso (navegación, toasts)
fun onIntent(intent: FooIntent)   // punto de entrada único para interacciones
```

---

## 🔒 Seguridad

La app implementa un modelo de seguridad en capas para proteger los mensajes y los datos del usuario:

- **Cifrado de mensajes (E2EE):** los mensajes 1:1 se cifran con ECDH (P-256) + HKDF + AES-256-GCM en el dispositivo antes de enviarse, de modo que Supabase nunca almacena contenido legible. Las claves derivadas se cachean en memoria para evitar round-trips repetidos a la base de datos.
- **Base de datos local cifrada:** la caché de mensajes en el dispositivo está protegida con SQLCipher AES-256, con la clave custodiada por el Android KeyStore.
- **Transporte seguro:** certificate pinning para Supabase y LiveKit, bloqueo de HTTP en claro, rechazo de CAs de usuario y validación de dominios en todas las URLs de medios.
- **Autenticación robusta:** expiración de sesión por inactividad, revocación global al cerrar sesión y secretos de servidor nunca incluidos en el cliente (tokens LiveKit generados en Edge Function).
- **Integridad del dispositivo:** verificación con Play Integrity API al arrancar y detección de root, bloqueando o advirtiendo al usuario si el entorno no es de confianza.
- **Privacidad en uso:** `FLAG_SECURE` impide capturas de pantalla, el portapapeles se limpia automáticamente a los 60 s y los logs se suprimen en producción.
- **Backend endurecido:** RLS estricto en todas las tablas de Supabase, permisos mínimos por rol, límite de tamaño de mensajes en BD y rate limiting en las Edge Functions.

---

## 🛠️ Tecnologías

### Android / Kotlin

| Tecnología | Versión | Uso |
|---|---|---|
| ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white) **Kotlin** | 2.3.21 | Lenguaje principal |
| ![AGP](https://img.shields.io/badge/-AGP-3DDC84?logo=android&logoColor=white) **Android Gradle Plugin** | 9.1.1 | Sistema de build |
| ![Compose](https://img.shields.io/badge/-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white) **Jetpack Compose BOM** | 2026.06.00 | UI declarativa |
| ![M3](https://img.shields.io/badge/-Material%203-757575?logo=materialdesign&logoColor=white) **Material 3** | (BOM) | Sistema de diseño |
| **Navigation 3** | 1.1.3 | Navegación entre pantallas |
| ![Room](https://img.shields.io/badge/-Room-FF6F00?logo=android&logoColor=white) **Room** | 2.8.4 | Base de datos local con migraciones |
| **SQLCipher** | 4.x | Cifrado AES-256 de la base de datos Room |
| ![Koin](https://img.shields.io/badge/-Koin-F97316?logoColor=white) **Koin** | 4.2.0 | Inyección de dependencias |
| **Kotlin Coroutines + Flow** | 1.10.1 | Concurrencia y streams asíncronos |
| **Kotlin Serialization** | 1.8.0 | Serialización JSON |
| **Paging 3** | 3.5.0 | Carga paginada de mensajes |
| ![Coil](https://img.shields.io/badge/-Coil-000000?logoColor=white) **Coil 3** | 3.1.0 | Carga de imágenes, GIFs y stickers (disk cache 50 MB + memory cache 20% heap) |
| **OkHttp** | 4.x | Cliente HTTP con certificate pinning |
| **Play Integrity API** | 1.4.0 | Verificación de integridad del dispositivo y la app |

### Backend / Servicios

| Tecnología | Versión | Uso |
|---|---|---|
| ![Supabase](https://img.shields.io/badge/-Supabase-3ECF8E?logo=supabase&logoColor=white) **Supabase** | 3.5.0 | PostgreSQL, Auth, Realtime y Storage |
| ![Ktor](https://img.shields.io/badge/-Ktor-0095D5?logo=kotlin&logoColor=white) **Ktor Client** | 3.5.0 | Cliente HTTP |
| ![Firebase](https://img.shields.io/badge/-Firebase%20FCM-FFCA28?logo=firebase&logoColor=black) **Firebase Cloud Messaging** | BOM 33.14.0 | Notificaciones push |
| ![Google](https://img.shields.io/badge/-Google%20Sign--In-4285F4?logo=google&logoColor=white) **Credential Manager** | 1.6.0 | Autenticación con Google |
| ![LiveKit](https://img.shields.io/badge/-LiveKit-E5363B?logoColor=white) **LiveKit** | 2.26.0 | Llamadas de voz y vídeo WebRTC |
| **Giphy API** | — | Búsqueda y envío de GIFs |
| ![Deno](https://img.shields.io/badge/-Deno%20%2F%20TypeScript-000000?logo=deno&logoColor=white) **Deno / TypeScript** | — | Supabase Edge Functions (FCM, LiveKit token, Play Integrity, assetlinks) |

### Testing

| Tecnología | Versión | Uso |
|---|---|---|
| **JUnit 4** | 4.13.2 | Framework de tests |
| ![MockK](https://img.shields.io/badge/-MockK-E14343?logoColor=white) **MockK** | 1.13.17 | Mocking en Kotlin |
| **Turbine** | 1.2.0 | Assertions sobre Flows |
| **Coroutines Test** | 1.10.1 | TestDispatcher y runTest |
| **Robolectric** | 4.14.1 | Tests unitarios con contexto Android |

### CI/CD

| Herramienta | Uso |
|---|---|
| ![GitHub Actions](https://img.shields.io/badge/-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white) **GitHub Actions** | Pipeline paralelo en cada push: unit tests + cobertura Jacoco, Android Lint, Detekt, build debug y release |
| ![GitHub Secrets](https://img.shields.io/badge/-GitHub%20Secrets-181717?logo=github&logoColor=white) **GitHub Secrets** | Gestión segura de claves (Supabase, Firebase, LiveKit, Giphy) |
| **Detekt 1.23.8** | Análisis estático de Kotlin con baseline para código heredado |
| **Jacoco** | Cobertura de tests unitarios generada en cada CI run |
| **OWASP Dependency Check** | Escaneo semanal de dependencias con vulnerabilidades conocidas (CVE) |

---

## 🚀 Setup local

1. Clona el repositorio
2. Copia `local.properties.example` → `local.properties` y rellena tus claves
3. Descarga tu `google-services.json` de [Firebase Console](https://console.firebase.google.com) y colócalo en `app/`
4. Abre el proyecto en Android Studio y ejecuta:

```bash
./gradlew assembleDebug
```

---

## 🌿 Estructura de ramas (Gitflow)

```
master        ← releases estables (v1.0, v1.1…)
└── develop   ← integración continua
    ├── feature/…                  (31 feature branches de funcionalidad)
    ├── feature/coil-disk-cache
    ├── feature/strictmode-debug
    ├── fix/viewmodel-coroutine-leaks
    ├── fix/e2ee-key-cache
    ├── ci/improvements
    ├── security/flag-secure
    ├── security/backup-rules
    ├── security/exported-components
    ├── security/play-integrity
    ├── security/session-revocation
    ├── security/storage-size-limits
    ├── security/proguard-obfuscation
    ├── security/dependency-pinning
    ├── security/coil-certificate-pinning
    ├── security/log-sanitization
    ├── security/message-length-validation
    ├── security/edge-function-rate-limiting
    ├── security/intent-validation
    ├── security/sticker-url-validation
    ├── security/media-url-whitelist
    ├── security/play-integrity-enforcement
    ├── security/deep-link-verification
    ├── security/postgres-grants-audit
    ├── security/clipboard-protection
    ├── security/certificate-transparency
    ├── security/session-expiration
    ├── security/root-detection
    └── security/e2ee-messages
```
