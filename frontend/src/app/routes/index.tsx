import { Navigate, createBrowserRouter } from 'react-router-dom'
import LoginPage from '../../pages/auth/LoginPage'
import RegisterPage from '../../pages/auth/RegisterPage'
import ForgotPasswordPage from '../../pages/auth/ForgotPasswordPage'
import ResetPasswordPage from '../../pages/auth/ResetPasswordPage'
import ProductsPage from '../../pages/products/ProductsPage'
import ProductDetailPage from '../../pages/products/ProductDetailPage'
import AccountPage from '../../pages/account/AccountPage'
import CartPage from '../../pages/cart/CartPage'
import CheckoutPage from '../../pages/checkout/CheckoutPage'
import PaymentPage from '../../pages/checkout/PaymentPage'
import OrderSuccessPage from '../../pages/orders/OrderSuccessPage'
import MyOrdersPage from '../../pages/orders/MyOrdersPage'
import AdminLayout from '../../pages/admin/AdminLayout'
import AdminDashboard from '../../pages/admin/AdminDashboard'
import AdminUsersPage from '../../pages/admin/AdminUsersPage'
import AdminCategoriesPage from '../../pages/admin/AdminCategoriesPage'
import AdminOrdersPage from '../../pages/admin/AdminOrdersPage'
import AdminProductsPage from '../../pages/admin/products/AdminProductsPage'
import AdminProductFormPage from '../../pages/admin/products/AdminProductFormPage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/products" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  { path: '/products', element: <ProductsPage /> },
  { path: '/products/:id', element: <ProductDetailPage /> },
  { path: '/account', element: <AccountPage /> },
  { path: '/cart', element: <CartPage /> },
  { path: '/checkout', element: <CheckoutPage /> },
  { path: '/checkout/payment', element: <PaymentPage /> },
  { path: '/orders', element: <MyOrdersPage /> },
  { path: '/orders/:orderNumber', element: <OrderSuccessPage /> },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <AdminDashboard /> },
      { path: 'products', element: <AdminProductsPage /> },
      { path: 'products/new', element: <AdminProductFormPage /> },
      { path: 'products/:id', element: <AdminProductFormPage /> },
      { path: 'users', element: <AdminUsersPage /> },
      { path: 'categories', element: <AdminCategoriesPage /> },
      { path: 'orders', element: <AdminOrdersPage /> },
    ],
  },
])
