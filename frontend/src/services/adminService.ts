
import { apiClient } from '../lib/apiClient'
import type { ProductResponse, Page, CategoryResponse } from './productService'
import type { OrderResponse } from './orderService'

export interface AdminProductRequest {
  name: string
  description: string
  brand: string
  categoryId: number
  imageUrls?: string[]
}

export interface StockResponse {
  id: number
  productId: number
  variantId?: number
  quantity: number
  reservedQuantity: number
  availableQuantity: number
}

export interface VariantRequest {
  sku: string
  attributes: Record<string, string>
  // Variant sale price (required, > 0)
  price: number
  // Discount rate (0-100) - optional, sends null when left empty
  discountRate?: number | null
  discountStartAt?: string | null
  discountEndAt?: string | null
}

export interface VariantResponse {
  id: number
  productId: number
  sku: string
  attributes: Record<string, string>
  price: number
  originalPrice: number | null
  discountRate: number | null
  effectivePrice: number
  discountStartAt: string | null
  discountEndAt: string | null
  status: string
}

export interface StockMovementResponse {
  id: number
  type: 'ADD' | 'RESERVE' | 'RELEASE' | 'CONFIRM'
  quantity: number
  note?: string
  createdAt: string
}

export interface AdminSearchParams {
  keyword?: string
  categoryId?: number
  status?: string
  page?: number
  size?: number
  sort?: string
}

export interface CategoryRequest {
  name: string
  parentId?: number | null
}

