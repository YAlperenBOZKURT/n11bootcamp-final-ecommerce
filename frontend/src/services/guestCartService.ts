/**
 * Guest cart service backed by localStorage
 *
 * Persists cart operations for users who are not logged in
 * Uses the same shape as CartItemResponse so CartContext can
 * manage guest and authenticated flows transparently
 */

import type { CartItemResponse, CartResponse } from './cartService'

const KEY = 'guest_cart'

// Generates a unique local itemId from the productId + variantId combination 
export function guestItemId(productId: number, variantId: number | null) {
  return `guest-${productId}-${variantId ?? 'null'}`
}

function read(): CartItemResponse[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as CartItemResponse[]) : []
  } catch {
    return []
  }
}

function write(items: CartItemResponse[]) {
  localStorage.setItem(KEY, JSON.stringify(items))
}

function total(items: CartItemResponse[]) {
  return items.reduce((s, i) => s + i.lineTotal, 0)
}

function toCart(items: CartItemResponse[]): CartResponse {
  return { userId: 'guest', items, totalAmount: total(items) }
}

export const guestCartService = {
  getCart(): CartResponse {
    return toCart(read())
  },

  addItem(payload: {
    productId: number
    variantId?: number | null
    productName: string
    unitPrice: number
    quantity: number
    attributes?: Record<string, string>
  }): CartResponse {
    const items = read()
    const vid = payload.variantId ?? null
    const id  = guestItemId(payload.productId, vid)
    const idx = items.findIndex((i) => i.itemId === id)

    if (idx >= 0) {
      // Already exists -> increase quantity
      items[idx] = {
        ...items[idx],
        quantity:  items[idx].quantity + payload.quantity,
        lineTotal: items[idx].unitPrice * (items[idx].quantity + payload.quantity),
      }
    } else {
      items.push({
        itemId:      id,
        productId:   payload.productId,
        variantId:   vid,
        productName: payload.productName,
        unitPrice:   payload.unitPrice,
        quantity:    payload.quantity,
        lineTotal:   payload.unitPrice * payload.quantity,
        attributes:  payload.attributes,
      })
    }

    write(items)
    return toCart(items)
  },

  updateQuantity(itemId: string, quantity: number): CartResponse {
    const items = read().map((i) =>
      i.itemId === itemId
        ? { ...i, quantity, lineTotal: i.unitPrice * quantity }
        : i,
    )
    write(items)
    return toCart(items)
  },

  removeItem(itemId: string): CartResponse {
    const items = read().filter((i) => i.itemId !== itemId)
    write(items)
    return toCart(items)
  },

  clearCart() {
    localStorage.removeItem(KEY)
  },

  // Returns pending raw items to sync to backend after login. 
  getPendingItems() {
    return read()
  },
}



