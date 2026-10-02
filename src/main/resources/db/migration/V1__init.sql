-- 学习知途：Flyway V1，表结构原样来自已确认阶段2 MySQL DDL。
-- 本工程使用MySQL 8.4/InnoDB。
-- UUID用ASCII char(36)，事件datetime(6)按UTC写入/读取，日期按Asia/Shanghai解释。
-- DDL会隐式提交，仅在独立空评审库执行；不含账号或业务种子。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET time_zone = '+00:00';

CREATE TABLE `app_user` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `username` varchar(100) NOT NULL UNIQUE,
 `email` varchar(254) NOT NULL UNIQUE,
 `password_hash` varchar(200) NOT NULL,
 `role` varchar(10) NOT NULL,
 `enabled` boolean NOT NULL DEFAULT true,
 `token_version` integer NOT NULL DEFAULT 0,
 `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT ck_app_user_1 CHECK (`role` IN ('USER','ADMIN')),
 CONSTRAINT ck_app_user_2 CHECK (`token_version` >= 0),
 CONSTRAINT ck_app_user_bool_enabled CHECK (`enabled` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
-- 同一登录输入不能同时成为 A 用户名和 B 邮箱。规范化规则见待定技术项。
CREATE TABLE `login_identifier` (
 `identifier` varchar(254) PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `kind` varchar(10) NOT NULL,
 CONSTRAINT fk_login_identifier_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT ck_login_identifier_1 CHECK (`kind` IN ('USERNAME','EMAIL','BOTH')),
 UNIQUE (`user_id`, `kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `course` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `code` varchar(20) NOT NULL UNIQUE,
 `legacy_id` varchar(100) UNIQUE,
 `name` varchar(200) NOT NULL,
 `course_type` varchar(10) NOT NULL,
 `active` boolean NOT NULL DEFAULT true,
 CONSTRAINT ck_course_1 CHECK (`course_type` IN ('THEORY','PRACTICE')),
 CONSTRAINT ck_course_bool_active CHECK (`active` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `exam_cycle` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `name` varchar(200) NOT NULL,
 `start_date` date NOT NULL,
 `end_date` date NOT NULL,
 `timezone` varchar(50) NOT NULL DEFAULT 'Asia/Shanghai',
 CONSTRAINT ck_exam_cycle_1 CHECK (`timezone` = 'Asia/Shanghai'),
 CONSTRAINT ck_exam_cycle_2 CHECK (`end_date` >= `start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `cycle_course` (
 `cycle_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `exam_date` date,
 `starts_at` time,
 `ends_at` time,
 CONSTRAINT fk_cycle_course_1 FOREIGN KEY (`cycle_id`) REFERENCES `exam_cycle`(`id`),
 CONSTRAINT fk_cycle_course_2 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 PRIMARY KEY (`cycle_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `enrollment` (
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `cycle_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `paid` boolean NOT NULL DEFAULT false,
 `fee` numeric(10,2),
 `official_score` numeric(5,2),
 `official_passed` boolean,
 `passed_month` date,
 `note` longtext NOT NULL DEFAULT (''),
 `revision` bigint NOT NULL DEFAULT 0,
 CONSTRAINT fk_enrollment_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT ck_enrollment_1 CHECK (`fee` >= 0),
 CONSTRAINT ck_enrollment_2 CHECK (`official_score` BETWEEN 0 AND 100),
 CONSTRAINT ck_enrollment_3 CHECK (DAYOFMONTH(`passed_month`) = 1),
 PRIMARY KEY (`user_id`, `cycle_id`, `course_id`),
 FOREIGN KEY (`cycle_id`,`course_id`) REFERENCES `cycle_course`(`cycle_id`,`course_id`),
 CONSTRAINT ck_enrollment_bool_paid CHECK (`paid` IN (0,1)),
 CONSTRAINT ck_enrollment_bool_official_passed CHECK (`official_passed` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `content_release` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `version_no` integer NOT NULL,
 `state` varchar(12) NOT NULL,
 `published_at` datetime(6),
 `source_sha` varchar(64),
 CONSTRAINT fk_content_release_1 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT ck_content_release_1 CHECK (`version_no` > 0),
 CONSTRAINT ck_content_release_2 CHECK (`state` IN ('DRAFT','PUBLISHED','RETIRED')),
 UNIQUE (`course_id`,`version_no`),
 UNIQUE (`id`,`course_id`),
 CONSTRAINT ck_content_release_3 CHECK (`state` = 'DRAFT' OR `published_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `chapter` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `stable_key` varchar(150) NOT NULL,
 CONSTRAINT fk_chapter_1 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 UNIQUE (`course_id`,`stable_key`),
 UNIQUE (`id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `chapter_revision` (
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `title` varchar(250) NOT NULL,
 `sort_order` integer NOT NULL,
 `participates_in_assessment` boolean NOT NULL,
 `gate_override` integer,
 CONSTRAINT ck_chapter_revision_1 CHECK (`sort_order` >= 0),
 CONSTRAINT ck_chapter_revision_2 CHECK (`gate_override` BETWEEN 20 AND 100),
 PRIMARY KEY (`release_id`,`chapter_id`),
 FOREIGN KEY (`release_id`,`course_id`) REFERENCES `content_release`(`id`,`course_id`),
 FOREIGN KEY (`chapter_id`,`course_id`) REFERENCES `chapter`(`id`,`course_id`),
 CONSTRAINT ck_chapter_revision_bool_participates_in_assessment CHECK (`participates_in_assessment` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `knowledge_point` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `stable_key` varchar(150) NOT NULL,
 `title` varchar(250) NOT NULL,
 CONSTRAINT fk_knowledge_point_1 FOREIGN KEY (`chapter_id`) REFERENCES `chapter`(`id`),
 UNIQUE (`chapter_id`,`stable_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `release_point` (
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `point_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `title_snapshot` varchar(250) NOT NULL,
 CONSTRAINT fk_release_point_1 FOREIGN KEY (`point_id`) REFERENCES `knowledge_point`(`id`),
 PRIMARY KEY (`release_id`,`point_id`),
 FOREIGN KEY (`release_id`,`chapter_id`) REFERENCES `chapter_revision`(`release_id`,`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `study_item` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `stable_key` varchar(200) NOT NULL,
 CONSTRAINT fk_study_item_1 FOREIGN KEY (`chapter_id`) REFERENCES `chapter`(`id`),
 UNIQUE (`chapter_id`,`stable_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `item_revision` (
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `item_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `title` varchar(300) NOT NULL,
 `resource_locator` longtext,
 `estimated_minutes` integer NOT NULL,
 `sort_order` integer NOT NULL,
 CONSTRAINT fk_item_revision_1 FOREIGN KEY (`item_id`) REFERENCES `study_item`(`id`),
 CONSTRAINT ck_item_revision_1 CHECK (`estimated_minutes` > 0),
 CONSTRAINT ck_item_revision_2 CHECK (`sort_order` >= 0),
 PRIMARY KEY (`release_id`,`item_id`),
 FOREIGN KEY (`release_id`,`chapter_id`) REFERENCES `chapter_revision`(`release_id`,`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `user_item_progress` (
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `item_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `completed` boolean NOT NULL DEFAULT false,
 `completed_at` datetime(6),
 `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `revision` bigint NOT NULL DEFAULT 0,
 CONSTRAINT fk_user_item_progress_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_user_item_progress_2 FOREIGN KEY (`item_id`) REFERENCES `study_item`(`id`),
 PRIMARY KEY (`user_id`,`item_id`),
 CONSTRAINT ck_user_item_progress_bool_completed CHECK (`completed` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `knowledge_module` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `stable_key` varchar(150) NOT NULL,
 CONSTRAINT fk_knowledge_module_1 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 UNIQUE (`course_id`,`stable_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `module_revision` (
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `module_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `title` varchar(250) NOT NULL,
 `content` longtext NOT NULL,
 `difficulty` smallint NOT NULL,
 `formulas` json NOT NULL DEFAULT (JSON_ARRAY()),
 `resources` json NOT NULL DEFAULT (JSON_ARRAY()),
 CONSTRAINT fk_module_revision_1 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT fk_module_revision_2 FOREIGN KEY (`module_id`) REFERENCES `knowledge_module`(`id`),
 CONSTRAINT ck_module_revision_1 CHECK (`difficulty` BETWEEN 1 AND 5),
 CONSTRAINT ck_module_revision_2 CHECK (JSON_TYPE(`formulas`) = 'ARRAY'),
 CONSTRAINT ck_module_revision_3 CHECK (JSON_TYPE(`resources`) = 'ARRAY'),
 PRIMARY KEY (`release_id`,`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `knowledge_example` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `module_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `stars` smallint NOT NULL,
 `question` longtext NOT NULL,
 `sort_order` integer NOT NULL,
 CONSTRAINT ck_knowledge_example_1 CHECK (`stars` BETWEEN 1 AND 5),
 CONSTRAINT ck_knowledge_example_2 CHECK (`sort_order` >= 0),
 FOREIGN KEY (`release_id`,`module_id`) REFERENCES `module_revision`(`release_id`,`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `example_solution` (
 `example_id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `answer` longtext NOT NULL,
 `solution` longtext NOT NULL,
 CONSTRAINT fk_example_solution_1 FOREIGN KEY (`example_id`) REFERENCES `knowledge_example`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `user_knowledge_note` (
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `module_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `mastery` smallint NOT NULL DEFAULT 0,
 `note` longtext NOT NULL DEFAULT (''),
 `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `revision` bigint NOT NULL DEFAULT 0,
 CONSTRAINT fk_user_knowledge_note_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_user_knowledge_note_2 FOREIGN KEY (`module_id`) REFERENCES `knowledge_module`(`id`),
 CONSTRAINT ck_user_knowledge_note_1 CHECK (`mastery` BETWEEN 0 AND 4),
 CONSTRAINT ck_user_knowledge_note_2 CHECK (char_length(`note`) <= 10000),
 PRIMARY KEY (`user_id`,`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `question` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `original_key` varchar(200) NOT NULL UNIQUE,
 `content_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 `legacy_id` varchar(250) UNIQUE,
 `mode` varchar(10) NOT NULL,
 `eligible_original` boolean NOT NULL,
 CONSTRAINT fk_question_1 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT ck_question_1 CHECK (`mode` IN ('CHAPTER','VARIANT')),
 FOREIGN KEY (`chapter_id`,`course_id`) REFERENCES `chapter`(`id`,`course_id`),
 CONSTRAINT ck_question_bool_eligible_original CHECK (`eligible_original` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `question_revision` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `question_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `stem` longtext NOT NULL,
 `options` json NOT NULL,
 `difficulty` smallint NOT NULL,
 `source_locator` longtext,
 `sort_order` integer NOT NULL,
 CONSTRAINT fk_question_revision_1 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT fk_question_revision_2 FOREIGN KEY (`question_id`) REFERENCES `question`(`id`),
 CONSTRAINT ck_question_revision_1 CHECK (JSON_TYPE(`options`) = 'ARRAY' AND JSON_LENGTH(`options`) >= 2),
 CONSTRAINT ck_question_revision_2 CHECK (`difficulty` BETWEEN 1 AND 5),
 CONSTRAINT ck_question_revision_3 CHECK (`sort_order` >= 0),
 UNIQUE (`release_id`,`question_id`),
 UNIQUE (`id`,`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `question_solution` (
 `revision_id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `correct_option` smallint NOT NULL,
 `explanation` longtext NOT NULL,
 CONSTRAINT fk_question_solution_1 FOREIGN KEY (`revision_id`) REFERENCES `question_revision`(`id`),
 CONSTRAINT ck_question_solution_1 CHECK (`correct_option` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `question_point` (
 `revision_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `point_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 CONSTRAINT fk_question_point_1 FOREIGN KEY (`revision_id`) REFERENCES `question_revision`(`id`),
 CONSTRAINT fk_question_point_2 FOREIGN KEY (`point_id`) REFERENCES `knowledge_point`(`id`),
 PRIMARY KEY (`revision_id`,`point_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `practice_submission` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `question_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `revision_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `selected_option` smallint NOT NULL,
 `correct` boolean NOT NULL,
 `submitted_at` datetime(6) NOT NULL,
 `idempotency_key` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 CONSTRAINT fk_practice_submission_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT ck_practice_submission_1 CHECK (`selected_option` >= 0),
 UNIQUE (`user_id`,`idempotency_key`),
 FOREIGN KEY (`revision_id`,`question_id`) REFERENCES `question_revision`(`id`,`question_id`),
 CONSTRAINT ck_practice_submission_bool_correct CHECK (`correct` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX practice_user_question_time ON `practice_submission`(`user_id`,`question_id`,`submitted_at`,`id`);
CREATE TABLE `user_question_mark` (
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `question_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `bookmarked` boolean NOT NULL DEFAULT false,
 `uncertain` boolean NOT NULL DEFAULT false,
 `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `revision` bigint NOT NULL DEFAULT 0,
 CONSTRAINT fk_user_question_mark_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_user_question_mark_2 FOREIGN KEY (`question_id`) REFERENCES `question`(`id`),
 PRIMARY KEY (`user_id`,`question_id`),
 CONSTRAINT ck_user_question_mark_bool_bookmarked CHECK (`bookmarked` IN (0,1)),
 CONSTRAINT ck_user_question_mark_bool_uncertain CHECK (`uncertain` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `recent_learning_position` (
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `item_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `question_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `pane` varchar(20) NOT NULL,
 `updated_at` datetime(6) NOT NULL,
 CONSTRAINT fk_recent_learning_position_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_recent_learning_position_2 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT fk_recent_learning_position_3 FOREIGN KEY (`chapter_id`) REFERENCES `chapter`(`id`),
 CONSTRAINT fk_recent_learning_position_4 FOREIGN KEY (`item_id`) REFERENCES `study_item`(`id`),
 CONSTRAINT fk_recent_learning_position_5 FOREIGN KEY (`question_id`) REFERENCES `question`(`id`),
 CONSTRAINT ck_recent_learning_position_1 CHECK (`pane` IN ('CATALOG','KNOWLEDGE','PRACTICE','EXAMS','NOTES','MANUAL')),
 PRIMARY KEY (`user_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `study_note` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `note_date` date NOT NULL,
 `content` longtext NOT NULL,
 `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `revision` bigint NOT NULL DEFAULT 0,
 CONSTRAINT fk_study_note_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_study_note_2 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT ck_study_note_1 CHECK (char_length(`content`) BETWEEN 1 AND 5000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX study_note_user_course_date ON `study_note`(`user_id`,`course_id`,`note_date` DESC);
CREATE TABLE `assessment_policy` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `gate_ratio` numeric(5,4) NOT NULL DEFAULT 0.6,
 `gate_floor` integer NOT NULL DEFAULT 20,
 `gate_cap` integer NOT NULL DEFAULT 100,
 `chapter_min_questions` integer NOT NULL DEFAULT 20,
 `chapter_max_questions` integer NOT NULL DEFAULT 40,
 `chapter_limit_minutes` integer NOT NULL DEFAULT 40,
 `chapter_pass_score` numeric(5,2) NOT NULL DEFAULT 90,
 `mock_question_count` integer NOT NULL DEFAULT 40,
 `mock_limit_minutes` integer NOT NULL DEFAULT 80,
 `mock_pass_score` numeric(5,2) NOT NULL DEFAULT 80,
 `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_assessment_policy_1 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT fk_assessment_policy_2 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT ck_assessment_policy_1 CHECK (`gate_ratio` BETWEEN 0 AND 1),
 CONSTRAINT ck_assessment_policy_2 CHECK (`gate_floor` > 0),
 CONSTRAINT ck_assessment_policy_3 CHECK (`gate_cap` >= `gate_floor`),
 CONSTRAINT ck_assessment_policy_4 CHECK (`chapter_min_questions` > 0),
 CONSTRAINT ck_assessment_policy_5 CHECK (`chapter_max_questions` >= `chapter_min_questions`),
 CONSTRAINT ck_assessment_policy_6 CHECK (`chapter_limit_minutes` > 0),
 CONSTRAINT ck_assessment_policy_7 CHECK (`chapter_pass_score` BETWEEN 0 AND 100),
 CONSTRAINT ck_assessment_policy_8 CHECK (`mock_question_count` > 0),
 CONSTRAINT ck_assessment_policy_9 CHECK (`mock_limit_minutes` > 0),
 CONSTRAINT ck_assessment_policy_10 CHECK (`mock_pass_score` BETWEEN 0 AND 100),
 UNIQUE (`release_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `mock_chapter_weight` (
 `policy_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `score_share` numeric(9,6) NOT NULL,
 `sample_from` date NOT NULL,
 `sample_to` date NOT NULL,
 `evidence` json NOT NULL,
 `approved_by` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `approved_at` datetime(6) NOT NULL,
 CONSTRAINT fk_mock_chapter_weight_1 FOREIGN KEY (`policy_id`) REFERENCES `assessment_policy`(`id`),
 CONSTRAINT fk_mock_chapter_weight_2 FOREIGN KEY (`chapter_id`) REFERENCES `chapter`(`id`),
 CONSTRAINT ck_mock_chapter_weight_1 CHECK (`score_share` BETWEEN 0 AND 1),
 CONSTRAINT ck_mock_chapter_weight_2 CHECK (`sample_to` >= `sample_from`),
 CONSTRAINT fk_mock_chapter_weight_3 FOREIGN KEY (`approved_by`) REFERENCES `app_user`(`id`),
 PRIMARY KEY (`policy_id`,`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `assessment_session` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `policy_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `kind` varchar(10) NOT NULL,
 `status` varchar(12) NOT NULL,
 `started_at` datetime(6) NOT NULL,
 `deadline_at` datetime(6) NOT NULL,
 `submitted_at` datetime(6),
 `question_count` integer NOT NULL,
 `correct_count` integer,
 `pass_score` numeric(5,2) NOT NULL,
 `passed` boolean,
 `policy_snapshot` json NOT NULL,
 `idempotency_key` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 CONSTRAINT fk_assessment_session_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_assessment_session_2 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT fk_assessment_session_3 FOREIGN KEY (`policy_id`) REFERENCES `assessment_policy`(`id`),
 CONSTRAINT ck_assessment_session_1 CHECK (`kind` IN ('CHAPTER','MOCK')),
 CONSTRAINT ck_assessment_session_2 CHECK (`status` IN ('IN_PROGRESS','SUBMITTED','TIMED_OUT')),
 CONSTRAINT ck_assessment_session_3 CHECK (`deadline_at` > `started_at`),
 CONSTRAINT ck_assessment_session_4 CHECK (`question_count` > 0),
 CONSTRAINT ck_assessment_session_5 CHECK (`correct_count` BETWEEN 0 AND `question_count`),
 CONSTRAINT ck_assessment_session_6 CHECK (`pass_score` BETWEEN 0 AND 100),
 UNIQUE (`user_id`,`idempotency_key`),
 UNIQUE (`id`,`user_id`),
 FOREIGN KEY (`release_id`,`course_id`) REFERENCES `content_release`(`id`,`course_id`),
 FOREIGN KEY (`chapter_id`,`course_id`) REFERENCES `chapter`(`id`,`course_id`),
 CONSTRAINT ck_assessment_session_7 CHECK ((`kind`='CHAPTER') = (`chapter_id` IS NOT NULL)),
 CONSTRAINT ck_assessment_session_8 CHECK ((`status`='IN_PROGRESS' AND `submitted_at` IS NULL AND `correct_count` IS NULL AND `passed` IS NULL)
     OR (`status`<>'IN_PROGRESS' AND `submitted_at` IS NOT NULL AND `correct_count` IS NOT NULL AND `passed` IS NOT NULL)),
 CONSTRAINT ck_assessment_session_bool_passed CHECK (`passed` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX assessment_owner_time ON `assessment_session`(`user_id`,`course_id`,`started_at` DESC);
CREATE TABLE `assessment_session_question` (
 `session_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `revision_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `position` integer NOT NULL,
 `selected_option` smallint,
 `answer_saved_at` datetime(6),
 CONSTRAINT fk_assessment_session_question_1 FOREIGN KEY (`session_id`) REFERENCES `assessment_session`(`id`),
 CONSTRAINT fk_assessment_session_question_2 FOREIGN KEY (`revision_id`) REFERENCES `question_revision`(`id`),
 CONSTRAINT ck_assessment_session_question_1 CHECK (`position` > 0),
 CONSTRAINT ck_assessment_session_question_2 CHECK (`selected_option` >= 0),
 PRIMARY KEY (`session_id`,`revision_id`),
 UNIQUE (`session_id`,`position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `assessment_answer_event` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `session_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `revision_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `selected_option` smallint NOT NULL,
 `saved_at` datetime(6) NOT NULL,
 CONSTRAINT ck_assessment_answer_event_1 CHECK (`selected_option` >= 0),
 FOREIGN KEY (`session_id`,`revision_id`) REFERENCES `assessment_session_question`(`session_id`,`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `assessment_pass` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `session_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `kind` varchar(10) NOT NULL,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `source` varchar(20) NOT NULL,
 `legacy_review_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `granted_at` datetime(6) NOT NULL,
 `invalidated_at` datetime(6),
 `invalidated_by` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `invalidation_reason` longtext,
 CONSTRAINT fk_assessment_pass_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_assessment_pass_2 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT ck_assessment_pass_1 CHECK (`kind` IN ('CHAPTER','MOCK')),
 CONSTRAINT fk_assessment_pass_3 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT ck_assessment_pass_2 CHECK (`source` IN ('ASSESSMENT','LEGACY_CONFIRMED')),
 CONSTRAINT fk_assessment_pass_4 FOREIGN KEY (`invalidated_by`) REFERENCES `app_user`(`id`),
 UNIQUE (`session_id`),
 FOREIGN KEY (`session_id`,`user_id`) REFERENCES `assessment_session`(`id`,`user_id`),
 FOREIGN KEY (`chapter_id`,`course_id`) REFERENCES `chapter`(`id`,`course_id`),
 CONSTRAINT ck_assessment_pass_3 CHECK ((`kind`='CHAPTER') = (`chapter_id` IS NOT NULL)),
 CONSTRAINT ck_assessment_pass_4 CHECK ((`source`='ASSESSMENT' AND `session_id` IS NOT NULL AND `legacy_review_id` IS NULL)
     OR (`source`='LEGACY_CONFIRMED' AND `session_id` IS NULL AND `legacy_review_id` IS NOT NULL)),
 CONSTRAINT ck_assessment_pass_5 CHECK ((`invalidated_at` IS NULL AND `invalidated_by` IS NULL AND `invalidation_reason` IS NULL)
     OR (`invalidated_at` IS NOT NULL AND `invalidated_by` IS NOT NULL AND `invalidation_reason` IS NOT NULL AND char_length(trim(`invalidation_reason`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX valid_chapter_pass ON `assessment_pass`(`user_id`,`chapter_id`,`kind`,`invalidated_at`);
CREATE INDEX valid_mock_pass ON `assessment_pass`(`user_id`,`course_id`,`kind`,`invalidated_at`);
CREATE TABLE `unlock_override` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `cycle_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `confirmed_at` datetime(6) NOT NULL,
 `exam_date_snapshot` date NOT NULL,
 `revoked_at` datetime(6),
 `active_slot` smallint GENERATED ALWAYS AS (CASE WHEN `revoked_at` IS NULL THEN 1 ELSE NULL END) STORED,
 `revision` bigint NOT NULL DEFAULT 0,
 UNIQUE (`user_id`,`cycle_id`,`course_id`,`active_slot`),
 FOREIGN KEY (`user_id`,`cycle_id`,`course_id`) REFERENCES `enrollment`(`user_id`,`cycle_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `bank_alert` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `alert_code` varchar(50) NOT NULL,
 `details` json NOT NULL,
 `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `acknowledged_at` datetime(6),
 `acknowledged_by` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 CONSTRAINT fk_bank_alert_1 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT fk_bank_alert_2 FOREIGN KEY (`chapter_id`) REFERENCES `chapter`(`id`),
 CONSTRAINT fk_bank_alert_3 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT fk_bank_alert_4 FOREIGN KEY (`acknowledged_by`) REFERENCES `app_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `stored_file` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `owner_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `purpose` varchar(25) NOT NULL,
 `storage_key` varchar(512) NOT NULL UNIQUE,
 `original_name` varchar(300) NOT NULL,
 `mime_type` varchar(100) NOT NULL,
 `size_bytes` bigint NOT NULL,
 `sha256` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `contains_answers` boolean NOT NULL DEFAULT false,
 `state` varchar(15) NOT NULL,
 `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 `deleted_at` datetime(6),
 CONSTRAINT fk_stored_file_1 FOREIGN KEY (`owner_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT ck_stored_file_1 CHECK (`purpose` IN ('PAPER','MANUAL','SCORE_IMAGE','LEGACY_ATTACHMENT')),
 CONSTRAINT ck_stored_file_2 CHECK (`size_bytes` > 0),
 CONSTRAINT ck_stored_file_3 CHECK (`state` IN ('ACTIVE','DELETE_PENDING','DELETED')),
 CONSTRAINT ck_stored_file_4 CHECK (`purpose` NOT IN ('SCORE_IMAGE','LEGACY_ATTACHMENT') OR `owner_id` IS NOT NULL),
 CONSTRAINT ck_stored_file_5 CHECK (`purpose` <> 'SCORE_IMAGE' OR (`mime_type` IN ('image/jpeg','image/png','image/webp') AND `size_bytes` <= 8388608)),
 CONSTRAINT ck_stored_file_6 CHECK ((`state`='DELETED') = (`deleted_at` IS NOT NULL)),
 CONSTRAINT ck_stored_file_bool_contains_answers CHECK (`contains_answers` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `paper` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `paper_month` date NOT NULL,
 `source_course_code` varchar(20),
 `question_file_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `answer_file_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `question_pages` integer,
 `answer_pages` integer,
 `note` longtext NOT NULL DEFAULT (''),
 CONSTRAINT fk_paper_1 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT ck_paper_1 CHECK (DAYOFMONTH(`paper_month`) = 1),
 CONSTRAINT fk_paper_2 FOREIGN KEY (`question_file_id`) REFERENCES `stored_file`(`id`),
 CONSTRAINT fk_paper_3 FOREIGN KEY (`answer_file_id`) REFERENCES `stored_file`(`id`),
 CONSTRAINT ck_paper_2 CHECK (`question_pages` > 0),
 CONSTRAINT ck_paper_3 CHECK (`answer_pages` > 0),
 UNIQUE (`course_id`,`paper_month`),
 UNIQUE (`id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `score_record` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `cycle_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `paper_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `practiced_on` date NOT NULL,
 `score` numeric(5,2) NOT NULL,
 `minutes` integer NOT NULL,
 `limit_minutes` integer NOT NULL,
 `complete` boolean NOT NULL,
 `closed_book` boolean NOT NULL,
 `answers_seen_before` boolean,
 `source` varchar(20) NOT NULL,
 `note` longtext NOT NULL DEFAULT (''),
 `created_at` datetime(6) NOT NULL,
 `updated_at` datetime(6) NOT NULL,
 `revision` bigint NOT NULL DEFAULT 0,
 CONSTRAINT ck_score_record_1 CHECK (`score` BETWEEN 0 AND 100),
 CONSTRAINT ck_score_record_2 CHECK (`minutes` BETWEEN 1 AND 1440),
 CONSTRAINT ck_score_record_3 CHECK (`limit_minutes` BETWEEN 1 AND 1440),
 CONSTRAINT ck_score_record_4 CHECK (`source` IN ('MANUAL','LEGACY')),
 CONSTRAINT ck_score_record_5 CHECK (char_length(`note`) <= 5000),
 FOREIGN KEY (`user_id`,`cycle_id`,`course_id`) REFERENCES `enrollment`(`user_id`,`cycle_id`,`course_id`),
 FOREIGN KEY (`paper_id`,`course_id`) REFERENCES `paper`(`id`,`course_id`),
 CONSTRAINT ck_score_record_6 CHECK (`source`='LEGACY' OR `answers_seen_before` IS NOT NULL),
 CONSTRAINT ck_score_record_bool_complete CHECK (`complete` IN (0,1)),
 CONSTRAINT ck_score_record_bool_closed_book CHECK (`closed_book` IN (0,1)),
 CONSTRAINT ck_score_record_bool_answers_seen_before CHECK (`answers_seen_before` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX prediction_first_valid ON `score_record`(`user_id`,`course_id`,`paper_id`,`practiced_on`,`created_at`,`id`);
CREATE TABLE `score_record_image` (
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `score_record_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `file_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 `sort_order` integer NOT NULL,
 CONSTRAINT fk_score_record_image_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_score_record_image_2 FOREIGN KEY (`score_record_id`) REFERENCES `score_record`(`id`),
 CONSTRAINT fk_score_record_image_3 FOREIGN KEY (`file_id`) REFERENCES `stored_file`(`id`),
 CONSTRAINT ck_score_record_image_1 CHECK (`sort_order` >= 0),
 PRIMARY KEY (`score_record_id`,`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `learning_plan` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `cycle_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `start_date` date NOT NULL,
 `end_date` date NOT NULL,
 `config_snapshot` json NOT NULL,
 `created_at` datetime(6) NOT NULL,
 CONSTRAINT fk_learning_plan_1 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_learning_plan_2 FOREIGN KEY (`cycle_id`) REFERENCES `exam_cycle`(`id`),
 CONSTRAINT ck_learning_plan_1 CHECK (`end_date` >= `start_date`),
 UNIQUE (`id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `plan_revision` (
 `plan_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `revision_no` integer NOT NULL,
 `snapshot` json NOT NULL,
 `confirmed_at` datetime(6) NOT NULL,
 CONSTRAINT fk_plan_revision_1 FOREIGN KEY (`plan_id`) REFERENCES `learning_plan`(`id`),
 CONSTRAINT ck_plan_revision_1 CHECK (`revision_no` > 0),
 PRIMARY KEY (`plan_id`,`revision_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE `plan_current_revision` (
 `plan_id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `revision_no` integer NOT NULL,
 CONSTRAINT fk_plan_current_revision_1 FOREIGN KEY (`plan_id`) REFERENCES `learning_plan`(`id`),
 FOREIGN KEY (`plan_id`,`revision_no`) REFERENCES `plan_revision`(`plan_id`,`revision_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `plan_day_capacity` (
 `plan_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `revision_no` integer NOT NULL,
 `day` date NOT NULL,
 `capacity_minutes` integer NOT NULL,
 CONSTRAINT ck_plan_day_capacity_1 CHECK (`capacity_minutes` BETWEEN 0 AND 1440),
 PRIMARY KEY (`plan_id`,`revision_no`,`day`),
 FOREIGN KEY (`plan_id`,`revision_no`) REFERENCES `plan_revision`(`plan_id`,`revision_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `plan_task_template` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `kind` varchar(15) NOT NULL,
 `title` varchar(500) NOT NULL,
 `estimated_minutes` integer NOT NULL,
 `resource_locator` longtext,
 `sort_order` integer NOT NULL,
 CONSTRAINT fk_plan_task_template_1 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT fk_plan_task_template_2 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT ck_plan_task_template_1 CHECK (`kind` IN ('PAPER','REVIEW')),
 CONSTRAINT ck_plan_task_template_2 CHECK (`estimated_minutes` > 0),
 CONSTRAINT ck_plan_task_template_3 CHECK (`sort_order` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `plan_task` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `plan_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `item_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `template_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `title_snapshot` varchar(500) NOT NULL,
 `estimated_minutes` integer NOT NULL,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `kind` varchar(15) NOT NULL,
 `completed` boolean NOT NULL DEFAULT false,
 `completed_at` datetime(6),
 CONSTRAINT fk_plan_task_1 FOREIGN KEY (`plan_id`) REFERENCES `learning_plan`(`id`),
 CONSTRAINT fk_plan_task_2 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT fk_plan_task_3 FOREIGN KEY (`item_id`) REFERENCES `study_item`(`id`),
 CONSTRAINT fk_plan_task_4 FOREIGN KEY (`template_id`) REFERENCES `plan_task_template`(`id`),
 CONSTRAINT ck_plan_task_1 CHECK (`estimated_minutes` > 0),
 CONSTRAINT fk_plan_task_5 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT ck_plan_task_2 CHECK (`kind` IN ('ITEM','PAPER','REVIEW')),
 UNIQUE (`id`,`plan_id`),
 CONSTRAINT ck_plan_task_3 CHECK ((`kind`='ITEM') = (`item_id` IS NOT NULL)),
 CONSTRAINT ck_plan_task_4 CHECK ((`kind`='ITEM') = (`template_id` IS NULL)),
 CONSTRAINT ck_plan_task_5 CHECK (NOT `completed` OR `completed_at` IS NOT NULL),
 CONSTRAINT ck_plan_task_bool_completed CHECK (`completed` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `plan_task_segment` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `plan_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `revision_no` integer NOT NULL,
 `task_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `scheduled_on` date,
 `minutes` integer NOT NULL,
 `state` varchar(15) NOT NULL,
 `sort_order` integer NOT NULL,
 CONSTRAINT ck_plan_task_segment_1 CHECK (`minutes` > 0),
 CONSTRAINT ck_plan_task_segment_2 CHECK (`state` IN ('SCHEDULED','UNSCHEDULED','AWAITING_DATE')),
 CONSTRAINT ck_plan_task_segment_3 CHECK (`sort_order` >= 0),
 FOREIGN KEY (`plan_id`,`revision_no`) REFERENCES `plan_revision`(`plan_id`,`revision_no`),
 FOREIGN KEY (`task_id`,`plan_id`) REFERENCES `plan_task`(`id`,`plan_id`),
 CONSTRAINT ck_plan_task_segment_4 CHECK ((`state`='SCHEDULED') = (`scheduled_on` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `plan_preview` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `plan_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `base_revision` integer NOT NULL,
 `as_of` date NOT NULL,
 `snapshot` json NOT NULL,
 `gap_minutes` integer NOT NULL,
 `input_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `created_at` datetime(6) NOT NULL,
 `confirmed_at` datetime(6),
 CONSTRAINT ck_plan_preview_1 CHECK (`gap_minutes` >= 0),
 FOREIGN KEY (`plan_id`,`user_id`) REFERENCES `learning_plan`(`id`,`user_id`),
 FOREIGN KEY (`plan_id`,`base_revision`) REFERENCES `plan_revision`(`plan_id`,`revision_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `legacy_import_batch` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `source_sha` varchar(64) NOT NULL,
 `target_user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `manifest_sha256` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `state` varchar(15) NOT NULL,
 `created_at` datetime(6) NOT NULL,
 CONSTRAINT fk_legacy_import_batch_1 FOREIGN KEY (`target_user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT ck_legacy_import_batch_1 CHECK (`state` IN ('STAGED','VERIFIED','IMPORTED','ROLLED_BACK'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `legacy_record` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `batch_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `old_owner` varchar(191) NOT NULL,
 `old_id` varchar(512) NOT NULL,
 `old_kind` varchar(50) NOT NULL,
 `payload` json NOT NULL,
 `source_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `read_only` boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_legacy_record_1 FOREIGN KEY (`batch_id`) REFERENCES `legacy_import_batch`(`id`),
 CONSTRAINT fk_legacy_record_2 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT ck_legacy_record_1 CHECK (`read_only` = 1),
 UNIQUE (`batch_id`,`old_owner`,`old_id`),
 CONSTRAINT ck_legacy_record_bool_read_only CHECK (`read_only` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `legacy_mapping` (
 `batch_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `source_type` varchar(50) NOT NULL,
 `source_key` varchar(512) NOT NULL,
 `target_type` varchar(50) NOT NULL,
 `target_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 CONSTRAINT fk_legacy_mapping_1 FOREIGN KEY (`batch_id`) REFERENCES `legacy_import_batch`(`id`),
 PRIMARY KEY (`batch_id`,`source_type`,`source_key`,`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `legacy_file_link` (
 `legacy_record_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `file_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `old_object_key` longtext NOT NULL,
 CONSTRAINT fk_legacy_file_link_1 FOREIGN KEY (`legacy_record_id`) REFERENCES `legacy_record`(`id`),
 CONSTRAINT fk_legacy_file_link_2 FOREIGN KEY (`file_id`) REFERENCES `stored_file`(`id`),
 PRIMARY KEY (`legacy_record_id`,`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `legacy_pass_review` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `legacy_record_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `course_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `chapter_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `release_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `kind` varchar(10) NOT NULL,
 `old_score` numeric(5,2) NOT NULL,
 `old_threshold` numeric(5,2) NOT NULL,
 `decision` varchar(15) NOT NULL DEFAULT 'PENDING',
 `decided_by` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `decided_at` datetime(6),
 `reason` longtext,
 CONSTRAINT fk_legacy_pass_review_1 FOREIGN KEY (`legacy_record_id`) REFERENCES `legacy_record`(`id`),
 CONSTRAINT fk_legacy_pass_review_2 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_legacy_pass_review_3 FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
 CONSTRAINT fk_legacy_pass_review_4 FOREIGN KEY (`chapter_id`) REFERENCES `chapter`(`id`),
 CONSTRAINT fk_legacy_pass_review_5 FOREIGN KEY (`release_id`) REFERENCES `content_release`(`id`),
 CONSTRAINT ck_legacy_pass_review_1 CHECK (`kind` IN ('CHAPTER','MOCK')),
 CONSTRAINT ck_legacy_pass_review_2 CHECK (`old_score` BETWEEN 0 AND 100),
 CONSTRAINT ck_legacy_pass_review_3 CHECK (`old_threshold` BETWEEN 0 AND 100),
 CONSTRAINT ck_legacy_pass_review_4 CHECK (`decision` IN ('PENDING','ACCEPT','REJECT')),
 CONSTRAINT fk_legacy_pass_review_6 FOREIGN KEY (`decided_by`) REFERENCES `app_user`(`id`),
 CONSTRAINT ck_legacy_pass_review_5 CHECK ((`decision`='PENDING' AND `decided_by` IS NULL AND `decided_at` IS NULL)
     OR (`decision`<>'PENDING' AND `decided_by` IS NOT NULL AND `decided_at` IS NOT NULL AND `reason` IS NOT NULL AND char_length(trim(`reason`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
ALTER TABLE `assessment_pass` ADD CONSTRAINT pass_legacy_review_fk
 FOREIGN KEY (`legacy_review_id`) REFERENCES `legacy_pass_review`(`id`);
CREATE UNIQUE INDEX one_pass_per_legacy_review ON `assessment_pass`(`legacy_review_id`);
CREATE TABLE `legacy_practice_summary` (
 `legacy_record_id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `user_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `question_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `old_question_id` longtext NOT NULL,
 `selected_option` integer,
 `correct` boolean,
 `attempts` integer,
 `last_updated_at` datetime(6),
 `gate_credit_approved` boolean NOT NULL DEFAULT false,
 CONSTRAINT fk_legacy_practice_summary_1 FOREIGN KEY (`legacy_record_id`) REFERENCES `legacy_record`(`id`),
 CONSTRAINT fk_legacy_practice_summary_2 FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`),
 CONSTRAINT fk_legacy_practice_summary_3 FOREIGN KEY (`question_id`) REFERENCES `question`(`id`),
 CONSTRAINT ck_legacy_practice_summary_1 CHECK (`attempts` > 0),
 CONSTRAINT ck_legacy_practice_summary_bool_correct CHECK (`correct` IN (0,1)),
 CONSTRAINT ck_legacy_practice_summary_bool_gate_credit_approved CHECK (`gate_credit_approved` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE TABLE `audit_event` (
 `id` char(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 `actor_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 `action` varchar(100) NOT NULL,
 `target_type` varchar(100) NOT NULL,
 `target_id` char(36) CHARACTER SET ascii COLLATE ascii_bin,
 `reason` longtext,
 `details` json NOT NULL,
 `occurred_at` datetime(6) NOT NULL,
 CONSTRAINT fk_audit_event_1 FOREIGN KEY (`actor_id`) REFERENCES `app_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX audit_target_time ON `audit_event`(`target_type`,`target_id`,`occurred_at` DESC);
