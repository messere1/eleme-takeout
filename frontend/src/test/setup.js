// 全局测试环境清理：保证每个用例之间不留 DOM 与登录态残留。
import { afterEach, vi } from 'vitest'

afterEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  vi.restoreAllMocks()
})
