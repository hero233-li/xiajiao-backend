# 接口映射与前端依赖

既有104个操作保留原HTTP方法、路径、成功响应envelope、分页及统计含义；不是运行两套旧新后端。两个新增操作列在下表。OpenAPI快照位于后端和前端各自的docs目录，可独立审查与生成。

行为补充：创建成绩可传Idempotency-Key（UUID）；同键重放原响应，同键不同请求409。目录和计划完成沿用clientMutationId并实现同样重放。成绩修订继续使用expectedRevision，新增修订查询。草稿写入/发布可传If-Match整数版本，旧请求省略时保持兼容；管理UI尚不存在，不宣称已适配未实现页面。五周新建/完整编辑缺少每科复习模板时422，实际计划页面展示服务错误，不自动补任务。

实际前端适配：api/exams.ts发送稳定创建键，ScoreDialog展示真实修订；私有下载和图片存档沿用原契约，代码生成更新类型/方法；修复成绩页按钮被浮层挡住的真实点击问题；历史原始JSON仅作为归档显示，不进入新请求模型。

|旧方法与路径|当前方法与路径|操作|实际前端调用源（不含生成代码/测试）|
|---|---|---|---|
|POST /auth/login|POST /auth/login|login|src/app/routes.tsx、src/features/auth/AuthProvider.tsx、src/features/auth/LoginForm.tsx、src/features/auth/RequireAuth.tsx、src/mocks/handlers.ts、src/pages/LoginPage.tsx、src/test/helpers.tsx、src/utils/navigation.ts|
|GET /auth/me|GET /auth/me|getCurrentUser|src/features/auth/AuthProvider.tsx、src/mocks/handlers.ts|
|POST /auth/logout|POST /auth/logout|logout|src/features/auth/AuthProvider.tsx、src/mocks/handlers.ts|
|GET /dashboard|GET /dashboard|getDashboard|src/api/dashboard.ts|
|GET /courses|GET /courses|listCourses|src/api/notes.ts、src/features/schedule/PlanConfiguration.tsx、src/pages/CoursesPage.tsx、src/pages/TrainingPage.tsx|
|GET /courses/{courseId}|GET /courses/{courseId}|getCourse|无实际页面调用；已生成客户端/后端能力|
|GET /courses/{courseId}/learning-position|GET /courses/{courseId}/learning-position|getLearningPosition|src/features/catalog/CatalogPanel.tsx|
|PUT /courses/{courseId}/learning-position|PUT /courses/{courseId}/learning-position|saveLearningPosition|src/features/catalog/CatalogPanel.tsx|
|GET /exams/cycles|GET /exams/cycles|listCycles|src/api/dashboard.ts、src/api/exams.ts、src/api/knowledge.ts、src/api/notes.ts、src/api/practice-selection.ts、src/features/cycle/CycleContext.tsx|
|GET /exams/cycles/{cycleId}|GET /exams/cycles/{cycleId}|getCycle|src/features/courses/ExamScheduleEditor.tsx|
|GET /courses/{courseId}/enrollments/{cycleId}|GET /courses/{courseId}/enrollments/{cycleId}|getEnrollment|src/features/courses/EnrollmentControls.tsx|
|PUT /courses/{courseId}/enrollments/{cycleId}|PUT /courses/{courseId}/enrollments/{cycleId}|saveEnrollment|src/features/courses/EnrollmentControls.tsx|
|GET /catalog/courses/{courseId}|GET /catalog/courses/{courseId}|getCatalog|src/api/catalog.ts、src/api/schedule.ts|
|PUT /catalog/courses/{courseId}/items/{itemId}/completion|PUT /catalog/courses/{courseId}/items/{itemId}/completion|completeCatalogItem|src/api/catalog.ts、src/api/manual.ts|
|PATCH /catalog/courses/{courseId}/completions|PATCH /catalog/courses/{courseId}/completions|completeCatalogBatch|src/api/catalog.ts|
|GET /catalog/courses/{courseId}/knowledge|GET /catalog/courses/{courseId}/knowledge|listKnowledge|src/api/knowledge.ts|
|GET /catalog/courses/{courseId}/knowledge/{moduleId}|GET /catalog/courses/{courseId}/knowledge/{moduleId}|getKnowledge|src/api/knowledge.ts|
|PUT /catalog/courses/{courseId}/knowledge/{moduleId}/note|PUT /catalog/courses/{courseId}/knowledge/{moduleId}/note|saveKnowledgeNote|src/api/knowledge.ts|
|GET /catalog/courses/{courseId}/examples/{exampleId}/solution|GET /catalog/courses/{courseId}/examples/{exampleId}/solution|getExampleSolution|src/api/knowledge.ts|
|GET /catalog/courses/{courseId}/manual|GET /catalog/courses/{courseId}/manual|getManual|src/api/manual.ts|
|GET /catalog/courses/{courseId}/resources/{fileId}|GET /catalog/courses/{courseId}/resources/{fileId}|downloadCourseResource|src/api/knowledge.ts、src/features/catalog/CatalogPanel.tsx|
|GET /practice/courses/{courseId}/overview|GET /practice/courses/{courseId}/overview|getPracticeOverview|src/api/practice-selection.ts、src/api/practice.ts|
|GET /practice/courses/{courseId}/stats|GET /practice/courses/{courseId}/stats|getPracticeStats|无实际页面调用；已生成客户端/后端能力|
|GET /practice/courses/{courseId}/questions|GET /practice/courses/{courseId}/questions|listQuestions|src/api/practice-selection.ts、src/api/practice.ts|
|GET /practice/courses/{courseId}/questions/{questionId}|GET /practice/courses/{courseId}/questions/{questionId}|getQuestion|src/api/practice.ts|
|POST /practice/courses/{courseId}/questions/{questionId}/submissions|POST /practice/courses/{courseId}/questions/{questionId}/submissions|submitPracticeAnswer|src/api/practice.ts|
|GET /practice/courses/{courseId}/submissions|GET /practice/courses/{courseId}/submissions|listPracticeHistory|无实际页面调用；已生成客户端/后端能力|
|GET /practice/courses/{courseId}/submissions/{submissionId}|GET /practice/courses/{courseId}/submissions/{submissionId}|getPracticeResult|无实际页面调用；已生成客户端/后端能力|
|PUT /practice/courses/{courseId}/questions/{questionId}/mark|PUT /practice/courses/{courseId}/questions/{questionId}/mark|saveQuestionMark|src/api/practice.ts|
|POST /practice/courses/{courseId}/assessments|POST /practice/courses/{courseId}/assessments|applyAssessment|src/api/practice-selection.ts、src/features/practice/AssessmentGate.tsx|
|GET /practice/courses/{courseId}/assessments|GET /practice/courses/{courseId}/assessments|listAssessments|src/features/practice/AssessmentGate.tsx|
|GET /practice/courses/{courseId}/assessments/{sessionId}|GET /practice/courses/{courseId}/assessments/{sessionId}|getAssessment|src/api/assessments.ts|
|PUT /practice/courses/{courseId}/assessments/{sessionId}/answers/{revisionId}|PUT /practice/courses/{courseId}/assessments/{sessionId}/answers/{revisionId}|saveAssessmentAnswer|src/api/assessments.ts|
|POST /practice/courses/{courseId}/assessments/{sessionId}/submission|POST /practice/courses/{courseId}/assessments/{sessionId}/submission|submitAssessment|src/api/assessments.ts|
|GET /practice/courses/{courseId}/assessments/{sessionId}/result|GET /practice/courses/{courseId}/assessments/{sessionId}/result|getAssessmentResult|src/api/assessments.ts|
|GET /practice/courses/{courseId}/passes|GET /practice/courses/{courseId}/passes|listPasses|无实际页面调用；已生成客户端/后端能力|
|GET /exams/courses/{courseId}/unlock|GET /exams/courses/{courseId}/unlock|getUnlock|src/api/exams.ts、src/features/practice/AssessmentGate.tsx|
|POST /exams/courses/{courseId}/cycles/{cycleId}/overrides|POST /exams/courses/{courseId}/cycles/{cycleId}/overrides|confirmUnlockOverride|src/api/exams.ts|
|POST /exams/courses/{courseId}/cycles/{cycleId}/overrides/current/revocation|POST /exams/courses/{courseId}/cycles/{cycleId}/overrides/current/revocation|revokeUnlockOverride|src/api/exams.ts|
|GET /exams/courses/{courseId}/papers|GET /exams/courses/{courseId}/papers|listPapers|src/api/exams.ts|
|GET /exams/courses/{courseId}/papers/{paperId}|GET /exams/courses/{courseId}/papers/{paperId}|getPaper|无实际页面调用；已生成客户端/后端能力|
|GET /exams/courses/{courseId}/papers/{paperId}/file|GET /exams/courses/{courseId}/papers/{paperId}/file|downloadPaper|src/api/exams.ts|
|GET /exams/courses/{courseId}/scores|GET /exams/courses/{courseId}/scores|listScores|src/api/exams.ts|
|POST /exams/courses/{courseId}/scores|POST /exams/courses/{courseId}/scores|createScore|src/api/exams.ts|
|GET /exams/courses/{courseId}/scores/{scoreId}|GET /exams/courses/{courseId}/scores/{scoreId}|getScore|无实际页面调用；已生成客户端/后端能力|
|PUT /exams/courses/{courseId}/scores/{scoreId}|PUT /exams/courses/{courseId}/scores/{scoreId}|updateScore|src/api/exams.ts|
|GET /exams/courses/{courseId}/prediction|GET /exams/courses/{courseId}/prediction|getPrediction|src/api/exams.ts|
|GET /exams/courses/{courseId}/score-trend|GET /exams/courses/{courseId}/score-trend|getScoreTrend|src/api/exams.ts|
|POST /exams/courses/{courseId}/scores/{scoreId}/images|POST /exams/courses/{courseId}/scores/{scoreId}/images|uploadScoreImage|src/api/exams.ts|
|GET /exams/courses/{courseId}/scores/{scoreId}/images/{fileId}|GET /exams/courses/{courseId}/scores/{scoreId}/images/{fileId}|downloadScoreImage|无实际页面调用；已生成客户端/后端能力|
|DELETE /exams/courses/{courseId}/scores/{scoreId}/images/{fileId}|DELETE /exams/courses/{courseId}/scores/{scoreId}/images/{fileId}|deleteScoreImage|无实际页面调用；已生成客户端/后端能力|
|GET /exams/history|GET /exams/history|listLegacyHistory|src/api/exams.ts|
|GET /exams/history/{legacyId}|GET /exams/history/{legacyId}|getLegacyHistory|src/api/exams.ts|
|GET /exams/history/{legacyId}/files/{fileId}|GET /exams/history/{legacyId}/files/{fileId}|downloadLegacyAttachment|无实际页面调用；已生成客户端/后端能力|
|DELETE /exams/history/{legacyId}/files/{fileId}|DELETE /exams/history/{legacyId}/files/{fileId}|deleteLegacyAttachment|无实际页面调用；已生成客户端/后端能力|
|GET /practice/history|GET /practice/history|listLegacyPractice|无实际页面调用；已生成客户端/后端能力|
|GET /notes|GET /notes|listNotes|src/api/notes.ts|
|POST /notes|POST /notes|createNote|src/api/notes.ts|
|GET /notes/{noteId}|GET /notes/{noteId}|getNote|无实际页面调用；已生成客户端/后端能力|
|PUT /notes/{noteId}|PUT /notes/{noteId}|updateNote|src/api/notes.ts|
|GET /notes/tags|GET /notes/tags|listNoteTags|src/api/notes.ts|
|GET /schedule/plans|GET /schedule/plans|listPlans|src/api/schedule.ts|
|POST /schedule/plans|POST /schedule/plans|createPlan|src/features/schedule/PlanConfiguration.tsx|
|GET /schedule/plans/{planId}|GET /schedule/plans/{planId}|getPlan|src/api/schedule.ts|
|GET /schedule/plans/{planId}/revisions/{revisionNo}|GET /schedule/plans/{planId}/revisions/{revisionNo}|getPlanRevision|src/pages/SchedulePage.tsx|
|PUT /schedule/plans/{planId}/tasks/{taskId}/completion|PUT /schedule/plans/{planId}/tasks/{taskId}/completion|completePlanTask|src/api/schedule.ts|
|POST /schedule/plans/{planId}/reschedule-previews|POST /schedule/plans/{planId}/reschedule-previews|previewReschedule|src/api/schedule.ts|
|GET /schedule/plans/{planId}/reschedule-previews/{previewId}|GET /schedule/plans/{planId}/reschedule-previews/{previewId}|getReschedulePreview|无实际页面调用；已生成客户端/后端能力|
|POST /schedule/plans/{planId}/reschedule-previews/{previewId}/confirmation|POST /schedule/plans/{planId}/reschedule-previews/{previewId}/confirmation|confirmReschedule|src/api/schedule.ts|
|GET /courses/by-code/{courseCode}|GET /courses/by-code/{courseCode}|getCourseByCode|src/api/assessments.ts、src/api/catalog.ts、src/api/exams.ts、src/api/knowledge.ts、src/api/practice-selection.ts、src/api/practice.ts|
|GET /admin/practice/legacy-credits|GET /admin/practice/legacy-credits|adminListLegacyCredits|无实际页面调用；已生成客户端/后端能力|
|POST /admin/practice/legacy-credits/{legacyId}/decision|POST /admin/practice/legacy-credits/{legacyId}/decision|adminDecideLegacyCredit|无实际页面调用；已生成客户端/后端能力|
|POST /admin/courses|POST /admin/courses|adminCreateCourse|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/courses/{courseId}|PUT /admin/courses/{courseId}|adminUpdateCourse|无实际页面调用；已生成客户端/后端能力|
|POST /admin/exams/cycles|POST /admin/exams/cycles|adminCreateCycle|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/exams/cycles/{cycleId}|PUT /admin/exams/cycles/{cycleId}|adminUpdateCycle|src/features/courses/ExamScheduleEditor.tsx|
|GET /admin/courses/{courseId}/releases|GET /admin/courses/{courseId}/releases|adminListReleases|无实际页面调用；已生成客户端/后端能力|
|POST /admin/courses/{courseId}/releases|POST /admin/courses/{courseId}/releases|adminCreateRelease|无实际页面调用；已生成客户端/后端能力|
|GET /admin/courses/{courseId}/releases/{releaseId}/catalog|GET /admin/courses/{courseId}/releases/{releaseId}/catalog|adminGetCatalog|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/courses/{courseId}/releases/{releaseId}/catalog|PUT /admin/courses/{courseId}/releases/{releaseId}/catalog|adminPutCatalog|无实际页面调用；已生成客户端/后端能力|
|GET /admin/courses/{courseId}/releases/{releaseId}/knowledge|GET /admin/courses/{courseId}/releases/{releaseId}/knowledge|adminGetKnowledge|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/courses/{courseId}/releases/{releaseId}/knowledge|PUT /admin/courses/{courseId}/releases/{releaseId}/knowledge|adminPutKnowledge|无实际页面调用；已生成客户端/后端能力|
|GET /admin/courses/{courseId}/releases/{releaseId}/questions|GET /admin/courses/{courseId}/releases/{releaseId}/questions|adminGetQuestions|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/courses/{courseId}/releases/{releaseId}/questions|PUT /admin/courses/{courseId}/releases/{releaseId}/questions|adminPutQuestions|无实际页面调用；已生成客户端/后端能力|
|GET /admin/courses/{courseId}/releases/{releaseId}/assessment-policy|GET /admin/courses/{courseId}/releases/{releaseId}/assessment-policy|adminGetAssessmentPolicy|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/courses/{courseId}/releases/{releaseId}/assessment-policy|PUT /admin/courses/{courseId}/releases/{releaseId}/assessment-policy|adminPutAssessmentPolicy|无实际页面调用；已生成客户端/后端能力|
|GET /admin/courses/{courseId}/releases/{releaseId}/task-templates|GET /admin/courses/{courseId}/releases/{releaseId}/task-templates|adminGetTaskTemplates|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/courses/{courseId}/releases/{releaseId}/task-templates|PUT /admin/courses/{courseId}/releases/{releaseId}/task-templates|adminPutTaskTemplates|无实际页面调用；已生成客户端/后端能力|
|GET /admin/courses/{courseId}/releases/{releaseId}/validation|GET /admin/courses/{courseId}/releases/{releaseId}/validation|adminValidateRelease|无实际页面调用；已生成客户端/后端能力|
|POST /admin/courses/{courseId}/releases/{releaseId}/publication|POST /admin/courses/{courseId}/releases/{releaseId}/publication|adminPublishRelease|无实际页面调用；已生成客户端/后端能力|
|GET /admin/files|GET /admin/files|adminListFiles|无实际页面调用；已生成客户端/后端能力|
|POST /admin/files|POST /admin/files|adminUploadContentFile|无实际页面调用；已生成客户端/后端能力|
|POST /admin/exams/courses/{courseId}/papers|POST /admin/exams/courses/{courseId}/papers|adminCreatePaper|无实际页面调用；已生成客户端/后端能力|
|PUT /admin/exams/courses/{courseId}/papers/{paperId}|PUT /admin/exams/courses/{courseId}/papers/{paperId}|adminUpdatePaper|无实际页面调用；已生成客户端/后端能力|
|POST /admin/practice/passes/{passId}/invalidation|POST /admin/practice/passes/{passId}/invalidation|adminInvalidatePass|无实际页面调用；已生成客户端/后端能力|
|GET /admin/practice/alerts|GET /admin/practice/alerts|adminListAlerts|无实际页面调用；已生成客户端/后端能力|
|POST /admin/practice/alerts/{alertId}/acknowledgement|POST /admin/practice/alerts/{alertId}/acknowledgement|adminAcknowledgeAlert|无实际页面调用；已生成客户端/后端能力|
|GET /admin/exams/legacy-pass-reviews|GET /admin/exams/legacy-pass-reviews|adminListLegacyPassReviews|无实际页面调用；已生成客户端/后端能力|
|GET /admin/exams/legacy-pass-reviews/{reviewId}|GET /admin/exams/legacy-pass-reviews/{reviewId}|adminGetLegacyPassReview|无实际页面调用；已生成客户端/后端能力|
|POST /admin/exams/legacy-pass-reviews/{reviewId}/decision|POST /admin/exams/legacy-pass-reviews/{reviewId}/decision|adminDecideLegacyPass|无实际页面调用；已生成客户端/后端能力|
|GET /admin/dashboard/audit-events|GET /admin/dashboard/audit-events|adminListAuditEvents|无实际页面调用；已生成客户端/后端能力|
|POST /auth/refresh|POST /auth/refresh|refreshTokens|src/api/auth-recovery.ts、src/mocks/handlers.ts|
|POST /auth/register|POST /auth/register|registerUser|src/mocks/handlers.ts|
|GET /health|GET /health|getHealth|src/features/health/HealthCheck.tsx|
|新增|GET /exams/courses/{courseId}/scores/{scoreId}/revisions|listScoreRevisions|src/api/exams.ts|
|新增|GET /health/readiness|getReadiness|无实际页面调用；已生成客户端/后端能力|
|GET /grading/submissions|GET /grading/submissions|localGrading1|无实际页面调用；已生成客户端/后端能力|
|POST /grading/submissions|POST /grading/submissions|localGrading2|无实际页面调用；已生成客户端/后端能力|
|GET /grading/submissions/{id}|GET /grading/submissions/{id}|localGrading3|无实际页面调用；已生成客户端/后端能力|
|POST /grading/submissions/{id}/pages|POST /grading/submissions/{id}/pages|localGrading4|无实际页面调用；已生成客户端/后端能力|
|PUT /grading/submissions/{id}/pages|PUT /grading/submissions/{id}/pages|localGrading5|无实际页面调用；已生成客户端/后端能力|
|DELETE /grading/submissions/{id}/pages/{file}|DELETE /grading/submissions/{id}/pages/{file}|localGrading6|无实际页面调用；已生成客户端/后端能力|
|POST /grading/submissions/{id}/tasks|POST /grading/submissions/{id}/tasks|localGrading7|无实际页面调用；已生成客户端/后端能力|
|GET /grading/submissions/{id}/tasks|GET /grading/submissions/{id}/tasks|localGrading8|无实际页面调用；已生成客户端/后端能力|
|GET /grading/tasks/{id}|GET /grading/tasks/{id}|localGrading11|无实际页面调用；已生成客户端/后端能力|
|POST /grading/tasks/{id}/review|POST /grading/tasks/{id}/review|localGrading12|无实际页面调用；已生成客户端/后端能力|
|GET /grading/rubrics|GET /grading/rubrics|localGrading13|无实际页面调用；已生成客户端/后端能力|
|POST /grading/rubrics|POST /grading/rubrics|localGrading14|无实际页面调用；已生成客户端/后端能力|
|PUT /grading/rubrics/{id}|PUT /grading/rubrics/{id}|localGrading15|无实际页面调用；已生成客户端/后端能力|
|POST /grading/rubrics/{id}/publish|POST /grading/rubrics/{id}/publish|localGrading16|无实际页面调用；已生成客户端/后端能力|
|POST /grading/rubric-tasks|POST /grading/rubric-tasks|localGrading17|无实际页面调用；已生成客户端/后端能力|
|GET /grading/workers|GET /grading/workers|localGrading18|无实际页面调用；已生成客户端/后端能力|
|POST /grading/workers|POST /grading/workers|localGrading19|无实际页面调用；已生成客户端/后端能力|
|DELETE /grading/workers/{id}|DELETE /grading/workers/{id}|localGrading20|无实际页面调用；已生成客户端/后端能力|
|POST /grading-worker/heartbeat|POST /grading-worker/heartbeat|localGrading21|无实际页面调用；已生成客户端/后端能力|
|POST /grading-worker/claim|POST /grading-worker/claim|localGrading22|无实际页面调用；已生成客户端/后端能力|
|POST /grading-worker/tasks/{id}/renew|POST /grading-worker/tasks/{id}/renew|localGrading23|无实际页面调用；已生成客户端/后端能力|
|GET /grading-worker/tasks/{id}/materials/{file}|GET /grading-worker/tasks/{id}/materials/{file}|localGrading24|无实际页面调用；已生成客户端/后端能力|
|POST /grading-worker/tasks/{id}/result|POST /grading-worker/tasks/{id}/result|localGrading25|无实际页面调用；已生成客户端/后端能力|
|POST /grading-worker/tasks/{id}/failure|POST /grading-worker/tasks/{id}/failure|localGrading26|无实际页面调用；已生成客户端/后端能力|
|GET /grading/tasks/{id}/inputs|GET /grading/tasks/{id}/inputs|localGrading9|无实际页面调用；已生成客户端/后端能力|
|GET /grading/tasks/{id}/materials/{file}|GET /grading/tasks/{id}/materials/{file}|localGrading10|无实际页面调用；已生成客户端/后端能力|

共132个操作。安全规则与数据归属见业务基线；管理API只有管理员可用，私有资料需额外课程资格，个人成绩/计划/笔记/历史以安全上下文owner隔离。测试样本逐一验证实际响应，不从Mock示例推断实现能力。
