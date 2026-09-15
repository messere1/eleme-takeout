# 轻量级外卖服务平台 API 契约 V2.0

基准日期：2026-09-11。基础路径：`/api/v1`。本文与 SRS V2.0 一致，V1.4 文档只用于历史追踪。

## 1. 公共约定

- 除注册、登录、公开店铺/商品、经营类别和图片访问外，请求头均携带 `Authorization: Bearer <token>`。
- 角色：`CUSTOMER`、`MERCHANT`、`RIDER`、`ADMIN`；Token 不得跨角色使用。
- 响应统一为 `{ "code": 0或业务码, "msg": "说明", "data": ..., "traceId": "追踪标识" }`。
- HTTP 状态：400参数错误，401未认证，403越权，404不存在，409业务冲突，500内部错误。
- 分页默认 `page=1&size=20`，最大100；金额范围 `0.01`～`99999999.99`，最多两位小数，不接受指数形式。

## 2. 账号、类别与商家

`POST /auth/login` 请求示例：

```json
{"account":"账号或手机号","password":"Abc12345","role":"CUSTOMER"}
```

成功返回 `token`、`role`、`expiresIn`。本地演示账号：管理员 `admin / Admin123`，骑手 `rider / Rider123`，共享部署前必须替换。

- `DELETE /users/me`（CUSTOMER）：无进行中订单时注销，旧凭据不可登录，手机号释放，历史数据隔离。
- `GET /business-categories`（公开）：默认含中式快餐、西式简餐、奶茶甜品、烧烤夜宵、日韩料理、地方菜系。
- `POST /merchants`（公开）：字段为 `merchantName`、`phone`、`password`、`businessScope`、`shopAddress`；自定义类别1～50字符。
- `PATCH /merchants/me`（MERCHANT）：修改 `businessScope`、`shopName`、`shopAddress`。
- `DELETE /merchants/me`（MERCHANT）：注销并阻止再次登录，历史订单与审计保留。

## 3. 购物车收货信息

- `GET /cart/delivery-info/{shopId}`（CUSTOMER）：读取当前顾客在指定店铺保存的信息。
- `PATCH /cart/delivery-info`（CUSTOMER）：保存信息。

```json
{
  "shopId":20,
  "recipientName":"张同学",
  "recipientPhone":"13800138000",
  "deliveryAddress":"天津大学北洋园校区学生宿舍1号楼",
  "saveToProfile":true
}
```

信息按顾客+店铺隔离；收货人1～50字符，电话可含 `+`、空格、连字符且规范化后7～15位，地址5～255字符。

## 4. 下单、支付与自动取消

`POST /orders`（CUSTOMER）使用与上节相同的完整收货字段。成功订单为 `status=CREATED`、`paymentStatus=UNPAID`，返回服务器生成的 `paymentDeadline`；商品和收货信息形成不可变快照，只清除所选店铺购物车。

`POST /orders/{orderId}/pay`（CUSTOMER）仅允许本人 `CREATED/UNPAID` 且服务器时间早于截止时间的订单。成功变为 `PAID`；重复已成功支付幂等。系统每30秒扫描到期未支付订单，改为 `CANCELLED` 并回补库存一次。支付与取消竞争时只能一方成功。

订单状态是「业务状态 + 支付状态」这一对（SRS §5.1），支付成功后 `status` 仍是 `CREATED`，只改 `paymentStatus`。因此 `GET /orders`、`GET /merchant/orders`、`GET /admin/orders` 的列表项除 `status` 外还必须返回 `paymentStatus` 与 `paymentDeadline`：

```json
{"id":60,"orderNo":"T20260901001","shopId":7,"totalAmount":17.00,
 "status":"CREATED","paymentStatus":"UNPAID","paymentDeadline":"2026-09-01T12:45:00",
 "createdAt":"2026-09-01T12:30:00"}
```

- `CREATED+UNPAID` 允许顾客支付与取消；`CREATED+PAID` 只允许商家接单或顾客申请退款——两者动作不同，页面不得只按 `status` 判断。
- 支付倒计时一律以 `paymentDeadline` 为准，页面不得用 `createdAt` 自行推算。
- 商家订单列表与顾客订单列表同样支持 `status`、`startTime`、`endTime`、`page`、`size`（FR-016）。

