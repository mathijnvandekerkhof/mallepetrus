# JIPTV Integrated Deployment Guide

## Overview

We hebben het admin dashboard geïntegreerd in de Spring Boot applicatie om de complexe proxy configuratie problemen te vermijden. Nu draait alles in één container.

## Architecture

```
┌─────────────────────────────────────────┐
│           jiptv-app Container           │
├─────────────────────────────────────────┤
│  ┌─────────────────┐ ┌─────────────────┐│
│  │  Admin Dashboard│ │  Spring Boot    ││
│  │  (Static Files) │ │  Backend API    ││
│  │  /admin/*       │ │  /api/*         ││
│  └─────────────────┘ └─────────────────┘│
│           Port 8080                     │
└─────────────────────────────────────────┘
```

## Deployment Steps

### 1. Update Backend with Integrated Dashboard

```bash
# On VPS
./update-backend.sh
```

This will:
- Build the Next.js admin dashboard as static files
- Include them in the Spring Boot container
- Deploy everything as one integrated application

### 2. Configure Nginx Proxy Manager

#### For admin.mallepetrus.nl (Admin Dashboard):
- **Domain**: `admin.mallepetrus.nl`
- **Forward Hostname/IP**: `jiptv-app`
- **Forward Port**: `8080`
- **Forward Path**: `/admin/`

#### For api.mallepetrus.nl (API Access):
- **Domain**: `api.mallepetrus.nl`
- **Forward Hostname/IP**: `jiptv-app`
- **Forward Port**: `8080`
- **Forward Path**: `/api/`

### 3. Remove Separate Admin Container

Since we're now using an integrated approach:

1. **Stop the separate jiptv-admin stack** in Portainer
2. **Delete the jiptv-admin stack** (it's no longer needed)
3. **Update the jiptv-app stack** with the new integrated image

## URL Structure

After deployment:

- **Admin Dashboard**: `https://admin.mallepetrus.nl` → `/admin/`
- **API Endpoints**: `https://api.mallepetrus.nl` → `/api/`
- **Direct Access**: `https://mallepetrus.nl:8080/admin/` (if needed)

## Benefits of Integrated Approach

✅ **Simplified Deployment**: One container instead of two
✅ **No Proxy Issues**: No Next.js Server Actions problems
✅ **Better Performance**: Direct serving of static files
✅ **Easier Maintenance**: Single image to manage
✅ **Reduced Complexity**: No complex nginx configurations needed

## Testing

### 1. Test Admin Dashboard
```bash
# Should show the admin dashboard
curl -I https://admin.mallepetrus.nl
```

### 2. Test API Endpoints
```bash
# Should return {"status":"UP"}
curl https://api.mallepetrus.nl/actuator/health

# Should return setup status
curl https://api.mallepetrus.nl/setup/status
```

### 3. Test Direct Container Access
```bash
# Test admin dashboard directly
docker exec -it jiptv-app wget -qO- http://localhost:8080/admin/

# Test API directly
docker exec -it jiptv-app wget -qO- http://localhost:8080/api/actuator/health
```

## Troubleshooting

### Admin Dashboard Not Loading

1. **Check if static files are built**:
   ```bash
   docker exec -it jiptv-app ls -la /app/static/admin/
   ```

2. **Check Spring Boot logs**:
   ```bash
   docker logs jiptv-app -f
   ```

3. **Verify controller mapping**:
   - Admin dashboard should be served by `AdminDashboardController`
   - API endpoints should have `/api` prefix

### API Endpoints Not Working

1. **Verify all controllers have `/api` prefix**:
   ```bash
   # Should show /api/setup/status, /api/auth/login, etc.
   docker exec -it jiptv-app wget -qO- http://localhost:8080/api/actuator/mappings
   ```

2. **Check CORS configuration**:
   - Should allow `admin.mallepetrus.nl` origin
   - Should allow credentials

## Nginx Proxy Manager Configuration

### Simple Configuration for admin.mallepetrus.nl:
```nginx
# Basic proxy headers
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

### Simple Configuration for api.mallepetrus.nl:
```nginx
# Basic proxy headers
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;

# CORS headers (if needed)
add_header 'Access-Control-Allow-Origin' 'https://admin.mallepetrus.nl' always;
add_header 'Access-Control-Allow-Credentials' 'true' always;
```

## Expected Results

After successful deployment:

✅ `https://admin.mallepetrus.nl` → JIPTV Admin Dashboard
✅ `https://api.mallepetrus.nl/actuator/health` → `{"status":"UP"}`
✅ `https://api.mallepetrus.nl/setup/status` → Setup status
✅ No more Next.js Server Actions errors
✅ Single container deployment
✅ Simplified maintenance

## Rollback Plan

If there are issues, you can quickly rollback:

1. **Revert to previous image**:
   ```bash
   docker tag jiptv:previous jiptv:latest
   ```

2. **Restart container**:
   ```bash
   docker restart jiptv-app
   ```

3. **Or redeploy separate containers** using the old configuration files

## Next Steps

1. Run `./update-backend.sh` to build and deploy
2. Configure Nginx Proxy Manager with the simple settings above
3. Test both admin dashboard and API endpoints
4. Remove the old jiptv-admin stack once everything works