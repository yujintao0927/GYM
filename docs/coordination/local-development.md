# 本地开发环境（不含凭据）

2026-11-13用户授权数据库自行命名创建，并说明MySQL在Docker且已启动。

- 容器mysql-dev（核查ID fab0b99538c9），标签mysql:latest，主机localhost:3306，MySQL协议。
- SELECT VERSION()实际返回26.7.0，不按标签推断MySQL8；后续实际验证JDBC/Flyway兼容。
- gym_dev创建前不存在，创建utf8mb4/utf8mb4_unicode_ci后查询核实，当前表数0；未建立业务表、迁移或改其他数据库。
- 凭据仅读适用全局规则，经stdin送容器内0600临时配置，退出清理；不放进命令参数、日志、Prompt、源码或跟踪配置。
- 后续003B只允许gym_dev新IAM初始迁移；隔离测试库gym_dev_test_003b可在不存在时创建。已有未知对象先报告，不清库、不覆盖表、不执行Flyway clean，不授权生产操作。
- 本机无mysql客户端，Python临时驱动下载TLS失败；最终使用容器现有客户端，未新增中间件。
