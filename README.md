# airport-transfer

Airport transfer booking across Tunisia (TND / EUR / USD). See CLAUDE.md for conventions and roadmap.

```bash
docker compose up -d
cd backend && mvn spring-boot:run      # http://localhost:8080  (Swagger: /swagger-ui.html)
cd frontend && npm install && npm start # http://localhost:4200
```

## Authentication (step 2)

Stateless JWT (HS256, `spring-boot-starter-oauth2-resource-server`), roles `CUSTOMER` / `DRIVER` / `ADMIN` in the `role` claim.

| Endpoint | Access | Purpose |
|---|---|---|
| `POST /api/auth/register` | public | Create a CUSTOMER account, returns a token |
| `POST /api/auth/login` | public | Returns `{token, tokenType, expiresAt, user}` |
| `GET /api/auth/me` | any signed-in user | Current user |
| `POST /api/admin/users` | ADMIN | Create a DRIVER, ADMIN or CUSTOMER account |

`/api/admin/**` requires ADMIN, `/api/driver/**` requires DRIVER; 401 and 403 use the standard `{code, message, fieldErrors}` format (`UNAUTHORIZED`, `FORBIDDEN`). Login failures always answer `INVALID_CREDENTIALS`.

Environment variables:

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | dev-only value | 32+ characters. The `prod` profile refuses to start with the dev default |
| `JWT_EXPIRATION_MINUTES` | `120` | Token lifetime |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | empty | Creates the first admin at startup if that e-mail does not exist (password 8+ characters). An existing account is never modified |

Frontend: `/login`, `/register`, `/account` (guarded). The token is stored in `localStorage`; the interceptor adds it to `/api/**` calls and logs out on a 401.
