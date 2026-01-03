# JIPTV Production Deployment Script (PowerShell)
# Usage: .\deploy.ps1 [build|deploy|update|logs|status]

param(
    [Parameter(Position=0)]
    [ValidateSet("build", "deploy", "update", "logs", "status", "help")]
    [string]$Command = "help"
)

# Configuration
$ImageName = "jiptv"
$ImageTag = "latest"

# Colors for output
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Function to build Docker image
function Build-Image {
    Write-Info "Building Docker image..."
    
    # Clean previous builds
    .\mvnw.cmd clean
    
    # Build Docker image
    docker build -t "${ImageName}:${ImageTag}" .
    
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Docker image built successfully: ${ImageName}:${ImageTag}"
    } else {
        Write-Error "Failed to build Docker image"
        exit 1
    }
}

# Function to deploy to production
function Deploy-Production {
    Write-Info "Deploying to production..."
    
    # Check if docker-compose.prod.yml exists
    if (-not (Test-Path "docker-compose.prod.yml")) {
        Write-Error "docker-compose.prod.yml not found!"
        exit 1
    }
    
    # Create production environment file if it doesn't exist
    if (-not (Test-Path ".env.prod")) {
        Write-Warn ".env.prod not found. Creating template..."
        Create-EnvTemplate
    }
    
    Write-Info "Starting production deployment..."
    
    # Deploy using docker-compose
    docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
    
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Production deployment completed successfully"
        Write-Info "Application should be available at your configured domain"
    } else {
        Write-Error "Production deployment failed"
        exit 1
    }
}

# Function to create environment template
function Create-EnvTemplate {
    $envContent = @"
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
"@

    $envContent | Out-File -FilePath ".env.prod" -Encoding UTF8
    Write-Warn "Please edit .env.prod with your production values before deploying!"
}

# Function to update running deployment
function Update-Deployment {
    Write-Info "Updating production deployment..."
    
    # Build new image
    Build-Image
    
    # Update deployment
    docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --force-recreate jiptv-app
    
    # Clean up old images
    docker image prune -f
    
    Write-Info "Deployment updated successfully"
}

# Function to show logs
function Show-Logs {
    Write-Info "Showing application logs..."
    docker-compose -f docker-compose.prod.yml --env-file .env.prod logs -f jiptv-app
}

# Function to show status
function Show-Status {
    Write-Info "Checking deployment status..."
    docker-compose -f docker-compose.prod.yml --env-file .env.prod ps
    
    Write-Info "Checking application health..."
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/actuator/health" -Method Get -TimeoutSec 5
        Write-Info "Health check passed: $($response.status)"
    } catch {
        Write-Warn "Health check failed or application not accessible: $($_.Exception.Message)"
    }
}

# Function to show help
function Show-Help {
    Write-Host "JIPTV Production Deployment Script" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage: .\deploy.ps1 [command]" -ForegroundColor White
    Write-Host ""
    Write-Host "Commands:" -ForegroundColor White
    Write-Host "  build   - Build Docker image" -ForegroundColor Gray
    Write-Host "  deploy  - Build and deploy to production" -ForegroundColor Gray
    Write-Host "  update  - Update running deployment" -ForegroundColor Gray
    Write-Host "  logs    - Show application logs" -ForegroundColor Gray
    Write-Host "  status  - Show deployment status" -ForegroundColor Gray
    Write-Host "  help    - Show this help message" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Before first deployment:" -ForegroundColor Yellow
    Write-Host "1. Edit .env.prod with your production values" -ForegroundColor Gray
    Write-Host "2. Ensure your VPS has Docker and docker-compose installed" -ForegroundColor Gray
    Write-Host "3. Ensure proxy_net network exists for Nginx Proxy Manager" -ForegroundColor Gray
    Write-Host ""
}

# Main script logic
switch ($Command) {
    "build" { Build-Image }
    "deploy" { Build-Image; Deploy-Production }
    "update" { Update-Deployment }
    "logs" { Show-Logs }
    "status" { Show-Status }
    "help" { Show-Help }
    default { Show-Help }
}