# TASK-002 / 拟 EXEC-002 设计交付报告

日期2026-11-11（Asia/Shanghai）。只提交设计供COORD-01登记待验收，未自授验收、未解锁代码。无聊天检索工具；sessions仍拟EXEC-002未绑定，现有分支/工作区未发现重复任务；不能声称核查了所有聊天，发现重复须暂停。

业务输入5f5e091含DEC-006–009；历史ac663c1仅对照；TASK-001验收输入f554f75；实际main起点26c45c78e88dffbce8bdc78cb9c9788242d32c50（含ACC-001和更新Prompt）。fetch后origin/main相同，requirements相对固定业务输入空差异，无额外业务变更。共享D:/GYM保持main；初始工作区干净。

独立工作区：C:/Users/28795/.codex/visualizations/2026/11/11/01a1ca82-ffc5-75e3-8e11-c36b69f7cadf/task-002。分支codex/task-002-domain-contracts，origin为用户指定GYM仓库。项目、父目录及docs未发现AGENTS.md；已读取用户提供全局规则，不记录本地连接凭据。首次fetch/worktree因.git权限失败，提升权限后成功；未退回共享目录编辑。

材料仅本目录：[领域](domain.md)、[权限](permissions.md)、[状态与例题](states.md)、[接口](contracts.md)、[场景与97项索引](scenarios.md)、[未决](open-decisions.md)、本报告。领域对象、字段、迁移所有权建议、14组契约及上下游生产消费均为草案，无运行时接口/迁移/数据库变更。

方案A既定方向，推荐Java/Spring/Flyway及Python/Django候选用官方资料核查，来源与日期见domain。完整设计/技术栈未批准。P01时间、P02日卡、P03补偿退款量化、P04运营权限、P05有效日、P06额度、P07商业支付、P08上线隐私、P09选型需协调者在正式验收或对应实现前处理，不能因未定价格阻塞无关权限。L01–L09全部保留。

人工走查：D01–D23逐项对照TASK-001及需求；单店越权、多身份独立、宽限端点、恢复交集25/40、赠送33.33/75、分币0.01、重复通知、退款消费互斥、授权撤回注销与跨端模拟均有明确输入动作预期；补偿分母33.33与28.57差异、停用尚未恢复50元、期限过仍剩次、日卡23:50边界和7自然日均明确为待批准结果。并发仅协议推演，不是锁/数据库测试；授权已送达内容不可追回，撤回拦新读取。

未运行构建/业务测试：无工程，未初始化、安装业务依赖、连接数据库、采购或配置渠道。模拟支付/手环没有实现；真实支付/AI/设备/消息/部署均未接入未验证。风险是跨模块事务实现、区间去重、渠道异常及上线授权保留期限尚需实证。TASK-003以后需设计及选型批准再拆分派发，TASK-005只支付退款，TASK-007独占模拟手环及榜单输入；TASK-008须拆唯一领域子任务。

验证与Git实绩将在实际执行后追加。未使用TASK-001/verify.py作main检查器。最终SHA以本分支HEAD为准，报告不能自包含自身SHA。没有自动回传能力，请用户将最终报告、最终SHA和docs/deliveries/TASK-002/材料路径带回COORD-01聊天。

## 实际文档检查

- Python标准库只读检查：7份Markdown、115个相对链接存在、REQ引用均在001–097、反向索引97/97唯一、23个D场景、14个C契约、常见password/api_key/secret赋值模式0；退出0。未声称扫描能识别所有敏感信息，另人工核对无凭据。
- `git diff --check`退出0；新增未跟踪文件另在暂存后执行`git diff --cached --check`，避免空差异漏检。
- 可复核命令：`git diff --name-only 26c45c7 HEAD`、`git diff --check 26c45c7`、`git status --short`；REQ用`rg '^\| REQ-' docs/deliveries/TASK-002/scenarios.md`计97行，D用`rg '^## D'`计23行，C用`rg '^\| C' docs/deliveries/TASK-002/contracts.md`计14行。链接检查逐个解析Markdown目标并用Path.exists确认。
- 人工确认只写本任务7份文档，未修改协调/需求/TASK-001；模拟与真实、待批准与已确认均区分。后续独立复核仅内部文档审阅，不是新增TASK执行聊天或正式验收。

内部只读复核提出三处缺项，均已补齐：C03消费/额度预占提交释放契约；未激活/到期权益进入退款hold及恢复派生状态；套餐目录及QuotaLedger唯一归属TASK-004。复核确认上述金额及日期例题未见算术错误，仍不构成政策批准或正式验收。暂存范围为7份本任务Markdown，`git diff --cached --check`退出0。
