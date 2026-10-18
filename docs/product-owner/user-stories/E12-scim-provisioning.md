# E12 SCIM 用户同步

← [用户故事地图](../User-Story-Map.md)

## 背景

企业在 Okta / Entra ID 中维护员工，Explore IAM 作为身份中心把用户推送给
Explore AI。员工首次打开 Explore AI 前账号已就绪；离职或移出分配组后账号
立即停用；在 IAM 删除后，Explore AI 擦除其全部数据。Explore AI 只实现
SCIM 2.0 Users，不接收 Groups。

---

## US-37 Explore IAM 预置与更新账号

**As a** 合规与平台管理员  
**I want** Explore IAM 通过 `/scim/v2/Users` 创建、更新和停用 Explore AI 账号  
**So that** 员工账号与企业目录保持一致，无需在 Explore AI 手工维护

### 验收标准

1. **Scenario** 只接受 IAM 的 SCIM 令牌
   **GIVEN** 请求携带 IAM 签发、含 `write:ai_scim` 的令牌  
   **WHEN** 调用 `/scim/v2/**`  
   **THEN** 请求被处理；缺少令牌返回 401，缺少 scope 返回 403（SCIM Error）

2. **Scenario** 预置账号并在首次登录时复用
   **GIVEN** IAM `POST /scim/v2/Users`，`externalId` 为 IAM 用户 id  
   **WHEN** 该员工之后用 Explore IAM 登录  
   **THEN** 复用同一个 Account（身份 `explore-iam` + IAM `sub`），不会重复建号

3. **Scenario** 冲突与并发
   **GIVEN** 相同 `externalId` 或 `userName` 已存在  
   **WHEN** 再次创建  
   **THEN** 返回 409 `uniqueness`；`PUT` / `PATCH` 的 `If-Match` 与当前版本不符时返回 412

4. **Scenario** 停用后不能登录
   **GIVEN** IAM 将账号设为 `active=false`  
   **WHEN** 员工登录或调用 `/api`  
   **THEN** 返回 403 `ACCOUNT_INACTIVE`，数据保留至账号被删除

5. **Scenario** 机器令牌不会成为账号
   **GIVEN** `client_credentials` 令牌（`aud` 包含 `sub`）  
   **WHEN** 调用需要登录的 `/api` 端点  
   **THEN** 返回 403 `MACHINE_TOKEN`，不创建 Account

### 状态

已实现

---

## US-38 删除账号时擦除数据

**As a** 合规与平台管理员  
**I want** IAM 删除用户时，Explore AI 删除账号并擦除其全部数据  
**So that** 满足删除权（GDPR 第 17 条）

### 验收标准

1. **Scenario** 删除即擦除
   **GIVEN** 账号拥有会话、文档、向量、Skills、工作流和自动化  
   **WHEN** IAM 调用 `DELETE /scim/v2/Users/{id}`  
   **THEN** 先按 Owner Key 擦除所有数据，再删除 Account，返回 204

2. **Scenario** 只暴露 IAM 账号
   **GIVEN** 存在 Google / GitHub 登录创建的账号  
   **WHEN** IAM 列出或查询 `/scim/v2/Users`  
   **THEN** 只返回 `explore-iam` 账号

### 状态

已实现

---

## 手工验证

上线前用 [Microsoft SCIM Validator](https://scimvalidator.microsoft.com/)
（只选 Users）或 Okta SCIM 测试集，配合 IAM 的 `iam-scim-outbound` 令牌验证
`/scim/v2/Users`。端到端链路见 Explore IAM 的 E8 SCIM 开通。

## 参考

- [RFC 7643 SCIM Core Schema](https://datatracker.ietf.org/doc/html/rfc7643)
- [RFC 7644 SCIM Protocol](https://datatracker.ietf.org/doc/html/rfc7644)
- [GDPR Art. 17 Right to erasure](https://gdpr-info.eu/art-17-gdpr/)
