// 登录态持久化：登录成功保存 token/role；登出或 401 时清除。
// 商家侧额外缓存“我的店铺”（注册响应带回 shopId，登录接口不返回）。
export const SESSION_KEY = 'takeout-auth'
export const SHOP_KEY = 'takeout-shop'

function save({ token, role }) {
  localStorage.setItem(SESSION_KEY, JSON.stringify({ token, role }))
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
