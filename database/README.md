# PostgreSQL 数据卷升级

空数据卷仍由 `backend/src/main/resources/schema.sql` 和 `database/data.sql` 初始化。已有数据卷不会再次执行 Docker 入口初始化脚本；启动新版后端前，必须先运行 `database/migrations/20260914_upgrade_existing_volume.sql`。该脚本在事务内补字段和第二阶段新表，保留旧记录，并可重复执行。

通常执行 `docker-compose up -d`（或 `docker compose up -d`）。Compose 的 `migrate` 一次性服务会等待 `postgres` 可连接后执行脚本。启动后端前检查 `docker-compose ps -a` 和 `docker-compose logs migrate`：迁移服务应以退出码 0 结束；若非 0，先解决报错，不要启动新版后端。不要通过删除 `takeout-postgres-data` 数据卷来处理升级失败。

已用 `pwsh -NoProfile -File scripts/verify-old-volume-upgrade.ps1` 在独立 PostgreSQL 16 数据卷验证：从第一阶段 schema 建库，写入旧用户、商家、店铺和订单，重启容器，执行迁移两次后检查数据、字段、表和旧收货地址。脚本只会清理它创建的测试容器与测试卷，不操作项目数据卷。

若使用非 Compose 的数据库部署，在备份并确认连接目标后，由数据库维护人员用 `psql -v ON_ERROR_STOP=1 -f database/migrations/20260914_upgrade_existing_volume.sql` 执行同一迁移；运行环境应为 PostgreSQL 16。
