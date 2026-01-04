# 🎨 JIPTV Admin Dashboard - VPS Deployment Guide

## 📋 Deployment Checklist

### Prerequisites
- [x] JIPTV Backend running and accessible
- [x] Git repository pushed to GitHub
- [ ] VPS access via SSH
- [ ] Docker installed on VPS
- [ ] Portainer running on VPS

## 🚀 Step-by-Step Deployment

### 1. SSH to VPS and Update Repository

```bash
# SSH to your VPS
ssh mathijsvdk@your-vps-ip

# Navigate to project directory
cd /opt/docker/mallepetrus

# Pull latest changes (includes admin dashboard)
git pull origin main

# Navigate to admin dashboard
cd jiptv-admin-dashboard
```

### 2. Build Admin Dashboard on VPS

```bash
# Option A: Interactive update with branch selection
./update-admin.sh

# Option B: Quick update to main branch
./quick-admin-update.sh main
```

The script will:
- ✅ Fetch latest changes from GitHub
- ✅ Discard any local changes (clean deployment)
- ✅ Build Docker image: `jiptv-admin:latest`
- ✅ Show deployment instructions

### 3. Deploy via Portainer

1. **Open Portainer**: https://dock.mallepetrus.nl
2. **Create New Stack**: 
   - Name: `jiptv-admin`
   - Copy content from `jiptv-admin-dashboard/portainer-stack.yml`
3. **Set Environment Variables**:
   ```
   NEXT_PUBLIC_API_URL=https://api.mallepetrus.nl
   ```
4. **Deploy Stack**

### 4. Configure Nginx Proxy Manager

1. **Add New Proxy Host**:
   - Domain: `admin.mallepetrus.nl`
   - Forward Hostname/IP: `jiptv-admin`
   - Forward Port: `3000`
   - Protocol: `http`

2. **SSL Configuration**:
   - Enable SSL
   - Force SSL
   - Use Let's Encrypt
   - Email: your-email@domain.com

## 🔧 Environment Variables for Portainer

```env
NEXT_PUBLIC_API_URL=https://api.mallepetrus.nl
NODE_ENV=production
NEXT_PUBLIC_APP_NAME=JIPTV Admin Dashboard
NEXT_PUBLIC_APP_VERSION=1.0.0
```

## 🌐 Access Points

After successful deployment:

- **Admin Dashboard**: https://admin.mallepetrus.nl
- **Backend API**: https://api.mallepetrus.nl
- **Portainer**: https://dock.mallepetrus.nl
- **Health Check**: https://admin.mallepetrus.nl/api/health

## 🔄 Update Workflow

For future updates:

```bash
# SSH to VPS
ssh mathijsvdk@your-vps-ip

# Navigate to admin dashboard
cd /opt/docker/mallepetrus/jiptv-admin-dashboard

# Quick update
./quick-admin-update.sh main

# Then restart in Portainer:
# Stacks → jiptv-admin → Restart
```

## 🐛 Troubleshooting

### Common Issues

1. **Docker Build Fails**:
   ```bash
   # Check Docker status
   docker --version
   docker ps
   
   # Check available space
   df -h
   ```

2. **Git Issues**:
   ```bash
   # Reset repository if needed
   git reset --hard origin/main
   git clean -fd
   ```

3. **Port Conflicts**:
   ```bash
   # Check if port 3000 is in use
   netstat -tulpn | grep :3000
   ```

4. **Container Logs**:
   ```bash
   # Check container logs
   docker logs jiptv-admin
   ```

### Health Checks

```bash
# Test admin dashboard health
curl http://localhost:3000/api/health

# Test backend API connection
curl https://api.mallepetrus.nl/actuator/health
```

## 📊 Monitoring

After deployment, monitor:

- **Container Status**: Portainer → Containers → jiptv-admin
- **Logs**: Portainer → Containers → jiptv-admin → Logs
- **Health**: https://admin.mallepetrus.nl/api/health
- **SSL Certificate**: Check expiry in Nginx Proxy Manager

## 🎯 Next Steps

After successful deployment:

1. **Test Authentication**: Login with admin credentials
2. **Verify API Connection**: Check if backend data loads
3. **Test MFA Flow**: If MFA is enabled
4. **Configure Dashboard**: Set up user management, device monitoring
5. **SSL Verification**: Ensure HTTPS works correctly

## 📝 Notes

- Admin dashboard runs on port 3000
- Uses `proxy_net` network to communicate with backend
- Automatic SSL via Let's Encrypt
- Health checks every 30 seconds
- Logs available in Portainer interface