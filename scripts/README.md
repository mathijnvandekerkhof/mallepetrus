# 🔧 JIPTV Scripts Directory

This directory contains all deployment and utility scripts for the JIPTV project.

## 📁 Directory Structure

```
scripts/
├── README.md                    # This file
├── make-executable.sh          # Makes all scripts executable
├── admin-dashboard/            # Admin dashboard scripts
│   ├── deploy-admin.sh         # Admin dashboard deployment
│   ├── build-and-upload.sh     # Build and upload to VPS
│   ├── quick-admin-update.sh   # Quick admin dashboard update
│   └── update-admin.sh         # Interactive admin dashboard update
├── debug-branches.sh           # Debug git branches
├── deploy-vps.sh              # VPS deployment
├── deploy.sh                  # General deployment
├── quick-update.sh            # Quick backend update
├── simple-update.sh           # Simple backend update
└── update-and-build.sh        # Interactive backend update
```

## 🚀 Quick Start

### Make All Scripts Executable
```bash
# Run this first after cloning the repository
./scripts/make-executable.sh
```

### Backend Updates
```bash
# Interactive update with branch selection
./scripts/update-and-build.sh

# Quick update to main branch
./scripts/quick-update.sh main

# Simple update (fallback)
./scripts/simple-update.sh
```

### Admin Dashboard Updates
```bash
# Interactive admin dashboard update
./scripts/admin-dashboard/update-admin.sh

# Quick admin dashboard update
./scripts/admin-dashboard/quick-admin-update.sh main
```

## 🎯 Convenience Scripts (Root Directory)

For easier access, convenience scripts are available in the root directory:

```bash
# Backend updates
./update-backend.sh              # Interactive backend update
./quick-update-backend.sh main   # Quick backend update

# Admin dashboard updates
./update-admin.sh                # Interactive admin update
./quick-update-admin.sh main     # Quick admin update
```

## 📋 Script Descriptions

### Backend Scripts

- **`update-and-build.sh`**: Interactive script with branch selection, builds Docker image
- **`quick-update.sh`**: Fast update script with branch parameter support
- **`simple-update.sh`**: Fallback script with basic functionality
- **`deploy-vps.sh`**: VPS deployment automation
- **`debug-branches.sh`**: Debug git branch issues

### Admin Dashboard Scripts

- **`update-admin.sh`**: Interactive admin dashboard update with branch selection
- **`quick-admin-update.sh`**: Fast admin dashboard update
- **`deploy-admin.sh`**: Admin dashboard deployment preparation
- **`build-and-upload.sh`**: Build and upload admin dashboard to VPS

### Utility Scripts

- **`make-executable.sh`**: Makes all scripts executable (run this first!)

## 🔄 Typical Workflow

### On VPS (Production Updates)

```bash
# SSH to VPS
ssh user@your-vps

# Navigate to project
cd /opt/docker/mallepetrus

# Make scripts executable (first time only)
./scripts/make-executable.sh

# Update backend
./update-backend.sh

# Update admin dashboard
./update-admin.sh

# Restart services in Portainer
```

### Local Development

```bash
# Make scripts executable
./scripts/make-executable.sh

# Start development
./mvnw spring-boot:run

# For admin dashboard
cd jiptv-admin-dashboard
npm run dev
```

## 🐛 Troubleshooting

### Permission Issues
```bash
# If scripts are not executable
./scripts/make-executable.sh

# Or manually
chmod +x scripts/*.sh
chmod +x scripts/admin-dashboard/*.sh
chmod +x *.sh
```

### Git Issues
```bash
# All scripts automatically discard local changes
# This prevents merge conflicts during updates
```

### Docker Issues
```bash
# Check Docker status
docker --version
docker ps

# Check available space
df -h
```

## 📝 Notes

- All scripts automatically discard local changes for clean deployments
- Scripts use colored output for better readability
- Error handling and validation included in all scripts
- Cross-platform compatibility (Linux/macOS)
- Designed for VPS deployment workflow