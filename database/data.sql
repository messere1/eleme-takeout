-- 外卖平台演示数据
-- 顾客和商家的演示密码统一为 abc123；下列值为 cost=10 的 BCrypt 哈希。

INSERT INTO users (
    id,
    username,
    phone,
    password_hash,
    nickname,
    address,
    created_at
)
VALUES (
    1001,
    'customer',
    '13800138000',
    '$2a$10$EkQnuxhMEdZZU1fRy75TFOsuBbsstJ7uCjkEGwxaGd6EaQFjVhNkq',
    '演示顾客',
    '天津大学北洋园校区',
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

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
    '$2a$10$EkQnuxhMEdZZU1fRy75TFOsuBbsstJ7uCjkEGwxaGd6EaQFjVhNkq',
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

-- 显式演示 ID 写入后同步序列，避免后续正常注册或新增数据发生主键碰撞。
SELECT setval(pg_get_serial_sequence('users', 'id'), GREATEST((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('merchants', 'id'), GREATEST((SELECT MAX(id) FROM merchants), 1), true);
SELECT setval(pg_get_serial_sequence('shops', 'id'), GREATEST((SELECT MAX(id) FROM shops), 1), true);
SELECT setval(pg_get_serial_sequence('categories', 'id'), GREATEST((SELECT MAX(id) FROM categories), 1), true);
SELECT setval(pg_get_serial_sequence('products', 'id'), GREATEST((SELECT MAX(id) FROM products), 1), true);
