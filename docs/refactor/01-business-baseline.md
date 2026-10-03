# 业务基线（代码核对，2026-10-03）

后端 HEAD 1727c00，前端 HEAD d49ba21，两个独立仓库。开始时后端 application.yml 与 IDEA 文件、前端成绩页面/测试/部署脚本有用户修改。原始差异备份于 /private/tmp/xuexizhitu-refactor-baseline；未重置、覆盖或提交这些修改。未连接业务数据库，真实数据库内容与附件完整性不能由种子数据推断。

|领域|核实的规则与关系|现状/问题|证据|
|---|---|---|---|
|认证|用户名/邮箱统一命名空间；USER/ADMIN；用户级 token_version；刷新/退出撤销其他设备旧令牌；注册默认关闭|实现；配置工作区有固定密码/密钥，保留用户配置但部署必须显式环境覆盖|AuthService、TokenService、JwtUserConverter；BackendMySqlIT|
|课程周期|课程代码保留前导零；周期科目关联考试日；报考状态本人、修订号更新|实现；管理更新未显式校验考试日处于周期内，不擅加规则|LearningService/Repository、AdminService；LearningMySqlIT|
|内容|稳定章节/条目/题目身份，草稿独立；发布新版本、旧发布 RETIRED；题干/选项/答案绑定 revision|实现但草稿编辑无乐观版本，只有行锁；原创资格仍在稳定 question 上；全章审核没有独立审批人分离规则|ContentAdminService、V6；RemainingMySqlIT|
|目录知识笔记|完成度只统计当前目录条目；完成引用稳定 item；知识收藏/备注本人；答案按需揭示|实现；JDBC repository 同时承担部分业务判定，需要边界整理|Learning/ContentService；ContentMySqlIT|
|练习错题|稳定原创题联合去重计门槛，提交后揭示答案；提交幂等结果快照；旧汇总不补造作答|实现；部分 per-question/per-chapter 查询重复|PracticeService/Repository；PracticeMySqlIT|
|检测|章节 max(minimum,P) 且不得超 maximum；不同题位考点匹配；模拟最大余数法；未取整比例判通过；到期未答算错|实现；申请/答案保存 noRollbackFor=BusinessException 过宽，申请缺少显式 maximum 防线；超时任务无持久失败记录|AssessmentPlanner/Service/Repository；AssessmentMySqlIT|
|通过/真题权限|旧有效 pass 跨版本有效；新增章只影响新模拟；已缴费且有效模拟pass或临考override允许下载/写成绩；作废最后来源立即关闭|实现；需应用层共用授权与管理角色校验|AssessmentRepository.unlock、ExamService、AdminService；RemainingMySqlIT|
|成绩预测|成绩与 paper/cycle/user 关联；所有周期，首有效记录→60天→最近5套；至少3套，权重1..n；旧未知是否看答案不默认false|实现；编辑原地覆盖而无修订快照；读单条/列表全量加载并逐条读附件；缺乏成绩创建幂等|ScoreAnalysis/ExamService；ScoreAnalysisTest、RemainingMySqlIT|
|文件|私有 managed 或 classpath；用途/归属校验；≤8MiB；立即禁止删除中下载|实现但回滚删除和清理失败静默；进程崩溃孤儿没有对账；多实例清理缺少租约|PrivateFileStore；RemainingMySqlIT|
|计划|任务/容量/版本快照；ITEM完成来自进度；顺延只移动过去未完成段；确认检查日期/版本/指纹；容量不足须明示接受|实现；五周无模板自动造每科60分钟任务与已确认文档冲突|PlanService、V7、WeeklyPlanAllocator；RemainingMySqlIT|
|历史审核审计|legacy payload只读；通过认领须本人ADMIN理由；旧汇总审批只映射原创稳定题；删除附件不改原 payload|实现；动态JSON合法用于旧原始归档，不应扩散到新请求|LegacyService、AuditRepository；RemainingMySqlIT|
|图片自动批改|新请求/排队/领取续租/版本/回传校验/人工核对|尚未实现；旧grading只读，上传图片不评分，不返回虚构结果|无新任务路由，LegacyService|

## 数据与能力边界

V1–V7 共54张业务表，种子含六科公共内容、知识、题库与周期，不含真实个人数据。题库原创资格、近五年权重和真实试卷必须由管理员以真实依据录入，接口存在不等于数据可用。线上 D1/R2 导出没有提供：本次新增迁移和对账工具覆盖现有MySQL数据，不能宣称已完成该来源的真实数据导入演练。

## 规则冲突处理

五周默认复习任务：文档要求管理员模板和估时，代码及已有测试自动造60分钟。用户本次明确选择严格管理员模板，缺少时提示补齐；保留已生成计划快照。V7是已执行历史迁移，绝不修改。历史 template_id=NULL 的 REVIEW 行保留原义；新建和完整编辑须有每科 REVIEW 模板。

章节 maximum：文档/发布校验要求上限，申请服务只依赖 stats 间接检查；增加申请时直接拒绝，不改变规则。健康接口当前仅存活，不代表数据库就绪；增加独立 readiness，不改变原存活语义。JWT用户级撤销是已确认行为，保留，不引入会话制度。

## 前端依赖

实际前端 repo/frontend（React + generated API + api/*.ts），不是工作区其他同名目录。业务依赖目录、知识、练习、检测、成绩、计划、首页、登录模块；管理员路由未见实际管理页，不能宣称有管理UI。所有现有路径和 envelope 保持兼容；新增修订历史/幂等/就绪接口同步生成契约与前端调用，并以真实HTTP/浏览器验证。
