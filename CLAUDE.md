# HMS Transport — Airport transfer booking in France and Tunisia

Brand: **HMS Transport** (renamed from the working name "Marhba" in step 11a; internal identifiers such as the JWT issuer and the `marhba.auth` storage key keep the old name on purpose). Web app to book a private transfer from or to a Tunisian airport (and, from step 11b, Paris airports), in TND, EUR or USD.

The owner (Hichem) is an experienced Java Spring / Angular developer and writes in French. **Talk to him in French. Write code, comments, identifiers and commit messages in English.**

## How to work (autonomy rules)
- Work through the roadmap below one step at a time, in order. Do not wait for approval between small decisions: pick the simplest option that fits the conventions, note it in the final report, move on.
- Ask a question only if you are truly blocked or a choice is expensive to undo (money rules, auth model, data deletion).
- After each step: run `mvn test` in `backend/` and `npm run build` in `frontend/`, fix failures, then commit (Conventional Commits, one branch per feature, e.g. `feat/data-model`).
- End each step with a short French report: what was done, how to try it, what is left, any assumption made.
- Never commit secrets. Configuration comes from environment variables with dev-only defaults in `application.yml`.
- Do not add features that are not in this file without asking. Keep V1 small and solid.

## Stack
- **Backend**: Spring Boot 3.3, Java 21, Maven, PostgreSQL 16, Flyway, Spring Data JPA, Spring Security (stateless JWT), springdoc OpenAPI, MapStruct. Root package `com.airporttransfer`.
- **Frontend**: Angular 17+ (standalone components, signals, new control flow `@if`/`@for`, lazy-loaded routes), Tailwind CSS, ngx-translate.
- **Local infra**: `docker compose up -d` (PostgreSQL + MailHog). Frontend dev server proxies `/api` to `http://localhost:8080`.
- **Tests**: JUnit 5 + Testcontainers on the backend (`*IT` classes). Frontend: add unit tests for services with money or currency logic.

## Current state
- Step 1 done: monorepo skeleton, `GET /api/public/ping`, CORS, Swagger.
- Step 2 done: JWT auth (`/api/auth/register|login|me`), roles CUSTOMER / DRIVER / ADMIN, `users` table (`V2__users.sql`), admin seeding via `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD`, Angular login / register / account pages, auth interceptor and guard.
- Public sign-up only creates CUSTOMER. DRIVER and ADMIN accounts are created by an admin. Admins create them with `POST /api/admin/users`. Token lifetime: `JWT_EXPIRATION_MINUTES` (default 120). `/api/driver/**` requires DRIVER.
- Step 3 done (branch `feat/data-model`): migrations `V3__catalog.sql`, `V4__pricing.sql`, `V5__seed_catalog.sql`, `V6__seed_demo_pricing.sql` (`V2` is kept for `users`). Packages `catalog` (airports, zones, areas, vehicle types) and `pricing` (currencies, exchange rates, tariffs, quote), `common` (`ApiException`, `GlobalExceptionHandler`, error format `{code, message, fieldErrors}`).
  - Zones: 15 pricing zones; an *area* is a governorate (Médenine is split into Djerba / mainland) that belongs to exactly one zone. Admins regroup by changing an area's zone (`PUT /api/admin/areas/{id}`).
  - Exchange rates are stored as **TND per 1 unit** of the currency, as an append-only history; the latest row applies. Quote = TND price / rate, rounded HALF_UP to the currency's decimals.
  - **Seeded tariffs and exchange rates are DEMO data** (`is_demo = true`, flagged in `V6`, `"demo": true` in quotes, WARN at startup in the `prod` profile). Replace them with the real price grid before going live; tariffs saved through the admin API are real (`is_demo = false`).
  - Endpoints: public `GET /api/public/{airports,zones,vehicle-types,currencies}` and `GET /api/public/quote?airportId&zoneId&vehicleTypeId&currency`; admin (ROLE_ADMIN, enforced in `SecurityConfig`) under `/api/admin/{airports,zones,areas,vehicle-types,tariffs,currencies,exchange-rates}`.
