# 前端测试约定

前端测试与后端完全分离：Vitest + jsdom + @vue/test-utils，mock 掉 api 模块即可测页面行为，**不需要后端运行或 mvn**。后端接口字段以 `docs/api-contract-v1.md` 为准。

## 运行命令（frontend/ 目录）

```bash
npm install        # 首次
npm test           # 全量运行（红灯阶段会有预期失败，退出码 1）
npm run test:watch # 监听模式，TDD 红绿循环
npm run coverage   # 覆盖率报告
npm run build      # 生产构建
```

## 目录规范

```
frontend/src/
├── views/     # 页面：Login.vue，测试同目录 Login.test.js
├── api/       # 后端接口封装：http.js（axios 实例）+ 按模块拆分（auth.js…）
├── utils/     # 通用工具（session.js 登录态）
└── test/      # 测试基础设施：setup.js、mountView.js（不放业务测试）
```

- 业务测试与被测文件同目录，命名 `*.test.js`。
- 页面测试用 `@/test/mountView` 挂载（自动装好 Element Plus 与内存路由），并 `vi.mock('@/api/xxx')` 伪造后端；**不要** mock 组件内部逻辑。
- 通用工具一律通过 `@` 别名导入（已在 `vite.config.js` 配置）。

## 提交规范

沿用后端 TDD 约定（`docs/phase-1-baseline.md`）：

- 测试提交：`test(FR-xxx): <页面> red baseline`
- 实现提交：`feat(FR-xxx): <页面>`
- 每步红灯测试单独提交，禁止把测试与实现混在一个提交里。

## 红灯 → 绿灯流程

1. 梳理该 FR 的验收行为（参考 SRS 与 api-contract）。
2. 先搭页面桩（能挂载、无业务逻辑），再写行为测试 → 跑 `npm test` 确认**按预期红**（失败原因是行为缺失，不是模块找不到）。
3. 单独提交红灯测试。
4. 实现页面使其转绿 → 单独提交实现。
5. 重构则再单独提交并回归。

## 页面测试的元素约定（testid）

为保证红灯测试与后续实现解耦，页面控件必须携带 `data-testid`，跳转断言统一读 `router.currentRoute.value.path`：

| 页面 | testid | 说明 |
| --- | --- | --- |
| 登录 | `login-account` / `login-password` / `login-submit` / `login-error` | 账号/密码输入、提交按钮、表单级错误区 |
| 注册 | `register-username` / `register-phone` / `register-password` / `register-submit` / `register-error` | 用户名/手机号/密码输入、提交按钮、错误区 |

约定的文案（测试按子串断言，实现须保持一致）：

- 登录/注册空提交：`请输入……`（不调用 api）
- 用户名：`用户名需 3~30 个字符`
- 手机号：`手机号格式不正确`
- 密码：`密码需 6~64 位且包含字母和数字`
- 登录失败：展示后端返回的 `msg`（如 `用户名或密码错误`）
- 注册 409：展示 `data.fieldErrors` 字段级文案

## mock 形状

- 登录成功：`{ token, role: 'CUSTOMER', expiresIn }` → 存 token 并跳转 `/`。
- 注册成功：`UserView` → 提示成功并跳转 `/login`。
- 业务失败：reject 一个带 `code` / `message` / `data.fieldErrors` 的 Error（页面只展示其文案）。
