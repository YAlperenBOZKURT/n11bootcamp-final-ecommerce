import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SlidersHorizontal, X } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import FilterSidebar from './components/FilterSidebar'
import ProductCard from './components/ProductCard'
import { productService } from '../../services/productService'
import type { CategoryResponse, FilterOption, ProductResponse } from '../../services/productService'
import './ProductsPage.css'

const PAGE_SIZE = 20

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedCategoryId = searchParams.get('categoryId')
    ? Number(searchParams.get('categoryId'))
    : null
  const keyword = searchParams.get('keyword') ?? ''

  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [filterOptions, setFilterOptions] = useState<FilterOption[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [activeFilters, setActiveFilters] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)

  const asArray = <T,>(value: unknown): T[] => (Array.isArray(value) ? (value as T[]) : [])

  useEffect(() => {
    productService
      .getCategories()
      .then((res) => setCategories(asArray<CategoryResponse>(res)))
      .catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    async function updateFilters() {
      if (selectedCategoryId) {
        productService
          .getFilterOptions(selectedCategoryId)
          .then((res) => setFilterOptions(asArray<FilterOption>(res)))
          .catch(() => setFilterOptions([]))
      } else {
        setFilterOptions([])
      }
      setActiveFilters({})
      setCurrentPage(0)
    }
    void updateFilters()
  }, [selectedCategoryId])

  useEffect(() => {
    async function resetPage() {
      setCurrentPage(0)
    }
    void resetPage()
  }, [keyword])

  useEffect(() => {
    let active = true
    async function fetchProducts() {
      setLoading(true)
      try {
        const page = await productService.searchProducts({
          keyword: keyword || undefined,
          categoryId: selectedCategoryId ?? undefined,
          attributes: Object.keys(activeFilters).length > 0 ? activeFilters : undefined,
          page: currentPage,
          size: PAGE_SIZE,
        })
        if (!active) return
        setProducts(asArray<ProductResponse>(page?.content))
        setTotalElements(Number(page?.totalElements ?? 0))
        setTotalPages(Number(page?.totalPages ?? 0))
      } catch {
        if (active) {
          setProducts([])
          setTotalElements(0)
          setTotalPages(0)
        }
      } finally {
        if (active) setLoading(false)
      }
    }
    void fetchProducts()
    return () => { active = false }
  }, [keyword, selectedCategoryId, activeFilters, currentPage])

  const handleCategorySelect = (id: number | null) => {
    const next = new URLSearchParams()
    if (id) next.set('categoryId', String(id))
    if (keyword) next.set('keyword', keyword)
    setSearchParams(next)
    setCurrentPage(0)
  }

  const handleFilterChange = (key: string, value: string) => {
    setCurrentPage(0)
    setActiveFilters((prev) => {
      if (prev[key] === value) {
        const next = { ...prev }
        delete next[key]
        return next
      }
      return { ...prev, [key]: value }
    })
  }

  const handleClearKeyword = () => {
    const next = new URLSearchParams(searchParams)
    next.delete('keyword')
    setSearchParams(next)
    setCurrentPage(0)
  }

  const handleClearAll = () => {
    setSearchParams({})
    setActiveFilters({})
    setCurrentPage(0)
  }

  const selectedCategory = categories.find((c) => c.id === selectedCategoryId)
  const activeFilterCount = Object.keys(activeFilters).length + (selectedCategoryId ? 1 : 0) + (keyword ? 1 : 0)

  return (
    <div className="products-page">
      <SiteHeader />

      <div className="products-container">
        <div className="products-sidebar hidden lg:block">
          <FilterSidebar
            categories={categories}
            selectedCategoryId={selectedCategoryId}
            filterOptions={filterOptions}
            activeFilters={activeFilters}
            onCategorySelect={handleCategorySelect}
            onFilterChange={handleFilterChange}
            onClearAll={handleClearAll}
          />
        </div>
        <div className="products-main">
          <div className="products-header">
            <div>
              <h1 className="products-heading">
                {keyword
                  ? <>{`"${keyword}"`} <span className="products-heading__sub">araması</span></>
                  : selectedCategory
                  ? selectedCategory.name
                  : 'Tüm Ürünler'
                }
              </h1>
              {!loading && (
                <p className="products-count">
                  {totalElements} ürün bulundu
                  {keyword && <span className="products-count__es-badge"> • Elasticsearch ile arandı</span>}
                </p>
              )}
            </div>
            <button
              className="products-mobile-filter-btn lg:hidden"
              onClick={() => setMobileSidebarOpen(true)}
            >
              <SlidersHorizontal size={16} />
              Filtrele
              {activeFilterCount > 0 && (
                <span className="products-mobile-filter-badge">{activeFilterCount}</span>
              )}
            </button>
          </div>
          {activeFilterCount > 0 && (
            <div className="products-chips">
              {keyword && (
                <ActiveChip label={`Arama: "${keyword}"`} color="purple" onRemove={handleClearKeyword} />
              )}
              {selectedCategory && (
                <ActiveChip label={selectedCategory.name} onRemove={() => handleCategorySelect(null)} />
              )}
              {Object.entries(activeFilters).map(([key, val]) => {
                const opt = filterOptions.find((f) => f.key === key)
                return (
                  <ActiveChip
                    key={key}
                    label={`${opt?.label ?? key}: ${val}`}
                    onRemove={() => handleFilterChange(key, val)}
                  />
                )
              })}
            </div>
          )}
          {loading ? (
            <ProductSkeleton />
          ) : products.length === 0 ? (
            <EmptyState onClear={handleClearAll} />
          ) : (
            <>
              <div className="products-grid">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              {totalPages > 1 && (
                <Pagination
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={(p) => {
                    setCurrentPage(p)
                    window.scrollTo({ top: 0, behavior: 'smooth' })
                  }}
                />
              )}
            </>
          )}
        </div>
      </div>
      {mobileSidebarOpen && (
        <>
          <div className="mobile-sidebar-overlay" onClick={() => setMobileSidebarOpen(false)} />
          <div className="mobile-sidebar-drawer">
            <div className="mobile-sidebar-drawer__header">
              <span className="mobile-sidebar-drawer__title">Filtreler</span>
              <button onClick={() => setMobileSidebarOpen(false)} className="mobile-sidebar-drawer__close-btn">
                <X size={20} />
              </button>
            </div>
            <FilterSidebar
              categories={categories}
              selectedCategoryId={selectedCategoryId}
              filterOptions={filterOptions}
              activeFilters={activeFilters}
              onCategorySelect={(id) => { handleCategorySelect(id); setMobileSidebarOpen(false) }}
              onFilterChange={handleFilterChange}
              onClearAll={() => { handleClearAll(); setMobileSidebarOpen(false) }}
            />
          </div>
        </>
      )}
    </div>
  )
}

