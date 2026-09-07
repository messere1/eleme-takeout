# 第一阶段需求—测试追踪矩阵

状态说明：“通过”表示当前自动化回归通过；“回归失败”表示已有测试稳定暴露实现或契约问题；“部分覆盖”表示后端测试已具备，但前端业务入口尚不完整。

| 需求 | 验收重点 | 自动化测试 | 当前状态 |
| --- | --- | --- | --- |
| FR-001 | 用户注册、重复账号、参数约束 | `UserRegistrationServiceTest`、`UserRegistrationControllerTest`、`Register.test.js` | 通过 |
| FR-002 | 用户登录、JWT、鉴权和失败提示 | `UserLoginServiceTest`、`MerchantLoginServiceTest`、`UserLoginControllerTest`、`JwtServiceRobustnessTest`、`SecurityIntegrationTest`、`Login.test.js` | 通过 |
| FR-003 | 用户资料查询、修改和身份隔离 | `UserProfileServiceTest`、`UserProfileControllerTest`、`Profile.test.js` | 通过 |
| FR-004 | 商家注册、重复约束和默认店铺 | `MerchantRegistrationServiceTest`、`MerchantRegistrationControllerTest`、`MerchantRegister.test.js` | 通过 |
| FR-005 | 商家独立登录 | `MerchantLoginServiceTest`、`MerchantLogin.test.js` | 通过 |
| FR-006 | 商家修改所属店铺营业状态 | `ShopStatusServiceTest`、`ShopStatusControllerTest`、`MerchantConsole.test.js` | 通过 |
| FR-007 | 店铺查询、修改、资源不存在和所属权 | `ShopManagementServiceTest`、`ShopControllerContractTest`、`MerchantConsole.test.js` | 通过 |
| FR-008 | 分类增删改查、重复名称、资源归属和非空删除 | `CategoryServiceTest`、`CategoryBoundaryServiceTest`、`CategoryControllerContractTest`、`MerchantConsole.test.js` | 通过 |
| FR-009 | 商品增删改查、上下架、资源归属 | `ProductServiceTest`、`ProductBoundaryServiceTest`、`ProductControllerContractTest`、`MerchantProducts.test.js` | 通过 |
| FR-010 | 商品价格为正数且最多两位小数 | `ProductPriceValidationTest`、`RequestDtoBoundaryTest` | 通过 |
| FR-011 | 库存非负、商品与店铺校验 | `ProductStockServiceTest`、`ProductBoundaryServiceTest`、`ProductControllerContractTest` | 回归失败：库存操作返回资源不存在或抛错 |
| FR-012 | 添加购物车、数量合并、下架与商品不存在 | `CartAddServiceTest`、`CartBoundaryServiceTest`、`CartControllerContractTest`、`Shop.test.js` | 回归失败：停业店铺仍可加购；后端相关测试已通过 |
| FR-013 | 购物车金额、商品可用性和库存提示 | `CartQueryServiceTest`、`CartControllerContractTest`、`Cart.test.js` | 通过 |
| FR-014 | 修改数量、零值边界、库存和越权 | `CartUpdateServiceTest`、`CartControllerContractTest`、`Cart.test.js` | 回归失败：前端未处理更新失败；后端相关测试已通过 |
| FR-015 | 删除单项、清空购物车和越权 | `CartDeleteServiceTest`、`CartControllerContractTest`、`Cart.test.js` | 通过 |
| FR-016 | 事务下单、空/跨店/下架、库存和响应明细 | `OrderCreationServiceTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest` | 回归失败：空购物车和下架商品错误码与当前契约断言不一致；订单主流程已通过 |
| FR-017 | 订单列表、筛选及分页边界 | `OrderListServiceTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest`、`Orders.test.js` | 回归失败：`size=101` 未拒绝 |
| FR-018 | 订单详情、非法角色和访问控制 | `OrderDetailAuthorizationTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest`、`OrderDetail.test.js` | 通过 |
| FR-020 | 统一成功/失败响应与追踪编号 | `ApiResponseTest`、`http.test.js` | 通过 |
| FR-021 | 参数错误及字段级错误响应 | `ValidationErrorTest`、`RequestDtoBoundaryTest`、各 Controller 参数测试 | 通过 |

## 追踪规则

1. 每次需求变更先更新本矩阵，再编写能在旧行为上准确失败的测试。
2. 测试提交使用 `test(FR-xxx): ...`，不得与实现提交混合。
3. 新增或修改功能至少包含正常、边界、异常和权限场景中的适用项。
4. 功能负责人完成实现后，由测试专员回归并更新状态和缺陷证据。
