# 码上智厨 INIT 文档

> 本文是项目接手入口，供后续开发、评审、拆任务和 AI Agent 快速理解项目现状。完整产品细节以 [需求文档.md](需求文档.md) 为准，工程落地以 [开发文档.md](开发文档.md) 为准。

## 1. 当前状态

当前目录已经从“原型与需求整理目录”推进到“需求 + 开发文档 + 若依 Cloud 骨架 + 数据库脚本”的准备阶段。

已有内容：

- [需求文档.md](需求文档.md)：完整需求说明，包含用户端、管理端、数据模型、接口清单、流程、技术栈和技术难点。
- [开发文档.md](开发文档.md)：工程落地文档，按总体架构、数据库、接口、业务流程、技术栈、安全和开发阶段拆分。
- [压缩库表ER图.puml](压缩库表ER图.puml)：首版压缩表设计 PlantUML ER 图，使用 `json` 对象格式。
- [sql/cook_schema_compressed.sql](sql/cook_schema_compressed.sql)：首版压缩后的业务建表 SQL，共 22 张业务表。
- [sql](sql)：若依基础 SQL 副本和本项目业务 SQL。
- [ui/user](ui/user)：用户端 HTML 静态原型，共 20 个页面。
- [ui/admin](ui/admin)：管理后台 HTML 静态原型，共 11 个页面。
- [projetc/RuoYi-Cloud](projetc/RuoYi-Cloud)：若依 Cloud v3.6.8 工程骨架。

当前重要事实：

- 本地 MySQL 数据库名为 `cook`。
- 已使用 [sql/cook_schema_compressed.sql](sql/cook_schema_compressed.sql) 在 `cook` 库中建立 22 张业务表。
- 当前若依 Cloud 骨架尚未完成“码上智厨”业务模块改造。
- 目录名 `projetc` 是当前真实路径，后续如需规范化可改为 `project`，改名前需同步所有文档和脚本路径。

## 2. 产品定位

码上智厨是一款面向美食学习、菜谱分享、社区互动、饮食打卡和 AI 营养服务的应用。

用户端能力：

- 注册登录、短信验证码、验证码登录、找回密码。
- 首页推荐、搜索、分类浏览。
- 菜谱详情、HLS 教学视频、食材、步骤、小贴士。
- 发布菜谱、保存草稿、提交审核、已发布菜谱版本重审。
- 社区动态、评论、点赞、收藏、分享。
- 打卡月历，支持点击日期查看当天全部打卡。
- AI 营养问答，使用默认对话模型。
- 拍照识热量，使用独立视觉模型。
- 私信、群聊、通知，消息中心三 Tab 切换。
- 用户自主创建群聊，入口在消息中心右上角“+”。
- 问题反馈和独立内容举报。

管理端能力：

- 仪表盘统计。
- 菜谱管理、菜谱分类、轮播图管理。
- 菜谱审核、动态审核。
- 用户管理、群组管理。
- 内容举报处理。
- AI 模型配置，对话模型和视觉模型分开配置。
- AI 对话日志、AI 识图日志。

## 3. 首版基线规则

以下规则是首版工程拆分和验收基线，不作为待确认项：

- 需要独立登录页、注册页、验证码登录和找回密码能力，并支持短信验证码。
- 菜谱已发布后允许直接编辑，但编辑结果必须生成新版本并重新审核；审核通过前旧版本继续展示。
- 所有动态发布均为先审后发。
- AI 识图单独配置视觉模型，不与 AI 问答对话模型共用默认配置。
- 群聊允许用户自主创建，入口在消息中心右上角“+”。
- 通知留在消息中心，消息中心固定包含私信、群聊、通知三个 Tab。
- 菜谱详情、动态详情、评论项都需要独立举报入口。
- 编辑资料页中的性别、生日、地区支持直接编辑。
- 菜谱分类使用固定分组：中华菜系、场景分类、更多分类。
- 打卡月历支持点击日期查看当天所有打卡记录。
- 发布菜谱页右上角无“发布”按钮，底部按钮文案为“提交”；提交后进入管理端审核列表，审核通过后才成功发布。

