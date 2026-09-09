# 第一阶段需求—测试追踪矩阵

需求基线已更新为 SRS V1.4。状态说明：“通过”表示现有自动化回归通过；“红灯”表示已提交测试且当前实现尚未满足；“既有缺陷”表示 V1.4 变更前已经存在的失败；“待浏览器验收”表示必须在可运行页面中补真实视口证据。

| 需求 | 验收重点 | 自动化测试 | 当前状态 |
| --- | --- | --- | --- |
| FR-001 | 用户注册、重复账号、密码复杂度、超长字段 | `UserRegistrationServiceTest`、`UserRegistrationControllerTest`、`RequestDtoBoundaryTest`、`Register.test.js` | 通过 |
| FR-002 | 用户登录、JWT、鉴权和失败提示 | `UserLoginServiceTest`、`MerchantLoginServiceTest`、`UserLoginControllerTest`、`JwtServiceRobustnessTest`、`SecurityIntegrationTest`、`Login.test.js` | 通过 |
| FR-003 | 用户资料查询、修改和身份隔离 | `UserProfileServiceTest`、`UserProfileControllerTest`、`Profile.test.js` | 通过 |
| FR-004 | 商家注册、重复约束和默认店铺 | `MerchantRegistrationServiceTest`、`MerchantRegistrationControllerTest`、`MerchantRegister.test.js` | 通过 |
| FR-005 | 商家独立登录 | `MerchantLoginServiceTest`、`MerchantLogin.test.js` | 通过 |
| FR-006 | 商家修改所属店铺营业状态 | `ShopStatusServiceTest`、`ShopStatusControllerTest`、`MerchantConsole.test.js` | 通过 |
| FR-007 | 店铺分页、修改、资源不存在和所属权 | `ShopManagementServiceTest`、`ShopControllerContractTest`、`SrsV12ApiMappingContractTest`、`MerchantConsole.test.js` | 通过 |
| FR-008 | 分类增删改查、重复名称、资源归属和非空删除 | `CategoryServiceTest`、`CategoryBoundaryServiceTest`、`CategoryControllerContractTest`、`MerchantConsole.test.js` | 通过 |
| FR-009 | 商品分页/详情、增删改查、可见性和资源归属 | `ProductServiceTest`、`ProductBoundaryServiceTest`、`ProductControllerContractTest`、`SrsV12ApiMappingContractTest`、`MerchantProducts.test.js` | 通过 |
| FR-010 | 商品价格范围、精度和独立修改接口 | `ProductPriceValidationTest`、`RequestDtoBoundaryTest`、`SrsV12ApiMappingContractTest` | 通过 |
| FR-011 | 库存非负、商品与店铺校验 | `ProductStockServiceTest`、`ProductBoundaryServiceTest`、`ProductControllerContractTest` | 通过 |
| FR-012 | 添加购物车、用户/店铺隔离、数量合并和库存 | `CartAddServiceTest`、`CartBoundaryServiceTest`、`CartControllerContractTest`、`SrsV12ApiMappingContractTest`、`Shop.test.js` | 通过 |
| FR-013 | 购物车金额、商品可用性和库存提示 | `CartQueryServiceTest`、`CartControllerContractTest`、`Cart.test.js` | 通过 |
| FR-014 | 修改数量、零值边界、库存和越权 | `CartUpdateServiceTest`、`CartControllerContractTest`、`Cart.test.js` | 通过 |
| FR-015 | 删除单项、清空购物车和越权 | `CartDeleteServiceTest`、`CartControllerContractTest`、`Cart.test.js` | 通过 |
| FR-016 | 事务下单、初始状态、唯一订单号、金额和停业限制 | `OrderCreationServiceTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest` | 通过 |
| FR-017 | 订单列表、时间筛选、稳定排序及分页边界 | `OrderListServiceTest`、`OrderBoundaryServiceTest`、`SrsV12ApiMappingContractTest`、`Orders.test.js` | 通过 |
| FR-018 | 订单详情、非法角色和访问控制 | `OrderDetailAuthorizationTest`、`OrderBoundaryServiceTest`、`OrderControllerContractTest`、`OrderDetail.test.js` | 通过 |
| FR-019 | 顾客取消本人 CREATED 订单、幂等与库存回补 | `OrderWorkflowServiceTest`、`Orders.test.js` | 通过 |
| FR-020 | 统一响应、全失败追踪编号和内部信息保护 | `ApiResponseTest`、`SecurityIntegrationTest`、`SensitiveDataExposureTest`、`http.test.js` | 既有缺陷：缺通用 500 处理；注册 DTO 诊断文本暴露密码和完整手机号 |
| FR-021 | 参数错误及字段级错误响应 | `ValidationErrorTest`、`RequestDtoBoundaryTest`、各 Controller 参数测试 | 通过 |
| FR-022 | 商家接单/完成、顾客确认收货、非法状态与越权 | `SrsV14ApiContractTest`、`OrderWorkflowServiceTest`、`MerchantOrders.test.js`、`Orders.test.js` | 通过 |
| FR-023 | 管理员只读查询用户和商家账户，禁止写操作 | `SrsV14ApiContractTest.exposesReadOnlyAdministratorAccountLists`、`srsV14RouteContract.test.js` | 红灯：管理员接口与前端页面尚未实现 |
| FR-024 | 下单采集并保存收货人、联系电话、地址快照 | `SrsV14ApiContractTest.createOrderRequestCarriesCompleteRecipientSnapshot`、`srsV12Contract.test.js`、`Cart.test.js` | 红灯：当前仅有 `address`，缺姓名、联系电话和完整快照契约 |

## 追踪规则

1. 每次需求变更先更新本矩阵，再编写能在旧行为上准确失败的测试。
2. 测试提交使用 `test(FR-xxx): ...`，不得与实现提交混合。
3. 新增或修改功能至少包含正常、边界、异常和权限场景中的适用项。
4. 功能负责人完成实现后，由测试专员回归并更新状态和缺陷证据。
5. 1280×720 无横向溢出属于真实浏览器验收，执行步骤见 `phase-1-p0-srs-v1.2-test-cases.md`。
