import { ChevronDown, ChevronUp, X } from 'lucide-react'
import { useState } from 'react'
import type { CategoryResponse, FilterOption } from '../../../services/productService'
import './FilterSidebar.css'

interface FilterSidebarProps {
  categories: CategoryResponse[]
  selectedCategoryId: number | null
  filterOptions: FilterOption[]
  activeFilters: Record<string, string>
  onCategorySelect: (id: number | null) => void
  onFilterChange: (key: string, value: string) => void
  onClearAll: () => void
}

export default function FilterSidebar({
  categories,
  selectedCategoryId,
  filterOptions,
  activeFilters,
  onCategorySelect,
  onClearAll,
  onFilterChange,
}: FilterSidebarProps) {
  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>({})

  const toggleSection = (key: string) => {
    setExpandedSections((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const isSectionExpanded = (key: string) => expandedSections[key] !== false

  const activeFilterCount = Object.keys(activeFilters).length + (selectedCategoryId ? 1 : 0)

  return (
    <aside className="filter-sidebar">
      <div className="filter-sidebar__header">
        <span className="filter-sidebar__title">
          Filtreler
          {activeFilterCount > 0 && (
            <span className="filter-sidebar__count-badge">{activeFilterCount}</span>
          )}
        </span>
        {activeFilterCount > 0 && (
          <button onClick={onClearAll} className="filter-sidebar__clear-btn">
            <X size={13} />
            Temizle
          </button>
        )}
      </div>
      <FilterSection
        title="Kategori"
        isExpanded={isSectionExpanded('category')}
        onToggle={() => toggleSection('category')}
      >
        <FilterRow
          label="Tüm Ürünler"
          selected={selectedCategoryId === null}
          onClick={() => onCategorySelect(null)}
          type="radio"
        />
        {categories.map((cat) => (
          <FilterRow
            key={cat.id}
            label={cat.name}
            selected={selectedCategoryId === cat.id}
            onClick={() => onCategorySelect(cat.id)}
            type="radio"
          />
        ))}
      </FilterSection>

      {selectedCategoryId && filterOptions.length > 0 && (
        <>
          <div className="filter-sidebar__divider" />
          {filterOptions.map((filter) => (
            <FilterSection
              key={filter.key}
              title={filter.label}
              isExpanded={isSectionExpanded(filter.key)}
              onToggle={() => toggleSection(filter.key)}
            >
              {filter.values.map((val) => (
                <FilterRow
                  key={val}
                  label={val}
                  selected={activeFilters[filter.key] === val}
                  onClick={() => onFilterChange(filter.key, val)}
                  type="checkbox"
                />
              ))}
            </FilterSection>
          ))}
        </>
      )}
    </aside>
  )
}

function FilterSection({
  title,
  isExpanded,
  onToggle,
  children,
}: {
  title: string
  isExpanded: boolean
  onToggle: () => void
  children: React.ReactNode
}) {
  return (
    <div className="filter-section">
      <button onClick={onToggle} className="filter-section__toggle">
        {title}
        {isExpanded ? <ChevronUp size={15} color="#94a3b8" /> : <ChevronDown size={15} color="#94a3b8" />}
      </button>
      {isExpanded && (
        <div className="filter-section__body">
          {children}
        </div>
      )}
    </div>
  )
}

function FilterRow({
  label,
  selected,
  onClick,
  type,
}: {
  label: string
  selected: boolean
  onClick: () => void
  type: 'radio' | 'checkbox'
}) {
  return (
    <button
      onClick={onClick}
      className={`filter-row${selected ? ' filter-row--selected' : ''}`}
    >
      <span
        className={`filter-row__indicator filter-row__indicator--${type} ${selected ? 'filter-row__indicator--checked' : 'filter-row__indicator--unchecked'}`}
      >
        {selected && (
          <span className={`filter-row__indicator-inner--${type}`} />
        )}
      </span>
      <span className={`filter-row__label ${selected ? 'filter-row__label--selected' : 'filter-row__label--unselected'}`}>
        {label}
      </span>
    </button>
  )
}


