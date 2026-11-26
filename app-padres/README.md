# Aúna · Parents app / App de padres

[English](#english) · [Español](#español)

## English

Android app for parents and legal guardians, built with Kotlin, Jetpack Compose and Material 3.

### Screens

| Route | Screen | File |
| --- | --- | --- |
| `inicio` | Home | `ui/screens/InicioScreen.kt` |
| `pasaporte` | Passport: sections, details, add a section | `ui/screens/PasaporteScreen.kt` |
| `hablar` | Talk → Review → Save | `ui/screens/HablarScreen.kt` |
| `probar` | Check the answers | `ui/screens/ProbarScreen.kt` |
| `accesos` | Who can see the passport | `ui/screens/AccesosScreen.kt` |
| `propuestas` | Suggestions from teachers | `ui/screens/PropuestasScreen.kt` |
| `qr` | Emergency QR card | `ui/screens/QrScreen.kt` |
| `ajustes` | Settings | `ui/screens/AjustesScreen.kt` |
| `perfiles` | Child profiles | `ui/screens/PerfilesScreen.kt` |

### How it's built

- `ui/theme/Theme.kt`: brand colours and the Nunito typeface.
- `ui/AunaApp.kt`: navigation with sliding transitions and the bottom bar.
- `ui/Components.kt`, `ui/Editors.kt`, `ui/KidEditor.kt`: shared components and the create and edit sheets.
- `data/`: the passport model, the example data (Marco and Lucas) and local storage.
- `cloud/`: family account and sync with Firebase through its REST API.
- `Ai.kt`, `WorkerBackend.kt`, `GroqBackend.kt`, `ClaudeBackend.kt`: the AI, with structured JSON output.
- `VoiceInput.kt` and `Speaker.kt`: Spanish speech recognition and reading aloud.
- `QrCard.kt`: the QR card, with printing and download.

### AI

«Hablar» and «Comprueba las respuestas» use AI. With a family account the app goes through the Aúna Cloudflare Worker, so no key is needed. Without an account, you can paste your own key in **Ajustes**: Groq (free, starts with `gsk_`) or Claude (starts with `sk-ant-`). The key stays on the phone.

### Data

The passport is saved on the phone. When the family creates an account in **Ajustes › Cuenta de la familia**, it is also synced to Firebase so the people they approve can see it on the web.

### Permissions

- Microphone: to talk to the app and dictate questions.
- Internet: for the AI, sync and speech recognition.

### Build

```bash
./gradlew assembleRelease
```

The APK ends up in `app/build/outputs/apk/release/app-release.apk`. It is signed with the debug key, for testing.

## Español

App Android para madres, padres y tutores legales, hecha con Kotlin, Jetpack Compose y Material 3.

### Pantallas

| Ruta | Pantalla | Archivo |
| --- | --- | --- |
| `inicio` | Inicio | `ui/screens/InicioScreen.kt` |
| `pasaporte` | Pasaporte: secciones, detalle, añadir sección | `ui/screens/PasaporteScreen.kt` |
| `hablar` | Hablar → Revisar → Guardar | `ui/screens/HablarScreen.kt` |
| `probar` | Comprueba las respuestas | `ui/screens/ProbarScreen.kt` |
| `accesos` | Quién puede ver el pasaporte | `ui/screens/AccesosScreen.kt` |
| `propuestas` | Propuestas de profes | `ui/screens/PropuestasScreen.kt` |
| `qr` | Tarjeta QR de emergencia | `ui/screens/QrScreen.kt` |
| `ajustes` | Ajustes | `ui/screens/AjustesScreen.kt` |
| `perfiles` | Perfiles de los niños | `ui/screens/PerfilesScreen.kt` |

### Cómo está hecha

- `ui/theme/Theme.kt`: colores de la marca y tipografía Nunito.
- `ui/AunaApp.kt`: navegación con transiciones deslizantes y barra inferior.
- `ui/Components.kt`, `ui/Editors.kt`, `ui/KidEditor.kt`: componentes comunes y las hojas para crear y editar.
- `data/`: modelo del pasaporte, datos de ejemplo (Marco y Lucas) y guardado local.
- `cloud/`: cuenta de la familia y sincronización con Firebase por su API REST.
- `Ai.kt`, `WorkerBackend.kt`, `GroqBackend.kt`, `ClaudeBackend.kt`: la IA, con salida JSON estructurada.
- `VoiceInput.kt` y `Speaker.kt`: reconocimiento de voz en español y lectura en voz alta.
- `QrCard.kt`: la tarjeta QR, con impresión y descarga.

### IA

«Hablar» y «Comprueba las respuestas» usan IA. Con cuenta de familia, la app pasa por el Worker de Cloudflare de Aúna y no hace falta ninguna clave. Sin cuenta, puedes pegar tu propia clave en **Ajustes**: Groq (gratis, empieza por `gsk_`) o Claude (empieza por `sk-ant-`). La clave se queda en el móvil.

### Datos

El pasaporte se guarda en el móvil. Cuando la familia crea su cuenta en **Ajustes › Cuenta de la familia**, también se sincroniza con Firebase para que las personas que acepte lo vean en la web.

### Permisos

- Micrófono: para hablar con la app y dictar preguntas.
- Internet: para la IA, la sincronización y el reconocimiento de voz.

### Compilar

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`. Está firmado con la clave de desarrollo, para pruebas.

Las notas de desarrollo de la app están en [`docs/notas-app.md`](../docs/notas-app.md).
