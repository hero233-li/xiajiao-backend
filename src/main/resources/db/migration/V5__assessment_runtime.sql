-- Frozen creation response supports exact replay after answers or content publication.
ALTER TABLE assessment_session ADD request_cycle_id char(36) CHARACTER SET ascii COLLATE ascii_bin,
 ADD apply_snapshot json,
 ADD CONSTRAINT assessment_request_cycle_fk FOREIGN KEY(request_cycle_id) REFERENCES exam_cycle(id),
 ADD CONSTRAINT assessment_apply_snapshot_object CHECK(apply_snapshot IS NULL OR JSON_TYPE(apply_snapshot)='OBJECT');
CREATE INDEX assessment_deadline_pending ON assessment_session(status,deadline_at,id);
-- Default policy is a configuration, not approval of original questions or mock weights.
INSERT INTO assessment_policy(id,course_id,release_id)
SELECT UUID(),c.id,r.id FROM course c JOIN content_release r ON r.course_id=c.id AND r.state='PUBLISHED'
WHERE c.course_type='THEORY' AND NOT EXISTS(SELECT 1 FROM assessment_policy p WHERE p.release_id=r.id);
