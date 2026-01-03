# JIPTV Deployment Guide

## 🚀 Productie Deployment via Portainer

### Stap 1: Image Bouwen

**Lokaal bouwen:**
```bash
# Build de Docker image
docker build -t jiptv:latest .

# Tag voor registry (optioneel)
docker tag jiptv:latest your-registry/jiptv:latest

# Push naar registry (optioneel)
docker push your-registry/jiptv:latest
```

**Of direct op VPS bouwen:**
```bash
# Via SSH naar VPS
ssh user@mallepetrus.nl

# Clone repository
git clone https://github.com/mathijnvandekerkhof/mallepetrus.git /opt/docker/jiptv-source
cd /opt/docker/jiptv-source

# Build image
docker build -t jiptv:latest .
```

### Stap 2: Portainer Stack Configuratie

1. **Login naar Portainer**
   - Ga naar `https://dock.mallepetrus.nl`
   - Login met admin credentials

2. **Nieuwe Stack Aanmaken**
   - Ga naar "Stacks" → "Add stack"
   - Naam: `jiptv-app`
   - Build method: "Web editor"

3. **Docker Compose Configuratie**
   Kopieer de inhoud van `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  jiptv-app:
    image: jiptv:latest
    container_name: jiptv-app
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      DB_HOST: jiptv-postgres
      DB_PORT: 5432
      DB_NAME: jiptv
      DB_USER: jiptv
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: jiptv-redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      REDIS_DATABASE_GENERAL: ${REDIS_DATABASE_GENERAL:-0}
      REDIS_DATABASE_ZEROTRUST: ${REDIS_DATABASE_ZEROTRUST:-1}
      REDIS_DATABASE_RATELIMIT: ${REDIS_DATABASE_RATELIMIT:-2}
      JWT_SECRET: ${JWT_SECRET}
      ZERO_TRUST_ENABLED: ${ZERO_TRUST_ENABLED:-true}
      ZERO_TRUST_RISK_THRESHOLD: ${ZERO_TRUST_RISK_THRESHOLD:-75}
      ZERO_TRUST_MAX_DEVICES: ${ZERO_TRUST_MAX_DEVICES:-5}
      BREVO_SMTP_HOST: ${BREVO_SMTP_HOST}
      BREVO_SMTP_PORT: ${BREVO_SMTP_PORT:-587}
      BREVO_SMTP_USER: ${BREVO_SMTP_USER}
      BREVO_SMTP_PASSWORD: ${BREVO_SMTP_PASSWORD}
      MAIL_FROM: ${MAIL_FROM}
      ADMIN_DOMAIN: ${ADMIN_DOMAIN}
      USER_DOMAIN: ${USER_DOMAIN}
      API_DOMAIN: ${API_DOMAIN}
    networks:
      - proxy_net
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/api/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '1.0'
        reservations:
          memory: 512M
          cpus: '0.5'
    labels:
      - "nginx-proxy-manager.enable=true"

networks:
  proxy_net:
    external: true
```

### Stap 3: Environment Variables Configureren

In Portainer, onder "Environment variables":

```bash
# Database & Cache
DB_PASSWORD=your_secure_db_password
REDIS_PASSWORD=your_secure_redis_password
REDIS_DATABASE_GENERAL=0
REDIS_DATABASE_ZEROTRUST=1
REDIS_DATABASE_RATELIMIT=2

# Security & Authentication
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here_minimum_256_bits
ZERO_TRUST_ENABLED=true
ZERO_TRUST_RISK_THRESHOLD=75
ZERO_TRUST_MAX_DEVICES=5

# Brevo SMTP
BREVO_SMTP_HOST=smtp-relay.brevo.com
BREVO_SMTP_PORT=587
BREVO_SMTP_USER=your_brevo_username
BREVO_SMTP_PASSWORD=your_brevo_password
MAIL_FROM=noreply@mallepetrus.nl

# Domains
ADMIN_DOMAIN=admin.mallepetrus.nl
USER_DOMAIN=users.mallepetrus.nl
API_DOMAIN=api.mallepetrus.nl

# Application
SPRING_PROFILES_ACTIVE=prod
```

### Stap 4: Stack Deployen

1. Klik "Deploy the stack"
2. Wacht tot deployment compleet is
3. Check container status in Portainer
4. Controleer logs voor errors

### Stap 5: Nginx Proxy Manager Configuratie

De proxy hosts zijn al geconfigureerd:
- `admin.mallepetrus.nl` → `http://jiptv-app:8080`
- `users.mallepetrus.nl` → `http://jiptv-app:8080`
- `tv.mallepetrus.nl` → `http://jiptv-app:8080`

### Stap 6: Setup Wizard Uitvoeren

1. **Check setup status:**
   ```bash
   curl https://admin.mallepetrus.nl/api/setup/status
   ```

2. **Voer setup uit:**
   ```bash
   curl -X POST https://admin.mallepetrus.nl/api/setup/initialize \
     -H "Content-Type: application/json" \
     -d '{
       "email": "admin@mallepetrus.nl",
       "password": "your_secure_admin_password",
       "confirmPassword": "your_secure_admin_password"
     }'
   ```

## 🔄 Updates Deployen

### Methode 1: Image Update
```bash
# Build nieuwe image
docker build -t jiptv:latest .

# Stop en start stack in Portainer
# Of gebruik "Recreate" optie
```

### Methode 2: Rolling Update
```bash
# In Portainer, ga naar stack
# Klik "Editor" → Update compose file
# Klik "Update the stack"
```

## 📊 Monitoring & Troubleshooting

### Health Checks
```bash
# Application health
curl https://admin.mallepetrus.nl/api/actuator/health

# Container status
docker ps | grep jiptv-app
```

### Logs Bekijken
```bash
# Via Portainer: Containers → jiptv-app → Logs
# Via SSH:
docker logs jiptv-app -f
```

### Database Connectie Testen
```bash
# Connect to PostgreSQL
docker exec -it jiptv-postgres psql -U jiptv -d jiptv

# Check tables
\dt

# Check users
SELECT * FROM users;
```

### Redis Connectie Testen
```bash
# Connect to Redis
docker exec -it jiptv-redis redis-cli -a your_redis_password

# Test databases
SELECT 0  # General cache
SELECT 1  # Zero Trust data
SELECT 2  # Rate limiting
```

## 🚨 Rollback Procedure

1. **Via Portainer:**
   - Ga naar stack "jiptv-app"
   - Stop de stack
   - Update image tag naar vorige versie
   - Start stack opnieuw

2. **Via SSH:**
   ```bash
   # Stop container
   docker stop jiptv-app
   
   # Start met vorige image
   docker run -d --name jiptv-app-rollback jiptv:previous-tag
   ```

## 🔐 Security Checklist

- [ ] JWT_SECRET is minimaal 256 bits
- [ ] Database passwords zijn sterk
- [ ] BREVO SMTP credentials zijn correct
- [ ] SSL certificaten zijn actief
- [ ] Firewall regels zijn geconfigureerd
- [ ] Container draait als non-root user
- [ ] Health checks zijn actief
- [ ] Logging is geconfigureerd

## 📈 Performance Tuning

### JVM Opties
Voeg toe aan environment variables:
```bash
JAVA_OPTS=-Xmx768m -Xms512m -XX:+UseG1GC
```

### Database Connection Pool
In application-prod.yml:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### Redis Configuratie
```bash
# Verhoog Redis memory limit indien nodig
docker exec jiptv-redis redis-cli CONFIG SET maxmemory 256mb
```