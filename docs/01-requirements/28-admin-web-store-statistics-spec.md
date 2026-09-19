# 管理后台 admin-web 医院数据统计 Spec（第 72 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈新增功能 Spec（医院列表「数据统计」按钮 + 统计弹窗，含后端聚合接口与医务人员登录时间落库） |
| 来源 | 测试反馈第 72 项（docs/测试记录.txt 第 218-219 行） |
| 涉及端 | 管理后台（admin-web）+ 后端（user-service、auth-service）+ 数据库（迁移 21） |
| 涉及页面 | 管理后台 - 医院管理 - 医院列表（/store/list） |
| 状态 | 已开发（2026-09-18 真机验证通过，见第 7 节；状态字段为 2026-09-19 补填） |

---

## 1. 需求清单（用户原话）

| # | 子项 | 主题 | 改动面 |
|---|---|---|---|
| 72-1 | 医院列表操作列新增「数据统计」按钮，点击弹出统计视图 | admin-web 前端（列表 + 弹窗 + API + 类型） |
| 72-2 | 弹窗含两方面：①医院各项基本信息；②动态统计数据：儿童档案数量、养护服务次数、本月预约数据、最后活跃日期（有医生或医生助理登录即为活跃） | 前端展示 + 后端聚合接口 |
| 72-3 | 「最后活跃日期」当前无数据源，需医务人员登录时间落库 | 后端 auth-service + 数据库迁移 |

用户原话：

- 「72、医院管理后台（http://39.162.49.28:5172/store/list）
  （1）在医院列表右侧的操作按钮区域，增加一个按钮，名称为数据统计，点击后弹出新页面，显示二个方面内容，第一个方面为医院各项基本信息，第二个方面为动态统计数据，名括儿童档案数量、养护服务次数、本月预约数据，及最后活跃日期（这个时间指有医生或医生助理登录即为活跃）」

---

## 2. 统计口径（已与用户逐点拍板）

| 指标 | 口径 | 依据 |
|---|---|---|
| 儿童档案数量 | `child_profile` 该院 `deleted_at IS NULL AND status <> 2`（**排除已隐藏**；已隐藏=家长侧删除的档案） | 「删除=隐藏」既定语义（第 67 项） |
| 养护服务次数 | `care_record` 该院 `status IN (1,2) AND deleted_at IS NULL`（**含养护中**） | 用户拍板 |
| 本月预约数据 | `reserve_order` 该院 `reserve_date` 落在本月、`status <> 4`（**排除已取消/已爽约**）、`deleted_at IS NULL`（**按预约日期**口径，与排班日历及既有统计接口一致） | 用户拍板 |
| 最后活跃日期 | `MAX(medical_staff.last_login_time)`（该院 `staff_role IN (1,2)` 即医生/医生助理）；登录成功才写入；无记录显示「暂无」 | 用户拍板「加登录时间列」；用户原话限定"有医生或医生助理登录即为活跃"（**不含店长等 sys_user 账号**） |

---

## 3. 后端设计

### 3.1 数据库迁移（`database/mysql/migration/21-medical-staff-last-login.sql`，新增）

```sql
-- 医务人员最后登录时间：医院活跃统计（有医生/医生助理登录即为活跃）
ALTER TABLE medical_staff
    ADD COLUMN last_login_time DATETIME NULL COMMENT '最后登录时间（登录成功写入）' AFTER status;
```

> MySQL 不支持 `ADD COLUMN IF NOT EXISTS`，脚本直接 ALTER；上线前按惯例先备份数据库。

### 3.2 登录时间写入（careld-auth-service）

- `mapper/MedicalStaffMapper.java` 新增专用语句（避免 `updateById` 误写其它列）：

```java
/** 登录成功写入最后登录时间（登录失败不写、不加锁定） */
@Update("UPDATE medical_staff SET last_login_time = #{time} WHERE id = #{id}")
int updateLastLoginTime(@Param("id") Long id, @Param("time") LocalDateTime time);
```

- `service/impl/AuthServiceImpl.java` `login()` 医务人员分支（现 92-97 行）：

