// @vitest-environment jsdom

import { expect, it } from 'vitest'

it('将应用挂载到约定的根节点', async () => {
  document.body.innerHTML = '<div id="app"></div>'

  await import('./main.js')

  expect(document.querySelector('#app h1')?.textContent).toBe('轻量级外卖服务平台')
}, 20_000)
