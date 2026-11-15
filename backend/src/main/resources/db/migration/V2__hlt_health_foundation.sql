CREATE TABLE hlt_record (
 id CHAR(36) PRIMARY KEY, account_id CHAR(36) NOT NULL, kind VARCHAR(16) NOT NULL,
 occurred_at BIGINT NOT NULL, payload JSON NOT NULL, version BIGINT NOT NULL,
 created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 weight_kg DECIMAL(10,3) GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.weightKg')) AS DECIMAL(10,3))) STORED,
 height_cm DECIMAL(10,3) GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.heightCm')) AS DECIMAL(10,3))) STORED,
 body_fat_percent DECIMAL(10,3) GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.bodyFatPercent')) AS DECIMAL(10,3))) STORED,
 waist_cm DECIMAL(10,3) GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.waistCm')) AS DECIMAL(10,3))) STORED,
 hip_cm DECIMAL(10,3) GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.hipCm')) AS DECIMAL(10,3))) STORED,
 chest_cm DECIMAL(10,3) GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.chestCm')) AS DECIMAL(10,3))) STORED,
 CONSTRAINT fk_hlt_record_account FOREIGN KEY (account_id) REFERENCES iam_account(id),
 INDEX ix_hlt_owner_time(account_id,kind,occurred_at,id)
);
CREATE TABLE hlt_receipt (
 account_id CHAR(36) NOT NULL, operation VARCHAR(32) NOT NULL, key_digest CHAR(64) NOT NULL,
 request_fingerprint CHAR(64) NOT NULL, record_id CHAR(36) NOT NULL,
 PRIMARY KEY(account_id,operation,key_digest),
 CONSTRAINT fk_hlt_receipt_account FOREIGN KEY(account_id) REFERENCES iam_account(id)
);
CREATE TABLE hlt_weight_goal (
 account_id CHAR(36) PRIMARY KEY, target_weight_kg DECIMAL(10,3) NOT NULL,
 target_date DATE NULL, version BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 CONSTRAINT fk_hlt_goal_account FOREIGN KEY(account_id) REFERENCES iam_account(id)
);
