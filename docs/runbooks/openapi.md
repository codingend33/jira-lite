# OpenAPI Runbook

This runbook provides the operational API entry points used by this project.

## Runtime URLs

Local:

- Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

Production (replace domain):

- Swagger UI: `https://<api-or-cloudfront-domain>/api/swagger-ui/index.html`
- OpenAPI JSON: `https://<api-or-cloudfront-domain>/api/v3/api-docs`

## Quick Checks

```bash
curl -X GET http://localhost:8080/api/health
curl -X GET http://localhost:8080/api/projects -H "Authorization: Bearer <JWT>"
```

## Related

- Local startup and verification: `docs/runbooks/local-dev.md`
- Deployment and CI/CD behavior: `docs/runbooks/ci-cd.md`
- AWS environment setup: `docs/runbooks/getting-started-aws.md`
