# 64 · 医院标志按类型显示（四端）+ admin 医院列表展示优化

- **编号**：#112（自拟，用户未给编号）
- **提交人**：用户（对话，分 4 批）
- **日期**：2026-10-05
- **状态**：已开发（2026-10-05，沙箱四端真机 + API/DB 对账，见 §5）

## 1. 需求清单（用户原话拆解，4 批）

1. 「三个图标（fy/ws/yyy）现在是白底的，处理成透明背景」——图标去白底转透明 PNG。
2. 「所有显示医院名称的页面中，医院名称前面的图标现在都是一个固定的，要按照医院类型分别显示不同的图才是正确的」——社区卫生服务中心/卫生院 → ws，妇幼保健院 → fy，医院/其它 → yyy。
3. 「http://39.162.49.28:5172/store/list 医院列表页中，筛选条件区域优化，把筛选条件等宽度优化，各种上条件和条件按钮等优化成一行显示。在医院列表中，医院名称前面不用加 logo 图，只显示医院名称即可」。
4. 「医院列表页中，医院名称和医院类型这二列适当加宽，不要占用二行显示，其它列也适当优化，不需要太宽的时候就不要占用过多的宽度」。

## 2. 现状诊断（改造前）

- 四端均无「按类型区分」的医院标志；图标素材为白底图。
- 后端 `storeType` 数据链路缺失：医院列表/儿童档案/视力记录/充值订单/试用卡/操作日志等接口的响应均不带 `store_type`。
- admin `/store/list` 筛选区为多行错落布局；名称列前无 logo 需求反复（先加后按用户要求移除）。

## 3. 改造设计

### 3.1 图标素材（四端各一份，透明 PNG）

- `admin-web/src/assets/logos/`、`store-web/src/assets/logos/`、`doctor-h5/src/static/logos/`、`parent-h5/src/static/logos/` 下 `ws.png / fy.png / yyy.png`（白底已去除，透明背景）。

### 3.2 后端：5 个服务补 `storeType` 数据链路（22 文件）

| 服务 | 文件 | 说明 |
|---|---|---|
| user-service（10） | `dto/UserResponse`、`dto/PhoneLookupResponse`、`dto/OperationLogResponse`、`dto/StatisticsDtos`、`entity/User`、`mapper/UserMapper`、`mapper/OperationLogMapper`、`mapper/StatisticsMapper`、`service/impl/UserServiceImpl`、`service/impl/StatisticsServiceImpl` | 用户/手机号查找/操作日志/统计响应携带 `storeType`（JOIN `store_info`） |
| child-service（4） | `mapper/TrialCardMapper`（`ust.store_type AS used_store_type`）、`entity/TrialCard`（`usedStoreType`）、`entity/ChildProfile`（`storeType @TableField(exist=false)`）、`mapper/ChildMapper`（`s.store_type AS store_type`） | 试用卡/儿童档案提供类型 |
| vision-service（3） | `entity/VisionTestRecord`、`dto/VisionRecordGroup`、`mapper/VisionMapper` | 视力记录列表/分组/详情（3 处 SELECT 均带 `store_type`） |
| store-service（2） | `dto/DeviceResponse`、`service/impl/DeviceServiceImpl` | 设备列表携带所属医院类型 |
| notify-service（3） | `entity/StoreRechargeOrder`、`mapper/StoreRechargeOrderMapper`、`dto/RechargeOrderVO` | 短信充值订单携带类型 |

### 3.3 前端：四端 `StoreLogo` 组件（各一份，逻辑一致）

