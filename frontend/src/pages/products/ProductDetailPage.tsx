import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useCart } from '../../context/CartContext'
import { useAuth } from '../../context/AuthContext'
import {
  ArrowLeft,
  ShoppingCart,
  Heart,
  Star,
  Tag,
  Package,
  Truck,
  ShieldCheck,
  RotateCcw,
  MessageSquare,
  Trash2,
} from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import Breadcrumb from '../../shared/ui/Breadcrumb'
import { productService } from '../../services/productService'
import type { ProductResponse, VariantResponse } from '../../services/productService'
import { reviewService } from '../../services/reviewService'
import type { ReviewResponse } from '../../services/reviewService'
import './ProductDetailPage.css'

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addItem } = useCart()
  const { user } = useAuth()

  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [selectedVariant, setSelectedVariant] = useState<VariantResponse | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [activeImageIndex, setActiveImageIndex] = useState(0)
  const [wishlist, setWishlist] = useState(false)
  const [addingToCart, setAddingToCart] = useState(false)
  const [cartFeedback, setCartFeedback] = useState<'success' | 'error' | null>(null)

  const [reviews, setReviews] = useState<ReviewResponse[]>([])
  const [reviewRating, setReviewRating] = useState(5)
  const [reviewText, setReviewText] = useState('')
  const [submittingReview, setSubmittingReview] = useState(false)
  const [reviewError, setReviewError] = useState<string | null>(null)
  const [reviewSuccess, setReviewSuccess] = useState(false)
  const reviewsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!id) return
    let active = true
    async function fetchProduct() {
      try {
        const [p, r] = await Promise.all([
          productService.getProductById(Number(id)),
          reviewService.getByProductId(Number(id)).catch(() => []),
        ])
        if (!active) return
        setError(false)
        setProduct(p)
        setReviews(r)
        setActiveImageIndex(0)
        const firstActive = p.variants?.find((v) => v.status === 'ACTIVE') ?? p.variants?.[0] ?? null
        setSelectedVariant(firstActive)
      } catch {
        if (active) setError(true)
      } finally {
        if (active) setLoading(false)
      }
    }
    void fetchProduct()
    return () => { active = false }
  }, [id])

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!product || !reviewText.trim()) return
    setSubmittingReview(true)
    setReviewError(null)
    try {
      const newReview = await reviewService.create({
        productId: product.id,
        rating: reviewRating,
        commentText: reviewText.trim(),
      })
      setReviews((prev) => [newReview, ...prev])
      setReviewText('')
      setReviewRating(5)
      setReviewSuccess(true)
      setTimeout(() => setReviewSuccess(false), 3000)
    } catch {
      setReviewError('Yorum gönderilemedi. Lütfen tekrar deneyin.')
    } finally {
      setSubmittingReview(false)
    }
  }

  const handleDeleteReview = async (id: number) => {
    try {
      await reviewService.delete(id)
      setReviews((prev) => prev.filter((r) => r.id !== id))
    } catch {
      // silent failure or optionally show an error message , i dont know !!
    }
  }

  const avgRating = reviews.length > 0
    ? reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length
    : null

  const handleAddToCart = async () => {
    if (!product || mustSelectVariant || isOutOfStock || !selectedVariantInStock) return
    setAddingToCart(true)
    setCartFeedback(null)

    const itemAttributes: Record<string, string> = selectedVariant?.attributes
      ? { ...selectedVariant.attributes }
      : { ...(product.attributes ?? {}) }

    try {
      await addItem({
        productId: product.id,
        variantId: selectedVariant?.id ?? null,
        productName: product.name,
        unitPrice: effectivePrice,
        quantity,
        attributes: Object.keys(itemAttributes).length > 0 ? itemAttributes : undefined,
      })
      setCartFeedback('success')
      setTimeout(() => setCartFeedback(null), 2500)
    } catch {
      setCartFeedback('error')
      setTimeout(() => setCartFeedback(null), 2500)
    } finally {
      setAddingToCart(false)
    }
  }

  const hasVariants = (product?.variants?.length ?? 0) > 0
  const effectivePrice = selectedVariant?.price ?? product?.priceFrom ?? 0
  const originalPrice = selectedVariant?.originalPrice ?? null
  const hasDiscount = (selectedVariant?.discountRate ?? 0) > 0
  const isOutOfStock = !product?.inStock
  const mustSelectVariant = hasVariants && !selectedVariant
  const selectedVariantInStock = !selectedVariant || selectedVariant.status === 'ACTIVE'
  const availableQty: number | null = null

  const cartBtnDisabled = mustSelectVariant || isOutOfStock || !selectedVariantInStock || addingToCart
  return (
    <div className="product-detail-page">
      <SiteHeader />

      <main className="mx-auto w-full max-w-[1320px] px-4 sm:px-6 py-6">
        {product && !loading && (
          <Breadcrumb
            items={[
              { label: 'Ürünler', to: '/products' },
              { label: product.category.name, to: `/products?categoryId=${product.category.id}` },
              { label: product.name },
            ]}
          />
        )}

        <button onClick={() => navigate(-1)} className="product-detail-back-btn">
          <ArrowLeft size={16} />
          Geri
        </button>

        {loading && <DetailSkeleton />}

        {error && !loading && (
          <div className="product-detail-error">
            <Package size={48} color="#cbd5e1" className="product-detail-error__icon" />
            <h2 className="product-detail-error__title">Ürün bulunamadı</h2>
            <p className="product-detail-error__subtitle">Aradığınız ürün mevcut değil veya kaldırılmış olabilir.</p>
            <Link to="/products" className="product-detail-error__link">Ürünlere Dön</Link>
          </div>
        )}

        {product && !loading && (
          <>
            <div className="product-detail-top-grid">
              <div className="product-detail-image-card">
                <div className="product-detail-main-image-wrapper">
                  {product.imageUrls?.length > 0 ? (
                    <img
                      src={product.imageUrls[activeImageIndex]}
                      alt={product.name}
                      className="product-detail-main-image"
                    />
                  ) : (
                    <div className="product-detail-image-placeholder">
                      <Package size={56} color="#cbd5e1" />
                    </div>
                  )}
                  {hasDiscount && selectedVariant?.discountRate != null && (
                    <div className="product-detail-discount-badge">
                      <Tag size={11} />%{Math.round(selectedVariant.discountRate)} İndirim
                    </div>
                  )}
                  <button
                    onClick={() => setWishlist((v) => !v)}
                    aria-label="Favorilere ekle"
                    className="product-detail-wishlist-btn"
                  >
                    <Heart size={15} fill={wishlist ? '#ef4444' : 'none'} stroke={wishlist ? '#ef4444' : '#94a3b8'} />
                  </button>
                </div>

                {product.imageUrls?.length > 1 && (
                  <div className="product-detail-thumbnails">
                    {product.imageUrls.map((url, i) => (
                      <button
                        key={i}
                        onClick={() => setActiveImageIndex(i)}
                        className={`product-detail-thumb-btn product-detail-thumb-btn--${i === activeImageIndex ? 'active' : 'inactive'}`}
                      >
                        <img src={url} alt={`${product.name} ${i + 1}`} className="product-detail-thumb-img" />
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="product-detail-info-card">
                <div className="product-detail-meta-row">
                  <span className="product-detail-brand">{product.brand}</span>
                  <StatusBadge inStock={product.inStock} variantInStock={selectedVariantInStock} />
                </div>

                <h1 className="product-detail-name">{product.name}</h1>

                {avgRating !== null && (
                  <button
                    className="product-detail-rating product-detail-rating--clickable"
                    onClick={() => reviewsRef.current?.scrollIntoView({ behavior: 'smooth' })}
                  >
                    {[1,2,3,4,5].map((i) => (
                      <Star key={i} size={13} fill={i <= Math.round(avgRating) ? '#f59e0b' : 'none'} stroke={i <= Math.round(avgRating) ? '#f59e0b' : '#e2e8f0'} />
                    ))}
                    <span className="product-detail-rating__count">{avgRating.toFixed(1)} · {reviews.length} değerlendirme</span>
                  </button>
                )}

                <div className="product-detail-price-row">
                  {mustSelectVariant ? (
                    <span className="product-detail-price--select">
                      {product.priceFrom ? `${product.priceFrom.toLocaleString('tr-TR')} ₺'den başlayan fiyatlar` : 'Fiyat için varyant seçin'}
                    </span>
                  ) : (
                    <>
                      <span className={`product-detail-price--main product-detail-price--main-${hasDiscount ? 'discount' : 'normal'}`}>
                        {effectivePrice.toLocaleString('tr-TR')} ₺
                      </span>
                      {hasDiscount && originalPrice != null && (
                        <span className="product-detail-price--original">{originalPrice.toLocaleString('tr-TR')} ₺</span>
                      )}
                      {hasDiscount && selectedVariant?.discountRate != null && (
                        <span className="product-detail-price--discount-pct">%{Math.round(selectedVariant.discountRate)} indirim</span>
                      )}
                    </>
                  )}
                </div>

                {product.variants && product.variants.length > 0 && (
                  <div>
                    <p className="product-detail-variants-label">Varyant Seç</p>
                    <div className="product-detail-variants">
                      {product.variants.map((v) => {
                        const isSelected = selectedVariant?.id === v.id
                        const isPassive = v.status === 'PASSIVE'
                        const label = Object.values(v.attributes ?? {}).join(' / ') || v.sku
                        return (
                          <button
                            key={v.id}
                            onClick={() => !isPassive && setSelectedVariant(v)}
                            disabled={isPassive}
                            title={isPassive ? 'Bu varyant stokta yok' : undefined}
                            className={`product-detail-variant-btn product-detail-variant-btn--${isSelected ? 'selected' : isPassive ? 'passive' : 'normal'}`}
                          >
                            {label}
                          </button>
                        )
                      })}
                    </div>
                  </div>
                )}

                <div className="product-detail-cart-row">
                  <div className="product-detail-qty-wrapper">
                    <QtyButton label="−" onClick={() => setQuantity((q) => Math.max(1, q - 1))} />
                    <input
                      type="number"
                      min={1}
                      max={availableQty ?? undefined}
                      value={quantity}
                      onChange={(e) => {
                        const val = parseInt(e.target.value, 10)
                        if (isNaN(val) || val < 1) return setQuantity(1)
                        if (availableQty !== null && val > availableQty) return setQuantity(availableQty)
                        setQuantity(val)
                      }}
                      className="product-detail-qty-input"
                    />
                    <QtyButton label="+" onClick={() => setQuantity((q) => availableQty !== null ? Math.min(q + 1, availableQty) : q + 1)} />
                  </div>

                  <button
                    disabled={cartBtnDisabled}
                    onClick={handleAddToCart}
                    className={`product-detail-cart-btn product-detail-cart-btn--${cartBtnDisabled ? 'disabled' : 'normal'} product-detail-cart-btn--${cartFeedback ?? 'default'}${addingToCart ? ' product-detail-cart-btn--adding' : ''}`}
                  >
                    <ShoppingCart size={15} />
                    {addingToCart
                      ? 'Ekleniyor...'
                      : cartFeedback === 'success'
                        ? 'Sepete Eklendi ✓'
                        : cartFeedback === 'error'
                          ? 'Hata oluştu'
                          : mustSelectVariant
                            ? 'Önce Varyant Seçin'
                            : (isOutOfStock || !selectedVariantInStock)
                              ? 'Stokta Yok'
                              : 'Sepete Ekle'}
                  </button>
                </div>
              </div>
            </div>
            <div className="product-detail-trust-badges">
              <TrustBadge icon={<Truck size={16} />} label="Ücretsiz Kargo" sub="200 ₺ üzeri" />
              <TrustBadge icon={<ShieldCheck size={16} />} label="Güvenli Ödeme" sub="256-bit SSL" />
              <TrustBadge icon={<RotateCcw size={16} />} label="Kolay İade" sub="14 gün içinde" />
            </div>
            <div className="product-detail-attrs-card">
              <div className="product-detail-attrs-card__header">Ürün Detayları</div>

              {product.description && (
                <div
                  className={`product-detail-description${Object.keys(product.attributes ?? {}).length > 0 ? ' product-detail-description--with-border' : ''}`}
                >
                  {product.description}
                </div>
              )}

              {product.attributes && Object.keys(product.attributes).length > 0 && (
                <table className="product-detail-attrs-table">
                  <tbody>
                    {Object.entries(product.attributes).map(([key, val], idx) => (
                      <tr key={key} className={`product-detail-attrs-table__row--${idx % 2 === 0 ? 'even' : 'odd'}`}>
                        <td className="product-detail-attrs-table__key-cell">{key}</td>
                        <td className="product-detail-attrs-table__val-cell">{val || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div className="product-detail-reviews-card" ref={reviewsRef}>
              <div className="product-detail-reviews-header">
                <MessageSquare size={18} />
                Değerlendirmeler
                {reviews.length > 0 && <span className="product-detail-reviews-count">{reviews.length}</span>}
              </div>

              {user ? (
                <form onSubmit={handleSubmitReview} className="product-detail-review-form">
                  <div className="product-detail-review-form__rating">
                    <span className="product-detail-review-form__label">Puanınız:</span>
                    {[1,2,3,4,5].map((i) => (
                      <button key={i} type="button" onClick={() => setReviewRating(i)} className="product-detail-review-star-btn">
                        <Star size={22} fill={i <= reviewRating ? '#f59e0b' : 'none'} stroke={i <= reviewRating ? '#f59e0b' : '#cbd5e1'} />
                      </button>
                    ))}
                  </div>
                  <textarea
                    value={reviewText}
                    onChange={(e) => setReviewText(e.target.value)}
                    placeholder="Ürün hakkında düşüncelerinizi paylaşın..."
                    maxLength={2000}
                    rows={4}
                    className="product-detail-review-textarea"
                    required
                  />
                  {reviewError && <p className="product-detail-review-error">{reviewError}</p>}
                  {reviewSuccess && <p className="product-detail-review-success">Yorumunuz eklendi!</p>}
                  <button type="submit" disabled={submittingReview || !reviewText.trim()} className="product-detail-review-submit">
                    {submittingReview ? 'Gönderiliyor...' : 'Yorum Gönder'}
                  </button>
                </form>
              ) : (
                <p className="product-detail-review-login-hint">
                  Yorum yapmak için <Link to="/login">giriş yapın</Link>.
                </p>
              )}

              {reviews.length === 0 ? (
                <p className="product-detail-reviews-empty">Henüz yorum yok. İlk yorumu siz yapın!</p>
              ) : (
                <ul className="product-detail-reviews-list">
                  {reviews.map((r) => {
                    const isOwn = user?.id === r.userId
                    return (
                      <li key={r.id} className="product-detail-review-item">
                        <div className="product-detail-review-item__header">
                          <div className="product-detail-review-item__left">
                            <div className="product-detail-review-item__stars">
                              {[1,2,3,4,5].map((i) => (
                                <Star key={i} size={13} fill={i <= r.rating ? '#f59e0b' : 'none'} stroke={i <= r.rating ? '#f59e0b' : '#e2e8f0'} />
                              ))}
                            </div>
                            <span className="product-detail-review-item__name">
                              {user?.id === r.userId ? `${user.firstName} ${user.lastName}` : 'Kullanıcı'}
                            </span>
                          </div>
                          <div className="product-detail-review-item__right">
                            <span className="product-detail-review-item__date">
                              {new Date(r.createdAt).toLocaleDateString('tr-TR')}
                            </span>
                            {isOwn && (
                              <button
                                onClick={() => handleDeleteReview(r.id)}
                                className="product-detail-review-delete-btn"
                                aria-label="Yorumu sil"
                              >
                                <Trash2 size={14} />
                              </button>
                            )}
                          </div>
                        </div>
                        <p className="product-detail-review-item__text">{r.commentText}</p>
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  )
}

function StatusBadge({ inStock, variantInStock }: { inStock: boolean; variantInStock: boolean }) {
  const ok = inStock && variantInStock
  return (
    <span className={`product-detail-stock-badge product-detail-stock-badge--${ok ? 'ok' : 'bad'}`}>
      {ok ? 'Stokta Var' : 'Stokta Yok'}
    </span>
  )
}

function QtyButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button onClick={onClick} className="product-detail-qty-btn">
      {label}
    </button>
  )
}

function TrustBadge({ icon, label, sub }: { icon: React.ReactNode; label: string; sub: string }) {
  return (
    <div className="product-detail-trust-badge">
      <span className="product-detail-trust-badge__icon">{icon}</span>
      <div>
        <div className="product-detail-trust-badge__label">{label}</div>
        <div className="product-detail-trust-badge__sub">{sub}</div>
      </div>
    </div>
  )
}

function DetailSkeleton() {
  return (
    <>
      <div className="product-detail-skeleton">
        <div className="product-detail-skeleton__grid">
          <div className="product-detail-skeleton__shimmer product-detail-skeleton__image" />
          <div className="product-detail-skeleton__rows">
            {[24, 120, 32, 80, 56, 48, 44].map((h, i) => (
              <div key={i} className="product-detail-skeleton__shimmer" style={{ height: `${h}px` }} />
            ))}
          </div>
        </div>
      </div>
      <div className="product-detail-skeleton__shimmer product-detail-skeleton__bottom" />
    </>
  )
}



