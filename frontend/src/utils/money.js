// 金额规则。SRS V2.0 §9.1「金额使用十进制字符串/数值且最多两位小数」、
// §4「商品价格 0.01–99,999,999.99 且最多 2 位小数」、EX-029「价格指数形式、三位小数、越界 → 400」、
// UC-06 异常流程「零/负/超额/三位小数」。
// 与后端 StrictMoneyDeserializer 的正则逐字一致，保证前端判定的合法集合与后端相同。

const MONEY_RE = /^(?:0|[1-9]\d{0,7})(?:\.\d{1,2})?$/

/** 形如 "12.50" / "0.01" 的普通十进制，最多两位小数，不接受指数形式与前导零。 */
export function isMoneyFormat(raw) {
  return MONEY_RE.test(String(raw ?? '').trim())
}

/** 金额必须能通过格式校验且严格大于 0（UC-06 的「零/负」都在此拦下）。 */
export function isPositiveMoney(raw) {
  const text = String(raw ?? '').trim()
  if (!MONEY_RE.test(text)) return false
  return Number(text) > 0
}
