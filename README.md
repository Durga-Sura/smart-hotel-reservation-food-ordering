# Table & Thyme

A staged Smart Hotel Reservation & Food Ordering Platform using Java 17+, Spring Boot, plain HTML/CSS/JavaScript, and MySQL 8. The responsive interface includes a hotel-owner workspace and a guest experience.

## Run with Docker (recommended)

1. Install Docker with Compose.
2. Copy `.env.example` to `.env` and replace both password placeholders with strong passwords. `OWNER_PASSWORD` must contain at least 12 characters.
3. Run `docker compose up --build`.
4. Open http://localhost:8080. The footer will show “Java + MySQL connected”.
5. Sign in from the sidebar account button with `OWNER_EMAIL` / `OWNER_PASSWORD`. This seeds The Fern Residency with tables and a sample menu on first startup. Real reservations and orders start empty.
6. Open Guest experience to browse, or register a separate guest account. Guest reservations require sign-in. Registering as a hotel owner creates a separate hotel, isolated from other owners.

The MySQL volume keeps data between restarts. Use `docker compose down` to stop without deleting it. Back up the database before changing schemas.

## Run without Docker

Install Java 17+ (JDK), Maven 3.9+, and MySQL 8. Create a database and application user:

```sql
CREATE DATABASE table_thyme CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tablethyme'@'localhost' IDENTIFIED BY 'YOUR_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON table_thyme.* TO 'tablethyme'@'localhost';
```

Set environment variables `DB_URL=jdbc:mysql://localhost:3306/table_thyme?connectionTimeZone=Asia/Kolkata`, `DB_USER=tablethyme`, `DB_PASSWORD`, `OWNER_EMAIL`, and `OWNER_PASSWORD`. Do not place real secrets in source files.

```sh
mvn test
mvn package
java -jar target/table-thyme-1.0.0.jar
```

The schema in `src/main/resources/schema.sql` is initialized automatically. Default port: 8080. Default hotel timezone: Asia/Kolkata; override with `HOTEL_ZONE`. All hotels in an installation currently use the same timezone. Sessions expire after 30 minutes. Set `COOKIE_SECURE=true` behind HTTPS, use a reverse proxy, and set production database credentials before exposing the Java service publicly.

## Included features, by your stages

| Phase | Implemented |
|---|---|
| 1 | Owner/customer registration, login, BCrypt passwords, server sessions, role checks, hotel ownership checks, sign-out |
| 2 | Hotel profile, cuisine, hours, food categories, prices, images, availability |
| 3 | Table creation, capacity, current floor status, soft removal |
| 4 | Date/time reservation, automatic table fit or selected table, 90-minute slots, capacity/hours checks, transactional overlap protection, status changes, cancellation, CSV export |
| 5 | Optional food pre-order tied to a reservation, server-calculated totals, immutable order price/name snapshots |
| 6 | Owner overview, floor view, guest reservations, kitchen stages (Pending → Preparing → Ready → Served), basic workspace analytics |
| 7 (partial) | Pay-at-hotel selection and local workspace activity notifications |
| 7–8 (planned) | Online payment gateway and verified webhooks, refunds, reviews, email/SMS/push, QR ordering, stock/inventory, advanced analytics |

There is no card processing or paid-status claim. “Completed order value” sums orders marked served; it is not a payment ledger. Reservation status and kitchen status are independent so staff can complete a visit without rewriting historical orders. Food orders are scheduled with their reservation; standalone delivery orders are outside this first release.

## Hosted preview versus Java application

The hosted Sites preview is static HTML/CSS/JavaScript with browser-local demo data. It does **not** run Java or connect to MySQL. It starts with an example owner workspace, offers a guest switch, and retains edits in localStorage on the current browser. The demo's account form is a local persona switch, not secure authentication; do not use real passwords there. Demo reset restores examples.

When served by the Java application, the same frontend detects `/api/health` and uses the real session API and MySQL data. No frontend framework or Node build is required. On this mode, all booking and owner changes pass through backend authorization. Public guests never receive other guests’ reservation data. Site-hosting access control is separate from application login.

The frontend uses Google Fonts and Unsplash HTTPS images. For offline operation or production branding, replace these with local assets and update image URLs.

## Structure

- `dist/`: HTML, CSS, JavaScript frontend
- `src/main/java/com/tablethyme/`: Java application, REST API, request guards
- `src/main/resources/schema.sql`: MySQL schema and indexes
- `src/main/resources/application.properties`: environment-based configuration
- `src/test/java/com/tablethyme/ApiTest.java`: API integration tests (H2 in MySQL compatibility mode)
- `compose.yaml`, `Dockerfile`: Java + MySQL deployment

## API

Public: `GET /api/health`, `/api/hotels`, `/api/workspace?hotelId=1`, `/api/availability?hotelId=1&date=YYYY-MM-DD&time=18:00&guests=2`.

Accounts: `POST /api/auth/register`, `/api/auth/login`, `/api/auth/logout`; `GET /api/auth/me`. Registration accepts `name`, `email`, `password`, and optional role `CUSTOMER` or `OWNER`.

Guests: `POST /api/reservations` accepts hotelId, tableId, guest, email, guests, date, time, notes, items `[{id,qty}]`. Client prices/totals are ignored. `PATCH /api/reservations/{id}` supports authorized status changes. Customers may cancel only confirmed bookings they created; owners may seat, complete, or cancel visits at their own hotel.

Owner: `PUT /api/owner/hotel`; `POST /api/owner/tables`, `PUT/DELETE /api/owner/tables/{id}`; `POST /api/owner/menu`, `PUT /api/owner/menu/{id}`; `PATCH /api/owner/orders/{id}` with status. Updates require ownership of the hotel, enforced in Java.

POST/PUT/PATCH use JSON. Session cookies are HttpOnly and SameSite=Strict. Mutation requests reject cross-origin Origin headers, reject non-JSON bodies, and return structured errors. No permissive CORS is enabled. Login/registration are rate limited per remote IP in a single application instance. Use shared rate limiting/session storage for horizontal scaling. Run the service at the root origin; subpath deployments need frontend API path configuration.

## Validation and limits

API integration tests cover accounts, access restrictions, server-side food totals, 90-minute booking overlap, adjacent slots, cancellation, owner isolation, kitchen transitions, and cross-origin mutation rejection. Tests use H2 MySQL compatibility mode, not a live MySQL instance. A real MySQL deployment smoke test is still required in your target environment. Frontend DOM smoke tests were performed separately for navigation, menu creation, bookings, conflicts, and safe text rendering. Browser visual QA was unavailable in the build environment.

Use migrations (Flyway/Liquibase), database backup/restore tests, concurrency load testing, password reset/email verification, and a real payment gateway before a production launch. The initial owner seed runs once; changing the owner environment password later does not reset an existing account.
