// 订单接口。SRS V2.0 §9 外部接口表、§5.1 状态机。
// 完成路径只有「骑手送达 → 顾客确认收货」（FR-022），不存在商家直接完成的接口。
import { http } from './http'

export function createOrder(payload) {
  return http.post('/orders', payload)
}

export function listOrders(query) {
  return http.get('/orders', { params: query })
}

export function getOrder(orderId) {
  return http.get(`/orders/${orderId}`)
}

// FR-016：商家订单同样按分页、状态、时间查询（后端 MerchantOrderController 支持这组参数）。
export function listMerchantOrders(query) {
  return http.get('/merchant/orders', { params: query })
}

export function acceptOrder(orderId) {
  return http.post(`/orders/${orderId}/accept`)
}

// POST /orders/{id}/complete 不在 SRS V2.0 §9 接口表内，§5.1 也没有这条迁移，
// 故不提供封装（调用只会打到不存在的映射）。
export function cancelOrder(orderId) {
  return http.post(`/orders/${orderId}/cancel`)
}

export function confirmOrder(orderId) {
  return http.post(`/orders/${orderId}/confirm`)
}

export function payOrder(orderId) {
  return http.post(`/orders/${orderId}/pay`)
}

export function requestRefund(orderId, payload) { return http.post(`/orders/${orderId}/refunds`, payload) }
export function listRefunds() { return http.get('/refunds') }
export function listRiderAvailable() { return http.get('/rider/orders/available') }
export function listRiderOrders() { return http.get('/rider/orders') }
export function claimOrder(orderId) { return http.post(`/rider/orders/${orderId}/claim`) }
export function deliverOrder(orderId) { return http.post(`/rider/orders/${orderId}/deliver`) }
export function listMerchantRefunds(){return http.get('/merchant/refunds')}
export function decideMerchantRefund(id,status){return http.patch(`/merchant/refunds/${id}`,{status})}
