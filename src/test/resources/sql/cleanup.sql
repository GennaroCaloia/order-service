-- Pulizia completa dopo ogni test che modifica i dati.
-- ON DELETE CASCADE su order_items garantisce che
-- basti eliminare le righe in orders.
DELETE FROM orders
WHERE id IN (
    'c0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000003',
    'c0000000-0000-0000-0000-000000000004'
);
-- Elimina anche gli ordini creati dal test end-to-end
-- (UUID random — non possiamo filtrarli per ID)
DELETE FROM orders
WHERE customer_id = 'a0000000-0000-0000-0000-000000000001'
  AND created_at > now() - INTERVAL '1 minute';