# 个人平台接口与数据契约

新增能力复用现有 JWT、CurrentUser、ApiResponse、错误处理和用户锁。不接受 userId。所有 SQL 都带当前用户限定。旧学习接口、管理员权限与数据表不变。网页路由为 `/`、`/study/**`、`/fitness/**`、`/settings`；`/zikao/**` 保留查询和锚点跳转。

## 数据模型

`fitness_record` 是按用户、资源类型、日期/标识分区的聚合存储。每日训练项目和食物属于当天聚合，以 JSON 保存有序列表，在一次版本写入中提交；没有可跨用户关联的共享动作实体。体重有 `DECIMAL(9,3)` 存储列。Java 所有连续数值均用 BigDecimal，最多3位小数。JSON 中未知值为 null；计划与实际、训练与饮食、打卡与分项各自有独立资源键和版本。删除保留空载荷及递增版本，防止删除重建产生旧版本覆盖。

| kind | key | data 类型 |
|---|---|---|
| goal | UUID | Goal |
| weight | YYYY-MM-DD | Weight |
| training-plan | YYYY-MM-DD | TrainingPlan |
| training | YYYY-MM-DD | Training（服务端保存 planSnapshot） |
| meal-plan、meals | YYYY-MM-DD | Meals |
| checkin | YYYY-MM-DD | Checkin |
| water | YYYY-MM-DD | Water |
| training-template | UUID | TrainingTemplate |
| week-template | UUID | WeekTemplate，周一至周日恰好7项 |
| meal-template | UUID | MealTemplate |
| profile | current | Profile |

字段及可空值以 `src/main/java/cn/xuexizhitu/fitness/domain/FitnessModels.java`为权威，浏览器模型用 `python3 frontend/scripts/generate-fitness-types.py` 生成。增量 [OpenAPI](openapi.yaml) 给出资源与响应定义，`frontend/scripts/generate-platform-openapi.mjs` 同步前后端文档。原有主 OpenAPI 保持已有学习与管理员接口。

Goal：类型 LOSE / GAIN / MAINTAIN；起始日期、起始及目标kg必填，目标日期与备注可空。减重目标小于起始体重、增重大于起始体重、维持相同。目标日期不早于起始日期。目标历史永不覆盖。

Exercise：UUID、名称和类型 STRENGTH / CARDIO / MOBILITY / OTHER 必填，组数、次数、kg、分钟、km、备注、completed可空。力量项目用组数/次数/kg；有氧用分钟/km；灵活性和其他用分钟。无关字段须为空。同一聚合中动作UUID不重复。实际 COMPLETE 状态要求所有当时计划动作的UUID都有 completed=true。REST实际不能包含动作，且不能与非休息安排冲突。PARTIAL、SKIPPED单独保留。PENDING、UNPLANNED是后端派生展示状态。

Food：餐次 BREAKFAST / LUNCH / DINNER / SNACK、名称必填。份量与单位同时填写或同时未知；单位由用户描述（克、碗、份等），不自动转换。热量 kcal 与蛋白质/碳水/脂肪 g 都可空，非负。没有外部营养数据库。Weight.kg正数，上限由存储精度限制，不能解释为健康阈值。Water.ml是非负整数；Checkin睡眠0–24小时、状态1–5可空，备注可空。所有备注最大2000字；名称最大160字；每份聚合最多100项目，详情见模型校验。

## 请求与错误

所有接口在 `/api/v1` 下。响应 `{code:0,message,data}`；失败保留现有格式和HTTP状态。响应 `Cache-Control:no-store`。

| 操作 | 接口 | 前端连接 |
|---|---|---|
| 个人今日摘要 | GET /personal/summary | 个人首页 |
| 当前目标、最近体重、连续打卡、本周打卡率、7天均值 | GET /fitness/summary | 健身今天、目标 |
| 指定日期与统一状态 | GET /fitness/days/{date} | 所有健身页面 |
| 日期日历 | GET /fitness/history?from=&to= | 周计划、体重、历史 |
| 范围统计 | GET /fitness/statistics?from=&to= | 周/月历史、体重曲线均值 |
| 分页模板/目标/偏好 | GET /fitness/records/{kind}?page=1&size=30 | 目标、模板、账号 |
| 单条查询 | GET /fitness/records/{kind}/{key} | 编辑日期重复检查、复制检查 |
| 新增/修改 | PUT /fitness/records/{kind}/{key} | 所有编辑表单 |
| 删除 | DELETE /fitness/records/{kind}/{key}?revision= | 分项与模板删除 |
| 创建/调整目标历史 | PUT /fitness/records/goal/{新UUID} | 目标管理 |
| 结束目标 | POST /fitness/goals/end | 目标管理 |
| 创建/调整/结束事件 | GET /fitness/goals/history?page=&size= | 可审计历史接口 |
| 复制日期计划或个人模板 | POST /fitness/copy | 计划复制、模板应用 |
| 生成完整周安排 | POST /fitness/weeks/generate | 周模板应用 |

