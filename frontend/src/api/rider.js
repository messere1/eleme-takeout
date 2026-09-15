import { http } from './http'

export function registerRider(payload) {
  return http.post('/riders', payload)
}
