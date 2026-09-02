// 全局测试环境清理
import { afterEach, vi } from 'vitest'

afterEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  vi.restoreAllMocks()
})
