-- Weekly review tasks belong to a plan, without mutating published course templates.
-- Existing ITEM and template-backed PAPER/REVIEW rows remain valid.
ALTER TABLE plan_task DROP CHECK ck_plan_task_4;
ALTER TABLE plan_task ADD CONSTRAINT ck_plan_task_4 CHECK (
  (kind = 'ITEM' AND template_id IS NULL) OR
  (kind = 'PAPER' AND template_id IS NOT NULL) OR kind = 'REVIEW'
);
