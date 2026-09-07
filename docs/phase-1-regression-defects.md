# 第一阶段回归缺陷清单

以下问题均由自动化测试复现。本文件只记录缺陷与影响，不包含业务修复。

| 编号 | 严重度 | 模块 | 现象与预期 | 测试证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BUG-P1-001 | 中 | 用户/商家登录 | 错误凭证实际返回“账号或密码错误”，契约断言为“用户名或密码错误”；需统一契约 | `UserLoginServiceTest`、`MerchantLoginServiceTest` | 待功能负责人确认 |
| BUG-P1-002 | 高 | 商家登录页 | 页面为空，4 个登录交互用例失败 | `MerchantLogin.test.js` | 待修复 |
| BUG-P1-003 | 中 | 商家注册页 | 注册成功实际跳转 `/login`，预期为 `/merchant/login` | `MerchantRegister.test.js` | 待修复 |
| BUG-P1-004 | 高 | 商品库存 | 库存不足、负数和原子扣减场景返回资源不存在或抛错 | `ProductStockServiceTest` | 待修复 |
| BUG-P1-005 | 中 | 购物车 | 下架/库存不足使用的错误码与当前 API 契约不一致 | `CartAddServiceTest`、`CartUpdateServiceTest` | 待确认契约 |
| BUG-P1-006 | 高 | 购物车页面 | 修改数量失败时未显示后端原因，并产生未处理 Promise 异常 | `Cart.test.js` | 待修复 |
| BUG-P1-007 | 高 | 店铺页面 | 店铺为 `CLOSED` 时加购按钮仍可用 | `Shop.test.js` | 待修复 |
| BUG-P1-008 | 高 | 创建订单 | 空购物车、停业、下架和库存不足的错误码与契约不一致 | `OrderCreationServiceTest`、`OrderBoundaryServiceTest` | 待确认契约 |
| BUG-P1-009 | 高 | 创建订单 | 成功响应中的订单商品明细为空 | `OrderCreationServiceTest` | 待修复 |
| BUG-P1-010 | 中 | 订单分页 | `size=101` 未被拒绝，超过约定最大页大小 100 | `OrderBoundaryServiceTest` | 待修复 |
| GAP-P1-001 | 高 | 商家商品管理 | 前端缺少完整的商品新增、编辑、删除、上下架和库存管理入口 | 追踪矩阵 FR-009/FR-011 | 待功能开发 |

## 关闭条件

1. 功能负责人提交最小修复，不修改或删除失败测试。
2. 对应目标测试、同模块测试和前后端全量回归均通过。
3. 若错误码或文案契约发生变更，先完成团队评审并同步 SRS、API 文档和测试。
