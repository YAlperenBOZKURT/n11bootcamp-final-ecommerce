
import { apiClient } from '../lib/apiClient'

export interface CartItemResponse {
  itemId: string
  productId: number
  variantId: number | null
  productName: string
  unitPrice: number
  quantity: number
  lineTotal: number
  // Selected product attributes (size, color, etc.) - stored on the frontend. 
  attributes?: Record<string, string>
}

export interface CartResponse {
  userId: string
  items: CartItemResponse[]
  totalAmount: number
}

export interface AddCartItemRequest {
  productId: number
  variantId?: number | null
  productName: string
  unitPrice: number
  quantity: number
  // Stored only in guest cart; not sent to backend.
  attributes?: Record<string, string>
}

export interface CouponPreviewResponse {
  code: string
  cartTotal: number
  discountAmount: number
  finalAmount: number
}

export const cartService = {
  async getCart(): Promise<CartResponse> {
    return apiClient.get<CartResponse>('/api/cart')
  },

  async addItem(payload: AddCartItemRequest): Promise<CartResponse> {
    return apiClient.post<CartResponse>('/api/cart/items', payload)
  },

  async updateQuantity(itemId: string, quantity: number): Promise<CartResponse> {
    return apiClient.patch<CartResponse>(`/api/cart/items/${itemId}`, { quantity })
  },

  async removeItem(itemId: string): Promise<CartResponse> {
    return apiClient.delete<CartResponse>(`/api/cart/items/${itemId}`)
  },

  async clearCart(): Promise<void> {
    return apiClient.delete<void>('/api/cart')
  },

  async previewCoupon(code: string): Promise<CouponPreviewResponse> {
    return apiClient.post<CouponPreviewResponse>('/api/cart/coupon/preview', { code })
  },
}


