# 第一阶段 P0 测试用例（SRS V1.2）

## 1. 范围与执行规则

- 需求基线：`srs/软件需求规格说明书-SRS-V1.2.docx`。
- 本轮仅新增或调整测试、接口契约和回归记录，不修改 Controller、Service、Mapper 或 Vue 页面实现。
- “红灯”表示测试已准确暴露当前实现与 SRS V1.2 的差异，应由功能负责人提交最小实现后再回归。
- 所有分页接口统一验证 `page=1`、`size=20`、`size<=100`；合法空页返回 200 和空 `items`。

## 2. 后端接口与业务用例

| 编号 | 模块 | 前置/输入 | 预期结果 | 自动化位置 |
| --- | --- | --- | --- | --- |
| P0-BE-001 | 店铺分页 | 不传分页参数 | 采用 `page=1,size=20` | `SrsV12ApiMappingContractTest`；接口实现后补集成断言 |
| P0-BE-002 | 店铺分页 | `size=100` / `size=101` | 100 合法；101 返回 400 和 `traceId` | 接口实现后执行集成测试 |
| P0-BE-003 | 店铺分页 | 合法条件无数据 | 200，`items=[]`，分页元数据完整 | 接口实现后执行集成测试 |
| P0-BE-004 | 店铺分页 | 查询发生未预期异常 | 500、通用文案、非空 `traceId`，无内部信息 | `ApiResponseTest` |
| P0-BE-005 | 商品分页 | 分类存在，包含上架、下架和逻辑删除商品 | 只返回上架且未删除商品 | `SrsV12ApiMappingContractTest`；接口实现后补数据集成断言 |
| P0-BE-006 | 商品分页 | 首尾页、空页、`size=100/101` | 边界元数据正确；空页 200；101 返回 400 | 接口实现后执行集成测试 |
| P0-BE-007 | 商品详情 | 商品不存在、下架或逻辑删除 | 均返回 404 和 `traceId` | `SrsV12ApiMappingContractTest`；接口实现后补集成断言 |
| P0-BE-008 | 价格 | `99999999.99` | 接受并精确保留两位小数 | `ProductPriceValidationTest` |
| P0-BE-009 | 价格 | `100000000.00`、`1.001` | 返回校验错误，不溢出、不隐式截断 | `ProductPriceValidationTest`、`RequestDtoBoundaryTest` |
| P0-BE-010 | 购物车隔离 | 两个用户加入同一商品；两个店铺有商品 | 条目按当前用户和店铺隔离 | `CartAddServiceTest`、`CartQueryServiceTest` |
| P0-BE-011 | 购物车越权 | 用户修改他人条目 | 403/404，不修改原条目 | `CartUpdateServiceTest` |
| P0-BE-012 | 重复加购 | 已有数量与新增数量之和超过库存 | 409，数量保持不变 | `CartAddServiceTest` |
| P0-BE-013 | 创建订单 | 正常购物车 | 初始状态 `CREATED` | `OrderCreationServiceTest` |
| P0-BE-014 | 创建订单 | 连续创建两笔订单 | 订单号均非空且唯一 | `OrderCreationServiceTest` |
| P0-BE-015 | 订单金额 | 含需小数运算的价格与数量 | 快照合计使用 `HALF_UP`，结果精确两位 | `OrderCreationServiceTest` |
| P0-BE-016 | 临时闭店 | 店铺状态 `TEMP_CLOSED` 后下单 | 409，不创建订单、不扣库存 | `OrderCreationServiceTest` |
| P0-BE-017 | 订单分页 | 不传参数、`size=100/101` | 默认 20；100 合法；101 返回 400 | `OrderListServiceTest`、`OrderBoundaryServiceTest`、`SrsV12ApiMappingContractTest` |
| P0-BE-018 | 时间筛选 | 合法起止时间、起始晚于结束 | 闭区间筛选；反向区间返回 400 | `OrderListServiceTest` |
| P0-BE-019 | 稳定排序 | 多笔订单创建时间相同 | 固定 `created_at DESC,id DESC` | `OrderListServiceTest` |

## 3. 安全与健壮性用例

