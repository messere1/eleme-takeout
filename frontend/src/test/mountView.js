import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'

function createTestRouter() {
  const Stub = { template: '<div class="route-stub">route stub</div>' }
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: Stub },
      { path: '/login', component: Stub },
      { path: '/register', component: Stub },
      { path: '/search', component: Stub },
      { path: '/shops/:id', component: Stub },
      { path: '/cart', component: Stub },
      { path: '/orders', component: Stub },
      { path: '/orders/:id', component: Stub },
      { path: '/profile', component: Stub },
      { path: '/merchant', component: Stub },
      { path: '/merchant/register', component: Stub },
      { path: '/rider', component: Stub },
      { path: '/merchant/login', component: Stub },
      { path: '/merchant/products', component: Stub },
      { path: '/merchant/orders', component: Stub },
      { path: '/merchant/profile', component: Stub },
      { path: '/admin', component: Stub },
      { path: '/rider', component: Stub },
      { path: '/orders/:id/pay', component: Stub },
    ],
  })
}

/**
 * 挂载一个页面组件，返回 { wrapper, router }。
 * @param {object} component 待挂载的 .vue 组件
 * @param {{ path?: string, global?: object }} options
 *   path —— 当前路由（默认 '/'）；global —— 附加的 mount 全局选项
 */
export async function mountView(component, { path = '/', global = {} } = {}) {
  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  const wrapper = mount(component, {
    global: {
      plugins: [router, ElementPlus],
      ...global,
    },
  })
  return { wrapper, router }
}
