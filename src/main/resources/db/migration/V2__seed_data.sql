-- ============================================================
-- V2: Seed dữ liệu mẫu
-- ============================================================

INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices and accessories'),
    ('Furniture',   'Office and home furniture'),
    ('Software',    'Software licenses and subscriptions');

INSERT INTO tags (name) VALUES
    ('bestseller'),
    ('new-arrival'),
    ('sale'),
    ('premium');

INSERT INTO items (name, price, stock, category_id) VALUES
    ('Laptop Pro 15',       25000000, 10, (SELECT id FROM categories WHERE name = 'Electronics')),
    ('Mechanical Keyboard',  2500000, 50, (SELECT id FROM categories WHERE name = 'Electronics')),
    ('4K Monitor',          12000000, 15, (SELECT id FROM categories WHERE name = 'Electronics')),
    ('Standing Desk',        8500000,  5, (SELECT id FROM categories WHERE name = 'Furniture')),
    ('Ergonomic Chair',      6000000,  8, (SELECT id FROM categories WHERE name = 'Furniture'));

-- Tag items
INSERT INTO item_tags (item_id, tag_id)
SELECT i.id, t.id FROM items i, tags t
WHERE i.name = 'Laptop Pro 15' AND t.name IN ('bestseller', 'premium');

INSERT INTO item_tags (item_id, tag_id)
SELECT i.id, t.id FROM items i, tags t
WHERE i.name = 'Mechanical Keyboard' AND t.name = 'new-arrival';
