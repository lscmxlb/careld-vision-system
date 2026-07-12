# 第七阶段：分层全维度测试方案

## 7.1 测试策略总览

```
┌─────────────────────────────────────────────────────────────────┐
│                      测试金字塔                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                          ┌─────────┐                           │
│                          │  E2E    │  端到端测试 (10%)          │
│                          │  测试   │                           │
│                       ┌──┴─────────┴──┐                      │
│                       │    集成测试      │  集成测试 (20%)       │
│                       │  (API/Service)  │                      │
│                    ┌──┴─────────────────┴──┐                 │
│                    │        单元测试          │  单元测试 (70%)  │
│                    │  (Service/Component)    │                 │
│                    └───────────────────────────┘                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7.2 单元测试

### 7.2.1 后端单元测试

#### 测试框架

| 组件 | 选型 | 说明 |
|------|------|------|
| 测试框架 | JUnit 5 | Java标准测试框架 |
| Mock框架 | Mockito | 模拟依赖对象 |
| 断言库 | AssertJ | 流式断言API |
| 覆盖率 | JaCoCo | 测试覆盖率统计 |

#### 测试目录结构

```
backend/careld-server/src/
├── main/java/com/careld/
│   └── ...
└── test/java/com/careld/
    ├── unit/
    │   ├── service/
    │   │   ├── ChildServiceTest.java
    │   │   ├── VisionServiceTest.java
    │   │   └── SyncServiceTest.java
    │   ├── utils/
    │   │   ├── EncryptionUtilTest.java
    │   │   └── DateUtilTest.java
    │   └── repository/
    │       └── ChildRepositoryTest.java
    └── integration/
        └── ...
```

#### 示例：儿童档案服务单元测试

```java
@ExtendWith(MockitoExtension.class)
class ChildServiceTest {

    @Mock
    private ChildRepository childRepository;
    
    @Mock
    private EncryptionService encryptionService;
    
    @InjectMocks
    private ChildService childService;
    
    @Test
    @DisplayName("创建儿童档案 - 成功")
    void createChild_Success() {
        // Given
        CreateChildRequest request = CreateChildRequest.builder()
            .name("张小明")
            .phone("13800138000")
            .birthDate(LocalDate.of(2015, 6, 1))
            .gender(1)
            .build();
        
        when(encryptionService.encrypt("张小明")).thenReturn("encrypted_name");
        when(encryptionService.encrypt("13800138000")).thenReturn("encrypted_phone");
        when(childRepository.save(any())).thenReturn(mockChildEntity());
        
        // When
        ChildDTO result = childService.createChild(request, 1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("张小明");
        assertThat(result.getAuditStatus()).isEqualTo(0); // 待审核
        verify(childRepository).save(any());
    }
    
    @Test
    @DisplayName("审核档案 - 通过")
    void auditChild_Approved() {
        // Given
        Long childId = 1L;
        AuditRequest request = AuditRequest.builder()
            .auditStatus(1)
            .auditRemark("审核通过")
            .build();
        
        ChildEntity child = mockChildEntity();
        child.setAuditStatus(0);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        
        // When
        childService.auditChild(childId, request, 1L);
        
        // Then
        assertThat(child.getAuditStatus()).isEqualTo(1);
        assertThat(child.getAuditedBy()).isEqualTo(1L);
        verify(childRepository).save(child);
    }
    
    @Test
    @DisplayName("查询档案 - 权限隔离")
    void getChildList_StoreIsolation() {
        // Given
        Long storeId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        
        // When
        childService.getChildList(storeId, null, pageable);
        
        // Then
        verify(childRepository).findByStoreId(storeId, pageable);
    }
}
```

### 7.2.2 TV端单元测试

#### 测试框架

| 组件 | 选型 | 说明 |
|------|------|------|
| 测试框架 | JUnit 4 | Android标准 |
| 测试框架 | AndroidJUnitRunner | Instrumentation测试 |
| Mock框架 | MockK | Kotlin专用Mock |
| UI测试 | Espresso | UI自动化测试 |

#### 示例：视力表计算器单元测试

```kotlin
class VisionChartCalculatorTest {

    private lateinit var calculator: VisionChartCalculator
    
    @Before
    fun setup() {
        calculator = VisionChartCalculator()
    }
    
    @Test
    fun `计算5_0视力视标尺寸 - 5米距离`() {
        // 5.0视力对应1分视角
        val sizeInMm = calculator.calculateOptotypeSize("5.0", 5.0)
        
        // 5米距离，1分视角，视标边长 = 2 * 5000 * tan(0.5') ≈ 1.454mm
        assertThat(sizeInMm).isBetween(1.45, 1.46)
    }
    
