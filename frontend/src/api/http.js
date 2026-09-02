// Axios 实例：统一 baseURL。
// 请求拦截器（携带 Bearer token）、响应拦截器（code === 0 解包、
// AUTH_INVALID/AUTH_EXPIRED 清理登录态并跳转）在接口联调阶段实现，
// 遵循 docs/api-contract-v1.md §2 与 §9。
import axios from 'axios'

export const http = axios.create({
  baseURL: '/api/v1',
})

export default http
