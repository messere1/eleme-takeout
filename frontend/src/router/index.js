import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Shop from '@/views/Shop.vue'
import Cart from '@/views/Cart.vue'
import Orders from '@/views/Orders.vue'
import OrderDetail from '@/views/OrderDetail.vue'
import Profile from '@/views/Profile.vue'
import MerchantConsole from '@/views/MerchantConsole.vue'
import MerchantProducts from '@/views/MerchantProducts.vue'
import MerchantOrders from '@/views/MerchantOrders.vue'
import MerchantProfile from '@/views/MerchantProfile.vue'
import Admin from '@/views/Admin.vue'
import Rider from '@/views/Rider.vue'
import Pay from '@/views/Pay.vue'
import Search from '@/views/Search.vue'
import { session } from '@/utils/session'

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/login', name: 'login', component: Login },
  { path: '/register', name: 'register', component: Register },
  { path: '/shops/:id', name: 'shop', component: Shop },
  { path: '/cart', name: 'cart', component: Cart },
  { path: '/orders', name: 'orders', component: Orders },
  { path: '/orders/:id', name: 'order-detail', component: OrderDetail },
  { path: '/profile', name: 'profile', component: Profile },
  { path: '/merchant', name: 'merchant-console', component: MerchantConsole },
  { path: '/merchant/products', name: 'merchant-products', component: MerchantProducts },
  { path: '/merchant/orders', name: 'merchant-orders', component: MerchantOrders },
  { path: '/merchant/profile', name: 'merchant-profile', component: MerchantProfile },
  { path: '/search', name: 'search', component: Search },
  { path: '/orders/:id/pay', name: 'pay', component: Pay },
  { path: '/admin', name: 'admin', component: Admin },
  { path: '/admin/users', name: 'admin-users', component: Admin, props: { initialTab: 'users' } },
  { path: '/admin/merchants', name: 'admin-merchants', component: Admin, props: { initialTab: 'merchants' } },
  { path: '/rider', name: 'rider', component: Rider },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const CUSTOMER_ONLY = new Set(['/cart', '/orders', '/profile'])
router.beforeEach((to) => {
  const auth = session.load()
  const role = auth?.role || ''
  if (CUSTOMER_ONLY.has(to.path) || to.path.startsWith('/orders/')) {
    if (!auth) return { path: '/login' }
    if (role !== 'CUSTOMER') return { path: '/' }
  }
  if (to.path.startsWith('/merchant')) {
    if (role !== 'MERCHANT') return { path: '/login' }
  }
  if (to.path.startsWith('/admin')) {
    if (role !== 'ADMIN') return { path: '/login' }
  }
  if (to.path.startsWith('/rider')) {
    if (role !== 'RIDER') return { path: '/login' }
  }
  if (role === 'MERCHANT' && !to.path.startsWith('/merchant') && to.path !== '/login') return { path: '/merchant' }
  if (role === 'ADMIN' && !to.path.startsWith('/admin') && to.path !== '/login') return { path: '/admin' }
  if (role === 'RIDER' && !to.path.startsWith('/rider') && to.path !== '/login') return { path: '/rider' }
  return true
})

export default router
