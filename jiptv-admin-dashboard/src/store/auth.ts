import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import Cookies from 'js-cookie'
import { apiClient, API_ENDPOINTS } from '@/lib/api'
import type { User, LoginRequest, LoginResponse, AuthState } from '@/types/auth'

interface AuthStore extends AuthState {
  login: (credentials: LoginRequest) => Promise<LoginResponse>
  logout: () => void
  refreshToken: () => Promise<void>
  checkAuth: () => Promise<void>
  setUser: (user: User) => void
  setToken: (token: string) => void
  setLoading: (loading: boolean) => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,

      login: async (credentials: LoginRequest): Promise<LoginResponse> => {
        set({ isLoading: true })
        try {
          const response = await apiClient.post<LoginResponse>(
            API_ENDPOINTS.AUTH.LOGIN,
            credentials
          )

          if (!response.requiresMfa) {
            // Login successful without MFA
            set({
              user: response.user,
              token: response.accessToken,
              isAuthenticated: true,
              isLoading: false,
            })

            // Store token in cookie
            Cookies.set('jiptv_token', response.accessToken, {
              expires: 7, // 7 days
              secure: process.env.NODE_ENV === 'production',
              sameSite: 'strict',
            })
          } else {
            // MFA required
            set({ isLoading: false })
          }

          return response
        } catch (error) {
          set({ isLoading: false })
          throw error
        }
      },

      logout: () => {
        // Call logout endpoint (fire and forget)
        apiClient.post(API_ENDPOINTS.AUTH.LOGOUT).catch(() => {
          // Ignore errors on logout
        })

        // Clear state and cookies
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          isLoading: false,
        })

        Cookies.remove('jiptv_token')

        // Redirect to login
        if (typeof window !== 'undefined') {
          window.location.href = '/login'
        }
      },

      refreshToken: async () => {
        try {
          const response = await apiClient.post<LoginResponse>(
            API_ENDPOINTS.AUTH.REFRESH
          )

          set({
            user: response.user,
            token: response.accessToken,
            isAuthenticated: true,
          })

          Cookies.set('jiptv_token', response.accessToken, {
            expires: 7,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
          })
        } catch (error) {
          // Refresh failed, logout user
          get().logout()
        }
      },

      checkAuth: async () => {
        const token = Cookies.get('jiptv_token')
        if (!token) {
          set({ isAuthenticated: false, isLoading: false })
          return
        }

        set({ isLoading: true })
        try {
          const user = await apiClient.get<User>(API_ENDPOINTS.AUTH.ME)
          set({
            user,
            token,
            isAuthenticated: true,
            isLoading: false,
          })
        } catch (error) {
          // Token invalid, clear auth state
          set({
            user: null,
            token: null,
            isAuthenticated: false,
            isLoading: false,
          })
          Cookies.remove('jiptv_token')
        }
      },

      setUser: (user: User) => set({ user }),
      setToken: (token: string) => set({ token }),
      setLoading: (loading: boolean) => set({ isLoading: loading }),
    }),
    {
      name: 'jiptv-auth',
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)