    @Test
    fun `计算4_0视力视标尺寸 - 5米距离`() {
        // 4.0视力对应10分视角
        val sizeInMm = calculator.calculateOptotypeSize("4.0", 5.0)
        
        // 10分视角是1分视角的10倍
        assertThat(sizeInMm).isBetween(14.5, 14.6)
    }
    
    @Test
    fun `像素尺寸转换`() {
        val sizeInMm = 1.454
        val pixelPerMm = 3.78  // 约96dpi
        
        val sizeInPixel = calculator.mmToPixel(sizeInMm, pixelPerMm)
        
        assertThat(sizeInPixel).isEqualTo(5)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `无效视力等级 - 抛出异常`() {
        calculator.calculateOptotypeSize("6.0", 5.0)
    }
}
```

### 7.2.3 前端单元测试

#### 测试框架

| 组件 | 选型 | 说明 |
|------|------|------|
| 测试框架 | Vitest | Vite原生测试 |
| UI测试 | Vue Test Utils | Vue组件测试 |
| Mock | MSW | API Mock |

#### 示例：Vue组件单元测试

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ChildList from '@/views/child/ChildList.vue'

describe('ChildList', () => {
  it('加载时显示加载状态', () => {
    const wrapper = mount(ChildList)
    expect(wrapper.find('.loading').exists()).toBe(true)
  })
  
  it('搜索后显示结果列表', async () => {
    const wrapper = mount(ChildList)
    
    // 模拟API响应
    vi.mock('@/api/child', () => ({
      getChildList: vi.fn().mockResolvedValue({
        data: {
          list: [
            { id: 1, name: '张小明', age: 8 }
          ]
        }
      })
    }))
    
    // 触发搜索
    await wrapper.find('input').setValue('张')
    await wrapper.find('button').trigger('click')
    
    // 验证结果
    expect(wrapper.findAll('.child-item')).toHaveLength(1)
    expect(wrapper.find('.child-item').text()).toContain('张小明')
  })
  
  it('无权限时显示空状态', async () => {
    // 模拟无权限
    vi.mock('@/stores/user', () => ({
      useUserStore: () => ({ storeId: null })
    }))
    
    const wrapper = mount(ChildList)
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })
})
```

---

## 7.3 集成测试

### 7.3.1 API集成测试

#### 测试范围

| 接口分组 | 测试场景 | 测试用例数 |
|---------|---------|-----------|
| 认证接口 | 登录/登出/Token刷新 | 15 |
| 用户接口 | CRUD/权限控制 | 20 |
| 门店接口 | CRUD/状态变更 | 15 |
| 档案接口 | 建档/审核/查询 | 25 |
| 预约接口 | 排班/预约/取消 | 20 |
| 视力接口 | 检测记录/对比 | 15 |
| 同步接口 | 上传/下载/冲突 | 30 |

#### 测试环境

```yaml
# docker-compose.test.yml
version: '3.8'
services:
  test-mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: test123
      MYSQL_DATABASE: careld_test
    ports:
      - "3307:3306"
  
  test-redis:
    image: redis:7-alpine
    ports:
      - "6380:6379"
  
  test-app:
    build: .
    depends_on:
      - test-mysql
      - test-redis
    environment:
      SPRING_PROFILES_ACTIVE: test
      SPRING_DATASOURCE_URL: jdbc:mysql://test-mysql:3306/careld_test
```

#### 示例：同步接口集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yml")
class SyncApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private VisionRecordRepository visionRecordRepository;
    
    @BeforeEach
    void setup() {
        visionRecordRepository.deleteAll();
    }
    
