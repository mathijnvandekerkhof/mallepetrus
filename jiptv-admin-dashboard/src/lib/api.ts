import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { message } from 'antd'
import Cookies from 'js-cookie'
import type { ApiResponse, ApiError } from '@/types/api'

class ApiClient {
  private client: AxiosInstance

  constructor() {
    this.client = axios.create({
      baseURL: process.env.NEXT_PUBLIC_API_URL || 'https://api.mallepetrus.nl',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {
    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config) => {
        const token = Cookies.get('jiptv_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response: AxiosResponse) => {
        return response
      },
      (error) => {
        const apiError: ApiError = {
          message: error.response?.data?.message || error.message || 'An error occurred',
          status: error.response?.status || 500,
          code: error.response?.data?.code,
        }

        // Handle specific error cases
        if (apiError.status === 401) {
          // Unauthorized - clear token and redirect to login
          Cookies.remove('jiptv_token')
          if (typeof window !== 'undefined') {
            window.location.href = '/login'
          }
        } else if (apiError.status >= 500) {
          message.error('Server error occurred. Please try again later.')
        } else if (apiError.status >= 400) {
          message.error(apiError.message)
        }

        return Promise.reject(apiError)
      }
    )
  }

  // Generic request methods
  async get<T>(url: string, params?: any): Promise<T> {
    const response = await this.client.get<ApiResponse<T>>(url, { params })
    return response.data.data
  }

  async post<T>(url: string, data?: any): Promise<T> {
    const response = await this.client.post<ApiResponse<T>>(url, data)
    return response.data.data
  }

  async put<T>(url: string, data?: any): Promise<T> {
    const response = await this.client.put<ApiResponse<T>>(url, data)
    return response.data.data
  }

  async delete<T>(url: string): Promise<T> {
    const response = await this.client.delete<ApiResponse<T>>(url)
    return response.data.data
  }

  // Raw client access for special cases
  getRawClient(): AxiosInstance {
    return this.client
  }
}

export const apiClient = new ApiClient()

// API endpoints
export const API_ENDPOINTS = {
  // Authentication
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh',
    ME: '/auth/me',
  },
  
  // Setup
  SETUP: {
    STATUS: '/setup/status',
    INITIALIZE: '/setup/initialize',
  },

  // User Management
  USERS: {
    LIST: '/users',
    INVITE: '/invitations/invite',
    INVITATIONS: '/invitations',
  },

  // Device Management
  DEVICES: {
    LIST: '/device-pairing/devices',
    GENERATE_QR: '/device-pairing/generate-qr',
    PAIR: '/device-pairing/pair',
  },

  // Stream Management
  STREAMS: {
    LIST: '/streams',
    CREATE: '/streams',
    ANALYZE: (id: string) => `/streams/${id}/analyze`,
    TRACKS: (id: string) => `/streams/${id}/tracks`,
    STATISTICS: '/streams/statistics',
  },

  // Zero Trust
  ZERO_TRUST: {
    DASHBOARD: '/zero-trust/dashboard',
    DEVICES: '/zero-trust/devices',
    SESSIONS: '/zero-trust/sessions',
    EVENTS: '/zero-trust/events',
  },

  // MFA
  MFA: {
    SETUP: '/mfa/setup',
    ENABLE: '/mfa/enable',
    DISABLE: '/mfa/disable',
  },

  // Health Check
  HEALTH: '/actuator/health',
}