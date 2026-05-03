import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import './Breadcrumb.css'

type BreadcrumbItem = {
  label: string
  to?: string
}

type Props = {
  items: BreadcrumbItem[]
  className?: string
}

export default function Breadcrumb({ items, className }: Props) {
  return (
    <nav aria-label="Breadcrumb" className={`breadcrumb${className ? ` ${className}` : ''}`}>
      {items.map((item, idx) => {
        const isLast = idx === items.length - 1
        return (
          <div key={`${item.label}-${idx}`} className="breadcrumb-item">
            {item.to && !isLast ? (
              <Link to={item.to} className="breadcrumb-link">
                {item.label}
              </Link>
            ) : (
              <span className={`breadcrumb-text${isLast ? ' breadcrumb-text--last' : ''}`}>
                {item.label}
              </span>
            )}
            {!isLast && <ChevronRight size={14} />}
          </div>
        )
      })}
    </nav>
  )
}
