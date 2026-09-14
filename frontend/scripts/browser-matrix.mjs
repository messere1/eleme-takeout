// Real-browser, read-only layout/navigation acceptance against a running local stack.
// Set TAKEOUT_CHROME_PATH and TAKEOUT_EDGE_PATH to browser executables.
import { chromium, firefox } from 'playwright'

const base = process.env.TAKEOUT_BROWSER_BASE ?? 'http://127.0.0.1:5173'
const chromePath = process.env.TAKEOUT_CHROME_PATH
const edgePath = process.env.TAKEOUT_EDGE_PATH
const expectOrderData = process.env.TAKEOUT_EXPECT_ORDER_DATA === '1'
const selected = (process.env.TAKEOUT_BROWSERS ?? 'edge').split(',').map(value => value.trim().toLowerCase())
const accounts = [
  { role: '顾客', account: 'customer', password: process.env.TAKEOUT_CUSTOMER_PASSWORD, home: '/', paths: ['/cart', '/orders', '/profile', '/shops/2001'] },
  { role: '商家', account: '梅园', password: process.env.TAKEOUT_MERCHANT_PASSWORD, home: '/merchant', paths: ['/merchant', '/merchant/products', '/merchant/orders', '/merchant/profile'] },
  { role: '管理员', account: 'admin', password: process.env.TAKEOUT_ADMIN_PASSWORD, home: '/admin', paths: ['/admin'] },
  { role: '骑手', account: 'rider', password: process.env.TAKEOUT_RIDER_PASSWORD, home: '/rider', paths: ['/rider'] },
]
if (accounts.some(item => !item.password)) {
  throw new Error('Four demo-account passwords must be provided via environment variables')
}

const available = {
  chrome: { name: 'Chrome for Testing', type: chromium, executablePath: chromePath },
  edge: { name: 'Edge', type: chromium, executablePath: edgePath },
  firefox: { name: 'Firefox (Playwright build)', type: firefox },
}
const browsers = selected.map(name => available[name])
if (browsers.some(item => !item || (item.type === chromium && !item.executablePath))) {
  throw new Error('Unknown browser or missing executable path')
}
const widths = [360, 390, 430, 520, 1280]
const publicPaths = ['/', '/search', '/shops/2001', '/login', '/register']
const results = []
const failures = []
const versions = {}
const faviconCounts = []

for (const spec of browsers) {
  let browser
  try {
    browser = await spec.type.launch({ headless: true, executablePath: spec.executablePath })
    versions[spec.name] = browser.version()
    for (const width of widths) {
      const context = await browser.newContext({ viewport: { width, height: width === 1280 ? 720 : 844 } })
      const page = await context.newPage()
      const errors = []
      let favicon404 = 0
      page.on('pageerror', error => errors.push(String(error)))
      page.on('response', response => {
        if (response.status() < 400) return
        if (new URL(response.url()).pathname === '/favicon.ico') favicon404 += 1
        else errors.push(`HTTP ${response.status()} ${response.url()}`)
      })
      page.on('console', message => {
        if (message.type() === 'error' && !message.text().includes('Failed to load resource: the server responded with a status of 404')) {
          errors.push(message.text())
        }
      })

      async function check(path, role) {
        await page.goto(new URL(path, base).href, { waitUntil: 'networkidle' })
        const state = await page.evaluate(() => ({
          path: location.pathname,
          width: innerWidth,
          scrollWidth: document.documentElement.scrollWidth,
          mainText: document.querySelector('main')?.innerText.trim().slice(0, 90) ?? '',
          errorOverlay: !!document.querySelector('vite-error-overlay,.vite-error-overlay'),
          buttonsOutside: [...document.querySelectorAll('main button')]
            .filter(element => element.getBoundingClientRect().right > innerWidth + 1).length,
          orderRows: document.querySelectorAll('[data-testid^="order-row-"]').length,
        }))
        results.push({ browser: spec.name, width, role, ...state })
        if (state.path !== path || state.width !== width || state.scrollWidth > width || !state.mainText || state.errorOverlay || state.buttonsOutside ||
          (expectOrderData && role === '顾客' && path === '/orders' && state.orderRows < 1)) {
          failures.push({ browser: spec.name, width, role, path, state })
        }
      }

      for (const path of publicPaths) await check(path, '公开')
      for (const account of accounts) {
        await page.goto(new URL('/login', base).href, { waitUntil: 'networkidle' })
        await page.getByRole('textbox', { name: '账号' }).fill(account.account)
        await page.getByRole('textbox', { name: '密码' }).fill(account.password)
        await page.getByRole('combobox', { name: '登录身份' }).selectOption({ label: `我是${account.role}` })
        await page.getByRole('button', { name: '登录', exact: true }).click()
        await page.waitForURL(new URL(account.home, base).href, { timeout: 10000 })
        for (const path of account.paths) await check(path, account.role)
        if (account.role === '管理员') {
          for (const name of ['用户', '商家', '商品', '订单', '退款']) {
            await page.getByRole('button', { name, exact: true }).click()
            await page.waitForLoadState('networkidle')
            const state = await page.evaluate(() => ({
              width: innerWidth,
              scrollWidth: document.documentElement.scrollWidth,
              buttonsOutside: [...document.querySelectorAll('main button')]
                .filter(element => element.getBoundingClientRect().right > innerWidth + 1).length,
              rows: document.querySelectorAll('tbody tr').length,
            }))
            results.push({ browser: spec.name, width, role: `管理员/${name}`, path: '/admin', ...state })
            if (state.scrollWidth > width || state.buttonsOutside) failures.push({ browser: spec.name, width, role: `管理员/${name}`, state })
          }
        }
        await page.getByRole('button', { name: '退出' }).click()
      }
      if (errors.length) failures.push({ browser: spec.name, width, consoleErrors: errors.slice(0, 5) })
      if (favicon404) faviconCounts.push({ browser: spec.name, width, count: favicon404 })
      await context.close()
    }
  } catch (error) {
    failures.push({ browser: spec.name, launchOrFlowError: String(error) })
  } finally {
    if (browser) await browser.close()
  }
}

console.log(JSON.stringify({ testedPages: results.length, versions, failures, faviconCounts,
  countsByBrowser: Object.fromEntries(browsers.map(item => [item.name, results.filter(result => result.browser === item.name).length])),
}, null, 2))
if (failures.length) process.exitCode = 1
