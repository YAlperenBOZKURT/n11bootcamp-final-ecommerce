import { Link, useNavigate } from 'react-router-dom'
import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { BadgePercent, CreditCard, Eye, EyeOff, Headset, Lock, Mail, Phone, ShieldCheck, UserRound } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { useAuth } from '../../context/AuthContext'
import { useCart } from '../../context/CartContext'
import { ApiError } from '../../lib/apiClient'

export default function RegisterPage() {
  const { register } = useAuth()
  const { syncGuestCart } = useCart()
  const navigate = useNavigate()
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [password, setPassword] = useState('')
  const [passwordAgain, setPasswordAgain] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showPasswordAgain, setShowPasswordAgain] = useState(false)
  const [acceptTerms, setAcceptTerms] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const passwordRule = useMemo(
    () => /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]).{8,}$/,
    []
  )

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)

    if (!firstName.trim() || !lastName.trim() || !email.trim() || !password.trim()) {
      setError('Ad, soyad, e-posta ve şifre alanları zorunludur.')
      return
    }

    if (!passwordRule.test(password)) {
      setError('Şifre en az 8 karakter, bir büyük harf, bir rakam ve bir özel karakter içermelidir.')
      return
    }

    if (password !== passwordAgain) {
      setError('Şifreler eşleşmiyor.')
      return
    }

    if (!acceptTerms) {
      setError('Devam etmek için kullanım koşullarını kabul etmelisiniz.')
      return
    }

    setIsSubmitting(true)
    try {
      await register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
        phoneNumber: phoneNumber.trim() || null,
      })
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

      <main className="grid w-full grid-cols-1 lg:flex-1 lg:min-h-0 lg:grid-cols-[720px_580px] lg:content-center lg:justify-center lg:gap-8 lg:px-0">
        <section className="hidden lg:flex lg:items-center lg:justify-center">
          <img
            src="/static/photos/registerPhoto.png"
            alt="Kayıt görseli"
            className="h-auto max-h-[calc(100vh-260px)] w-full rounded-3xl object-contain"
          />
        </section>

        <section className="flex items-center justify-center p-4 sm:p-8 lg:p-0">
          <div className="w-full max-w-xl rounded-none bg-white p-6 shadow-none sm:rounded-3xl sm:p-8 sm:shadow-sm lg:max-h-[calc(100vh-260px)] lg:max-w-none lg:overflow-y-auto lg:p-6">
            <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-violet-100 text-violet-600">
              <UserRound size={22} />
            </div>
            <h2 className="text-center text-3xl font-semibold tracking-tight">Kayıt Ol</h2>
            <p className="mt-2 text-center text-sm text-slate-500">
              Hızlı ve kolayca hesabınızı oluşturun.
            </p>

            <form className="mt-6 space-y-3.5" onSubmit={onSubmit}>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Ad</label>
                  <div className="relative">
                    <UserRound className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                    <input
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      placeholder="Adınızı girin"
                      className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-3 text-slate-900 outline-none ring-violet-500 focus:ring-2"
                    />
                  </div>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Soyad</label>
                  <div className="relative">
                    <UserRound className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                    <input
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      placeholder="Soyadınızı girin"
                      className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-3 text-slate-900 outline-none ring-violet-500 focus:ring-2"
                    />
                  </div>
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">E-posta adresi</label>
                <div className="relative">
                  <Mail className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="ornek@mail.com"
                    className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-3 text-slate-900 outline-none ring-violet-500 focus:ring-2"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Telefon numarası (opsiyonel)</label>
                <div className="relative">
                  <Phone className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                  <input
                    type="tel"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    placeholder="5XX XXX XX XX"
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
                    placeholder="En az 8 karakter"
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

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Şifre Tekrar</label>
                <div className="relative">
                  <Lock className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                  <input
                    type={showPasswordAgain ? 'text' : 'password'}
                    value={passwordAgain}
                    onChange={(e) => setPasswordAgain(e.target.value)}
                    placeholder="Şifrenizi tekrar girin"
                    className="w-full rounded-md border border-slate-300 py-2 pl-9 pr-11 text-slate-900 outline-none ring-violet-500 focus:ring-2"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPasswordAgain((s) => !s)}
                    aria-label={showPasswordAgain ? 'Şifreyi gizle' : 'Şifreyi göster'}
                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
                  >
                    {showPasswordAgain ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <label className="flex items-center gap-2 text-sm text-slate-600">
                <input
                  type="checkbox"
                  checked={acceptTerms}
                  onChange={(e) => setAcceptTerms(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-300 text-violet-600 focus:ring-violet-500"
                />
                Kullanım koşullarını kabul ediyorum
              </label>

              {error && <p className="text-sm font-medium text-rose-600">{error}</p>}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-md bg-violet-600 px-4 py-2.5 font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {isSubmitting ? 'Kaydediliyor...' : 'Kayıt Ol'}
              </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-600">
              Zaten hesabın var mı?{' '}
              <Link to="/login" className="font-medium text-violet-600 hover:underline">
                Giriş Yap
              </Link>
            </p>
          </div>
        </section>
      </main>

      <section className="hidden border-y border-slate-200 bg-white lg:mt-auto lg:block">
        <div className="mx-auto grid w-full max-w-[1320px] grid-cols-4 gap-6 px-6 py-5">
          <div className="flex items-start gap-3">
            <div className="rounded-full bg-violet-100 p-2 text-violet-600"><BadgePercent size={18} /></div>
            <div>
              <p className="text-sm font-semibold text-slate-800">Geniş Ürün Yelpazesi</p>
              <p className="text-xs text-slate-500">Binlerce ürün, yüzlerce marka</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="rounded-full bg-violet-100 p-2 text-violet-600"><BadgePercent size={18} /></div>
            <div>
              <p className="text-sm font-semibold text-slate-800">Özel Kampanyalar</p>
              <p className="text-xs text-slate-500">Kaçırılmayacak fırsatlar</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="rounded-full bg-violet-100 p-2 text-violet-600"><Headset size={18} /></div>
            <div>
              <p className="text-sm font-semibold text-slate-800">7/24 Müşteri Desteği</p>
              <p className="text-xs text-slate-500">Her zaman yanınızdayız</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="rounded-full bg-violet-100 p-2 text-violet-600"><ShieldCheck size={18} /></div>
            <div>
              <p className="text-sm font-semibold text-slate-800">Güvenli Ödeme</p>
              <p className="text-xs text-slate-500">Tüm ödemeleriniz güvende</p>
            </div>
          </div>
        </div>
      </section>

      <footer className="hidden bg-slate-900 lg:block">
        <div className="mx-auto flex h-14 w-full max-w-[1320px] items-center justify-between px-6 text-xs text-slate-300">
          <p>© 2026 Bozkurt. Tüm hakları saklıdır.</p>
          <div className="flex items-center gap-5">
            <a href="#" className="hover:text-white">Hakkımızda</a>
            <a href="#" className="hover:text-white">Kariyer</a>
            <a href="#" className="hover:text-white">Gizlilik</a>
            <a href="#" className="hover:text-white">Kullanım Koşulları</a>
            <a href="#" className="hover:text-white">İletişim</a>
            <CreditCard size={15} />
          </div>
        </div>
      </footer>
    </div>
  )
}
