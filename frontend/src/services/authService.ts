
import { apiClient } from '../lib/apiClient'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  phoneNumber?: string | null
}

export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN' | string

export interface AuthUser {
  id: number
  email: string
  firstName: string
  lastName: string
  role: UserRole
}

/** GET /api/users/me — tam profil verisi */
export interface UserProfile extends AuthUser {
  id: number
  phoneNumber: string | null
  status: string
  createdAt: string
}

export interface UpdateProfileRequest {
  firstName: string
  lastName: string
  phoneNumber?: string | null
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface AddressRequest {
  title: string
  recipientName: string
  recipientPhone: string
  city: string
  district: string
  neighborhood?: string
  addressLine: string
  zipCode?: string
}

export interface AddressResponse {
  id: number
  title: string
  recipientName: string
  recipientPhone: string
  city: string
  district: string
  neighborhood: string | null
  addressLine: string
  zipCode: string | null
  isDefault: boolean
}

export const authService = {
  /**
   * Authenticate an existing user.
   * On success the server sets access_token and refresh_token as HttpOnly cookies.
   */
  async login(payload: LoginRequest): Promise<AuthUser> {
    return apiClient.post<AuthUser>('/api/users/auth/login', payload)
  },

  /**
   * Create a new account.
   * On success the server sets auth cookies just like login.
   */
  async register(payload: RegisterRequest): Promise<AuthUser> {
    return apiClient.post<AuthUser>('/api/users/auth/register', payload)
  },

  /**
   * Invalidate tokens and clear auth cookies server-side.
   */
  async logout(): Promise<void> {
    return apiClient.post<void>('/api/users/auth/logout')
  },

  /**
   * Exchange the refresh_token cookie for a new access_token cookie.
   * Called automatically by apiClient on 401; not typically called directly.
   */
  async refresh(): Promise<void> {
    return apiClient.post<void>('/api/users/auth/refresh')
  },

  /**
   * Fetch the current authenticated user's profile.
   * GET /api/users/me — requires a valid access_token cookie.
   */
  async me(): Promise<AuthUser> {
    return apiClient.get<AuthUser>('/api/users/me')
  },

  /** Full profile including phoneNumber, status, createdAt. */
  async getProfile(): Promise<UserProfile> {
    return apiClient.get<UserProfile>('/api/users/me')
  },

  /** Update firstName, lastName, phoneNumber. */
  async updateProfile(payload: UpdateProfileRequest): Promise<UserProfile> {
    return apiClient.put<UserProfile>('/api/users/me', payload)
  },

  /** Change password. */
  async changePassword(payload: ChangePasswordRequest): Promise<void> {
    return apiClient.put<void>('/api/users/me/password', payload)
  },

  /** Send a password-reset token to the given email. Always returns 200. */
  async forgotPassword(payload: ForgotPasswordRequest): Promise<void> {
    return apiClient.post<void>('/api/users/auth/forgot-password', payload)
  },

  /** Reset password using the token received by email. */
  async resetPassword(payload: ResetPasswordRequest): Promise<void> {
    return apiClient.post<void>('/api/users/auth/reset-password', payload)
  },

  async getAddresses(): Promise<AddressResponse[]> {
    return apiClient.get<AddressResponse[]>('/api/users/me/addresses')
  },

  async createAddress(payload: AddressRequest): Promise<AddressResponse> {
    return apiClient.post<AddressResponse>('/api/users/me/addresses', payload)
  },

  async updateAddress(id: number, payload: AddressRequest): Promise<AddressResponse> {
    return apiClient.put<AddressResponse>(`/api/users/me/addresses/${id}`, payload)
  },

  async deleteAddress(id: number): Promise<void> {
    return apiClient.delete<void>(`/api/users/me/addresses/${id}`)
  },

  async setDefaultAddress(id: number): Promise<AddressResponse> {
    return apiClient.put<AddressResponse>(`/api/users/me/addresses/${id}/default`)
  },
}

