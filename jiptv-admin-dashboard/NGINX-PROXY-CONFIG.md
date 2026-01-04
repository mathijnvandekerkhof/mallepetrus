# Nginx Proxy Manager Configuration for JIPTV Admin Dashboard

## Required Headers for Next.js Behind Reverse Proxy

When deploying the JIPTV Admin Dashboard behind Nginx Proxy Manager, you need to configure proper headers to prevent Server Actions errors.

### Nginx Proxy Manager Configuration

1. **Create Proxy Host**:
   - Domain: `admin.mallepetrus.nl`
   - Forward Hostname/IP: `jiptv-admin` (container name)
   - Forward Port: `3000`
   - Enable SSL with Let's Encrypt

2. **Advanced Tab - Custom Nginx Configuration**:
   Add the following configuration to handle Next.js properly:

```nginx
# Required headers for Next.js Server Actions
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Port $server_port;

# Required for Next.js Server Actions
proxy_set_header Origin $scheme://$host;
proxy_set_header Referer $scheme://$host$request_uri;

# WebSocket support (if needed)
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";

# Timeouts
proxy_connect_timeout 60s;
proxy_send_timeout 60s;
proxy_read_timeout 60s;

# Buffer settings
proxy_buffering on;
proxy_buffer_size 128k;
proxy_buffers 4 256k;
proxy_busy_buffers_size 256k;
```

### Alternative: Simple Configuration

If the above doesn't work, try this minimal configuration:

```nginx
# Minimal headers for Next.js
proxy_set_header Host $host;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header Origin $scheme://$host;
```

## Troubleshooting

### Common Issues

1. **"Missing origin header" errors**:
   - Ensure `proxy_set_header Origin $scheme://$host;` is configured
   - Check that SSL is properly configured

2. **"Failed to find Server Action" errors**:
   - This indicates a deployment mismatch
   - Restart the `jiptv-admin` container after configuration changes
   - Clear browser cache

3. **Container not starting**:
   - Check container logs: `docker logs jiptv-admin`
   - Verify the image was built correctly
   - Check port 3000 is not in use

### Verification Steps

1. **Check container status**:
   ```bash
   docker ps | grep jiptv-admin
   ```

2. **Test direct container access**:
   ```bash
   curl http://localhost:3000/api/health
   ```

3. **Test through proxy**:
   ```bash
   curl https://admin.mallepetrus.nl/api/health
   ```

4. **Check container logs**:
   ```bash
   docker logs jiptv-admin -f
   ```

## Deployment Steps

1. **Build and deploy**:
   ```bash
   ./update-admin.sh
   ```

2. **Create Portainer stack** named `jiptv-admin`:
   - Use the `portainer-stack.yml` configuration
   - Set environment variables if needed

3. **Configure Nginx Proxy Manager**:
   - Add the custom configuration above
   - Test SSL certificate

4. **Verify deployment**:
   - Navigate to `https://admin.mallepetrus.nl`
   - Check browser console for errors
   - Verify setup wizard appears if no admin exists