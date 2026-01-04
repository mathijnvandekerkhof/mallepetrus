# Nginx Proxy Manager 2.13.5 Configuratie voor Geïntegreerde JIPTV

## Overzicht

Na de integratie hebben we nu **één container** (`jiptv-app`) die zowel de admin dashboard als de API serveert:

```
jiptv-app container (172.18.0.6:8080)
├── /admin/          → Admin Dashboard (Next.js static files)
├── /api/            → Backend API (Spring Boot)
└── /                → Redirect naar /admin/
```

## Stap 1: Verwijder Oude Configuraties

### 1.1 Verwijder Bestaande Proxy Hosts
1. Ga naar **Nginx Proxy Manager** → **Hosts** → **Proxy Hosts**
2. Zoek naar bestaande entries voor:
   - `admin.mallepetrus.nl`
   - `api.mallepetrus.nl`
3. Klik op de **drie puntjes** (⋮) rechts van elke entry
4. Selecteer **Delete** voor beide entries

### 1.2 Stop Oude Admin Container
1. Ga naar **Portainer** → **Stacks**
2. Zoek naar `jiptv-admin` stack
3. Klik **Stop** en daarna **Remove**
4. De `jiptv-admin` container is niet meer nodig

## Stap 2: Configureer Admin Dashboard (admin.mallepetrus.nl)

### 2.1 Maak Nieuwe Proxy Host
1. Ga naar **Nginx Proxy Manager** → **Hosts** → **Proxy Hosts**
2. Klik **Add Proxy Host** (grote blauwe knop rechtsboven)

### 2.2 Details Tab
In Nginx Proxy Manager 2.13.5 vul je de volgende gegevens in:

| Veld | Waarde | Opmerking |
|------|--------|-----------|
| **Domain Names** | `admin.mallepetrus.nl` | Eén regel |
| **Scheme** | `http` | Dropdown selectie |
| **Forward Hostname / IP** | `jiptv-app` | Container naam |
| **Forward Port** | `8080` | Poort nummer |

**Let op**: Er is geen "Forward Path" veld in NPM 2.13.5. We configureren de path routing via de Advanced tab.

**Checkboxes (onderaan Details tab)**:
- ✅ **Cache Assets** 
- ✅ **Block Common Exploits**
- ✅ **Websockets Support**
- ❌ **Access List** (laat leeg)

### 2.3 SSL Tab
1. Klik op **SSL** tab (tweede tab)
2. **SSL Certificate**: Selecteer **Request a new SSL Certificate**
3. **Email Address for Let's Encrypt**: `admin@mallepetrus.nl`
4. **Checkboxes**:
   - ✅ **Use a DNS Challenge**? → **NEE, laat uit**
   - ✅ **Force SSL**
   - ✅ **HTTP/2 Support**
   - ✅ **HSTS Enabled**
   - ✅ **I Agree to the Let's Encrypt Terms of Service**

### 2.4 Advanced Tab
1. Klik op **Advanced** tab (derde tab)
2. In het **Custom Nginx Configuration** tekstveld, voeg toe:

```nginx
# Redirect root naar admin dashboard
location = / {
    return 301 /admin/;
}

# Proxy alle requests naar admin dashboard
location /admin/ {
    proxy_pass http://jiptv-app:8080/admin/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    
    # Cache instellingen voor statische bestanden
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://jiptv-app:8080;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Geen cache voor HTML bestanden
    location ~* \.html$ {
        proxy_pass http://jiptv-app:8080;
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        add_header Pragma "no-cache";
        add_header Expires "0";
    }
}
```

### 2.5 Opslaan
1. Klik **Save** (groene knop rechtsonder)
2. Wacht tot de SSL certificate wordt aangemaakt (kan 1-2 minuten duren)

## Stap 3: Configureer API (api.mallepetrus.nl)

### 3.1 Maak Nieuwe Proxy Host
1. Klik opnieuw **Add Proxy Host**

### 3.2 Details Tab
Vul de volgende gegevens in:

| Veld | Waarde | Opmerking |
|------|--------|-----------|
| **Domain Names** | `api.mallepetrus.nl` | Eén regel |
| **Scheme** | `http` | Dropdown selectie |
| **Forward Hostname / IP** | `jiptv-app` | Container naam |
| **Forward Port** | `8080` | Poort nummer |

**Let op**: Geen Forward Path veld - we configureren dit via Advanced tab.

**Checkboxes**:
- ✅ **Cache Assets** (maar wordt overschreven door Advanced config)
- ✅ **Block Common Exploits**
- ✅ **Websockets Support**
- ❌ **Access List** (laat leeg)

### 3.3 SSL Tab
1. Klik op **SSL** tab
2. **SSL Certificate**: Selecteer **Request a new SSL Certificate**
3. **Email Address for Let's Encrypt**: `admin@mallepetrus.nl`
4. **Checkboxes**:
   - ✅ **Force SSL**
   - ✅ **HTTP/2 Support**
   - ✅ **HSTS Enabled**
   - ✅ **I Agree to the Let's Encrypt Terms of Service**

### 3.4 Advanced Tab
1. Klik op **Advanced** tab
2. In het **Custom Nginx Configuration** tekstveld, voeg toe:

```nginx
# Proxy alle API requests
location /api/ {
    proxy_pass http://jiptv-app:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    
    # CORS headers voor admin dashboard
    add_header 'Access-Control-Allow-Origin' 'https://admin.mallepetrus.nl' always;
    add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
    add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, Accept, Origin, X-Requested-With' always;
    add_header 'Access-Control-Allow-Credentials' 'true' always;
    
    # Handle preflight requests
    if ($request_method = 'OPTIONS') {
        add_header 'Access-Control-Allow-Origin' 'https://admin.mallepetrus.nl' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, Accept, Origin, X-Requested-With' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;
        add_header 'Content-Length' 0;
        return 204;
    }
    
    # Geen cache voor API responses
    add_header Cache-Control "no-cache, no-store, must-revalidate" always;
    add_header Pragma "no-cache" always;
    add_header Expires "0" always;
}
```

### 3.5 Opslaan
1. Klik **Save**
2. Wacht tot de SSL certificate wordt aangemaakt

## Stap 4: Verificatie in NPM 2.13.5

### 4.1 Check Proxy Host Status
1. Ga naar **Hosts** → **Proxy Hosts**
2. Je zou nu twee entries moeten zien:
   - `admin.mallepetrus.nl` - Status: **Online** (groene cirkel)
   - `api.mallepetrus.nl` - Status: **Online** (groene cirkel)

### 4.2 Check SSL Certificates
1. Ga naar **SSL Certificates** (in het menu)
2. Je zou twee nieuwe certificates moeten zien voor beide domeinen
3. Status zou **Valid** moeten zijn

### 4.3 Test Container Status
```bash
docker ps | grep jiptv-app
```

### 4.4 Test Direct Container Access
```bash
# Test admin dashboard
docker exec -it jiptv-app wget -qO- http://localhost:8080/admin/ | head -10

# Test API
docker exec -it jiptv-app wget -qO- http://localhost:8080/api/actuator/health
```

## Stap 5: Test Via Browser

### 5.1 Admin Dashboard Test
1. Ga naar `https://admin.mallepetrus.nl`
2. Je zou de JIPTV admin dashboard moeten zien
3. Open **Developer Tools** (F12) → **Console**
4. Check voor errors (zou geen CORS errors moeten zijn)

### 5.2 API Test
1. Ga naar `https://api.mallepetrus.nl/actuator/health`
2. Je zou JSON moeten zien: `{"status":"UP"}`
3. Test setup: `https://api.mallepetrus.nl/setup/status`

## Stap 6: NPM 2.13.5 Specifieke Troubleshooting

### 6.1 SSL Certificate Problemen
**Symptoom**: "Your connection is not private"

**Oplossing in NPM 2.13.5**:
1. Ga naar **SSL Certificates**
2. Zoek je certificate
3. Klik op **View** (oog icoon)
4. Check **Expires** datum
5. Als expired: klik **Renew** knop

### 6.2 Proxy Host Offline Status
**Symptoom**: Rode cirkel bij proxy host

**Oplossing**:
1. Klik op **Edit** (potlood icoon) bij de proxy host
2. Test de **Forward Hostname/IP**: `jiptv-app`
3. Test de **Forward Port**: `8080`
4. Save en check status opnieuw

### 6.3 Advanced Configuration Errors
**Symptoom**: Nginx configuration errors

**NPM 2.13.5 heeft betere error reporting**:
1. Check **Logs** → **Nginx Logs** voor specifieke errors
2. Syntax errors worden nu direct getoond bij Save

### 6.4 Container Network Issues
```bash
# Check of containers in zelfde network zitten
docker network inspect proxy_net

# Zou beide containers moeten tonen:
# - nginx_proxy_manager
# - jiptv-app
```

## Stap 7: NPM 2.13.5 Nieuwe Features

### 7.1 Real-time Status Monitoring
- Proxy hosts tonen nu real-time status (Online/Offline)
- SSL certificates tonen expiry warnings
- Better error reporting in de interface

### 7.2 Improved SSL Management
- Automatic renewal warnings
- Better Let's Encrypt integration
- Wildcard certificate support (als je dat later wilt)

### 7.3 Enhanced Logging
- Ga naar **Settings** → **Logs** voor gedetailleerde logs
- Real-time log viewing beschikbaar

## Verwachte Resultaten

Na succesvolle configuratie in NPM 2.13.5:

✅ **Proxy Hosts**: Beide online (groene status)
✅ **SSL Certificates**: Valid en auto-renewing
✅ `https://admin.mallepetrus.nl` → JIPTV Admin Dashboard
✅ `https://api.mallepetrus.nl/actuator/health` → `{"status":"UP"}`
✅ **No CORS errors** in browser console
✅ **Fast loading** door caching configuratie
✅ **Automatic HTTPS redirect**

## NPM 2.13.5 Voordelen

- 🔄 **Real-time status updates**
- 🔒 **Improved SSL management**
- 📊 **Better logging and monitoring**
- 🚀 **Enhanced performance**
- 🛠️ **Better error reporting**

De configuratie zou nu perfect moeten werken met je NPM 2.13.5 setup! 🎉