```java
if (staff != null) {
    if (!passwordEncoder.matches(request.getPassword(), staff.getLoginPassword())) {
        throw new BusinessException(1001, "用户名或密码错误");
    }
    medicalStaffMapper.updateLastLoginTime(staff.getId(), LocalDateTime.now());
    return generateStaffTokenResponse(staff);
}
```

- 影响面：仅医务人员密码登录（手机号/用户名登录回退到 medical_staff 的路径同属此分支）；`refreshToken`、设备登录不写入；失败登录不写入（保持「医务人员不计数不锁定」现状）。

### 3.3 聚合接口（careld-user-service）

- `StatisticsMapper.java` 新增（共享库跨表聚合，沿用现有 `dashboard()` 子查询写法）：

```java
/** 医院数据统计：基本信息 + 动态统计（儿童/养护/本月预约/最后活跃） */
@Select("SELECT s.id AS storeId, s.store_code AS storeCode, s.store_name AS storeName, "
        + "s.institution_type AS institutionType, s.status AS status, "
        + "s.province_name AS provinceName, s.city_name AS cityName, s.district_name AS districtName, "
        + "s.address AS address, s.contact_name AS contactName, s.contact_phone AS contactPhone, "
        + "s.business_hours AS businessHours, s.join_date AS joinDate, s.bed_count AS bedCount, "
        + "a.agent_name AS agentName, c.center_name AS centerName, "
        + "(SELECT COUNT(*) FROM child_profile cp WHERE cp.store_id = s.id AND cp.deleted_at IS NULL AND cp.status <> 2) AS childCount, "
        + "(SELECT COUNT(*) FROM care_record cr WHERE cr.store_id = s.id AND cr.status IN (1,2) AND cr.deleted_at IS NULL) AS careCount, "
        + "(SELECT COUNT(*) FROM reserve_order ro WHERE ro.store_id = s.id "
        + "  AND ro.reserve_date BETWEEN #{monthStart} AND #{monthEnd} AND ro.status <> 4 AND ro.deleted_at IS NULL) AS monthlyReserveCount, "
        + "(SELECT MAX(ms.last_login_time) FROM medical_staff ms WHERE ms.store_id = s.id AND ms.staff_role IN (1,2)) AS lastActiveTime "
        + "FROM store_info s "
        + "LEFT JOIN agent a ON a.id = s.agent_id AND a.deleted_at IS NULL "
        + "LEFT JOIN ops_center c ON c.id = a.center_id AND c.deleted_at IS NULL "
        + "WHERE s.id = #{storeId} AND s.deleted_at IS NULL")
StatisticsDtos.StoreOverview storeOverview(@Param("storeId") Long storeId,
                                           @Param("monthStart") LocalDate monthStart,
                                           @Param("monthEnd") LocalDate monthEnd);
```

- `StatisticsDtos.java` 新增扁平 DTO `StoreOverview`：基本信息字段（storeId/storeCode/storeName/institutionType/status/provinceName/cityName/districtName/address/contactName/contactPhone/businessHours/joinDate/bedCount/agentName/centerName）+ 统计字段（childCount/careCount/monthlyReserveCount/lastActiveDate）。
- `StatisticsServiceImpl.storeOverview()`：按月首末日（`LocalDate.now().withDayOfMonth(1)` ~ `withDayOfMonth(lengthOfMonth())`）调用 Mapper；`lastActiveTime` 为空则 `lastActiveDate=null`，否则格式化为 `yyyy-MM-dd`；门店不存在时抛业务异常（沿用既有异常口径）。
- `StatisticsController` 新增：

```java
@Operation(summary = "医院数据统计")
@GetMapping("/store-overview")
public Result<StatisticsDtos.StoreOverview> storeOverview(@RequestParam("storeId") Long storeId) {
    return Result.success(statisticsService.storeOverview(DataScopeHelper.resolveStoreId(storeId)));
}
```

