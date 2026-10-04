-- Additive migration. No user seed and no changes to learning tables.
CREATE TABLE fitness_record (
 user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 kind VARCHAR(24) NOT NULL,
 record_key VARCHAR(36) NOT NULL,
 revision BIGINT NOT NULL,
 payload JSON NULL,
 weight_kg DECIMAL(9,3) GENERATED ALWAYS AS (CASE WHEN kind='weight' THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.kg')) AS DECIMAL(9,3)) ELSE NULL END) STORED,
 training_status VARCHAR(16) GENERATED ALWAYS AS (CASE WHEN kind='training' THEN JSON_UNQUOTE(JSON_EXTRACT(payload,'$.status')) ELSE NULL END) STORED,
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(user_id, kind, record_key),
 INDEX ix_fitness_calendar(user_id,record_key,kind),
 INDEX ix_fitness_status(user_id,training_status,record_key),
 CONSTRAINT fk_fitness_user FOREIGN KEY(user_id) REFERENCES app_user(id),
 CONSTRAINT ck_fitness_revision CHECK(revision>=0),
 CONSTRAINT ck_fitness_kind CHECK(kind IN ('goal','weight','training-plan','training','meal-plan','meals','checkin','water','training-template','meal-template','week-template','profile'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE fitness_goal_state (
 user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 current_key VARCHAR(36) NULL,
 revision BIGINT NOT NULL DEFAULT -1,
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 CHECK(revision>=-1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE fitness_goal_event (
 user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 version BIGINT NOT NULL,
 goal_key VARCHAR(36) NULL,
 action VARCHAR(12) NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(user_id,version),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 CHECK(action IN ('CREATED','ADJUSTED','ENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
