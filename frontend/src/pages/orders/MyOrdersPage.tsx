import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Package, ChevronDown, ChevronUp, XCircle } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { orderService, type OrderResponse } from '../../services/orderService'
import { useAuth } from '../../context/AuthContext'
import './MyOrdersPage.css'

const STATUS_MAP: Record<string, { bg: string; color: string; label: string }> = {
  CONFIRMED: { bg: '#dcfce7', color: '#15803d', label: 'Onaylandı' },
  PENDING:   { bg: '#fef9c3', color: '#a16207', label: 'Bekliyor' },
  CANCELLED: { bg: '#fee2e2', color: '#b91c1c', label: 'İptal Edildi' },
  FAILED:    { bg: '#fce7f3', color: '#9d174d', label: 'Başarısız' },
}

function StatusBadge({ status }: { status: string }) {
  const s = STATUS_MAP[status] ?? { bg: '#f1f5f9', color: '#475569', label: status }
  return (
    <span className="order-status-badge" style={{ background: s.bg, color: s.color }}>
      {s.label}
    </span>
  )
}

export default function MyOrdersPage() {
  const { user } = useAuth()
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<number | null>(null)
  const [cancelling, setCancelling] = useState<number | null>(null)

  useEffect(() => {
    orderService.getMyOrders()
      .then(setOrders)
      .catch(() => setError('Siparişler yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleCancel(order: OrderResponse) {
    if (cancelling) return
    if (!confirm('Bu siparişi iptal etmek istediğinizden emin misiniz?')) return
    setCancelling(order.id)
    try {
      const updated = await orderService.cancelOrder(order.orderNumber)
      setOrders((prev) => prev.map((o) => (o.id === updated.id ? updated : o)))
    } catch {
      setError('Sipariş iptal edilemedi.')
    } finally {
      setCancelling(null)
    }
  }

  if (!user) {
    return (
      <div className="orders-page">
        <SiteHeader />
        <div className="orders-login-gate">
          <p className="orders-login-gate__text">Siparişlerinizi görmek için <Link to="/login" className="orders-login-gate__link">giriş yapın</Link>.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="orders-page">
      <SiteHeader />
      <main className="orders-main">
        <h1 className="orders-title">Siparişlerim</h1>

        {error && <div className="orders-error">{error}</div>}

        {loading ? (
          <div className="orders-loading">
            <div className="orders-spinner" />
          </div>
        ) : orders.length === 0 ? (
          <div className="orders-empty">
            <Package size={48} color="#cbd5e1" className="orders-empty__icon" />
            <p className="orders-empty__text">Henüz siparişiniz yok.</p>
            <Link to="/products" className="orders-empty__cta">
              Alışverişe Başla
            </Link>
          </div>
        ) : (
          <div className="orders-list">
            {orders.map((order) => {
              const isOpen = expanded === order.id
              const canCancel = order.status === 'PENDING'
              return (
                <div key={order.id} className="order-card">
                  <div className="order-card__header">
                    <div className="order-card__field">
                      <p className="order-card__field-label">Sipariş No</p>
                      <p className="order-card__order-number">{order.orderNumber}</p>
                    </div>
                    <div className="order-card__date-field">
                      <p className="order-card__field-label">Tarih</p>
                      <p className="order-card__field-value">{new Date(order.createdAt).toLocaleDateString('tr-TR')}</p>
                    </div>
                    <div className="order-card__amount-field">
                      <p className="order-card__field-label">Tutar</p>
                      <p className="order-card__amount">₺{Number(order.finalAmount ?? order.totalAmount).toFixed(2)}</p>
                    </div>
                    <StatusBadge status={order.status} />
                    <div className="order-card__actions">
                      {canCancel && (
                        <button
                          onClick={() => handleCancel(order)}
                          disabled={cancelling === order.id}
                          className="order-card__cancel-btn">
                          <XCircle size={14} />
                          {cancelling === order.id ? 'İptal ediliyor...' : 'İptal Et'}
                        </button>
                      )}
                      <button
                        onClick={() => setExpanded(isOpen ? null : order.id)}
                        className="order-card__detail-btn">
                        {isOpen ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                        {isOpen ? 'Gizle' : 'Detaylar'}
                      </button>
                    </div>
                  </div>
                  {isOpen && (
                    <div className="order-card__detail">
                      {order.failReason && (
                        <p className="order-card__detail-error"><strong>Hata:</strong> {order.failReason}</p>
                      )}
                      {order.couponCode && (
                        <p className="order-card__detail-coupon">
                          <strong>Kupon:</strong> {order.couponCode} (İndirim: ₺{Number(order.discountAmount ?? 0).toFixed(2)})
                        </p>
                      )}
                      <div className="order-card__items">
                        {order.items?.map((item) => (
                          <div key={item.id} className="order-card__item-row">
                            <div>
                              <p className="order-card__item-name">{item.productName}</p>
                              <p className="order-card__item-sub">Birim: ₺{Number(item.unitPrice).toFixed(2)} × {item.quantity}</p>
                            </div>
                            <p className="order-card__item-total">₺{Number(item.lineTotal).toFixed(2)}</p>
                          </div>
                        ))}
                      </div>
                      <div className="order-card__detail-total">
                        <span className="order-card__detail-total-amount">Toplam: ₺{Number(order.finalAmount ?? order.totalAmount).toFixed(2)}</span>
                      </div>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </main>
    </div>
  )
}

