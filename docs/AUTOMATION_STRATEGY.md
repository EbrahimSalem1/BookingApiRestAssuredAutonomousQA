# Automation strategy: booking.com at the API layer

> Task 2 deliverable: a high-level list of automation ideas (coverage strategy, not test cases).
> **Implemented** marks what this repository automates today; the rest is the backlog, ordered by risk.

## Guiding principles

1. **Test at the lowest layer that can prove the rule.** Search relevance, price integrity and auth
   are API facts; only rendering and interaction need a browser. API checks run in seconds, so
   they can run on every commit.
2. **Separate "the product is broken" from "we could not look".** booking.com sits behind bot
   protection. A challenged request is *inconclusive*, never green and never red. The
   triage engine makes this distinction explicit and machine-readable.
3. **Hermetic gate, live signal.** Pull requests are gated by a WireMock model of the contracts
   (deterministic, about 1 minute). The live run is the product signal: nightly, plus after every
   merge.
4. **Never mutate production.** No orders are created; checkout stops at `orders/preview`.
5. **Contracts are versioned artefacts.** JSON schemas and shape baselines live in the repo, and
   drift is proposed for review, never auto-accepted.

## Coverage map

### Search (risk: highest traffic, entry point of every booking)

| Idea | Layer | Status |
|---|---|---|
| Autocomplete returns the typed city first, in the right country (data-driven, 5 markets) | autocomplete JSON | **Implemented** |
| Autocomplete response contract (schema) and shape-drift baseline | autocomplete JSON | **Implemented** |
| Query normalisation (case, whitespace) resolves to the same destination | autocomplete JSON | **Implemented** |
| `size` parameter honoured | autocomplete JSON | **Implemented** |
| Multilingual queries (Arabic script, emoji) | autocomplete JSON | **Implemented** |
| Edge queries (empty, 1 char, 512 chars) and malformed JSON: never 5xx | autocomplete JSON | **Implemented** |
| Injection payloads treated as text (XSS, SQLi, SSTI, path traversal) | autocomplete + results page | **Implemented** |
| Results page served for valid criteria; invalid criteria (past dates, checkout before check-in, 0/999 adults, 400-night stay) never 5xx | HTML over HTTP | **Implemented** |
| Latency objectives for typing (autocomplete) and results (page) | both | **Implemented** (non-blocking category) |
| Typo tolerance ("Kairo", "Amsterdm") ranks the intended city top 3 | autocomplete | Backlog |
| Disambiguation (Alexandria EG vs Alexandria US, Paris FR vs Paris TX) ranks by locale | autocomplete | Backlog |
| Filters and sort (price, stars, distance) applied consistently: every result satisfies the filter | Demand API search | Backlog (needs credentials) |
| Pagination: no duplicates or gaps across pages | Demand API search | **Implemented** (mock/sandbox) |

### Checkout (risk: revenue and trust)

| Idea | Layer | Status |
|---|---|---|
| Auth gate: search and order preview refused without, or with invalid, credentials; token never echoed; no stack traces | Demand API (live) | **Implemented** |
| Price integrity: availability price == order preview total, same currency | Demand API | **Implemented** (mock/sandbox) |
| Unknown product rejected at preview, never silently priced | Demand API | **Implemented** (mock/sandbox) |
| Past check-in rejected at availability | Demand API | **Implemented** (mock/sandbox) |
| Total >= base price (charges only add) | Demand API | **Implemented** (mock/sandbox) |
| Currency conversion: booker vs accommodation currency consistent with the published rate | Demand API | Backlog |
| Cancellation policy at preview equals policy at availability (free cancellation must not silently become non-refundable) | Demand API | Backlog |
| Children allocation and occupancy limits | Demand API | Backlog |
| Idempotency of order creation (double submit does not double book), sandbox only | Demand API sandbox | Backlog |
| Concurrency: last room booked by two clients, exactly one succeeds | Demand API sandbox | Backlog |

### Platform

| Idea | Status |
|---|---|
| HSTS >= 1 year, clickjacking protection, no server version disclosure, HTTP to HTTPS redirect | **Implemented** |
| Rate limiting answers 429 with `Retry-After`, never 5xx | Backlog (needs an agreed load window) |
| Localisation: `lang`/currency parameters change labels and prices consistently | Backlog |

## How the suite is organised

* **TestNG groups = surfaces and intents.** Surfaces: `autocomplete`, `web`, `demand-auth`,
  `demand-partner`. Intents: `smoke`, `contract`, `negative`, `security`, `performance`. The
  planner switches surfaces on and off; intents label why a check exists and drive the smoke phase.
* **Cucumber mirrors the surfaces with tags** (`@web`, `@autocomplete`, `@demand-partner`,
  `@demand-auth`), so one planner decision applies to both layers.
* **Clients own HTTP, tests own intent.** Tests never build URLs or headers. Every call goes through
  `SpecFactory`, which applies timeouts, browser-like headers and redacted Allure attachments.
* **Data is relative and external.** Dates are always relative to today, and destinations come from
  `testdata/destinations.json`.

## What is deliberately *not* automated

* Full end-to-end bookings on production: there is no safe and ethical way to do this.
* Visual and layout checks of the results page: they belong to a UI suite (see the sibling iOS
  project `BookingIOSNativeMobileAppiumMCP`).
* Load testing against booking.com: not ours to load. Latency objectives are observational.
