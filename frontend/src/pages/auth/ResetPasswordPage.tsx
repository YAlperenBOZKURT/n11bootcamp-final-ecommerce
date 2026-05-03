import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ArrowLeft, CheckCircle, Eye, EyeOff, KeyRound, Lock } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { authService } from '../../services/authService'
import { ApiError } from '../../lib/apiClient'

type PasswordStrength = 'weak' | 'fair' | 'strong' | null

function getPasswordStrength(password: string): PasswordStrength {
  if (!password) return null
  let score = 0
  if (password.length >= 8) score++
  if (/[A-Z]/.test(password)) score++
  if (/[0-9]/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password)) score++
  if (score <= 1) return 'weak'
  if (score <= 2) return 'fair'
  return 'strong'
}

const strengthConfig: Record<NonNullable<PasswordStrength>, { label: string; color: string; bars: number }> = {
  weak:   { label: 'Zayıf',   color: 'bg-rose-500',   bars: 1 },
  fair:   { label: 'Orta',    color: 'bg-amber-400',  bars: 2 },
  strong: { label: 'Güçlü',   color: 'bg-green-500',  bars: 3 },
}

export default function ResetPasswordPage() {
  const navigate = useNavigate()
  const [token, setToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showNew, setShowNew] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [success, setSuccess] = useState(false)

  const strength = getPasswordStrength(newPassword)

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)

    if (!token.trim()) {
      setError('Sıfırlama kodu zorunludur.')
      return
    }
    if (!newPassword) {
      setError('Yeni şifre zorunludur.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('Şifreler eşleşmiyor.')
      return
    }
    if (strength === 'weak') {
      setError('Daha güçlü bir şifre seçin.')
      return
    }

    setIsSubmitting(true)
    try {
      await authService.resetPassword({ token: token.trim(), newPassword })
      setSuccess(true)
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

            {success ? (
              <SuccessView onGoToLogin={() => navigate('/login', { replace: true })} />
            ) : (
              <ResetForm
                token={token}
                setToken={setToken}
                newPassword={newPassword}
                setNewPassword={setNewPassword}
                confirmPassword={confirmPassword}
                setConfirmPassword={setConfirmPassword}
                showNew={showNew}
                setShowNew={setShowNew}
                showConfirm={showConfirm}
                setShowConfirm={setShowConfirm}
                strength={strength}
                error={error}
                isSubmitting={isSubmitting}
                onSubmit={onSubmit}
              />
            )}

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

interface ResetFormProps {
  token: string
  setToken: (v: string) => void
  newPassword: string
  setNewPassword: (v: string) => void
  confirmPassword: string
  setConfirmPassword: (v: string) => void
  showNew: boolean
  setShowNew: (v: boolean) => void
  showConfirm: boolean
  setShowConfirm: (v: boolean) => void
  strength: PasswordStrength
  error: string | null
  isSubmitting: boolean
  onSubmit: (e: FormEvent<HTMLFormElement>) => void
}

function ResetForm({
  token, setToken,
  newPassword, setNewPassword,
  confirmPassword, setConfirmPassword,
  showNew, setShowNew,
  showConfirm, setShowConfirm,
  strength, error, isSubmitting, onSubmit,
}: ResetFormProps) {
  return (
    <>
      <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-violet-100 text-violet-600">
        <KeyRound size={22} />
      </div>
      <h2 className="text-center text-3xl font-semibold tracking-tight">Şifre Sıfırla</h2>
      <p className="mt-2 text-center text-sm text-slate-500">
        E-postanızdaki kodu ve yeni şifrenizi girin.
      </p>

      <form className="mt-6 space-y-4" onSubmit={onSubmit}>
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Sıfırlama Kodu</label>
          <div className="relative">
            <KeyRound className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="E-postanızdaki kodu yapıştırın"
              className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-3 font-mono text-sm text-slate-900 outline-none ring-violet-500 focus:ring-2"
            />
          </div>
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Yeni Şifre</label>
          <div className="relative">
            <Lock className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type={showNew ? 'text' : 'password'}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Yeni şifrenizi girin"
              className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-11 text-slate-900 outline-none ring-violet-500 focus:ring-2"
            />
            <button
              type="button"
              onClick={() => setShowNew(!showNew)}
              aria-label={showNew ? 'Şifreyi gizle' : 'Şifreyi göster'}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
            >
              {showNew ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
          {strength && (
            <PasswordStrengthBar strength={strength} />
          )}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Şifre Tekrar</label>
          <div className="relative">
            <Lock className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type={showConfirm ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Şifrenizi tekrar girin"
              className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-11 text-slate-900 outline-none ring-violet-500 focus:ring-2"
            />
            <button
              type="button"
              onClick={() => setShowConfirm(!showConfirm)}
              aria-label={showConfirm ? 'Şifreyi gizle' : 'Şifreyi göster'}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
            >
              {showConfirm ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
          {confirmPassword && newPassword !== confirmPassword && (
            <p className="mt-1 text-xs text-rose-500">Şifreler eşleşmiyor.</p>
          )}
        </div>

        {error && <p className="text-sm font-medium text-rose-600">{error}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-violet-600 px-4 py-2.5 font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-70"
        >
          {isSubmitting ? 'Sıfırlanıyor...' : 'Şifremi Sıfırla'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        Kod almadınız mı?{' '}
        <Link to="/forgot-password" className="font-medium text-violet-600 hover:underline">
          Tekrar gönder
        </Link>
      </p>
      <p className="mt-2 text-center text-sm text-slate-600">
        <Link to="/login" className="inline-flex items-center gap-1 font-medium text-violet-600 hover:underline">
          <ArrowLeft size={14} />
          Giriş sayfasına dön
        </Link>
      </p>
    </>
  )
}

function PasswordStrengthBar({ strength }: { strength: NonNullable<PasswordStrength> }) {
  const cfg = strengthConfig[strength]
  return (
    <div className="mt-2 flex items-center gap-2">
      <div className="flex flex-1 gap-1">
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            className={`h-1 flex-1 rounded-full transition-colors ${i < cfg.bars ? cfg.color : 'bg-slate-200'}`}
          />
        ))}
      </div>
      <span className="text-xs font-medium text-slate-500">{cfg.label}</span>
    </div>
  )
}

function SuccessView({ onGoToLogin }: { onGoToLogin: () => void }) {
  return (
    <div className="flex flex-col items-center text-center">
      <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600">
        <CheckCircle size={32} />
      </div>
      <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Şifreniz güncellendi</h2>
      <p className="mt-3 text-sm text-slate-500 max-w-sm">
        Şifreniz başarıyla sıfırlandı. Yeni şifrenizle giriş yapabilirsiniz.
      </p>
      <button
        onClick={onGoToLogin}
        className="mt-6 w-full rounded-md bg-violet-600 px-4 py-2.5 font-medium text-white hover:bg-violet-700"
      >
        Giriş Yap
      </button>
    </div>
  )
}

