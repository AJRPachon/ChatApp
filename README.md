# ChatApp

Aplicación de mensajería instantánea para Android desarrollada como proyecto personal para poner en práctica lo aprendido en desarrollo Android nativo. El 80% del código y las decisiones de arquitectura son de mi autoría; usé IA (Claude) como guía puntual para cosas concretas: testing, algunas integraciones, la configuración de Gitflow, el pipeline de CI y este mismo README son buenos ejemplos de ello.

## Funcionalidades

- Registro e inicio de sesión con email/contraseña y Google Sign-In
- Lista de conversaciones en tiempo real
- Chat individual y grupal con soporte para texto, imágenes, audio, GIFs y stickers
- Llamadas de voz y videollamadas entre usuarios
- Sistema de invitaciones de amistad con bloqueo de usuarios
- Gestión completa de grupos (crear, editar, añadir/eliminar miembros, roles)
- Notificaciones push con Firebase Cloud Messaging
- Perfil de usuario con avatar e información editable

---

## Arquitectura

El proyecto sigue **Clean Architecture** con separación estricta en tres capas y el patrón **MVI** en la capa de presentación.

```
app/
├── domain/                        # Capa de dominio — Kotlin puro, sin dependencias Android
│   ├── model/                     # Business Objects (BO): UserBO, MessageBO, ConversationBO…
│   ├── repository/                # Interfaces de repositorio
│   └── usecase/                   # Casos de uso (un archivo por caso)
│
├── data/                          # Capa de datos — implementa las interfaces del dominio
│   ├── local/
│   │   ├── entity/                # Entidades Room (DBO)
│   │   ├── dao/                   # DAOs de acceso a la BD local
│   │   ├── ChatDatabase.kt        # Definición de la base de datos Room (v10)
│   │   └── DatabaseBuilder.kt     # Migraciones v1 → v10
│   ├── remote/
│   │   ├── dto/                   # Data Transfer Objects de la API Supabase
│   │   └── source/                # Fuentes de datos remotas (Supabase, FCM)
│   ├── repository/                # Implementaciones: coordinan caché local ↔ remoto
│   ├── mapper/                    # Funciones de extensión DBO ↔ BO y DTO → DBO
│   └── session/                   # Gestión de sesión de autenticación
│
├── ui/                            # Capa de presentación — Jetpack Compose + MVI
│   ├── auth/                      # Pantalla de login y registro
│   ├── conversations/             # Lista de conversaciones
│   ├── chat/                      # Pantalla de chat (+ GiphyClient, StickerPicker)
│   ├── call/                      # Pantalla de llamada en curso
│   │   └── IncomingCall*          # Overlay de llamada entrante
│   ├── newchat/                   # Buscar usuario / importar contactos
│   ├── group/                     # Crear grupo y gestión de grupo
│   ├── invitations/               # Invitaciones de amistad
│   ├── profile/                   # Perfil propio
│   ├── userinfo/                  # Perfil de otro usuario
│   ├── components/                # Componentes reutilizables (Avatar, Button, TextField, Shimmer)
│   └── theme/                     # Color, Shape, Theme (Material3 pastel)
│
├── service/                       # Servicios en background
│   ├── ChatFirebaseMessagingService.kt
│   ├── FcmTokenManager.kt
│   ├── FcmMessageHandler.kt
│   └── ActiveChatTracker.kt
│
├── di/                            # Módulos Koin (AppModule, SharedModules)
├── utils/                         # AppLogger, CoroutineUtils (catchResult)
├── MainActivity.kt                # NavDisplay con Navigation 3 y todas las rutas
└── ChatApplication.kt             # Inicialización de Koin y Supabase

supabase/
├── functions/
│   └── send-fcm-notification/     # Edge Function Deno/TypeScript — envía push via FCM v1
└── migrations/                    # Migraciones SQL del esquema de Supabase
```

### Patrón MVI por pantalla

Cada pantalla expone:

```kotlin
val state: StateFlow<FooState>       // estado observable
val effects: Flow<FooEffect>         // efectos de un solo uso (navegación, toasts)
fun onIntent(intent: FooIntent)      // punto de entrada único para interacciones
```

Los cambios de estado siempre pasan por `_state.update { it.copy(...) }`.

---

## Tecnologías

### Android / Kotlin

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.1.20 | Lenguaje principal |
| Android Gradle Plugin | 9.1.1 | Sistema de build |
| Jetpack Compose BOM | 2026.05.00 | UI declarativa |
| Material3 | (BOM) | Sistema de diseño |
| Navigation 3 | 1.0.0 | Navegación entre pantallas |
| Room | 2.8.4 | Base de datos local SQLite con migraciones |
| Koin | 4.2.0 | Inyección de dependencias |
| Kotlin Coroutines | 1.10.1 | Concurrencia y streams asíncronos |
| Kotlin Serialization | 1.8.0 | Serialización JSON |
| Paging 3 | 3.3.6 | Carga paginada de mensajes |
| Coil 3 | 3.1.0 | Carga de imágenes, GIFs y stickers |
| KSP | 2.1.20-1.0.32 | Procesador de anotaciones para Room |

### Backend / Servicios

| Tecnología | Versión | Uso |
|---|---|---|
| Supabase | 3.5.0 | Base de datos PostgreSQL, Auth, Realtime y Storage |
| Ktor Client | 3.1.3 | Cliente HTTP (usado por el SDK de Supabase) |
| Firebase Cloud Messaging | BOM 33.14.0 | Notificaciones push |
| Google Sign-In (Credential Manager) | 1.5.0 | Autenticación con Google |
| LiveKit | 2.7.0 | Llamadas de voz y vídeo WebRTC |
| Giphy API | — | Búsqueda y envío de GIFs |
| Deno / TypeScript | — | Supabase Edge Function para envío de FCM |

### Testing

| Tecnología | Versión | Uso |
|---|---|---|
| JUnit 4 | 4.13.2 | Framework de tests |
| MockK | 1.13.17 | Mocking en Kotlin |
| Turbine | 1.2.0 | Assertions sobre Flows |
| Kotlin Coroutines Test | 1.10.1 | TestDispatcher y runTest |
| Robolectric | 4.14.1 | Tests unitarios con contexto Android |

### CI/CD

| Herramienta | Uso |
|---|---|
| GitHub Actions | Build y tests automáticos en cada push a `master`/`develop` |
| GitHub Secrets | Gestión segura de claves (Supabase, Firebase, LiveKit, Giphy) |

---

## Setup local

1. Clona el repositorio
2. Copia `local.properties.example` a `local.properties` y rellena tus claves
3. Descarga tu `google-services.json` de Firebase Console y colócalo en `app/`
4. Abre el proyecto en Android Studio y ejecuta

```bash
./gradlew assembleDebug
```

---

## Estructura de ramas (Gitflow)

```
master       ← releases (v1.0, v1.1…)
└── develop  ← integración continua
    ├── feature/domain-models
    ├── feature/domain-repositories
    ├── feature/domain-usecases
    ├── feature/data-local-*
    ├── feature/data-remote-*
    ├── feature/data-repositories
    ├── feature/dependency-injection
    ├── feature/auth-screen
    ├── feature/chat-screen
    ├── feature/call-screen
    ├── feature/push-notifications
    └── …(31 feature branches en total)
```
