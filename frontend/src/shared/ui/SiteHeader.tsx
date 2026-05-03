import { useEffect, useRef, useState } from 'react'
import { Menu, Search, ShoppingCart, User, X, ChevronDown, LogOut, ShieldCheck, Package } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useCart } from '../../context/CartContext'
import './SiteHeader.css'

const categories = [
  { label: 'Kadın',           to: '/products?categoryId=1' },
  { label: 'Erkek',           to: '/products?categoryId=2' },
  { label: 'Çocuk',           to: '/products?categoryId=3' },
  { label: 'Teknoloji',       to: '/products?categoryId=4' },
  { label: 'Ayakkabı',        to: '/products?categoryId=5' },
  { label: 'Spor & Outdoor',  to: '/products?categoryId=6' },
  { label: 'Tüm Ürünler',     to: '/products' },
]

export default function SiteHeader() {
  const { user, logout, status } = useAuth()
  const { itemCount } = useCart()
  const isAuthenticated = user !== null
  const isLoading = status === 'loading'
  const navigate = useNavigate()
  const location = useLocation()
  const currentCategoryId = new URLSearchParams(location.search).get('categoryId')
  const currentKeyword = new URLSearchParams(location.search).get('keyword') ?? ''
  const isOnProducts = location.pathname === '/products'
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [searchFocused, setSearchFocused] = useState(false)
  const [searchValue, setSearchValue] = useState(currentKeyword)
  const [scrolled, setScrolled] = useState(false)
  const searchRef = useRef<HTMLInputElement>(null)

  // Sync input value when URL keyword changes (e.g. browser back/forward)
  useEffect(() => {
    setSearchValue(currentKeyword)
  }, [currentKeyword])

  const handleSearch = (value: string) => {
    const trimmed = value.trim()
    const params = new URLSearchParams()
    if (trimmed) params.set('keyword', trimmed)
    if (currentCategoryId) params.set('categoryId', currentCategoryId)
    navigate(`/products${params.toString() ? '?' + params.toString() : ''}`)
  }

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 10)
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  // Close mobile menu on resize to desktop
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 1024) setMobileMenuOpen(false)
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return (
    <>
      <header className={`site-header${scrolled ? ' site-header--scrolled' : ''}`}>
        <div className="mx-auto w-full max-w-[1320px] px-4 sm:px-6">
          <div className="flex h-16 items-center gap-4 lg:h-[72px]">
            <Link to="/" className="flex shrink-0 items-center gap-2.5 group">
              <div className="site-header-logo-mark group-hover:scale-105 group-hover:shadow-indigo-300">
                <img
                  src="/static/logos/websiteLogo.png"
                  alt="Bozkurt logo"
                  className="site-header-logo-image"
                  onError={(e) => {
                    // Fallback: hide broken image
                    (e.currentTarget as HTMLImageElement).style.display = 'none'
                  }}
                />
              </div>
              <span className="site-header-logo-text group-hover:opacity-80">
                Bozkurt
              </span>
            </Link>
            <div className="site-header-search-desktop hidden flex-1 lg:flex">
              <SearchBar
                value={searchValue}
                onChange={setSearchValue}
                onSubmit={handleSearch}
                focused={searchFocused}
                onFocus={() => setSearchFocused(true)}
                onBlur={() => setSearchFocused(false)}
                inputRef={searchRef}
              />
            </div>
            <div className="ml-auto hidden items-center gap-1 lg:flex">
              {isLoading ? (
                <div className="site-header-skeleton-row">
                  <div className="site-header-skeleton-pill site-header-skeleton-pill--w80" />
                  <div className="site-header-skeleton-pill site-header-skeleton-pill--w72" />
                </div>
              ) : isAuthenticated ? (
                <>
                  {user && (
                    <span className="site-header-greeting">
                      Merhaba, {user.firstName}
                    </span>
                  )}
                  {user?.role === 'ADMIN' && (
                    <NavAction to="/admin" icon={<ShieldCheck size={18} />} label="Admin" />
                  )}
                  <NavAction to="/account" icon={<User size={18} />} label="Hesabım" />
                  <NavAction to="/orders" icon={<Package size={18} />} label="Siparişlerim" />
                  <NavAction to="/cart" icon={<ShoppingCart size={18} />} label="Sepetim" badge={itemCount > 0 ? itemCount : undefined} />
                  <button
                    onClick={async () => { await logout(); navigate('/login') }}
                    title="Çıkış Yap"
                    className="site-header-logout-btn hover:bg-red-50"
                  >
                    <LogOut size={16} />
                    Çıkış
                  </button>
                </>
              ) : (
                <>
                  <NavAction to="/cart" icon={<ShoppingCart size={18} />} label="Sepetim" badge={itemCount > 0 ? itemCount : undefined} />
                  <Link
                    to="/login"
                    className="site-header-login-link hover:bg-indigo-50"
                  >
                    Giriş Yap
                  </Link>
                  <Link
                    to="/register"
                    className="site-header-register-link hover:opacity-90 hover:scale-105"
                  >
                    Kayıt Ol
                  </Link>
                </>
              )}
            </div>
            <div className="ml-auto flex items-center gap-2 lg:hidden">
              {itemCount > 0 && (
                <Link
                  to="/cart"
                  className="site-header-mobile-cart-link hover:bg-slate-100"
                  aria-label="Sepet"
                >
                  <ShoppingCart size={20} />
                  <span className="site-header-cart-badge">
                    {itemCount > 0 ? itemCount : ''}
                  </span>
                </Link>
              )}
              <button
                type="button"
                aria-label={mobileMenuOpen ? 'Menüyü kapat' : 'Menüyü aç'}
                onClick={() => setMobileMenuOpen((v) => !v)}
                className={`site-header-mobile-menu-btn${mobileMenuOpen ? ' site-header-mobile-menu-btn--open' : ''}`}
              >
                {mobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
              </button>
            </div>
          </div>
          <div className="pb-3 lg:hidden">
            <SearchBar
              value={searchValue}
              onChange={setSearchValue}
              onSubmit={handleSearch}
              focused={searchFocused}
              onFocus={() => setSearchFocused(true)}
              onBlur={() => setSearchFocused(false)}
              inputRef={searchRef}
            />
          </div>
          <nav className="hidden border-t border-slate-100 lg:flex">
            <div className="flex items-center gap-1 py-1">
              {categories.map((cat) => {
                const catId = new URLSearchParams(cat.to.split('?')[1] ?? '').get('categoryId')
                const isActive = isOnProducts && (
                  catId ? currentCategoryId === catId : !currentCategoryId
                )
                return (
                  <Link
                    key={cat.label}
                    to={cat.to}
                    className={`site-header-category-link${isActive ? ' site-header-category-link--active' : ' hover:bg-slate-100 hover:text-indigo-600'}`}
                  >
                    {cat.label}
                  </Link>
                )
              })}
            </div>
          </nav>
        </div>
        <div className={`site-header-mobile-drawer lg:hidden${mobileMenuOpen ? ' site-header-mobile-drawer--open' : ''}`}>
          <div className="mx-auto w-full max-w-[1320px] px-4 py-4 sm:px-6">
            <div className="site-header-mobile-auth-wrap">
              {isLoading ? (
                <div className="site-header-mobile-skeleton-row">
                  <div className="site-header-mobile-skeleton-pill" />
                  <div className="site-header-mobile-skeleton-pill" />
                </div>
              ) : isAuthenticated ? (
                <>
                  {user?.role === 'ADMIN' && (
                    <MobileAuthLink to="/admin" icon={<ShieldCheck size={16} />} label="Admin" />
                  )}
                  <MobileAuthLink to="/account" icon={<User size={16} />} label="Hesabım" />
                  <MobileAuthLink to="/orders" icon={<Package size={16} />} label="Siparişlerim" />
                  <MobileAuthLink to="/cart" icon={<ShoppingCart size={16} />} label="Sepetim" />
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    onClick={() => setMobileMenuOpen(false)}
                    className="site-header-mobile-auth-link site-header-mobile-auth-link--login"
                  >
                    Giriş Yap
                  </Link>
                  <Link
                    to="/register"
                    onClick={() => setMobileMenuOpen(false)}
                    className="site-header-mobile-auth-link site-header-mobile-auth-link--register"
                  >
                    Kayıt Ol
                  </Link>
                </>
              )}
            </div>
            <div className="site-header-mobile-categories">
              {categories.map((cat) => {
                const catId = new URLSearchParams(cat.to.split('?')[1] ?? '').get('categoryId')
                const isActive = isOnProducts && (
                  catId ? currentCategoryId === catId : !currentCategoryId
                )
                return (
                  <Link
                    key={cat.label}
                    to={cat.to}
                    onClick={() => setMobileMenuOpen(false)}
                    className={`site-header-mobile-category-link${isActive ? ' site-header-mobile-category-link--active' : ' hover:bg-slate-50'}`}
                  >
                    <span>{cat.label}</span>
                    <ChevronDown size={14} className="site-header-mobile-category-chevron" />
                  </Link>
                )
              })}
            </div>
          </div>
        </div>
      </header>
    </>
  )
}

