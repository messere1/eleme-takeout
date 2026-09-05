// 订单接口。契约：docs/api-contract-v1.md §8（创建需 CUSTOMER，详情支持 CUSTOMER/MERCHANT）。
import { http } from './http'

export function createOrder() {
  return http.post('/orders')
}

export function listOrders(query) {
  return http.get('/orders', { params: query })
}

export function getOrder(orderId) {
  return http.get(`/orders/${orderId}`)
}
