'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Spin, Card, Typography } from 'antd'
import { apiClient, API_ENDPOINTS } from '@/lib/api'
import type { SetupStatus } from '@/types/api'

const { Text } = Typography

export default function HomePage() {
  const router = useRouter()
  const [checking, setChecking] = useState(true)
  const [status, setStatus] = useState<string>('Checking system status...')

  useEffect(() => {
    checkSetupAndRedirect()
  }, [router])

  const checkSetupAndRedirect = async () => {
    try {
      setStatus('Checking if setup is required...')
      console.log('🔍 Checking setup status...')
      console.log('🌐 API Base URL:', process.env.NEXT_PUBLIC_API_URL)
      
      // Check if setup is needed
      const setupStatus = await apiClient.get<SetupStatus>(API_ENDPOINTS.SETUP.STATUS)
      console.log('📊 Setup status:', setupStatus)
      
      if (setupStatus.needsSetup) {
        setStatus('Setup required. Redirecting to setup wizard...')
        console.log('🚀 Setup needed, redirecting to /setup')
        // Setup needed, redirect to setup wizard
        setTimeout(() => {
          router.push('/setup')
        }, 1500)
      } else {
        setStatus('System configured. Redirecting to login...')
        console.log('✅ Setup complete, redirecting to /login')
        // Setup complete, redirect to login
        setTimeout(() => {
          router.push('/login')
        }, 1500)
      }
    } catch (error: any) {
      console.error('❌ Setup check failed:', error)
      console.error('Error details:', {
        message: error.message,
        status: error.status,
        response: error.response?.data
      })
      
      // If API is not available or returns error, assume setup is needed
      setStatus('Unable to connect to backend. Redirecting to setup wizard...')
      setTimeout(() => {
        router.push('/setup')
      }, 2000)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <Card className="w-full max-w-md shadow-lg text-center">
        <div className="mb-6">
          <div className="text-6xl mb-4">🎬</div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">
            JIPTV Admin Dashboard
          </h1>
          <Text type="secondary">
            IPTV Streaming Service Administration
          </Text>
        </div>
        
        <div className="flex flex-col items-center space-y-4">
          <Spin size="large" />
          <Text className="text-gray-600">
            {status}
          </Text>
        </div>
      </Card>
    </div>
  )
}