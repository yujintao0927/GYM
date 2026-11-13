# 本地开发环境（不含凭据）

2026-11-13用户授权数据库自行命名创建，并说明MySQL在Docker且已启动。

- 容器mysql-dev（核查ID fab0b99538c9），标签mysql:latest，主机localhost:3306，MySQL协议。
- SELECT VERSION()实际返回26.7.0，不按标签推断MySQL8；后续实际验证JDBC/Flyway兼容。
- gym_dev创建前不存在，创建utf8mb4/utf8mb4_unicode_ci后查询核实，当前表数0；未建立业务表、迁移或改其他数据库。
- 凭据仅读适用全局规则，经stdin送容器内0600临时配置，退出清理；不放进命令参数、日志、Prompt、源码或跟踪配置。
- 后续003B只允许gym_dev新IAM初始迁移；隔离测试库gym_dev_test_003b可在不存在时创建。已有未知对象先报告，不清库、不覆盖表、不执行Flyway clean，不授权生产操作。
- 本机无mysql客户端，Python临时驱动下载TLS失败；最终使用容器现有客户端，未新增中间件。

## 003B迁移及运行状态更新

e20e499已安装V1 IAM迁移于gym_dev及隔离测试库gym_dev_test_003b；据执行者和独立测试均为七IAM表加Flyway历史表，迁移重复启动可验证。先前“空库/无表”为创建时快照，不再代表当前状态。禁止重写已安装V1或清库；新模块需增量迁移及所有者协调。JDBC/Flyway警告与有限开发接受见ACC-009/RISK-013。

006A数据边界补充（DEC-025）：只在gym_dev以V2新增HLT本人记录/回执表，不改已执行V1；可创建不存在的gym_dev_test_006a作隔离测试，编码中同时保留gym_dev_test_003b的IAM回归。未知表/数据先报告、不DROP库/清库，后续业务不能凭此扩大访问范围。

夜间批次：006A已取消，原gym_dev_test_006a未要求创建。NIGHT001专用gym_dev_test_night001（可创建，V1+V2），NIGHT002只用原gym_dev_test_003b（V1），不连接开发gym_dev；只有NIGHT001可在gym_dev作保留V1的HLT增量验证。避免两分支不同迁移互相干扰，未知对象不自动清理。

## PAR-BIZ本地测试库和迁移预留（DEC-027）

六个新模块库仅用自己测试schema，用户已授权项目库创建，读取本地适用规则获取连接配置不落盘凭据：merchant-core=gym_dev_test_biz001/V3/mch_；trade-core=gym_dev_test_biz002/V4/trd_；booking-core=gym_dev_test_biz003/V5/bkg_；operations-core=gym_dev_test_biz004/V6/ops_；support-core=gym_dev_test_biz005/V7/sup_；ranking-core=gym_dev_test_biz006/V8/rnk_与vis_。

本轮协调者仅登记预留，尚未创建这些库或执行迁移；执行者只可创建自己的不存在测试库并显式本模块locations，禁止连接gym_dev、003b/night001及其他模块测试库。已有未知对象先报告，不DROP/clean/清库，不改V1/V2。池建议每模块<=4、单测试JVM/有界并发，不改全局MySQL/占共享服务端口。模块脚本放自己的src/main/resources/db/modules/<name>/，不自动加载入主服务。未来统一迁移与事务装配另行验证。
