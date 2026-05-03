import './ProductFormSection.css'

type Props = {
  title: string
  icon?: React.ReactNode
  action?: React.ReactNode
  children: React.ReactNode
}

export default function ProductFormSection({ title, icon, action, children }: Props) {
  return (
    <div className="product-form-section">
      <div className="product-form-section__header">
        <div className="product-form-section__heading">
          {icon}{title}
        </div>
        {action}
      </div>
      <div className="product-form-section__body">{children}</div>
    </div>
  )
}

