// 含空格、连字符或国际前缀的电话，规范化后 7–15 位可接受，其他必须被拒。
import { describe, expect, it } from 'vitest'

import { isValidRecipientPhone, normalizeRecipientPhone } from './recipientPhone'

describe('收货联系电话规则', () => {
  it('去掉空格、连字符与前导 +', () => {
    expect(normalizeRecipientPhone('+86 138-0013-8000')).toBe('8613800138000')
    expect(normalizeRecipientPhone('022 8535 6000')).toBe('02285356000')
    expect(normalizeRecipientPhone('13800138000')).toBe('13800138000')
  })

  it('容忍空值', () => {
    expect(normalizeRecipientPhone('')).toBe('')
    expect(normalizeRecipientPhone(null)).toBe('')
    expect(normalizeRecipientPhone(undefined)).toBe('')
  })

  it('规范化后 7–15 位数字可接受', () => {
    expect(isValidRecipientPhone('1234567')).toBe(true)
    expect(isValidRecipientPhone('13800138000')).toBe(true)
    expect(isValidRecipientPhone('+86 138-0013-8000')).toBe(true)
    expect(isValidRecipientPhone('123456789012345')).toBe(true)
  })

  it('位数不足、超长或规范化后非纯数字必须被拒', () => {
    expect(isValidRecipientPhone('123456')).toBe(false)
    expect(isValidRecipientPhone('1234567890123456')).toBe(false)
    expect(isValidRecipientPhone('++----')).toBe(false)
    expect(isValidRecipientPhone('-------+')).toBe(false)
    expect(isValidRecipientPhone('138 0013 800a')).toBe(false)
    expect(isValidRecipientPhone('')).toBe(false)
  })
})
