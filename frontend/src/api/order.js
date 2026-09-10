// 订单接口（顾客下单/列表/详情，商家接单/完成，顾客取消）。契约 §8 与商家扩展动作。
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
