// 金额规则，与后端 StrictMoneyDeserializer 的正则逐字一致。

const MONEY_RE = /^(?:0|[1-9]\d{0,7})(?:\.\d{1,2})?$/

/** 形如 "12.50" / "0.01" 的普通十进制，最多两位小数，不接受指数形式与前导零。 */
export function isMoneyFormat(raw) {
  return MONEY_RE.test(String(raw ?? '').trim())
}

/** 金额格式合法且严格大于 0。 */
export function isPositiveMoney(raw) {
  const text = String(raw ?? '').trim()
  if (!MONEY_RE.test(text)) return false
  return Number(text) > 0
}
