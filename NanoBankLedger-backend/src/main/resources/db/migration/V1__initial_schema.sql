-- ============================================================
-- V1__initial_schema.sql
-- Esquema inicial del MVP NanoBank Ledger
-- ============================================================

-- Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ENUMs
-- ============================================================
CREATE TYPE wallet_type AS ENUM ('SAVINGS', 'CHECKING', 'INVESTMENT', 'CASH');
CREATE TYPE transaction_type AS ENUM ('INCOME', 'EXPENSE');
CREATE TYPE category_type AS ENUM ('INCOME', 'EXPENSE');

-- ============================================================
-- TABLA: users
-- ============================================================
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);

-- ============================================================
-- TABLA: refresh_tokens
-- ============================================================
CREATE TABLE refresh_tokens (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    family_id       UUID        NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMPTZ,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ============================================================
-- TABLA: wallets (billeteras)
-- ============================================================
CREATE TABLE wallets (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,
    type            wallet_type     NOT NULL DEFAULT 'CHECKING',
    balance         NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);

-- CHECK: balance >= 0 (no negativo)
ALTER TABLE wallets ADD CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0);

-- ============================================================
-- TABLA: categories (catálogo de categorías)
-- ============================================================
CREATE TABLE categories (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100)    NOT NULL,
    type            category_type   NOT NULL,
    icon            VARCHAR(50),
    color           VARCHAR(7),  -- Hex color, ej: #FF5733
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLA: transactions (movimientos financieros)
-- ============================================================
CREATE TABLE transactions (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_id       UUID            NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    category_id     UUID            REFERENCES categories(id) ON DELETE SET NULL,
    type            transaction_type NOT NULL,
    amount          NUMERIC(15,2)   NOT NULL,
    description     VARCHAR(255),
    date            DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_wallet_id ON transactions (wallet_id);
CREATE INDEX idx_transactions_category_id ON transactions (category_id);
CREATE INDEX idx_transactions_date ON transactions (date);
CREATE INDEX idx_transactions_type ON transactions (type);
CREATE INDEX idx_transactions_wallet_date ON transactions (wallet_id, date);

-- CHECK: amount > 0 (siempre positivo; el tipo INCOME/EXPENSE indica dirección)
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0);

-- ============================================================
-- FUNCIÓN: Actualizar updated_at automáticamente
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers para actualizar updated_at en todas las tablas
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_wallets_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- SEED: Categorías por defecto
-- ============================================================
INSERT INTO categories (id, name, type, icon, color) VALUES
    ('a1b2c3d4-0001-4000-8000-000000000001', 'Salario', 'INCOME', 'briefcase', '#346538'),
    ('a1b2c3d4-0001-4000-8000-000000000002', 'Freelance', 'INCOME', 'laptop', '#1F6C9F'),
    ('a1b2c3d4-0001-4000-8000-000000000003', 'Inversiones', 'INCOME', 'trending-up', '#956400'),
    ('a1b2c3d4-0001-4000-8000-000000000004', 'Otros ingresos', 'INCOME', 'plus-circle', '#787774'),
    ('a1b2c3d4-0001-4000-8000-000000000005', 'Alimentación', 'EXPENSE', 'shopping-cart', '#9F2F2D'),
    ('a1b2c3d4-0001-4000-8000-000000000006', 'Transporte', 'EXPENSE', 'car', '#1F6C9F'),
    ('a1b2c3d4-0001-4000-8000-000000000007', 'Vivienda', 'EXPENSE', 'home', '#956400'),
    ('a1b2c3d4-0001-4000-8000-000000000008', 'Servicios', 'EXPENSE', 'zap', '#346538'),
    ('a1b2c3d4-0001-4000-8000-000000000009', 'Salud', 'EXPENSE', 'heart', '#9F2F2D'),
    ('a1b2c3d4-0001-4000-8000-000000000010', 'Entretenimiento', 'EXPENSE', 'film', '#787774'),
    ('a1b2c3d4-0001-4000-8000-000000000011', 'Educación', 'EXPENSE', 'book', '#1F6C9F'),
    ('a1b2c3d4-0001-4000-8000-000000000012', 'Otros gastos', 'EXPENSE', 'more-horizontal', '#787774');
