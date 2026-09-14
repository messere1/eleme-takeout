// 骑手接口。
//
// 注意：骑手注册尚未纳入 SRS V2.0 范围（FR-001 只定义注册顾客，FR-004 只定义注册商家，
// §9 接口表里骑手只有 GET/POST /rider/orders/**）。这份封装是前后端自行约定的扩展，
// 与 POST /users（注册顾客）、POST /merchants（注册商家）保持同构，后端待实现。
//
// 约定契约（供后端实现对齐）：
//   POST /api/v1/riders          公开接口，无需 token
//   请求体：{ "riderName": "张三", "phone": "13900139000", "password": "abc123" }
//     - riderName 非空、最多 50 字符（与 SRS §4「商家 名称1–50」同规格；SRS 未给骑手姓名定长度）
//     - phone 大陆手机号 11 位，在有效骑手中唯一
//     - password 8~64 位且含字母和数字（与 SRS §4 顾客密码同规格）
//   成功：201，data 为骑手视图 { id, riderName, phone, enabled }，不得返回密码或摘要（FR-028）
//   失败：400 VALIDATION_ERROR（字段级错误放在 data.fieldErrors，页面按字段展示）
//         409 手机号已被占用（EX-001「重复用户名/手机号 → 409；不创建账号」）
//
// 注册成功后前端用同一手机号 + 密码调 POST /auth/login（role=RIDER）自动登录。
import { http } from './http'

export function registerRider(payload) {
  return http.post('/riders', payload)
}
