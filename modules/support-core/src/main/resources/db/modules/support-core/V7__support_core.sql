CREATE TABLE sup_ticket (
 id VARCHAR(36) PRIMARY KEY, owner_id VARCHAR(100) NOT NULL, merchant_id VARCHAR(100) NOT NULL,
 store_id VARCHAR(100) NOT NULL, kind VARCHAR(24) NOT NULL, state VARCHAR(24) NOT NULL,
 assignee_id VARCHAR(100), version BIGINT NOT NULL, refund_ref VARCHAR(100),
 INDEX sup_owner_page(owner_id,id)
);
CREATE TABLE sup_entry (
 sequence BIGINT AUTO_INCREMENT PRIMARY KEY, ticket_id VARCHAR(36) NOT NULL, action VARCHAR(32) NOT NULL,
 actor_id VARCHAR(100) NOT NULL, text TEXT NOT NULL, request_id VARCHAR(100) NOT NULL,
 occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(ticket_id) REFERENCES sup_ticket(id)
);
CREATE TABLE sup_receipt (
 actor_id VARCHAR(100) NOT NULL, idem_key VARCHAR(100) NOT NULL, digest CHAR(64) NOT NULL,
 result_id VARCHAR(36) NOT NULL, PRIMARY KEY(actor_id,idem_key)
);
CREATE TABLE sup_attachment (
 id VARCHAR(36) PRIMARY KEY, ticket_id VARCHAR(36) NOT NULL, media_type VARCHAR(32) NOT NULL,
 content MEDIUMBLOB NOT NULL, FOREIGN KEY(ticket_id) REFERENCES sup_ticket(id)
);
CREATE TABLE sup_outbox (
 event_id VARCHAR(36) PRIMARY KEY, type VARCHAR(64) NOT NULL, schema_version INT NOT NULL,
 producer VARCHAR(32) NOT NULL, aggregate_id VARCHAR(36) NOT NULL, aggregate_version BIGINT NOT NULL,
 merchant_id VARCHAR(100) NOT NULL, store_id VARCHAR(100) NOT NULL,
 occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), mode VARCHAR(16) NOT NULL,
 payload JSON NOT NULL
);