    @Test
    @DisplayName("批量上传视力记录 - 成功")
    void uploadVisionRecords_Success() throws Exception {
        // Given
        SyncUploadRequest request = SyncUploadRequest.builder()
            .batchId("batch_001")
            .deviceId(1L)
            .storeId(1L)
            .records(Arrays.asList(
                VisionUploadRecord.builder()
                    .localId("local_001")
                    .childId("CH001")
                    .eyeType("left")
                    .visionLevel("4.8")
                    .testTime(LocalDateTime.now())
                    .beforeAfter("before")
                    .build()
            ))
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/sync/upload")
                .header("Authorization", "Bearer " + getDeviceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.successCount").value(1));
        
        // 验证数据库
        assertThat(visionRecordRepository.count()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("批量上传 - 重复数据去重")
    void uploadVisionRecords_Duplicate() throws Exception {
        // 首次上传
        uploadVisionRecords_Success();
        
        // 重复上传相同数据
        SyncUploadRequest request = createUploadRequest();
        
        mockMvc.perform(post("/api/v1/sync/upload")
                .header("Authorization", "Bearer " + getDeviceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successCount").value(0))
            .andExpect(jsonPath("$.data.failCount").value(1));
    }
    
    @Test
    @DisplayName("跨门店数据隔离 - TV设备只能访问绑定门店")
    void crossStoreIsolation_Forbidden() throws Exception {
        // 使用门店A的设备Token访问门店B的数据
        String storeAToken = getDeviceTokenForStore(1L);
        
        mockMvc.perform(get("/api/v1/children/search")
                .header("Authorization", "Bearer " + storeAToken)
                .param("storeId", "2")) // 门店B
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
    }
}
```

### 7.3.2 数据库集成测试

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ChildRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("careld_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private ChildRepository childRepository;
    
    @Test
    @DisplayName("按门店查询档案 - 包含分页")
    void findByStoreId_WithPagination() {
        // Given
        Long storeId = 1L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        
        // 插入测试数据
        for (int i = 0; i < 25; i++) {
            childRepository.save(createChildEntity(storeId));
        }
        
        // When
        Page<ChildEntity> page = childRepository.findByStoreId(storeId, pageable);
        
        // Then
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("加密字段存储 - 验证加密")
    void encryptedFieldStorage() {
        // Given
        ChildEntity child = ChildEntity.builder()
            .nameEncrypted("encrypted_name_here")
            .nameMask("张**")
            .phoneEncrypted("encrypted_phone_here")
            .phoneMask("138****1234")
            .build();
        
        // When
        ChildEntity saved = childRepository.save(child);
        ChildEntity found = childRepository.findById(saved.getId()).orElseThrow();
        
        // Then
        assertThat(found.getNameEncrypted()).isEqualTo("encrypted_name_here");
        assertThat(found.getNameMask()).isEqualTo("张**");
    }
}
```

---

## 7.4 专项测试

### 7.4.1 TV离线缓存专项测试

#### 测试场景

```gherkin
Feature: TV离线缓存功能

Scenario: 断网状态下完成多次视力检测
  Given TV设备已绑定门店
  And 网络连接正常
  When 断开网络连接
  And 完成5次视力检测
  Then 所有检测数据应保存在本地SQLite
  And 同步状态为"未同步"

Scenario: 恢复网络后自动同步
  Given 本地有5条未同步记录
  When 恢复网络连接
  Then 应自动触发同步
  And 云端应收到5条记录
  And 本地同步状态更新为"已同步"

Scenario: 大批量数据同步
  Given 本地有500条未同步记录
  When 触发同步
  Then 应分批上传（每批100条）
  And 总同步时间应小于30秒
  And 所有记录同步成功

Scenario: 同步失败重试
  Given 网络不稳定
  When 同步过程中断网
  Then 应标记为同步失败
  And 重试次数+1
  And 按退避策略下次重试
```

#### 测试脚本

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class OfflineSyncTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun offlineVisionTest_MultipleRecords() {
        // 1. 断开网络
        device().executeShellCommand("svc wifi disable")
        device().executeShellCommand("svc data disable")
        
        // 2. 完成5次检测
        repeat(5) {
            onView(withId(R.id.btn_start_test)).perform(click())
            // 模拟视力检测流程
            performVisionTest()
            onView(withId(R.id.btn_save_result)).perform(click())
        }
        
        // 3. 验证本地存储
        val db = AppDatabase.getInstance(activityRule.activity)
        val unsyncedCount = db.visionRecordDao().getUnsyncedRecords().size
        assertEquals(5, unsyncedCount)
        
        // 4. 恢复网络
        device().executeShellCommand("svc wifi enable")
        
        // 5. 等待自动同步
        Thread.sleep(5000)
        
        // 6. 验证同步完成
        val remainingCount = db.visionRecordDao().getUnsyncedRecords().size
        assertEquals(0, remainingCount)
    }
    
    @Test
    fun syncConflict_Resolution() {
        // 测试冲突解决逻辑
        // ...
    }
}
```

### 7.4.2 并发压力测试

#### 测试方案

```java
@SpringBootTest
public class ConcurrentStressTest {

    @Autowired
    private VisionRecordService visionRecordService;
    
    @Test
    @DisplayName("20家门店同时上传 - 并发测试")
    void concurrentUpload_20Stores() throws InterruptedException {
        int storeCount = 20;
        int recordsPerStore = 50;
        ExecutorService executor = Executors.newFixedThreadPool(storeCount);
        CountDownLatch latch = new CountDownLatch(storeCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < storeCount; i++) {
            final long storeId = i + 1;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < recordsPerStore; j++) {
                        VisionRecordDTO record = createVisionRecord(storeId);
                        visionRecordService.save(record);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long duration = System.currentTimeMillis() - startTime;
        
        // 验证
        assertThat(duration).isLessThan(30000); // 30秒内完成
        assertThat(visionRecordService.count()).isEqualTo(storeCount * recordsPerStore);
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("高并发查询 - 性能测试")
    void concurrentQuery_Performance() throws InterruptedException {
        // 模拟100个并发查询请求
        // ...
    }
}
```

### 7.4.3 兼容性测试

#### Web端兼容性矩阵

| 浏览器 | 版本 | 运营Web | 门店Web | 家长Web |
|--------|------|---------|---------|---------|
| Chrome | 120+ | ✅ | ✅ | ✅ |
| Edge | 120+ | ✅ | ✅ | ✅ |
| Firefox | 120+ | ✅ | ✅ | ✅ |
| Safari | 17+ | ✅ | ✅ | ✅ |
| 微信内置 | 最新 | - | - | ✅ |
| Chrome Mobile | 120+ | - | - | ✅ |

#### TV端兼容性矩阵

| 品牌 | 型号 | Android版本 | 屏幕尺寸 | 测试结果 |
|------|------|------------|---------|---------|
| 小米 | 小米电视6 | 11 | 55寸 | 待测试 |
| TCL | C11 | 11 | 65寸 | 待测试 |
| 海信 | U7H | 10 | 75寸 | 待测试 |
| 创维 | A23 | 9 | 55寸 | 待测试 |
| 索尼 | X90L | 10 | 65寸 | 待测试 |
| 三星 | QN85C | 12 | 75寸 | 待测试 |

### 7.4.4 安全测试

| 测试项 | 测试内容 | 工具 |
|--------|---------|------|
| SQL注入 | 参数化查询验证 | SQLMap |
| XSS攻击 | 输入过滤验证 | XSSer |
| 越权访问 | 权限隔离验证 | 自定义脚本 |
| 敏感数据泄露 | 加密存储验证 | 数据库检查 |
| 接口安全 | 鉴权绕过测试 | Postman |

---

## 7.5 UAT用户验收测试

### 7.5.1 测试用户分组

| 用户类型 | 人数 | 测试内容 |
|---------|------|---------|
| 总部运营 | 2-3人 | 门店管理、档案审核、数据看板 |
| 门店医护 | 5-10人 | 建档、排班、预约、TV检测 |
| 家长用户 | 10-20人 | 档案查看、报告查看 |

### 7.5.2 UAT测试用例模板

```
用例编号: UAT-001
用例名称: 门店医护完成一次完整养护流程
前置条件: 
  - 门店医护账号已创建
  - TV设备已绑定门店
  - 门店已创建排班

测试步骤:
1. 门店医护登录门店Web
2. 新建儿童档案
3. 提交档案审核
4. 运营后台审核通过
5. 门店创建预约
6. TV端检索儿童档案
7. TV端完成养护前视力检测
8. 门店完成养护服务
9. TV端完成养护后视力检测
10. 门店Web查看对比记录
11. 家长Web查看报告

预期结果:
- 所有步骤可正常完成
- 数据在各端实时同步
- 视力对比数据准确

实际结果:
[待填写]

是否通过: [ ]通过 [ ]不通过
测试人: ___________
测试日期: ___________
```

---

## 7.6 测试交付物

1. **单元测试报告** - 覆盖率统计、测试通过率
2. **集成测试报告** - API测试结果、问题清单
3. **性能测试报告** - 并发测试结果、性能指标
4. **安全测试报告** - 漏洞扫描结果、修复建议
5. **兼容性测试报告** - 设备兼容性矩阵
6. **UAT测试报告** - 用户反馈、问题清单
7. **测试验收报告** - 综合测试结论

---

## 7.7 缺陷管理

### 缺陷分级

| 级别 | 定义 | 响应时间 | 修复时限 |
|------|------|---------|---------|
| P0-致命 | 系统崩溃、数据丢失 | 立即 | 4小时 |
| P1-严重 | 核心功能不可用 | 2小时 | 24小时 |
| P2-一般 | 次要功能异常 | 1天 | 3天 |
| P3-轻微 | UI问题、优化建议 | 3天 | 7天 |

### 缺陷跟踪

使用 Jira/禅道 进行缺陷管理，每个缺陷包含：
- 缺陷描述
- 复现步骤
- 截图/日志
- 严重程度
- 指派给
- 修复状态
