# 前后端最终测试记录

## 1. 测试结论

- 执行日期：2026-09-08
- 业务代码基线：`1a16150`（前端代码功能补全）
- 需求基线：SRS V1.2
- 总体结论：**关键业务覆盖率任务已完成，系统仍为有条件不通过**。`OrderService`、`ProductService` 的方法覆盖率均达到 100%，但后端仍有此前记录的 3 个安全测试失败。
- 本轮修改范围：仅维护测试、补充关键业务用例并更新记录，未修改前后端业务实现。

不能把当前版本标记为最终验收通过。后端剩余安全问题修复且全量回归变为零失败后，方可更新结论。

## 2. 测试环境

| 项目 | 版本/环境 |
| --- | --- |
| 操作系统 | Windows 11 x64 |
| Java | Eclipse Temurin 17.0.19 |
| Maven | 3.9.11 |
| Node.js | 24.11.1 |
| npm | 11.12.1 |
| 浏览器 | Microsoft Edge 130.0.2849.56 |
| 浏览器视口 | 1280×720 |

## 3. 执行结果

| 范围 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| 后端全量测试 | `cd backend && mvn test` | 167 项：164 通过、3 失败、0 错误、0 跳过 | 覆盖率新增测试全绿；系统安全回归未全绿 |
| 后端整体覆盖率 | `cd backend && mvn jacoco:report` | 行 91.77%、分支 80.67%、方法 83.93%、指令 90.23% | 整体行覆盖率超过 90% |
| `OrderService` | JaCoCo 类级统计 | 行 98.11%、分支 83.93%、方法 100%、指令 96.89% | 达到关键业务方法 90% 目标 |
| `ProductService` | JaCoCo 类级统计 | 行 98.20%、分支 89.47%、方法 100%、指令 97.61% | 达到关键业务方法 90% 目标 |
| 前端全量测试 | `cd frontend && npm test` | 95/95 通过，0 失败 | 通过 |
| 前端覆盖率 | `cd frontend && npm run coverage` | 语句/行 89.88%、分支 75.53%、函数 63.69% | 已记录，函数与分支仍有提升空间 |
| 前端生产构建 | `cd frontend && npm run build` | 构建成功，1702 个模块完成转换 | 通过，存在体积警告 |
| 浏览器冒烟 | Edge，1280×720 | 6/6 页面无横向溢出，无页面运行错误 | 通过（不含真实后端联调） |

## 4. 后端剩余失败

| 编号 | 测试 | 实际问题 | 风险 | 建议关闭条件 |
| --- | --- | --- | --- | --- |
| FINAL-BE-001 | `ApiResponseTest.unexpectedExceptionHasUnifiedInternalErrorHandler` | `GlobalExceptionHandler` 没有针对 `Exception` 的统一兜底处理 | 未预期异常可能返回非统一 500 响应，缺少稳定错误码和 `traceId`，也可能泄露内部信息 | 增加通用 500 响应后，该测试及后端全量测试通过 |
| FINAL-BE-002 | `SensitiveDataExposureTest.authenticationRequestsDoNotExposePasswordsInDiagnosticText` | `UserRegistrationRequest.toString()` 包含明文密码 | DTO 被日志或异常输出时可能泄露密码 | 诊断文本隐藏密码，相关安全测试通过 |
| FINAL-BE-003 | `SensitiveDataExposureTest.authenticationRequestsMaskCompletePhoneNumbersInDiagnosticText` | `UserRegistrationRequest.toString()` 包含完整手机号 | 日志可能泄露个人敏感信息 | 诊断文本掩码手机号，相关安全测试通过 |

除上述 3 项外，其余 164 个后端测试均通过。订单创建与权限、商家订单分页、取消/接单/完成/确认、购物车隔离、店铺与商品分页、商品详情、价格精度与改价、库存、统一接口映射、注册登录、分类和用户资料均已有通过证据。

## 5. 测试适配记录

最新订单实现增加了 `UserMapper`、`MerchantMapper` 依赖，下单方法增加收货地址参数，Controller 增加 `CreateOrderRequest`。原订单测试因此出现 18 个编译错误。本轮只做测试代码适配：

