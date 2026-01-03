#!/bin/bash

# JIPTV VPS Deployment Script
# Usage: ./deploy-vps.sh [build|deploy|update|logs|status]

set -e

# Configuration
REPO_URL="https://github.com/mathijnvandekerkhof/mallepetrus.git"
VPS_HOST="mallepetrus.nl"
VPS_USER="root"  # Update with your VPS username
DEPLOY_DIR="/opt/docker/one"
IMAGE_NAME="jiptv"
IMAGE_TAG="latest"

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

# Function to deploy to VPS
deploy_to_vps() {
    log_info "Deploying JIPTV to VPS: ${VPS_HOST}"
    
    # SSH to VPS and execute deployment
    ssh ${VPS_USER}@${VPS_HOST} << 'ENDSSH'
        set -e
        
        echo "[VPS] Starting JIPTV deployment..."
        
        # Create deployment directory
        mkdir -p /opt/docker/jiptv
        cd /opt/docker/jiptv
        
        # Clone or update repository
        if [ -d ".git" ]; then
            echo "[VPS] Updating existing repository..."
            git fetch origin
            git reset --hard origin/main
        else
            echo "[VPS] Cloning repository..."
            git clone https://github.com/mathijnvandekerkhof/mallepetrus.git .
        fi
        
        # Build Docker image
        echo "[VPS] Building Docker image..."
        docker build -t jiptv:latest .
        
        # Check if Portainer stack exists
        if docker ps --format "table {{.Names}}" | grep -q "jiptv-app"; then
            echo "[VPS] Updating existing deployment..."
            # Stop existing container
            docker stop jiptv-app || true
            docker rm jiptv-app || true
        fi
        
        # Create production environment file if it doesn't exist
        if [ ! -f ".env.prod" ]; then
            echo "[VPS] Creating production environment template..."
            cat > .env.prod << 'EOF'
# JIPTV Production Environment Variables
# Update these values for your production environment

# Database Configuration
DB_HOST=jiptv-postgres
DB_PASSWORD=your_secure_database_password_here
REDIS_HOST=jiptv-redis
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
MAIL_FROM=noreply@mallepetrus.nl

# Domain Configuration
ADMIN_DOMAIN=admin.mallepetrus.nl
USER_DOMAIN=users.mallepetrus.nl
API_DOMAIN=api.mallepetrus.nl

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
EOF
            echo "[VPS] Please edit .env.prod with your production values!"
        fi
        
        # Deploy using docker-compose
        echo "[VPS] Starting deployment..."
        docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
        
        # Wait for health check
        echo "[VPS] Waiting for application to start..."
        sleep 30
        
        # Check application health
        if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
            echo "[VPS] ✅ Deployment successful! Application is healthy."
        else
            echo "[VPS] ⚠️  Deployment completed but health check failed. Check logs."
        fi
        
        echo "[VPS] Deployment completed!"
ENDSSH
    
    if [ $? -eq 0 ]; then
        log_info "VPS deployment completed successfully!"
        log_info "Application should be available at: https://api.mallepetrus.nl"
    else
        log_error "VPS deployment failed!"
        exit 1
    fi
}

# Function to show VPS logs
show_vps_logs() {
    log_info "Showing VPS application logs..."
    ssh ${VPS_USER}@${VPS_HOST} "docker logs jiptv-app -f"
}

# Function to show VPS status
show_vps_status() {
    log_info "Checking VPS deployment status..."
    ssh ${VPS_USER}@${VPS_HOST} << 'ENDSSH'
        echo "=== Container Status ==="
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(jiptv|postgres|redis)"
        
        echo ""
        echo "=== Application Health ==="
        if curl -f http://localhost:8080/api/actuator/health 2>/dev/null; then
            echo "✅ Application is healthy"
        else
            echo "❌ Application health check failed"
        fi
        
        echo ""
        echo "=== Setup Status ==="
        curl -s http://localhost:8080/api/setup/status 2>/dev/null || echo "❌ Setup endpoint not accessible"
ENDSSH
}

# Function to update VPS deployment
update_vps_deployment() {
    log_info "Updating VPS deployment..."
    ssh ${VPS_USER}@${VPS_HOST} << 'ENDSSH'
        cd /opt/docker/jiptv
        
        echo "[VPS] Pulling latest changes..."
        git fetch origin
        git reset --hard origin/main
        
        echo "[VPS] Building new image..."
        docker build -t jiptv:latest .
        
        echo "[VPS] Updating deployment..."
        docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --force-recreate jiptv-app
        
        echo "[VPS] Cleaning up old images..."
        docker image prune -f
        
        echo "[VPS] Update completed!"
ENDSSH
    
    log_info "VPS deployment updated successfully!"
}

# Function to setup VPS environment
setup_vps() {
    log_info "Setting up VPS environment for JIPTV..."
    ssh ${VPS_USER}@${VPS_HOST} << 'ENDSSH'
        echo "[VPS] Installing dependencies..."
        
        # Update system
        apt update && apt upgrade -y
        
        # Install Docker if not present
        if ! command -v docker &> /dev/null; then
            echo "[VPS] Installing Docker..."
            curl -fsSL https://get.docker.com -o get-docker.sh
            sh get-docker.sh
            systemctl enable docker
            systemctl start docker
        fi
        
        # Install Docker Compose if not present
        if ! command -v docker-compose &> /dev/null; then
            echo "[VPS] Installing Docker Compose..."
            curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
            chmod +x /usr/local/bin/docker-compose
        fi
        
        # Create proxy network if it doesn't exist
        docker network create proxy_net 2>/dev/null || true
        
        # Create directories
        mkdir -p /opt/jiptv/transcoded
        mkdir -p /opt/jiptv/logs
        
        echo "[VPS] VPS environment setup completed!"
ENDSSH
    
    log_info "VPS environment setup completed!"
}

# Function to show help
show_help() {
    echo "JIPTV VPS Deployment Script"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  setup   - Setup VPS environment (Docker, directories, etc.)"
    echo "  deploy  - Deploy JIPTV to VPS"
    echo "  update  - Update existing VPS deployment"
    echo "  logs    - Show VPS application logs"
    echo "  status  - Show VPS deployment status"
    echo "  help    - Show this help message"
    echo ""
    echo "Configuration:"
    echo "  VPS Host: ${VPS_HOST}"
    echo "  VPS User: ${VPS_USER}"
    echo "  Repository: ${REPO_URL}"
    echo ""
    echo "Before first deployment:"
    echo "1. Ensure SSH access to VPS is configured"
    echo "2. Run './deploy-vps.sh setup' to prepare VPS environment"
    echo "3. Run './deploy-vps.sh deploy' to deploy application"
    echo ""
}

# Main script logic
case "${1:-help}" in
    "setup")
        setup_vps
        ;;
    "deploy")
        deploy_to_vps
        ;;
    "update")
        update_vps_deployment
        ;;
    "logs")
        show_vps_logs
        ;;
    "status")
        show_vps_status
        ;;
    "help"|*)
        show_help
        ;;
esac