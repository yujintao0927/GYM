# C09-MANUAL v1：本人手动记录后端子集

依据已确认免费体重/饮食/健身手动记录、本人历史可查，及C09数据归属；DEC-025批准由协调者细化常规实现。本契约只用于local-dev本人记录，不改变正在执行WEB001的mock边界，不扩到教练共享、AI、营养估算/趋势、会员或注销。

## API

路径/v1/me/health-records，复用当前Bearer会话和成功/错误信封，mode=SIMULATED、authnSource LOCAL_DEV（如返回）；表示本地开发环境，记录数据实际持久化到开发MySQL，不能称正式健康云服务。主体只从当前认证取，不接受accountId/ownerId/merchantId/storeId/role字段。任一记录无本人才有的权限；平台/员工入口无读取豁免。

记录DTO：id不透明UUID、kind=WEIGHT/DIET/TRAINING、occurredAt带时区RFC3339毫秒（存UTC）、payload类型专用、version、createdAt、updatedAt。客户端传kind/occurredAt/payload；服务端生成所有其他字段，禁止额外字段。

- WEIGHT payload={weightKg:"十进制字符串"}，正值、最多3位小数，存DECIMAL(10,3)；上限9999999.999仅数据库技术范围，不冒充健康建议。
- DIET payload={foodName,portionText}，修剪后各1–120/1–200字；不估算热量、不识别图片。
- TRAINING payload={name,durationMinutes}，名称1–120字、分钟正整数且不溢出32位；不计榜单有效日，不生成训练计划。

POST根：必填Idempotency-Key，201返回记录。键绑定actor+操作+规范化请求HMAC指纹（复用私有密钥能力，不记录无密钥健康正文摘要）；同键同体返回同一记录、不重复新增，异体409。每次重放仍检查当前身份/所有权；若记录已删除，返回404且不恢复旧正文。

GET根：可选kind、from/to（带时区、[from,to)）、limit默认20最大100、cursor；200 data={items,nextCursor,asOf}。稳定(occurredAt,id)排序，游标绑定本人及筛选，防篡改；跨主体/筛选的游标拒绝422，不泄露或先加载全部用户再筛。GET /{id}仅本人，否则404。

PATCH /{id}：{occurredAt,payload,expectedVersion}，不允许换kind或主体，200返回新version；CAS不符409且零写入。DELETE /{id}：expectedVersion参数必填，200 data={id,deleted:true}；只删除本人这一条，版本不符409，不存在/非本人均404。相同删除重试不存在可404，不声称再次删除成功。

记录删除直接移除本条健康正文，不保留含原文的审计副本；必要幂等元数据仅id/键摘要/受密钥指纹，不包含可还原正文，也不能恢复已删记录。本子集是本地开发中用户对本人单条记录的纠错管理，不替代账号注销、商用保留/清理或财务保留政策，P08仍未完成。

## 认证、事务与错误

401无效/过期/撤销/关闭账号；跨主体404；422字段/游标非法；409版本/幂等冲突；503依赖错误，无SQL/秘密/健康正文泄漏。敏感响应no-store，未认证不能通过响应差异枚举记录。记录/回执修改同一本地事务，错误全回滚；不能误用身份防护计数的“预期拒绝仍提交”来提交半个健康写操作。

复用身份模块权威认证，可增加最小只读主体解析接口；Health不得另验密码、自建Session表、或通过clientKind/entry授予读取。解析与记录写在Account栅栏同事务，锁序Account→Session→Record/Receipt；退出/epoch/关闭与写竞争的先后明确且测试，不必实现关闭API。

仅local-dev注册记录路由，默认profile仍health唯一公开；不启Cookie/真实微信。当前frontend共同契约仍mock，后续单独联调时映射DTO，不能一边后台实现一边擅改WEB001/MINI001代码或静默切真实存储。