- 服务归属说明：统计类接口既有全部落在 user-service `/api/v1/statistics/*`（admin-web 已代理 `/statistics → 8282`），本接口沿用，不新增 store-service 端点；`DataScopeHelper.resolveStoreId` 保证总部/admin 可传任意 storeId、非总部角色被强制回自身范围。

---

## 4. 前端设计（admin-web）

### 4.1 操作列（`views/store/list.vue:77-87`）

- 列宽 `240 → 320`（容纳 4 个按钮，`.action-buttons` 保持 nowrap + gap 8px）；
- 新增按钮（置于「设备明细」之后、「启用/禁用」之前，与查看类操作相邻）：

```html
<el-button type="warning" size="small" @click="handleViewStatistics(row)">数据统计</el-button>
```

> 不加 `v-permission`：与同列的「设备明细」一致（该接口沿用 statistics 控制器现有鉴权口径——仅需登录态，admin-web 仅总部/管理角色可达）。

### 4.2 统计弹窗（沿用「设备明细」弹窗范式）

```html
<el-dialog v-model="statDialogVisible" :title="`${statStoreName} - 数据统计`" width="700px" destroy-on-close>
  <div v-loading="statLoading">
    <el-divider content-position="left">医院基本信息</el-divider>
    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="医院编码">{{ statData.storeCode }}</el-descriptions-item>
      <el-descriptions-item label="医院名称">{{ statData.storeName }}</el-descriptions-item>
      <el-descriptions-item label="机构性质">{{ institutionTypeLabel(statData.institutionType) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statData.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
      <el-descriptions-item label="运营中心">{{ statData.centerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="代理商">{{ statData.agentName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="所在地区" :span="2">{{ [provinceName, cityName, districtName].filter(Boolean).join(' / ') || '-' }}</el-descriptions-item>
      <el-descriptions-item label="详细地址" :span="2">{{ statData.address || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ statData.contactName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ statData.contactPhone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="营业时间">{{ statData.businessHours || '-' }}</el-descriptions-item>
      <el-descriptions-item label="加盟时间">{{ statData.joinDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="床位数量">{{ statData.bedCount ?? '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider content-position="left">动态统计</el-divider>
    <el-row :gutter="16">
      <el-col :span="6"><div class="stat-box"><div class="stat-value">{{ statData.childCount ?? 0 }}</div><div class="stat-label">儿童档案数量</div></div></el-col>
      <el-col :span="6"><div class="stat-box"><div class="stat-value">{{ statData.careCount ?? 0 }}</div><div class="stat-label">养护服务次数</div></div></el-col>
      <el-col :span="6"><div class="stat-box"><div class="stat-value">{{ statData.monthlyReserveCount ?? 0 }}</div><div class="stat-label">本月预约数据</div></div></el-col>
      <el-col :span="6"><div class="stat-box"><div class="stat-value">{{ statData.lastActiveDate || '暂无' }}</div><div class="stat-label">最后活跃日期</div></div></el-col>
    </el-row>
  </div>
</el-dialog>
```

- 统计块样式（scoped）：`.stat-box { background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; padding:12px; text-align:center }`、`.stat-value { font-size:20px; font-weight:700; color:#001529 }`、`.stat-label { margin-top:6px; font-size:12px; color:#64748b }`；「最后活跃日期」值为日期文本，字号可降为 16px 防折行。
- handler：

```ts
const handleViewStatistics = async (row: Store) => {
  statStoreName.value = row.storeName
  statDialogVisible.value = true
  statLoading.value = true
  statData.value = {}
  try {
    statData.value = await statisticsApi.getStoreOverview(row.id)
  } catch (e) {
    console.error('获取数据统计失败', e)
  } finally {
    statLoading.value = false
  }
}
```

### 4.3 API 与类型

- `src/api/statistics.ts` 新增：

```ts
getStoreOverview: (storeId: number): Promise<StoreOverview> =>
  request.get('/statistics/store-overview', { params: { storeId } })
```

- `src/types/index.ts` 新增 `StoreOverview`（上述 DTO 字段的前端映射，全可选 + 统计字段 `number | null`）。

