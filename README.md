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

## Booking operations (step 5)

Staff (MODERATOR or ADMIN) manage every booking under `/api/staff`; drivers work their own trips under `/api/driver/trips`. See CLAUDE.md for the full endpoint list and rules.

Typical flow: a customer books (PENDING) → staff confirm (CONFIRMED, customer e-mailed) → staff assign a driver (ASSIGNED, customer and driver e-mailed) → the driver starts (IN_PROGRESS) and completes (COMPLETED) the trip. Staff can cancel with a reason until the trip starts; every change is kept in the booking's history.

Try it: start the backend with `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD`, sign in as that admin in Swagger (`/api/auth/login`, then "Authorize"), create a moderator and a driver with `POST /api/admin/users` (roles `MODERATOR`, `DRIVER`). The web screens for staff and drivers come in step 6.

## Staff and driver screens (step 6)

- Moderator or admin: `/staff/bookings` (also reached from "Gestion" in the header). Click a booking for its detail: confirm, assign a driver, start, complete, cancel with a reason, and the full history.
- Driver: `/driver` ("Mes courses"): upcoming trips with the customer's phone and the amount to collect, buttons to start and complete.
- Each role is sent to its own page after signing in. Create moderator and driver accounts with `POST /api/admin/users` until the user-management screen of step 7.

## Administration (step 7)

ADMIN only (`/api/admin/**`, screens under `/admin`):

- **Users**: `GET /api/admin/users?q&role&enabled&page&size`, `GET|PUT /api/admin/users/{id}`, `POST /api/admin/users`. An admin cannot demote or disable their own account; a driver with an assigned or in-progress trip cannot be disabled or changed to another role. A disabled account cannot sign in; a token already issued stays valid until it expires, and a role change applies at the next sign-in.
- **Statistics**: `GET /api/admin/stats?from&to` (default: 30 days back to 30 days ahead, 366 days max). Revenue is given per currency and never added across currencies.
- **Tariffs, catalog, currencies**: screens on top of the step 3 endpoints. Saving a tariff in the grid replaces the demo price with a real one. Catalog items are deactivated, not deleted.

No new environment variable and no migration.
