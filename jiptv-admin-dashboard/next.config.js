/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  experimental: {
    serverComponentsExternalPackages: []
  },
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'https://api.mallepetrus.nl',
    NEXT_PUBLIC_APP_NAME: 'JIPTV Admin Dashboard'
  },
  // Optimize build performance
  swcMinify: true,
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production',
  },
  // Disable source maps in production for faster builds
  productionBrowserSourceMaps: false,
  // Optimize images
  images: {
    unoptimized: true
  },
  // Remove rewrites that were causing issues
  async rewrites() {
    return []
  }
}

module.exports = nextConfig