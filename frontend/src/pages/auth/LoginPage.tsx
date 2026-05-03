import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Eye, EyeOff, Lock, Mail, UserRound } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { useAuth } from '../../context/AuthContext'
import { useCart } from '../../context/CartContext'
import { ApiError } from '../../lib/apiClient'

export default function LoginPage() {
  const { login } = useAuth()
  const { syncGuestCart } = useCart()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)

    if (!email.trim() || !password.trim()) {
      setError('E-posta ve şifre alanları zorunludur.')
      return
    }

    setIsSubmitting(true)
    try {
      await login({ email: email.trim(), password })
      await syncGuestCart()
      navigate('/products', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Sunucuya bağlanırken hata oluştu.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 lg:flex lg:h-screen lg:flex-col lg:overflow-hidden">
      <SiteHeader />

      <main className="grid w-full grid-cols-1 lg:flex-1 lg:min-h-0 lg:content-center lg:justify-center lg:px-0">
        <section className="flex items-center justify-center p-4 sm:p-8 lg:p-0">
          <div className="w-full max-w-xl rounded-none bg-white p-6 shadow-none sm:rounded-3xl sm:p-8 sm:shadow-sm lg:max-h-[calc(100vh-260px)] lg:overflow-y-auto lg:p-8">
            <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-violet-100 text-violet-600">
              <UserRound size={22} />
            </div>
            <h2 className="text-center text-3xl font-semibold tracking-tight">Giriş Yap</h2>
            <p className="mt-2 text-center text-sm text-slate-500">
              Hesabınıza giriş yaparak devam edin.
            </p>

            <form className="mt-6 space-y-4" onSubmit={onSubmit}>
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">E-posta adresi</label>
                <div className="relative">
                  <Mail className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="örnek@mail.com"
                    className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-3 text-slate-900 outline-none ring-violet-500 focus:ring-2"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Şifre</label>
                <div className="relative">
                  <Lock className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Şifrenizi girin"
                    className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-11 text-slate-900 outline-none ring-violet-500 focus:ring-2"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((s) => !s)}
                    aria-label={showPassword ? 'Şifreyi gizle' : 'Şifreyi göster'}
                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between gap-4">
                <label className="flex items-center gap-2 text-sm text-slate-600">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 text-violet-600 focus:ring-violet-500"
                  />
                  Beni hatırla
                </label>
                <Link to="/forgot-password" className="text-sm font-medium text-violet-600 hover:underline">
                  Şifremi unuttum
                </Link>
              </div>

              {error && <p className="text-sm font-medium text-rose-600">{error}</p>}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-md bg-violet-600 px-4 py-2.5 font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {isSubmitting ? 'Giriş yapılıyor...' : 'Giriş Yap'}
              </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-600">
              Hesabın yok mu?{' '}
              <Link to="/register" className="font-medium text-violet-600 hover:underline">
                Kayıt Ol
              </Link>
            </p>
          </div>
        </section>
      </main>

      <footer className="hidden bg-slate-900 lg:block">
        <div className="mx-auto flex h-14 w-full max-w-[1320px] items-center justify-between px-6 text-xs text-slate-300">
          <p>© 2026 Bozkurt. Tüm hakları saklıdır.</p>
          <div className="flex items-center gap-5">
            <a href="#" className="hover:text-white">Hakkımızda</a>
            <a href="#" className="hover:text-white">Kariyer</a>
            <a href="#" className="hover:text-white">Gizlilik</a>
            <a href="#" className="hover:text-white">Kullanım Koşulları</a>
            <a href="#" className="hover:text-white">İletişim</a>
          </div>
        </div>
      </footer>
    </div>
  )
}
