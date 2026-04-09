-- ─────────────────────────────────────────────────────────────
-- V1: Schema iniziale — orders + order_items
-- Flyway esegue questo script in una singola transazione.
-- ─────────────────────────────────────────────────────────────

-- Enum PostgreSQL per lo stato dell'ordine.
-- Più efficiente di VARCHAR: occupa 4 byte, validazione a livello DB.
CREATE TYPE order_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

-- ── Tabella orders ────────────────────────────────────────────
CREATE TABLE orders (
    id            UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id   UUID            NOT NULL,
    status        order_status    NOT NULL DEFAULT 'PENDING',
    total_amount  NUMERIC(12, 2)  NOT NULL CHECK (total_amount >= 0),
    notes         VARCHAR(500),
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version       BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_orders PRIMARY KEY (id)
);

-- Indici dichiarati anche a livello JPA (@Index) per la validazione
-- di Hibernate (ddl-auto: validate controlla che esistano).
CREATE INDEX idx_orders_customer_id
    ON orders (customer_id);

CREATE INDEX idx_orders_status
    ON orders (status);

CREATE INDEX idx_orders_created_at
    ON orders (created_at DESC);

-- Indice composito per la query più frequente:
-- "tutti gli ordini PENDING di un dato customer"
CREATE INDEX idx_orders_customer_status
    ON orders (customer_id, status);

-- ── Tabella order_items ───────────────────────────────────────
CREATE TABLE order_items (
    id            UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id      UUID            NOT NULL,
    product_id    UUID            NOT NULL,
    product_name  VARCHAR(255)    NOT NULL,
    quantity      INTEGER         NOT NULL CHECK (quantity > 0),
    unit_price    NUMERIC(10, 2)  NOT NULL CHECK (unit_price >= 0),
    subtotal      NUMERIC(12, 2)  NOT NULL CHECK (subtotal >= 0),
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_order_items     PRIMARY KEY (id),
    CONSTRAINT fk_items_order_id  FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE          -- elimina items quando l'ordine viene eliminato
        ON UPDATE CASCADE
);

CREATE INDEX idx_order_items_order_id
    ON order_items (order_id);

CREATE INDEX idx_order_items_product_id
    ON order_items (product_id);

-- ── Trigger: aggiorna updated_at automaticamente ──────────────
-- Evita di dipendere dall'applicazione per questo campo.
-- Spring Auditing (@LastModifiedDate) fa lo stesso, ma avere
-- il trigger è un safety net se si eseguono update diretti sul DB.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ── Commenti sulle tabelle (visibili in psql \d+ e pgAdmin) ───
COMMENT ON TABLE  orders                IS 'Ordini dei clienti';
COMMENT ON COLUMN orders.version        IS 'Optimistic locking — incrementato ad ogni UPDATE';
COMMENT ON COLUMN orders.total_amount   IS 'Somma dei subtotali degli order_items';
COMMENT ON TABLE  order_items           IS 'Righe di dettaglio di un ordine';
COMMENT ON COLUMN order_items.subtotal  IS 'quantity * unit_price — denormalizzato per performance';