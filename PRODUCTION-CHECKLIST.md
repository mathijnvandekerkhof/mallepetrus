# 🚀 JIPTV Production Deployment Checklist

## Pre-Deployment Vereisten

### VPS Setup ✅
- [ ] Strato VPS toegankelijk via SSH
- [ ] Docker 20.10+ geïnstalleerd
- [ ] Docker Compose v2 geïnstalleerd  
- [ ] Portainer draait op `https://dock.mallepetrus.nl`
- [ ] Nginx Proxy Manager geconfigureerd
- [ ] `proxy_net` Docker network bestaat

### Database Stack ✅
- [ ] `jiptv-databases` stack draait in Portainer
- [ ] PostgreSQL 15 container (`jiptv-postgres`) is healthy
- [ ] Redis 7 container (`jiptv-redis`) is healthy
- [ ] Database credentials zijn veilig opgeslagen

### Domain & SSL ✅
- [ ] DNS records wijzen naar VPS IP
  - `admin.mallepetrus.nl`
  - `users.mallepetrus.nl` 
  - `api.mallepetrus.nl`
- [ ] SSL certificaten zijn geïnstalleerd via Let's Encrypt
- [ ] Nginx Proxy Manager proxy hosts geconfigureerd

## Deployment Stappen

### Stap 1: Code Preparatie
- [ ] Laatste code gecommit en gepusht
- [ ] Alle tests slagen lokaal
- [ ] `.env.prod` bestand aangemaakt met productie values
- [ ] Productie configuratie gevalideerd

### Stap 2: Docker Image
- [ ] Docker image gebouwd: `docker build -t jiptv:latest .`
- [ ] Image getest lokaal
- [ ] Image geüpload naar VPS (via registry of direct build)

### Stap 3: Portainer Deployment
- [ ] Nieuwe stack `jiptv-app` aangemaakt in Portainer
- [ ] `portainer-stack.yml` configuratie gekopieerd
- [ ] Environment variables geconfigureerd:

#### Verplichte Environment Variables:
```bash
# Database
DB_PASSWORD=<secure_password>
REDIS_PASSWORD=<secure_password>

# Security  
JWT_SECRET=<256_bit_secret>

# Email (Brevo)
BREVO_SMTP_HOST=smtp-relay.brevo.com
BREVO_SMTP_PORT=587
BREVO_SMTP_USER=<brevo_username>
BREVO_SMTP_PASSWORD=<brevo_password>
MAIL_FROM=noreply@mallepetrus.nl

# Domains
ADMIN_DOMAIN=admin.mallepetrus.nl
USER_DOMAIN=users.mallepetrus.nl
API_DOMAIN=api.mallepetrus.nl

# Transcoding
TRANSCODING_MAX_CONCURRENT_JOBS=2
TRANSCODING_JOB_TIMEOUT_HOURS=6
TRANSCODING_CLEANUP_DAYS=14
```

- [ ] Stack gedeployed en draait
- [ ] Container health check is groen
- [ ] Logs tonen geen errors

### Stap 4: Application Setup
- [ ] Setup status gecontroleerd: `GET /api/setup/status`
- [ ] Admin account aangemaakt via setup wizard
- [ ] MFA geconfigureerd voor admin account
- [ ] Email functionaliteit getest

### Stap 5: Functionaliteit Testen
- [ ] Authentication endpoints werken
- [ ] Zero Trust functionaliteit actief
- [ ] Device pairing getest (QR code generatie)
- [ ] FFmpeg beschikbaar: `GET /api/streams/ffmpeg-status`
- [ ] Stream management endpoints werken
- [ ] Transcoding job queue functioneel

## Post-Deployment Verificatie

### Health Checks
- [ ] Application health: `https://api.mallepetrus.nl/actuator/health`
- [ ] Database connectiviteit
- [ ] Redis connectiviteit  
- [ ] Email service connectiviteit

### Security Verificatie
- [ ] HTTPS werkt voor alle domeinen
- [ ] HTTP redirect naar HTTPS
- [ ] JWT tokens worden correct uitgegeven
- [ ] MFA enforcement werkt
- [ ] Zero Trust risk assessment functioneel

### Performance Checks
- [ ] Application start tijd < 90 seconden
- [ ] Memory usage < 1GB onder normale load
- [ ] CPU usage < 50% idle
- [ ] Database connection pool gezond

### Monitoring Setup
- [ ] Prometheus metrics beschikbaar: `/actuator/prometheus`
- [ ] Log aggregatie geconfigureerd
- [ ] Alerting rules ingesteld
- [ ] Backup strategie geïmplementeerd

## Rollback Plan

### Rollback Triggers
- [ ] Application start failures
- [ ] Database migration failures  
- [ ] Critical security issues
- [ ] Performance degradation > 50%

### Rollback Procedure
1. [ ] Stop huidige stack in Portainer
2. [ ] Revert naar vorige image tag
3. [ ] Database rollback indien nodig
4. [ ] Restart stack met vorige configuratie
5. [ ] Verify rollback success

## Production Monitoring

### Daily Checks
- [ ] Application health status
- [ ] Error logs review
- [ ] Performance metrics
- [ ] Security events review

### Weekly Checks  
- [ ] Database backup verification
- [ ] Transcoded files cleanup
- [ ] Security updates check
- [ ] Performance trend analysis

### Monthly Checks
- [ ] SSL certificate renewal
- [ ] Dependency updates
- [ ] Security audit
- [ ] Capacity planning review

## Emergency Contacts & Procedures

### Critical Issues
- **Database down**: Check `jiptv-databases` stack
- **Application crash**: Check logs in Portainer
- **SSL issues**: Check Nginx Proxy Manager
- **Performance issues**: Check resource usage

### Escalation Path
1. Check application logs
2. Check infrastructure status
3. Review recent changes
4. Implement rollback if needed
5. Document incident

## Success Criteria

### Deployment Success
- [ ] All health checks pass
- [ ] Admin can login and access all features
- [ ] Email notifications work
- [ ] Device pairing functional
- [ ] Stream management operational
- [ ] Transcoding jobs can be created

### Performance Targets
- [ ] Application response time < 500ms
- [ ] Database query time < 100ms
- [ ] Memory usage stable < 1.5GB
- [ ] CPU usage < 70% under load
- [ ] Transcoding jobs complete successfully

## Sign-off

**Deployment Completed By:** ________________  
**Date:** ________________  
**Version Deployed:** ________________  
**Rollback Plan Verified:** ☐ Yes ☐ No  
**Monitoring Configured:** ☐ Yes ☐ No  
**Documentation Updated:** ☐ Yes ☐ No  

**Production Ready:** ☐ Yes ☐ No

---

*Deze checklist moet volledig afgewerkt zijn voordat de applicatie als production-ready wordt beschouwd.*