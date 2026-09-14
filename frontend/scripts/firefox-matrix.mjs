// Stock Firefox layout/navigation verification via geckodriver (520px and desktop).
import { Builder, By, until } from 'selenium-webdriver'
import firefox from 'selenium-webdriver/firefox.js'
import { Select } from 'selenium-webdriver/lib/select.js'

const binary = process.env.TAKEOUT_FIREFOX_PATH
const driverPath = process.env.TAKEOUT_GECKODRIVER_PATH
const expectOrderData = process.env.TAKEOUT_EXPECT_ORDER_DATA === '1'
if (!binary || !driverPath) throw new Error('Firefox and geckodriver paths are required')
const accounts = [
  { role: 'CUSTOMER', account: 'customer', password: process.env.TAKEOUT_CUSTOMER_PASSWORD, home: '/', paths: ['/cart', '/orders', '/profile', '/shops/2001'] },
  { role: 'MERCHANT', account: '梅园', password: process.env.TAKEOUT_MERCHANT_PASSWORD, home: '/merchant', paths: ['/merchant', '/merchant/products', '/merchant/orders', '/merchant/profile'] },
  { role: 'ADMIN', account: 'admin', password: process.env.TAKEOUT_ADMIN_PASSWORD, home: '/admin', paths: ['/admin'] },
  { role: 'RIDER', account: 'rider', password: process.env.TAKEOUT_RIDER_PASSWORD, home: '/rider', paths: ['/rider'] },
]
if (accounts.some(item => !item.password)) throw new Error('Four demo-account passwords are required')
const base = process.env.TAKEOUT_BROWSER_BASE ?? 'http://127.0.0.1:5173'
const options = new firefox.Options().setBinary(binary).addArguments('-headless')
const service = new firefox.ServiceBuilder(driverPath)
const driver = await new Builder().forBrowser('firefox').setFirefoxOptions(options).setFirefoxService(service).build()
const failures = []
let checks = 0

async function check(path, width, role) {
  await driver.get(new URL(path, base).href)
  await driver.sleep(150)
  const state = await driver.executeScript(`return {
    width: innerWidth,
    scrollWidth: document.documentElement.scrollWidth,
    text: document.querySelector('main')?.innerText.trim().slice(0,80) ?? '',
    overlay: !!document.querySelector('vite-error-overlay,.vite-error-overlay'),
    buttonsOutside: [...document.querySelectorAll('main button')].filter(e => e.getBoundingClientRect().right > innerWidth + 1).length,
    orderRows: document.querySelectorAll('[data-testid^="order-row-"]').length
  }`)
  checks += 1
  if (state.width !== width || state.scrollWidth > state.width || !state.text || state.overlay || state.buttonsOutside ||
    (expectOrderData && role === 'CUSTOMER' && path === '/orders' && state.orderRows < 1)) {
    failures.push({ width, role, path, state })
  }
}

try {
  for (const width of [520, 1280]) {
    await driver.manage().window().setRect({ width, height: width === 1280 ? 720 : 844 })
    for (const path of ['/', '/search', '/shops/2001', '/login', '/register']) await check(path, width, 'PUBLIC')
    for (const account of accounts) {
      await driver.get(new URL('/login', base).href)
      await driver.findElement(By.id('login-account')).sendKeys(account.account)
      await driver.findElement(By.id('login-password')).sendKeys(account.password)
      await new Select(driver.findElement(By.id('login-role'))).selectByValue(account.role)
      await driver.findElement(By.css('[data-testid="login-submit"]')).click()
      await driver.wait(until.urlIs(new URL(account.home, base).href), 10000)
      for (const path of account.paths) await check(path, width, account.role)
      if (account.role === 'ADMIN') {
        for (const name of ['用户', '商家', '商品', '订单', '退款']) {
          await driver.findElement(By.xpath(`//main//button[normalize-space(.)='${name}']`)).click()
          await driver.sleep(150)
          const state = await driver.executeScript('return {width:innerWidth,scrollWidth:document.documentElement.scrollWidth}')
          checks += 1
          if (state.scrollWidth > state.width) failures.push({ width, role: `ADMIN/${name}`, state })
        }
      }
      await driver.findElement(By.xpath("//button[normalize-space(.)='退出']")).click()
    }
  }
} catch (error) {
  failures.push({ flowError: String(error) })
} finally {
  await driver.quit()
}
console.log(JSON.stringify({ browser: 'Firefox', checks, failures }, null, 2))
if (failures.length) process.exitCode = 1
