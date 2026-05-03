import { Link } from 'react-router-dom'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ArrowLeft, Mail, Send } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { authService } from '../../services/authService'
import { ApiError } from '../../lib/apiClient'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)

    if (!email.trim()) {
      setError('E-posta adresi zorunludur.')
      return
    }

    setIsSubmitting(true)
    try {
      await authService.forgotPassword({ email: email.trim() })
      setSubmitted(true)
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
          <div className="w-full max-w-xl rounded-none bg-white p-6 shadow-none sm:rounded-3xl sm:p-8 sm:shadow-sm lg:p-8">

            {submitted ? (
              <SuccessView email={email} />
            ) : (
              <RequestView
                email={email}
                setEmail={setEmail}
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

interface RequestViewProps {
  email: string
  setEmail: (v: string) => void
  error: string | null
  isSubmitting: boolean
  onSubmit: (e: FormEvent<HTMLFormElement>) => void
}

function RequestView({ email, setEmail, error, isSubmitting, onSubmit }: RequestViewProps) {
  return (
    <>
      <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-violet-100 text-violet-600">
        <Mail size={22} />
      </div>
      <h2 className="text-center text-3xl font-semibold tracking-tight">Şifremi Unuttum</h2>
      <p className="mt-2 text-center text-sm text-slate-500">
        E-posta adresinizi girin, şifre sıfırlama kodunu gönderelim.
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

        {error && <p className="text-sm font-medium text-rose-600">{error}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-violet-600 px-4 py-2.5 font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-70 flex items-center justify-center gap-2"
        >
          <Send size={16} />
          {isSubmitting ? 'Gönderiliyor...' : 'Sıfırlama Kodu Gönder'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        <Link to="/login" className="inline-flex items-center gap-1 font-medium text-violet-600 hover:underline">
          <ArrowLeft size={14} />
          Giriş sayfasına dön
        </Link>
      </p>
    </>
  )
}

function SuccessView({ email }: { email: string }) {
  return (
    <div className="flex flex-col items-center text-center">
      <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600">
        <Mail size={28} />
      </div>
      <h2 className="text-2xl font-semibold tracking-tight text-slate-900">E-postanızı kontrol edin</h2>
      <p className="mt-3 text-sm text-slate-500 max-w-sm">
        <span className="font-medium text-slate-700">{email}</span> adresine şifre sıfırlama kodu gönderdik.
        Kodun geçerlilik süresi <span className="font-medium">15 dakikadır</span>.
      </p>

      <div className="mt-6 w-full rounded-xl border border-slate-200 bg-slate-50 p-4 text-left text-sm text-slate-600 space-y-2">
        <p className="font-semibold text-slate-700">Sonraki adımlar:</p>
        <ol className="list-decimal list-inside space-y-1 text-slate-500">
          <li>E-postanızdaki sıfırlama kodunu kopyalayın.</li>
          <li>Aşağıdaki butona tıklayarak şifre sıfırlama sayfasına gidin.</li>
          <li>Kodu ve yeni şifrenizi girin.</li>
        </ol>
      </div>

      <Link
        to="/reset-password"
        className="mt-6 w-full rounded-md bg-violet-600 px-4 py-2.5 text-center font-medium text-white hover:bg-violet-700"
      >
        Şifremi Sıfırla
      </Link>

      <p className="mt-4 text-center text-sm text-slate-600">
        <Link to="/login" className="inline-flex items-center gap-1 font-medium text-violet-600 hover:underline">
          <ArrowLeft size={14} />
          Giriş sayfasına dön
        </Link>
      </p>
    </div>
  )
}
