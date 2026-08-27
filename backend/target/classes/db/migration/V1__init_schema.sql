-- ============================================================================
-- Finance App - Initial schema
-- Fase 1 (implementada nesta entrega): users, accounts, categories, transactions
-- Fase 2+ (schema pronto, endpoints chegam nas próximas entregas): bills,
--   credit_cards, credit_card_transactions, investment_types, investments,
--   goals, debts, financial_settings
-- ============================================================================

-- ---------- FASE 1 ----------

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    email           VARCHAR(180)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(120)  NOT NULL,
    bank            VARCHAR(120),
    type            VARCHAR(30)   NOT NULL, -- CHECKING, SAVINGS, WALLET, DIGITAL, INVESTMENT, OTHER
    balance         NUMERIC(14,2) NOT NULL DEFAULT 0,
    color           VARCHAR(20)   DEFAULT '#1F6F54',
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_accounts_user ON accounts(user_id);

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        REFERENCES users(id) ON DELETE CASCADE, -- NULL = categoria padrão do sistema
    name            VARCHAR(80)   NOT NULL,
    type            VARCHAR(10)   NOT NULL, -- INCOME, EXPENSE
    icon            VARCHAR(10),
    is_default      BOOLEAN       NOT NULL DEFAULT false,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_categories_user ON categories(user_id);

CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id      BIGINT        NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id     BIGINT        REFERENCES categories(id) ON DELETE SET NULL,
    type            VARCHAR(10)   NOT NULL, -- INCOME, EXPENSE
    description     VARCHAR(200)  NOT NULL,
    amount          NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    date            DATE          NOT NULL,
    payment_method  VARCHAR(40),
    status          VARCHAR(20)   NOT NULL DEFAULT 'PAID', -- PAID, PENDING, LATE, SCHEDULED
    recurrence      VARCHAR(20)   NOT NULL DEFAULT 'NONE', -- NONE, WEEKLY, MONTHLY, YEARLY
    notes           TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_transactions_user_date ON transactions(user_id, date);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_category ON transactions(category_id);

-- ---------- FASE 2+ (schema já criado; endpoints/entidades chegam depois) ----------

CREATE TABLE bills (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id      BIGINT        REFERENCES accounts(id) ON DELETE SET NULL,
    category_id     BIGINT        REFERENCES categories(id) ON DELETE SET NULL,
    description     VARCHAR(200)  NOT NULL,
    amount          NUMERIC(14,2) NOT NULL,
    due_date        DATE          NOT NULL,
    recurrence      VARCHAR(20)   NOT NULL DEFAULT 'NONE',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, LATE
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_bills_user_due ON bills(user_id, due_date);

CREATE TABLE credit_cards (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                VARCHAR(120)  NOT NULL,
    bank                VARCHAR(120),
    credit_limit        NUMERIC(14,2) NOT NULL,
    closing_day         SMALLINT      NOT NULL,
    due_day             SMALLINT      NOT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE credit_card_transactions (
    id              BIGSERIAL PRIMARY KEY,
    credit_card_id  BIGINT        NOT NULL REFERENCES credit_cards(id) ON DELETE CASCADE,
    category_id     BIGINT        REFERENCES categories(id) ON DELETE SET NULL,
    description     VARCHAR(200)  NOT NULL,
    amount          NUMERIC(14,2) NOT NULL,
    purchase_date   DATE          NOT NULL,
    installments    SMALLINT      NOT NULL DEFAULT 1,
    installment_no  SMALLINT      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_cc_tx_card ON credit_card_transactions(credit_card_id);

CREATE TABLE investment_types (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(80) NOT NULL,   -- CDB, LCI, Tesouro, Ações, FII, ETF, Cripto...
    class   VARCHAR(30) NOT NULL    -- FIXED_INCOME, VARIABLE_INCOME, OTHER
);

CREATE TABLE investments (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    investment_type_id  BIGINT        REFERENCES investment_types(id),
    name                VARCHAR(150)  NOT NULL,
    invested_amount     NUMERIC(14,2) NOT NULL,
    current_amount      NUMERIC(14,2) NOT NULL,
    invested_at         DATE          NOT NULL,
    expected_rate       NUMERIC(6,3), -- % a.a. estimado
    institution         VARCHAR(120),
    notes               TEXT,
    created_at          TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_investments_user ON investments(user_id);

CREATE TABLE goals (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(150)  NOT NULL,
    target_amount   NUMERIC(14,2) NOT NULL,
    current_amount  NUMERIC(14,2) NOT NULL DEFAULT 0,
    deadline        DATE,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_goals_user ON goals(user_id);

CREATE TABLE debts (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    creditor            VARCHAR(150)  NOT NULL,
    original_amount     NUMERIC(14,2) NOT NULL,
    current_amount      NUMERIC(14,2) NOT NULL,
    interest_rate       NUMERIC(6,3),
    installments_total  SMALLINT,
    installments_paid   SMALLINT      NOT NULL DEFAULT 0,
    due_date            DATE,
    status              VARCHAR(20)   NOT NULL DEFAULT 'OPEN', -- OPEN, PAID
    created_at          TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_debts_user ON debts(user_id);

CREATE TABLE financial_settings (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE, -- NULL = default global
    savings_rate        NUMERIC(6,3) DEFAULT 6.17,   -- poupança, % a.a. aproximado (regra atual: 70% da Selic + TR, quando Selic <= 8,5% a.a.)
    cdb_rate            NUMERIC(6,3) DEFAULT 11.5,
    treasury_rate       NUMERIC(6,3) DEFAULT 11.0,
    fixed_income_rate   NUMERIC(6,3) DEFAULT 10.5,
    fii_rate            NUMERIC(6,3) DEFAULT 9.0,
    stocks_rate         NUMERIC(6,3) DEFAULT 12.0,
    updated_at          TIMESTAMP DEFAULT now()
);
