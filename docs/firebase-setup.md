# Firebase — configuración de `google-services.json`

## Proyecto Firebase real

- **Project ID:** `chatapp-27ec7`
- **Project number:** `169505634310`
- **App Android registrada:** `com.ajrpachon.chatapp` (debe coincidir exactamente con `applicationId` / `namespace` de `app/build.gradle.kts`)
- **Consola:** https://console.firebase.google.com/project/chatapp-27ec7/settings/general

## Por qué falta el archivo tras un checkout limpio

`app/google-services.json` está en `.gitignore` (a propósito — el repo tuvo credenciales reales committeadas y se limpiaron en el commit *"Remove sensitive files from tracking, update .gitignore"*). **No hay ningún placeholder committeado en el repo como fallback**: el archivo simplemente no existe tras un `git clone`/checkout limpio, y cada entorno (cada máquina de desarrollo) necesita su propia copia local.

En CI (`.github/workflows/ci.yml` y `dependency-update.yml`) el archivo se genera en tiempo de build a partir del secret de GitHub `GOOGLE_SERVICES_JSON`:

```yaml
- name: Create google-services.json
  run: echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json
```

Ese secret contiene el JSON real del proyecto `chatapp-27ec7`, así que los builds de CI (tests, lint, `assembleDebug`) usan el archivo correcto. **El problema es exclusivamente local**: si tu checkout no tiene `app/google-services.json`, el build de Gradle falla al ejecutar el plugin `google-services` (tarea `process<Variant>GoogleServices`) con un error tipo `File google-services.json is missing from module root folder`. Si en algún momento se genera manualmente un archivo de relleno (`project_id: "chatapp-placeholder"`, `project_number: "000000000000"`, `current_key: "placeholder_key"`) para poder compilar, el build sí compila, pero Firebase Cloud Messaging (push) y Crashlytics/Analytics nunca llegan al proyecto real — sin errores visibles en build, solo en runtime/logcat (`IllegalArgumentException: Please set a valid API key`, fallos al contactar `firebase-settings.crashlytics.com`, notificaciones push que nunca llegan).

## Pasos para obtener el archivo real (humano, manual)

1. Entra en [Firebase Console → chatapp-27ec7 → Configuración del proyecto](https://console.firebase.google.com/project/chatapp-27ec7/settings/general).
2. En la pestaña **General**, baja hasta **Tus apps** y selecciona la app Android `com.ajrpachon.chatapp`.
   - Si no existe todavía ninguna app Android registrada con ese `package name`, créala primero (**Agregar app → Android**, nombre de paquete `com.ajrpachon.chatapp`).
3. Pulsa el botón **`google-services.json`** para descargarlo.
4. Copia el archivo descargado a `app/google-services.json` (raíz del módulo `app`, junto a `build.gradle.kts`). El archivo queda ignorado por git — no hace falta (ni se debe) commitearlo.
5. Verifica rápido que es el real y no un placeholder:
   ```bash
   grep project_id app/google-services.json
   ```
   Debe mostrar `"project_id": "chatapp-27ec7"`. Si muestra `chatapp-placeholder` (o cualquier otro project_id), sustitúyelo.
6. Verifica que el `package_name` dentro del archivo coincide con `com.ajrpachon.chatapp` (el mismo `applicationId` de `app/build.gradle.kts`). Si no coincide, el plugin `google-services` falla el build con `No matching client found for package name 'com.ajrpachon.chatapp'`.
7. (Opcional, para que FCM funcione en emuladores/dispositivos de debug) Registra el SHA-1 del debug keystore en Firebase Console → Configuración del proyecto → Tus apps → Android:
   ```bash
   ./gradlew signingReport
   ```

## Antes de compilar el release para Play Store

**Bloqueante:** confirma que `app/google-services.json` es el archivo real de `chatapp-27ec7` (paso 5 arriba) antes de generar el AAB/APK de release. Si el archivo está ausente, `bundleRelease`/`assembleRelease` fallará directamente en la tarea `processReleaseGoogleServices`. Si el archivo es un placeholder, el build de release se generará "con éxito" pero las notificaciones push y Crashlytics/Analytics no funcionarán en producción — un fallo silencioso que solo se detecta en runtime, ya publicado.

## Referencia relacionada

El README (`Setup local`) ya menciona brevemente descargar `google-services.json`; este documento amplía esos pasos y documenta el mecanismo real de CI (secret `GOOGLE_SERVICES_JSON`, sin placeholder committeado).
