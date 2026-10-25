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
- **Statuses**: PENDING → CONFIRMED → ASSIGNED → IN_PROGRESS → COMPLETED, and CANCELLED from any state before IN_PROGRESS. Enforce transitions in one place (a service), not in controllers.
- **Payment**: cash to the driver, or card online. Put every provider behind a `PaymentGateway` interface: Stripe for EUR and USD cards, and an optional local TND gateway (Konnect or Flouci) added later. A webhook marks the booking paid; never trust the browser for payment status.
- **Notifications**: confirmation e-mail on booking (MailHog in dev). WhatsApp is out of scope for V1.
- **Guests**: booking requires an account in V1 (keep it simple).

## Roadmap
1. ~~Monorepo skeleton~~ done
2. ~~Authentication (JWT, roles)~~ done
3. **Data model**: Flyway migrations + entities + admin CRUD endpoints for airports, zones, vehicle types, tariffs, currencies and exchange rates. Seed the 8 airports, the 3 vehicle types and TND/EUR/USD. Public read endpoints for the booking form. Price quote endpoint: `GET /api/public/quote?airportId&zoneId&vehicleTypeId&currency`.
4. **Booking flow**: bookings API (create, list mine, cancel) with the price snapshot rule above; Angular 3-step booking form (trip → vehicle and price → details and confirmation) that matches the design below; "My bookings" page.
5. **Payment**: `PaymentGateway` abstraction, cash flow, Stripe card flow with webhook, payment status on bookings.
6. **Driver and admin areas**: admin dashboard (bookings list and filters, assign driver, manage tariffs, zones and exchange rates, create driver accounts); driver page (my assigned trips, change status).
7. **Quality and deployment**: error handling and consistent API error format, validation messages translated, accessibility pass, Dockerfiles for both apps, production `docker-compose`, README deployment section.

## API conventions
- REST under `/api`, JSON, ISO-8601 dates in UTC. Admin endpoints under `/api/admin/**`, driver under `/api/driver/**`, public under `/api/public/**`.
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
