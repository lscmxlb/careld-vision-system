# 第四阶段：API接口规范设计

## 4.1 接口设计原则

### 4.1.1 RESTful规范

```
┌─────────────────────────────────────────────────────────────────┐
│                      RESTful设计规范                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  URL设计                                                        │
│  ├─ 全部小写，使用连字符(-)分隔                                  │
│  ├─ 资源名词使用复数形式                                         │
│  ├─ 避免使用动词，使用HTTP方法表达操作                           │
│  └─ 示例：/api/v1/children, /api/v1/stores/123/devices           │
│                                                                 │
│  HTTP方法                                                       │
│  ├─ GET    - 获取资源                                           │
│  ├─ POST   - 创建资源                                           │
│  ├─ PUT    - 完整更新资源                                        │
│  ├─ PATCH  - 部分更新资源                                        │
│  └─ DELETE - 删除资源                                           │
│                                                                 │
│  状态码                                                         │
│  ├─ 200 OK - 成功                                               │
│  ├─ 201 Created - 创建成功                                      │
│  ├─ 400 Bad Request - 请求参数错误                              │
│  ├─ 401 Unauthorized - 未认证                                  │
│  ├─ 403 Forbidden - 无权限                                      │
│  ├─ 404 Not Found - 资源不存在                                  │
│  ├─ 422 Unprocessable - 业务校验失败                            │
│  └─ 500 Internal Error - 服务器错误                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.1.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1704067200000,
  "traceId": "abc123def456"
}
```

#### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "pages": 5
    }
  },
  "timestamp": 1704067200000
}
```

---

## 4.2 接口分组

### 4.2.1 接口列表总览

| 分组 | 路径前缀 | 说明 |
|------|---------|------|
| 认证接口 | /api/v1/auth | 登录、登出、Token刷新 |
| 用户接口 | /api/v1/users | 用户管理 |
| 门店接口 | /api/v1/stores | 门店管理 |
| TV设备接口 | /api/v1/devices | TV设备管理 |
| 儿童档案接口 | /api/v1/children | 儿童档案管理 |
| 预约排班接口 | /api/v1/schedules | 排班预约管理 |
| 视力检测接口 | /api/v1/vision | 视力检测记录 |
| TV同步接口 | /api/v1/sync | TV数据同步 |
| 数据统计接口 | /api/v1/statistics | 数据统计报表 |
| 文件接口 | /api/v1/files | 文件上传下载 |

---

## 4.3 认证接口

### 4.3.1 用户登录

```http
POST /api/v1/auth/login
Content-Type: application/json
```

**请求参数**

```json
{
  "username": "admin",
  "password": "password123",
  "captcha": "abc123",
  "captchaKey": "uuid-key",
  "loginType": 1
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "userType": 1,
      "storeId": null,
      "roles": ["super_admin"],
      "permissions": ["*"]
    }
  }
}
```

### 4.3.2 Token刷新

```http
POST /api/v1/auth/refresh
Authorization: Bearer {refreshToken}
```

### 4.3.3 用户登出

```http
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
```

### 4.3.4 TV设备登录

```http
POST /api/v1/auth/device-login
Content-Type: application/json
```

**请求参数**

```json
{
  "storeCode": "STORE001",
  "deviceCode": "TV20240101001",
  "appVersion": "1.0.0",
  "androidVersion": "11",
  "screenResolution": "1920x1080",
  "screenSize": 55.0
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "设备登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "deviceId": 1,
    "storeId": 1,
    "storeName": "北京朝阳门店",
    "calibrationStatus": 0,
    "calibrationData": null
  }
}
```

---

## 4.4 用户接口

### 4.4.1 获取当前用户信息

```http
GET /api/v1/users/me
Authorization: Bearer {accessToken}
```

### 4.4.2 用户列表（运营后台）

```http
GET /api/v1/users?page=1&size=20&userType=2&storeId=1&keyword=张三
Authorization: Bearer {accessToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "username": "doctor01",
        "realName": "张医生",
        "phone": "138****1234",
        "userType": 2,
        "storeId": 1,
        "storeName": "北京朝阳门店",
        "status": 1,
        "lastLoginTime": "2024-01-01 10:00:00",
        "createdAt": "2023-12-01 09:00:00"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 50,
      "pages": 3
    }
  }
}
```

### 4.4.3 创建用户

```http
POST /api/v1/users
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "username": "doctor02",
  "password": "TempPass123",
  "realName": "李医生",
  "phone": "13912345678",
  "userType": 2,
  "storeId": 1,
  "roleIds": [4]
}
```

### 4.4.4 修改用户

```http
PUT /api/v1/users/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### 4.4.5 重置密码

```http
POST /api/v1/users/{id}/reset-password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "newPassword": "NewPass123"
}
```

---

