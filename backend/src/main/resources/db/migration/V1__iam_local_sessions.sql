CREATE TABLE iam_account (
 id CHAR(36) PRIMARY KEY, status VARCHAR(16) NOT NULL, session_epoch BIGINT NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 0, created_at BIGINT NOT NULL
) ENGINE=InnoDB;
CREATE TABLE iam_local_credential (
 account_id CHAR(36) PRIMARY KEY, login_normalized VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY(account_id) REFERENCES iam_account(id)
) ENGINE=InnoDB;
CREATE TABLE iam_session (
 id CHAR(36) PRIMARY KEY, account_id CHAR(36) NOT NULL, account_epoch BIGINT NOT NULL,
 access_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 access_expires_at BIGINT NOT NULL, absolute_expires_at BIGINT NOT NULL,
 revoked_at BIGINT NULL, version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(account_id) REFERENCES iam_account(id), INDEX(account_id)
) ENGINE=InnoDB;
CREATE TABLE iam_refresh_family (
 id CHAR(36) PRIMARY KEY, session_id CHAR(36) NOT NULL UNIQUE,
 absolute_expires_at BIGINT NOT NULL, revoked_at BIGINT NULL,
 FOREIGN KEY(session_id) REFERENCES iam_session(id)
) ENGINE=InnoDB;
CREATE TABLE iam_refresh_node (
 digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 family_id CHAR(36) NOT NULL, generation BIGINT NOT NULL,
 consumed_at BIGINT NULL, consumed_key CHAR(64) NULL, request_fingerprint CHAR(64) NULL,
 FOREIGN KEY(family_id) REFERENCES iam_refresh_family(id), UNIQUE(family_id,generation)
) ENGINE=InnoDB;
CREATE TABLE iam_login_receipt (
 account_id CHAR(36) NOT NULL, key_digest CHAR(64) NOT NULL,
 request_fingerprint CHAR(64) NOT NULL, session_id CHAR(36) NOT NULL,
 created_at BIGINT NOT NULL, PRIMARY KEY(account_id,key_digest),
 FOREIGN KEY(account_id) REFERENCES iam_account(id), FOREIGN KEY(session_id) REFERENCES iam_session(id)
) ENGINE=InnoDB;
CREATE TABLE iam_auth_failure (
 bucket_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 window_start BIGINT NOT NULL, failure_count INT NOT NULL
) ENGINE=InnoDB;
