# 第一阶段回归缺陷清单

以下问题均由自动化测试复现。本文件只记录缺陷与影响，不包含业务修复。

| 编号 | 严重度 | 模块 | 现象与预期 | 测试证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BUG-P1-004 | 高 | 商品库存 | 库存不足、负数和原子扣减场景返回资源不存在或抛错 | `ProductStockServiceTest` | 待修复 |
| BUG-P1-006 | 高 | 购物车页面 | 修改数量失败时未显示后端原因，并产生未处理 Promise 异常 | `Cart.test.js` | 已修复 |
| BUG-P1-007 | 高 | 店铺页面 | 店铺为 `CLOSED` 时加购按钮仍可用 | `Shop.test.js` | 已修复 |
| BUG-P1-008 | 中 | 创建订单 | 空购物车和下架商品使用细分错误码，与当前测试契约中的 `BUSINESS_CONFLICT` 不一致 | `OrderBoundaryServiceTest` | 待确认契约 |
| BUG-P1-010 | 中 | 订单分页 | 默认仍为 `size=10`，且 `size=101` 未被拒绝 | `OrderBoundaryServiceTest`、`OrderListServiceTest`、`Orders.test.js` | 待修复 |
| BUG-P1-011 | 高 | V1.2 API 契约 | 缺少店铺分页、分类商品分页、商品详情和独立价格接口；购物车写路径/方法仍为旧版 | `SrsV12ApiMappingContractTest`、`srsV12Contract.test.js` | 待修复 |
| BUG-P1-012 | 高 | 订单契约 | 新订单仍为 `PENDING`，列表默认仍为 10 条，前端筛选也使用旧状态和旧分页 | `OrderCreationServiceTest`、`OrderListServiceTest`、`Orders.test.js` | 待修复 |
| BUG-P1-013 | 高 | 异常响应 | 缺少未预期异常的统一 500 处理，前端 `ApiError` 未保留 `traceId` | `ApiResponseTest`、`http.test.js` | 待修复 |
| BUG-P1-014 | 高 | 敏感信息 | 认证请求诊断文本包含明文密码和完整手机号 | `SensitiveDataExposureTest` | 待修复 |
| BUG-P1-015 | 高 | 页面异常状态 | 资料、商家店铺/商品/订单页面未正确呈现 403、404 或 500，部分产生未处理 Promise 异常 | `Profile.test.js`、`MerchantConsole.test.js`、`MerchantProducts.test.js`、`MerchantOrders.test.js` | 待修复 |
| BUG-P1-016 | 中 | 防重复提交 | 加购、保存店铺和创建商品请求期间按钮仍可点击 | `Shop.test.js`、`MerchantConsole.test.js`、`MerchantProducts.test.js` | 待修复 |
| BUG-P1-017 | 高 | 临时闭店 | `TEMP_CLOSED` 状态下顾客仍可点击加购；后端下单约束已有测试保护 | `Shop.test.js`、`OrderCreationServiceTest` | 已修复 |

## 本轮已关闭

- 用户/商家错误凭证文案测试已通过。
- 商家登录页 4 个测试已通过。
- 商家注册成功跳转测试已通过。
- 后端购物车下架、库存不足和更新数量测试已通过。
- 订单创建主流程、库存冲突处理和成功响应明细测试已通过。
- 商家商品管理 5 个测试已通过。
- 商家订单管理 4 个测试及取消订单测试已通过。
- 购物车修改失败处理、`CLOSED` 与 `TEMP_CLOSED` 禁止加购已由远端提交 `58797e1` 修复并回归通过。

## 当前未通过汇总

- 后端：137 项中 14 失败、2 错误，主要涉及 V1.2 接口、订单默认值/状态、通用 500、敏感信息、既有商品库存和订单边界。
- 前端：94 项中 73 通过、21 失败，另有 5 个未处理异常；红灯集中于 V1.2 API 契约、错误状态、`CREATED`/20 条分页和防重复提交。
- 1280×720 横向溢出尚待真实浏览器验收，未在缺少运行页面时虚报通过。

## 关闭条件

1. 功能负责人提交最小修复，不修改或删除失败测试。
2. 对应目标测试、同模块测试和前后端全量回归均通过。
3. 若错误码或文案契约发生变更，先完成团队评审并同步 SRS、API 文档和测试。
