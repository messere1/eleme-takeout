# 轻量级外卖服务平台 API 接口文档 V1.2

## 1. 文档状态

- 接口前缀：`/api/v1`
- 建议本地地址：`http://localhost:8080`
- 数据格式：`application/json; charset=UTF-8`
- 时间格式：ISO 8601，例如 `2026-09-01T12:30:00`
- 金额格式：JSON number，后端使用两位小数的 `BigDecimal`
- 需求基线：`srs/软件需求规格说明书-SRS-V1.2.docx`
- 当前状态：V1.2 契约已确认，尚未对齐的实现由失败测试跟踪，详见 `phase-1-regression-defects.md`

本文档以 SRS V1.2 为契约依据。当前 Controller 或前端调用与本文不一致时，以本文和 SRS 为准，先提交失败测试，再由功能负责人修改实现。文件名暂保留 `api-contract-v1.md`，避免已有链接失效。

## 2. 统一约定

### 2.1 认证方式

登录成功后，前端保存 `data.token`，访问受保护接口时携带：

```http
Authorization: Bearer <token>
Content-Type: application/json
```

角色枚举：

- `CUSTOMER`：普通用户
- `MERCHANT`：商家

### 2.2 统一成功响应

除 HTTP 状态码外，所有接口统一返回以下结构：

```json
{
  "code": 0,
  "msg": "success",
  "data": {},
  "traceId": "d76c269f99024718aa341c652ba5af6c"
}
```

删除成功时 HTTP 状态仍为 `200`，`data` 为 `null`。用户和商家注册成功时 HTTP 状态为 `201`，其他成功接口通常为 `200`。

### 2.3 统一失败响应

```json
{
  "code": "VALIDATION_ERROR",
  "msg": "请求参数校验失败",
  "data": {
    "fieldErrors": {
      "phone": "手机号格式不正确"
    }
  },
  "traceId": "3b819c36e2b541e39ddc40337591252d"
}
```

前端判断成功应使用 `code === 0`，不要只依赖 HTTP 状态。报错时优先显示 `msg`；表单可读取 `data.fieldErrors` 显示字段级错误。向后端反馈问题时应同时提供 `traceId`。

所有失败响应（包括 400、401、403、404、409、500）必须包含非空 `traceId`。响应中的 `msg`、`data` 和字段错误不得包含 Java 类名、堆栈、SQL 语句、表名、数据库驱动信息、密码、Token 或完整手机号；500 统一使用可理解的通用文案，详细异常只允许记录在受控服务端日志中并通过 `traceId` 关联。

### 2.4 常用错误码

| HTTP 状态 | code                        | 含义                                         | 前端建议                                                |
| --------: | --------------------------- | -------------------------------------------- | ------------------------------------------------------- |
|       400 | `VALIDATION_ERROR`        | 请求字段或分页参数不合法                     | 保留表单并显示字段错误                                  |
|       401 | `AUTH_REQUIRED`           | 未携带登录令牌                               | 跳转登录页                                              |
|       401 | `AUTH_INVALID`            | 凭证错误或 Token 无效                        | 清理登录态并跳转登录页                                  |
|       401 | `AUTH_EXPIRED`            | Token 已过期                                 | 清理登录态并提示重新登录                                |
|       403 | `FORBIDDEN`               | 当前角色或资源归属无权访问                   | 显示无权限提示，不重复请求                              |
|       404 | `RESOURCE_NOT_FOUND`      | 用户、店铺、分类、商品、购物车项或订单不存在 | 提示资源不存在并刷新列表                                |
|       409 | `USER_ALREADY_EXISTS`     | 用户名或手机号已存在                         | 提示更换注册信息                                        |
|       409 | `MERCHANT_ALREADY_EXISTS` | 商家名称或手机号已存在                       | 提示更换注册信息                                        |
|       409 | `BUSINESS_CONFLICT`       | 库存不足、商品下架、店铺打烊等业务冲突       | 显示`msg` 并刷新相关数据                              |
|       409 | `ORDER_ALREADY_CANCELLED` | 对已取消订单再次执行取消                     | 保持已取消状态，不重复回补库存                          |
|       500 | `INTERNAL_ERROR`          | 未预期异常                                   | 显示通用提示并保留`traceId`，不得展示堆栈或数据库信息 |

## 3. 接口总览

