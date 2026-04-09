-- Abilita l'estensione per la generazione di UUID (gen_random_uuid())
-- già inclusa in PostgreSQL 13+ ma esplicitarla è buona pratica.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Statistiche estese — utile per EXPLAIN ANALYZE in dev
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";