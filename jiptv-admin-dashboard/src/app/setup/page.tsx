'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { 
  Card, 
  Form, 
  Input, 
  Button, 
  Typography, 
  Alert, 
  Steps, 
  Space,
  Divider,
  Spin
} from 'antd'
import { 
  UserOutlined, 
  LockOutlined, 
  CheckCircleOutlined,
  RocketOutlined 
} from '@ant-design/icons'
import { apiClient, API_ENDPOINTS } from '@/lib/api'
import type { SetupStatus, SetupRequest, SetupResponse } from '@/types/api'

const { Title, Text, Paragraph } = Typography

export default function SetupWizardPage() {
  const router = useRouter()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [checking, setChecking] = useState(true)
  const [currentStep, setCurrentStep] = useState(0)
  const [error, setError] = useState<string>('')
  const [setupStatus, setSetupStatus] = useState<SetupStatus | null>(null)

  // Check if setup is needed
  useEffect(() => {
    checkSetupStatus()
  }, [])

  const checkSetupStatus = async () => {
    try {
      setChecking(true)
      const status = await apiClient.get<SetupStatus>(API_ENDPOINTS.SETUP.STATUS)
      setSetupStatus(status)
      
      if (!status.needsSetup) {
        // Setup already completed, redirect to login
        router.push('/login')
      }
    } catch (err: any) {
      setError('Failed to check setup status. Please try again.')
    } finally {
      setChecking(false)
    }
  }

  const handleSetup = async (values: SetupRequest) => {
    setError('')
    setLoading(true)

    try {
      const response = await apiClient.post<SetupResponse>(API_ENDPOINTS.SETUP.INITIALIZE, values)
      
      // Setup successful
      setCurrentStep(2)
      
      // Redirect to login after 3 seconds
      setTimeout(() => {
        router.push('/login')
      }, 3000)

    } catch (err: any) {
      setError(err.message || 'Setup failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (checking) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
        <Card className="w-full max-w-md shadow-lg text-center">
          <Spin size="large" />
          <div className="mt-4">
            <Text>Checking setup status...</Text>
          </div>
        </Card>
      </div>
    )
  }

  if (!setupStatus?.needsSetup) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
        <Card className="w-full max-w-md shadow-lg text-center">
          <Spin size="large" />
          <div className="mt-4">
            <Text>Redirecting to login...</Text>
          </div>
        </Card>
      </div>
    )
  }

  const steps = [
    {
      title: 'Welcome',
      icon: <RocketOutlined />
    },
    {
      title: 'Create Admin',
      icon: <UserOutlined />
    },
    {
      title: 'Complete',
      icon: <CheckCircleOutlined />
    }
  ]

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <Card className="w-full max-w-2xl shadow-lg">
        <div className="text-center mb-8">
          <Title level={2} className="mb-2">
            🎬 JIPTV Setup Wizard
          </Title>
          <Text type="secondary">
            Welcome to JIPTV Admin Dashboard Setup
          </Text>
        </div>

        <Steps current={currentStep} items={steps} className="mb-8" />

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            className="mb-6"
            closable
            onClose={() => setError('')}
          />
        )}

        {currentStep === 0 && (
          <div className="text-center">
            <Title level={3}>Welcome to JIPTV!</Title>
            <Paragraph className="text-lg mb-6">
              This is your first time accessing the JIPTV Admin Dashboard. 
              Let's set up your administrator account to get started.
            </Paragraph>
            
            <div className="bg-blue-50 p-6 rounded-lg mb-6">
              <Title level={4} className="text-blue-800 mb-4">
                What you'll get:
              </Title>
              <div className="text-left space-y-2">
                <div>✅ <strong>User Management</strong> - Invite and manage users</div>
                <div>✅ <strong>Device Monitoring</strong> - Track WebOS TV devices</div>
                <div>✅ <strong>Stream Management</strong> - Manage IPTV streams</div>
                <div>✅ <strong>Security Dashboard</strong> - Zero Trust monitoring</div>
                <div>✅ <strong>Multi-Factor Authentication</strong> - Enhanced security</div>
              </div>
            </div>

            <Button 
              type="primary" 
              size="large" 
              onClick={() => setCurrentStep(1)}
              className="px-8"
            >
              Get Started
            </Button>
          </div>
        )}

        {currentStep === 1 && (
          <div>
            <Title level={3} className="text-center mb-6">
              Create Administrator Account
            </Title>
            
            <Form
              form={form}
              name="setup"
              onFinish={handleSetup}
              layout="vertical"
              size="large"
            >
              <Form.Item
                name="email"
                label="Administrator Email"
                rules={[
                  { required: true, message: 'Please enter your email address' },
                  { type: 'email', message: 'Please enter a valid email address' },
                ]}
              >
                <Input
                  prefix={<UserOutlined />}
                  placeholder="admin@mallepetrus.nl"
                  autoComplete="email"
                />
              </Form.Item>

              <Form.Item
                name="password"
                label="Password"
                rules={[
                  { required: true, message: 'Please enter a password' },
                  { min: 8, message: 'Password must be at least 8 characters' },
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="Enter a strong password"
                  autoComplete="new-password"
                />
              </Form.Item>

              <Form.Item
                name="confirmPassword"
                label="Confirm Password"
                dependencies={['password']}
                rules={[
                  { required: true, message: 'Please confirm your password' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve()
                      }
                      return Promise.reject(new Error('Passwords do not match'))
                    },
                  }),
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="Confirm your password"
                  autoComplete="new-password"
                />
              </Form.Item>

              <div className="bg-yellow-50 p-4 rounded-lg mb-6">
                <Text className="text-sm text-yellow-800">
                  <strong>Security Note:</strong> This administrator account will have full access 
                  to the JIPTV system. Choose a strong password and enable MFA after setup.
                </Text>
              </div>

              <Space direction="vertical" className="w-full">
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  block
                  size="large"
                >
                  Create Administrator Account
                </Button>
                
                <Button
                  type="link"
                  onClick={() => setCurrentStep(0)}
                  block
                  disabled={loading}
                >
                  Back
                </Button>
              </Space>
            </Form>
          </div>
        )}

        {currentStep === 2 && (
          <div className="text-center">
            <div className="mb-6">
              <CheckCircleOutlined className="text-6xl text-green-500 mb-4" />
              <Title level={3} className="text-green-600">
                Setup Complete!
              </Title>
            </div>
            
            <Paragraph className="text-lg mb-6">
              Your JIPTV administrator account has been created successfully. 
              You will be redirected to the login page shortly.
            </Paragraph>

            <div className="bg-green-50 p-6 rounded-lg mb-6">
              <Title level={4} className="text-green-800 mb-4">
                Next Steps:
              </Title>
              <div className="text-left space-y-2">
                <div>1. 🔐 <strong>Login</strong> with your new administrator account</div>
                <div>2. 🛡️ <strong>Setup MFA</strong> for enhanced security</div>
                <div>3. 👥 <strong>Invite Users</strong> to your JIPTV system</div>
                <div>4. 📺 <strong>Configure Streams</strong> and manage devices</div>
              </div>
            </div>

            <Button 
              type="primary" 
              size="large" 
              onClick={() => router.push('/login')}
              className="px-8"
            >
              Go to Login
            </Button>
          </div>
        )}

        <Divider />

        <div className="text-center">
          <Text type="secondary" className="text-sm">
            JIPTV Admin Dashboard Setup v1.0.0
          </Text>
        </div>
      </Card>
    </div>
  )
}