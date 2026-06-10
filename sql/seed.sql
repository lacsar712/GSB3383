USE online_ordering;
SET NAMES utf8mb4;

INSERT INTO users (username, password_hash, role, created_at)
VALUES ('admin', '65536:JpAJGKse8QpsKK9IW5N4FA==:wnAltb5GLAMDYvw0tfTlILS/urFkNv9hAcgUK21GA7c=', 'ADMIN', NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO users (username, password_hash, role, created_at)
VALUES ('test_user', '65536:Ghat2iHwblBYSYMbieckZg==:EShNSTHapEqI+Wa8t2H4DyfiJo+BBWwhvHClyUEA+GY=', 'USER', NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO menu_items (name, description, price, image_url, is_available, created_at)
VALUES
    ('黑椒牛肉饭', '黑椒汁牛肉配蔬菜，口味浓郁', 28.00, '/assets/images/menu/black-pepper-beef-rice.jpg', 1, NOW()),
    ('宫保鸡丁饭', '经典川味宫保鸡丁，微辣下饭', 24.00, '/assets/images/menu/kung-pao-chicken-rice.jpg', 1, NOW()),
    ('番茄意面', '番茄罗勒风味意大利面，酸甜适中', 26.50, '/assets/images/menu/tomato-pasta.jpg', 1, NOW()),
    ('鲜虾沙拉', '低脂轻食，搭配油醋汁', 22.00, '/assets/images/menu/shrimp-salad.jpg', 1, NOW()),
    ('香辣鸡翅', '外酥里嫩，香辣口感', 18.00, '/assets/images/menu/spicy-chicken-wings.jpg', 0, NOW());
