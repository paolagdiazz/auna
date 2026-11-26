# Puesta en marcha (todo gratis)

*English version: [SETUP.md](SETUP.md)*

Guía para montar tu propia copia de Aúna. Usa servicios gratuitos y solo hay que configurarlos una vez.

| Servicio | Para qué | Plan |
| --- | --- | --- |
| **Firebase** (Google) | Cuentas y base de datos compartida entre la app y la web | Spark (gratis, sin tarjeta) |
| **Cloudflare Workers** | Puente de IA: guarda la clave de Groq en secreto | Free |
| **GitHub Pages** | Publicar la web | Gratis en repos públicos |
| **Groq** | La IA (modelo `openai/gpt-oss-120b`) | Free |

## 1. Firebase

1. https://console.firebase.google.com → **Crear proyecto** (`auna`, sin Google Analytics).
2. **Authentication › Comenzar › Método de inicio de sesión**: activa **Correo electrónico/contraseña** y **Google**.
3. **Authentication › Configuración › Dominios autorizados**: añade `<tu-usuario>.github.io`.
4. **Firestore Database › Crear base de datos**: ubicación `eur3`, modo producción.
5. **Firestore › Reglas**: borra lo que hay, pega el contenido de [`firebase/firestore.rules`](../firebase/firestore.rules) y pulsa **Publicar**.
6. **Copiar la configuración de la web** (`firebaseConfig`):
   1. Arriba a la izquierda, junto a **Descripción general del proyecto**, pulsa el **engranaje ⚙** › **Configuración del proyecto**.
   2. Quédate en la pestaña **General** y baja hasta el final, a la tarjeta **Tus apps**.
   3. Si dice «No hay apps en tu proyecto», pulsa el botón redondo con el símbolo **`</>`** (Web).
      Si ya creaste la app web antes, salta al punto 7.
   4. En **Apodo de la app** escribe `Aúna web`. **No** marques «Configurar también Firebase Hosting». Pulsa **Registrar app**.
   5. Aparece **Añadir el SDK de Firebase** con dos opciones (npm / `<script>`). Da igual cuál: en las dos sale un bloque que empieza por
      `const firebaseConfig = {` y termina en `};`, con 6 o 7 líneas (`apiKey`, `authDomain`, `projectId`, `storageBucket`,
      `messagingSenderId`, `appId` y quizá `measurementId`).
   6. Copia esos valores en [`web/app/config.js`](../web/app/config.js) (`firebaseConfig`) y en
      [`CloudConfig.kt`](../app-padres/app/src/main/java/com/auna/padres/cloud/CloudConfig.kt) (`API_KEY` y `PROJECT_ID`).
      No es secreto: toda web hecha con Firebase lo lleva dentro.
   7. Pulsa **Ir a la consola**. Si ya habías registrado la app: en **Tus apps** pulsa **Aúna web** y, en **Configuración del SDK y de la app**,
      elige **Configuración** (o **Config**): ahí está el mismo bloque.

## 2. Cloudflare (puente de IA)

**A. Crear la cuenta**
1. Abre https://dash.cloudflare.com/sign-up, escribe tu correo y una contraseña y pulsa **Sign up**.
2. Confirma el correo que te envían (enlace **Verify email**). Si te ofrece añadir un dominio, sáltalo: no hace falta.

**B. Crear el Worker**
1. En el menú de la izquierda, abre **Compute (Workers)** › **Workers & Pages** (en algunas cuentas se llama solo **Workers & Pages**).
2. Pulsa **Create** (o **Create application**). Si salen pestañas, elige **Workers** y luego **Start with Hello World!** (o **Create Worker**).
3. En **Worker name** escribe `auna-ia` y pulsa **Deploy**. Espera a «Success! Your project is deployed».
4. Pulsa **Edit code** (o **Continue to project › Edit code**). Se abre un editor con un archivo `worker.js` que dice «Hello World».
5. Haz clic dentro del editor, selecciónalo todo (**Cmd + A**) y bórralo.
6. Abre [`worker/auna-ia.js`](../worker/auna-ia.js) en GitHub (botón **Raw**), selecciona todo (**Cmd + A**), cópialo y pégalo en el editor.
7. Pulsa **Deploy** (arriba a la derecha) y confirma. Vuelve atrás con la flecha **←** junto al nombre `auna-ia`.

**C. Guardar la clave de Groq y el proyecto de Firebase**
1. Dentro del Worker `auna-ia`, abre la pestaña **Settings** › **Variables and Secrets** › **+ Add**.
2. Primera variable:
   - **Type**: `Secret`
   - **Variable name**: `GROQ_API_KEY`
   - **Value**: tu clave de Groq (https://console.groq.com › **API Keys**; empieza por `gsk_`).
3. Pulsa **+ Add variable** (o **Add** otra vez) para la segunda:
   - **Type**: `Text`
   - **Variable name**: `FIREBASE_PROJECT_ID`
   - **Value**: el `projectId` del bloque de Firebase (lo que va entre comillas después de `projectId:`, p. ej. `auna-1a2b3`).
4. Pulsa **Deploy** (o **Save and deploy**).

**D. Copiar la dirección del Worker**
1. En la pestaña **Settings** › **Domains & Routes** (o en la tarjeta de arriba de **Overview**) verás una dirección que termina en
   `.workers.dev`, por ejemplo `https://auna-ia.tu-nombre.workers.dev`.
2. Ponla como `AI_URL` en [`web/app/config.js`](../web/app/config.js) y en
   [`CloudConfig.kt`](../app-padres/app/src/main/java/com/auna/padres/cloud/CloudConfig.kt).
3. Comprobación opcional: ábrela en el navegador. Si dice `{"error":"Solo POST."}`, el puente funciona.

## 3. GitHub Pages (publicar la web)

1. Abre tu copia (fork) del repositorio en GitHub e inicia sesión.
2. Arriba, en la barra de pestañas del repositorio, pulsa **⚙ Settings** (la última; si no la ves, está en el menú **···**).
3. En el menú de la izquierda, sección **Code and automation**, pulsa **Pages**.
4. En **Build and deployment › Source** cambia **Deploy from a branch** por **GitHub Actions**. No hace falta pulsar nada más: se guarda solo.
5. Abre la pestaña **Actions** del repositorio. Si sale el flujo **Publicar la web** en rojo (falló porque Pages aún no estaba activado),
   ábrelo y pulsa **Re-run all jobs**. Si no sale ninguno, haz cualquier cambio en `web/` y súbelo.
6. Cuando salga en verde (1 o 2 minutos), la web estará en `https://<tu-usuario>.github.io/<repositorio>/`.
   Pon esa dirección + `app/` como `WEB_URL` en `CloudConfig.kt` y añade `<tu-usuario>.github.io` a los dominios autorizados de Firebase.

## Cómo se usa

- **Familia (app):** Ajustes › Cuenta de la familia › crear cuenta. El pasaporte se sube solo y la tarjeta QR pasa a llevar el enlace a la web y un código `AUN-XXXX-XXXX`.
- **Profes y familiares (web):** escanean el QR o escriben el código en «Añadir niño», crean su cuenta y envían una nota.
- **Familia (app):** la nota aparece en Accesos; al aceptarla, esa persona ve «Lo esencial», los pictos y puede preguntar en el chat.

## Límites gratuitos (de sobra para pruebas)

- Firestore: 50 000 lecturas y 20 000 escrituras al día.
- Cloudflare Workers: 100 000 peticiones al día.
- Groq: depende del modelo; si se pasa, la app avisa de esperar un minuto.
