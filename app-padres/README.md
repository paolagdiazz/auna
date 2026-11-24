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
- `ui/Components.kt`: botones, chips, fotos, campos de texto y demás piezas comunes.
- `ui/AunaApp.kt`: navegación y barra inferior con el botón central «Hablar».
- `data/`: modelo, datos de ejemplo (`Sample.kt`) y guardado local (`Store.kt`).

## Datos

Esta versión usa los datos de ejemplo del diseño (Marco, Lucas, Ana, Javier, Laura…).
Lo que cambia la familia (perfiles, secciones nuevas, accesos, propuestas revisadas,
ajustes y qué muestra el QR) se guarda **solo en el móvil**, en un JSON privado de la app.
No hay servidor ni se envía nada. «Ajustes › Borrar todos los datos» vuelve al estado inicial.

Simulado por ahora: la grabación de voz (no usa el micrófono), el inicio de sesión de la
vista previa del QR, las llamadas y la ubicación.
