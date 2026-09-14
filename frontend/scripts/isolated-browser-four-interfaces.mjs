// Browser → Vite proxy → isolated backend → H2. Never target the shared 5173/8080 stack.
import { chromium } from 'playwright'

const base = 'http://localhost:5174'
const backend = 'http://127.0.0.1:18081'
const suffix = Math.random().toString(36).slice(2, 9)
const password = `Test${suffix}9`
const phone = n => `138${String(Date.now() % 10000000).padStart(7, '0')}${n}`
const merchant = `联调页面商家${suffix}`
const rider = `联调页面骑手${suffix}`
const requests = []
const errors = []
const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' })
const page = await browser.newPage({ viewport: { width: 1280, height: 720 } })
page.on('pageerror', error => errors.push(String(error)))
page.on('response', response => {
  const path = new URL(response.url()).pathname
  if (path.startsWith('/api/v1/')) requests.push(`${response.request().method()} ${path} ${response.status()}`)
})

try {
  await page.goto(`${base}/register`, { waitUntil: 'networkidle' })
  await page.locator('[data-testid="register-role"]').selectOption('MERCHANT')
  await page.locator('[data-testid="register-merchant-name"]').fill(merchant)
  await page.locator('[data-testid="register-phone"]').fill(phone(1))
  await page.locator('[data-testid="register-password"]').fill(password)
  await page.locator('[data-testid="register-scope"]').fill('快餐便当')
  await page.locator('[data-testid="register-shop-address"]').fill('天津大学北洋园校区测试地址')
  await page.locator('[data-testid="register-submit"]').click()
  await page.waitForURL(`${base}/merchant`, { timeout: 12000 })
  await page.waitForLoadState('networkidle')

  await page.locator('[data-testid="opening-time-input"]').fill('08:30')
  await page.locator('[data-testid="closing-time-input"]').fill('20:30')
  await page.locator('[data-testid="business-hours-save"]').click()
  await page.getByText('营业时间已保存').waitFor()
  await page.goto(`${base}/merchant/profile`, { waitUntil: 'networkidle' })
  const select = page.locator('.profile-page .el-select')
  await select.locator('.el-select__wrapper').click()
  await page.getByRole('option', { name: '快餐便当' }).click()
  await page.getByRole('option', { name: '奶茶饮品' }).click()
  await page.locator('.profile-page button').filter({ hasText: '保存资料' }).click()
  await page.getByText('资料已保存').waitFor()

  const merchantState = await page.evaluate(() => document.querySelector('.profile-page')?.innerText.slice(0, 200))
  const actualCalls = requests.filter(item => /business-categories|business-hours|\/merchants|\/auth\/login/.test(item))
  if (!actualCalls.some(item => item.includes('PATCH /api/v1/shops/') && item.includes('/business-hours 200'))) throw new Error('browser did not complete business-hours PATCH')
  if (!actualCalls.some(item => item.includes('PUT /api/v1/merchant/shop/business-categories 200'))) throw new Error('browser did not complete categories PUT')

  await page.getByRole('button', { name: '退出' }).click()
  await page.goto(`${base}/register`, { waitUntil: 'networkidle' })
  await page.locator('[data-testid="register-role"]').selectOption('RIDER')
  await page.locator('[data-testid="register-rider-name"]').fill(rider)
  await page.locator('[data-testid="register-phone"]').fill(phone(2))
  await page.locator('[data-testid="register-password"]').fill(password)
  await page.locator('[data-testid="register-submit"]').click()
  await page.waitForURL(`${base}/rider`, { timeout: 12000 })
  await page.waitForLoadState('networkidle')
  if (!requests.some(item => item === 'POST /api/v1/riders 201')) throw new Error('browser did not complete rider registration POST')
  if (errors.length) throw new Error(`browser page errors: ${errors.join('; ')}`)

  console.log(JSON.stringify({ result: 'PASS', frontend: base, backend, merchantPage: merchantState, riderPage: (await page.locator('main').innerText()).slice(0, 120), requests: actualCalls.concat(requests.filter(item => item.includes('/riders'))) }, null, 2))
} finally {
  await browser.close()
}