## 4.5 门店接口

### 4.5.1 门店列表

```http
GET /api/v1/stores?page=1&size=20&status=1&provinceCode=110000&keyword=朝阳
Authorization: Bearer {accessToken}
```

### 4.5.2 门店详情

```http
GET /api/v1/stores/{id}
Authorization: Bearer {accessToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "storeCode": "STORE001",
    "storeName": "北京朝阳门店",
    "provinceCode": "110000",
    "provinceName": "北京市",
    "cityCode": "110100",
    "cityName": "北京市",
    "districtCode": "110105",
    "districtName": "朝阳区",
    "address": "建国路88号",
    "longitude": 116.4551,
    "latitude": 39.9283,
    "contactName": "王店长",
    "contactPhone": "13800138000",
    "businessHours": "09:00-21:00",
    "networkType": 1,
    "status": 1,
    "deviceCount": 2,
    "staffCount": 5,
    "openTime": "2023-01-01",
    "createdAt": "2023-01-01 00:00:00"
  }
}
```

### 4.5.3 创建门店

```http
POST /api/v1/stores
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "storeCode": "STORE002",
  "storeName": "北京海淀门店",
  "provinceCode": "110000",
  "cityCode": "110100",
  "districtCode": "110108",
  "address": "中关村大街1号",
  "contactName": "李店长",
  "contactPhone": "13900139000",
  "businessHours": "09:00-21:00",
  "networkType": 1
}
```

### 4.5.4 更新门店

```http
PUT /api/v1/stores/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### 4.5.5 禁用/启用门店

```http
PATCH /api/v1/stores/{id}/status
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "status": 0
}
```

---

## 4.6 TV设备接口

### 4.6.1 设备列表

```http
GET /api/v1/devices?storeId=1&status=1&page=1&size=20
Authorization: Bearer {accessToken}
```

### 4.6.2 设备详情

```http
GET /api/v1/devices/{id}
Authorization: Bearer {accessToken}
```

### 4.6.3 绑定设备到门店

```http
POST /api/v1/devices/bind
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "storeId": 1,
  "deviceCode": "TV20240101002",
  "deviceName": "门店电视2"
}
```

### 4.6.4 解绑设备

```http
POST /api/v1/devices/{id}/unbind
Authorization: Bearer {accessToken}
```

### 4.6.5 更新设备校准数据

```http
PUT /api/v1/devices/{id}/calibration
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "calibrationStatus": 1,
  "calibrationData": {
    "pixelPerMm": 3.78,
    "screenWidthMm": 1210,
    "screenHeightMm": 680,
    "calibrationTime": "2024-01-01T10:00:00",
    "calibratedBy": "系统自动"
  }
}
```

---

## 4.7 儿童档案接口

### 4.7.1 档案列表

```http
GET /api/v1/children?page=1&size=20&storeId=1&auditStatus=1&keyword=张三
Authorization: Bearer {accessToken}
```

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| storeId | Long | 门店ID（门店医护必传） |
| auditStatus | Integer | 审核状态 |
| keyword | String | 姓名/手机号搜索 |
| startDate | String | 创建开始日期 |
| endDate | String | 创建结束日期 |

### 4.7.2 档案详情

```http
GET /api/v1/children/{id}
Authorization: Bearer {accessToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "childCode": "CH20240101001",
    "storeId": 1,
    "storeName": "北京朝阳门店",
    "name": "张小明",
    "phone": "138****1234",
    "birthDate": "2015-06-01",
    "age": 8,
    "gender": 1,
    "eyeCondition": "轻度近视",
    "medicalHistory": "无",
    "allergyInfo": "无",
    "familyHistory": "父亲近视",
    "auditStatus": 1,
    "auditRemark": null,
    "auditedBy": "运营管理员",
    "auditedAt": "2024-01-01 12:00:00",
    "parentUserId": null,
    "lastVisionTest": {
      "testTime": "2024-01-15 10:00:00",
      "leftEye": "4.8",
      "rightEye": "4.9"
    },
    "createdAt": "2024-01-01 09:00:00"
  }
}
```

### 4.7.3 创建档案

```http
POST /api/v1/children
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "name": "李小红",
  "phone": "13912345678",
  "birthDate": "2016-03-15",
  "gender": 0,
  "eyeCondition": "正常",
  "medicalHistory": "无",
  "allergyInfo": "无",
  "familyHistory": "无"
}
```

### 4.7.4 更新档案

```http
PUT /api/v1/children/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### 4.7.5 审核档案

```http
POST /api/v1/children/{id}/audit
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "auditStatus": 1,
  "auditRemark": "审核通过"
}
```

### 4.7.6 TV端检索儿童档案

```http
GET /api/v1/children/search?keyword=张小明&storeId=1
Authorization: Bearer {deviceToken}
```

