# UniLinkHub

Connect. Buy. Sell & Succeed. — a centralized digital marketplace for student entrepreneurs
in university residences. See `UniLinkHub_Documentation.pdf` for the full project brief and
`UniLinkHub Brand Guide.pdf` for the visual/voice standards this UI follows.

## Repository layout

```
UniLinkHub/
├── backend/     Spring Boot API (Maven project: pom.xml, src/main, src/test)
├── frontend/    Vue 3 + Vite single-page app
├── Dockerfile   builds both into one deployable image (comments inside explain how)
└── README.md
```

The two halves only meet over HTTP (`/api/**`): in dev, Vite proxies `/api` to the backend; in
production the Dockerfile folds the frontend's build into the backend's static resources. If
you use IntelliJ, open the repo root and, if it doesn't pick the module up automatically, right-
click `backend/pom.xml` → *Add as Maven Project*.

## Stack

- **Backend:** Spring Boot 3.3 (Java 21), Spring Security + JWT, Spring Data JPA, MySQL
  (H2 for tests), Domain-Driven Design package layout (`domain` / `repository` port /
  `infrastructure` adapter / `application` / `web` per bounded context).
- **Frontend:** Vue 3 + TypeScript, Vite, Pinia, Vue Router, Tailwind CSS (configured with the
  brand guide's navy/teal/gold tokens plus tonal scales around them, and the Poppins/Inter type
  scale), Lucide icons.

