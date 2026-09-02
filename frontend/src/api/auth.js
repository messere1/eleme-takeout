// 认证接口封装（桩）：方法签名与契约保持一致，业务实现待绿灯阶段补充。
// 页面测试通过 vi.mock('@/api/auth') 在 api 层伪造响应，不依赖真实网络。
// 契约：docs/api-contract-v1.md §4.1（登录）、§4.2（用户注册）。

export function login(_payload) {
  throw new Error('待功能开发：登录接口尚未实现')
}

export function register(_payload) {
  throw new Error('待功能开发：注册接口尚未实现')
}
