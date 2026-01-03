#!/bin/bash

# JIPTV Production Deployment Script
# Usage: ./deploy.sh [build|deploy|update]

set -e

# Configuration
IMAGE_NAME="jiptv"
IMAGE_TAG="latest"
REGISTRY_URL="your-registry.com"  # Update with your registry if using one

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to build Docker image
build_image() {
    log_info "Building Docker image..."
    
    # Clean previous builds
    ./mvnw clean
    
    # Build Docker image
    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
    
    if [ $? -eq 0 ]; then
        log_info "Docker image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}"
    else
        log_error "Failed to build Docker image"
        exit 1
    fi
}

# Function to deploy to production
deploy_production() {
    log_info "Deploying to production..."
    
    # Check if docker-compose.prod.yml exists
    if [ ! -f "docker-compose.prod.yml" ]; then
        log_error "docker-compose.prod.yml not found!"
        exit 1
    fi
    
    # Create production environment file if it doesn't exist
    if [ ! -f ".env.prod" ]; then
        log_warn ".env.prod not found. Creating template..."
        create_env_template
    fi
    
    log_info "Starting production deployment..."
    
    # Deploy using docker-compose
    docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
    
    if [ $? -eq 0 ]; then
        log_info "Production deployment completed successfully"
        log_info "Application should be available at your configured domain"
    else
        log_error "Production deployment failed"
        exit 1
    fi
}

# Function to create environment template
create_env_template() {
    cat > .env.prod << EOF
# JIPTV Production Environment Variables
# Copy this file and update with your production values

# Database Configuration
DB_PASSWORD=your_secure_database_password_here

# Redis Configuration  
REDIS_PASSWORD=your_secure_redis_password_here
REDIS_DATABASE_GENERAL=0
REDIS_DATABASE_ZEROTRUST=1
REDIS_DATABASE_RATELIMIT=2

# JWT Security
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_at_least_256_bits_long

# Zero Trust Configuration
ZERO_TRUST_ENABLED=true
ZERO_TRUST_RISK_THRESHOLD=75
ZERO_TRUST_MAX_DEVICES=5

# Email Configuration (Brevo SMTP)
BREVO_SMTP_HOST=smtp-relay.brevo.com
BREVO_SMTP_PORT=587
BREVO_SMTP_USER=your_brevo_username
BREVO_SMTP_PASSWORD=your_brevo_password
MAIL_FROM=noreply@yourdomain.com

# Domain Configuration
ADMIN_DOMAIN=admin.yourdomain.com
USER_DOMAIN=users.yourdomain.com
API_DOMAIN=api.yourdomain.com

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# FFmpeg Configuration
FFMPEG_PATH=ffmpeg
FFPROBE_PATH=ffprobe
FFMPEG_TIMEOUT=3600

# Transcoding Configuration
TRANSCODING_OUTPUT_DIR=/app/transcoded
TRANSCODING_MAX_CONCURRENT_JOBS=2
TRANSCODING_JOB_TIMEOUT_HOURS=6
TRANSCODING_CLEANUP_DAYS=14
HLS_SEGMENT_DURATION=6

# Optional: External media directory
# MEDIA_PATH=/path/to/your/media
EOF

    log_warn "Please edit .env.prod with your production values before deploying!"
}

# Function to update running deployment
update_deployment() {
    log_info "Updating production deployment..."
    
    # Build new image
    build_image
    
    # Update deployment
    docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --force-recreate jiptv-app
    
    # Clean up old images
    docker image prune -f
    
    log_info "Deployment updated successfully"
}

# Function to show logs
show_logs() {
    log_info "Showing application logs..."
    docker-compose -f docker-compose.prod.yml --env-file .env.prod logs -f jiptv-app
}

# Function to show status
show_status() {
    log_info "Checking deployment status..."
    docker-compose -f docker-compose.prod.yml --env-file .env.prod ps
    
    log_info "Checking application health..."
    if command -v curl &> /dev/null; then
        curl -f http://localhost:8080/api/actuator/health || log_warn "Health check failed or application not accessible"
    else
        log_warn "curl not available, cannot perform health check"
    fi
}

# Main script logic
case "${1:-help}" in
    "build")
        build_image
        ;;
    "deploy")
        build_image
        deploy_production
        ;;
    "update")
        update_deployment
        ;;
    "logs")
        show_logs
        ;;
    "status")
        show_status
        ;;
    "help"|*)
        echo "JIPTV Production Deployment Script"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  build   - Build Docker image"
        echo "  deploy  - Build and deploy to production"
        echo "  update  - Update running deployment"
        echo "  logs    - Show application logs"
        echo "  status  - Show deployment status"
        echo "  help    - Show this help message"
        echo ""
        echo "Before first deployment:"
        echo "1. Edit .env.prod with your production values"
        echo "2. Ensure your VPS has Docker and docker-compose installed"
        echo "3. Ensure proxy_net network exists for Nginx Proxy Manager"
        echo ""
        ;;
esac