- Step 4 done (branch `feat/bookings`): migration `V7__bookings.sql`; package `booking` (entity, repository, `BookingService`, `BookingPricingService`, `BookingStatusService`, `ReferenceGenerator`, confirmation mail). New dependency: `spring-boot-starter-mail`; texts in `messages*.properties` (fr default, en, ar chosen from `Accept-Language`); new env variable `MAIL_FROM` (default `noreply@marhba.tn`).
  - Endpoints: `POST /api/bookings` (one way, or round trip when `returnPickupAt` is set: two bookings, opposite directions, shared `groupId`), `GET /api/bookings` (mine, latest pickup first), `GET /api/bookings/{id}`, `POST /api/bookings/{id}/cancel`; public `GET /api/public/booking-quote?airportId&zoneId&vehicleTypeId&currency&roundTrip` (per-leg price and total, same calculation as the booking, so preview = charge).
  - Price snapshot per booking: `price_tnd`, `currency`, `exchange_rate` (null for TND), `amount`, `price_is_demo`. A customer sees only their own bookings (someone else's id answers 404 `BOOKING_NOT_FOUND`).
  - Rules: pickup must be in the future (`PICKUP_IN_PAST`); return after the first trip (`RETURN_BEFORE_OUTBOUND`); passengers up to the vehicle's maximum (a bigger vehicle may carry fewer people, `VEHICLE_CAPACITY_EXCEEDED`); only CASH is accepted until step 5 (`PAYMENT_METHOD_NOT_AVAILABLE`, see `BookingService.ENABLED_PAYMENT_METHODS`). Status transitions live only in `BookingStatusService`; new bookings start PENDING.
  - Frontend: `/book` (guarded; 3 steps in `features/booking`, state in `BookingDraftService`) and `/bookings` (guarded). The customer picks a governorate (area); its zone is derived. Dates are entered in Tunis time and sent as UTC (Tunisia is UTC+1 all year). Prices are fetched from the API in the chosen currency; the browser only formats them. Unit tests: `ng test` (Karma/Jasmine added in `package.json`, run `npm install` to refresh the lockfile).
  - Not in V1 step 4 (left for later steps): card payment (5), admin/driver status changes (6), the home page booking card (the home page only has a "Réserver" button for now).

- Step 5 done (branch `feat/operations`): migration `V8__operations.sql` (MODERATOR role, `booking_status_history`, `bookings.customer_language`, `bookings.version` for optimistic locking; existing bookings get a creation history row). No new environment variable.
  - Staff API (MODERATOR and ADMIN, `/api/staff/**`): `GET /bookings` (filters `status, from, to, airportId, driverId, unassigned, q`; `page, size` (max 100), `order=asc|desc` on pickup time; `q` searches reference and customer name, e-mail or phone), `GET /bookings/{id}` (with history), `GET /drivers`, `POST /bookings/{id}/confirm|assign|start|complete|cancel`. Assign body `{driverId, force}`: refused with 409 `DRIVER_UNAVAILABLE` when the driver has another ASSIGNED or IN_PROGRESS trip within 2 hours of the pickup, unless `force`; only CONFIRMED or ASSIGNED bookings can be assigned (`BOOKING_NOT_ASSIGNABLE`); reassigning keeps ASSIGNED and logs the change. Cancel body `{reason}` is optional.
  - Driver API (`/api/driver/trips`, ROLE_DRIVER only): `GET ?scope=upcoming|past`, `GET /{id}`, `POST /{id}/start`, `POST /{id}/complete`. A trip of another driver answers 404 `TRIP_NOT_FOUND`. Drivers see the customer's name and phone and the amount to collect.
  - Every status change goes through `BookingWorkflowService` (rules in `BookingStatusService`, then history row, then event). E-mails after commit: customer on CONFIRMED, COMPLETED, driver assigned, and cancellation by staff (in the booking's `customer_language`); driver (in French) on assignment and cancellation. Two people acting on the same booking at once: the second gets 409 `CONCURRENT_UPDATE`.
  - Roles: MODERATOR manages bookings and drivers' assignments only; `/api/admin/**` stays ADMIN-only. Accounts are still created by an admin (`POST /api/admin/users`, role MODERATOR now accepted).

- Step 6 done (branch `feat/staff-ui`, frontend only): `/staff/bookings` (filters, search, pagination), `/staff/bookings/:id` (detail, history, confirm, assign or reassign with the "assign anyway" prompt on a 2-hour conflict, start, complete, cancel with reason) for MODERATOR and ADMIN; `/driver` (upcoming and past trips, start, complete, call the customer) for DRIVER. `roleGuard(...roles)` in `core/auth.guard.ts` (the server enforces the same rules); after signing in each role lands on its own page (`homeFor`). The header shows "Gestion" or "Mes courses" by role. New shared `StatusBadgeComponent`. Staff booking creation on behalf of a customer (phone orders) is not done yet; it is proposed as a small follow-up after step 8.

- Step 7 done (branch `feat/admin`): no migration, no new environment variable. Backend package `admin`.
  - `GET /api/admin/users` (filters `q, role, enabled`; `page, size` max 100), `GET /api/admin/users/{id}`, `PUT /api/admin/users/{id}` (`fullName, phone, role, enabled`; the e-mail is not editable), `POST /api/admin/users` as before. `UserDto` now carries `enabled`. Rules: an admin cannot demote or disable their own account (409 `CANNOT_MODIFY_OWN_ACCESS`); a driver with an ASSIGNED or IN_PROGRESS trip cannot be disabled or moved to another role (409 `DRIVER_HAS_ACTIVE_TRIPS`). A disabled account cannot sign in; a token already issued stays valid until it expires (`JWT_EXPIRATION_MINUTES`), and a role change applies at the next sign-in.
  - `GET /api/admin/stats?from&to` (UTC instants, pickup time in [from, to); default 30 days back to 30 days ahead; max 366 days, else 400 `INVALID_RANGE`): bookings per status, non-cancelled count, revenue per currency (charged amount and base TND, never mixed across currencies), bookings per airport.
  - Frontend `/admin` (ADMIN only, lazy children): `stats`, `users`, `tariffs` (grid per airport: zone x vehicle, saving a demo tariff makes it real), `catalog` (airports, zones, governorates, vehicles; items are deactivated, never deleted from the UI), `currencies` (currencies and append-only rate history). Header link "Administration" for ADMIN.

- Step 8 done (branch `feat/customer`): migration `V9__password_reset.sql`. New environment variable `PUBLIC_URL` (default `http://localhost:4200`): base URL of the web app, used in the e-mailed reset link.
  - `PUT /api/bookings/{id}` (`address, flightNumber, pickupAt, luggageCount, notes`): only while PENDING or CONFIRMED (409 `BOOKING_NOT_EDITABLE` once a driver is assigned); pickup must be in the future; in a round trip the outbound leg (created first) stays before the return leg (`RETURN_BEFORE_OUTBOUND`). Vehicle, destination, passengers and the price snapshot never change. A history row notes the edit. `BookingDto` has a new `editable` flag.
  - `PUT /api/auth/me` (`fullName, phone`), `POST /api/auth/change-password` (wrong current password: 400 `CURRENT_PASSWORD_INVALID`, never 401, so the client does not log out).
  - Forgot password: `POST /api/auth/forgot-password` always answers 204 (no account enumeration; nothing is sent for unknown or disabled accounts), `POST /api/auth/reset-password` `{token, newPassword}`. Tokens are 256-bit random, stored as SHA-256 hash, valid 1 hour, single use; asking again invalidates earlier links; any failure is 400 `RESET_TOKEN_INVALID`. Existing JWTs stay valid after a password change until they expire. No rate limiting yet (planned with step 10).
  - Frontend: home page as designed (hero, floating booking card with live price from `booking-quote`, vehicle cards, airport chips, how it works, footer); the card hands its choices to `/book` through query parameters (`features/booking/prefill.ts`; malformed values ignored, a saved draft wins); `/bookings/:id` (details, edit, cancel, printable receipt via the browser print dialog, header hidden in print); `/forgot-password`, `/reset-password?token=`; account page with profile and password change; "Mot de passe oublié ?" link on the login page.

- Step 9 done for cash (branch `feat/payment`): migration `V10__payments.sql` (`bookings.payment_status` UNPAID / PAID / REFUNDED, `paid_at`, table `payments`, unique index: one successful payment per booking). No new environment variable. Package `payment`: `PaymentGateway` interface (`method()`, `provider()`, `supports(currency)`, `refund(payment)`), `CashGateway`, `PaymentService` (gateways are looked up by payment method).
  - Cash: the driver records it with `POST /api/driver/trips/{id}/cash-collected`, staff with `POST /api/staff/bookings/{id}/cash-collected`. Allowed only when the trip is IN_PROGRESS or COMPLETED (409 `PAYMENT_NOT_COLLECTABLE`), once (409 `ALREADY_PAID`), on a CASH booking (409 `PAYMENT_METHOD_MISMATCH`). The recorded amount is always the booking's price snapshot (no amount in the request). A history note records who collected it.
  - Cancelling a PAID booking refunds it through its gateway (`BookingWorkflowService`); for cash this is only a manual marker (`external_ref = manual`). `BookingDto` has `paymentStatus`; the admin stats show the collected amount per currency.
  - **Card payments are not done**: `BookingService.ENABLED_PAYMENT_METHODS` still allows only CASH. To add Stripe once an account exists: add `stripe-java`, implement a `StripeGateway` (EUR and USD) that creates a PaymentIntent, add a webhook endpoint (public path, signature verified) that calls `Booking.markPaid` through `PaymentService`, then enable CARD. New variables then: `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`.
  - Frontend: payment status on the customer detail and receipt, staff detail, driver trips; "Espèces encaissées" button for the driver and for staff; "Encaissé" column in the admin stats.

- Step 10 done (branch `chore/quality-deploy`): no migration. `mvn verify` runs the `*IT` classes through Failsafe (`AbstractIT`: Testcontainers, or `TEST_DB_URL`). API error messages are translated (fr default, en, ar from `Accept-Language`) through `error.<CODE>` keys in `messages*.properties`, including 401/403 and validation errors; the `code` stays stable. Rate limiting (in memory, per instance, `common/RateLimiter`): login locks an e-mail for 15 minutes after 10 failed attempts (429 `TOO_MANY_REQUESTS`; this can be used to lock someone out for 15 minutes, accepted trade-off), forgot-password is limited to 5 per e-mail and 30 per IP per hour. Accessibility: skip link, `<main id="main">` focused after each navigation, translated aria-labels (`a11y.*`). Deployment: `backend/Dockerfile`, `frontend/Dockerfile` + `nginx.conf` (serves the SPA, proxies `/api` to `backend`), `docker-compose.prod.yml`, `.env.example`, `application-prod.yml` (Swagger off, forwarded headers). New environment variables: `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS`, `WEB_PORT`, `DB_NAME` (compose only).

- Step 11a done (branch `feat/rebrand`): application renamed HMS Transport (UI, e-mails in fr/en/ar, page title, inline SVG favicon), new logo, booking references now start with `HMS-` (existing `MH-` references stay valid). Default `MAIL_FROM` is now `noreply@hms-transport.example`: set the real address in production.

- Step 11b done (branch `feat/france`): migrations `V12__france.sql` (structure) and `V13__seed_france.sql` (data). Airports and zones have a `country` (TN, FR); an airport has a `time_zone` (Africa/Tunis, Europe/Paris): pickup times are typed and shown in the airport's zone (`core/zoned-time.ts`, DST-safe; `tunis-time.ts` is only a Tunis shortcut for admin screens), mails use `airport.timeZone`. Seeded: airports ORY and CDG, zone `FR01` "Paris" (one area, `FR-PARIS`; add areas in the admin catalog to extend it), vehicle types ECO (Seat Ibiza, Toyota Yaris, Renault Clio) and ECO_PLUS (Toyota C-HR, Corolla), both 1-3 passengers (assumption, editable), `vehicle_types.models` (free text, same in every language).
  - **Money rule change**: a tariff is stored in the currency it is sold in (`tariffs.price` + `tariffs.currency`, was `price_tnd`). Paris tariffs are real (`is_demo = false`): EUR 150 Eco, EUR 160 Eco Plus, per trip, same for ORY and CDG. A quote in the tariff's own currency is the tariff amount itself (EUR 150.00 stays exact whatever the rate); in another currency it goes through TND (`price_tnd` = price x rate of the tariff currency, then / rate of the target). A tariff in EUR therefore needs an EUR exchange rate even when charged in EUR (`price_tnd` and `exchange_rate` of the snapshot stay filled). The admin API uses `price` + `currency` (`TariffRequest`), airports `country` + `timeZone`, zones `country`, vehicle types `models`.
  - Frontend: destinations are filtered by the airport's country; only vehicles with a tariff on the route are listed; the home card prices the smallest vehicle sold on the route; the admin tariff grid shows the zones of the airport's country in EUR for France.

- Step 11c done (branch `feat/pack`): no migration, no new environment variable. A **pack** books 2 to 6 linked one-way trips (for instance Paris to ORY, then TUN to the hotel, and back) in one call: `POST /api/bookings/pack` `{legs[], currency, paymentMethod}`; each leg has its own airport, direction, zone, address, flight, pickup, passengers, luggage and vehicle. Same rules as a normal booking per leg (capacity, active catalog, CASH only); the first pickup must be in the future (`PICKUP_IN_PAST`) and the legs strictly chronological (`LEGS_OUT_OF_ORDER`). All legs share one `groupId` and one currency, each keeps its own price snapshot. Public `POST /api/public/pack-quote` `{currency, legs[airportId, zoneId, vehicleTypeId]}` returns each leg and `totalAmount` = sum of the rounded leg amounts (preview = charge). Frontend `/pack` (guarded, `features/pack`), nav link and a link under the home booking card. Vehicles are not filtered per country in the pack form: a missing tariff shows the quote error.

- Step 11d done (branch `feat/sms-reminders`): migration `V14__sms_reminders.sql` (`bookings.reminder_24h_sent_at`, `reminder_1h_sent_at`). New package `notification`: `SmsGateway` (`send(phone, text)`), `LoggingSmsGateway` (default: logs a masked number and the text, sends nothing), `SmsReminderService`, `SmsReminderJob` (`@Scheduled`, only when `SMS_ENABLED=true`). Reminders go to CONFIRMED or ASSIGNED bookings, to the customer's phone, in the booking's `customer_language`, with the pickup time in the airport's time zone (texts `sms.reminder.24h|1h` in `messages*.properties`). The 24 h reminder is sent when the pickup is between 2 h and 24 h away, the 1 h reminder when it is under 1 h away; each is marked and sent once; a failed send is retried at the next run; customers without a phone are skipped. New variables: `SMS_ENABLED` (default false), `SMS_PROVIDER` (default `log`), `SMS_POLL_INTERVAL_MS` (default 60000). **No SMS provider yet**: to add one (Twilio, Brevo...), implement `SmsGateway` as a bean enabled by `app.sms.provider=<name>`, add its credentials as environment variables, then set `SMS_ENABLED=true` and `SMS_PROVIDER`. Single backend instance assumed (no distributed lock).

## Product rules
- **Coverage**: all of Tunisia, plus Paris (Orly ORY, Charles de Gaulle CDG, from step 11b). Tunisian airports: Tunis-Carthage (TUN), Monastir (MIR), Enfidha-Hammamet (NBE), Djerba-Zarzis (DJE), Sfax (SFA), Tozeur (TOE), Tabarka (TBJ), Gafsa (GAF). Airports are data, not code: admins can add more.
- **Directions**: airport → address, address → airport, or round trip (two bookings linked by a shared group id).
- **Pricing V1**: flat rate per (airport, destination zone, vehicle type). Zones are groups of cities or governorates managed by admin. No live distance calculation in V1.
- **Vehicles**: Sedan (1–3 passengers), Van (4–7), Minibus (8–16). Capacities are configurable data.
- **Currencies**: TND, EUR, USD. **TND is the base currency** (exchange rates are TND per unit); each tariff is stored in the currency it is sold in (TND for Tunisia, EUR for France).
  - Exchange rates (TND per 1 EUR, TND per 1 USD) are managed by admin and stored with a timestamp.
  - A booking stores a snapshot: base price in TND, the currency chosen, the rate used, and the final charged amount. Later rate changes never alter an existing booking.
  - Use `BigDecimal` everywhere for money, never `double`. TND has 3 decimals (millimes), EUR and USD have 2. Round `HALF_UP` only when converting for display or charging.
- **Booking fields**: reference (human-readable, unique), customer, airport, direction, destination zone, address, flight number (optional), pickup date and time (stored as UTC, shown in the time zone of the airport: Africa/Tunis or Europe/Paris), passengers, luggage count, vehicle type, notes, price snapshot, status, payment method, assigned driver (optional).
- **Roles**: CUSTOMER, DRIVER, MODERATOR (manages bookings and drivers' assignments, no access to users, tariffs or catalog), ADMIN (everything).
- **Statuses**: PENDING → CONFIRMED → ASSIGNED → IN_PROGRESS → COMPLETED, and CANCELLED from any state before IN_PROGRESS. Enforce transitions in one place (a service), not in controllers.
- **Payment**: cash to the driver, or card online. Put every provider behind a `PaymentGateway` interface: Stripe for EUR and USD cards, and an optional local TND gateway (Konnect or Flouci) added later. A webhook marks the booking paid; never trust the browser for payment status.
- **Notifications**: confirmation e-mail on booking (MailHog in dev). WhatsApp is out of scope for V1.
- **Guests**: booking requires an account in V1 (keep it simple).

## Roadmap
1. ~~Monorepo skeleton~~ done
2. ~~Authentication (JWT, roles)~~ done
3. ~~Data model~~ done
4. ~~Booking flow~~ done
5. ~~Booking operations (backend)~~ done
6. ~~Staff and driver web apps~~ done
7. ~~Administration~~ done: user management (list, search, create, disable, change role); screens for tariffs, zones and areas, airports, vehicle types, currencies and exchange rates (replacing the demo prices); statistics (bookings and revenue per currency).
8. ~~Customer experience~~ done:  home page as designed (floating booking card with live price preview, vehicle cards, airport chips, how it works, footer); booking detail; edit address, flight, notes, luggage and pickup time while PENDING or CONFIRMED (price never changes: to change the vehicle or destination, cancel and rebook); profile (name, phone, password); forgot-password e-mail flow; printable receipt.
9. ~~Payment (cash)~~ done, Stripe pending:  `PaymentGateway` abstraction, cash flow, payment status on bookings, Stripe card flow with webhook (when a Stripe account exists), refund on cancel, optional local TND gateway later.
10. ~~Quality and deployment~~ done (branch `chore/quality-deploy`): integration tests runnable with `mvn verify` (Failsafe), consistent API error format, translated validation messages, accessibility pass, Dockerfiles, production `docker-compose`, README deployment section.

Later, not planned: promo codes, driver ratings, WhatsApp, real distance pricing.

## API conventions
- REST under `/api`, JSON, ISO-8601 dates in UTC. Admin endpoints (ADMIN only) under `/api/admin/**`, staff endpoints (MODERATOR and ADMIN) under `/api/staff/**`, driver under `/api/driver/**`, public under `/api/public/**`.
- DTOs are records. Never expose JPA entities. Map with MapStruct.
- Validation with Jakarta annotations; one global `@RestControllerAdvice` returning `{ "code", "message", "fieldErrors" }`. Error `code` is a stable key the frontend translates.
- Database changes only through Flyway (`V<n>__description.sql`). `ddl-auto` stays `validate`. Never edit an applied migration.
- Services hold business rules; controllers stay thin. Authorization with roles at route level plus ownership checks (a customer only sees their own bookings).

## Frontend conventions
- Feature folders under `src/app/features/<feature>`, shared pieces in `shared/`, services and guards in `core/`.
- All user-facing text goes through ngx-translate keys in `src/assets/i18n/{fr,en,ar}.json`. French is the default. Arabic is right-to-left: use logical Tailwind utilities (`ms-`, `me-`, `ps-`, `pe-`, `text-start`) so the layout flips correctly.
- The selected currency lives in `CurrencyService` (signal). Prices come from the API already converted, never recompute money in the browser.
- Mobile-first, fully responsive. Touch targets at least 44px. Visible focus states, labels on every input.
- Reactive forms, typed. No `any` unless unavoidable.

## Design
Modern, clean, trustworthy, Tunisian. **White and Tunisian red.** It must not look like a cheap or generic local app.
- Colors (Tailwind tokens already in `tailwind.config.js`): `brand` #D2001A, `brand-dark` #A10015, `brand-soft` #FFF6F6, `brand-line` #EBCFD2, `ink` #0B1B33, `muted` #4A5B73. White header. **No red banner or stripe at the top of pages.**
- Font: Plus Jakarta Sans (400 / 600 / 800).
- Logo: red rounded square with a white curved flight path ending in an arrow, wordmark "HMS Transport" and tagline "FRANCE · TUNISIE". Component: `shared/logo.component.ts`.
- Home page: hero with headline and a floating booking card (direction tabs, airport, destination, date and time, passengers, price preview), vehicle cards, airport chips band, "how it works" in 3 steps, footer. Header: logo, nav, currency switch (TND / EUR / USD), language switch (FR / AR / EN), login.
- Booking flow on mobile: step indicator, route summary card, selectable vehicle cards, sticky bottom bar with the total and a primary "Continuer" button.

## Commands
```bash
docker compose up -d
cd backend && mvn spring-boot:run          # http://localhost:8080, Swagger at /swagger-ui.html
cd backend && mvn test                     # unit tests only (fast, no Docker)
cd backend && mvn verify                   # unit tests + *IT integration tests (Failsafe); needs Docker, or TEST_DB_URL / TEST_DB_USER / TEST_DB_PASSWORD pointing at an empty disposable PostgreSQL
cd frontend && npm install && npm start    # http://localhost:4200
cd frontend && npm run build
```
Dev admin: start the backend with `SEED_ADMIN_EMAIL=admin@marhba.tn SEED_ADMIN_PASSWORD=change-me-now`. Outside dev, always set `JWT_SECRET` (32+ characters).

## Definition of done (every step)
- Backend tests pass and cover the new rules, especially money, currency snapshot and status transitions.
- Frontend builds with no errors; new screens work in FR, EN and AR (RTL) and on a 390px-wide viewport.
- Migrations apply on an empty database.
- README and this file are updated (mark the step done, note new env variables).