**响应示例（TV端简化）**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "张小明",
      "phone": "138****1234",
      "birthDate": "2015-06-01",
      "gender": 1,
      "age": 8
    }
  ]
}
```

---

## 4.8 预约排班接口

### 4.8.1 排班日历

```http
GET /api/v1/schedules/calendar?storeId=1&startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {accessToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "2024-01-01": [
      {
        "id": 1,
        "scheduleDate": "2024-01-01",
        "timeSlotStart": "09:00",
        "timeSlotEnd": "10:00",
        "technicianId": 1,
        "technicianName": "技师A",
        "maxCapacity": 3,
        "reservedCount": 2,
        "availableCount": 1,
        "status": 1
      }
    ]
  }
}
```

### 4.8.2 创建排班

```http
POST /api/v1/schedules
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "scheduleDate": "2024-01-02",
  "technicianId": 1,
  "timeSlotStart": "09:00",
  "timeSlotEnd": "10:00",
  "maxCapacity": 3
}
```

### 4.8.3 批量创建排班

```http
POST /api/v1/schedules/batch
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-07",
  "technicianId": 1,
  "timeSlots": [
    {"start": "09:00", "end": "10:00", "capacity": 3},
    {"start": "10:00", "end": "11:00", "capacity": 3},
    {"start": "14:00", "end": "15:00", "capacity": 3}
  ],
  "weekDays": [1, 2, 3, 4, 5, 6, 7]
}
```

### 4.8.4 预约列表

```http
GET /api/v1/reserves?storeId=1&status=0&date=2024-01-01&page=1&size=20
Authorization: Bearer {accessToken}
```

### 4.8.5 创建预约

```http
POST /api/v1/reserves
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "childId": 1,
  "scheduleId": 1,
  "reserveType": 1,
  "parentName": "张先生",
  "parentPhone": "13800138000",
  "remark": "首次养护"
}
```

### 4.8.6 取消预约

```http
POST /api/v1/reserves/{id}/cancel
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "cancelReason": "客户改期"
}
```

---

## 4.9 视力检测接口

### 4.9.1 检测记录列表

```http
GET /api/v1/vision/records?childId=1&storeId=1&startDate=2024-01-01&page=1&size=20
Authorization: Bearer {accessToken}
```

### 4.9.2 检测记录详情

```http
GET /api/v1/vision/records/{id}
Authorization: Bearer {accessToken}
```

### 4.9.3 视力对比数据

```http
GET /api/v1/vision/compare?childId=1&reserveId=1
Authorization: Bearer {accessToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "childId": 1,
    "childName": "张小明",
    "reserveId": 1,
    "reserveDate": "2024-01-15",
    "beforeTest": {
      "testTime": "2024-01-15 09:30:00",
      "leftEye": "4.8",
      "rightEye": "4.9",
      "testerName": "张医生"
    },
    "afterTest": {
      "testTime": "2024-01-15 11:00:00",
      "leftEye": "4.9",
      "rightEye": "5.0",
      "testerName": "张医生"
    },
    "improvement": {
      "leftEye": "+0.1",
      "rightEye": "+0.1"
    }
  }
}
```

### 4.9.4 手动录入检测记录

```http
POST /api/v1/vision/records
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "childId": 1,
  "reserveId": 1,
  "testType": 1,
  "eyeType": 1,
  "visionLevel": "4.8",
  "testerName": "张医生",
  "remark": "室内光线良好"
}
```

---

## 4.10 TV同步接口（核心）

### 4.10.1 批量上传检测数据

```http
POST /api/v1/sync/upload
Authorization: Bearer {deviceToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "batchId": "batch_tv001_20240101120000",
  "deviceId": 1,
  "storeId": 1,
  "records": [
    {
      "localId": "local_001",
      "childId": "CH20240101001",
      "eyeType": "left",
      "visionLevel": "4.8",
      "testTime": "2024-01-01T10:00:00",
      "beforeAfter": "before",
      "testerName": "张医生",
      "remark": "室内光线良好"
    },
    {
      "localId": "local_002",
      "childId": "CH20240101001",
      "eyeType": "right",
      "visionLevel": "4.9",
      "testTime": "2024-01-01T10:05:00",
      "beforeAfter": "before",
      "testerName": "张医生",
      "remark": ""
    }
  ]
}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "batchId": "batch_tv001_20240101120000",
    "totalCount": 2,
    "successCount": 2,
    "failCount": 0,
    "results": [
      {
        "localId": "local_001",
        "status": "success",
        "cloudRecordId": "REC20240101001"
      },
      {
        "localId": "local_002",
        "status": "success",
        "cloudRecordId": "REC20240101002"
      }
    ]
  }
}
```

### 4.10.2 增量下发儿童档案

```http
POST /api/v1/sync/download/children
Authorization: Bearer {deviceToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "storeId": 1,
  "lastSyncTime": 1703980800000,
  "page": 1,
  "size": 100
}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "childId": "CH20240101001",
        "name": "张小明",
        "phone": "138****1234",
        "birthDate": "2015-06-01",
        "gender": 1,
        "updatedAt": 1704067200000
      }
    ],
    "hasMore": false,
    "nextCursor": null,
    "syncTime": 1704067200000
  }
}
```

### 4.10.3 同步状态回调

```http
POST /api/v1/sync/callback
Authorization: Bearer {deviceToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "batchId": "batch_tv001_20240101120000",
  "deviceId": 1,
  "syncType": "upload",
  "status": "success",
  "recordCount": 2,
  "successCount": 2,
  "failCount": 0,
  "completedAt": "2024-01-01T12:00:05"
}
```

### 4.10.4 获取同步配置

```http
GET /api/v1/sync/config
Authorization: Bearer {deviceToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "autoSyncInterval": 30,
    "maxBatchSize": 100,
    "retryInterval": [60, 300, 900, 3600],
    "maxRetryCount": 5,
    "dataRetentionDays": 90
  }
}
```

---

## 4.11 数据统计接口

### 4.11.1 门店客流统计

```http
GET /api/v1/statistics/store-traffic?storeId=1&startDate=2024-01-01&endDate=2024-01-31&groupBy=day
Authorization: Bearer {accessToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "summary": {
      "totalVisits": 150,
      "avgDaily": 5,
      "maxDaily": 12,
      "growthRate": "+15%"
    },
    "details": [
      {
        "date": "2024-01-01",
        "visitCount": 8,
        "newChildren": 3,
        "returnChildren": 5
      }
    ]
  }
}
```

### 4.11.2 视力改善统计

```http
GET /api/v1/statistics/vision-improvement?storeId=1&startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {accessToken}
```

### 4.11.3 全国门店数据汇总（运营后台）

```http
GET /api/v1/statistics/national-summary?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {accessToken}
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "storeCount": 50,
    "totalChildren": 5000,
    "monthlyVisits": 1500,
    "avgImprovement": "+0.15",
    "topStores": [
      {
        "storeId": 1,
        "storeName": "北京朝阳门店",
        "visitCount": 150,
        "improvementRate": "85%"
      }
    ]
  }
}
```

### 4.11.4 数据导出

```http
POST /api/v1/statistics/export
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求参数**

