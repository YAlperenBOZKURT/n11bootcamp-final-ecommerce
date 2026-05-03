import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import Breadcrumb from '../../shared/ui/Breadcrumb'
import { adminService } from '../../services/adminService'
import type { OrderResponse } from '../../services/orderService'
import type { Page } from '../../services/productService'
import './admin-shared.css'
import './AdminOrdersPage.css'

const STATUS_COLORS: Record<string, { tone: 'confirmed' | 'pending' | 'cancelled' | 'failed' | 'default'; label: string }> = {
  CONFIRMED: { tone: 'confirmed', label: 'Onaylandı' },
  PENDING: { tone: 'pending', label: 'Bekliyor' },
  CANCELLED: { tone: 'cancelled', label: 'İptal' },
  FAILED: { tone: 'failed', label: 'Başarısız' },
}

function StatusBadge({ status }: { status: string }) {
  const s = STATUS_COLORS[status] ?? { tone: 'default' as const, label: status }
  return <span className={`orders-status-badge orders-status-badge--${s.tone}`}>{s.label}</span>
}

export default function AdminOrdersPage() {
  const [data, setData] = useState<Page<OrderResponse> | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<number | null>(null)

  async function load(p: number) {
    setLoading(true)
    setError(null)
    try {
      const res = await adminService.getAllOrders(p)
      setData(res)
    } catch {
      setError('Siparişler yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(page)
  }, [page])

  const totalPages = data?.totalPages ?? 0

  return (
    <div className="admin-page">
      <Breadcrumb items={[{ label: 'Admin', to: '/admin' }, { label: 'Siparişler' }]} className="breadcrumb--mb-12" />

      <div className="admin-page__header">
        <div>
          <h1 className="admin-page__title">Tüm Siparişler</h1>
          {data && <p className="admin-page__subtitle">Toplam {data.totalElements} sipariş</p>}
        </div>
        <button onClick={() => load(page)} className="admin-icon-btn" title="Yenile">
          <RefreshCw size={16} />
        </button>
      </div>

      {error && <div className="admin-error">{error}</div>}

      {loading ? (
        <div className="admin-center-state"><div className="admin-spinner-sm" /></div>
      ) : (
        <>
          <div className="admin-table-card">
            <table className="admin-table">
              <thead>
                <tr>
                  {['Sipariş No', 'Kullanıcı', 'Tarih', 'Tutar', 'Durum', ''].map((h) => (
                    <th key={h} className="admin-th">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data?.content.map((order) => (
                  <>
                    <tr key={order.id}>
                      <td className="admin-td"><span className="orders-order-number">{order.orderNumber}</span></td>
                      <td className="admin-td">{order.userEmail}</td>
                      <td className="admin-td">{new Date(order.createdAt).toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })}</td>
                      <td className="admin-td"><strong>₺{Number(order.finalAmount ?? order.totalAmount).toFixed(2)}</strong></td>
                      <td className="admin-td"><StatusBadge status={order.status} /></td>
                      <td className="admin-td">
                        <button onClick={() => setExpanded(expanded === order.id ? null : order.id)} className="admin-link-btn">
                          {expanded === order.id ? 'Gizle' : 'Detay'}
                        </button>
                      </td>
                    </tr>
                    {expanded === order.id && (
                      <tr key={`${order.id}-detail`}>
                        <td colSpan={6} className="orders-detail-cell">
                          <div className="orders-expanded-detail">
                            {order.failReason && (
                              <p className="orders-expanded-detail__error">
                                <strong>Hata:</strong> {order.failReason}
                              </p>
                            )}
                            {order.couponCode && (
                              <p className="orders-expanded-detail__coupon">
                                <strong>Kupon:</strong> {order.couponCode} (İndirim: ₺{Number(order.discountAmount ?? 0).toFixed(2)})
                              </p>
                            )}
                            <table className="orders-inner-table">
                              <thead>
                                <tr>
                                  <th className="orders-inner-th-left">Ürün</th>
                                  <th className="orders-inner-th-center">Adet</th>
                                  <th className="orders-inner-th-right">Birim Fiyat</th>
                                  <th className="orders-inner-th-right">Toplam</th>
                                </tr>
                              </thead>
                              <tbody>
                                {order.items?.map((item) => (
                                  <tr key={item.id}>
                                    <td className="orders-inner-td">{item.productName}</td>
                                    <td className="orders-inner-td--center">{item.quantity}</td>
                                    <td className="orders-inner-td--right">₺{Number(item.unitPrice).toFixed(2)}</td>
                                    <td className="orders-inner-td--total">₺{Number(item.lineTotal).toFixed(2)}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>

            {!data?.content?.length && <div className="admin-center-state">Henüz sipariş yok.</div>}
          </div>

          {totalPages > 1 && (
            <div className="admin-pagination">
              <button onClick={() => setPage((p) => p - 1)} disabled={page === 0} className={page === 0 ? 'admin-page-btn admin-page-btn--disabled' : 'admin-page-btn'}>
                <ChevronLeft size={16} />
              </button>
              <span className="admin-pagination__label">Sayfa {page + 1} / {totalPages}</span>
              <button onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1} className={page >= totalPages - 1 ? 'admin-page-btn admin-page-btn--disabled' : 'admin-page-btn'}>
                <ChevronRight size={16} />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
