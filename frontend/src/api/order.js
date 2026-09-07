// 订单接口。契约：docs/api-contract-v1.md §8。
// 以下商家/状态动作为新增约定（后端实现中）：
//   商家列表   GET    /merchant/orders      —— 返回本人店铺订单
//   接单/完成  POST   /orders/{id}/accept | /orders/{id}/complete
//   顾客取消   POST   /orders/{id}/cancel   —— 仅可取消待接单订单并回补库存
// 订单状态按后端实际：PENDING(待接单)/ACCEPTED(已接单)/COMPLETED(已完成)/CANCELLED(已取消)。
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

export function listMerchantOrders() {
  return http.get('/merchant/orders')
}

export function acceptOrder(orderId) {
  return http.post(`/orders/${orderId}/accept`)
}

export function completeOrder(orderId) {
  return http.post(`/orders/${orderId}/complete`)
}

export function cancelOrder(orderId) {
  return http.post(`/orders/${orderId}/cancel`)
}
