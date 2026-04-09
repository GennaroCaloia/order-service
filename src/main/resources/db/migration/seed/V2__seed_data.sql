-- ─────────────────────────────────────────────────────────────
-- V2: Seed data per sviluppo e test locali.
-- NON eseguire in prod: usa spring.flyway.locations per profilo
-- o il flag flyway.target=V1 in produzione.
-- ─────────────────────────────────────────────────────────────

-- UUID fissi per customer e product — referenziabili nei test
-- senza bisogno di query preliminari.
DO $$
DECLARE
    -- Customers
    cust_alice  UUID := 'a0000000-0000-0000-0000-000000000001';
    cust_bob    UUID := 'a0000000-0000-0000-0000-000000000002';

    -- Products
    prod_laptop UUID := 'b0000000-0000-0000-0000-000000000001';
    prod_mouse  UUID := 'b0000000-0000-0000-0000-000000000002';
    prod_kbd    UUID := 'b0000000-0000-0000-0000-000000000003';
    prod_mon    UUID := 'b0000000-0000-0000-0000-000000000004';

    -- Orders
    ord_1 UUID := 'c0000000-0000-0000-0000-000000000001';
    ord_2 UUID := 'c0000000-0000-0000-0000-000000000002';
    ord_3 UUID := 'c0000000-0000-0000-0000-000000000003';
    ord_4 UUID := 'c0000000-0000-0000-0000-000000000004';
BEGIN

    -- ── Ordini ────────────────────────────────────────────────
    INSERT INTO orders (id, customer_id, status, total_amount, notes, created_at, updated_at)
    VALUES
        -- Alice: ordine PENDING appena creato
        (ord_1, cust_alice, 'PENDING',   1349.98, 'Consegna al piano',
         now() - INTERVAL '2 hours',    now() - INTERVAL '2 hours'),

        -- Alice: ordine già CONFIRMED
        (ord_2, cust_alice, 'CONFIRMED',  249.99, NULL,
         now() - INTERVAL '3 days',     now() - INTERVAL '2 days'),

        -- Bob: ordine SHIPPED
        (ord_3, cust_bob,   'SHIPPED',   1899.00, 'Fragile',
         now() - INTERVAL '7 days',     now() - INTERVAL '1 day'),

        -- Bob: ordine DELIVERED (storico)
        (ord_4, cust_bob,   'DELIVERED',  89.90, NULL,
         now() - INTERVAL '30 days',    now() - INTERVAL '28 days');

    -- ── Items ordine 1 (Alice — PENDING) ─────────────────────
    INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, subtotal)
    VALUES
        (ord_1, prod_laptop, 'Laptop Pro 15"',    1, 1199.99, 1199.99),
        (ord_1, prod_mouse,  'Wireless Mouse',    1,   49.99,   49.99),
        (ord_1, prod_kbd,    'Mechanical Keyboard',1, 100.00,  100.00);

    -- ── Items ordine 2 (Alice — CONFIRMED) ───────────────────
    INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, subtotal)
    VALUES
        (ord_2, prod_mouse, 'Wireless Mouse',     5,  49.99,  249.95);
    -- Nota: total_amount = 249.99 include arrotondamento intenzionale
    -- per testare gestione delle discrepanze nel service layer.

    -- ── Items ordine 3 (Bob — SHIPPED) ───────────────────────
    INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, subtotal)
    VALUES
        (ord_3, prod_mon,    '4K Monitor 27"',    1,  899.00,  899.00),
        (ord_3, prod_laptop, 'Laptop Pro 15"',    1, 1000.00, 1000.00);

    -- ── Items ordine 4 (Bob — DELIVERED) ─────────────────────
    INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, subtotal)
    VALUES
        (ord_4, prod_kbd, 'Mechanical Keyboard',  1,   89.90,   89.90);

END $$;