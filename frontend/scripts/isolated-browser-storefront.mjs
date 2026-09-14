// Browser → Vite proxy → isolated H2 backend. Do not target shared ports.
import { chromium } from 'playwright'

const base = 'http://localhost:5174'
const api = 'http://127.0.0.1:18081/api/v1'
const suffix = Math.random().toString(36).slice(2, 9)
const merchantName = `联调商家${suffix}`
const customerName = `联调顾客${suffix}`
const password = `Test${suffix}9`
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/WZkAAAAASUVORK5CYII=', 'base64')
const checks = []

async function request(method, path, body, token) {
  const response = await fetch(`${api}${path}`, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  const data = await response.json()
  if (!response.ok || data.code !== 0) throw new Error(`${method} ${path}: ${response.status} ${JSON.stringify(data).slice(0, 200)}`)
  return data.data
}

const defaults = await request('GET', '/business-categories')
const primary = defaults[0]
const secondary = defaults[1]
await request('POST', '/merchants', {
  merchantName, phone: `137${String(Date.now() % 10000000).padStart(7, '0')}1`,
  password, businessScope: primary.name, shopAddress: '天津大学北洋园校区隔离测试地址',
})
const merchantToken = (await request('POST', '/auth/login', { account: merchantName, password, role: 'MERCHANT' })).token
const shop = await request('GET', '/merchant/shop', null, merchantToken)
await request('PUT', '/merchant/shop/business-categories', { categoryIds: [primary.id, secondary.id] }, merchantToken)
await request('PATCH', `/shops/${shop.id}/business-hours`, { openingTime: '08:30', closingTime: '20:30' }, merchantToken)
await request('PATCH', `/shops/${shop.id}/status`, { status: 'OPEN' }, merchantToken)
const productCategory = await request('POST', `/shops/${shop.id}/categories`, { name: `联调菜品${suffix}`, sort: 1 }, merchantToken)
const productName = `联调特色餐${suffix}`
const product = await request('POST', `/shops/${shop.id}/products`, {
  name: productName, categoryId: productCategory.id, description: '浏览器隔离数据', price: '19.80', stock: 10,
}, merchantToken)

const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' })
const page = await browser.newPage({ viewport: { width: 1280, height: 720 } })
const errors = []
const uploads = []
page.on('pageerror', error => errors.push(String(error)))
page.on('response', response => {
  if (new URL(response.url()).pathname === '/api/v1/images') uploads.push(`${response.request().method()} ${response.status()}`)
})
const file = { name: 'fixture.png', mimeType: 'image/png', buffer: png }

try {
  await page.goto(base, { waitUntil: 'networkidle' })
  await page.locator(`[data-testid="home-category-${secondary.id}"]`).click()
  await page.locator(`[data-testid="home-shop-${shop.id}"]`).waitFor()
  checks.push('home secondary category filters a multiply linked shop')

  await page.locator('[data-testid="home-search-entry"]').click()
  await page.locator('[data-testid="search-input"]').fill(secondary.name)
  await page.locator('[data-testid="search-submit"]').click()
  await page.locator(`[data-testid="search-result-${shop.id}"]`).waitFor()
  await page.locator('[data-testid="search-input"]').fill(productName)
  await page.locator('[data-testid="search-submit"]').click()
  await page.locator(`[data-testid="search-result-${shop.id}"]`).waitFor()
  checks.push('category and product-name search return the shop')

  await page.locator(`[data-testid="search-result-${shop.id}"]`).click()
  await page.locator('[data-testid="shop-hours"]').getByText('08:30–20:30').waitFor()
  checks.push('shop details display saved business hours')

  await page.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await page.locator('[data-testid="login-account"]').fill(merchantName)
  await page.locator('[data-testid="login-password"]').fill(password)
  await page.locator('[data-testid="login-role"]').selectOption('MERCHANT')
  await page.locator('[data-testid="login-submit"]').click()
  await page.waitForURL(`${base}/merchant`)
  await page.goto(`${base}/merchant/profile`, { waitUntil: 'networkidle' })
  for (const testid of ['profile-shop-image-input', 'profile-cover-image-input']) {
    await page.locator(`[data-testid="${testid}"]`).setInputFiles(file)
    await page.locator(`[data-testid="${testid}-preview"]`).waitFor()
    await page.waitForFunction(id => document.querySelector(`[data-testid="${id}-preview"]`)?.naturalWidth > 0, testid, { timeout: 5000 })
  }
  checks.push('merchant uploads shop image and cover through browser')
  await page.goto(`${base}/merchant/products`, { waitUntil: 'networkidle' })
  await page.locator(`[data-testid="product-img-${product.id}"]`).setInputFiles(file)
  await page.locator(`[data-testid="product-img-${product.id}-preview"]`).waitFor()
  await page.waitForFunction(id => document.querySelector(`[data-testid="product-img-${id}-preview"]`)?.naturalWidth > 0, product.id, { timeout: 5000 })
  checks.push('merchant uploads product image through browser')

  await request('POST', '/users', {
    username: customerName, phone: `136${String(Date.now() % 10000000).padStart(7, '0')}2`, password,
  })
  await page.getByRole('button', { name: '退出' }).click()
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await page.locator('[data-testid="login-account"]').fill(customerName)
  await page.locator('[data-testid="login-password"]').fill(password)
  await page.locator('[data-testid="login-role"]').selectOption('CUSTOMER')
  await page.locator('[data-testid="login-submit"]').click()
  await page.waitForURL(base + '/')
  await page.goto(`${base}/profile`, { waitUntil: 'networkidle' })
  await page.locator('[data-testid="profile-avatar-input"]').setInputFiles(file)
  await page.locator('[data-testid="profile-avatar-input-preview"]').waitFor()
  await page.waitForFunction(() => document.querySelector('[data-testid="profile-avatar-input-preview"]')?.naturalWidth > 0, null, { timeout: 5000 })
  checks.push('customer uploads avatar through browser')

  if (uploads.length !== 4 || uploads.some(item => item !== 'POST 200')) throw new Error(`upload responses: ${uploads}`)
  if (errors.length) throw new Error(`page errors: ${errors.join('; ')}`)
  console.log(JSON.stringify({ result: 'PASS', shopId: shop.id, productId: product.id, checks, uploads, pageErrors: errors }, null, 2))
} finally {
  await browser.close()
}
