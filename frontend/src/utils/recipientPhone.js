// 收货联系电话规则，与后端 CartService.normalizePhone 一致：去掉空格与连字符、去掉开头的 +，
// 结果必须是 7–15 位纯数字。

const RECIPIENT_PHONE_RE = /^\d{7,15}$/

export function normalizeRecipientPhone(raw) {
  return String(raw ?? '')
    .replace(/[\s-]/g, '')
    .replace(/^\+/, '')
}

export function isValidRecipientPhone(raw) {
  return RECIPIENT_PHONE_RE.test(normalizeRecipientPhone(raw))
}
