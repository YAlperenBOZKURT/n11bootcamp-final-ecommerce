import { apiClient } from '../lib/apiClient'

export interface ReviewResponse {
  id: number
  productId: number
  userId: number
  rating: number
  commentText: string
  createdAt: string
  updatedAt: string
}

export interface CreateReviewRequest {
  productId: number
  rating: number
  commentText: string
}

export const reviewService = {
  getByProductId(productId: number): Promise<ReviewResponse[]> {
    return apiClient.get<ReviewResponse[]>(`/api/reviews/products/${productId}`)
  },

  create(request: CreateReviewRequest): Promise<ReviewResponse> {
    return apiClient.post<ReviewResponse>('/api/reviews', request)
  },

  delete(id: number): Promise<void> {
    return apiClient.delete<void>(`/api/reviews/${id}`)
  },
}
