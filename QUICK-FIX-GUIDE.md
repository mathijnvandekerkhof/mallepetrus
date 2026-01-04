# JIPTV Quick Fix Guide

## Current Issues & Solutions

### Issue 1: Empty update-backend.sh script
**Status**: ✅ FIXED
- Script content has been restored
- Run `./update-backend.sh` to update backend

### Issue 2: Admin Dashboard Server Actions Errors
**Status**: 🔧 NEEDS CONFIGURATION

The admin dashboard is showing "Missing origin header" errors because Nginx Proxy Manager needs specific configuration for Next.js Server Actions.

## Immediate Fix Steps

### Step 1: Update Backend
```bash
# On VPS
./update-backend.sh
```

### Step 2: Update Admin Dashboard
```bash
# On VPS
./update-admin.sh
```

### Step 3: Configure Nginx Proxy Manager

#### For api.mallepetrus.nl:
1. Go to Nginx Proxy Manager → Proxy Hosts
2. Edit or create `api.mallepetrus.nl`
3. **Details Tab**:
   - Domain: `api.mallepetrus.nl`
   - Forward Hostname/IP: `jiptv-app`
   - Forward Port: `8080`
4. **Advanced Tab** - Add this configuration:
```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;

add_header 'Access-Control-Allow-Origin' 'https://admin.mallepetrus.nl' always;
add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, Accept, Origin, X-Requested-With' always;
add_header 'Access-Control-Allow-Credentials' 'true' always;

if ($request_method = 'OPTIONS') {
    add_header 'Access-Control-Allow-Origin' 'https://admin.mallepetrus.nl' always;
    add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
    add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, Accept, Origin, X-Requested-With' always;
    add_header 'Access-Control-Allow-Credentials' 'true' always;
    add_header 'Content-Length' 0;
    return 204;
}
```

#### For admin.mallepetrus.nl:
1. Edit or create `admin.mallepetrus.nl`
2. **Details Tab**:
   - Domain: `admin.mallepetrus.nl`
   - Forward Hostname/IP: `jiptv-admin`
   - Forward Port: `3000`
3. **Advanced Tab** - Add this configuration:
```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Port $server_port;

# CRITICAL for Next.js Server Actions
proxy_set_header Origin $scheme://$host;
proxy_set_header Referer $scheme://$host$request_uri;

proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_set_header X-NginX-Proxy true;

proxy_cache off;
```

### Step 4: Restart Containers
```bash
# Restart both containers
docker restart jiptv-app jiptv-admin
```

### Step 5: Test
1. Test API: `https://api.mallepetrus.nl/actuator/health`
2. Test Admin: `https://admin.mallepetrus.nl`

## Quick Verification Commands

```bash
# Check container status
docker ps | grep jiptv

# Check container logs
docker logs jiptv-app -f
docker logs jiptv-admin -f

# Test containers directly
docker exec -it jiptv-app wget -qO- http://localhost:8080/actuator/health
docker exec -it jiptv-admin wget -qO- http://localhost:3000/api/health
```

## Expected Results

After following these steps:
- ✅ `https://api.mallepetrus.nl/actuator/health` should return `{"status":"UP"}`
- ✅ `https://admin.mallepetrus.nl` should show the JIPTV admin dashboard
- ✅ No more "Missing origin header" errors in browser console
- ✅ Setup wizard should appear if no admin account exists

## If Still Having Issues

1. **Check DNS**: Ensure `admin.mallepetrus.nl` and `api.mallepetrus.nl` point to your VPS IP
2. **Check SSL**: Verify SSL certificates are valid in Nginx Proxy Manager
3. **Check Network**: All containers should be in `proxy_net` network
4. **Check Logs**: Look for specific error messages in container logs

## Container Network Status
Current containers in proxy_net:
- `jiptv-admin`: 172.18.0.7:3000
- `jiptv-app`: 172.18.0.6:8080
- `nginx_proxy_manager`: 172.18.0.5
- `jiptv-postgres`: 172.18.0.4:5432
- `jiptv-redis`: 172.18.0.2:6379
- `portainer`: 172.18.0.3:9000