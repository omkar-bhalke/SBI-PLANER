# WA Statement Tool

## Railway deploy
1. GitHub pe push karo, Railway me "Deploy from GitHub repo".
2. Variables: `APP_PASSWORD=<koi password>`, `AUTH_DIR=/data`
3. Volume add karo, mount path `/data` (warna redeploy pe WhatsApp logout ho jayega).
4. Settings -> Networking -> Generate Domain. Wahi domain open karo.

Local: `npm install && npm start`