- `frontend/{admin-web,store-web,doctor-h5,parent-h5}/src/components/StoreLogo.vue`：`storeType===3 → fy`；`===4 || ===5 → yyy`；其余（1/2/undefined）→ `ws`；`size` 默认 20（各端按需传）。
- admin-web 接线页面：`store/list`（统计/试用弹窗标题、统计卡）、`store/trial-card`、`store/audit`、`store/recharge`、`store/medical-staff`*、`organization/agents`、`organization/centers`、`profile`、`statistics`、`vision`、`device`、`user`*、`operation-log`、`dashboard`（* 两处后续按 spec 65 移除）。
- store-web：顶部页头医院名前接 `StoreLogo`；doctor-h5：工作台首页、我的页；parent-h5：儿童档案卡「建档医院」行。

### 3.4 admin `/store/list` 展示优化

- 筛选区改 `flex` 等宽排布，一行显示（运营中心/代理商/状态/机构性质/医院类型/关键词/按钮组）。
- 名称列移除 logo，仅显示医院名称。
- 列宽最终值：医院编码 100 / 医院名称 min-240 / 省市区 190 / 机构性质 120 / 医院类型 150 / 运营中心 140 / 代理商 140 / 业务负责人 100 / 可用余额 110(右对齐) / 加盟时间 110 / 床位数 70 / 设备数 70 / 员工数 70 / 状态 70 / 操作 300(fixed)。
- `.action-buttons` 去除 `.el-button + .el-button { margin-left: 0 }` 造成的双重间距，按钮组 `gap: 8px`（4 按钮总宽 264 < 内容区 276，不裁切）。

## 4. 验证计划

1. 后端 5 服务重新打包重启，逐一 curl 对账 `storeType` 字段。
2. admin/store-web/doctor-h5/parent-h5 类型检查（vue-tsc）。
3. 沙箱真机：临时切换 store 1（北京朝阳门店）`store_type` 1→3→4，分别在四端断言 logo 文件（fy/yyy/ws），完成后**恢复原值 1**。
4. admin `/store/list`：列宽无折行、筛选区单行。

## 5. 验证结果（2026-10-05）

1. **后端对账**：`/vision/records`、`/vision/records/grouped`、children、trial-cards、devices、recharge orders、users、operation-logs、statistics 响应均含 `storeType`/`usedStoreType`（沙箱 live curl）。注：`VisionMapper` 曾因 rsync `-t` 保留旧 mtime 导致 Maven 增量编译跳过、jar 为旧代码——已用「push 后 touch 源文件再构建」修复；vision-service 启动一度因旧进程优雅关闭占用端口被 `start-all.sh` 误判"就绪"，已确认用新 jar 拉起（pid 3510546）。
2. **四端真机 logo 断言**（临时改 store 1 类型，DOM `img.src` 对账，随后恢复原值 1）：
   - type=3 → store-web `/dashboard`、`/child`、`/authorization-record` 页头均 `logos/fy.png`；doctor-h5 首页/我的 `fy.png`；parent-h5 儿童档案 4 张卡 `fy.png`。
   - type=4 → store-web `/child` 页头 `logos/yyy.png`。
   - type=1（恢复后）→ store-web `/child` 页头 `logos/ws.png`。
3. **admin `/store/list` 列宽**（1920 视口 iframe 实测）：6 条启用行全部单行（单元格高 42px）；医院名称最长 14 字不折行；操作列 4 按钮右缘 1851 < 单元格右缘 1875（未裁切）；唯一折行为禁用测试行 id9 的长地址（测试数据，可接受）。全表总宽 1980 > 容器 1610，横向滚动属预期。
4. `vue-tsc` 类型检查通过（四端）。

## 6. 遗留

- store-web `src/utils/xlsx.ts` 存在 pre-existing 类型错误（本批未触碰，与本次功能无关）。
- `GET /api/v1/medical-staff` 响应无 `storeName/storeType`（仅 `storeId`）；admin 医务人员列表 logo 按 spec 65 已移除，该缺口不再影响 UI，如未来需要再补。
- admin 用户管理「运营中心/代理商」两个 tab 与「医院维护」共用同一表格分支，医院列 logo 移除后同步生效（该两 tab 医院列原本多为"-"，无视觉影响）。
