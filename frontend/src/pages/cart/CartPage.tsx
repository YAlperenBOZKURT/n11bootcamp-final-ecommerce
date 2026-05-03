import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Trash2, Plus, Minus, ShoppingCart, Tag, ArrowRight, PackageOpen } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { useCart } from '../../context/CartContext'
import { useAuth } from '../../context/AuthContext'
import { ApiError } from '../../lib/apiClient'
import { cartService } from '../../services/cartService'
import { productService } from '../../services/productService'
import './CartPage.css'

export default function CartPage() {
  const { user } = useAuth()
  const { cart, loading, updateQuantity, removeItem, clearCart } = useCart()

  const [couponCode, setCouponCode]   = useState('')
  const [couponError, setCouponError] = useState<string | null>(null)
  const [couponSuccess, setCouponSuccess] = useState<string | null>(null)
  const [appliedCoupon, setAppliedCoupon] = useState<{ code: string; discountAmount: number; finalAmount: number } | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busyItem, setBusyItem]       = useState<string | null>(null)

  const [variantAttrs, setVariantAttrs] = useState<Record<number, Record<string, string>>>({})

  useEffect(() => {
    if (!cart || cart.items.length === 0) return
    const itemsWithVariant = cart.items.filter((i) => i.variantId != null)
    if (itemsWithVariant.length === 0) return

    const uniqueProductIds = [...new Set(itemsWithVariant.map((i) => i.productId))]
    Promise.all(uniqueProductIds.map((pid) => productService.getProductById(pid).catch(() => null)))
      .then((products) => {
        const map: Record<number, Record<string, string>> = {}
        products.forEach((p) => {
          if (!p) return
          p.variants?.forEach((v) => { map[v.id] = v.attributes })
        })
        setVariantAttrs(map)
      })
  }, [cart])
  const isGuest = !user
  if (loading) {
    return (
      <div className="cart-page">
        <SiteHeader />
        <main className="cart-main cart-main--loading">
          {[1, 2, 3].map((i) => (
            <div key={i} className="cart-skeleton-item">
              <div className="cart-skeleton-item__image" />
              <div className="cart-skeleton-item__info">
                <div className="cart-skeleton-item__line1" />
                <div className="cart-skeleton-item__line2" />
              </div>
            </div>
          ))}
        </main>
      </div>
    )
  }
  if (!cart || cart.items.length === 0) {
    return (
      <div className="cart-page">
        <SiteHeader />
        <main className="cart-empty">
          <PackageOpen size={56} color="#c7d2fe" className="cart-empty__icon" />
          <h2 className="cart-empty__title">Sepetiniz boş</h2>
          <p className="cart-empty__subtitle">Beğendiğiniz ürünleri sepete ekleyin.</p>
          <Link to="/products" className="cart-empty__cta">
            Alışverişe Başla
          </Link>
          {isGuest && (
            <p className="cart-empty__guest-hint">
              <Link to="/login" className="cart-empty__login-link">Giriş yaparsanız</Link> sepetiniz kaydedilir.
            </p>
          )}
        </main>
      </div>
    )
  }

  const handleQty = async (itemId: string, delta: number, current: number) => {
    const next = current + delta
    if (next < 1) return
    setBusyItem(itemId)
    setActionError(null)
    try {
      await updateQuantity(itemId, next)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'İşlem başarısız.')
    } finally {
      setBusyItem(null)
    }
  }

  const handleRemove = async (itemId: string) => {
    setBusyItem(itemId)
    setActionError(null)
    try {
      await removeItem(itemId)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Silme başarısız.')
    } finally {
      setBusyItem(null)
    }
  }

  const handleClear = async () => {
    setActionError(null)
    try {
      await clearCart()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Sepet temizlenemedi.')
    }
  }

  const handleCoupon = async () => {
    setCouponError(null)
    setCouponSuccess(null)
    if (!couponCode.trim()) return
    try {
      const preview = await cartService.previewCoupon(couponCode.trim())
      const coupon = {
        code: couponCode.trim().toUpperCase(),
        discountAmount: preview.discountAmount,
        finalAmount: preview.finalAmount,
      }
      setAppliedCoupon(coupon)
      sessionStorage.setItem('applied_coupon', JSON.stringify(coupon))
      setCouponSuccess(
        `Kupon geçerli. İndirim: ₺${preview.discountAmount.toFixed(2)} · Yeni toplam: ₺${preview.finalAmount.toFixed(2)}`
      )
    } catch (err) {
      setAppliedCoupon(null)
      sessionStorage.removeItem('applied_coupon')
      setCouponError(err instanceof ApiError ? err.message : 'Kupon uygulanamadı.')
    }
  }

  return (
    <div className="cart-page">
      <SiteHeader />

      <main className="cart-main">
        {isGuest && (
          <div className="cart-guest-banner">
            <p className="cart-guest-banner__text">
              Misafir olarak alışveriş yapıyorsunuz. Giriş yaparsanız sepetiniz otomatik kaydedilir.
            </p>
            <div className="cart-guest-banner__actions">
              <Link to="/login" className="cart-guest-banner__login-btn">
                Giriş Yap
              </Link>
              <Link to="/register" className="cart-guest-banner__register-btn">
                Kayıt Ol
              </Link>
            </div>
          </div>
        )}
        <div className="cart-title-row">
          <h1 className="cart-title">
            Sepetim <span className="cart-title__count">({cart.items.length} ürün)</span>
          </h1>
          <button onClick={handleClear} className="cart-clear-btn">
            <Trash2 size={14} /> Sepeti Temizle
          </button>
        </div>

        {actionError && (
          <div className="cart-error">{actionError}</div>
        )}

        <div className="cart-layout">
          <div className="cart-items">
            {cart.items.map((item) => (
              <div
                key={item.itemId}
                className={`cart-item${busyItem === item.itemId ? ' cart-item--busy' : ''}`}
              >
                <div className="cart-item__icon">
                  <ShoppingCart size={22} color="#c7d2fe" />
                </div>
                <div className="cart-item__info">
                  <Link to={`/products/${item.productId}`} className="cart-item__name">
                    {item.productName}
                  </Link>

                  {item.variantId != null && variantAttrs[item.variantId] && (
                    <span className="cart-item__variant">
                      {Object.values(variantAttrs[item.variantId]).join(' / ')}
                    </span>
                  )}

                  <p className="cart-item__unit-price">
                    Birim fiyat: <strong>₺{item.unitPrice.toFixed(2)}</strong>
                  </p>
                </div>
                <div className="cart-qty-controls">
                  <QtyBtn onClick={() => handleQty(item.itemId, -1, item.quantity)} disabled={busyItem === item.itemId || item.quantity <= 1}>
                    <Minus size={13} />
                  </QtyBtn>
                  <span className="cart-qty-controls__value">{item.quantity}</span>
                  <QtyBtn onClick={() => handleQty(item.itemId, +1, item.quantity)} disabled={busyItem === item.itemId}>
                    <Plus size={13} />
                  </QtyBtn>
                </div>
                <div className="cart-item__total">
                  <p>₺{item.lineTotal.toFixed(2)}</p>
                </div>
                <button
                  onClick={() => handleRemove(item.itemId)}
                  disabled={busyItem === item.itemId}
                  className="cart-item__remove-btn"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
          <div className="cart-summary">
            <h2 className="cart-summary__title">Sipariş Özeti</h2>

            <div className="cart-summary__items">
              {cart.items.map((item) => (
                <div key={item.itemId} className="cart-summary__item">
                  <span className="cart-summary__item-name">
                    {item.productName} × {item.quantity}
                  </span>
                  <span className="cart-summary__item-price">₺{item.lineTotal.toFixed(2)}</span>
                </div>
              ))}
            </div>

            <div className="cart-summary__divider">
              {appliedCoupon && (
                <div className="cart-summary__total-row cart-summary__total-row--discount">
                  <span>Kupon İndirimi ({appliedCoupon.code})</span>
                  <span>-₺{appliedCoupon.discountAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="cart-summary__total-row">
                <span>Toplam</span>
                <span className="cart-summary__total-amount">₺{(appliedCoupon?.finalAmount ?? cart.totalAmount).toFixed(2)}</span>
              </div>
            </div>

            {!isGuest && (
              <div className="cart-coupon">
                <div className="cart-coupon__input-row">
                  <div className="cart-coupon__input-wrapper">
                    <Tag size={14} className="cart-coupon__icon" />
                    <input
                      value={couponCode}
                      onChange={(e) => {
                        setCouponCode(e.target.value)
                        setCouponError(null)
                        setCouponSuccess(null)
                        setAppliedCoupon(null)
                        sessionStorage.removeItem('applied_coupon')
                      }}
                      placeholder="Kupon kodu"
                      className="cart-coupon__input"
                    />
                  </div>
                  <button onClick={handleCoupon} className="cart-coupon__apply-btn">
                    Uygula
                  </button>
                </div>
                {couponError && <p className="cart-coupon__error">{couponError}</p>}
                {couponSuccess && <p className="cart-coupon__success">{couponSuccess}</p>}
              </div>
            )}

            {isGuest ? (
              <Link to="/login" className="cart-cta-btn">
                Ödeme için Giriş Yap <ArrowRight size={16} />
              </Link>
            ) : (
              <Link
                to="/checkout"
                state={{ coupon: appliedCoupon }}
                className="cart-cta-btn"
              >
                Ödemeye Geç <ArrowRight size={16} />
              </Link>
            )}

            <Link to="/products" className="cart-back-link">
              ← Alışverişe Devam Et
            </Link>
          </div>
        </div>
      </main>
    </div>
  )
}

function QtyBtn({ onClick, disabled, children }: {
  onClick: () => void
  disabled?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`qty-btn${disabled ? ' qty-btn--disabled' : ''}`}
    >
      {children}
    </button>
  )
}
