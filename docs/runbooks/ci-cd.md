# CI/CD Runbook

This document describes the current behavior of:

- `.github/workflows/backend.yml`
- `.github/workflows/frontend.yml`

## What Runs and When

Both workflows are triggered by:

- `pull_request` to `main`
- `push` to `main`
- `workflow_dispatch`

There is no `paths` filter in either workflow. Any PR to `main` can trigger both workflows.

## Backend Workflow (`backend.yml`)

Jobs:

1. `Test`
   - Runs on PR and push.
   - Runs `./mvnw test` in `backend/`.
   - Uploads Surefire reports.

2. `Build and Deploy`
   - Runs only on `push` to `main`.
   - Condition: `github.ref == 'refs/heads/main' && github.event_name == 'push'`.
   - Builds image, pushes to ECR, deploys to EC2, checks `http://localhost:8080/api/health`.

## Frontend Workflow (`frontend.yml`)

Job:

1. `Build and Deploy`
   - Always runs install, lint, test, build on PR and push.
   - Deploy steps (AWS auth, S3 sync, CloudFront invalidation) run only on `push` to `main`.

Result:

- PR: CI checks only, no deployment.
- Merge to `main`: CI runs again on the merge commit, then deployment runs.

## Branch Protection (Recommended)

For PR merge gates, require:

- Backend `Test`
- Frontend `Build and Deploy`

Do not require backend `Build and Deploy` as a PR check, because it is main-push only.

## Typical Flow

1. Open PR to `main`.
2. Wait for required checks:
   - Backend `Test`
   - Frontend `Build and Deploy`
3. Merge PR.
4. On `push main`, deployment jobs execute automatically.

## Manual Trigger

Use `workflow_dispatch` when needed:

1. Open GitHub `Actions`.
2. Select workflow.
3. Click `Run workflow`.
4. Choose branch and run.

## Secrets and Variables

Secrets:

- `AWS_ROLE_TO_ASSUME`
- `EC2_SSH_KEY` (backend deploy)

Repository Variables:

- `AWS_REGION`
- `ECR_REPOSITORY`
- `EC2_HOST`
- `EC2_USER` (optional, default `ec2-user`)
- `FRONTEND_BUCKET`
- `CF_DIST_ID`
- `VITE_API_BASE_URL`
- `VITE_COGNITO_DOMAIN`
- `VITE_COGNITO_CLIENT_ID`
- `VITE_COGNITO_REDIRECT_URI`
- `VITE_COGNITO_LOGOUT_URI`
- `VITE_COGNITO_SCOPE` (optional)

## Troubleshooting

### PR shows "Expected - Waiting for status to be reported"

Checks:

1. Branch protection check names exactly match workflow job names.
2. Required checks come from active workflows in this repository.
3. Push a new commit after workflow or branch-protection changes.

Quick retrigger:

```bash
git commit --allow-empty -m "chore: retrigger ci"
git push
```

### Backend deployment fails

On EC2:

```bash
docker ps -a
docker logs jira-backend --tail 200
cat /home/ec2-user/.env
curl -f http://localhost:8080/api/health
```

### Frontend deploy succeeds but old UI still appears

Check CloudFront invalidation:

```bash
aws cloudfront list-invalidations --distribution-id <CF_DIST_ID>
```

### OIDC auth errors in GitHub Actions

Validate role trust and permissions:

```bash
aws iam get-role --role-name <github-actions-role>
aws iam list-attached-role-policies --role-name <github-actions-role>
```

## Local Commands to Match CI

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm run test
npm run build
```

Backend:

```bash
cd backend
./mvnw test
./mvnw clean package -DskipTests
```
