export const SESSION_KEY = 'takeout-auth'
export const SHOP_KEY = 'takeout-shop'

function notify() {
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('auth-change'))
}

function save({ token, role }) {
  localStorage.setItem(SESSION_KEY, JSON.stringify({ token, role }))
  notify()
}

function load() {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    return parsed && parsed.token ? parsed : null
  } catch {
    return null
  }
}

function clear() {
  localStorage.removeItem(SESSION_KEY)
  localStorage.removeItem(SHOP_KEY)
  notify()
}

function saveShop(shop) {
  localStorage.setItem(SHOP_KEY, JSON.stringify(shop))
}

function loadShop() {
  const raw = localStorage.getItem(SHOP_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    return parsed && parsed.shopId ? parsed : null
  } catch {
    return null
  }
}

export const session = { save, load, clear, saveShop, loadShop }
