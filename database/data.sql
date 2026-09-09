-- 外卖平台演示数据
-- 登录密码稍后统一设置为 abc123，并在此处保存 BCrypt 哈希。

INSERT INTO merchants (
    id,
    merchant_name,
    phone,
    password_hash,
    business_scope,
    created_at
)
VALUES (
    1001,
    '梅园',
    '13900139000',
    '$2a$10$稍后替换为真实的BCrypt哈希',
    '中式快餐',
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

INSERT INTO shops (
    id,
    merchant_id,
    shop_name,
    notice,
    status
)
VALUES (
    2001,
    1001,
    '北洋餐厅',
    '营业时间 08:00—21:00',
    'OPEN'
)
ON CONFLICT DO NOTHING;

INSERT INTO categories (
    id,
    shop_id,
    name,
    sort_order
)
VALUES
    (3001, 2001, '热销', 1),
    (3002, 2001, '主食', 2)
ON CONFLICT DO NOTHING;

INSERT INTO products (
    id,
    shop_id,
    category_id,
    name,
    description,
    price,
    stock,
    status,
    deleted
)
VALUES
    (
        4001,
        2001,
        3001,
        '煎饼果子',
        '现做现卖',
        8.50,
        20,
        'ON_SALE',
        FALSE
    ),
    (
        4002,
        2001,
        3001,
        '鸡蛋灌饼',
        '香酥可口',
        7.00,
        0,
        'ON_SALE',
        FALSE
    ),
    (
        4003,
        2001,
        3002,
        '牛肉面',
        '暂时下架',
        15.00,
        10,
        'OFF_SALE',
        FALSE
    )
ON CONFLICT DO NOTHING;