function ActiveChip({ label, onRemove, color = 'indigo' }: { label: string; onRemove: () => void; color?: 'indigo' | 'purple' }) {
  return (
    <span className={`active-chip active-chip--${color}`}>
      {label}
      <button onClick={onRemove} className="active-chip__remove-btn">
        <X size={12} />
      </button>
    </span>
  )
}

function ProductSkeleton() {
  return (
    <div className="products-grid">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="product-skeleton">
          <div className="product-skeleton__image" />
          <div className="product-skeleton__body">
            <div className="product-skeleton__line product-skeleton__line--sm" />
            <div className="product-skeleton__line product-skeleton__line--lg" />
            <div className="product-skeleton__line product-skeleton__line--md" />
            <div className="product-skeleton__line product-skeleton__line--price" />
          </div>
        </div>
      ))}
    </div>
  )
}

function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}: {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}) {
  const delta = 2
  const pages: (number | '...')[] = []

  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || (i >= currentPage - delta && i <= currentPage + delta)) {
      pages.push(i)
    } else if (pages[pages.length - 1] !== '...') {
      pages.push('...')
    }
  }

  return (
    <div className="products-pagination">
      <PageBtn onClick={() => onPageChange(currentPage - 1)} disabled={currentPage === 0} label="‹" />
      {pages.map((p, i) =>
        p === '...' ? (
          <span key={`dots-${i}`} className="products-pagination__dots">...</span>
        ) : (
          <PageBtn
            key={p}
            onClick={() => onPageChange(p)}
            active={p === currentPage}
            label={String(p + 1)}
          />
        )
      )}
      <PageBtn onClick={() => onPageChange(currentPage + 1)} disabled={currentPage === totalPages - 1} label="›" />
    </div>
  )
}

function PageBtn({
  onClick,
  disabled,
  active,
  label,
}: {
  onClick: () => void
  disabled?: boolean
  active?: boolean
  label: string
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`page-btn${active ? ' page-btn--active' : disabled ? ' page-btn--disabled' : ''}`}
    >
      {label}
    </button>
  )
}

function EmptyState({ onClear }: { onClear: () => void }) {
  return (
    <div className="products-empty">
      <div className="products-empty__emoji">🔍</div>
      <h2 className="products-empty__title">Ürün bulunamadı</h2>
      <p className="products-empty__subtitle">Seçtiğiniz filtreler için sonuç yok.</p>
      <button onClick={onClear} className="products-empty__btn">Tüm Ürünlere Dön</button>
    </div>
  )
}