| 编号 | 场景 | 输入/触发方式 | 预期结果 | 自动化位置 |
| --- | --- | --- | --- | --- |
| P0-SEC-001 | 失败响应追踪 | 400、401、403、404、409、500 | 每个响应含非空 `traceId` | `ApiResponseTest`、`SecurityIntegrationTest`、各边界测试 |
| P0-SEC-002 | 内部信息保护 | 触发权限、缺失、冲突和系统异常 | 不包含堆栈、Java 类名、SQL、表名和驱动信息 | `ApiResponseTest` |
| P0-SEC-003 | 密码复杂度 | 少于 6 位、超过 64 位、纯字母、纯数字 | 注册返回字段级校验错误 | `RequestDtoBoundaryTest` |
| P0-SEC-004 | 超长与枚举 | 用户名/昵称/地址/店铺字段超长；非法角色和状态 | 400，字段错误明确 | `RequestDtoBoundaryTest`、相关 Controller 测试 |
| P0-SEC-005 | SQL 注入式输入 | 名称、账号等包含引号、注释符或恒真表达式 | 作为普通数据校验/参数绑定，不改变查询语义 | `PersistenceSecurityContractTest`、注册/登录测试 |
| P0-SEC-006 | 敏感信息日志 | DTO/异常被格式化或记录 | 不输出明文密码、Token、完整手机号 | `SensitiveDataExposureTest`；日志集成检查待实现后执行 |

## 4. 前端状态与防重复提交用例

| 编号 | 页面/范围 | 场景 | 预期结果 | 自动化位置 |
| --- | --- | --- | --- | --- |
| P0-FE-001 | 店铺/商品/购物车/订单/资料/商家页 | 请求未完成 | 显示加载状态，不显示陈旧成功状态 | 各 `*.test.js` 页面测试 |
| P0-FE-002 | 列表页面 | 返回空数组或空分页 | 显示明确空状态，不白屏 | `Cart.test.js`、`Orders.test.js`、`MerchantOrders.test.js` 等 |
| P0-FE-003 | 全页面 | 后端返回 403 | 显示无权限状态，不无限重试 | 页面参数化失败测试 |
| P0-FE-004 | 详情/列表页面 | 后端返回 404 | 显示资源不存在或空状态 | 页面参数化失败测试 |
| P0-FE-005 | 全页面 | 后端返回 500 | 显示通用错误及可反馈的 `traceId`，不白屏 | `http.test.js`、页面参数化失败测试 |
| P0-FE-006 | 注册/保存/加购/结算/创建商品 | 首次提交 Promise 未结束时再次点击 | 按钮禁用，只发送一次请求 | `Register.test.js`、`Profile.test.js`、`Shop.test.js`、`Cart.test.js`、`MerchantConsole.test.js`、`MerchantProducts.test.js` |
| P0-FE-007 | API 客户端 | SRS V1.2 新路径、分页和方法 | 请求 URL、HTTP 方法、参数与契约一致 | `srsV12Contract.test.js` |

## 5. 1280×720 浏览器验收

当前工程尚未配置 Playwright/Cypress，因此本项先作为第一阶段必须执行的真实浏览器验收，不用组件测试冒充视口测试。实现页面可运行后，在 Chrome、Edge、Firefox 各执行一次：

1. 将视口固定为 1280×720，分别打开登录、注册、店铺列表、店铺详情、购物车、订单列表、订单详情、商家控制台、商品管理和商家订单页。
2. 在正常、加载中、空数据、403、404、500 六类状态下检查页面。
3. 断言 `document.documentElement.scrollWidth <= document.documentElement.clientWidth`，且正文、弹窗、表格、按钮和错误提示均可见、可操作。
4. 保存每个失败页面的浏览器、页面、状态、截图和复现步骤；任何横向溢出均按 P0 缺陷登记。

## 6. 完成判定

第一阶段本职责范围完成的标准是：上述测试已提交、能够编译/运行、当前实现差异以红灯或缺陷记录呈现，接口文档同步到 V1.2；不以测试人员代写核心业务代码作为完成条件。功能负责人修复后必须重新执行目标测试和前后端全量回归，最终要求零失败、零未处理异常。
