// End-to-end API smoke flow for a disposable database only. Never point at shared data.
const base = process.env.TAKEOUT_FLOW_BASE
const target = base ? new URL(base) : null
if (!target || !['127.0.0.1', 'localhost'].includes(target.hostname) || target.port !== '18081') {
  throw new Error('Use only the isolated local backend on port 18081')
}
const identities = [
  ['customer', 'CUSTOMER', process.env.TAKEOUT_CUSTOMER_PASSWORD],
  ['梅园', 'MERCHANT', process.env.TAKEOUT_MERCHANT_PASSWORD],
  ['admin', 'ADMIN', process.env.TAKEOUT_ADMIN_PASSWORD],
  ['rider', 'RIDER', process.env.TAKEOUT_RIDER_PASSWORD],
]
if (identities.some(item => !item[2])) throw new Error('Four demo-account passwords are required')

const tokens = {}
const steps = []
async function call(method, path, role, body, expected = 200) {
  const response = await fetch(new URL(`/api/v1${path}`, base), {
    method,
    headers: { ...(role ? { Authorization: `Bearer ${tokens[role]}` } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  const data = await response.json()
  if (response.status !== expected || (expected === 200 && data.code !== 0)) {
    throw new Error(`${method} ${path}: expected ${expected}, got ${response.status} ${JSON.stringify(data).slice(0, 200)}`)
  }
  if (expected >= 400 && (!data.traceId || /stack|SQLException|org\.postgresql/i.test(JSON.stringify(data)))) {
    throw new Error(`${method} ${path}: unsafe failure response`)
  }
  steps.push(`${method} ${path}: ${response.status}`)
  return data.data
}

for (const [account, role, password] of identities) {
  const login = await call('POST', '/auth/login', null, { account, password, role })
  tokens[role] = login.token
}
await call('GET', '/cart', 'MERCHANT', undefined, 403)
await call('POST', '/cart/items', 'CUSTOMER', { productId: 4001, quantity: 1 })
const order = await call('POST', '/orders', 'CUSTOMER', {
  shopId: 2001, recipientName: '测试收货人', recipientPhone: '13800000000',
  deliveryAddress: '天津大学北洋园校区测试地址', saveToProfile: false,
})
if (order.status !== 'CREATED' || order.paymentStatus !== 'UNPAID') throw new Error('Order initial state mismatch')
await call('POST', `/orders/${order.id}/pay`, 'CUSTOMER')
await call('POST', `/orders/${order.id}/accept`, 'MERCHANT')
const available = await call('GET', '/rider/orders/available', 'RIDER')
if (!available.some(item => item.id === order.id)) throw new Error('Accepted order absent from rider queue')
await call('POST', `/rider/orders/${order.id}/claim`, 'RIDER')
await call('POST', `/rider/orders/${order.id}/deliver`, 'RIDER')
await call('POST', `/orders/${order.id}/confirm`, 'CUSTOMER')
const detail = await call('GET', `/orders/${order.id}`, 'CUSTOMER')
if (detail.status !== 'COMPLETED' || detail.paymentStatus !== 'PAID') throw new Error('Delivered order final state mismatch')
const refund = await call('POST', `/orders/${order.id}/refunds`, 'CUSTOMER', { amount: 1, reason: '测试部分退款', evidenceUrls: [] })
if (refund.status !== 'PENDING') throw new Error('Refund initial state mismatch')
const decision = await call('PATCH', `/merchant/refunds/${refund.id}`, 'MERCHANT', { status: 'APPROVED' })
if (decision.status !== 'APPROVED') throw new Error('Refund approval mismatch')
const allRefunds = await call('GET', '/admin/refunds', 'ADMIN')
if (!Array.isArray(allRefunds.items) || !allRefunds.items.some(item => item.id === refund.id)) {
  throw new Error('Administrator cannot see refund in paginated list')
}
await call('GET', '/orders/99999999', 'CUSTOMER', undefined, 404)

console.log(JSON.stringify({ orderId: order.id, refundId: refund.id, steps, result: 'PASS' }, null, 2))
