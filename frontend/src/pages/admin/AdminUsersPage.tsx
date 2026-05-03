import './admin-shared.css'
import './AdminUsersPage.css'
import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight, RefreshCw, Trash2, Lock, Unlock } from 'lucide-react'
import Breadcrumb from '../../shared/ui/Breadcrumb'
import { adminService, type UserResponse } from '../../services/adminService'
import type { Page } from '../../services/productService'

const STATUS_MAP: Record<string, { tone: 'active' | 'frozen' | 'deleted' | 'default'; label: string }> = {
  ACTIVE: { tone: 'active', label: 'Aktif' },
  FROZEN: { tone: 'frozen', label: 'Donduruldu' },
  DELETED: { tone: 'deleted', label: 'Silindi' },
}

function StatusBadge({ status }: { status: string }) {
  const s = STATUS_MAP[status] ?? { tone: 'default' as const, label: status }
  return <span className={`users-status-badge users-status-badge--${s.tone}`}>{s.label}</span>
}

export default function AdminUsersPage() {
  const [data, setData] = useState<Page<UserResponse> | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionId, setActionId] = useState<number | null>(null)

  async function load(p: number) {
    setLoading(true)
    setError(null)
    try {
      const res = await adminService.getAllUsers(p)
      setData(res)
    } catch {
      setError('Kullanıcılar yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(page)
  }, [page])

  async function handleFreeze(user: UserResponse) {
    if (actionId) return
    const isFrozen = user.status === 'FROZEN'
    setActionId(user.id)
    try {
      if (isFrozen) {
        await adminService.unfreezeUser(user.id)
      } else {
        await adminService.freezeUser(user.id)
      }
      await load(page)
    } catch {
      setError(`${isFrozen ? 'Aktifleştirme' : 'Dondurma'} başarısız.`)
    } finally {
      setActionId(null)
    }
  }

  async function handleDelete(user: UserResponse) {
    if (actionId) return
    if (!confirm(`${user.email} kullanıcısı silinsin mi? Bu işlem geri alınamaz.`)) return
    setActionId(user.id)
    try {
      await adminService.deleteUser(user.id)
      await load(page)
    } catch {
      setError('Silme işlemi başarısız.')
    } finally {
      setActionId(null)
    }
  }

  const totalPages = data?.totalPages ?? 0

  return (
    <div className="admin-page">
      <Breadcrumb items={[{ label: 'Admin', to: '/admin' }, { label: 'Kullanıcılar' }]} className="breadcrumb--mb-12" />

      <div className="admin-page__header">
        <div>
          <h1 className="admin-page__title">Kullanıcılar</h1>
          {data && <p className="admin-page__subtitle">Toplam {data.totalElements} kullanıcı</p>}
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
                  {['#', 'Ad Soyad', 'E-posta', 'Rol', 'Durum', 'Kayıt Tarihi', 'İşlemler'].map((h) => (
                    <th key={h} className="admin-th">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data?.content.map((user) => {
                  const busy = actionId === user.id
                  return (
                    <tr key={user.id} className={user.status === 'DELETED' ? 'users-deleted-row' : undefined}>
                      <td className="admin-td"><span className="admin-td__muted">{user.id}</span></td>
                      <td className="admin-td"><strong>{user.firstName} {user.lastName}</strong></td>
                      <td className="admin-td"><span>{user.email}</span></td>
                      <td className="admin-td">
                        <span className={user.role === 'ADMIN' ? 'users-role-badge users-role-badge--admin' : 'users-role-badge users-role-badge--user'}>
                          {user.role}
                        </span>
                      </td>
                      <td className="admin-td"><StatusBadge status={user.status} /></td>
                      <td className="admin-td">{new Date(user.createdAt).toLocaleDateString('tr-TR')}</td>
                      <td className="admin-td">
                        {user.status !== 'DELETED' && (
                          <div className="users-actions">
                            <button
                              onClick={() => handleFreeze(user)}
                              disabled={busy}
                              title={user.status === 'FROZEN' ? 'Aktifleştir' : 'Dondur'}
                              className={`users-action-btn ${user.status === 'FROZEN' ? 'users-action-btn--unfreeze' : 'users-action-btn--freeze'}`}
                            >
                              {user.status === 'FROZEN' ? <Unlock size={14} /> : <Lock size={14} />}
                            </button>
                            <button
                              onClick={() => handleDelete(user)}
                              disabled={busy}
                              title="Sil"
                              className="users-action-btn users-action-btn--delete"
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>

            {!data?.content?.length && <div className="admin-center-state">Henüz kullanıcı yok.</div>}
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
