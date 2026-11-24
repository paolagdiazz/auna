# Puesta en marcha (todo gratis)

Aúna usa tres servicios gratuitos. Hay que configurarlos una vez.

| Servicio | Para qué | Plan |
| --- | --- | --- |
| **Firebase** (Google) | Cuentas y base de datos compartida entre la app y la web | Spark (gratis, sin tarjeta) |
| **Cloudflare Workers** | Puente de IA: guarda la clave de Groq en secreto | Free |
| **GitHub Pages** | Publicar la web | Gratis en repos públicos |
| **Groq** | La IA (modelo `openai/gpt-oss-120b`) | Free |

## 1. Firebase

1. https://console.firebase.google.com → **Crear proyecto** (`auna`, sin Google Analytics).
2. **Authentication › Comenzar › Método de inicio de sesión**: activa **Correo electrónico/contraseña** y **Google**.
3. **Authentication › Configuración › Dominios autorizados**: añade `paolagdiazz.github.io`.
4. **Firestore Database › Crear base de datos**: ubicación `eur3`, modo producción.
5. **Firestore › Reglas**: borra lo que hay, pega el contenido de [`firebase/firestore.rules`](../firebase/firestore.rules) y pulsa **Publicar**.
6. **Configuración del proyecto › General › Tus apps › Web (`</>`)**: copia `firebaseConfig`.
   - Pégalo en [`web/config.js`](../web/config.js) (`export const firebaseConfig = { ... };`).
   - Copia `apiKey` y `projectId` en [`app-padres/.../cloud/CloudConfig.kt`](../app-padres/app/src/main/java/com/auna/padres/cloud/CloudConfig.kt).

## 2. Cloudflare (puente de IA)

1. https://dash.cloudflare.com → crea una cuenta gratis.
2. **Workers & Pages › Create › Create Worker** → nombre `auna-ia` → **Deploy**.
3. **Edit code**: borra el ejemplo, pega [`worker/auna-ia.js`](../worker/auna-ia.js) y pulsa **Deploy**.
4. **Settings › Variables and Secrets**:
   - `GROQ_API_KEY` (tipo **Secret**): tu clave de https://console.groq.com (empieza por `gsk_`).
   - `FIREBASE_PROJECT_ID` (tipo **Text**): el `projectId` de Firebase.
5. Copia la dirección del Worker (`https://auna-ia.<tu-cuenta>.workers.dev`) en `AI_URL` de `web/config.js` y de `CloudConfig.kt`.

## 3. GitHub Pages

1. En https://github.com/paolagdiazz/auna › **Settings › Pages › Build and deployment › Source**: elige **GitHub Actions**.
2. Cada cambio en `web/` se publica solo en https://paolagdiazz.github.io/auna/ (pestaña **Actions** para ver el progreso).

## Cómo se usa

- **Familia (app):** Ajustes › Cuenta de la familia › crear cuenta. El pasaporte se sube solo y la tarjeta QR pasa a llevar el enlace a la web y un código `AUN-XXXX-XXXX`.
- **Profes y familiares (web):** escanean el QR o escriben el código en «Añadir niño», crean su cuenta y envían una nota.
- **Familia (app):** la nota aparece en Accesos; al aceptarla, esa persona ve «Lo esencial», los pictos y puede preguntar en el chat.

## Límites gratuitos (de sobra para pruebas)

- Firestore: 50 000 lecturas y 20 000 escrituras al día.
- Cloudflare Workers: 100 000 peticiones al día.
- Groq: depende del modelo; si se pasa, la app avisa de esperar un minuto.
