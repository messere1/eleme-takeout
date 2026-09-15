// Disposable end-to-end API check. Never run against the shared 8080 database.
const base = process.env.TAKEOUT_FLOW_BASE
const target = base && new URL(base)
if (!target || !['127.0.0.1', 'localhost'].includes(target.hostname) || target.port !== '18081') {
  throw new Error('This test only runs against the isolated backend on port 18081')
}

const suffix = Math.random().toString(36).slice(2, 9)
const password = `Test${suffix}9`
const phone = n => `139${String(Date.now() % 10000000).padStart(7, '0')}${n}`
const checks = []
const tokens = {}

async function call(name, method, path, { role, body, expected = 200 } = {}) {
  const response = await fetch(new URL(`/api/v1${path}`, base), {
    method,
    headers: {
      ...(role ? { Authorization: `Bearer ${tokens[role]}` } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const payload = await response.json()
  if (response.status !== expected || (expected < 400 && payload.code !== 0)) {
    throw new Error(`${name}: expected HTTP ${expected}, got ${response.status} ${JSON.stringify(payload).slice(0, 250)}`)
  }
  if (expected >= 400 && (!payload.traceId || /SQLException|org\.postgresql|Exception\bat|passwordHash|\bat java\./i.test(JSON.stringify(payload)))) {
    throw new Error(`${name}: unsafe error response`)
  }
  checks.push(`${name}: HTTP ${response.status}`)
  return payload.data
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

async function login(account, role) {
  const data = await call(`login ${role}`, 'POST', '/auth/login', {
    body: { account, password, role },
  })
  assert(data.token, `${role} login returned no token`)
  tokens[role] = data.token
}

const names = [`联调商家甲${suffix}`, `联调商家乙${suffix}`]
await call('register merchant A', 'POST', '/merchants', {
  expected: 201, body: { merchantName: names[0], phone: phone(1), password, businessScope: '快餐便当', shopAddress: '天津大学北洋园校区测试地址' },
})
await call('register merchant B', 'POST', '/merchants', {
  expected: 201, body: { merchantName: names[1], phone: phone(2), password, businessScope: '快餐便当', shopAddress: '天津大学北洋园校区测试地址' },
})
await login(names[0], 'MERCHANT')
const merchantA = tokens.MERCHANT
const shopA = await call('merchant A own shop', 'GET', '/merchant/shop', { role: 'MERCHANT' })
await login(names[1], 'MERCHANT')
const merchantB = tokens.MERCHANT
const shopB = await call('merchant B own shop', 'GET', '/merchant/shop', { role: 'MERCHANT' })
assert(shopA.id !== shopB.id, 'merchant shops are not isolated')

const customerName = `customer${suffix}`
await call('register customer', 'POST', '/users', {
  expected: 201, body: { username: customerName, phone: phone(3), password },
})
await login(customerName, 'CUSTOMER')

const defaults = await call('public categories', 'GET', '/business-categories')
assert(defaults.length >= 3, 'fewer than three default categories')
tokens.MERCHANT = merchantA
const initial = await call('GET own categories after registration', 'GET', '/merchant/shop/business-categories', { role: 'MERCHANT' })
assert(Array.isArray(initial) && initial.length === 1 && initial[0].name === '快餐便当', 'registered business scope was not linked to the shop')
await call('GET categories without token', 'GET', '/merchant/shop/business-categories', { expected: 401 })
await call('GET categories as customer', 'GET', '/merchant/shop/business-categories', { role: 'CUSTOMER', expected: 403 })

const categoryIds = defaults.slice(0, 3).map(item => item.id)
const selected = await call('PUT three categories', 'PUT', '/merchant/shop/business-categories', {
  role: 'MERCHANT', body: { categoryIds },
})
assert(selected.length === 3, 'PUT did not return three categories')
const repeated = await call('PUT same categories again', 'PUT', '/merchant/shop/business-categories', {
  role: 'MERCHANT', body: { categoryIds },
})
assert(repeated.length === 3, 'repeated PUT created duplicates')
await call('open merchant A shop before public filtering', 'PATCH', `/shops/${shopA.id}/status`, {
  role: 'MERCHANT', body: { status: 'OPEN' },
})
const filtered = await call('filter shops by first category', 'GET', `/shops?businessCategoryId=${categoryIds[0]}`)
assert(filtered.items.some(item => item.id === shopA.id), 'category filter omitted shop A')
const secondaryCategoryName = defaults[1].name
const categorySearch = await call('search shops by secondary linked category', 'GET', `/shops/search?keyword=${encodeURIComponent(secondaryCategoryName)}`)
assert(categorySearch.items.some(item => item.id === shopA.id), 'search omitted shop linked to secondary business category')
const productCategory = await call('create product category for search', 'POST', `/shops/${shopA.id}/categories`, {
  role: 'MERCHANT', body: { name: `联调菜品${suffix}`, sort: 1 },
})
const productName = `联调特色餐${suffix}`
await call('create product for search', 'POST', `/shops/${shopA.id}/products`, {
  role: 'MERCHANT', body: { name: productName, categoryId: productCategory.id, description: '隔离测试商品', price: '18.50', stock: 10 },
})
const productSearch = await call('search shops by product name', 'GET', `/shops/search?keyword=${encodeURIComponent(productName)}`)
assert(productSearch.items.some(item => item.id === shopA.id), 'search omitted shop with matching product name')
await call('PUT unknown category', 'PUT', '/merchant/shop/business-categories', {
  role: 'MERCHANT', body: { categoryIds: [categoryIds[0], 999999] }, expected: 400,
})
const afterInvalid = await call('GET categories after rejected PUT', 'GET', '/merchant/shop/business-categories', { role: 'MERCHANT' })
assert(afterInvalid.length === 3, 'invalid PUT changed saved categories')
await call('PUT categories as customer', 'PUT', '/merchant/shop/business-categories', {
  role: 'CUSTOMER', body: { categoryIds }, expected: 403,
})
tokens.MERCHANT = merchantB
const otherCategories = await call('GET merchant B categories', 'GET', '/merchant/shop/business-categories', { role: 'MERCHANT' })
assert(otherCategories.length === 1 && otherCategories[0].name === '快餐便当', 'merchant B categories are not isolated from merchant A')

tokens.MERCHANT = merchantA
const changedHours = await call('PATCH own business hours', 'PATCH', `/shops/${shopA.id}/business-hours`, {
  role: 'MERCHANT', body: { openingTime: '08:00', closingTime: '21:00' },
})
assert(changedHours.openingTime?.startsWith('08:00') && changedHours.closingTime?.startsWith('21:00'), 'saved hours mismatch')
const overnightHours = await call('PATCH overnight business hours', 'PATCH', `/shops/${shopA.id}/business-hours`, {
  role: 'MERCHANT', body: { openingTime: '22:00', closingTime: '02:00' },
})
assert(overnightHours.openingTime?.startsWith('22:00') && overnightHours.closingTime?.startsWith('02:00'), 'overnight hours mismatch')
await call('PATCH zero-length business hours', 'PATCH', `/shops/${shopA.id}/business-hours`, {
  role: 'MERCHANT', body: { openingTime: '21:00', closingTime: '21:00' }, expected: 400,
})
const afterHoursError = await call('GET shop after invalid hours', 'GET', `/shops/${shopA.id}`)
assert(afterHoursError.openingTime?.startsWith('22:00'), 'invalid hours changed shop A')
await call('PATCH other merchant shop', 'PATCH', `/shops/${shopB.id}/business-hours`, {
  role: 'MERCHANT', body: { openingTime: '08:00', closingTime: '21:00' }, expected: 403,
})
await call('PATCH business hours as customer', 'PATCH', `/shops/${shopA.id}/business-hours`, {
  role: 'CUSTOMER', body: { openingTime: '09:00', closingTime: '22:00' }, expected: 403,
})

const riderName = `联调骑手${suffix}`
await call('POST rider registration', 'POST', '/riders', {
  expected: 201, body: { riderName, phone: phone(4), password },
})
await login(riderName, 'RIDER')
await call('GET rider work queue', 'GET', '/rider/orders/available', { role: 'RIDER' })
await call('POST duplicate rider registration', 'POST', '/riders', {
  expected: 409, body: { riderName, phone: phone(4), password },
})
await call('POST invalid rider registration', 'POST', '/riders', {
  expected: 400, body: { riderName: '', phone: '123', password: 'weak' },
})

console.log(JSON.stringify({ result: 'PASS', isolatedBackend: base, shopIds: [shopA.id, shopB.id], checks }, null, 2))
