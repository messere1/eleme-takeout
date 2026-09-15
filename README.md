# 轻量级外卖服务平台（仿饿了么）

天津大学软件学院软件工程（系列）综合实践项目。本仓库采用 Monorepo 方式集中管理前端、后端、联调与项目文档。

> 当前状态：三周功能开发与联调已完成。后端 286 项、前端 253 项测试全部通过（2026-09-15，`main` 分支）。

## 课程信息

- 实践周期：2026-08-31 至 2026-09-20（共 3 周）
- 小组规模：3–4 人
- 核心目标：TDD 测试优先开发、前后端分离、REST API、需求变更适配与交叉验收
- 协作要求：持续保留 Git 提交记录，禁止临近验收时集中上传代码

## 仓库结构

```text
eleme-takeout/
├── frontend/     Vue 3 前端工程与前端说明
├── backend/      Spring Boot 后端工程与后端说明
├── database/     表结构演进脚本与演示数据
├── docs/         API 契约、测试记录与验收材料
├── srs/          需求规格说明
├── scripts/      联调与冒烟脚本
└── README.md     项目总览
```

- [前端说明](frontend/README.md)
- [后端说明](backend/README.md)
- [API 接口文档 V2](docs/api-contract-v2.md)（现行）
- [API 接口文档 V1](docs/api-contract-v1.md)（历史版本，仅用于回归追踪）
- [店铺推荐算法说明](docs/recommendation.md)
- [第三阶段完整测试用例](docs/phase-3-complete-test-cases.md)
- [第二阶段联调最终记录（2026-09-14）](docs/phase-2-integration-final-2026-09-14.md)
- [浏览器兼容性检查（2026-09-14）](docs/browser-compatibility-check-2026-09-14.md)
- [前后端最终测试记录（2026-09-08）](docs/final-test-report-2026-09-08.md)

`docs/` 集中存放数据库设计、API 文档、架构设计、TDD 测试报告、需求迭代记录、团队分工、开发日志和交叉验收材料。

## 技术栈

### 前端

- Vue 3.5 + Vue Router 4.5
- Element Plus 2.9
- Axios 1.8
- Vite 6.3
- Vitest 3.1 + @vue/test-utils + jsdom
- Playwright / Selenium（浏览器兼容性验证）

### 后端

- Java 17 + Spring Boot 3.4.5
- MyBatis 3.0.4（注解式 SQL）
- PostgreSQL 16（测试用 H2，`MODE=PostgreSQL`）
- Spring Security + JWT
- JUnit 5 + MockMvc + Mockito + AssertJ
- JaCoCo（覆盖率）

## 本地运行

```bash
# 数据库：PostgreSQL，库名 take_out
psql -h localhost -p 5432 -U take_out -d take_out -f backend/src/main/resources/schema.sql

# 后端（默认连 localhost:15432，本地原生库在 5432 时需覆盖）
cd backend
DB_URL=jdbc:postgresql://localhost:5432/take_out mvn spring-boot:run   # → :8080

# 前端
cd frontend
npm install && npm run dev                                             # → :5173，代理 /api 到 8080
```

演示账号：顾客 `customer` / `abc123`，管理员 `admin` / `Admin123`，骑手 `rider` / `Rider123`。详见 `database/data.sql`。

## 核心功能

- 用户注册、登录与个人信息维护
- 商家注册及营业状态管理
- 店铺、商品分类、商品、价格和库存管理
- 购物车增删改查
- 订单创建、支付、列表和状态查询
- 退款申请与审核
- 骑手接单与配送
- 管理员治理（用户/商家/商品/订单/退款）
- 店铺推荐：按搜索历史与下单历史个性化排序，见 [算法说明](docs/recommendation.md)
- 前后端联调、异常提示与数据校验

## TDD 工作流

每项功能遵循以下顺序：

1. 梳理业务需求和验收行为。
2. 编写正常、异常和边界场景测试。
3. 运行测试并确认失败。
4. 编写最小实现使测试通过。
5. 重构并执行回归测试。
6. 分阶段提交，保留“红 - 绿 - 重构”的 Git 证据。

课程目标要求核心业务接口测试覆盖率 100%，关键业务方法覆盖率不低于 90%，并留存测试类、测试报告、测试日志和回归测试记录。

## 三周计划

| 阶段 | 时间 | 目标 | 状态 |
| --- | --- | --- | --- |
| 第 1 周 | 基础需求 | 搭建架构，完成用户、店铺、商品、购物车和基础订单功能及测试 | 已完成 |
| 第 2 周 | 需求迭代 | 先补测试再实现变更，执行旧功能回归测试，避免大面积重构 | 已完成 |
| 第 3 周 | 完善验收 | 重构、优化交互、完成联调和全量测试，整理验收材料 | 进行中 |

## API 基本约定

- 接口统一使用 `/api/v1` 前缀。
- URI 以资源名词为主，正确使用 HTTP 方法和状态码。
- 分页、过滤和排序通过 Query 参数传递。
- 后端统一返回 `code`、`msg`、`data`。
- 登录、取消订单等明确业务动作允许采用动作型子路径。

现行契约见 [API 接口文档 V2](docs/api-contract-v2.md)，在 `docs/` 中维护。

## 需求依据

- 本学期《轻量级外卖服务平台（仿饿了么）》课程实践说明
- 《饿了么前端项目任务书》中的页面流程与交互示例
- 《饿了么 JDBC 项目任务书》中的业务功能与数据库设计参考

旧任务书中的静态页面、Java SE 控制台和原生 JDBC 方案仅作为需求参考；本项目以本学期 Vue 3、Spring Boot/MyBatis、前后端分离和 TDD 要求为实施基线。

## AI 使用说明

项目允许使用 AI 辅助脚手架、样板代码、错误定位和文档初稿，但核心业务逻辑、测试设计、架构决策与交叉验收必须由小组理解并完成。后续将在项目文档中记录主要 AI 辅助使用点。

## 使用范围

本仓库为课程实践私有仓库，仅供项目组成员学习、协作与验收使用。
