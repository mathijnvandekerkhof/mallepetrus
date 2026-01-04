'use client'

import { useState } from 'react'
import { Card, Button, Typography, Space, Alert } from 'antd'
import { apiClient, API_ENDPOINTS } from '@/lib/api'

const { Title, Text, Paragraph } = Typography

export default function DebugPage() {
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<any>(null)
  const [error, setError] = useState<string>('')

  const testSetupStatus = async () => {
    setLoading(true)
    setError('')
    setResult(null)

    try {
      console.log('Testing setup status endpoint...')
      console.log('API Base URL:', process.env.NEXT_PUBLIC_API_URL)
      console.log('Full URL:', `${process.env.NEXT_PUBLIC_API_URL}${API_ENDPOINTS.SETUP.STATUS}`)
      
      const response = await apiClient.get(API_ENDPOINTS.SETUP.STATUS)
      console.log('Setup status response:', response)
      setResult(response)
    } catch (err: any) {
      console.error('Setup status error:', err)
      setError(err.message || 'Unknown error')
    } finally {
      setLoading(false)
    }
  }

  const testDirectFetch = async () => {
    setLoading(true)
    setError('')
    setResult(null)

    try {
      const url = `${process.env.NEXT_PUBLIC_API_URL}/api/setup/status`
      console.log('Testing direct fetch to:', url)
      
      const response = await fetch(url)
      const data = await response.json()
      
      console.log('Direct fetch response:', { status: response.status, data })
      setResult({ status: response.status, data })
    } catch (err: any) {
      console.error('Direct fetch error:', err)
      setError(err.message || 'Unknown error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen p-8 bg-gray-50">
      <Card className="max-w-4xl mx-auto">
        <Title level={2}>🔧 JIPTV Admin Debug</Title>
        
        <div className="mb-6">
          <Title level={4}>Environment Info</Title>
          <div className="bg-gray-100 p-4 rounded">
            <Text code>NEXT_PUBLIC_API_URL: {process.env.NEXT_PUBLIC_API_URL || 'Not set'}</Text><br/>
            <Text code>Setup Status Endpoint: {API_ENDPOINTS.SETUP.STATUS}</Text><br/>
            <Text code>Full URL: {process.env.NEXT_PUBLIC_API_URL}{API_ENDPOINTS.SETUP.STATUS}</Text>
          </div>
        </div>

        <Space direction="vertical" className="w-full">
          <Title level={4}>API Tests</Title>
          
          <Space>
            <Button 
              type="primary" 
              onClick={testSetupStatus} 
              loading={loading}
            >
              Test Setup Status (API Client)
            </Button>
            
            <Button 
              onClick={testDirectFetch} 
              loading={loading}
            >
              Test Direct Fetch
            </Button>
          </Space>

          {error && (
            <Alert
              message="Error"
              description={error}
              type="error"
              showIcon
            />
          )}

          {result && (
            <div>
              <Title level={4}>Result:</Title>
              <div className="bg-gray-100 p-4 rounded">
                <pre>{JSON.stringify(result, null, 2)}</pre>
              </div>
            </div>
          )}
        </Space>
      </Card>
    </div>
  )
}