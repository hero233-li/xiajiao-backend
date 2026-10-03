-- Preserve the paper business identity used by a score, even after administrator metadata edits.
ALTER TABLE score_record ADD COLUMN paper_key_snapshot varchar(64);
UPDATE score_record s JOIN paper p ON p.id=s.paper_id JOIN course c ON c.id=s.course_id
 SET s.paper_key_snapshot=CONCAT(c.code,':',DATE_FORMAT(p.paper_month,'%Y-%m'));
ALTER TABLE score_record MODIFY paper_key_snapshot varchar(64) NOT NULL;
ALTER TABLE score_record_revision ADD COLUMN paper_key_snapshot varchar(64);
UPDATE score_record_revision h JOIN score_record s ON s.id=h.score_id SET h.paper_key_snapshot=s.paper_key_snapshot;
ALTER TABLE score_record_revision MODIFY paper_key_snapshot varchar(64) NOT NULL;
