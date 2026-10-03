-- Additive: old scores, files, answer history and migration checksums are preserved.
ALTER TABLE score_record DROP CHECK ck_score_record_4;
ALTER TABLE score_record ADD CONSTRAINT ck_score_record_4 CHECK (source IN ('MANUAL','LEGACY','CODEX'));
CREATE TABLE grading_submission (
 id char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY, user_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, course_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 cycle_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, paper_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, revision bigint NOT NULL DEFAULT 0,
 created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(user_id) REFERENCES app_user(id), FOREIGN KEY(course_id) REFERENCES course(id),
 FOREIGN KEY(cycle_id) REFERENCES exam_cycle(id), FOREIGN KEY(paper_id) REFERENCES paper(id),
 INDEX ix_submission_owner(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE grading_submission_page (
 submission_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, file_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, page_no int NOT NULL,
 PRIMARY KEY(submission_id,file_id), UNIQUE KEY uq_submission_page(submission_id,page_no),
 FOREIGN KEY(submission_id) REFERENCES grading_submission(id), FOREIGN KEY(file_id) REFERENCES stored_file(id),
 CHECK(page_no BETWEEN 1 AND 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE grading_rubric (
 id char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY, paper_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, version int NOT NULL,
 state varchar(16) NOT NULL DEFAULT 'DRAFT', document json NOT NULL, created_by char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), published_at datetime(6),
 FOREIGN KEY(paper_id) REFERENCES paper(id), FOREIGN KEY(created_by) REFERENCES app_user(id),
 UNIQUE KEY uq_rubric_version(paper_id,version), CHECK(state IN ('DRAFT','PUBLISHED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE grading_worker (
 id char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY, user_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, token_hash char(64) NOT NULL UNIQUE,
 label varchar(100) NOT NULL, revoked_at datetime(6), last_seen datetime(6),
 paused boolean NOT NULL DEFAULT false, reason varchar(500),
 created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), FOREIGN KEY(user_id) REFERENCES app_user(id),
 INDEX ix_worker_user(user_id,last_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE grading_task (
 id char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY, user_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, paper_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 submission_id char(36) CHARACTER SET ascii COLLATE ascii_bin, submission_revision bigint, rubric_id char(36) CHARACTER SET ascii COLLATE ascii_bin,
 kind varchar(16) NOT NULL, state varchar(16) NOT NULL DEFAULT 'QUEUED',
 input_document json NOT NULL, result_document json, model_result_document json, model varchar(100),
 worker_id char(36) CHARACTER SET ascii COLLATE ascii_bin, lease_token char(36) CHARACTER SET ascii COLLATE ascii_bin, lease_until datetime(6), deadline datetime(6),
 attempts int NOT NULL DEFAULT 0, result_hash char(64), score_id char(36) CHARACTER SET ascii COLLATE ascii_bin, error_message varchar(500),
 created_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(user_id) REFERENCES app_user(id), FOREIGN KEY(paper_id) REFERENCES paper(id),
 FOREIGN KEY(submission_id) REFERENCES grading_submission(id), FOREIGN KEY(rubric_id) REFERENCES grading_rubric(id),
 FOREIGN KEY(worker_id) REFERENCES grading_worker(id), FOREIGN KEY(score_id) REFERENCES score_record(id),
 CHECK(kind IN ('GRADE','RUBRIC')), CHECK(state IN ('QUEUED','GRADING','REVIEW','COMPLETED','FAILED')),
 INDEX ix_task_claim(user_id,state,created_at), INDEX ix_task_submission(submission_id,submission_revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE grading_task_page (
 task_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 file_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 page_no int NOT NULL,
 PRIMARY KEY(task_id,page_no), INDEX ix_grading_pinned_file(file_id),
 FOREIGN KEY(task_id) REFERENCES grading_task(id), FOREIGN KEY(file_id) REFERENCES stored_file(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
