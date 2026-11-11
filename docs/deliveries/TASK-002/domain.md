# TASK-002 领域与数据归属草案 v0.1

2026-11-11；EXEC-002 拟登记；设计待批准。业务固定5f5e091，调度起点26c45c7，TASK-001验收交付f554f75。所有新增字段、接口和算法均为设计建议，不能解释为实现授权。已确认规则以[需求](../../requirements.md)及DEC-006–009为准；差异分支见[未决](open-decisions.md)。

## 边界与唯一写入者建议

方案A内单部署、统一MySQL；模块内部仓储不可被其他模块调用。ID是逻辑引用，不授予写权限。跨模块原子事务由应用用例编排，各所有者执行自己的命令，不能直接写他人表。以下TASK是未来职责建议，不代表已派发；TASK-008须先拆分并绑定唯一执行者。

| 模块/建议负责人 | 独占对象与关键字段 | 依赖/发布契约 | 迁移所有权建议 |
| --- | --- | --- | --- |
| 身份 TASK-003 | Account(id,status,sessionEpoch)、StaffGrant(accountId,merchantId,storeScope,role,validUntil)、入口角色 | C01；向各模块提供已验证主体，不持卡状态 | IAM前缀，仅TASK-003 |
| 商家 TASK-004 | Merchant、Store(id,merchantId)、ServiceTerm(storeId,endsAt)、停用区间、恢复批次 | C02；向权益发布服务版本；入驻人工审核操作由后续管理子任务调用本模块 | TEN前缀，仅TASK-004 |
| 权益 TASK-004 | CardProduct、Entitlement(ownerId,storeId或null,type,totalUnits,remainingUnits,startAt,endAt,status,version)、CompensationLedger、消费流水 | C03/C04；交易发放/暂停/关闭命令；健康、课程、入场仅查询能力或请求消费 | ENT前缀，仅TASK-004 |
| 交易 TASK-005 | Order(beneficiary,productSnapshot,policyVersion,paidMinor,currency,mode)、PaymentAttempt、Refund(snapshot)、收退款流水、对账差异 | C05/C06/C07；只调用权益命令；支付适配器不写权益；卡及私教分开账类 | PAY前缀，仅TASK-005 |
| 出入场 TASK-004 | Visit(accountId,storeId,enteredAt,leftAt,source,estimated)、人工更正审计 | C04/C10；为TASK-007生产到店事实，不直接改榜单 | VIS前缀，仅TASK-004 |
| 课程 TASK-008未来课程唯一子任务 | ClassSession(capacity,coachId,timeRange)、Booking、CoachSlot、商家收费取消政策及接受证据 | C08；依赖身份/权益/交易；消费由权益写，课程只存消费引用 | CRS前缀，仅课程负责人 |
| 健康 TASK-006 | HealthRecord、Goal、MealDraft、Plan、CoachConsent(subject,coach,categories,expiresAt,epoch)、删除隔离作业 | C09；调用权益及AI适配；教练只有投影读取 | HLT前缀，仅TASK-006 |
| 设备与榜单 TASK-007 | 模拟手环原始事件、有效日、Participation、RegionAssignment、Appeal、榜单投影 | C10/C11；消费到店事实；唯一生产标准手环事件，TASK-005不涉此契约 | RNK前缀，仅TASK-007 |
| 器械 TASK-008未来器械子任务 | Equipment(storeId,state)、RepairReport、MaintenanceLog | C12；依赖身份及本店权益 | EQP前缀，仅器械负责人 |
| 客服运营 TASK-008未来客服/经营子任务 | Ticket、AttachmentRef、Complaint、ActionAudit、Notification、只读经营投影 | C13/C14；健康数据不复制进默认工单；交易/到店事实投影 | OPS前缀，拆分后各对象唯一负责人 |

迁移统一登记建议由TASK-003基础维护者串行分配全局序号；领域负责人提交自己前缀迁移，由COORD-01安排集成顺序。不得多模块修改同一对象结构；共享字段变更先升级契约并审影响消费者。本次没有迁移文件。Outbox由事件生产模块写，Inbox由消费模块写；共享事件信封定义建议TASK-003维护，业务负载由上表生产者独占。拒绝“统一库所以可以任意联表更新”。

## 不变量与数据最小化

个人权益storeId=null；门店卡必须绑定且永不转店。员工授权和付费资格分别计算；公共付费能力是所有有效来源的并集，专属能力仍限定对应来源。等级仅配置levelCode、识别/计划quota、显示标签，不产生券、积分、奖励或核销对象。REQ-019不解锁L03/L04。

不可变商品快照含购买/赠送数量、期限语义、激活政策、收费及取消政策版本；订单按归属区分平台个人收入、商家卡和私教收入。结算、抽成和银行卡路径未知，不能假设零抽成。时间补偿和资金退款分账，账务只追加冲正，不覆盖已确认事实。经营投影区分实收、实退、净额及模拟模式；会员总数按本店去重有效身份（提案），今日到店按自然日去重，当前在场按未离场Visit，缺失离场标estimated=true。

健康资料账号独占，按教练/类别/期限授权。位置只持久化地区码；请求坐标用于转换后丢弃且不写日志（提案）。工单图片私有访问、关联工单权限并作脱敏提示。财务必要隔离保留不意味着继续向普通客服公开健康资料。

## 技术候选（未批准）

| 候选 | 适配方案A及取舍 | 迁移工具 |
| --- | --- | --- |
| 推荐：Java + Spring Boot + MySQL | 显式服务边界和事务适合多模块退款/消费原子编排；类型约束便于共享契约审查，代码量和配置成本较高 | Flyway版本化SQL，统一序号；领域负责人评审，禁止启动时未经审批执行生产迁移 |
| Python + Django 5.2 LTS + MySQL | 管理后台与模型表达较快；模块边界需额外约束，不能让admin绕过领域命令 | Django原生迁移，集中依赖图管理；不与Flyway同时拥有同一表 |

官方核验日期2026-11-11：[Spring Boot系统要求](https://docs.spring.io/spring-boot/system-requirements.html)当次页面为4.1.2，要求Java至少17；[Django 5.2发布说明](https://docs.djangoproject.com/en/5.2/releases/5.2/)标注LTS、至少三年安全更新，支持Python3.10–3.14（3.14自5.2.8）；[Flyway迁移](https://documentation.red-gate.com/fd/migrations-271585107.html)说明版本迁移、历史表与顺序应用。以上为访问时观察，不承诺“永久最新”。推荐Spring方向不等于锁定4.1.2；实施前再核验补丁、Java版本、MySQL兼容和维护周期。没有安装/兼容测试，数据库名及配置到对应任务再询问。REQ-093；场景D22。

各模块REQ与用例的逐项映射见[场景](scenarios.md)，接口字段见[契约](contracts.md)。

## 内部复核补齐：商品与额度唯一所有权

权益TASK-004独占会员卡、平台个人套餐、门店服务年费套餐的商品目录及价格版本；商家只通过授权命令配置本店卡，平台专权配置个人/服务套餐，金额未批准时不可发布售卖。课程商品与取消政策由课程唯一子任务独占。交易TASK-005持下单时不可变商品/价格/归属快照，不回写目录；商家服务模块持实际服务期限，不持资金流水。

权益TASK-004独占能力额度配置及QuotaLedger(accountId,capability,period,limit,reserved,consumed,version)，健康TASK-006只请求预占/提交/释放，不自行计数；额度周期/叠加仍P06待批。共享账号跨卡能力的额度只此一处裁决。目录及Quota迁移均ENT前缀，避免健康/交易重复拥有。