## 5. 退款

`POST /orders/{orderId}/refunds`（CUSTOMER）：

```json
{"amount":12.50,"reason":"漏放商品","evidenceUrls":["/uploads/evidence-1.jpg"]}
```

仅本人已支付订单；金额大于0且最多两位小数；待处理+已批准累计不得超过实付；原因最多255字符；证据最多3张。

- `GET /refunds`：顾客查看本人退款。
- `GET /merchant/refunds`、`PATCH /merchant/refunds/{id}`：本店商家查询并提交 `APPROVED` 或 `REJECTED`。
- `GET /admin/refunds`、`PATCH /admin/refunds/{id}`：管理员查询和处理。查询为分页，响应 `data` 是 `{items, page, size, total, totalPages}` 信封（与其他 admin 列表一致），不再是裸数组。

状态为 `PENDING → APPROVED | REJECTED`，终态不可重复处理。

## 6. 骑手配送

- `GET /rider/orders/available`：查看尚未领取的已接订单。
- `GET /rider/orders`：查看当前骑手任务。
- `POST /rider/orders/{id}/claim`：原子领取，`ACCEPTED → DELIVERING`；并发仅一人成功。
- `POST /rider/orders/{id}/deliver`：所属骑手操作，`DELIVERING → DELIVERED`。
- 顾客调用 `POST /orders/{id}/confirm`，`DELIVERED → COMPLETED`。

## 7. 管理员

仅 `ADMIN`：

- `GET /admin/users|merchants|products|orders|refunds`
- `PATCH /admin/users|merchants|products|orders/{id}/status`
- `PATCH /admin/refunds/{id}`

五个列表接口统一分页（默认 `page=1&size=20`，最大 100），响应 `data` 均为 `{items, page, size, total, totalPages}`。订单列表另支持 `status`、`startTime`、`endTime` 筛选（FR-016），且 `startTime` 不得晚于 `endTime`。

订单列表项不含收货地址，手机号仅以 `recipientPhoneMasked` 脱敏下发（NFR-004）。

账号状态为 `ENABLED|DISABLED`，商品为 `ON_SALE|OFF_SALE`，订单状态必须属于 SRS 状态机。写操作记录管理员、动作、目标和时间；列表手机号脱敏，不返回密码摘要或 Token。

## 8. 图片上传

`POST /images?targetType=...&targetId=...` 使用 `multipart/form-data`，文件字段 `file`。`targetType` 为 `USER_AVATAR`、`SHOP_IMAGE`、`SHOP_COVER`、`PRODUCT_IMAGE`。仅 JPEG/PNG/WebP，单文件≤5MiB，服务端校验真实文件头和目标归属。失败不更新URL，成功返回 `{ "url": "/uploads/<随机名>" }`。

## 9. 店铺推荐

推荐是**附带效果**，不改变任何已有接口的响应结构：`GET /shops` 对登录顾客把偏好命中的店铺提到当页前面，游客看到的顺序与本文其余部分描述的一致。

`GET /recommendations/shops`（公开，游客可用）：

- 查询参数 `limit` 默认 6，超出 1～50 时收敛到边界；游客或没有历史的顾客按全站热度返回，不返回 401。
- 返回 `data` 为店铺数组，元素与 `ShopView` 同构。
- 推荐只包含营业中店铺，已关店和商家被停用的店铺不在结果内。

`GET /shops/search` 在搜索成功后记录登录顾客的关键词，作为推荐的偏好信号；游客不记录，参数非法（关键词为空、`page<1`、`size` 超出 1～100）导致 400 时不记录。关键词超过 50 字符截断。`DELETE /users/me` 注销时清除本人全部搜索历史。

打分口径、权重、时间衰减和已知限制见 [店铺推荐算法说明](./recommendation.md)。

## 10. 前端验收

四类登录工作区完全隔离。购物车底部汇总常驻并向上展开；支付页按 `paymentDeadline` 倒计时；全部页面处理加载、空数据、403、404、409、500和重复点击。1280×720及360/390/430/520宽度无横向溢出。