| 模块   | 方法   | 路径                                                        | 权限              | 说明                           |
| ------ | ------ | ----------------------------------------------------------- | ----------------- | ------------------------------ |
| 认证   | POST   | `/api/v1/auth/login`                                      | 公开              | 用户或商家登录                 |
| 用户   | POST   | `/api/v1/users`                                           | 公开              | 用户注册                       |
| 用户   | GET    | `/api/v1/users/me`                                        | CUSTOMER          | 查询本人资料                   |
| 用户   | PATCH  | `/api/v1/users/me`                                        | CUSTOMER          | 修改本人资料                   |
| 商家   | POST   | `/api/v1/merchants`                                       | 公开              | 商家注册并创建初始店铺         |
| 店铺   | GET    | `/api/v1/shops?page=1&size=20`                            | 公开              | 店铺分页列表                   |
| 店铺   | GET    | `/api/v1/shops/{shopId}`                                  | 公开              | 查询店铺详情                   |
| 店铺   | PATCH  | `/api/v1/shops/{shopId}`                                  | MERCHANT          | 修改所属店铺信息               |
| 店铺   | PATCH  | `/api/v1/shops/{shopId}/status`                           | MERCHANT          | 修改所属店铺状态               |
| 分类   | GET    | `/api/v1/shops/{shopId}/categories`                       | 公开              | 查询店铺分类                   |
| 分类   | POST   | `/api/v1/shops/{shopId}/categories`                       | MERCHANT          | 新增所属店铺分类               |
| 分类   | PATCH  | `/api/v1/categories/{categoryId}`                         | MERCHANT          | 修改所属店铺分类               |
| 分类   | DELETE | `/api/v1/categories/{categoryId}`                         | MERCHANT          | 删除空分类                     |
| 商品   | GET    | `/api/v1/categories/{categoryId}/products?page=1&size=20` | 公开              | 分类下商品分页列表             |
| 商品   | GET    | `/api/v1/products/{productId}`                            | 公开              | 查询可见商品详情               |
| 商品   | POST   | `/api/v1/shops/{shopId}/products`                         | MERCHANT          | 新增商品                       |
| 商品   | PATCH  | `/api/v1/products/{productId}`                            | MERCHANT          | 修改商品                       |
| 商品   | PATCH  | `/api/v1/products/{productId}/price`                      | MERCHANT          | 修改价格                       |
| 商品   | PATCH  | `/api/v1/products/{productId}/status`                     | MERCHANT          | 商品上下架                     |
| 商品   | PATCH  | `/api/v1/products/{productId}/stock`                      | MERCHANT          | 修改库存                       |
| 商品   | DELETE | `/api/v1/products/{productId}`                            | MERCHANT          | 逻辑删除商品                   |
| 购物车 | POST   | `/api/v1/cart/items`                                      | CUSTOMER          | 添加商品                       |
| 购物车 | GET    | `/api/v1/cart`                                            | CUSTOMER          | 查询购物车                     |
| 购物车 | PATCH  | `/api/v1/cart/items/{itemId}`                             | CUSTOMER          | 修改数量；0 表示删除           |
| 购物车 | DELETE | `/api/v1/cart/items/{itemId}`                             | CUSTOMER          | 删除单项                       |
| 购物车 | DELETE | `/api/v1/cart`                                            | CUSTOMER          | 清空本人购物车                 |
| 订单   | POST   | `/api/v1/orders`                                          | CUSTOMER          | 按当前购物车创建订单           |
| 订单   | GET    | `/api/v1/orders`                                          | CUSTOMER          | 查询本人订单列表               |
| 订单   | GET    | `/api/v1/orders/{orderId}`                                | CUSTOMER/MERCHANT | 查询有权访问的订单详情         |
| 订单   | GET    | `/api/v1/merchant/orders`                                 | MERCHANT          | 查询本店订单列表               |
| 订单   | POST   | `/api/v1/orders/{orderId}/cancel`                         | CUSTOMER          | 取消本人 CREATED 订单（阶段2） |

## 4. 认证与账户

### 4.1 登录

`POST /api/v1/auth/login`

请求：

```json
{
  "account": "13800138000",
  "password": "abc123",
  "role": "CUSTOMER"
}
```

| 字段     | 类型   | 必填 | 约束                           |
| -------- | ------ | ---- | ------------------------------ |
| account  | string | 是   | 用户可使用账号，商家使用手机号 |
| password | string | 是   | 非空                           |
| role     | string | 是   | `CUSTOMER` 或 `MERCHANT`   |

