-- Additive migration. Never rewrite existing score/pass/answer/content rows.
CREATE TABLE file_cleanup_task (
 file_id char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 attempts int NOT NULL DEFAULT 0,
 next_attempt_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 lease_token char(36) CHARACTER SET ascii COLLATE ascii_bin,
 lease_until datetime(6),
 last_error varchar(100),
 completed_at datetime(6),
 FOREIGN KEY (file_id) REFERENCES stored_file(id),
 CHECK (attempts >= 0),
 INDEX cleanup_ready(next_attempt_at,lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
INSERT INTO file_cleanup_task(file_id) SELECT id FROM stored_file WHERE state='DELETE_PENDING';

CREATE TABLE file_reconciliation (
 file_id char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 category varchar(100) NOT NULL,
 observed_at datetime(6) NOT NULL,
 resolved_at datetime(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
-- No FK: crash orphans deliberately have no stored_file record.

CREATE TABLE score_record_revision (
 score_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 revision bigint NOT NULL,
 user_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 cycle_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 course_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 paper_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 practiced_on date NOT NULL,
 score numeric(5,2) NOT NULL,
 minutes int NOT NULL,
 limit_minutes int NOT NULL,
 complete boolean NOT NULL,
 closed_book boolean NOT NULL,
 answers_seen_before boolean,
 source varchar(20) NOT NULL,
 note longtext NOT NULL,
 created_at datetime(6) NOT NULL,
 updated_at datetime(6) NOT NULL,
 actor_id char(36) CHARACTER SET ascii COLLATE ascii_bin,
 capture_kind varchar(20) NOT NULL,
 captured_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(score_id,revision),
 FOREIGN KEY(score_id) REFERENCES score_record(id),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(actor_id) REFERENCES app_user(id),
 CHECK (capture_kind IN ('MIGRATION_BASELINE','CREATED','REVISED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
INSERT INTO score_record_revision(score_id,revision,user_id,cycle_id,course_id,paper_id,practiced_on,score,minutes,limit_minutes,complete,closed_book,answers_seen_before,source,note,created_at,updated_at,capture_kind)
SELECT id,revision,user_id,cycle_id,course_id,paper_id,practiced_on,score,minutes,limit_minutes,complete,closed_book,answers_seen_before,source,note,created_at,updated_at,'MIGRATION_BASELINE' FROM score_record;
-- Previous overwritten revisions cannot be reconstructed; baseline means observed at migration.

CREATE TABLE request_replay (
 user_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 operation varchar(60) NOT NULL,
 request_key char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 request_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 response_json json NOT NULL,
 created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(user_id,operation,request_key),
 FOREIGN KEY(user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

ALTER TABLE content_release ADD COLUMN draft_revision bigint NOT NULL DEFAULT 0;

CREATE TABLE background_failure (
 task_kind varchar(40) NOT NULL,
 target_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 attempts int NOT NULL DEFAULT 1,
 last_error varchar(100) NOT NULL,
 next_attempt_at datetime(6) NOT NULL,
 updated_at datetime(6) NOT NULL,
 PRIMARY KEY(task_kind,target_id),
 INDEX background_retry(next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
