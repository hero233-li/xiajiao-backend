-- Draft edits are isolated from published stable identities and runtime authorization.
ALTER TABLE content_release ADD draft_content json,
 ADD CONSTRAINT release_draft_content_object CHECK(draft_content IS NULL OR JSON_TYPE(draft_content)='OBJECT');
