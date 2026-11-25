# airport-transfer

HMS Shuttle: airport transfer booking in France and Tunisia (TND / EUR / USD). See CLAUDE.md for conventions and roadmap.

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
cd backend && mvn test     # unit tests
cd backend && mvn verify   # + integration tests (*IT); needs Docker, or set TEST_DB_URL / TEST_DB_USER / TEST_DB_PASSWORD
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

Environment variable: `MAIL_FROM` (default `noreply@hms-shuttle.example`), next to `MAIL_HOST` / `MAIL_PORT`.

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

## Customer experience (step 8)

- Home page with a booking card: the price preview comes from `GET /api/public/booking-quote` (smallest vehicle that carries the group); the card opens `/book` pre-filled.
- A customer can edit address, flight, pickup time, luggage and notes of a booking while it is PENDING or CONFIRMED (`PUT /api/bookings/{id}`). The price never changes: to change the vehicle or destination, cancel and book again.
- Profile and password change on `/account`. Forgot password: `/forgot-password` sends a one-hour, single-use link to `PUBLIC_URL/reset-password?token=...` (read it in MailHog at http://localhost:8025 in development).
- Receipt: open a booking (`/bookings/:id`) and use "Imprimer le reçu".

New environment variable: `PUBLIC_URL` (default `http://localhost:4200`). New migration: `V9__password_reset.sql`.

## Payment (step 9, cash)

- Every booking has a payment status: UNPAID, PAID or REFUNDED. Cash is recorded by the driver (or by staff) once the trip has started; the amount is always the price snapshot of the booking.
- Card payments (Stripe) are not available yet: the `PaymentGateway` interface and the payment ledger are in place, see CLAUDE.md for what to add when a Stripe account exists.
- Migration `V10__payments.sql`; no new environment variable.

## Quality and deployment (step 10)

- `mvn verify` runs unit tests and the `*IT` integration tests (Failsafe). Without Docker, point `TEST_DB_URL` at an empty disposable PostgreSQL.
- API errors are translated (fr, en, ar via `Accept-Language`); the `code` field is stable.
- Rate limits (in memory, per instance): login is locked for 15 minutes after 10 failures for an e-mail; forgot-password allows 5 per e-mail and 30 per IP per hour.

### Deploying with Docker

```bash
cp .env.example .env        # fill in DB_PASSWORD, JWT_SECRET (32+ chars), PUBLIC_URL, MAIL_*
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

- `caddy` terminates HTTPS (automatic Let's Encrypt certificate for `DOMAIN`; the DNS record must already point to the server and ports 80 and 443 must be open). Behind it, `web` (nginx) serves the Angular app and proxies `/api` to the backend. `WEB_PORT` is no longer used.
- The backend runs with the `prod` profile: Swagger is disabled, and it refuses to start with the dev `JWT_SECRET`.
- To create the first admin, set `SEED_ADMIN_EMAIL` and `SEED_ADMIN_PASSWORD` for the first start, then remove them.
- **Before going live**, replace the demo tariffs and exchange rates in `/admin` (the backend logs a WARN while demo data remains).
- Back up the database with `./scripts/backup.sh` (daily cron, see the script). Flyway migrates the schema on startup.
- Update: `git pull && docker compose -f docker-compose.prod.yml --env-file .env up -d --build`.

## France (step 11b)

Paris airports (ORY, CDG) are sold with flat EUR prices per trip: EUR 150 Eco, EUR 160 Eco Plus (migration `V13__seed_france.sql`). Tariffs are stored in their own currency, pickup times use the airport's time zone. Apply the migrations by starting the backend.


## Pack voyage (step 11c)
`POST /api/bookings/pack` books several linked trips (France and Tunisia) at once; `POST /api/public/pack-quote` previews the total. UI: `/pack`.


## SMS reminders (step 11d)
Off by default. `SMS_ENABLED=true` starts the job that texts customers 24 h and 1 h before pickup. Until a provider adapter exists (`SMS_PROVIDER`), messages are only logged.


## Client feedback round (step 11e)
Currency order EUR/USD/TND, public booking flow with sign-up before confirming, redesigned home card, 4-leg round-trip pack, return flight number required on round trips, new vehicle capacities (migration V15).

## Client feedback round 2 (step 11f)

Airports can list terminals (`airports.terminals`, comma-separated, editable in Admin > Catalog); the customer may pick one (stored in `bookings.terminal`, optional, shown to staff and drivers). Seeded: CDG Terminal 1-3, ORY Orly 1-4 (check them). The home page has one short tagline, the card toggle reads "Aller" / "Retour", and `/book` uses the same Départ / Destination layout. The "Destinations" nav link is gone. Migration `V16`.