成功响应 `data`：

```json
{
  "token": "eyJ...",
  "role": "CUSTOMER",
  "expiresIn": 7200
}
```

`expiresIn` 单位为秒。

### 4.2 用户注册

`POST /api/v1/users`，成功 HTTP `201`。

```json
{
  "username": "beiyang_user",
  "phone": "13800138000",
  "password": "abc123"
}
```

| 字段     | 类型   | 必填 | 约束                         |
| -------- | ------ | ---- | ---------------------------- |
| username | string | 是   | 3～30 个字符                 |
| phone    | string | 是   | 中国大陆手机号格式           |
| password | string | 是   | 6～64 位，同时包含字母和数字 |

成功响应 `data` 为 `UserView`。

### 4.3 查询本人资料

`GET /api/v1/users/me`，需要 `CUSTOMER`。

```json
{
  "id": 7,
  "username": "beiyang_user",
  "phone": "13800138000",
  "nickname": "北洋用户",
  "address": "天津市津南区海河教育园"
}
```

### 4.4 修改本人资料

`PATCH /api/v1/users/me`，需要 `CUSTOMER`。

```json
{
  "nickname": "北洋用户",
  "phone": "13800138000",
  "address": "天津市津南区海河教育园"
}
```

约束：昵称不超过 30 个字符，手机号格式合法，地址不超过 255 个字符，三个字段均不能为空。成功响应 `data` 为更新后的 `UserView`。

### 4.5 商家注册

`POST /api/v1/merchants`，成功 HTTP `201`。

```json
{
  "merchantName": "北洋餐厅",
  "phone": "13900139000",
  "password": "abc123",
  "businessScope": "中式快餐"
}
```

| 字段          | 类型   | 必填 | 约束                         |
| ------------- | ------ | ---- | ---------------------------- |
| merchantName  | string | 是   | 最多 50 个字符               |
| phone         | string | 是   | 中国大陆手机号格式           |
| password      | string | 是   | 6～64 位，同时包含字母和数字 |
| businessScope | string | 是   | 最多 100 个字符              |

成功响应 `data`：

```json
{
  "id": 12,
  "merchantName": "北洋餐厅",
  "phone": "13900139000",
  "businessScope": "中式快餐",
  "shopId": 20,
  "shopStatus": "CLOSED"
}
```

## 5. 店铺与分类

### 5.1 店铺数据结构

```json
{
  "id": 20,
  "merchantId": 12,
  "shopName": "北洋餐厅",
  "notice": "欢迎光临",
  "status": "OPEN"
}
```

店铺状态：`OPEN` 营业、`CLOSED` 关闭、`TEMP_CLOSED` 临时闭店。

### 5.2 查询店铺列表

`GET /api/v1/shops?page=1&size=20`，公开接口。

