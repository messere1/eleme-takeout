# 第一阶段需求—测试追踪矩阵

状态“红灯基线”表示测试代码已完成且可编译，当前因对应 Service 尚未实现而按预期失败。

| 需求 | 验收重点 | 自动化测试 | 状态 |
| --- | --- | --- | --- |
| FR-001 | 用户注册、重复账号、参数约束 | `UserRegistrationServiceTest`、`UserRegistrationControllerTest` | 红灯基线 |
| FR-002 | 用户登录、JWT 签发与校验、接口鉴权 | `UserLoginServiceTest`、`MerchantLoginServiceTest`、`UserLoginControllerTest`、`JwtServiceRobustnessTest`、`SecurityIntegrationTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-003 | 用户资料查询与修改、身份隔离 | `UserProfileServiceTest`、`UserProfileControllerTest` | 红灯基线 |
| FR-004 | 商家注册、名称/手机号重复、默认店铺 | `MerchantRegistrationServiceTest`、`MerchantRegistrationControllerTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-005 | 商家独立登录 | `MerchantLoginServiceTest` | 红灯基线 |
| FR-006 | 商家修改所属店铺营业状态 | `ShopStatusServiceTest`、`ShopStatusControllerTest`、`ShopControllerContractTest` | 红灯基线 |
| FR-007 | 店铺信息查询、资源不存在、所属权与修改 | `ShopManagementServiceTest`、`ShopControllerContractTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-008 | 分类增删改查、重复名称/排序、资源不存在、所属权 | `CategoryServiceTest`、`CategoryBoundaryServiceTest`、`CategoryControllerContractTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-009 | 商品增删改查、上下架、资源不存在与所属权 | `ProductServiceTest`、`ProductBoundaryServiceTest`、`ProductControllerContractTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-010 | 商品价格为正数且最多两位小数 | `ProductPriceValidationTest` | 已通过 |
| FR-011 | 库存非负、商品与店铺校验 | `ProductStockServiceTest`、`ProductBoundaryServiceTest`、`ProductControllerContractTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-012 | 添加购物车、数量合并、下架与商品不存在 | `CartAddServiceTest`、`CartBoundaryServiceTest`、`CartControllerContractTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-013 | 购物车金额、商品可用性和库存提示 | `CartQueryServiceTest`、`CartControllerContractTest` | 红灯基线 |
| FR-014 | 修改购物车数量、零值边界和越权防护 | `CartUpdateServiceTest`、`CartControllerContractTest`、`RequestDtoBoundaryTest` | 红灯基线 |
| FR-015 | 删除单项、清空购物车和越权防护 | `CartDeleteServiceTest`、`CartControllerContractTest` | 红灯基线 |
| FR-016 | 事务化创建订单、空/跨店/下架场景、库存和购物车一致性 | `OrderCreationServiceTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest` | 红灯基线 |
| FR-017 | 用户订单列表、筛选、分页及上下界 | `OrderListServiceTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest` | 红灯基线 |
| FR-018 | 订单不存在、非法角色及用户/商家访问控制 | `OrderDetailAuthorizationTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest`、`SecurityIntegrationTest` | 红灯基线 |
| FR-020 | 统一成功与失败响应、追踪编号 | `ApiResponseTest` | 已通过 |
| FR-021 | 参数错误及字段级错误响应 | `ValidationErrorTest`、各 Controller 参数校验测试 | 已通过 |
| 前端入口 | 平台名称、阶段状态、主内容区域、应用挂载 | `App.test.js`、`main.test.js` | 已通过 |

## 追踪规则

1. 每次需求变更先更新本矩阵，再编写失败测试。
2. 测试提交使用 `test(FR-xxx): ...`，不得与实现提交混合。
3. 新增或修改功能至少包含正常、边界、异常和权限场景中的适用项。
4. 功能负责人完成实现后，将对应状态由“红灯基线”改为“已通过”，并记录回归结果。