function SearchBar({
  value,
  onChange,
  onSubmit,
  focused,
  onFocus,
  onBlur,
  inputRef,
}: {
  value: string
  onChange: (v: string) => void
  onSubmit: (v: string) => void
  focused: boolean
  onFocus: () => void
  onBlur: () => void
  inputRef: React.RefObject<HTMLInputElement | null>
}) {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      inputRef.current?.blur()
      onSubmit(value)
    }
    if (e.key === 'Escape') {
      onChange('')
      inputRef.current?.blur()
    }
  }

  const handleClear = () => {
    onChange('')
    onSubmit('')
    inputRef.current?.focus()
  }

  return (
    <div className="site-header-search-wrap">
      <div
        className={`site-header-search-box${focused ? ' site-header-search-box--focused' : ''}`}
        onClick={() => inputRef.current?.focus()}
      >
        <Search
          size={16}
          className={`site-header-search-icon${focused ? ' site-header-search-icon--focused' : ''}`}
        />
        <input
          ref={inputRef}
          type="text"
          value={value}
          placeholder="Ürün, kategori veya marka ara..."
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={onFocus}
          onBlur={onBlur}
          className="site-header-search-input"
        />
        {value ? (
          <button
            type="button"
            onMouseDown={(e) => e.preventDefault()}
            onClick={handleClear}
            className="site-header-search-clear-btn"
            aria-label="Aramayı temizle"
          >
            <X size={14} />
          </button>
        ) : focused ? (
          <kbd className="site-header-search-kbd">
            Enter ↵
          </kbd>
        ) : null}
      </div>
    </div>
  )
}

function NavAction({
  to,
  icon,
  label,
  badge,
}: {
  to: string
  icon: React.ReactNode
  label: string
  badge?: number
}) {
  return (
    <Link
      to={to}
      className="site-header-nav-action hover:bg-slate-100 hover:text-indigo-600"
    >
      <span className="site-header-nav-action-icon-wrap">
        {icon}
        {badge != null && (
          <span className="site-header-nav-badge">
            {badge}
          </span>
        )}
      </span>
      {label}
    </Link>
  )
}

function MobileAuthLink({
  to,
  icon,
  label,
}: {
  to: string
  icon: React.ReactNode
  label: string
}) {
  return (
    <Link
      to={to}
      className="site-header-mobile-link"
    >
      {icon}
      {label}
    </Link>
  )
}
