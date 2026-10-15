CREATE TABLE IF NOT EXISTS feconomy_balances (
    uuid CHAR(36) CHARACTER SET ascii NOT NULL,
    username VARCHAR(16) NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (uuid),
    CONSTRAINT feconomy_balances_non_negative CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
