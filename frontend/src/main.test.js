// @vitest-environment jsdom

import { expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({
  listShops: vi.fn().mockResolvedValue([]),
}))

it('将应用挂载到约定的根节点', async () => {
  document.body.innerHTML = '<div id="app"></div>'

  await import('./main.js')

  expect(document.querySelector('#app h1')?.textContent).toBe('轻量级外卖服务平台')
}, 40_000)
