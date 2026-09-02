// 登录态持久化：登录成功保存 token/role，登出或 401 时清除。
// 契约：docs/api-contract-v1.md §2.1/§9 —— 前端保存 data.token，不自行传 userId。
export const SESSION_KEY = 'takeout-auth'

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
}

export const session = { save, load, clear }