分页规则与订单列表一致：`page` 默认 1，`size` 默认 20、最大 100。空结果仍返回 HTTP 200 和空 `items`，不得返回 404。`data`：

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "total": 0
}
```

### 5.3 查询店铺详情

`GET /api/v1/shops/{shopId}`，公开接口，成功响应 `data` 为 `ShopView`。

### 5.4 修改店铺信息

`PATCH /api/v1/shops/{shopId}`，需要 `MERCHANT` 且只能修改本人店铺。

```json
{
  "shopName": "北洋餐厅",
  "notice": "营业时间 08:00—21:00"
}
```

`shopName` 最多 50 个字符，`notice` 最多 255 个字符，均不能为空。

### 5.5 修改营业状态

`PATCH /api/v1/shops/{shopId}/status`，需要 `MERCHANT` 且只能修改本人店铺。

```json
{
  "status": "OPEN"
}
```

### 5.6 分类数据结构

```json
{
  "id": 30,
  "shopId": 20,
  "name": "热销",
  "sort": 1
}
```

### 5.7 查询分类

`GET /api/v1/shops/{shopId}/categories`，公开接口，`data` 为 `CategoryView[]`。

### 5.8 新增分类

`POST /api/v1/shops/{shopId}/categories`，需要 `MERCHANT`。

```json
{
  "name": "热销",
  "sort": 1
}
```

`name` 最多 30 个字符，`sort` 为大于等于 0 的整数。同一店铺内名称和排序号应保持唯一。

### 5.9 修改与删除分类

- `PATCH /api/v1/categories/{categoryId}`：请求体与新增分类相同。
- `DELETE /api/v1/categories/{categoryId}`：分类下存在商品时返回 `409 BUSINESS_CONFLICT`。

两者均需要 `MERCHANT`，且只能操作本人店铺的分类。

## 6. 商品

### 6.1 商品数据结构

```json
{
  "id": 40,
  "shopId": 20,
  "categoryId": 30,
  "name": "煎饼果子",
  "description": "现做现卖",
  "price": 8.50,
  "stock": 20,
  "status": "ON_SALE"
}
```

商品状态：`ON_SALE` 上架、`OFF_SALE` 下架。

### 6.2 查询分类下商品

`GET /api/v1/categories/{categoryId}/products?page=1&size=20`

公开接口，`page` 默认 1，`size` 默认 20、最大 100；越界参数返回 400，合法空页返回 HTTP 200 和空 `items`。列表只返回 `ON_SALE` 且未逻辑删除的商品，按稳定顺序返回分页结构。下架或已删除商品不得出现。

### 6.3 查询商品详情

`GET /api/v1/products/{productId}`，公开接口。仅返回顾客可见商品；资源不存在、下架或已逻辑删除时返回 404，避免通过详情接口绕过列表可见性约束。

### 6.4 新增或修改商品

- 新增：`POST /api/v1/shops/{shopId}/products`
- 修改：`PATCH /api/v1/products/{productId}`

均需要 `MERCHANT`。请求体：

```json
{
  "name": "煎饼果子",
  "categoryId": 30,
  "description": "现做现卖",
  "price": 8.50,
  "stock": 20
}
```

| 字段        | 类型    | 必填 | 约束                                 |
| ----------- | ------- | ---- | ------------------------------------ |
| name        | string  | 是   | 最多 50 个字符                       |
| categoryId  | integer | 是   | 分类必须属于当前店铺                 |
| description | string  | 否   | 最多 500 个字符                      |
| price       | number  | 是   | 大于 0，整数最多 8 位，小数最多 2 位 |
| stock       | integer | 是   | 大于等于 0                           |

新商品初始状态应为 `OFF_SALE`。

### 6.5 价格、上下架、库存和删除

- `PATCH /api/v1/products/{productId}/price`，请求：`{"price":8.50}`；价格必须满足 `DECIMAL(10,2)`，不得隐式截断。
- `PATCH /api/v1/products/{productId}/status`，请求：`{"status":"ON_SALE"}`。
- `PATCH /api/v1/products/{productId}/stock`，请求：`{"stock":20}`。
- `DELETE /api/v1/products/{productId}`，执行逻辑删除，成功 `data` 为 `null`。

均需要 `MERCHANT` 且只能操作本人店铺的商品。

## 7. 购物车

所有购物车接口都需要 `CUSTOMER`，后端从 Token 获取用户编号，前端不得提交 `userId`。

### 7.1 添加商品

`POST /api/v1/cart/items`

```json
{
  "productId": 40,
  "quantity": 2
}
```

`quantity` 必须大于 0。重复添加同一商品时应累加数量。成功 `data`：

```json
{
  "id": 50,
  "productId": 40,
  "quantity": 2
}
```

### 7.2 查询购物车

`GET /api/v1/cart`

```json
{
  "items": [
    {
      "id": 50,
      "productId": 40,
      "productName": "煎饼果子",
      "price": 8.50,
      "quantity": 2,
      "subtotal": 17.00,
      "available": true,
      "unavailableReason": null
    }
  ],
  "totalAmount": 17.00
}
```

`unavailableReason` 当前可能为 `OFF_SALE` 或 `INSUFFICIENT_STOCK`。前端下单前应检查 `available`。

### 7.3 修改、删除与清空

- `PATCH /api/v1/cart/items/{itemId}`，请求 `{"quantity":3}`；数量为 `0` 时删除该项。
- `DELETE /api/v1/cart/items/{itemId}`，删除本人购物车单项。
- `DELETE /api/v1/cart`，清空本人购物车。

## 8. 订单

### 8.1 创建订单

`POST /api/v1/orders`，需要 `CUSTOMER`，无请求体。

后端从当前用户购物车创建订单。购物车为空、店铺未营业、商品已下架或库存不足时返回 `409 BUSINESS_CONFLICT`。

创建成功的订单初始状态必须为 `CREATED`。订单号全局唯一；金额由商品价格快照乘数量后求和，使用 `DECIMAL(10,2)`，最终采用 `HALF_UP` 保留两位小数。

### 8.2 查询订单列表

`GET /api/v1/orders`，需要 `CUSTOMER`。

Query 参数：

| 参数      | 类型    | 必填 | 默认值 | 说明              |
| --------- | ------- | ---- | ------ | ----------------- |
| status    | string  | 否   | 无     | 订单状态筛选      |
| startTime | string  | 否   | 无     | ISO 8601 起始时间 |
| endTime   | string  | 否   | 无     | ISO 8601 结束时间 |
| page      | integer | 否   | 1      | 从 1 开始         |
| size      | integer | 否   | 20     | 1～100            |

当 `startTime > endTime` 时返回 400。列表固定按 `createdAt DESC, id DESC` 排序，确保相同创建时间及跨页查询结果稳定。

示例：

```http
GET /api/v1/orders?status=CREATED&page=1&size=20&startTime=2026-09-01T00:00:00
```

成功 `data`：

```json
{
  "items": [
    {
      "id": 60,
      "orderNo": "T20260901001",
      "shopId": 20,
      "totalAmount": 17.00,
      "status": "CREATED",
      "createdAt": "2026-09-01T12:30:00"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 1,
  "totalPages": 1
}
```

### 8.3 查询订单详情

`GET /api/v1/orders/{orderId}`，允许 `CUSTOMER` 或 `MERCHANT`。

- 用户只能查看自己的订单。
- 商家只能查看本人店铺的订单。

成功 `data`：

```json
{
  "id": 60,
  "orderNo": "T20260901001",
  "shopId": 20,
  "totalAmount": 17.00,
  "status": "CREATED",
  "createdAt": "2026-09-01T12:30:00",
  "items": [
    {
      "productId": 40,
      "productName": "煎饼果子",
      "unitPrice": 8.50,
      "quantity": 2,
      "subtotal": 17.00
    }
  ]
}
```

购物车必须同时按当前用户和店铺隔离。用户不得查询或修改他人的购物车条目；重复加入后的总数量不得超过实时库存，冲突时返回 409 且不得部分写入。

### 8.4 查询本店订单

`GET /api/v1/merchant/orders`，需要 `MERCHANT`。仅返回当前商家所属店铺的订单；顾客令牌或其他店铺商家访问返回 403。响应采用订单列表分页结构，默认 `page=1`、`size=20`。

### 8.5 取消订单（阶段2 FR-019）

`POST /api/v1/orders/{orderId}/cancel`，需要 `CUSTOMER`。

- 仅订单所有者可取消自己的 `CREATED` 订单。
- 首次取消把状态改为 `CANCELLED`，并在同一事务内回补一次库存。
- 重复取消返回 `409 ORDER_ALREADY_CANCELLED`，不得再次回补。
- 并发取消最多一次成功。

`ACCEPTED`、`COMPLETED` 及 `/accept`、`/complete` 不属于当前 SRS V1.2 基线。需要此类状态或接口时，必须先登记 CR、更新状态转换表、本文档和测试。

## 9. 前端联调建议

1. Axios 基础地址配置为后端服务地址，业务请求统一添加 `/api/v1`。
2. 请求拦截器仅在存在 Token 时添加 `Authorization`，注册和登录不需要 Token。
3. 响应拦截器统一解包 `data`，以 `code === 0` 判定业务成功。
4. 遇到 `AUTH_INVALID` 或 `AUTH_EXPIRED` 时清理 Token、角色和用户缓存，然后跳转登录页。
5. 路由权限必须与后端一致：购物车和用户资料仅限用户端，店铺/分类/商品写操作仅限商家端。
6. 金额只用于展示和提交商品资料；购物车小计、总价和订单金额以后端响应为准。
7. 不在请求中传递当前用户或商家编号，身份统一从 JWT 获取。

## 10. 联调前检查清单

- [ ] 前后端确认服务地址、端口和跨域策略。
- [ ] 登录后能够正确保存并携带 Token。
- [ ] 正常、空数据、参数错误、未登录、越权五类响应均已验证。
- [ ] 前端字段名称与本文档保持一致，不自行转换金额和时间语义。
- [ ] 后端每完成一个 Service，先运行对应目标测试，再开始接口联调。
- [ ] 任何契约调整均先更新本文档和测试，不直接口头变更。
