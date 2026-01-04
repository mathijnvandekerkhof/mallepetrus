# Nginx Proxy Manager Configuration for JIPTV

## Exact Configuration Settings

### 1. API Backend (api.mallepetrus.nl)

**Proxy Host Settings:**
- Domain Names: `api.mallepetrus.nl`
- Scheme: `http`
- Forward Hostname/IP: `jiptv-app`
- Forward Port: `8080`
- Cache Assets: ✅ Enabled
- Block Common Exploits: ✅ Enabled
- Websockets Support: ✅ Enabled

**SSL Tab:**
- SSL Certificate: Request a new SSL Certificate
- Force SSL: ✅ Enabled
- HTTP/2 Support: ✅ Enabled
- HSTS Enabled: ✅ Enabled

**Advanced Tab - Custom Nginx Configuration:**
```nginx
# API Backend Configuration
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;

# CORS headers for API
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

# Timeouts for API
proxy_connect_timeout 60s;
proxy_send_timeout 60s;
proxy_read_timeout 60s;
```

### 2. Admin Dashboard (admin.mallepetrus.nl)

**Proxy Host Settings:**
- Domain Names: `admin.mallepetrus.nl`
- Scheme: `http`
- Forward Hostname/IP: `jiptv-admin`
- Forward Port: `3000`
- Cache Assets: ✅ Enabled
- Block Common Exploits: ✅ Enabled
- Websockets Support: ✅ Enabled

**SSL Tab:**
- SSL Certificate: Request a new SSL Certificate
- Force SSL: ✅ Enabled
- HTTP/2 Support: ✅ Enabled
- HSTS Enabled: ✅ Enabled

**Advanced Tab - Custom Nginx Configuration:**
```nginx
# Next.js Admin Dashboard Configuration
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Port $server_port;

# CRITICAL: Required for Next.js Server Actions
proxy_set_header Origin $scheme://$host;
proxy_set_header Referer $scheme://$host$request_uri;

# WebSocket support for Next.js
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";

# Next.js specific headers
proxy_set_header X-Forwarded-Prefix "";
proxy_set_header X-NginX-Proxy true;

# Timeouts
proxy_connect_timeout 60s;
proxy_send_timeout 60s;
proxy_read_timeout 60s;

# Buffer settings for Next.js
proxy_buffering on;
proxy_buffer_size 128k;
proxy_buffers 4 256k;
proxy_busy_buffers_size 256k;

# Disable proxy cache for dynamic content
proxy_cache off;
```

## Troubleshooting Steps

### If Admin Dashboard Shows "Internal Error"

1. **Check container status:**
   ```bash
   docker ps | grep jiptv-admin
   docker logs jiptv-admin -f
   ```

2. **Test direct container access:**
   ```bash
   docker exec -it jiptv-admin wget -qO- http://localhost:3000/api/health
   ```

3. **Restart admin container:**
   ```bash
   docker restart jiptv-admin
   ```

### If API Returns 404 Errors

1. **Check backend container:**
   ```bash
   docker ps | grep jiptv-app
   docker logs jiptv-app -f
   ```

2. **Test backend directly:**
   ```bash
   docker exec -it jiptv-app wget -qO- http://localhost:8080/actuator/health
   ```

3. **Restart backend container:**
   ```bash
   docker restart jiptv-app
   ```

### Container Network Verification

All containers should be in the `proxy_net` network:
```bash
docker network inspect proxy_net
```

Expected containers:
- `jiptv-admin` (172.18.0.7)
- `jiptv-app` (172.18.0.6)
- `nginx_proxy_manager` (172.18.0.5)

## Deployment Order

1. **Update and rebuild admin dashboard:**
   ```bash
   ./update-admin.sh
   ```

2. **Update and rebuild backend:**
   ```bash
   ./update-backend.sh
   ```

3. **Restart stacks in Portainer:**
   - Restart `jiptv-admin` stack
   - Restart `jiptv-app` stack

4. **Configure Nginx Proxy Manager:**
   - Delete existing proxy hosts if they have issues
   - Create new proxy hosts with the exact configuration above
   - Test SSL certificates

5. **Verify deployment:**
   - Test: `https://api.mallepetrus.nl/actuator/health`
   - Test: `https://admin.mallepetrus.nl`