The project docs floated Next.js/React in one section and Vue.js in another (the two brand/
project documents don't fully agree); this build follows the Project Documentation's explicit
technical decision (Vue.js + Spring Boot, "familiar from coursework") since that's the one tied
to a rationale, and it matches the Spring Boot backend that was already scaffolded.

## Demo accounts

Seeded directly against the running dev database so there's something real to click through
without registering from scratch. All four use the password `Demo@1234`.

| Role | Email | What's there |
|---|---|---|
| Seller | `demo.seller1@mycput.ac.za` | **Thabo's Prints** (Printing, verified) — A2 Poster Printing, Custom Mug Printing (low-stock badge), Business Card Design (service), an active `WELCOME10` promo code |
| Seller | `demo.seller2@mycput.ac.za` | **Lindiwe's Bakes** (Food, verified) — Custom Cupcake Box, Birthday Cake (sold out), Cake Tasting (service), a `CUPCAKE5` promo scoped to one listing; **and** a second business, **Zanele's Crafts & Jewellery** (Other, verified) — Beaded Bracelet, Beaded Keyring |
| Buyer | `demo.buyer@mycput.ac.za` | 2 saved listings, follows Lindiwe's Bakes, a saved search ("Food under R300"), a stock alert on the sold-out cake, an open message thread with Thabo's Prints, a CONFIRMED order for a bracelet, a 5-star review on Lindiwe's Bakes, and an answered question |
| Admin | `demo.admin@mycput.ac.za` | Full admin console access |

Business logos and product photos are `placehold.co` images tinted with the brand's own colour
tokens and labelled with the product name, rather than random stock photography that wouldn't
actually match what's being sold.

## What's implemented so far

MVP scope (Project Documentation, Section 11.1), plus a growing set of post-MVP items that
turned out to be small enough to build well beyond that scope: ratings/reviews, in-app
notifications, service bookings, direct messaging, product ordering, listing Q&amp;A, seller
promo codes, and saved searches with alerts. Real payment processing remains out of scope and
is still the empty stub package it started as - fulfilment and payment are arranged directly
between buyer and seller on pickup, the same as bookings already worked.

- Student registration (CPUT `@mycput.ac.za` addresses only) with **real email verification**:
  the student clicks the emailed link to activate their own account - admins never approve
  students by hand. Links expire after 48 hours and can be re-sent from the login page.
- Self-service "Forgot password" (emailed link, valid for 1 hour) and "Change email"
  (password-confirmed, the new address confirmed by email before it takes effect).
- Single-account model for students: any student can "Become a Seller" from their dashboard
  after accepting the marketplace rules (see below). Admin accounts are separate - see
  "Accounts, email and marketplace rules".
- Register/manage a business, request verification, and resubmit for another review if
  rejected (edit details from Account Settings, then resubmit).
- Create/edit/deactivate/reactivate Product or Service listings, editable inline from the
  dashboard (title, category, description, price, stock/duration/availability, status, and an
  uploaded photo) - plus a unified "My listings" table across all of a
  seller's businesses.
- A fixed suggested category taxonomy (`GET /api/categories`) offered as a dropdown everywhere
  a listing or business category is set, so values can't drift into near-duplicates
  ("Printing" vs "printing").
- Product stock hitting zero now marks the listing SOLD_OUT (shown distinctly from a
  deliberately-paused INACTIVE listing) instead of silently deactivating it, and automatically
  clears back to ACTIVE once restocked.
- Public browse with keyword/category search, price range, product/service type, verified-
  sellers-only, and sort (newest, price asc/desc, most viewed); per-listing view counts, a
  "Browse by category" tile grid, and a "Trending this week" (most-viewed) section.
- Provider directory ("Browse businesses") — a public page listing every non-rejected
  business, with keyword/category search and a verified-only filter, linking through to each
  provider's profile; each profile also shows other verified businesses in the same category.
- Report/flag a listing, plus an admin review queue (begin-review / resolve / dismiss) that
  resolves reporter/target ids into names for display, a status-counts endpoint, and a
  "N total reports on this target" context banner when reviewing.
- Business verification: admin can browse the full verification history (all/pending/
  verified/rejected, searchable) rather than just a pending queue, expand a business to see
  its submitted listings before deciding, and reject with a reason the seller sees on their
  Account Settings page before resubmitting.
- Student account management: admin can view/search every student account, re-send a
  verification email, suspend (with a reason the student sees - and a suspension logs them out
  immediately) or reactivate an account, and expand an account to see its businesses and the
  reports it filed/received.
- Self-service "Deactivate my account" from Account Settings - hides the student's businesses/
  listings immediately, and simply logging back in with the right password reactivates it (no
  admin step needed), reusing the existing DEACTIVATED account status.
- A public landing hero (marketing copy, "how it works", featured verified businesses) shown on
  Browse to logged-out visitors only; logged-in users see the existing browse hero straight away.
- Search autocomplete on Browse - typing suggests matching categories and listings inline as you
  type, before you even hit enter.
- Share a listing (copy-link popover) from the listing detail page.
- Compare up to 3 listings side by side - a "Compare" checkbox on any listing card, a persistent
  tray showing the current selection, and a comparison table (price/status/views/saves).
- A full "Recently viewed" page (beyond the dashboard's 3-item preview), with a "Clear history"
  action.
- "Contact seller" on a provider's profile - reveals the owner's email/phone to signed-in buyers
  (no in-app messaging yet, so this is the stand-in).
- A "Get verified" checklist on the seller dashboard for any business still PENDING (profile
  complete / has a listing / has a logo).
- Bulk activate/deactivate on "My listings" (multi-select + a sticky action bar), plus a
  per-listing "Duplicate" action (creates a copy, saved inactive, ready to edit).
- Site-wide announcements: admin can publish/deactivate a banner shown to every visitor (an
  "Announcements" tab in the admin console), only one active at a time.
- Admin activity log: a chronological, filterable trail of verify/reject, suspend/reactivate/
  promote, resolve/dismiss and announcement actions, each attributed to the admin who did it.
- CSV export of the reports queue from the admin console (client-side, respects the current
  status filter).
- Seller business performance: listings/views/saves/follower counts per business on the
  dashboard.
- Follow a provider - heart-style "Follow" button on a provider's profile, with a "Providers
  you follow" section on the buyer dashboard, mirroring the saved-listings pattern.
- Business logo (optional image URL) shown on directory cards and the provider profile header.
- "More from this seller" - related active listings from the same business shown on a
  listing's detail page.
- Listing social proof: view count and "N students saved this" shown on the listing detail
  page, plus how long ago it was listed.
- A "My activity" summary (businesses, listings, saved, following, reports filed) on Account
  Settings.
- A proper 404 page for unmatched routes instead of a blank screen.
- Saved/favourited listings (heart toggle on any listing card or the listing detail page),
  recently viewed listings (tracked client-side, per browser), and a "your reports" status
  list - the Buyer Dashboard requirements from Section 11.1, all on the same single dashboard
  alongside the seller section.
- Provider profile page (business info, verification badge, stats, their active listings).
- Account settings page ("My account"): edit profile, edit business details, and change
  password (with current-password verification).
- Admin overview: a stats tab (students, businesses, listings, and reports, each broken down
  by status) alongside the existing report queue, business verification, and student account
  tabs.
- A functional Vue UI for all of the above: browse with filters/category tiles/trending,
  provider directory, listing detail + report, provider profile, login/register, forgot/reset
  password, account settings, a combined buyer/seller dashboard, a unified "My listings" page,
  and an admin console (overview, report queue, business verification, student account
  management) gated by role.
- In-app notifications (bell icon with unread badge in the header, polled every 30s, plus a full
  `/notifications` page) - fired for business verification decisions, booking updates, stock/
  price alerts and new reviews, each independently toggleable from Account Settings. A new
  `notification` module the other modules call into (never the other way - `NotificationService`
  deliberately does not depend on `UserService`, since `UserService` needs to call it and a
  two-way dependency between the beans would be circular).
- Service bookings: a buyer can request a booking on any Service listing (preferred date/time +
  a note); the seller accepts or declines (with a reason) from a "Booking requests" inbox on
  their dashboard; the buyer tracks status on a "My bookings" page. New `booking` module.
- Ratings &amp; reviews: a signed-in student can leave one star rating + comment per business
  (a second submission edits the first, since there's no purchase record to tie a review to);
  shown as an average + star-distribution + list on the provider profile; any reviewer can flag
  a review, and admin gets a moderation queue plus a platform ratings overview (top/lowest rated
  businesses). New `review` module.
- "Notify me when back in stock" on a sold-out product - a one-shot subscription that fires
  (and clears itself) the moment the listing restocks.
- Seller-set low-stock alert threshold on a product - notifies the seller once, the first time
  stock crosses at/under that number, not on every subsequent edit.
- Deeper per-listing insights page (views/saves/save-rate, compared against the seller's other
  listings) linked from My Listings, and a CSV export of My Listings.
- A "Recommended for you" section on the buyer dashboard, based on categories from saved
  listings and followed providers.
- Bulk-select and remove multiple saved listings at once from the dashboard.
- Admin: a targeted broadcast notification (distinct from the site banner - this lands directly
  in recipients' notification bells) to all students, all sellers, or pending business owners;
  a bookings overview tab (counts, most-booked services); the reviews moderation/overview tab
  above.
- Direct messaging: a buyer can message a business straight from any listing; one thread per
  (business, buyer) pair regardless of which listing started it, a header message icon with
  unread badge and preview dropdown, and a full inbox + conversation page. New `messaging`
  module (filled in the previously-empty stub package).
- Product ordering: a client-side cart (product listings only - services stay on the booking
  flow), checkout with a pickup/delivery choice and an optional promo code, and a full order
  lifecycle (placed → confirmed → ready → completed, or cancelled with a reason) seen from a
  buyer's "My orders" and a seller's "Orders" page; checking out decrements stock through the
  same low-stock-notification path a manual stock edit uses. A cart spanning several sellers
  becomes one order per business, never one order no single seller could act on. New `ordering`
  module (filled in the previously-empty stub package).
- Listing Q&amp;A: any signed-in student can publicly ask a question on a listing (a textarea with
  a character counter, an avatar-initial per asker, and relative timestamps); the seller answers
  from a "Questions awaiting your reply" panel on their dashboard or a full Q&amp;A management page
  (`/questions` - filter by all/pending/answered, search, edit a past answer); both the question
  and answer are visible to everyone, not just the asker. Any signed-in student can flag an
  inappropriate question (mirroring how reviews are flagged), and admin gets a moderation queue
  plus stats (total/answered/pending/flagged), with removals recorded in the activity log. New
  `qa` module.
- Seller promo codes: percentage or fixed-amount discounts, scoped to a business or to one
  listing, with an expiry and an active/off toggle; a matching listing shows the discounted
  price and a promo badge; admin gets a redemptions/discount-given overview. New `promo` module.
- Saved searches: save the current browse filters with one click, get notified the moment a new
  listing matches (checked against just that one new listing when it's published, not by
  polling), and see how many new matches are waiting next to each saved search. New
  `savedsearch` module.
- A proper logo: a hand-authored SVG "UH" monogram (Playfair Display serif, matching the brand
  guide's colours exactly) with a graduation-cap-over-shopping-cart icon nested in its swoosh,
  used as the favicon, in the header next to the wordmark, and as an animated splash screen shown
  while the app's first route resolves (removed once `router.isReady()` settles, so it never
  flashes the real UI before the auth check is done).
- A global toast notification system (`ToastContainer.vue` + a Pinia store) with a spring-eased
  enter, a shrinking progress bar that pauses correctly on hover (the JS auto-dismiss timer and
  the CSS bar share the same remaining-time tracking, so they can't drift out of sync), and
  stacking/reflow animations. Every mutating API call (POST/PATCH/PUT/DELETE) toasts automatically
  on failure via an axios interceptor; GET requests are excluded since several of those are
  intentionally-silent background fetches (polling, prefetching) that would otherwise spam the
  user. Success toasts are wired into the highest-traffic actions across the app: auth, cart/
  checkout, messaging, listings, bookings, reviews, Q&amp;A, promo codes, saved searches, and the
  full admin console.
- Mobile responsive down to a 320px-wide viewport. Below 768px the primary navigation moves to a
  bottom tab bar (Explore / Providers / Search / Messages / Dashboard), and everything else lives
  in the avatar menu. "My listings" swaps its
  table for a stacked card list on mobile rather than making the table horizontally scrollable,
  because its per-row action menu is positioned to escape the row and would get clipped by a
  scroll container - a direct list of action links avoids that instead of fighting it. Verified
  with an automated sweep (Playwright, not just eyeballing screenshots - a plain headless-Chrome
  screenshot tool turned out to render this app inaccurately at narrow widths) across every route,
  every admin tab, and the booking/message modals at exactly 320px, checking each page's actual
  `scrollWidth` against its viewport rather than trusting a screenshot.

### Design refresh + new features (September 2026)

- **Redesigned UI.** A shared design system in `frontend/src/style.css` + `tailwind.config.js`
  (buttons, inputs, cards, chips, skeletons, modals, shadows, motion) so every page picks up the
  same look. Sticky glass header with active-page states and an avatar menu, a landing page with
  live platform stats and a trending carousel, split-screen auth pages, a two-column listing page
  with a sticky purchase panel, a site footer, route transitions, skeleton loaders and consistent
  Lucide icons in place of the old emoji and pasted SVG paths. The neutral greys were darkened
  slightly - the brand manual's `#8A94A6` is only ~3:1 on white, below WCAG AA for body text.
- **Command palette (⌘K / Ctrl+K, or `/`).** Global search across listings, sellers, categories
  and pages, fully keyboard-driven (`frontend/src/components/CommandPalette.vue`).
- **Seller sales analytics.** `GET /api/orders/seller/analytics?days=30&businessId=` returns
  revenue, orders, average order value, unique/repeat buyers and a daily series for the window,
  each compared with the previous window of the same length, plus best-selling listings. The
  dashboard's new *Selling* tab charts it (7/30/90 days, per business, with a table view).
- **Public platform stats.** `GET /api/stats/public` (no auth) - students, verified businesses,
  live listings and completed orders, shown in the landing-page hero.
- **Dashboard split into Buying / Selling tabs**, with quick-action tiles.
- **Smaller additions:** password show/hide + strength meter on sign-up, a copyable promo-code
  chip on listings, "Only N left" and "New" badges on cards, images that fail to load fall back to
  a category-tinted placeholder, and the app is installable to a phone's home screen (web manifest).
- **Fixes along the way:** search autocomplete rendered listing names with `v-html` unescaped (a
  stored-XSS hole) - now escaped; moving between two listings (e.g. via "More from this seller")
  kept showing the old one; header dropdowns never closed on an outside click; the provider
  directory's banner heading was navy-on-navy and invisible.

All of the above has been exercised end-to-end against a real MySQL database (see the smoke
test script below) — it isn't just "compiles", it actually runs.

### Accounts, email and marketplace rules (October 2026)

- **Every rejection explains itself.** Anything the API refuses comes back as one JSON shape with
  a plain-English `message` saying why (and, where useful, a `code` the UI acts on). A 403 means
  "you're not allowed to do this" and always says why; 401 only ever means "you're not logged
  in / your session ended". Covered for controller errors, security-filter rejections, bad links
  and validation (e.g. "Student number is required.").
- **Admin accounts are separate from student accounts.** Admins run the marketplace and stay
  neutral: they can't become sellers, buy, book, review, message sellers, save listings or follow
  businesses (`@StudentOnly` on those endpoints; the UI hides them too). A student can't be
  promoted to admin. New admins are invited from the console's *Admin team* tab - they get an
  email to set their own password - and the very first admin is created from the `ADMIN_EMAIL` /
  `ADMIN_PASSWORD` environment variables on startup if no admin exists. An admin can't verify a
  business they own.
- **Restricted items.** Alcohol, drugs, weapons, cigarettes/vapes/hubbly, cheating services, fake
  documents/stolen goods and adult content can't be listed. Listings and businesses are checked
  when saved (`RestrictedItemsPolicy`, whole-word matching with an allow-list so "ginger beer" or
  "glue gun" are fine) and refused with an explanation of the rule and its consequences. Sellers
  must accept the rules before selling (the time is recorded), the rules are shown on a public
  *Marketplace rules* page and on every listing form, students can report a listing as
  "Restricted or illegal item", and admins can take any listing down from the new *Listings* tab
  - the seller is notified with the reason, and only an admin can restore it.
- **Photo uploads** for listings (products and services) and business logos. Photos are resized
  in the browser, checked by their actual bytes (JPEG/PNG/WebP only - no SVG), capped at 5 MB and
  stored in the database (so they survive redeploys on hosts with temporary disks).
- **Honest-trading rules:** sellers can't order from, book, review or ask questions on their own
  business; reviews need a completed order or accepted booking; cancelling an order puts its stock
  back; checkouts lock stock rows so two buyers can't both get the last item.
- **Privacy fixes:** a seller's email/phone (`/api/businesses/{id}/contact`) now needs a login,
  and full account details (`/api/users/{id}`) are admin-only.

### Business posts (October 2026)

Verified businesses can post updates on their provider page, Facebook-page style: text, a photo,
and optionally one of their own listings as a tappable card. The provider page now has **Posts /
Listings / Reviews** tabs, with Posts first.

- Followers are notified of new posts and see them in a *From businesses you follow* feed on their
  dashboard - so following a business finally does something.
- Students can like, comment and share (a link that opens the page scrolled to that post). The
  page owner can pin one post to the top, edit or delete posts, and delete comments on their page.
- Only the owner of a **verified** business can post, so new accounts can't use posts to spam;
  posts and comments go through the same restricted-items check as listings; each business can
  post 10 times per 24 hours (HTTP 429 with an explanation after that).
- Students can report a post; admins see reported posts first in the console's *Posts* tab and can
  remove them with a reason the business is shown.
- API: `GET/POST /api/businesses/{id}/posts`, `PATCH/DELETE /api/posts/{id}`,
  `POST /api/posts/{id}/pin|like|flag`, `DELETE /api/posts/{id}/like`,
  `GET/POST /api/posts/{id}/comments`, `DELETE /api/post-comments/{id}`, `GET /api/posts/feed`,
  `GET /api/admin/posts`, `POST /api/admin/posts/{id}/remove`. Tests: `BusinessPostsTest`.

#### Setting up email

Without `MAIL_HOST`, emails aren't sent - each one is written to the backend log with its full
link, which is fine for local development. To send real email, set:

| Variable | Example (Gmail) |
|---|---|
| `MAIL_HOST` | `smtp.gmail.com` |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | `unilinkhub.app@gmail.com` |
| `MAIL_PASSWORD` | a Gmail **App Password** (Google Account → Security → App passwords), not the normal password |
| `MAIL_FROM` | `UniLinkHub <unilinkhub.app@gmail.com>` |
| `APP_BASE_URL` | where the frontend lives, e.g. `https://unilinkhub.onrender.com` - email links point here |

`ALLOWED_EMAIL_DOMAIN` (default `mycput.ac.za`) controls which addresses can sign up.

#### Upgrading a database created before migrations existed

Databases made by the old `ddl-auto=update` setting upgrade themselves - just start the backend.
Flyway marks them as already at V1, then `V1_1__AdoptPreFlywaySchema` compares them with
`V1__baseline.sql` and creates missing tables, adds missing columns, updates changed column types
(e.g. new enum values, `student_number` becoming optional for admins) and makes columns the app
no longer uses optional. It never deletes tables, columns or data, and the log lists every change.
If a migration ever fails half-way (MySQL can't undo table changes), the next start clears the
failed record and runs it again (`FlywayConfig`).

### Discovery, trust and real-time (October 2026)

- **Log-in and sign-up abuse limits.** 5 wrong passwords for one account locks log-in for that
  account for 15 minutes; per-network limits cap failed log-ins, sign-ups and outgoing emails
  (verification / password reset). Every limit answers with HTTP 429 and a plain-English reason.
  Tune with `unilinkhub.rate-limits.*` in `application.yml` (`failed-logins-per-email`,
  `failed-logins-per-ip`, `registrations-per-ip-per-hour`, `emails-per-address-per-hour`,
  `emails-per-ip-per-hour`). Behind a proxy, the visitor's address comes from `X-Forwarded-For`.
- **Campus and pickup spot.** Every business picks its CPUT campus (Bellville, District Six,
  Mowbray, Wellington, Granger Bay) and a pickup spot; students can save their own campus. Explore
  has campus chips plus a **Near me** shortcut, and listing cards/pages say where to collect.
- **Pickup codes.** Each pickup order gets a 4-digit code that only the buyer sees (shown big on
  *My orders* and in the "ready" notification). The seller enters it to complete the order, which
  proves the hand-over happened. 5 wrong codes lock the order for 15 minutes.
- **Paging everywhere it matters.** Explore loads 24 listings at a time with infinite scroll and a
  "Load more" fallback (`GET /api/listings/search` returns `{items, page, totalItems, hasNext}`);
  category counts come from one query (`GET /api/listings/category-counts`); admin listings page
  by 30; post feeds load older posts with a `before` cursor.
- **Up to 6 photos per listing**, reorderable when editing, shown in a swipeable gallery with
  thumbnails, arrows and keyboard support.
- **Trust signals** on cards, listing pages and shop pages: verified badge, rating, orders
  completed and typical reply time (`GET /api/businesses/{id}/trust`).
- **Real-time updates.** New messages and notifications arrive instantly over Server-Sent Events
  (`GET /api/stream`): open chats update in place, toasts appear on any page, and badge polling
  only runs as a fallback when the stream is down. Connections are kept in memory, so with more
  than one backend instance this needs a shared broker (e.g. Redis pub/sub).
- **Keyboard-friendly dialogs.** Every modal and the cart drawer move focus inside when opened,
  keep Tab inside, close on Escape, stop the page behind from scrolling, and give focus back to
  the button that opened them (`v-dialog` directive, `frontend/src/directives/dialog.ts`).
- **Dark mode** with Light / Dark / Match device, in the account menu and the footer; remembered
  per browser and applied before first paint. It's generated at build time by a small PostCSS
  plugin (`frontend/postcss/darkTheme.js`) that gives every light-palette colour a dark twin, so
  new components get dark mode without writing `dark:` classes.
- **Seller setup checklist** on the dashboard: rules, business, campus and pickup spot, logo,
  first listing with a photo, first post, verification - each with a shortcut to do it.
- **Flyway migrations** replace `ddl-auto=update` (see *Database* below) and **GitHub Actions CI**
  (`.github/workflows/ci.yml`) runs the backend tests (including migrations on a real MySQL), the
  frontend type-check and build, and a Docker image build on every push and pull request.

### Responsive layout (October 2026)

Every page, admin tab, menu, drawer and dialog was checked automatically at 17 screen sizes (phones
from 320px, phones turned sideways, tablets, laptops, 1920px desktops and 2560px monitors) for
anything cut off by the screen edge, clipped by its container, hidden behind the bottom tab bar or
making the page scroll sideways, and every finding was fixed.

The CSS is **mobile-first**: unprefixed Tailwind classes are the phone layout and each breakpoint
adds to the one below. Breakpoints (`frontend/tailwind.config.js`): `xs` 400px (large phones),
`sm` 640, `md` 768 (tablets - the bottom tab bar hands over to the top navigation here), `lg` 1024
(small laptops - filters sidebar), `xl` 1280, `2xl` 1536 and `3xl` 1920 (large monitors - wider
page and an extra column of listings). Rules of thumb that prevent the bugs we found:

- Give every `grid` a phone column (`grid-cols-1`) and write fraction tracks as `minmax(0,1fr)`,
  so long content can't stretch the grid past the screen.
- In a `flex` row, wrap mixed text and inline tags in one `<span>`, or each piece becomes a column.
- Rows of "details + action buttons" stack on phones (`flex-col sm:flex-row`).
- Dialogs and dropdowns are height-capped and scroll inside (`.modal-panel`, `.dropdown-panel`).

## Not built yet (next steps)

- Real payment processing - fulfilment and payment are arranged directly between buyer and
  seller on pickup/delivery; the `payment` and `appointment` packages are still the empty stubs
  they started as.
- Deployment/hosting decision (Section 13.1 in the docs still flags this as open).
- Deeper automated test coverage — `MarketplaceSmokeTest` (see below) covers the golden path end
  to end via MockMvc, but there are no isolated unit tests per use case/controller yet.

## Database: MySQL setup

The app talks to MySQL (or MariaDB, e.g. XAMPP) via `spring-boot-starter-data-jpa` +
`mysql-connector-j`. Flyway creates and upgrades the tables on startup - **you do not need to
write `CREATE TABLE` statements yourself.** You only need to create the database and a user once.

> **XAMPP / MariaDB:** the default `max_allowed_packet` is 1 MB, which is too small for photo
> uploads (the app explains this to the user instead of failing silently). Set
> `max_allowed_packet=16M` under `[mysqld]` in `C:\xampp\mysql\bin\my.ini` and restart MySQL.
> MySQL 8 defaults to 64 MB, so production needs no change.

### 1. Create the database and app user

Run this once against your MySQL server (as root, or whichever account can create databases):

```sql
CREATE DATABASE IF NOT EXISTS unilinkhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'unilinkhub'@'localhost' IDENTIFIED BY 'unilinkhub';
GRANT ALL PRIVILEGES ON unilinkhub.* TO 'unilinkhub'@'localhost';
FLUSH PRIVILEGES;
```

From a shell: `mysql -u root -p < setup.sql` (paste the block above into `setup.sql`), or run it
interactively in `mysql -u root -p`. If you're on the bundled XAMPP MySQL with the default empty
root password, drop `-p`.

If you'd rather use different credentials, change the `IDENTIFIED BY '...'` password above and
set `DB_USERNAME` / `DB_PASSWORD` env vars to match when you run the app (defaults are both
`unilinkhub`, matching the block above).

### 2. The schema (Flyway migrations)

The schema lives in versioned SQL files in `backend/src/main/resources/db/migration`, applied by
Flyway on startup:

- `V1__baseline.sql` - every table, generated from the JPA entities (works on MySQL 8 and
  MariaDB 10.4+). Read it if you need to hand in a schema.
- `V2__lookup_indexes.sql` - indexes for the lookups every page makes (listings by business,
  orders by buyer, unread notifications, ...).

Hibernate runs with `ddl-auto=validate`: it never changes tables, it only refuses to start if the
entities and the database disagree. **To change the schema, add a new file** such as
`V3__add_listing_tags.sql` - never edit a migration that has already run somewhere. Adding a value
to an enum stored in the database (for example a new `Campus`) also needs a migration, because
those columns are MySQL `ENUM`s.

`MigrationTest` applies the migrations to a real, empty MySQL database and lets Hibernate validate
the result. It runs in CI; locally, point it at an empty database:

```sh
MIGRATION_TEST_DB_URL="jdbc:mysql://localhost:3306/unilinkhub_migration_test" \
MIGRATION_TEST_DB_USERNAME=root MIGRATION_TEST_DB_PASSWORD= mvn test
```

`id` columns are `binary(16)` because entity IDs are Java `UUID`s.

### 3. Useful queries once you've used the app for a bit

```sql
-- See registered users and their verification/seller state
SELECT student_number, email, account_status, is_seller, role FROM unilinkhub.users;

-- See listings with their owning business
SELECT l.name, l.listing_type, l.price, l.status, l.view_count, b.business_name
FROM unilinkhub.listings l
JOIN unilinkhub.businesses b ON b.id = l.business_id;

-- Open reports waiting for admin review
SELECT id, reason, details, status, created_at FROM unilinkhub.reports WHERE status = 'OPEN';

-- The first admin is normally created on startup from ADMIN_EMAIL / ADMIN_PASSWORD, and every
-- admin after that is invited from the console's "Admin team" tab. Admins and students are
-- separate accounts, so don't turn a student account into an admin.
SELECT email, first_name, last_name FROM unilinkhub.users WHERE role = 'ADMIN';
```

## Running it locally

### Backend

1. Run the two `CREATE DATABASE` / `CREATE USER` statements above, once.
2. Start the app from the `backend/` folder (no local Maven install needed if you're on
   IntelliJ — it bundles one; or use `mvn` directly if you have it on PATH):
   ```sh
   cd backend
   mvn spring-boot:run
   ```
   Env vars you can override: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (set a real 32+
   byte secret before this ever goes near production - `openssl rand -base64 48`), `ADMIN_EMAIL` /
   `ADMIN_PASSWORD` (creates the first admin if there isn't one), the `MAIL_*` settings above,
   `JPA_DDL_AUTO` (default `validate`; see *Upgrading a database* above), and `PORT` (defaults to
   **8081**, not 8080 - picked to avoid clashing with another local Spring Boot project on this
   machine).
3. API is served at `http://localhost:8081/api`. `mvn test` runs against an in-memory H2
   database, so tests don't need MySQL running at all.

### Frontend

```sh
cd frontend
npm install
npm run dev
```

Served at `http://localhost:5173` (or the next free port if that's taken); Vite proxies `/api`
to `localhost:8081` in dev (`frontend/vite.config.ts`), so no CORS config is needed locally.

## Automated smoke test

`backend/src/test/java/za/co/unilinkhub/MarketplaceSmokeTest.java` walks the same golden path as the
curl script below, but as a real `mvn test` (MockMvc against the H2 test database, no server or
MySQL needed): register a seller and buyer, list a product, browse/search for it, message
between buyer and seller with unread counts, apply a promo code at checkout and confirm the
discount + stock decrement, confirm the order, ask and answer a listing question, and save a
search that fires a notification the moment a new listing matches it. It exists so a future
change can't silently break one of these flows again without failing the build.

## Testing the system end-to-end (curl)

This is the exact sequence used to verify the backend against a real MySQL database - register,
verify, log in, become a seller, create a business + listing, browse/search publicly, file a
report, and review it as an admin. Save as `smoke-test.sh` and run with the backend up:

```sh
#!/usr/bin/env bash
set -e
API=http://localhost:8081/api

extract() { echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }

echo "== register =="
curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" -d '{
  "studentNumber": "222357614",
  "firstName": "Siphokuhle",
  "lastName": "Test",
  "email": "siphokuhle.test@mycput.ac.za",
  "password": "password123"
}'; echo

echo "Grab the verification token from the backend console log:"
echo '  grep "Verification link" <backend log>'
read -rp "Paste the token here: " VERIFY_TOKEN

echo "== verify =="
curl -s "$API/auth/verify?token=$VERIFY_TOKEN"; echo

echo "== login =="
LOGIN_JSON=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" -d '{
  "email": "siphokuhle.test@mycput.ac.za",
  "password": "password123"
}')
JWT=$(extract "$LOGIN_JSON" "token")
echo "$LOGIN_JSON"; echo

echo "== become seller =="
curl -s -X POST "$API/users/me/become-seller" -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d '{"acceptedRules": true}'; echo

echo "== create business =="
BUSINESS_JSON=$(curl -s -X POST "$API/businesses" -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d '{
  "businessName": "Siphos Prints",
  "description": "Affordable printing and binding for res students",
  "category": "Printing",
  "campus": "BELLVILLE",
  "pickupLocation": "Res block C, room 12"
}')
echo "$BUSINESS_JSON"
BUSINESS_ID=$(extract "$BUSINESS_JSON" "id")

echo "== create a product listing =="
LISTING_JSON=$(curl -s -X POST "$API/listings/products" -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d "{
  \"businessId\": \"$BUSINESS_ID\",
  \"name\": \"A4 Black & White Printing\",
  \"description\": \"10c per page, same-day turnaround\",
  \"category\": \"Printing\",
  \"price\": 0.10,
  \"stockQuantity\": 5000
}")
echo "$LISTING_JSON"
LISTING_ID=$(extract "$LISTING_JSON" "id")

echo "== public search (no auth needed) =="
curl -s "$API/listings?keyword=printing"; echo

echo "== public listing detail (view count increments each call) =="
curl -s "$API/listings/$LISTING_ID"; echo

echo "== file a report =="
curl -s -X POST "$API/reports" -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d "{
  \"targetType\": \"LISTING\",
  \"targetId\": \"$LISTING_ID\",
  \"reason\": \"MISREPRESENTATION\",
  \"details\": \"Testing the report flow\"
}"; echo

echo "To review it, log in with a separate admin account (e.g. the one created from ADMIN_EMAIL)"
echo "- not this student account - and with that admin token call:"
echo "  GET  $API/admin/reports?status=OPEN"
echo "  POST $API/admin/reports/{id}/resolve   -d '{\"note\": \"...\"}'"
```

## Project management docs still to fill in

The Project Documentation PDF flags a few `[TEAM TO COMPLETE]` sections that are outside what
code can answer for you: team/role allocation, Trello sprint breakdown, survey sample size,
and final wireframes/Figma link.
