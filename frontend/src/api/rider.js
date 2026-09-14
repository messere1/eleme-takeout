// 骑手注册：POST /api/v1/riders（公开），请求体 { riderName, phone, password }。
import { http } from './http'

export function registerRider(payload) {
  return http.post('/riders', payload)
}
