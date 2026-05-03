import { useEffect, useState } from 'react'
import { useParams, useLocation, Link } from 'react-router-dom'
import { CheckCircle, XCircle, Package, ShoppingBag, Clock } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { orderService, type OrderResponse } from '../../services/orderService'
import './OrderSuccessPage.css'

export default function OrderSuccessPage() {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const location = useLocation()
  const passedOrder = (location.state as { order?: OrderResponse } | null)?.order

  const [order, setOrder] = useState<OrderResponse | null>(passedOrder ?? null)
  const [loading, setLoading] = useState(!passedOrder)

  useEffect(() => {
    if (passedOrder || !orderNumber) return
    orderService.getByOrderNumber(orderNumber)
      .then(setOrder)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [orderNumber])

  if (loading) {
    return (
      <div className="order-success-page">
        <SiteHeader />
        <div className="order-success-loading">
          <div className="order-success-loading__spinner" />
          <p className="order-success-loading__text">Sipariş bilgisi yükleniyor...</p>
        </div>
      </div>
    )
  }

  const status = order?.status
  const isSuccess = status === 'CONFIRMED'
  const isFailed = status === 'FAILED' || status === 'CANCELLED'

  return (
    <div className="order-success-page">
      <SiteHeader />

      <main className="order-success-main">
        <div
          className="order-success-card"
          style={{ border: `1px solid ${isSuccess ? '#bbf7d0' : isFailed ? '#fecaca' : '#e2e8f0'}` }}
        >
          {isSuccess ? (
            <CheckCircle size={56} color="#22c55e" className="order-success-card__icon" />
          ) : isFailed ? (
            <XCircle size={56} color="#ef4444" className="order-success-card__icon" />
          ) : (
            <Clock size={56} color="#f59e0b" className="order-success-card__icon" />
          )}

          <h1 className="order-success-card__title">
            {isSuccess ? 'Siparişiniz Alındı!' : isFailed ? 'Ödeme Başarısız' : 'Sipariş Beklemede'}
          </h1>

          <p className="order-success-card__desc">
            {isSuccess
              ? 'Siparişiniz başarıyla oluşturuldu. Kargoya verildikten sonra bildirim alacaksınız.'
              : isFailed
              ? (order?.failReason ?? 'Ödeme işlemi tamamlanamadı. Lütfen tekrar deneyin.')
              : 'Siparişiniz işlemde, lütfen bekleyin.'}
          </p>

          {order && (
            <div
              className="order-success-card__number-badge"
              style={{
                background: isSuccess ? '#f0fdf4' : isFailed ? '#fef2f2' : '#fffbeb',
                border: `1px solid ${isSuccess ? '#bbf7d0' : isFailed ? '#fecaca' : '#fde68a'}`,
              }}
            >
              <p className="order-success-card__number-label">Sipariş Numarası</p>
              <p className="order-success-card__number-value">{order.orderNumber}</p>
            </div>
          )}
        </div>
        {order && (
          <div className="order-success-details">
            <h2 className="order-success-details__title">
              <Package size={16} color="#6366f1" /> Sipariş Detayları
            </h2>

            <div className="order-success-details__items">
              {order.items?.map((item) => (
                <div key={item.id} className="order-success-details__item">
                  <span className="order-success-details__item-name">
                    {item.productName} <span className="order-success-details__item-qty">× {item.quantity}</span>
                  </span>
                  <span className="order-success-details__item-price">₺{(item.lineTotal ?? item.unitPrice * item.quantity).toFixed(2)}</span>
                </div>
              ))}
            </div>

            <div className="order-success-details__totals">
              {order.discountAmount != null && order.discountAmount > 0 && (
                <div className="order-success-details__discount-row">
                  <span>İndirim</span>
                  <span>-₺{order.discountAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="order-success-details__total-row">
                <span>Toplam</span>
                <span className="order-success-details__total-amount">₺{(order.finalAmount ?? order.totalAmount).toFixed(2)}</span>
              </div>
            </div>
          </div>
        )}
        <div className="order-success-actions">
          {isFailed && (
            <Link to="/checkout" className="order-success-actions__primary-btn">
              Tekrar Dene
            </Link>
          )}
          <Link to="/products" className="order-success-actions__secondary-btn">
            <ShoppingBag size={16} /> Alışverişe Devam Et
          </Link>
        </div>
      </main>
    </div>
  )
}

