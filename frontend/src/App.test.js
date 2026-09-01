// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import App from './App.vue'

describe('第一阶段应用入口', () => {
  it('展示平台名称与当前阶段状态', () => {
    const wrapper = mount(App)

    expect(wrapper.get('h1').text()).toBe('轻量级外卖服务平台')
    expect(wrapper.text()).toContain('第一阶段开发中')
  })

  it('使用唯一的主内容区域承载页面', () => {
    const wrapper = mount(App)

    expect(wrapper.findAll('main')).toHaveLength(1)
    expect(wrapper.get('main').classes()).toContain('shell')
  })
})
