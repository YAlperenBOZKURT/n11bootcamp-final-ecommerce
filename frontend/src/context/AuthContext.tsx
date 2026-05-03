/**
 * AuthContext - global authentication state
 *
 * Strategy: "show immediately, validate in the background"
 *
 * 1. On page load, if localStorage has `user_cache`, we hydrate state immediately
 *    (`status` starts as 'idle', so the user does not see a skeleton)
 * 2. In the background, we silently call /auth/refresh + /api/users/me
 *    - Success -> update cache and sync user state with fresh data
 *    - Failure -> clear cache and set user to null (if there is no valid session)
 * 3. After login/register, user is written to both state and cache
 * 4. After logout, cache is removed
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { authService, type AuthUser, type LoginRequest, type RegisterRequest } from '../services/authService'
import { ApiError } from '../lib/apiClient'

const CACHE_KEY = 'user_cache'
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/+$/, '') ?? ''

function readCache(): AuthUser | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    return raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    return null
  }
}

function writeCache(user: AuthUser) {
  localStorage.setItem(CACHE_KEY, JSON.stringify(user))
}

function clearCache() {
  localStorage.removeItem(CACHE_KEY)
}

interface AuthContextValue {
  user: AuthUser | null
  status: 'loading' | 'idle'
  login: (payload: LoginRequest) => Promise<void>
  register: (payload: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const cached = readCache()
  const [user, setUser] = useState<AuthUser | null>(cached)
  // If cache exists, start in 'idle' immediately to avoid showing a skeleton
  const [status, setStatus] = useState<'loading' | 'idle'>(cached ? 'idle' : 'loading')
  const sessionChecked = useRef(false)

  useEffect(() => {
    if (sessionChecked.current) return
    sessionChecked.current = true

    ;(async () => {
      try {
        // Use direct fetch so token refresh does not enter apiClient's
        // silentRefresh/auth:expired loop
        await fetch(`${API_BASE_URL}/api/users/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
        }).catch(() => {})

        const meRes = await fetch(`${API_BASE_URL}/api/users/me`, { credentials: 'include' })
        if (meRes.ok) {
          const envelope = (await meRes.json()) as { data: AuthUser }
          writeCache(envelope.data)
          setUser(envelope.data)
        } else {
          // Server does not recognize the session -> clear cache
          clearCache()
          setUser(null)
        }
      } catch {
        // Network error -> keep showing cached user if available
      } finally {
        setStatus('idle')
      }
    })()
  }, [])

  // apiClient 401 -> silentRefresh fails -> auth:expired
  useEffect(() => {
    const handler = () => {
      clearCache()
      setUser(null)
    }
    window.addEventListener('auth:expired', handler)
    return () => window.removeEventListener('auth:expired', handler)
  }, [])

  const login = useCallback(async (payload: LoginRequest) => {
    await authService.login(payload)
    const profile = await authService.me()
    writeCache(profile)
    setUser(profile)
  }, [])

  const register = useCallback(async (payload: RegisterRequest) => {
    await authService.register(payload)
    const profile = await authService.me()
    writeCache(profile)
    setUser(profile)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authService.logout()
    } catch (err) {
      if (!(err instanceof ApiError)) throw err
    } finally {
      clearCache()
      setUser(null)
    }
  }, [])

  return (
    <AuthContext.Provider value={{ user, status, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an <AuthProvider>')
  }
  return ctx
}

