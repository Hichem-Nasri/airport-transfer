# Marhba — Airport transfer booking across Tunisia

Working name: **Marhba** (provisional, easy to rename). Web app to book a private transfer from or to any Tunisian airport, in TND, EUR or USD.

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

- Step 6 done (branch `feat/staff-ui`, frontend only): `/staff/bookings` (filters, search, pagination), `/staff/bookings/:id` (detail, history, confirm, assign or reassign with the "assign anyway" prompt on a 2-hour conflict, start, complete, cancel with reason) for MODERATOR and ADMIN; `/driver` (upcoming and past trips, start, complete, call the customer) for DRIVER. `roleGuard(...roles)` in `core/auth.guard.ts` (the server enforces the same rules); after signing in each role lands on its own page (`homeFor`). The header shows "Gestion" or "Mes courses" by role. New shared `StatusBadgeComponent`. Staff booking creation on behalf of a customer (phone orders) is planned with step 8, once the password-reset flow exists.

- Step 7 done (branch `feat/admin`): no migration, no new environment variable. Backend package `admin`.
  - `GET /api/admin/users` (filters `q, role, enabled`; `page, size` max 100), `GET /api/admin/users/{id}`, `PUT /api/admin/users/{id}` (`fullName, phone, role, enabled`; the e-mail is not editable), `POST /api/admin/users` as before. `UserDto` now carries `enabled`. Rules: an admin cannot demote or disable their own account (409 `CANNOT_MODIFY_OWN_ACCESS`); a driver with an ASSIGNED or IN_PROGRESS trip cannot be disabled or moved to another role (409 `DRIVER_HAS_ACTIVE_TRIPS`). A disabled account cannot sign in; a token already issued stays valid until it expires (`JWT_EXPIRATION_MINUTES`), and a role change applies at the next sign-in.
  - `GET /api/admin/stats?from&to` (UTC instants, pickup time in [from, to); default 30 days back to 30 days ahead; max 366 days, else 400 `INVALID_RANGE`): bookings per status, non-cancelled count, revenue per currency (charged amount and base TND, never mixed across currencies), bookings per airport.
  - Frontend `/admin` (ADMIN only, lazy children): `stats`, `users`, `tariffs` (grid per airport: zone x vehicle, saving a demo tariff makes it real), `catalog` (airports, zones, governorates, vehicles; items are deactivated, never deleted from the UI), `currencies` (currencies and append-only rate history). Header link "Administration" for ADMIN.

## Product rules
- **Coverage**: all of Tunisia. Airports: Tunis-Carthage (TUN), Monastir (MIR), Enfidha-Hammamet (NBE), Djerba-Zarzis (DJE), Sfax (SFA), Tozeur (TOE), Tabarka (TBJ), Gafsa (GAF). Airports are data, not code: admins can add more.
- **Directions**: airport → address, address → airport, or round trip (two bookings linked by a shared group id).
- **Pricing V1**: flat rate per (airport, destination zone, vehicle type). Zones are groups of cities or governorates managed by admin. No live distance calculation in V1.
- **Vehicles**: Sedan (1–3 passengers), Van (4–7), Minibus (8–16). Capacities are configurable data.
- **Currencies**: TND, EUR, USD. **TND is the base currency**; all tariffs are stored in TND.
  - Exchange rates (TND per 1 EUR, TND per 1 USD) are managed by admin and stored with a timestamp.
  - A booking stores a snapshot: base price in TND, the currency chosen, the rate used, and the final charged amount. Later rate changes never alter an existing booking.
  - Use `BigDecimal` everywhere for money, never `double`. TND has 3 decimals (millimes), EUR and USD have 2. Round `HALF_UP` only when converting for display or charging.
- **Booking fields**: reference (human-readable, unique), customer, airport, direction, destination zone, address, flight number (optional), pickup date and time (stored as UTC, shown in Africa/Tunis), passengers, luggage count, vehicle type, notes, price snapshot, status, payment method, assigned driver (optional).
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
8. **Customer experience**: home page as designed (floating booking card with live price preview, vehicle cards, airport chips, how it works, footer); booking detail; edit address, flight, notes, luggage and pickup time while PENDING or CONFIRMED (price never changes: to change the vehicle or destination, cancel and rebook); profile (name, phone, password); forgot-password e-mail flow; printable receipt.
9. **Payment**: `PaymentGateway` abstraction, cash flow, payment status on bookings, Stripe card flow with webhook (when a Stripe account exists), refund on cancel, optional local TND gateway later.
10. **Quality and deployment**: integration tests runnable with `mvn verify` (Failsafe), consistent API error format, translated validation messages, accessibility pass, Dockerfiles, production `docker-compose`, README deployment section.

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
- Logo: red horseshoe arch (like a Tunisian door) with a white eight-pointed star, wordmark "Marhba" and tagline "TRANSFERT AÉROPORT". Component: `shared/logo.component.ts`.
- Home page: hero with headline and a floating booking card (direction tabs, airport, destination, date and time, passengers, price preview), vehicle cards, airport chips band, "how it works" in 3 steps, footer. Header: logo, nav, currency switch (TND / EUR / USD), language switch (FR / AR / EN), login.
- Booking flow on mobile: step indicator, route summary card, selectable vehicle cards, sticky bottom bar with the total and a primary "Continuer" button.

## Commands
```bash
docker compose up -d
cd backend && mvn spring-boot:run          # http://localhost:8080, Swagger at /swagger-ui.html
cd backend && mvn test                     # needs Docker for Testcontainers
cd frontend && npm install && npm start    # http://localhost:4200
cd frontend && npm run build
```
Dev admin: start the backend with `SEED_ADMIN_EMAIL=admin@marhba.tn SEED_ADMIN_PASSWORD=change-me-now`. Outside dev, always set `JWT_SECRET` (32+ characters).

## Definition of done (every step)
- Backend tests pass and cover the new rules, especially money, currency snapshot and status transitions.
- Frontend builds with no errors; new screens work in FR, EN and AR (RTL) and on a 390px-wide viewport.
- Migrations apply on an empty database.
- README and this file are updated (mark the step done, note new env variables).
