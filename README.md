# JIPTV - IPTV Streaming Platform

Een complete IPTV streaming platform met WebOS TV ondersteuning, gebouwd met Spring Boot en FFmpeg transcoding.

## 🚀 Features

### ✅ Geïmplementeerd
- **Setup Wizard** - Automatische admin account aanmaak
- **Authentication System** - JWT-based met refresh tokens
- **Multi-Factor Authentication** - TOTP-based (Google Authenticator compatible)
- **Zero Trust Architecture** - Risk assessment en device management
- **User Management** - Email uitnodigingen via Brevo SMTP
- **WebOS TV Device Pairing** - QR code-based device koppeling
- **IPTV Streaming Proxy** - Complete FFmpeg stream analysis
- **Stream Transcoding & HLS Generation** - WebOS TV compatibility transcoding
- **WebOS TV Streaming API** - HLS en transcoded video delivery

### 🔄 In Development
- WebOS TV App
- Admin Dashboard
- Advanced Analytics

## 🛠 Tech Stack

- **Backend**: Spring Boot 3.2.1, Java 21
- **Database**: PostgreSQL 15, Redis 7
- **Transcoding**: FFmpeg/FFprobe
- **Security**: JWT, MFA (TOTP), Zero Trust
- **Deployment**: Docker, Portainer, Nginx Proxy Manager
- **Email**: Brevo SMTP

## 📋 Quick Start

### Lokale Development

1. **Clone repository:**
   ```bash
   git clone https://github.com/mathijnvandekerkhof/mallepetrus.git
   cd mallepetrus
   ```

2. **Start databases:**
   ```bash
   docker compose up postgres redis -d
   ```

3. **Run applicatie:**
   ```bash
   # Windows
   .\mvnw.cmd spring-boot:run
   
   # Linux/macOS
   ./mvnw spring-boot:run
   ```

4. **Setup admin account:**
   ```bash
   curl -X POST http://localhost:8080/api/setup/initialize \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@jiptv.local","password":"admin123","confirmPassword":"admin123"}'
   ```

### Production Deployment

#### Methode 1: Lokaal Builden
```bash
# Windows
.\deploy.ps1 deploy

# Linux/macOS
./deploy.sh deploy
```

#### Methode 2: VPS Deployment
```bash
# Setup VPS environment (eenmalig)
./deploy-vps.sh setup

# Deploy naar VPS
./deploy-vps.sh deploy

# Update deployment
./deploy-vps.sh update
```

#### Update Scripts (VPS)
Voor snelle updates op de VPS zijn er verschillende scripts beschikbaar:

```bash
# Interactive update met branch selectie
./update-and-build.sh

# Snelle update met branch parameter
./quick-update.sh 1          # Branch nummer
./quick-update.sh main       # Branch naam

# Eenvoudige fallback update
./simple-update.sh
```

**Features van update scripts:**
- ✅ Interactieve branch selectie met nummers
- ✅ Automatische git pull en Docker build
- ✅ Colored output en progress indicators
- ✅ Error handling en validatie
- ✅ Portainer restart instructies

#### Methode 3: Portainer Stack
1. Upload `portainer-stack.yml` naar Portainer
2. Configureer environment variables
3. Deploy stack

## 🔧 Configuration

### Environment Variables

**Database & Cache:**
```env
DB_HOST=jiptv-postgres
DB_PASSWORD=your_secure_password
REDIS_HOST=jiptv-redis
REDIS_PASSWORD=your_redis_password
```

**Security:**
```env
JWT_SECRET=your_256_bit_secret
ZERO_TRUST_ENABLED=true
ZERO_TRUST_RISK_THRESHOLD=75
```

**Email (Brevo SMTP):**
```env
BREVO_SMTP_HOST=smtp-relay.brevo.com
BREVO_SMTP_USER=your_username
BREVO_SMTP_PASSWORD=your_password
MAIL_FROM=noreply@yourdomain.com
```

**Transcoding:**
```env
TRANSCODING_MAX_CONCURRENT_JOBS=2
TRANSCODING_JOB_TIMEOUT_HOURS=6
HLS_SEGMENT_DURATION=6
```

## 📡 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token
- `GET /api/auth/me` - Current user info

### Setup & Management
- `GET /api/setup/status` - Check setup status
- `POST /api/setup/initialize` - Initialize admin account

### MFA
- `POST /api/mfa/setup` - Setup MFA
- `POST /api/mfa/enable` - Enable MFA
- `POST /api/mfa/verify` - Verify MFA code

### Device Pairing
- `POST /api/device-pairing/generate-qr` - Generate QR code
- `POST /api/device-pairing/pair` - Pair device

### Stream Management
- `GET /api/streams` - List streams
- `POST /api/streams` - Add stream (Admin)
- `POST /api/streams/{id}/analyze` - Analyze stream (Admin)
- `GET /api/streams/{id}/tracks` - Get stream tracks

### Transcoding
- `POST /api/transcoding/jobs` - Start transcoding job (Admin)
- `GET /api/transcoding/jobs/{id}` - Get job status
- `GET /api/transcoding/queue/statistics` - Queue statistics (Admin)

### Stream Delivery
- `GET /api/stream-delivery/hls/{id}/playlist.m3u8` - HLS playlist
- `GET /api/stream-delivery/transcoded/{id}` - Transcoded video
- `GET /api/stream-delivery/streams/{id}/info` - Stream delivery info

## 🔒 Security Features

- **JWT Authentication** met refresh tokens
- **Multi-Factor Authentication** (TOTP)
- **Zero Trust Architecture** met risk assessment
- **Device Fingerprinting** en management
- **Rate Limiting** via Redis
- **Email Verification** workflow
- **Secure Session Management**

## 📊 Monitoring

### Health Checks
- Application: `/api/actuator/health`
- Metrics: `/api/actuator/metrics`
- Prometheus: `/api/actuator/prometheus`

### Logging
```bash
# Container logs
docker logs jiptv-app -f

# Via deployment script
./deploy.ps1 logs        # Windows
./deploy.sh logs         # Linux/macOS
./deploy-vps.sh logs     # VPS
```

## 🐳 Docker

### Local Development
```bash
# Build image
docker build -t jiptv:latest .

# Run with compose
docker-compose up -d
```

### Production
```bash
# Production deployment
docker-compose -f docker-compose.prod.yml up -d
```

## 📁 Project Structure

```
jiptv/
├── src/main/java/nl/mallepetrus/jiptv/
│   ├── config/          # Security, Database, Redis configs
│   ├── controller/      # REST API endpoints
│   ├── service/         # Business logic & FFmpeg integration
│   ├── repository/      # Data access layer
│   ├── entity/          # JPA entities
│   ├── dto/             # Data transfer objects
│   └── security/        # JWT, MFA, Zero Trust
├── src/main/resources/
│   ├── application.yml  # Application configuration
│   └── db/migration/    # Flyway database migrations
├── docker-compose.yml   # Development containers
├── docker-compose.prod.yml # Production deployment
├── Dockerfile           # Production image
├── deploy.ps1          # Windows deployment script
├── deploy.sh           # Linux/macOS deployment script
├── deploy-vps.sh       # VPS deployment script
├── update-and-build.sh # Interactive update & build script
├── quick-update.sh     # Fast update script with branch selection
├── simple-update.sh    # Simple fallback update script
└── portainer-stack.yml # Portainer stack configuration
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit pull request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

Voor vragen of problemen:
- GitHub Issues: [Create Issue](https://github.com/mathijnvandekerkhof/mallepetrus/issues)
- Email: admin@mallepetrus.nl

---

**JIPTV** - Professional IPTV Streaming Platform