export interface User {
  id: string
  email: string
  role: 'ADMIN' | 'USER'
  mfaEnabled: boolean
  createdAt: string
  lastLogin?: string
}

export interface LoginRequest {
  email: string
  password: string
  mfaCode?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: User
  requiresMfa: boolean
}

export interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
}