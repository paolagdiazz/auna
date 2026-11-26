# Setup (all free)

*Versión en español: [PUESTA-EN-MARCHA.md](PUESTA-EN-MARCHA.md)*

How to run your own copy of Aúna. Everything uses free tiers and only needs to be configured once.

| Service | What it's for | Plan |
| --- | --- | --- |
| **Firebase** (Google) | Accounts and the database shared by the app and the web | Spark (free, no card) |
| **Cloudflare Workers** | AI bridge: keeps the Groq key secret | Free |
| **GitHub Pages** | Hosting the website | Free for public repos |
| **Groq** | The AI (model `openai/gpt-oss-120b`) | Free |

## 1. Firebase

1. Go to https://console.firebase.google.com and click **Create project** (for example `auna`, without Google Analytics).
2. **Authentication › Get started › Sign-in method**: enable **Email/Password** and **Google**.
3. **Authentication › Settings › Authorized domains**: add `<your-user>.github.io`.
4. **Firestore Database › Create database**: location `eur3`, production mode.
5. **Firestore › Rules**: replace everything with the contents of [`firebase/firestore.rules`](../firebase/firestore.rules) and click **Publish**.
6. **Copy the web config** (`firebaseConfig`):
   1. Click the **gear ⚙** next to **Project Overview** › **Project settings**.
   2. In the **General** tab, scroll down to **Your apps**.
   3. If there are no apps yet, click the **`</>`** (Web) button, name it `Aúna web` (don't enable Firebase Hosting) and click **Register app**.
   4. You'll see a block that starts with `const firebaseConfig = {`.
   5. Copy those values into [`web/app/config.js`](../web/app/config.js) (`firebaseConfig`) and into
      [`CloudConfig.kt`](../app-padres/app/src/main/java/com/auna/padres/cloud/CloudConfig.kt) (`API_KEY` and `PROJECT_ID`).
      This config is not secret: every Firebase website ships it.

## 2. Cloudflare (AI bridge)

**A. Create the account**
1. Sign up at https://dash.cloudflare.com/sign-up and verify your email. You don't need a domain.

**B. Create the Worker**
1. Open **Compute (Workers) › Workers & Pages** and click **Create**.
2. Choose **Start with Hello World!**, name it `auna-ia` and click **Deploy**.
3. Click **Edit code**, delete everything, paste the contents of [`worker/auna-ia.js`](../worker/auna-ia.js) and click **Deploy**.

**C. Add the Groq key and the Firebase project**
1. In the Worker, open **Settings › Variables and Secrets › + Add**.
2. Add a **Secret** called `GROQ_API_KEY` with your Groq key (https://console.groq.com › **API Keys**, it starts with `gsk_`).
3. Add a **Text** variable called `FIREBASE_PROJECT_ID` with your Firebase `projectId`.
4. Click **Deploy**.

**D. Copy the Worker address**
1. In **Settings › Domains & Routes** you'll see an address ending in `.workers.dev`.
2. Set it as `AI_URL` in [`web/app/config.js`](../web/app/config.js) and in
   [`CloudConfig.kt`](../app-padres/app/src/main/java/com/auna/padres/cloud/CloudConfig.kt).
3. Optional check: open it in a browser. If it says `{"error":"Solo POST."}`, the bridge works.

## 3. GitHub Pages

1. Open your fork on GitHub › **Settings › Pages**.
2. Under **Build and deployment › Source**, choose **GitHub Actions**.
3. Open the **Actions** tab. If the **Publicar la web** workflow failed before Pages was enabled, open it and click **Re-run all jobs**.
4. After a minute or two the site is live at `https://<your-user>.github.io/<repo>/`.
   Set that address plus `app/` as `WEB_URL` in `CloudConfig.kt`.

## How it's used

- **Family (app):** Settings › Family account › create an account. The passport syncs and the QR card now links to the web, with a code like `AUN-XXXX-XXXX`.
- **Teachers and relatives (web):** scan the QR or type the code in «Añadir niño», create an account and send a short note.
- **Family (app):** the note shows up in Accesos. Once accepted, that person sees the essentials and the pictograms and can ask questions in the chat.

## Free tier limits (plenty for testing)

- Firestore: 50,000 reads and 20,000 writes per day.
- Cloudflare Workers: 100,000 requests per day.
- Groq: depends on the model. If the limit is reached, the app asks you to wait a minute.