## 4. 工程与技术栈

推荐目标技术栈：

- 管理端：若依 + Vue 3 + Element Plus + Vite + Pinia + TypeScript。
- 用户端：uniapp + Vue 3 + uView + Vite + Pinia + TypeScript。
- 后端：若依 Cloud + JDK 17 + Spring Boot 4 + Spring Cloud Alibaba。
- 数据库：MySQL 8.0。
- 缓存与实时：Redis、WebSocket。
- 视频：OSS/MinIO 存储 + FFmpeg/JavaCV 转 HLS。
- AI：Spring AI 接入，对话模型和视觉模型分别配置。

当前实际骨架：

- [projetc/RuoYi-Cloud](projetc/RuoYi-Cloud) 是 RuoYi-Cloud v3.6.8。
- 后端 `pom.xml` 当前为 JDK 17、Spring Boot 4.0.3、Spring Cloud 2025.1.0、Spring Cloud Alibaba 2025.1.0.0。
- 当前 `ruoyi-ui` 是 Vue 2 + Element UI + Vue CLI，不是 Vue 3 + Element Plus。若坚持目标技术栈，需要替换为若依 Vue3/TypeScript 版本或单独新建管理端。
- 若依内置模块包括 `ruoyi-gateway`、`ruoyi-auth`、`ruoyi-system`、`ruoyi-gen`、`ruoyi-job`、`ruoyi-file`、`ruoyi-monitor`。

## 5. 数据库现状

首版采用压缩表设计，优先控制业务表数量。若依后台自带系统权限表不计入本项目业务表范围。

业务 SQL：

- 文件：[sql/cook_schema_compressed.sql](sql/cook_schema_compressed.sql)
- 数据库：`cook`
- 业务表数量：22 张
- 执行方式：`mysql -uroot -p < sql/cook_schema_compressed.sql`

22 张业务表：

- 用户：`users`、`verification_codes`
- 菜谱：`recipes`、`recipe_versions`
- 社区互动：`posts`、`comments`、`content_interactions`、`checkins`
- 消息与群组：`groups`、`conversations`、`conversation_members`、`messages`
- 运营与风控：`banners`、`feedbacks`、`reports`
- AI：`ai_models`、`ai_messages`、`ai_image_recognition_logs`
- 文件与审计统计：`media_assets`、`user_penalties`、`admin_operation_logs`、`daily_statistics`

首版不单独建表的内容：

- 菜谱分类使用固定配置或后端枚举，菜谱保存分类编码。
- 菜谱食材、步骤、小贴士和视频信息放入 `recipe_versions` 的 JSON 字段，视频资源关联 `media_assets`。
- 动态图片、话题和位置放入 `posts` 的 JSON 字段，图片资源关联 `media_assets`。
- 菜谱审核、动态审核、模型配置、轮播上下架等后台动作统一写入 `admin_operation_logs`。
- 通知作为 `conversations` 中的通知会话和 `messages` 中的系统消息处理。
- 搜索历史和热门搜索首版可用 Redis 或用户本地缓存承载，后续需要运营分析时再拆表。

若依基础 SQL：

- [sql/ry_20260417_副本.sql](sql/ry_20260417_副本.sql)
- [sql/ry_config_20260311_副本.sql](sql/ry_config_20260311_副本.sql)
- [sql/quartz_副本.sql](sql/quartz_副本.sql)
- [sql/ry_seata_20210128_副本.sql](sql/ry_seata_20210128_副本.sql)

## 6. 核心数据关系

