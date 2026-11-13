CREATE TABLE bkg_coach_locks (coach_id VARCHAR(64) PRIMARY KEY) ENGINE=InnoDB;
CREATE TABLE bkg_sessions (
 id VARCHAR(64) PRIMARY KEY, merchant_id VARCHAR(64) NOT NULL, store_id VARCHAR(64) NOT NULL,
 coach_id VARCHAR(64) NOT NULL, kind VARCHAR(16) NOT NULL, starts_at TIMESTAMP(6) NOT NULL,
 ends_at TIMESTAMP(6) NOT NULL, capacity INT NOT NULL, reserved INT NOT NULL DEFAULT 0,
 policy_id VARCHAR(64) NOT NULL, policy_version BIGINT NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CHECK (capacity>0 AND reserved>=0 AND reserved<=capacity), CHECK (ends_at>starts_at),
 INDEX bkg_coach_time(coach_id, starts_at, ends_at)
) ENGINE=InnoDB;
CREATE TABLE bkg_bookings (
 id VARCHAR(64) PRIMARY KEY, session_id VARCHAR(64) NOT NULL, owner_id VARCHAR(64) NOT NULL,
 state VARCHAR(16) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 source_id VARCHAR(64) NOT NULL, rights_version BIGINT NOT NULL, order_id VARCHAR(64) NOT NULL,
 product_kind VARCHAR(16) NOT NULL, policy_id VARCHAR(64) NOT NULL, policy_version BIGINT NOT NULL,
 disclosed BOOLEAN NOT NULL, accepted BOOLEAN NOT NULL, cancellation_minor BIGINT NOT NULL,
 reservation_ref VARCHAR(128) NOT NULL, consumption_ref VARCHAR(128), charge_ref VARCHAR(128),
 reconciliation VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED_SIMULATED',
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (session_id) REFERENCES bkg_sessions(id),
 INDEX bkg_owner_page(owner_id,id)
) ENGINE=InnoDB;
CREATE TABLE bkg_receipts (
 actor_id VARCHAR(64) NOT NULL, operation VARCHAR(32) NOT NULL, idem_key VARCHAR(64) NOT NULL,
 body_hash CHAR(64) NOT NULL, result_id VARCHAR(64) NOT NULL,
 PRIMARY KEY(actor_id,operation,idem_key)
) ENGINE=InnoDB;
CREATE TABLE bkg_outbox (
 event_id VARCHAR(64) PRIMARY KEY, event_type VARCHAR(64) NOT NULL, schema_version INT NOT NULL DEFAULT 1,
 aggregate_id VARCHAR(64) NOT NULL, aggregate_version BIGINT NOT NULL,
 occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 merchant_id VARCHAR(64) NOT NULL, store_id VARCHAR(64) NOT NULL,
 producer VARCHAR(32) NOT NULL DEFAULT 'booking-core', mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATED',
 payload JSON NOT NULL
) ENGINE=InnoDB;
