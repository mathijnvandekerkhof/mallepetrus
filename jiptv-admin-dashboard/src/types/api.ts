export interface ApiResponse<T = any> {
  data: T
  message?: string
  success: boolean
}

export interface ApiError {
  message: string
  status: number
  code?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// Dashboard Statistics
export interface DashboardStats {
  totalUsers: number
  activeUsers: number
  totalDevices: number
  activeDevices: number
  totalStreams: number
  activeStreams: number
  securityEvents: number
  systemHealth: 'HEALTHY' | 'WARNING' | 'ERROR'
}

// Stream Management
export interface StreamSource {
  id: string
  name: string
  description?: string
  sourceUrl: string
  sourceType: 'FILE' | 'HTTP' | 'RTSP'
  fileSize?: number
  fileHash?: string
  duration?: number
  createdAt: string
  updatedAt: string
  tracks: StreamTrack[]
}

export interface StreamTrack {
  id: string
  streamSourceId: string
  trackType: 'VIDEO' | 'AUDIO' | 'SUBTITLE'
  trackIndex: number
  codec: string
  language?: string
  title?: string
  bitrate?: number
  width?: number
  height?: number
  frameRate?: number
  channels?: number
  sampleRate?: number
  webosCompatible: boolean
  transcodingRequired: boolean
}

// Device Management
export interface TvDevice {
  id: string
  userId: string
  deviceName: string
  macAddressHash: string
  macAddressMasked: string
  isActive: boolean
  lastSeen?: string
  createdAt: string
}

// User Management
export interface UserInvitation {
  id: string
  email: string
  invitationCode: string
  isUsed: boolean
  expiresAt: string
  createdAt: string
  usedAt?: string
}

// Zero Trust
export interface SecurityEvent {
  id: string
  userId?: string
  eventType: string
  riskScore: number
  details: Record<string, any>
  ipAddress: string
  userAgent: string
  createdAt: string
}

export interface DeviceFingerprint {
  id: string
  userId: string
  fingerprint: string
  deviceName: string
  isTrusted: boolean
  lastSeen: string
  createdAt: string
}