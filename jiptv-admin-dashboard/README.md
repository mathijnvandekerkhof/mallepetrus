# JIPTV Admin Dashboard

Modern web-based administration panel for the JIPTV streaming service, built with Next.js 14, TypeScript, and Ant Design.

## Features

- 🔐 **Secure Authentication** - JWT + MFA integration with backend
- 📊 **Dashboard Overview** - System statistics and health monitoring
- 👥 **User Management** - Invite users, manage accounts and permissions
- 📱 **Device Management** - WebOS TV device pairing and monitoring
- 🎬 **Stream Management** - IPTV stream configuration and analytics
- 🛡️ **Zero Trust Security** - Risk assessment and security event monitoring
- 🎨 **Modern UI** - Professional interface with Ant Design components

## Tech Stack

- **Framework**: Next.js 14 with App Router
- **Language**: TypeScript
- **UI Library**: Ant Design 5
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **API Client**: Axios with React Query
- **Authentication**: JWT tokens with HTTP-only cookies

## Development Setup

### Prerequisites

- Node.js 18+ 
- npm or yarn
- JIPTV Backend running on `localhost:8080`

### Local Development

1. **Clone and setup**:
   ```bash
   cd jiptv-admin-dashboard
   npm install
   ```

2. **Environment configuration**:
   ```bash
   cp .env.example .env.local
   # Edit .env.local with your API URL
   ```

3. **Start development server**:
   ```bash
   npm run dev
   ```

4. **Access the application**:
   - Open http://localhost:3000
   - Login with your JIPTV admin credentials

### Docker Development

```bash
# Build and run with Docker Compose
docker-compose up --build

# Access at http://localhost:3000
```

## 🚀 Quick Deployment

### First Time Setup (VPS)
```bash
ssh user@your-vps
cd /opt/docker/mallepetrus
git pull origin main
./setup-scripts.sh
```

### Update Backend
```bash
./update-backend.sh
# Then restart 'jiptv-app' in Portainer
```

### Update Admin Dashboard
```bash
./update-admin.sh
# Then restart 'jiptv-admin' in Portainer
```

That's it! Just 2 simple commands.

### Environment Variables

#### Development
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NODE_ENV=development
```

#### Production
```env
NEXT_PUBLIC_API_URL=https://api.mallepetrus.nl
NODE_ENV=production
```

## Project Structure

```
jiptv-admin-dashboard/
├── src/
│   ├── app/                 # Next.js App Router pages
│   │   ├── login/          # Authentication pages
│   │   ├── dashboard/      # Main dashboard
│   │   └── layout.tsx      # Root layout
│   ├── components/         # Reusable UI components
│   ├── lib/               # Utilities and API client
│   ├── store/             # Zustand state management
│   └── types/             # TypeScript type definitions
├── public/                # Static assets
├── Dockerfile            # Production container
├── docker-compose.yml    # Local development
└── portainer-stack.yml   # VPS deployment
```

## API Integration

The dashboard integrates with the JIPTV backend API:

- **Authentication**: `/api/auth/*`
- **User Management**: `/api/invitations/*`
- **Device Management**: `/api/device-pairing/*`
- **Stream Management**: `/api/streams/*`
- **Zero Trust**: `/api/zero-trust/*`
- **Health Monitoring**: `/api/actuator/health`

## Security Features

- JWT token authentication with automatic refresh
- HTTP-only cookies for token storage
- MFA (TOTP) integration
- Automatic logout on token expiration
- CSRF protection
- Secure headers and HTTPS enforcement

## Development Commands

```bash
# Development
npm run dev          # Start development server
npm run build        # Build for production
npm run start        # Start production server
npm run lint         # Run ESLint
npm run type-check   # TypeScript type checking

# Docker
docker build -t jiptv-admin:latest .
docker-compose up --build
```

## Deployment Checklist

- [ ] Backend API is running and accessible
- [ ] Environment variables configured
- [ ] Docker image built and tagged
- [ ] Portainer stack deployed
- [ ] Nginx Proxy Manager configured
- [ ] SSL certificate active
- [ ] Domain DNS pointing to VPS
- [ ] Health check passing

## Troubleshooting

### Common Issues

1. **API Connection Failed**:
   - Check `NEXT_PUBLIC_API_URL` environment variable
   - Verify backend is running and accessible
   - Check network connectivity between containers

2. **Authentication Issues**:
   - Verify JWT secret matches backend
   - Check cookie settings and domain
   - Ensure HTTPS in production

3. **Build Failures**:
   - Clear `.next` directory: `rm -rf .next`
   - Reinstall dependencies: `rm -rf node_modules && npm install`
   - Check TypeScript errors: `npm run type-check`

### Logs and Debugging

```bash
# Container logs
docker logs jiptv-admin

# Development logs
npm run dev

# Production build logs
npm run build
```

## Contributing

1. Follow TypeScript strict mode
2. Use Ant Design components consistently
3. Implement proper error handling
4. Add loading states for async operations
5. Follow the established project structure

## License

Private project for JIPTV streaming service.