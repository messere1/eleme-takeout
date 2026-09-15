// 订单接口。
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

export function listMerchantOrders(query) {
  return http.get('/merchant/orders', { params: query })
}

export function acceptOrder(orderId) {
  return http.post(`/orders/${orderId}/accept`)
}

// 后端没有商家直接完成的接口，故不提供封装。
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
