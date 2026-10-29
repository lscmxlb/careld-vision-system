# PROGRESS — 开发进度看板

> 多人/多会话协作看板：开工前先读一遍，收工时更新。
> **规则：每完成一个小功能就提交到 dev 并更新本文件**；改动长期停留在未提交状态 = 别人覆盖你的风险。

**最后更新**：2026-10-29
**当前版本**：v2.3.6（dev = main = 61dd0b6，tag v2.3.6）

## 已上线（生产 182.92.100.21，最近部署 2026-10-29）

| 功能 | 相关迁移 | 状态 |
|---|---|---|
| 医务人员删除（软删 + 姓名追加「[已经删除]」+ 释放手机号 + 4 表姓名快照）及两端列表「已经删除」筛选（v2.3.6） | — | ✅ 已上线 |
| 运营中心体验卡「导出 Excel」按钮权限（center_admin 追加 export，v2.3.6） | 41 | ✅ 已上线 |
| 家长端隐私政策合规改造（注册必选勾选 + 政策全文页 + 登录提示 + 我的入口 + user_consent 留痕，v2.3.5） | 40 | ✅ 已上线 |
| 验证码校验超级密码放行（231218，v2.3.5 auth） | — | ✅ 已上线 |
| 基础信息页「预约规则设置」注释同行显示（v2.3.4 store-web） | — | ✅ 已上线 |
| 登录端级角色隔离（医生端仅医务人员、管理端拒家长） | — | ✅ 已上线 |
| child/vision/sync 接入 GuardFilter 轻量鉴权 | — | ✅ 已上线 |
| 爽约次数返还配置（appointment_config.no_show_refund） | 38 | ✅ 已上线 |
| 用户禁用存量治理（ZF- 前缀/清手机号） | 39 | ✅ 已上线 |
| 微信支付证书恢复（dev + 生产均验证 realReady=true） | — | ✅ 已上线 |
| 医院类型与品牌 Logo（v2.3.2） | 37 | ✅ 已上线 |
| 体验卡功能（v2.3.1） | — | ✅ 已上线 |

## 进行中

（无）

## v2.3.6 生产部署记录（2026-10-29）

- 数据库：部署前全库备份 `/root/backups/careld_vision-pre-v236-20261029-122354.sql.gz`（140,862 bytes）；执行迁移 41，`center_admin` 的 `trialcard:view` actions 已追加 `export`（JSON_CONTAINS=1）；清理 Redis `careld:perm:user:*` 权限缓存
- 后端：`careld-user-service-2.3.6.jar`（md5 `73f1098b00498557f9cca3382b167557`，生产/构建机一致），systemd ExecStart 切换后重启，8 个服务全部 active、8282 监听、日志 0 ERROR
- 前端：admin-web `index-hIpuicQx.js`（52 文件）、store-web `index-DOdBIgJd.js`（44 文件），旧版本备份于 `/root/backups/{admin-web,store-web}.bak-20261029-122419`
- 验证：公网 ops/med/staff/appt 均 200；API 登录成功，`status=2` 返回 200/0 条、`status=1` 11 条；admin-web 医务人员页「已经删除」筛选 UI 9/9 通过（截图 `/tmp/verify-236-prod-shots/`）
- 未覆盖：store-web 医务人员页与运营中心体验卡导出按钮未用真实医院/运营中心账号做 UI 登录验证（已通过 dist bundle 含新代码 + 公网 200 + 权限数据校验替代）

## 待办 / 遗留

- [ ] **支付回调 HTTPS 证书**：dev `https://h5.careld.net`、生产 `https://h51.careld.net` 均未配 SSL 证书，支付成功回调收不到，目前靠人工对账
- [ ] 生产服务器 /root/careld/repo 未重建（仅本地打包上传 jar/dist 的更新方式）
- [ ] 生产旧机 8080/8081/8082 后备入口未在新机配置

## 协作约定（必读）

1. **改代码前先 `git pull origin dev`**，改完尽快小步提交，不要攒大批未提交改动
2. **登录隔离等关键不变量见 [AGENTS.md](AGENTS.md)**——这些代码块任何人（含 AI 会话）不得删除
3. 提交分支为 dev；发布时同步 main（`git push origin dev:main` + 打 tag）
4. 生产部署：本地打包 → 上传 jar/dist → 重启；迁移 SQL 放 `database/mysql/migration/` 递增编号，生产执行前必须先备份库
5. 生产数据库备份放共享盘 db-dumps/，不入仓库