---

## 5. 验证计划

- 后端：`mvn -pl careld-auth-service,careld-user-service -am package`（先 kill 旧进程再打包再启动，端口 8281/8282）；迁移 21 在本地库执行并核验 `medical_staff.last_login_time` 列存在。
- 落库链路：用医务人员测试账号（张小丽 13788888888，store 7）登录一次 → `SELECT id, store_id, staff_role, last_login_time FROM medical_staff WHERE phone='13788888888'` 应更新为当前时间；**不改动/重置任何密码**（登录前先记录原 last_login_time，验证后如需还原按原值处理）。
- 接口：`GET /api/v1/statistics/store-overview?storeId=<管理后台可见门店>`（admin token）返回基本信息 + 4 项统计；与 MySQL 直查交叉核对 4 个数字（儿童档案/养护/本月预约/最后活跃），数字不一致先查口径 SQL。
- 前端：admin-web `vue-tsc` 通过；浏览器（5172）医院列表操作列显示「数据统计」，点击弹窗标题为「{医院名} - 数据统计」，两个分区均有数据（无数据显示 `-`/`暂无`/`0`），操作列 4 按钮不折行。
- 边界：从未有医生登录的医院 → 最后活跃日期显示「暂无」；本月无预约 → 0。

## 6. 遗留与注意事项

1. **空列既有问题**：医院列表「设备数/人员数」两列后端不返回、恒空白（本批未要求，未列入改动；如需一并修需另定口径与后端补充查询）。
2. **索引**：`care_record.store_id`、`reserve_order(store_id, reserve_date)` 无专用索引；本接口为低频点查，数据量大后再评估加索引。
3. **口径可调点**：儿童档案数量是否含「已隐藏」、本月预约是否含已取消，均为一行 SQL 改动；如客户口径调整可直接改 `storeOverview` 语句。
4. 权限：沿用 statistics 控制器现状（无 `@RequirePermission`，仅需登录态）；若后续要按 `store:list:view` 收紧，需同步给按钮加 `v-permission`。

---

## 7. 验证结果（2026-09-18 本地实测）

- 数据库：迁移 `21-medical-staff-last-login.sql` 已在本地库执行，`SHOW COLUMNS FROM medical_staff LIKE 'last_login_time'` = `datetime YES NULL`。
- 编译/重启：`mvn -pl careld-auth-service,careld-user-service -am package` 成功；kill 旧进程后用 `./start-all.sh --no-frontend` 拉起（该脚本会加载 `backend/.env`，缺 `JWT_SECRET` 会启动失败），8281/8282 就绪，其余 8283-8287 未受影响。
- 落库链路：张小丽（13788888888，store 7，登录失败不锁定）登录 → `medical_staff.last_login_time` 由 `NULL` 变为 `2026-09-18 18:15:45`（验证后保留该真实登录时间；未改动任何密码）。
- 接口：`GET /api/v1/statistics/store-overview?storeId=7`（注意真实路径前缀为 `/api/v1`）返回 200，`childCount=17 / careCount=15 / monthlyReserveCount=23 / lastActiveDate=2026-09-18` 与 MySQL 直查交叉核对完全一致；登录前该字段为 `null`。
- 前端：admin-web `vue-tsc --noEmit` 通过；浏览器（localhost:5172/store/list）操作列出现 `数据统计` 按钮（careld3 无 `store:list:update`，故同排仅显示 设备明细 + 数据统计），点击后弹窗标题 = `武汉武昌协和卫生服务中心 - 数据统计`，基本信息 13 项（el-descriptions）与 4 个统计块渲染正确（17/15/23/2026-09-18）。
- 布局：操作列 `width=320`，单元格可用宽 296px；小号按钮实测宽度 设备明细/数据统计 = 72px，短按钮约 48px → 四按钮 + 3×8px 间距 ≈ 264px < 296px，不折行。
- 空值显示：`lastActiveDate` 为空时显示 `-`（与本文档第 5 节验证计划的「暂无」措辞不同，实际实现为 `-`）。
