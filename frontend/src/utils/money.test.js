// 金额只接受普通十进制、最多两位小数，拒绝指数形式、三位小数、零与负值。
import { describe, expect, it } from 'vitest'

import { isMoneyFormat, isPositiveMoney } from './money'

describe('金额格式', () => {
  it('接受普通十进制且最多两位小数', () => {
    expect(isMoneyFormat('0.01')).toBe(true)
    expect(isMoneyFormat('12.5')).toBe(true)
    expect(isMoneyFormat('12.50')).toBe(true)
    expect(isMoneyFormat('99999999.99')).toBe(true)
  })

  it('拒绝指数形式与三位及以上小数', () => {
    expect(isMoneyFormat('1e-7')).toBe(false)
    expect(isMoneyFormat('1E5')).toBe(false)
    expect(isMoneyFormat('1.234')).toBe(false)
  })

  it('拒绝前导零、负号、空值与带分隔符的写法', () => {
    expect(isMoneyFormat('007')).toBe(false)
    expect(isMoneyFormat('-1')).toBe(false)
    expect(isMoneyFormat('')).toBe(false)
    expect(isMoneyFormat('1,000')).toBe(false)
    expect(isMoneyFormat('abc')).toBe(false)
  })

  it('超出 8 位整数部分时拒绝（§4 上限 99,999,999.99）', () => {
    expect(isMoneyFormat('100000000')).toBe(false)
    expect(isMoneyFormat('999999999')).toBe(false)
  })
})

describe('正金额（UC-06 的零/负）', () => {
  it('严格大于 0 才算正金额', () => {
    expect(isPositiveMoney('0.01')).toBe(true)
    expect(isPositiveMoney('12.50')).toBe(true)
  })

  it('零与负值、非法格式一律不算', () => {
    expect(isPositiveMoney('0')).toBe(false)
    expect(isPositiveMoney('0.00')).toBe(false)
    expect(isPositiveMoney('-1')).toBe(false)
    expect(isPositiveMoney('1.234')).toBe(false)
    expect(isPositiveMoney('')).toBe(false)
  })
})
