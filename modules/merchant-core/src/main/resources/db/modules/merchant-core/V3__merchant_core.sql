CREATE TABLE mch_mutex(id INT PRIMARY KEY);
INSERT INTO mch_mutex VALUES(1);
CREATE TABLE mch_merchant(id VARCHAR(64) PRIMARY KEY, applicant VARCHAR(64) NOT NULL, name VARCHAR(160) NOT NULL, evidence_ref VARCHAR(160) NOT NULL, review_state VARCHAR(24) NOT NULL, version BIGINT NOT NULL);
CREATE TABLE mch_store(id VARCHAR(64) PRIMARY KEY, merchant_id VARCHAR(64) NOT NULL, name VARCHAR(160) NOT NULL, deleted BOOLEAN NOT NULL DEFAULT FALSE, end_at VARCHAR(40), grace_end VARCHAR(40), version BIGINT NOT NULL, INDEX(merchant_id,id));
CREATE TABLE mch_grant(id VARCHAR(64) PRIMARY KEY, merchant_id VARCHAR(64) NOT NULL, account_id VARCHAR(64) NOT NULL, role_name VARCHAR(24) NOT NULL, revoked BOOLEAN NOT NULL, version BIGINT NOT NULL, INDEX(account_id,merchant_id));
CREATE TABLE mch_grant_store(grant_id VARCHAR(64) NOT NULL, store_id VARCHAR(64) NOT NULL, PRIMARY KEY(grant_id,store_id));
CREATE TABLE mch_receipt(actor VARCHAR(64) NOT NULL, operation_key VARCHAR(100) NOT NULL, fingerprint CHAR(64) NOT NULL, result_id VARCHAR(64) NOT NULL, result_version BIGINT NOT NULL, PRIMARY KEY(actor,operation_key));
CREATE TABLE mch_audit(id VARCHAR(64) PRIMARY KEY, actor VARCHAR(64) NOT NULL, action_name VARCHAR(64) NOT NULL, aggregate_id VARCHAR(64) NOT NULL, request_id VARCHAR(64) NOT NULL, occurred_at VARCHAR(40) NOT NULL);
CREATE TABLE mch_outbox(event_id VARCHAR(64) PRIMARY KEY, event_type VARCHAR(64) NOT NULL, schema_version INT NOT NULL, producer VARCHAR(32) NOT NULL, aggregate_id VARCHAR(64) NOT NULL, aggregate_version BIGINT NOT NULL, occurred_at VARCHAR(40) NOT NULL, merchant_id VARCHAR(64), store_id VARCHAR(64), mode VARCHAR(16) NOT NULL, payload TEXT NOT NULL);
