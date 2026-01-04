'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Form, Input, Button, Card, Typography, Alert, Space, Divider } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useAuthStore } from '@/store/auth'
import type { LoginRequest } from '@/types/auth'

const { Title, Text } = Typography

export default function LoginPage() {
  const router = useRouter()
  const { login, isLoading } = useAuthStore()
  const [form] = Form.useForm()
  const [error, setError] = useState<string>('')
  const [requiresMfa, setRequiresMfa] = useState(false)
  const [loginData, setLoginData] = useState<LoginRequest | null>(null)

  const handleLogin = async (values: LoginRequest) => {
    setError('')
    try {
      const response = await login(values)
      
      if (response.requiresMfa) {
        setRequiresMfa(true)
        setLoginData(values)
      } else {
        // Login successful, redirect to dashboard
        router.push('/dashboard')
      }
    } catch (err: any) {
      setError(err.message || 'Login failed')
    }
  }

  const handleMfaSubmit = async (values: { mfaCode: string }) => {
    if (!loginData) return

    setError('')
    try {
      await login({
        ...loginData,
        mfaCode: values.mfaCode,
      })
      router.push('/dashboard')
    } catch (err: any) {
      setError(err.message || 'MFA verification failed')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <Card className="w-full max-w-md shadow-lg">
        <div className="text-center mb-8">
          <Title level={2} className="mb-2">
            🎬 JIPTV Admin
          </Title>
          <Text type="secondary">
            IPTV Streaming Service Administration
          </Text>
        </div>

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            className="mb-4"
            closable
            onClose={() => setError('')}
          />
        )}

        {!requiresMfa ? (
          <Form
            form={form}
            name="login"
            onFinish={handleLogin}
            layout="vertical"
            size="large"
          >
            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Please enter your email' },
                { type: 'email', message: 'Please enter a valid email' },
              ]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="admin@jiptv.local"
                autoComplete="email"
              />
            </Form.Item>

            <Form.Item
              name="password"
              label="Password"
              rules={[{ required: true, message: 'Please enter your password' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Password"
                autoComplete="current-password"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={isLoading}
                block
                size="large"
              >
                Sign In
              </Button>
            </Form.Item>
          </Form>
        ) : (
          <Form
            name="mfa"
            onFinish={handleMfaSubmit}
            layout="vertical"
            size="large"
          >
            <div className="text-center mb-4">
              <SafetyOutlined className="text-4xl text-blue-500 mb-2" />
              <Title level={4}>Two-Factor Authentication</Title>
              <Text type="secondary">
                Enter the 6-digit code from your authenticator app
              </Text>
            </div>

            <Form.Item
              name="mfaCode"
              label="Authentication Code"
              rules={[
                { required: true, message: 'Please enter your MFA code' },
                { len: 6, message: 'MFA code must be 6 digits' },
              ]}
            >
              <Input
                placeholder="000000"
                maxLength={6}
                className="text-center text-lg tracking-widest"
                autoComplete="one-time-code"
              />
            </Form.Item>

            <Space direction="vertical" className="w-full">
              <Button
                type="primary"
                htmlType="submit"
                loading={isLoading}
                block
                size="large"
              >
                Verify & Sign In
              </Button>
              
              <Button
                type="link"
                onClick={() => {
                  setRequiresMfa(false)
                  setLoginData(null)
                  form.resetFields()
                }}
                block
              >
                Back to Login
              </Button>
            </Space>
          </Form>
        )}

        <Divider />

        <div className="text-center">
          <Text type="secondary" className="text-sm">
            JIPTV Admin Dashboard v1.0.0
          </Text>
        </div>
      </Card>
    </div>
  )
}