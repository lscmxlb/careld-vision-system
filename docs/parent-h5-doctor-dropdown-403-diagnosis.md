# parent-h5 儿童建档医师下拉"没有权限访问"问题诊断指南

## 问题描述
家长端 H5 创建儿童档案时，选择医院后"主治医师"下拉框显示"没有权限访问"错误。

## 已修复内容
- **commit**: `82e7ec1` (dev 分支)
- **文件**: `frontend/parent-h5/src/pages/child/form.vue`
- **改动**: `loadDoctors()` catch 块现在会：
  - 打印详细错误日志到 console
  - 通过 uni.showToast 显示后端返回的实际错误消息

## 诊断步骤

### 1. 重启前端服务
```bash
cd frontend/parent-h5
npm run dev:h5
```
访问 http://localhost:5177

### 2. 打开浏览器开发者工具
- Chrome/Edge: F12 或右键 -> 检查
- 切换到 Console 和 Network 标签

### 3. 重现问题
1. 使用家长账号登录（手机号 + 验证码 123456）
2. 进入"添加孩子"页面
3. 选择省/市/区
4. 选择医院
5. 观察"主治医师"下拉框

### 4. 检查控制台输出
**预期看到**：
```
加载医师列表失败: Error: xxx
```

**可能的错误消息及原因**：

| 错误消息 | HTTP 状态码 | 可能原因 | 解决方案 |
|---------|------------|---------|---------|
| 登录已过期，请重新登录 | 401 | Token 过期 | 退出重新登录 |
| 没有权限访问 | 403 | Spring Security 拒绝 | 检查用户类型是否为 parent(3) |
| 网络连接失败 | N/A | 网络不通或服务未启动 | 检查 user-service:8282 是否运行 |
| 该医院暂无可选医师 | 200 | 后端返回空列表 | 在管理后台为该医院添加医生 |

### 5. 检查 Network 请求
在 Network 标签中找到：
```
GET /api/v1/medical-staff?storeId=xxx&staffRole=1&status=1&page=1&size=200
```

**检查点**：
- **Status Code**: 应该是 200
- **Request Headers**: 应该有 `Authorization: Bearer eyJ...`
- **Response**: 应该返回 `{ "code": 200, "data": { "list": [...] } }`

### 6. 验证后端服务
```bash
# 检查 user-service 是否运行
curl http://127.0.0.1:8282/api/v1/medical-staff?storeId=1\&staffRole=1\&status=1\&page=1\&size=200

# 如果没有 token，先登录获取
curl -X POST http://127.0.0.1:8281/api/v1/auth/sms/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","code":"123456"}'
```

## 常见原因排查

### Token 过期
**症状**: 所有 API 都返回 401
**解决**: 
```javascript
// 在浏览器控制台执行
localStorage.removeItem('careld_parent_token')
localStorage.removeItem('careld_parent_user')
location.reload()
```

### CORS 问题（仅生产环境）
**症状**: Network 标签显示 CORS policy 错误
**解决**: 检查 nginx 配置，添加：
```nginx
add_header 'Access-Control-Allow-Origin' '*';
add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type';
```

### 医院确实没有医生
**症状**: API 返回 200，但 `data.list` 为空数组
**解决**: 
1. 登录管理后台 (http://localhost:5172)
2. 进入"系统设置" -> "医务人员"
3. 选择对应医院，点击"新增医务人员"
4. 填写姓名、手机号，角色选择"医生"

### 用户类型不是 parent
**症状**: DataScopeHelper 过滤逻辑异常
**解决**: 检查数据库中 sys_user.user_type 字段值应为 3

## 技术细节

### 后端权限控制
- `/api/v1/medical-staff` **没有** `@RequirePermission` 注解
- Spring Security 配置为 `.anyRequest().authenticated()`
- JwtAuthFilter 解析 token 失败时会清理 UserContext 并放行
- Spring Security 发现无认证信息会返回 **401**（不是 403）

### 前端错误处理链路
1. `uni.request` 收到响应
2. `request.ts` 检查 HTTP 状态码
3. 如果是 403，映射为 "没有权限访问"
4. 如果是业务错误信封 `{ code: xxx, message: yyy }`，显示 message
5. Toast 提示 + Promise reject
6. form.vue catch 块捕获并显示

### 数据权限逻辑
```java
// DataScopeHelper.resolveStoreIdWithParentChoice()
if (isParent()) {  // userType == 3
    return paramStoreId;  // 家长可以选择任意医院
}
return resolveStoreId(paramStoreId);  // 其他角色受限制
```

## 下一步
如果以上步骤都无法定位问题，请提供：
1. 浏览器控制台的完整错误日志
2. Network 标签中失败请求的详细信息（Headers + Response）
3. 用户信息（从 localStorage 读取 `careld_parent_user`）
