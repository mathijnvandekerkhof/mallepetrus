#!/bin/bash

# JIPTV Scripts - Make All Executable
# Usage: ./make-executable.sh

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_info() { echo -e "${BLUE}ℹ${NC} $1"; }
print_success() { echo -e "${GREEN}✅${NC} $1"; }
print_step() { echo -e "${CYAN}▶${NC} $1"; }

clear
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              🔧 JIPTV Scripts - Make Executable              ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo

print_step "Making all scripts executable..."
echo

# Backend scripts
print_info "Backend deployment scripts:"
chmod +x scripts/debug-branches.sh && echo "  ✓ debug-branches.sh"
chmod +x scripts/deploy-vps.sh && echo "  ✓ deploy-vps.sh"
chmod +x scripts/deploy.sh && echo "  ✓ deploy.sh"
chmod +x scripts/quick-update.sh && echo "  ✓ quick-update.sh"
chmod +x scripts/simple-update.sh && echo "  ✓ simple-update.sh"
chmod +x scripts/update-and-build.sh && echo "  ✓ update-and-build.sh"

echo

# Admin dashboard scripts
print_info "Admin dashboard scripts:"
chmod +x scripts/admin-dashboard/deploy-admin.sh && echo "  ✓ deploy-admin.sh"
chmod +x scripts/admin-dashboard/build-and-upload.sh && echo "  ✓ build-and-upload.sh"
chmod +x scripts/admin-dashboard/quick-admin-update.sh && echo "  ✓ quick-admin-update.sh"
chmod +x scripts/admin-dashboard/update-admin.sh && echo "  ✓ update-admin.sh"

echo

# Maven wrapper
print_info "Maven wrapper scripts:"
chmod +x mvnw && echo "  ✓ mvnw"

echo

print_success "All scripts are now executable! 🎉"
echo
print_info "Available scripts:"
echo "  📦 Backend:"
echo "    ./scripts/update-and-build.sh     - Interactive backend update"
echo "    ./scripts/quick-update.sh         - Quick backend update"
echo "    ./scripts/simple-update.sh        - Simple backend update"
echo "    ./scripts/deploy-vps.sh           - VPS deployment"
echo
echo "  🎨 Admin Dashboard:"
echo "    ./scripts/admin-dashboard/update-admin.sh       - Interactive admin update"
echo "    ./scripts/admin-dashboard/quick-admin-update.sh - Quick admin update"
echo "    ./scripts/admin-dashboard/deploy-admin.sh       - Admin deployment"
echo
echo "  🔧 Utilities:"
echo "    ./scripts/debug-branches.sh       - Debug git branches"
echo "    ./mvnw                            - Maven wrapper"