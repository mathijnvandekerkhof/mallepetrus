// Generic API response wrapper
export interface ApiResponse<T> {
  data: T
  message?: string
  success: boolean
}

// API error type
export interface ApiError {
  message: string
  status: number
  code?: string
}

// Setup types
export interface SetupStatus {
  needsSetup: boolean
  totalUsers: number
  adminUsers: number
}

export interface SetupRequest {
  email: string
  password: string
  confirmPassword: string
}

export interface SetupResponse {
  message: string
  adminEmail: string
  adminId: number
}