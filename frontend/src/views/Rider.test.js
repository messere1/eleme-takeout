// @vitest-environment jsdom
// rider-available-<id> / rider-claim-<id> / rider-mine-<id> / rider-deliver-<id>
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({
  listRiderAvailable: vi.fn(),
  listRiderOrders: vi.fn(),
  claimOrder: vi.fn(),
  deliverOrder: vi.fn(),
}))

import { claimOrder, deliverOrder, listRiderAvailable, listRiderOrders } from '@/api/order'
import { mountView } from '@/test/mountView'
import { session } from '@/utils/session'
import Rider from './Rider.vue'

const AVAILABLE = [{ id: 70, orderNo: 'T20260901001', status: 'ACCEPTED' }]
const MINE = [{ id: 71, orderNo: 'T20260902002', status: 'DELIVERING' }]

async function mountRider(available = AVAILABLE, mine = MINE) {
  session.save({ token: 'rd-1', role: 'RIDER' })
  listRiderAvailable.mockResolvedValue(available)
  listRiderOrders.mockResolvedValue(mine)
  const ctx = await mountView(Rider, { path: '/rider' })
  await flushPromises()
  return ctx
}

describe('骑手配送台', () => {
  beforeEach(() => {
    session.clear()
    vi.clearAllMocks()
  })

  it('分别展示可接订单与本人配送任务', async () => {
    const { wrapper } = await mountRider()
    expect(wrapper.get('[data-testid="rider-available-70"]').text()).toContain('T20260901001')
    expect(wrapper.get('[data-testid="rider-mine-71"]').text()).toContain('T20260902002')
  })

  it('领取成功后刷新两个列表', async () => {
    claimOrder.mockResolvedValue({ id: 70, status: 'DELIVERING' })
    const { wrapper } = await mountRider()

    await wrapper.get('[data-testid="rider-claim-70"]').trigger('click')
    await flushPromises()

    expect(claimOrder).toHaveBeenCalledWith(70)
    expect(listRiderAvailable).toHaveBeenCalledTimes(2)
    expect(listRiderOrders).toHaveBeenCalledTimes(2)
  })

  // EX-017：两名骑手同时领取时仅一名成功，另一名 409「并刷新列表」。
  it('领取冲突时展示后端原因并刷新列表', async () => {
    claimOrder.mockRejectedValue(new Error('订单已被其他骑手领取'))
    const { wrapper } = await mountRider()

    await wrapper.get('[data-testid="rider-claim-70"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('订单已被其他骑手领取')
    expect(listRiderAvailable).toHaveBeenCalledTimes(2)
    expect(listRiderOrders).toHaveBeenCalledTimes(2)
  })

  it('确认送达调用接口并刷新列表', async () => {
    deliverOrder.mockResolvedValue({ id: 71, status: 'DELIVERED' })
    const { wrapper } = await mountRider()

    await wrapper.get('[data-testid="rider-deliver-71"]').trigger('click')
    await flushPromises()

    expect(deliverOrder).toHaveBeenCalledWith(71)
    expect(listRiderOrders).toHaveBeenCalledTimes(2)
  })

  // EX-018：越级送达或操作他单返回 403/409，同样刷新，界面不停在过期状态。
  it('送达被拒时展示后端原因并刷新列表', async () => {
    deliverOrder.mockRejectedValue(new Error('无权操作该订单'))
    const { wrapper } = await mountRider()

    await wrapper.get('[data-testid="rider-deliver-71"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('无权操作该订单')
    expect(listRiderOrders).toHaveBeenCalledTimes(2)
  })

  it('没有可接订单和配送任务时展示空状态', async () => {
    const { wrapper } = await mountRider([], [])

    expect(wrapper.get('[data-testid="rider-available-empty"]').text()).toContain('暂无可接订单')
    expect(wrapper.get('[data-testid="rider-mine-empty"]').text()).toContain('暂无配送任务')
  })
})
