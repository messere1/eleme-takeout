// 收货联系电话规则。SRS V2.0 §4「购物车收货信息：电话规范化后 7–15 位」、
// EX-007「收货电话含空格、连字符或国际前缀 → 规范化后 7–15 位可接受；其他 400」。
// 与后端 CartService.normalizePhone 逐条对应：先去空格与连字符，再去掉开头的 +，最后必须是 7–15 位纯数字。

const RECIPIENT_PHONE_RE = /^\d{7,15}$/

export function normalizeRecipientPhone(raw) {
  return String(raw ?? '')
    .replace(/[\s-]/g, '')
    .replace(/^\+/, '')
}

export function isValidRecipientPhone(raw) {
  return RECIPIENT_PHONE_RE.test(normalizeRecipientPhone(raw))
}