列表默认30条，最多100条，page从1起；按updated_at、key倒序，不一次读取全部历史。前端可加载下一页。日期范围最多367天（含两端），支持周/月和自定义自然日期；1900-01-01至当前日期后5年为软件日期查询范围。实际记录限过去及今天，计划允许未来，业务今天按Asia/Shanghai和注入Clock产生。不存在的本人单条资源返回data=null（含他人同名键）；复制他人的源返回404。

PUT：`{expectedRevision:-1,data:{...}}`。首次创建版本0；更新必须提供读取的版本；返回版本+1。goal写入还必须提供 `expectedGoalRevision`，来自summary，没有目标状态时为-1；创建后目标状态版本0。每次调整使用新UUID写入快照，状态PK保证一个当前目标，并新增历史事件。结束 `{expectedGoalRevision:n}` 原子清空当前指针、追加ENDED事件，快照保留。

所有PUT、复制、周生成和结束目标必须带 `Idempotency-Key: UUID`。服务端在当前用户锁下将操作、请求哈希、结果与业务修改一起提交；同键同请求返回原结果，同键不同请求40904。前端编辑表单在不改变草稿时重用写入键，保存中禁用重复提交。删除用版本检查及事务。

复制：`{sourceKind,sourceKey,destination,expectedRevision}`，只支持training-plan、training-template、meal-plan、meal-template。读源时校验当前用户。周生成：`{templateKey,monday,expectedRevisions:[7个版本]}`，起始必须周一；任一天冲突整个事务回滚。模板和日期内容均是独立快照；修改模板不影响已生成安排。

40001参数、类型、单位组合或范围不合法；40101登录失效；40301管理权限不足；40401本人复制源不存在；40901版本/重复日期冲突；40902目标历史不可覆盖等状态冲突；40904幂等键用于不同请求；42203未来实际日期；50001服务异常。前端显示错误并保留草稿，可取消后重新读取；不会静默覆盖冲突。

## 后端统一统计

- 7天体重均值：结束日及之前6个自然日期内实际记录的平均值，返回mean、samples、from、to。无样本mean=null。MySQL DECIMAL → Java BigDecimal，平均值保留3位，不补零、不插值。
- 训练完成率：范围内不晚于今天的已安排非休息训练日为分母；COMPLETED必须全部计划项目完成。PARTIAL与SKIPPED单列。无计划分母0，rate=null。实际记录保存当时planSnapshot，后续计划改动不改变该次实际的解释。
- 打卡：只看独立checkin载荷，允许空睡眠/状态/备注，不依赖其他分项。连续按自然日：今天有记录从今天开始，否则从昨天开始，首个断档终止。
- 本周打卡率：周一至今天，打卡天数/已经过自然天数，包含今天；未来日期不参与。休息日仍可打卡。
- 日历state：CHECKED_IN / TODAY_PENDING / PAST_MISSING / PARTIAL_RECORDS / FUTURE；rest单独表示，不能和打卡状态互相替代。
- 营养：每个指标返回knownTotal（没有已知值为null）、knownCount、foodCount、complete。任一食物指标未知，该指标complete=false；零值是明确填零，未知不算零。范围总量只针对实际已记录食物，dietDays另报覆盖天数，不能视为未记录日期的摄入总量。
- 前端只读取统计结果，曲线仅按实际记录画点，相邻自然日期才连线。UI不再计算第二套均值、完成率或打卡规则。

## 第一周训练与食谱导入（2026-10-04新增）

`POST /api/v1/fitness/weeks/import`，共用当前登录账号，必需UUID `Idempotency-Key`。

请求：`{startDate: "YYYY-MM-DD", days: [{training: TrainingPlan, meals: Meals, expectedTrainingRevision: -1, expectedMealRevision: -1}, ...]}`。

- days严格7项，按Day 1至Day 7排列；startDate可以是任意星期，不要求周一。
- training/meals和两个版本必填，现有模型/字段组合/日期范围校验继续生效。新建版本-1；覆盖需提供真实当前版本，含删除墓碑版本。
- 返回`Batch {items: FitnessEntry[]}`，按日期排列，每天先training-plan、后meal-plan，总计14项。
- 14项和幂等结果同一事务，任一非法值或版本冲突整体回滚；重试同键同内容返回原结果，同键异内容409。
- 不新建目标、实际训练、实际饮食、体重或打卡；已有实际快照不受计划覆盖影响。
- 页面内置内容是用户明确提供的计划文本，可编辑预览，首次只有点击保存才写入当前用户数据库。重量范围、可选食物与热量估算保留为备注，不伪造精确营养值。
- 保存后通过原日计划编辑接口继续修改，也可存为个人模板。
