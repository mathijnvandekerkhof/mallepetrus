#!/bin/bash

# JIPTV Admin Dashboard - Build and Upload to VPS
# Usage: ./build-and-upload.sh

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

print_info() { echo -e "${BLUE}ℹ${NC} $1"; }
print_success() { echo -e "${GREEN}✅${NC} $1"; }
print_error() { echo -e "${RED}❌${NC} $1"; }
print_step() { echo -e "${CYAN}▶${NC} $1"; }

clear
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║           🎨 JIPTV Admin Dashboard - Build & Upload          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    print_error "Not in admin dashboard directory!"
    exit 1
fi

# Step 1: Build Docker image locally
print_step "[1/3] Building Docker image locally..."
if docker build -t jiptv-admin:latest .; then
    print_success "Docker image built successfully"
else
    print_error "Docker build failed"
    exit 1
fi

# Step 2: Save image to tar file
print_step "[2/3] Saving Docker image to tar file..."
if docker save jiptv-admin:latest -o jiptv-admin-latest.tar; then
    print_success "Image saved to jiptv-admin-latest.tar"
    print_info "File size: $(du -h jiptv-admin-latest.tar | cut -f1)"
else
    print_error "Failed to save image"
    exit 1
fi

# Step 3: Upload instructions
print_step "[3/3] Upload instructions"
echo
echo -e "${CYAN}📤 Next Steps - Upload to VPS:${NC}"
echo "   1. Upload the tar file to your VPS:"
echo "      scp jiptv-admin-latest.tar user@your-vps:/opt/docker/"
echo
echo "   2. SSH to your VPS and load the image:"
echo "      ssh user@your-vps"
echo "      cd /opt/docker"
echo "      docker load -i jiptv-admin-latest.tar"
echo
echo "   3. Verify the image is loaded:"
echo "      docker images | grep jiptv-admin"
echo
echo -e "${CYAN}🐳 Then proceed with Portainer deployment${NC}"

print_success "Build completed! Ready for VPS upload 🎉"