- `OrderBoundaryServiceTest`
- `OrderCreationServiceTest`
- `OrderListServiceTest`
- `OrderDetailAuthorizationTest`
- `OrderControllerContractTest`

适配后原有 137 个后端测试均能执行。随后新增两个覆盖率测试类，共 30 项：

- `OrderWorkflowServiceTest`：14 项，覆盖商家订单分页、取消及并发取消、接单、完成、顾客确认、权限和联系方式脱敏。
- `ProductQueryAndPriceServiceTest`：16 项，覆盖商品分页、可见详情、独立改价、商家商品列表及资源不存在/越权边界。

两个新增测试类 30/30 通过。全量增至 167 项，其中 164 项通过，剩余 3 项仍是既有安全红灯。测试调整未放宽 SRS V1.2 验收条件。

## 6. 关键业务覆盖率完成记录

| 关键类 | 补充前方法覆盖率 | 补充后方法覆盖率 | 补充后行覆盖率 | 结果 |
| --- | ---: | ---: | ---: | --- |
| `OrderService` | 73.33% | 100% | 98.11% | 达标 |
| `ProductService` | 85.71% | 100% | 98.20% | 达标 |

后端整体行覆盖率由 80.92% 提升至 91.77%，整体方法覆盖率由 72.30% 提升至 83.93%。课程要求针对“关键业务方法”，因此以两个关键 Service 的类级方法覆盖率作为本项关闭证据；不将项目中 DTO、配置类和简单访问器的整体方法覆盖率误作关键业务方法指标。

## 7. 前端构建与覆盖风险

生产构建成功，但有以下非阻断警告：

1. `src/router/index.js` 同时被静态和动态导入，动态导入无法形成独立代码块。
2. 主 JavaScript 产物约 1149.65 kB（gzip 约 377.12 kB），超过 Vite 的 500 kB 警告阈值。
3. 前端函数覆盖率 63.69%、分支覆盖率 75.53%，较低区域主要位于 API 包装函数及商家商品/店铺页面分支。

这些问题不阻断当前功能测试，但建议在性能与代码质量验收前补充测试并拆分主包。

## 8. 1280×720 浏览器记录

使用 Microsoft Edge 在固定 1280×720 视口检查以下页面：

| 页面 | 实际路径 | 横向溢出 | 页面结果 |
| --- | --- | --- | --- |
| 首页 | `/` | 无，`scrollWidth=clientWidth=1280` | 正常展示 |
| 登录 | `/login` | 无 | 正常展示 |
| 注册 | `/register` | 无 | 正常展示 |
| 店铺详情 | `/shops/1` | 无 | 后端未启动时显示静态预览提示 |
| 购物车 | `/cart` | 无 | 后端未启动时显示失败提示与空状态 |
| 订单列表 | `/orders` | 无 | 后端未启动时显示失败提示与空状态 |

浏览器未发现页面级 JavaScript 错误。Vite 代理记录的 `ECONNREFUSED` 来源于本次未启动后端服务，前端已展示可理解的降级状态。

## 9. 尚未覆盖的最终验收

1. 未启动真实后端与数据库，因此没有执行“浏览器 → API → 数据库 → 响应”的端到端联调。
2. 1280×720 本轮只验证 Edge，Chrome 和 Firefox 尚需补测。
3. 未执行并发压测、长时间稳定性测试及生产环境安全扫描。
4. `OrderService` 分支覆盖率为 83.93%，`ProductService` 为 89.47%；方法覆盖率目标已完成，分支覆盖率仍可继续提升，但当前没有课程明确的 90% 分支阈值。

## 10. 最终关闭标准

1. 修复 3 个后端安全失败，后端全量测试达到 167/167 或更新后的全部测试零失败。
2. 修复不得删除、跳过或弱化现有安全断言。
3. 补齐真实数据库联调和 Chrome/Firefox 1280×720 验收证据。
4. 关键业务方法覆盖率已完成；后续若团队新增分支覆盖率阈值，再按新阈值补充测试。
