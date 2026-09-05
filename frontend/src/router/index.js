// 路由表：网址 → 页面。首页为店铺浏览入口（暂无店铺列表接口，示例直达店铺 1）。
import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Shop from '@/views/Shop.vue'
import Cart from '@/views/Cart.vue'
import Orders from '@/views/Orders.vue'
import OrderDetail from '@/views/OrderDetail.vue'
import Profile from '@/views/Profile.vue'
import MerchantRegister from '@/views/MerchantRegister.vue'
import MerchantConsole from '@/views/MerchantConsole.vue'

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/login', name: 'login', component: Login },
  { path: '/register', name: 'register', component: Register },
  { path: '/shops/:id', name: 'shop', component: Shop },
  { path: '/cart', name: 'cart', component: Cart },
  { path: '/orders', name: 'orders', component: Orders },
  { path: '/orders/:id', name: 'order-detail', component: OrderDetail },
  { path: '/profile', name: 'profile', component: Profile },
  { path: '/merchant/register', name: 'merchant-register', component: MerchantRegister },
  { path: '/merchant', name: 'merchant-console', component: MerchantConsole },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