- 用户 `1:N` 菜谱、动态、评论、打卡、消息、反馈、举报。
- 用户兴趣标签首版存 `users.interest_tags_json`。
- 用户关注关系通过 `content_interactions(target_type=user, action_type=follow)` 承载。
- 分类首版使用固定配置；菜谱保存分类编码。
- 菜谱 `1:N` 菜谱版本；食材、步骤、小贴士和视频信息随版本存储。
- 菜谱编辑已发布内容时，新内容写入 `recipe_versions`，审核通过后替换线上版本。
- 动态与话题首版通过 `posts.topic_codes_json` 关联。
- 菜谱/动态/评论/用户与点赞收藏关注等互动是 `N:N`，通过 `content_interactions` 统一记录。
- 群组与用户是 `N:N`，通过 `conversation_members` 统一承载。
- 会话与用户是 `N:N`，通过 `conversation_members` 关联。
- 会话 `1:N` 消息。
- 通知作为特殊会话和系统消息处理。
- AI 模型 `1:N` AI 对话消息，模型类型为 `chat`。
- AI 模型 `1:N` AI 识图记录，模型类型为 `vision`。
- 举报 `N:1` 指向菜谱、动态、评论或用户。

## 7. 接口分层

用户端接口前缀：

- `/api/v1`

管理端接口前缀：

- `/api/admin/v1`

重点接口域：

- 认证：`/auth/codes`、`/auth/register`、`/auth/login`、`/auth/password/reset`
- 用户：`/users/me`、`/users/{id}/profile`、关注接口
- 首页搜索：`/home`、`/banners`、`/categories`、`/search`
- 菜谱：`/recipes`、`/recipes/{id}/submit`
- 评论互动：`/comments`、点赞、收藏、关注
- 动态：`/posts`、`/posts/{id}/submit`
- 打卡：`/checkins`、`/checkins/by-date`
- AI：`/ai/chat`、`/ai/recognize-food`
- 消息：`/conversations`、`/messages`、`/notifications`、`/groups`
- 举报：`/reports`
- 文件：`/uploads/images`、`/uploads/videos`、`/uploads/audios`
- 管理端审核：`/recipe-audits`、`/post-audits`
- 管理端 AI：`/ai/models`、`/ai/conversation-logs`、`/ai/recognition-logs`

## 8. 核心流程

菜谱发布：

1. 用户填写菜谱。
2. 可保存草稿。
3. 点击“提交”后进入 `pending_review`。
4. 管理端审核通过后公开展示。
5. 驳回后用户查看原因并修改重提。
6. 已发布菜谱再次编辑时生成新版本，旧版本继续在线展示，新版本审核通过后替换。

动态发布：

1. 用户发布动态。
2. 所有动态先进入审核。
3. 审核通过后进入社区广场。
4. 审核驳回或屏蔽后通过通知告知用户。

消息中心：

1. 消息中心固定三个 Tab：私信、群聊、通知。
2. 私信和群聊通过 WebSocket 推送实时消息。
3. 通知在消息中心展示，不单独做通知中心页面。
4. 右上角“+”进入创建群聊。

AI：

1. AI 问答调用默认对话模型。
2. AI 识图调用默认视觉模型。
3. 对话、识图都需要写入日志，供后台查询和标记。

举报：

1. 用户在菜谱详情、动态详情或评论项点击举报。
2. 提交举报类型、原因、说明和截图。
3. 管理端处理举报，可采纳、驳回或关闭。
4. 采纳后可联动屏蔽内容或处罚用户。

## 9. 开发入口与建议

后端建议：

- 优先在 [projetc/RuoYi-Cloud/ruoyi-modules](projetc/RuoYi-Cloud/ruoyi-modules) 下新增业务模块，例如 `ruoyi-cook`，承载菜谱、社区、AI、消息、运营等业务。
- 也可以按若依 Cloud 风格拆多个模块，但首版建议先合并业务模块，降低微服务拆分成本。
- 复用若依的认证、权限、日志、字典、代码生成、文件服务和定时任务能力。
- 业务表优先接入 `cook` 库中的 22 张压缩表。