export const adminService = {

  searchProducts(params: AdminSearchParams): Promise<Page<ProductResponse>> {
    const q = new URLSearchParams()
    if (params.keyword)    q.set('keyword', params.keyword)
    if (params.categoryId) q.set('categoryId', String(params.categoryId))
    if (params.status)     q.set('status', params.status)
    if (params.page != null) q.set('page', String(params.page))
    if (params.size != null) q.set('size', String(params.size))
    if (params.sort)       q.set('sort', params.sort)
    // Admin endpoint: returns all statuses (ACTIVE + PASSIVE)
    return apiClient.get<Page<ProductResponse>>(`/api/products/admin/search?${q}`)
  },

  getProduct(id: number): Promise<ProductResponse> {
    return apiClient.get<ProductResponse>(`/api/products/${id}`)
  },

  createProduct(data: AdminProductRequest): Promise<ProductResponse> {
    return apiClient.post<ProductResponse>('/api/products', data)
  },

  updateProduct(id: number, data: AdminProductRequest): Promise<ProductResponse> {
    return apiClient.put<ProductResponse>(`/api/products/${id}`, data)
  },

  deleteProduct(id: number): Promise<void> {
    return apiClient.delete<void>(`/api/products/${id}`)
  },

  getCategories(): Promise<CategoryResponse[]> {
    return apiClient.get<CategoryResponse[]>('/api/products/categories')
  },

  createCategory(data: CategoryRequest): Promise<CategoryResponse> {
    return apiClient.post<CategoryResponse>('/api/products/categories', data)
  },

  updateCategory(id: number, data: CategoryRequest): Promise<CategoryResponse> {
    return apiClient.put<CategoryResponse>(`/api/products/categories/${id}`, data)
  },

  deleteCategory(id: number): Promise<void> {
    return apiClient.delete<void>(`/api/products/categories/${id}`)
  },

  /**
   * Uploads product images to MinIO (max 3 total)
   * Sent as multipart/form-data with FormData
   */
  uploadProductImages(productId: number, files: File[]): Promise<ProductResponse> {
    const formData = new FormData()
    files.forEach((f) => formData.append('files', f))
    return apiClient.post<ProductResponse>(`/api/products/${productId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /**
   * Stok ekle.
   * If total stock becomes > 0 after the added amount, stock-service publishes via RabbitMQ
   * a REPLENISHED event to product-service -> product becomes ACTIVE
   * If stock drops to 0, DEPLETED event -> PASSIVE
   */
  getStock(productId: number): Promise<StockResponse> {
    return apiClient.get<StockResponse>(`/api/stocks/${productId}`)
  },

  addStock(productId: number, quantity: number, note?: string): Promise<StockResponse> {
    return apiClient.post<StockResponse>(`/api/stocks/${productId}/add`, { quantity, note })
  },

  /**
   * Decrease stock: reserve first, then confirm immediately -> permanent deduction
   * This keeps RESERVE + CONFIRM entries in stock movement logs
   */
  async reduceStock(productId: number, quantity: number, note?: string): Promise<StockResponse> {
    await apiClient.post<StockResponse>(`/api/stocks/${productId}/reserve`, { quantity, note })
    return apiClient.post<StockResponse>(`/api/stocks/${productId}/confirm`, { quantity, note })
  },

  getStockMovements(productId: number): Promise<StockMovementResponse[]> {
    return apiClient.get<StockMovementResponse[]>(`/api/stocks/${productId}/movements`)
  },

  getVariants(productId: number): Promise<VariantResponse[]> {
    return apiClient.get<VariantResponse[]>(`/api/products/${productId}/variants`)
  },

  createVariant(productId: number, data: VariantRequest): Promise<VariantResponse> {
    return apiClient.post<VariantResponse>(`/api/products/${productId}/variants`, data)
  },

  updateVariant(productId: number, variantId: number, data: VariantRequest): Promise<VariantResponse> {
    return apiClient.put<VariantResponse>(`/api/products/${productId}/variants/${variantId}`, data)
  },

  deleteVariant(productId: number, variantId: number): Promise<void> {
    return apiClient.delete<void>(`/api/products/${productId}/variants/${variantId}`)
  },

  initVariantStock(productId: number, variantId: number): Promise<void> {
    return apiClient.post<void>(`/api/stocks/${productId}/variants/${variantId}/init`, {})
  },

  getAllVariantStocks(productId: number): Promise<StockResponse[]> {
    return apiClient.get<StockResponse[]>(`/api/stocks/${productId}/variants`)
  },

  getVariantStock(variantId: number): Promise<StockResponse> {
    return apiClient.get<StockResponse>(`/api/stocks/variants/${variantId}`)
  },

  addVariantStock(variantId: number, quantity: number, note?: string): Promise<StockResponse> {
    return apiClient.post<StockResponse>(`/api/stocks/variants/${variantId}/add`, { quantity, note })
  },

  async reduceVariantStock(variantId: number, quantity: number, note?: string): Promise<StockResponse> {
    await apiClient.post<StockResponse>(`/api/stocks/variants/${variantId}/reserve`, { quantity, note })
    return apiClient.post<StockResponse>(`/api/stocks/variants/${variantId}/confirm`, { quantity, note })
  },

  getVariantStockMovements(variantId: number): Promise<StockMovementResponse[]> {
    return apiClient.get<StockMovementResponse[]>(`/api/stocks/variants/${variantId}/movements`)
  },

  getAllOrders(page = 0, size = 20): Promise<Page<OrderResponse>> {
    return apiClient.get<Page<OrderResponse>>(`/api/orders/admin?page=${page}&size=${size}&sort=createdAt,desc`)
  },

  getAllUsers(page = 0, size = 20): Promise<Page<UserResponse>> {
    return apiClient.get<Page<UserResponse>>(`/api/users/admin?page=${page}&size=${size}&sort=createdAt,desc`)
  },

  freezeUser(id: number): Promise<void> {
    return apiClient.patch<void>(`/api/users/admin/${id}/freeze`)
  },

  unfreezeUser(id: number): Promise<void> {
    return apiClient.patch<void>(`/api/users/admin/${id}/unfreeze`)
  },

  deleteUser(id: number): Promise<void> {
    return apiClient.delete<void>(`/api/users/admin/${id}`)
  },
}

export interface UserResponse {
  id: number
  email: string
  firstName: string
  lastName: string
  role: string
  status: string
  createdAt: string
}


