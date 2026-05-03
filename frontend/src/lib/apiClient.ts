
import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'

export interface ApiEnvelope<T> {
  data: T
  message: string | null
  success: boolean
}

export class ApiError extends Error {
  readonly status: number
  readonly body?: unknown

  constructor(status: number, message: string, body?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

let isRefreshing = false
let refreshSubscribers: Array<(ok: boolean) => void> = []
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/+$/, '') ?? ''

function notifySubscribers(ok: boolean) {
  refreshSubscribers.forEach((fn) => fn(ok))
  refreshSubscribers = []
}

async function silentRefresh(): Promise<boolean> {
  if (isRefreshing) {
    return new Promise((resolve) => refreshSubscribers.push(resolve))
  }

  isRefreshing = true
  try {
    // Use raw axios to avoid entering the interceptor loop (not the instance)
    await axios.post(`${API_BASE_URL}/api/users/auth/refresh`, null, { withCredentials: true })
    notifySubscribers(true)
    return true
  } catch {
    notifySubscribers(false)
    return false
  } finally {
    isRefreshing = false
  }
}

const instance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean }
const AUTH_ENDPOINT_PREFIX = '/api/users/auth/'

function shouldAttemptRefresh(status: number, config?: RetryConfig): boolean {
  if (status !== 401 || !config || config._retry) return false

  const url = config.url ?? ''
  // A 401 can be expected for auth endpoints such as login/register
  // In this case, skip refresh flow and show the backend message directly to the user
  if (url.includes(AUTH_ENDPOINT_PREFIX)) return false

  return true
}

instance.interceptors.response.use(
  (response) => {
    // Unwrap ApiEnvelope<T> to T
    const body = response.data
    if (body !== null && typeof body === 'object' && 'data' in body && 'success' in body) {
      return (body as ApiEnvelope<unknown>).data as never
    }
    return body
  },
  async (error) => {
    const config = error.config as RetryConfig | undefined
    const status: number = error.response?.status ?? 0

    if (shouldAttemptRefresh(status, config) && config) {
      config._retry = true
      const refreshed = await silentRefresh()
      if (refreshed) {
        return instance(config)
      }
      window.dispatchEvent(new CustomEvent('auth:expired'))
      throw new ApiError(401, 'Oturum süresi doldu. Lütfen tekrar giriş yapın.')
    }

    const body: unknown = error.response?.data
    let message = `HTTP ${status || 'unknown'}`
    if (body && typeof body === 'object' && 'message' in body) {
      message = (body as { message: string }).message
    }
    throw new ApiError(status, message, body)
  },
)

export const apiClient = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get<never, T>(url, config)
  },

  post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post<never, T>(url, body, config)
  },

  put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put<never, T>(url, body, config)
  },

  patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.patch<never, T>(url, body, config)
  },

  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete<never, T>(url, config)
  },
}



