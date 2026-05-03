/**
 * CartContext manages carts for both guest and authenticated users
 *
 * - Not logged in: localStorage via guestCartService
 * - Logged in: backend cart-service
 *
 * After login/register, locally stored cart items are synced to backend
 * and localStorage is cleared
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { cartService, type CartResponse, type AddCartItemRequest } from '../services/cartService'
import { guestCartService } from '../services/guestCartService'
import { useAuth } from './AuthContext'

interface CartContextValue {
  cart: CartResponse | null
  itemCount: number
  loading: boolean
  addItem: (payload: AddCartItemRequest) => Promise<void>
  updateQuantity: (itemId: string, quantity: number) => Promise<void>
  removeItem: (itemId: string) => Promise<void>
  clearCart: () => Promise<void>
  /** Sync guest cart to backend after login. */
  syncGuestCart: () => Promise<void>
  refresh: () => Promise<void>
}

const CartContext = createContext<CartContextValue | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const isAuth = user !== null

  const [cart, setCart]       = useState<CartResponse | null>(() =>
    // Load guest cart immediately on initial page load
    // if is not auth, load from guestCartService; else, start with null and refresh from backend
    isAuth ? null : guestCartService.getCart()
  )
  const [loading, setLoading] = useState(false)
  const refresh = useCallback(async () => {
    if (!isAuth) {
      setCart(guestCartService.getCart())
      return
    }
    setLoading(true)
    try {
      setCart(await cartService.getCart())
    } catch {
      setCart(null)
    } finally {
      setLoading(false)
    }
  }, [isAuth])


  // Reset and refresh cart when auth state changes (login/logout)
  useEffect(() => {
    if (!isAuth) {
      setCart(guestCartService.getCart())
    } else {
      refresh()
    }
  }, [isAuth, refresh])
  const syncGuestCart = useCallback(async () => {
    const pending = guestCartService.getPendingItems()
    if (pending.length === 0) return

    // Add each item to the backend one by one (skip failed items and continue)
    for (const item of pending) {
      try {
        await cartService.addItem({
          productId:   item.productId,
          variantId:   item.variantId,
          productName: item.productName,
          unitPrice:   item.unitPrice,
          quantity:    item.quantity,
          attributes:  item.attributes,
        })
      } catch {
        // Skip item on stock or validation errors
      }
    }
    guestCartService.clearCart()
    // Fetch the final state from backend
    setCart(await cartService.getCart())
  }, [])

  const addItem = useCallback(async (payload: AddCartItemRequest) => {
    if (!isAuth) {
      setCart(guestCartService.addItem(payload))
      return
    }
    const updated = await cartService.addItem(payload)
    setCart(updated)
  }, [isAuth])

  const updateQuantity = useCallback(async (itemId: string, quantity: number) => {
    if (!isAuth) {
      setCart(guestCartService.updateQuantity(itemId, quantity))
      return
    }
    setCart(await cartService.updateQuantity(itemId, quantity))
  }, [isAuth])

  const removeItem = useCallback(async (itemId: string) => {
    if (!isAuth) {
      setCart(guestCartService.removeItem(itemId))
      return
    }
    setCart(await cartService.removeItem(itemId))
  }, [isAuth])

  const clearCart = useCallback(async () => {
    if (!isAuth) {
      guestCartService.clearCart()
      setCart(guestCartService.getCart())
      return
    }
    await cartService.clearCart()
    setCart((prev) => prev ? { ...prev, items: [], totalAmount: 0 } : null)
  }, [isAuth])

  const itemCount = cart?.items.reduce((s, i) => s + i.quantity, 0) ?? 0

  return (
    <CartContext.Provider value={{
      cart, itemCount, loading,
      addItem, updateQuantity, removeItem, clearCart,
      syncGuestCart, refresh,
    }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within <CartProvider>')
  return ctx
}



