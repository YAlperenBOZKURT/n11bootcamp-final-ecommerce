

import { apiClient } from '../lib/apiClient'

export interface StockResponse {
  id: number
  productId: number
  variantId: number | null
  quantity: number
  reservedQuantity: number
  availableQuantity: number
}

export type ProductStatus = 'ACTIVE' | 'PASSIVE' | 'DELETED'
export type VariantStatus = 'ACTIVE' | 'PASSIVE'

export interface CategoryResponse {
  id: number
  name: string
  parentId: number | null
}

export interface VariantResponse {
  id: number
  productId: number
  sku: string
  attributes: Record<string, string>
  // Variant-specific sale price (discount already applied).
  price: number
  // Price before discount - null means no discount.
  originalPrice: number | null
  // Discount rate (0-100) - null means no discount.
  discountRate: number | null
  // effectivePrice = price (discount is already applied to price).
  effectivePrice: number
  discountStartAt: string | null
  discountEndAt: string | null
  // ACTIVE -> in stock and selectable. PASSIVE -> disabled.
  status: VariantStatus
}

export interface ProductResponse {
  id: number
  name: string
  description: string
  brand: string
  imageUrl: string | null
  imageUrls: string[]
  priceFrom: number | null
  inStock: boolean
  attributes: Record<string, string>
  status: ProductStatus
  category: CategoryResponse
  variants: VariantResponse[]
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface FilterOption {
  key: string
  label: string
  values: string[]
}

export interface ProductSearchParams {
  categoryId?: number
  keyword?: string
  attributes?: Record<string, string>
  page?: number
  size?: number
  sort?: string
}

export const productService = {

  getCategories(): Promise<CategoryResponse[]> {
    return apiClient.get<CategoryResponse[]>('/api/products/categories')
  },

  /**
   * Search/filter products with optional keyword, categoryId and attribute filters.
   * GET /api/products/search?keyword=&categoryId=&page=&size=&{attrKey}={attrVal}
   */
  searchProducts(params: ProductSearchParams): Promise<Page<ProductResponse>> {
    const query = new URLSearchParams()

    if (params.keyword) query.set('keyword', params.keyword)
    if (params.categoryId != null) query.set('categoryId', String(params.categoryId))
    if (params.page != null) query.set('page', String(params.page))
    if (params.size != null) query.set('size', String(params.size))
    if (params.sort) query.set('sort', params.sort)

    // Attribute filters are passed as individual query params (backend @RequestParam Map)
    if (params.attributes) {
      Object.entries(params.attributes).forEach(([key, val]) => query.set(key, val))
    }

    return apiClient.get<Page<ProductResponse>>(`/api/products/search?${query.toString()}`)
  },


  getProductById(id: number): Promise<ProductResponse> {
    return apiClient.get<ProductResponse>(`/api/products/${id}`)
  },


  getFilterOptions(categoryId: number): Promise<FilterOption[]> {
    return apiClient.get<FilterOption[]>(`/api/products/filters?categoryId=${categoryId}`)
  },


  getStock(productId: number): Promise<StockResponse> {
    return apiClient.get<StockResponse>(`/api/stocks/${productId}`)
  },
}


