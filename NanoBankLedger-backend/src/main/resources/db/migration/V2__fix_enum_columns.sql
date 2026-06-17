-- ============================================================
-- V2__fix_enum_columns.sql
-- Convierte columnas ENUM nativas a VARCHAR para compatibilidad
-- con Hibernate @Enumerated(EnumType.STRING)
-- ============================================================

-- 1. Wallet type: wallet_type -> VARCHAR(20)
ALTER TABLE wallets 
    ALTER COLUMN type TYPE VARCHAR(20);

-- 2. Transaction type: transaction_type -> VARCHAR(10)
ALTER TABLE transactions 
    ALTER COLUMN type TYPE VARCHAR(10);

-- 3. Category type: category_type -> VARCHAR(10)
ALTER TABLE categories 
    ALTER COLUMN type TYPE VARCHAR(10);

-- 4. Eliminar los tipos ENUM ya no necesarios
-- Usamos CASCADE porque hay DEFAULT values que dependen de estos tipos
DROP TYPE IF EXISTS wallet_type CASCADE;
DROP TYPE IF EXISTS transaction_type CASCADE;
DROP TYPE IF EXISTS category_type CASCADE;
