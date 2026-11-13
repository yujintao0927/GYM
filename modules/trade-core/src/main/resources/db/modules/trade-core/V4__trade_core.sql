CREATE TABLE trd_product (
 id VARCHAR(36) PRIMARY KEY, store_id VARCHAR(80) NOT NULL, merchant_id VARCHAR(80) NOT NULL,
 kind VARCHAR(20) NOT NULL, formula VARCHAR(20) NOT NULL, price_minor BIGINT NOT NULL,
 purchased_units BIGINT NOT NULL, gift_units BIGINT NOT NULL, policy_id VARCHAR(80) NOT NULL,
 policy_version VARCHAR(80) NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE trd_order (
 id VARCHAR(36) PRIMARY KEY, product_id VARCHAR(36) NOT NULL, owner_id VARCHAR(80) NOT NULL,
 store_id VARCHAR(80) NOT NULL, merchant_id VARCHAR(80) NOT NULL, kind VARCHAR(20) NOT NULL,
 formula VARCHAR(20) NOT NULL, paid_minor BIGINT NOT NULL, total_units BIGINT NOT NULL,
 remaining_units BIGINT NOT NULL DEFAULT 0, reserved_units BIGINT NOT NULL DEFAULT 0,
 state VARCHAR(24) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 policy_id VARCHAR(80) NOT NULL, policy_version VARCHAR(80) NOT NULL,
 mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATED',
 refund_id VARCHAR(36), refund_minor BIGINT NOT NULL DEFAULT 0, refund_remaining BIGINT,
 refund_state VARCHAR(32), instruction_id VARCHAR(36), refunded_minor BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT trd_balances CHECK (paid_minor >= 0 AND remaining_units >= 0 AND reserved_units >= 0 AND reserved_units <= remaining_units AND refunded_minor <= paid_minor),
 INDEX trd_owner (owner_id,id)
);
CREATE TABLE trd_receipt (
 actor_id VARCHAR(80) NOT NULL, operation VARCHAR(40) NOT NULL, idem_key VARCHAR(100) NOT NULL,
 body_hash CHAR(64) NOT NULL, result_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(actor_id,operation,idem_key)
);
CREATE TABLE trd_money (
 id VARCHAR(36) PRIMARY KEY, order_id VARCHAR(36) NOT NULL, kind VARCHAR(16) NOT NULL,
 amount_minor BIGINT NOT NULL, currency CHAR(3) NOT NULL DEFAULT 'CNY',mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATED',
 UNIQUE KEY trd_money_once(order_id,kind)
);
CREATE TABLE trd_outbox (
 event_id VARCHAR(36) PRIMARY KEY, type VARCHAR(80) NOT NULL, schema_version INT NOT NULL DEFAULT 1,
 producer VARCHAR(20) NOT NULL DEFAULT 'trade-core', aggregate_id VARCHAR(36) NOT NULL,
 aggregate_version BIGINT NOT NULL, occurred_at TIMESTAMP(6) NOT NULL,
 merchant_id VARCHAR(80),store_id VARCHAR(80),mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATED',
 payload TEXT NOT NULL
);
CREATE TABLE trd_reservation (
 business_key VARCHAR(100) NOT NULL, order_id VARCHAR(36) NOT NULL, units BIGINT NOT NULL,
 state VARCHAR(16) NOT NULL,PRIMARY KEY(order_id,business_key)
);
CREATE TABLE trd_difference (
 id VARCHAR(36) PRIMARY KEY, order_id VARCHAR(36) NOT NULL, expected_minor BIGINT NOT NULL,
 observed_minor BIGINT NOT NULL, state VARCHAR(24) NOT NULL DEFAULT 'MANUAL_REVIEW',mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATED'
);
