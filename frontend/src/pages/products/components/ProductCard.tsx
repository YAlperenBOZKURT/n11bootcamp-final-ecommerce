import { Link } from 'react-router-dom'
import type { ProductResponse } from '../../../services/productService'
import { ShoppingCart } from 'lucide-react'
import './ProductCard.css'

interface ProductCardProps {
  product: ProductResponse
}

export default function ProductCard({ product }: ProductCardProps) {
  const displayPrice = product.priceFrom != null
    ? product.priceFrom.toLocaleString('tr-TR')
    : null

  return (
    <Link to={`/products/${product.id}`} className="product-card-link">
      <article className="product-card">
        <div className="product-card__image-wrapper">
          <img
            src={product.imageUrl ?? undefined}
            alt={product.name}
            className="product-card__image"
            loading="lazy"
          />

          {!product.inStock && (
            <div className="product-card__oos-overlay">
              <span className="product-card__oos-badge">Stokta Yok</span>
            </div>
          )}

          {product.inStock && (
            <button aria-label="Sepete ekle" className="card-cart-btn">
              <ShoppingCart size={14} />
              İncele
            </button>
          )}
        </div>
        <div className="product-card__content">
          <span className="product-card__brand">{product.brand}</span>

          <h3 className="product-card__name">{product.name}</h3>

          <div className="product-card__price-area">
            {displayPrice != null ? (
              <span className="product-card__price">{displayPrice} ₺'den</span>
            ) : (
              <span className="product-card__no-price">Fiyat için seçin</span>
            )}
          </div>
        </div>
      </article>
    </Link>
  )
}