管理端建议：

- 当前 [projetc/RuoYi-Cloud/ruoyi-ui](projetc/RuoYi-Cloud/ruoyi-ui) 是 Vue 2 + Element UI。
- 如果短期追求落地速度，可先基于当前若依 UI 改管理端页面。
- 如果严格执行目标技术栈，建议替换成若依 Vue3/TypeScript 版本，再实现管理端页面。

用户端建议：

- 新建独立 `user-app` 或 `uniapp-user` 工程。
- 以 [ui/user](ui/user) 静态原型为页面参考。
- 用户端 API 统一走 `/api/v1`。

数据库建议：

- 首版不要恢复完整多表设计，先使用 [sql/cook_schema_compressed.sql](sql/cook_schema_compressed.sql)。
- 只有在查询、审核、运营统计或权限颗粒度真实复杂后，再从 JSON 字段拆出明细表。

## 10. 开发顺序建议

第一阶段：基础骨架

- 配置若依 Cloud 基础环境、Nacos、Redis、MySQL。
- 执行若依基础 SQL 和本项目 `cook_schema_compressed.sql`。
- 接入认证、用户表、验证码。
- 落地登录、注册、验证码登录和找回密码基础页面。
- 建立统一响应、分页、权限、异常、审计日志。

第二阶段：菜谱主链路

- 固定分类配置、菜谱、菜谱版本、封面、食材、步骤、视频资源。
- 用户发布菜谱、草稿、提交审核。
- 管理端菜谱审核。
- 菜谱详情、评论、点赞、收藏。
- 已发布菜谱编辑生成新版本并重审。

第三阶段：社区主链路

- 动态发布、动态审核、社区广场。
- 评论、点赞、收藏、举报。
- 打卡记录和打卡月历。

第四阶段：消息与群组

- 会话、消息、群组、群成员。
- WebSocket 实时消息。
- 消息中心三个 Tab。
- 用户自主创建群聊。

第五阶段：AI 能力

- AI 模型配置，对话模型和视觉模型分开。
- AI 问答、AI 识图。
- AI 日志后台。

第六阶段：运营与统计

- 轮播图、热门搜索、推荐策略。
- 仪表盘统计。
- 操作日志、处罚、反馈和举报闭环。

## 11. 实现注意事项

- 不要把“反馈”和“举报”混成同一套业务：反馈是产品问题，举报是内容风控。
- 不要让已发布菜谱直接覆盖线上内容：必须走 `recipe_versions` 审核。
- 不要让动态先发后审：所有动态先审后发。
- 不要让 AI 识图复用对话模型默认配置：视觉模型必须独立。
- 菜谱分类分组固定，首版不需要后台维护分组字典。
- 通知保留在消息中心 Tab 内，不需要独立通知中心。
- 发布菜谱页底部按钮文案是“提交”，不是“提交审核”；右上角无“发布”按钮。
- 当前 ER 图是 PlantUML `json` 对象格式，渲染时需要 PlantUML 支持 JSON 对象语法；若本地渲染失败，优先检查 PlantUML 版本和 Graphviz `dot` 配置。
- 不要在文档中记录数据库密码。执行 SQL 时使用 `mysql -uroot -p` 手动输入密码。

## 12. 当前交付物

- 需求文档：[需求文档.md](需求文档.md)
- 开发文档：[开发文档.md](开发文档.md)
- 压缩库表 ER 图：[压缩库表ER图.puml](压缩库表ER图.puml)
- 业务建表 SQL：[sql/cook_schema_compressed.sql](sql/cook_schema_compressed.sql)
- 若依基础 SQL 目录：[sql](sql)
- 若依 Cloud 工程骨架：[projetc/RuoYi-Cloud](projetc/RuoYi-Cloud)
- 用户端原型目录：[ui/user](ui/user)
- 管理端原型目录：[ui/admin](ui/admin)