```json
{
  "exportType": "vision_records",
  "storeId": 1,
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "format": "excel"
}
```

---

## 4.12 文件接口

### 4.12.1 上传文件

```http
POST /api/v1/files/upload
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

file: [二进制文件]
path: "avatars"
```

**响应示例**

```json
{
  "code": 200,
  "data": {
    "fileId": "file_001",
    "fileName": "avatar.jpg",
    "fileUrl": "https://oss.careld.com/files/avatars/avatar_001.jpg",
    "fileSize": 102400,
    "mimeType": "image/jpeg"
  }
}
```

### 4.12.2 获取文件URL

```http
GET /api/v1/files/{fileId}/url?expireHours=24
Authorization: Bearer {accessToken}
```

---

## 4.13 错误码定义

| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| 200 | 成功 | 200 |
| 400 | 请求参数错误 | 400 |
| 401 | 未认证或Token过期 | 401 |
| 403 | 无权限访问 | 403 |
| 404 | 资源不存在 | 404 |
| 409 | 资源冲突 | 409 |
| 422 | 业务校验失败 | 422 |
| 429 | 请求过于频繁 | 429 |
| 500 | 服务器内部错误 | 500 |
| 503 | 服务暂不可用 | 503 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名或密码错误 |
| 1002 | 账号已被锁定 |
| 1003 | 验证码错误 |
| 2001 | 门店编码已存在 |
| 2002 | 门店不存在 |
| 3001 | 设备码已绑定其他门店 |
| 3002 | 设备未校准 |
| 4001 | 档案审核中，不可修改 |
| 4002 | 手机号已存在 |
| 5001 | 排班时间冲突 |
| 5002 | 预约已满 |
| 6001 | 同步数据格式错误 |
| 6002 | 同步数据冲突 |

---

## 4.14 阶段交付物

1. **《API接口规范文档V1.0》** - 本文档
2. **接口入参出参示例** - JSON示例文件
3. **基础接口测试用例** - Postman/YApi集合
4. **Swagger/OpenAPI文档** - 自动生成

---

## 4.15 API版本管理

```
版本策略：
├─ URL版本控制: /api/v1/, /api/v2/
├─ 向后兼容：旧版本API保持至少6个月
├─ 废弃通知：提前30天通知客户端
└─ 版本文档：每个版本独立文档
```
