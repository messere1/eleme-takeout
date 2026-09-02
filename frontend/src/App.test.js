// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { mountView } from '@/test/mountView'
import App from './App.vue'

describe('应用外壳', () => {
  it('展示平台名称', async () => {
    const { wrapper } = await mountView(App, { path: '/' })
    expect(wrapper.get('h1').text()).toBe('轻量级外卖服务平台')
  })

  it('使用唯一的主内容区域承载路由页面', async () => {
    const { wrapper } = await mountView(App, { path: '/' })
    expect(wrapper.findAll('main')).toHaveLength(1)
    expect(wrapper.get('main').classes()).toContain('shell')
  })
})
