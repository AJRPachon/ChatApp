# Room: historia del esquema

## Reinicio en la versión 1 (2026-10-06)

La base de datos se reinició en `version = 1` antes de publicar la app. Hasta entonces el esquema había llegado a la versión 40 durante el desarrollo, con 39 migraciones, pero ninguna versión se distribuyó: no hay usuarios con datos que migrar. Además, 11 de esos 40 schemas exportados (13, 14, 22 a 25 y 27 a 31) no se podían recuperar, porque nunca existieron en ningún commit (las ramas de funcionalidades se fusionaron en lotes) o pertenecían a otra numeración. Una cadena de migraciones que no se puede verificar entera no protegía a nadie, así que se descartó entera en lugar de inventar los schemas que faltaban.

Se conserva el historial en Git: el último commit con la cadena completa es el anterior al PR "reset the Room schema to version 1".

## Cómo se trabaja desde ahora

- `app/schemas/com.ajrpachon.chatapp.data.local.ChatDatabase/1.json` es el esquema de partida; lo escribe el procesador KSP de Room en cada compilación.
- **Hasta la primera versión publicada** se puede seguir tocando el esquema de la versión 1 (si no hay instalaciones que conservar, se desinstala la app de los dispositivos de prueba).
- **Desde la primera versión publicada**, cada cambio en una entidad exige: subir `version` en `ChatDatabase.kt`, registrar `Migration(X, Y)` con `addMigrations(...)` en `buildChatDatabase` (`DatabaseBuilder.kt`) y exportar `Y.json`, además de un test con `MigrationTestHelper` (`connectedDebugAndroidTest`). La skill `/room-migration` describe el flujo.
- No se usa `fallbackToDestructiveMigration`: borraría los datos de los usuarios en silencio.

## Qué lo vigila

- `ChatDatabaseSchemaTest` (JVM, Robolectric): las tablas de la base real coinciden exactamente con las entidades del `1.json` exportado. Si se cambia una entidad sin exportar el esquema, falla.
- `StickerSeedTest`: una base nueva trae los cuatro packs de stickers del sistema (`seedStickerPacks`, llamado desde el callback `onCreate` de `buildChatDatabase`).
