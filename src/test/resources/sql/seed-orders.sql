-- Reinserisce i dati seed per i test che ne hanno bisogno.
-- Usa gli stessi UUID fissi del V2__seed_data.sql.
-- Eseguito BEFORE_TEST_METHOD — cleanup.sql lo pulisce dopo.

INSERT INTO orders (id, customer_id, status, total_amount, created_at, updated_at, version)
VALUES
    ('c0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000001',
     'PENDING', 1349.98, now(), now(), 0),

    ('c0000000-0000-0000-0000-000000000003',
     'a0000000-0000-0000-0000-000000000002',
     'SHIPPED', 1899.00, now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, subtotal, created_at)
VALUES
    (gen_random_uuid(),
     'c0000000-0000-0000-0000-000000000001',
     'b0000000-0000-0000-0000-000000000001',
     'Laptop Pro 15"', 1, 1199.99, 1199.99, now()),

    (gen_random_uuid(),
     'c0000000-0000-0000-0000-000000000003',
     'b0000000-0000-0000-0000-000000000004',
     '4K Monitor 27"', 1, 899.00, 899.00, now())
ON CONFLICT DO NOTHING;