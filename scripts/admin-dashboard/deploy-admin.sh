#!/bin/bash

# JIPTV Admin Dashboard Deployment Script
# Usage: ./deploy-admin.sh

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}❌${NC} $1"
}

print_step() {
    echo -e "${CYAN}▶${NC} $1"
}

# Main deployment function
main() {
    clear
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║              🎨 JIPTV Admin Dashboard Deployment             ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo

    # Check if we're in the right directory
    if [ ! -f "package.json" ]; then
        print_error "Not in admin dashboard directory! Please run from jiptv-admin-dashboard/"
        exit 1
    fi

    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH!"
        exit 1
    fi

    # Show current status
    print_info "Current directory: $(pwd)"
    print_info "Project: $(grep '"name"' package.json | cut -d'"' -f4)"
    echo

    # Confirm deployment
    print_warning "This will build and deploy the JIPTV Admin Dashboard"
    print_warning "Make sure the backend API is running and accessible!"
    echo
    read -p "Continue with deployment? (y/N): " confirm
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        print_info "Deployment cancelled."
        exit 0
    fi

    echo
    print_step "Starting deployment process..."
    echo

    # Step 1: Install dependencies
    print_step "[1/4] Installing dependencies..."
    if npm ci --only=production; then
        print_success "Dependencies installed"
    else
        print_error "Failed to install dependencies"
        exit 1
    fi

    # Step 2: Build the application
    print_step "[2/4] Building Next.js application..."
    if npm run build; then
        print_success "Application built successfully"
    else
        print_error "Build failed"
        exit 1
    fi

    # Step 3: Build Docker image
    print_step "[3/4] Building Docker image: jiptv-admin:latest"
    if docker build -t jiptv-admin:latest .; then
        print_success "Docker image built successfully"
        
        # Show image info
        echo
        print_info "Image details:"
        docker images jiptv-admin:latest --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | head -2
    else
        print_error "Docker build failed"
        exit 1
    fi

    # Step 4: Show deployment instructions
    print_step "[4/4] Deployment instructions"
    echo
    echo -e "${CYAN}🚀 Next Steps for VPS Deployment:${NC}"
    echo "   1. Go to Portainer: https://dock.mallepetrus.nl"
    echo "   2. Create new stack: 'jiptv-admin'"
    echo "   3. Use the portainer-stack.yml configuration"
    echo "   4. Set environment variables:"
    echo "      - NEXT_PUBLIC_API_URL=https://api.mallepetrus.nl"
    echo "   5. Deploy the stack"
    echo
    echo -e "${CYAN}🌐 Configure Nginx Proxy Manager:${NC}"
    echo "   1. Add new proxy host"
    echo "   2. Domain: admin.mallepetrus.nl"
    echo "   3. Forward to: jiptv-admin:3000"
    echo "   4. Enable SSL with Let's Encrypt"
    echo
    echo -e "${CYAN}📋 Environment Variables for Portainer:${NC}"
    echo "   NEXT_PUBLIC_API_URL=https://api.mallepetrus.nl"
    echo

    print_success "Admin Dashboard deployment preparation completed! 🎉"
    echo
    print_info "The Docker image 'jiptv-admin:latest' is ready for deployment"
    print_warning "Don't forget to configure the environment variables in Portainer!"
}

# Run main function
main "$@"