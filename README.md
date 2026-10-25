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

## Catalog and pricing API (step 3)

- Public (no token): `GET /api/public/airports`, `/zones` (with their areas), `/vehicle-types`, `/currencies`, and
  `GET /api/public/quote?airportId=&zoneId=&vehicleTypeId=&currency=TND|EUR|USD`.
- Admin (role ADMIN): CRUD under `/api/admin/` for `airports`, `zones`, `areas`, `vehicle-types`, `tariffs`;
  `currencies` (list, create, update); `exchange-rates` (list, create: a new rate is a new row, the latest applies).
- Errors always have the shape `{ "code": "...", "message": "...", "fieldErrors": { "field": "Constraint" } }`.

### Demo data

The tariffs and exchange rates seeded by `V6__seed_demo_pricing.sql` are **placeholders, not real prices**
(`is_demo = true`; quotes carry `"demo": true`). Replace them with the real price grid before going live.
Exchange rates are expressed as TND per 1 unit (EUR 3.4 means 1 EUR = 3.4 TND).

```bash
cd backend && mvn test   # integration tests (*IT) need Docker for Testcontainers
```

## Bookings (step 4)

| Endpoint | Access | Purpose |
|---|---|---|
| `GET /api/public/booking-quote` | public | Price preview: `leg`, `legs`, `totalAmount` (`roundTrip=true` means two legs) |
| `POST /api/bookings` | signed-in | Create a booking, or two linked bookings when `returnPickupAt` is sent |
| `GET /api/bookings` | signed-in | My bookings, latest pickup first |
| `GET /api/bookings/{id}` | owner | One booking (404 for anyone else) |
| `POST /api/bookings/{id}/cancel` | owner | Cancel (only before IN_PROGRESS; otherwise 409 `INVALID_STATUS_TRANSITION`) |

Each booking stores its own price snapshot (TND price, currency, exchange rate, charged amount); later tariff or rate changes never alter it. Pickup times are stored in UTC and shown in Africa/Tunis. A confirmation e-mail is sent after the booking is committed (MailHog UI: http://localhost:8025); a mail failure never fails the booking.

Environment variable: `MAIL_FROM` (default `noreply@marhba.tn`), next to `MAIL_HOST` / `MAIL_PORT`.

Frontend tests: `cd frontend && npm install && npm test` (Karma + Jasmine, needs Chrome).
