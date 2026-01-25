# AGENTS.md

## 1) 项目目标与交付标准

**项目：** Jira Lite（多租户工单系统），用于澳洲 junior 全栈/后端岗位简历展示。  
**技术栈固定：**

- Backend: Java 17, Spring Boot 3, Spring Security, JPA/Hibernate, Flyway, OpenAPI
- Frontend: React + TypeScript + React Router + React Query + MUI
- AWS: Cognito, S3 (pre-signed), RDS Postgres, ECS Fargate, ALB, CloudWatch, ECR, CloudFront+S3
- IaC: Terraform
- CI/CD: GitHub Actions

**最终交付必须包含：**

- 公网可访问 Demo（前端+后端）
- IaC 可复现部署
- CI/CD 自动构建/测试/部署
- README + 架构图 + Runbook + Work logs

---

## 2) 输出风格要求（回答时必须遵守）

任何方案都要分步骤，并给出：

✅ 要做什么  
✅ 怎么做  
✅ 验收标准（Definition of Done）  
✅ 工作留痕要写什么（Docs/PR/Issue）

默认流程：Issues → Branch → PR → Review → Merge → Release。

优先给具体清单/模板，避免抽象描述。  
默认用中文解释，关键术语保留英文。  
关键核心设计都给 Mermaid 图。  
所有代码注释用英文。

---

## 3) 企业级“工作留痕”规范（强制）

每个功能/模块必须产生以下留痕：

### ✅ GitHub Issue（模板）

```
## Scope
- …

## Acceptance Criteria
- [ ] …

## Out of Scope
- …

## Testing Notes
- …
```

### ✅ PR 描述（模板）

```
## What
## Why
## How
## Testing
## Risks
```

### ✅ Docs 更新（按需）

- `docs/design/*`（契约/设计）
- `docs/adr/*`（关键决策）
- `docs/runbooks/*`（部署/排障）

## 4) 代码与架构约束（Backend）

分层固定：

- controller（仅处理 HTTP + DTO）
- service（业务逻辑）
- repository（DB）
- dto（request/response）
- mapper（可选）

### ✅ 多租户隔离（强制）

- 每个表有 org_id（或可推导的 org_id）
- 所有查询必须校验 org membership
- **org_id 必须从 JWT claims 获取，禁止前端传 org_id**

### ✅ RBAC 最小实现

- ADMIN, MEMBER
- ADMIN：管理成员、创建项目
- MEMBER：创建/处理工单

### ✅ 统一错误格式

```
{ "code": "...", "message": "...", "traceId": "..." }
```

### ✅ 必须有健康检查

- GET `/health`

### ✅ 必须有 DB migration（Flyway）

禁止手工改库不留痕。

---

## 5) 代码与架构约束（Frontend）

页面最小集合：

- Login（先占位，后接 Cognito）
- Projects List
- Tickets List（分页/筛选/排序）
- Ticket Detail（评论、附件）
- Ticket Create/Edit

数据层用 React Query，避免手写复杂缓存。  
UI 以可用为主，样式整洁一致。

---

## 6) 测试要求（最小但硬）

每个模块至少满足之一：

- Service 单测（JUnit + Mockito）
- 或 Testcontainers 集成测（推荐至少 1–2 个关键路径）

CI 必须跑：

- backend: `mvn test`
- frontend: `npm test`（如无测试，至少 `npm run build`）

PR 必须提供“测试证据”。

---

## 7) AWS 与安全要求

- 禁止写入 secrets（repo、日志、prompt）
- S3 pre-signed：短过期 + 限制 content-type（可行则必须）
- ECS/RDS 联通问题优先排查：CloudWatch logs / 安全组 / 子网 / 路由
- IaC（Terraform）为主，尽量模块化，至少覆盖核心资源

---

## 8) 默认工作节奏（每天 3 小时）

每次问“下一步做什么”，必须给出：

- 今日目标（1 句话）
- Tasks（按顺序、可执行）
- Commands（需要跑哪些命令）
- 验收标准（DoD）
- 留痕清单（Issue/PR/Docs/Prompt log）

---

## 9) API 一致性规范（补充）

### ✅ 分页/排序规范

```
请求参数：page(0-based), size, sort=field,asc|desc
响应结构：
{
  content: [],
  page: { number, size, totalElements, totalPages }
}
```

### ✅ 统一日志字段

每个请求 log 必须包含：`traceId`, `org_id`, `user_id`

---

## 10) Mermaid 规范

每个设计至少包含：

- C4 Context（系统与外部依赖）
- Sequence（关键流程）

命名风格：`lowercase-with-dash`  
Mermaid 代码放在 docs/design/\* 内

---
