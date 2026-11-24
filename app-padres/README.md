# Aúna · App de padres

App Android (Kotlin + Jetpack Compose, Material 3) que implementa el diseño «Aúna – App de padres (mock interactivo)».

## Pantallas

| Ruta | Pantalla | Archivo |
| --- | --- | --- |
| `inicio` | 1 · Inicio | `ui/screens/InicioScreen.kt` |
| `pasaporte` | 2 · Pasaporte (cuadrícula, detalle de sección, añadir sección) | `ui/screens/PasaporteScreen.kt` |
| `hablar` | 3 · Hablar → Revisar → Guardar | `ui/screens/HablarScreen.kt` |
| `probar` | 4 · Comprueba las respuestas | `ui/screens/ProbarScreen.kt` |
| `accesos` | 5 · Accesos | `ui/screens/AccesosScreen.kt` |
| `propuestas` | 6 · Propuestas (campana) | `ui/screens/PropuestasScreen.kt` |
| `qr` | 7 · Mi tarjeta QR + vista de quien escanea | `ui/screens/QrScreen.kt` |
| `ajustes` | 8 · Ajustes | `ui/screens/AjustesScreen.kt` |
| `perfiles` | 9 · Mis perfiles | `ui/screens/PerfilesScreen.kt` |

## Cómo está hecha

- `ui/theme/Theme.kt`: colores del diseño y tipografía Nunito (del kit de marca).
- `ui/Icons.kt`: los iconos de trazo del diseño, con los mismos paths SVG.
- `ui/Components.kt`, `ui/Editors.kt`, `ui/KidEditor.kt`: botones, campos, fotos y las hojas para crear y editar.
- `ui/AunaApp.kt`: navegación (las pantallas de detalle entran deslizando) y barra inferior.
- `data/`: modelo del pasaporte (todo editable, progreso calculado), datos de ejemplo y guardado local.
- `Claude.kt`: llamadas a Claude (SDK oficial `anthropic-java`) con salida JSON estructurada.
- `VoiceInput.kt`: reconocimiento de voz de Android en español. `Speaker.kt`: lectura en voz alta.
- `QrCard.kt`: QR real con la ficha de emergencia, impresión y descarga de la tarjeta.

## Claude (IA)

«Hablar» y «Comprueba las respuestas» usan Claude (`claude-opus-5-5`). Cada familia pega su clave de la API
de Anthropic en **Ajustes › Claude (IA)**; se comprueba al guardarla y se queda solo en el móvil (no va en el
repositorio ni en copias de seguridad). Al usarlo se envían a Anthropic el pasaporte del niño activo y lo que
se acaba de contar o preguntar.

## Datos

Empieza con los datos de ejemplo (Marco y Lucas) y todo se puede editar. Lo que cambia la familia se guarda
**solo en el móvil**, en un JSON privado de la app. «Ajustes › Borrar todos los datos» vuelve al inicio.

Por ahora todo es local: las invitaciones se envían por WhatsApp u otra app, pero las solicitudes de acceso y
las propuestas de profes son de ejemplo hasta que exista el servidor (con la web app).

## Permisos

- Micrófono: para «Hablar» y dictar preguntas.
- Internet: para Claude y el reconocimiento de voz.

## Compilar

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk` (firmado con la clave de desarrollo, para pruebas).

Lista de lo que se arregló en v0.2: [`PENDIENTES.md`](PENDIENTES.md).
