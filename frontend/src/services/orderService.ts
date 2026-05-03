import { apiClient } from '../lib/apiClient'

export interface OrderItemRequest {
  productId: number
  variantId?: number | null
  quantity: number
}

export interface CardRequest {
  cardHolderName: string
  cardNumber: string
  expireMonth: string
  expireYear: string
  cvc: string
}

export interface PlaceOrderRequest {
  items: OrderItemRequest[]
  card: CardRequest
  couponCode?: string | null
}

export interface OrderItemResponse {
  id: number
  productId: number
  productName: string
  variantId: number | null
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface OrderResponse {
  id: number
  orderNumber: string
  userId: number
  userEmail: string
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED' | string
  totalAmount: number
  discountAmount: number | null
  finalAmount: number
  couponCode: string | null
  failReason: string | null
  items: OrderItemResponse[]
  createdAt: string
  updatedAt: string
}

export const orderService = {
  async placeOrder(payload: PlaceOrderRequest): Promise<OrderResponse> {
    return apiClient.post<OrderResponse>('/api/orders', payload)
  },

  async getMyOrders(): Promise<OrderResponse[]> {
    return apiClient.get<OrderResponse[]>('/api/orders/my')
  },

  async getByOrderNumber(orderNumber: string): Promise<OrderResponse> {
    return apiClient.get<OrderResponse>(`/api/orders/${orderNumber}`)
  },

  async cancelOrder(orderNumber: string): Promise<OrderResponse> {
    return apiClient.post<OrderResponse>(`/api/orders/${orderNumber}/cancel`)
  },
}
