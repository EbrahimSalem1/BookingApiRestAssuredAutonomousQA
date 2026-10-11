# Exploratory analysis: booking.com Search and Checkout (API layer)

> Task 1 deliverable. Scope: **Search** and **Checkout**, explored at the HTTP/API layer.
> Evidence was collected on **2026-10-11** by the agent's explorer (`autonomous/probes.yaml`)
> from GitHub-hosted runners, plus a read of the public Booking.com Demand API documentation.
> Raw evidence for every run is published to the `qa-reports` branch (`live/latest.json`).
> booking.com is used only as a public surface for a skills assessment. Nothing here is a claim
> about the product's correctness.

---

## 1. Prioritisation: what I explored first, and why

| Order | Area | Risk reasoning |
|---|---|---|
| 1 | **Search entry point: destination autocomplete** | Every journey starts here. If it misunderstands the user, nothing downstream matters. It is also the one public JSON API, so it can be explored and automated without a browser. |
| 2 | **Checkout trust boundary: Demand API auth gate** | Search and orders sit behind credentials. A leaky gate is the highest-impact defect class (data exposure, fraudulent orders), and it can be verified *without* credentials. |
| 3 | **Search results page (server side)** | Validation of search criteria (dates, party size) and reflection of user input. It needs a browser-grade client, so the first question was whether automation can reach it at all. |
| 4 | **Checkout price integrity (search -> availability -> preview)** | Revenue-critical, but needs partner credentials. Explored from the documented contract and modelled in the mock until sandbox credentials exist. |
| 5 | **Transport security headers** | Cheap to observe, protects every session. |

The ordering principle was **impact x reachability**: start where a defect would hurt most *and*
where I can get evidence today. Then spend the remaining time on the hard-to-reach,
high-impact area (checkout), where the output is a contract model and charters rather than live
evidence.

---

## 2. Charters

Format: *Explore {target} with {resources} to discover {information}.*

| # | Charter | Time-box | Status |
|---|---|---|---|
| C1 | Explore **autocomplete** with real city names in 5 markets to discover whether the intended city ranks first and what contract the endpoint actually returns | 30 min | Done |
| C2 | Explore **autocomplete** with typos, ambiguous names, exonyms and non-Latin scripts to discover how forgiving and locale-aware ranking is | 30 min | Done |
| C3 | Explore **autocomplete** with boundary and malformed inputs (size 0/1000, wrong types, empty, 512 chars, wrong HTTP method, injection strings) to discover validation and robustness behaviour | 30 min | Done |
| C4 | Explore **the Demand API auth gate** with no token, an invalid token, and the wrong method to discover whether every route refuses anonymous callers and what it discloses | 20 min | Done |
| C5 | Explore **the search results page** with valid and invalid criteria to discover server-side validation, then reachability from automation networks | 20 min | Done (blocked, see O9) |
| C6 | Explore **checkout preview** with the documented contract to discover which invariants a test must protect (price, currency, policy, product identity) | 30 min | Modelled. Live run needs sandbox credentials |
| C7 | Explore **the results page in a real browser** from a residential network to discover client-side validation, filters and sort consistency | 45 min | Next session (see section 5) |

---

## 3. Observations

Severity labels show how much I would worry if each one were confirmed in production: **High**
(trust, money or security), **Medium** (user would be misled or blocked), **Low** (polish or
ambiguity).

### Search: destination autocomplete (`POST accommodations.booking.com/autocomplete.json`)

| # | Observation | Evidence | Type | Severity |
|---|---|---|---|---|
| O1 | **An oversized `size` returns an empty list instead of an error or a capped list.** `size: 1000` for "Paris" gave `200 {"results":[]}`. A client that asks for "many" gets "nothing", which is indistinguishable from "no such destination". | `autocomplete-size-huge` probe | Bug candidate / API design risk | Medium |
| O2 | `size: 0` also silently returns `200 {"results":[]}`. There is no validation error for either boundary. | `autocomplete-size-zero` | Ambiguity (undocumented contract) | Low |
| O3 | **No typo tolerance toward the dominant destination.** "Kairo" ranks *Kairouan, Tunisia* first. Cairo does not appear as the top match. This is a plausible conversion leak for a top destination. | `autocomplete-typo` | Usability / relevance risk | Medium |
| O4 | **Ambiguous names rank by something other than the booker's market.** "Alexandria" with `language: en-gb` ranks *Alexandria, Virginia, US* first. The request came from a GitHub-hosted (likely US) datacenter, so ranking may be IP-geo driven. Needs a controlled check from an Egyptian network before calling it a defect. | `autocomplete-ambiguous` | Ambiguity | Low/Medium |
| O5 | **Mixed-language labels.** `language: fr` for "Le Caire" returns "Le Caire, *Cairo Governorate*, Égypte": the city and country are localised, but the region is not. | `autocomplete-localised` | Localisation inconsistency | Low |
| O6 | Lenient typing: `query: 12345` (a number) is coerced and matched as a postcode ("Miraflores (12345), Lima"). This is reasonable, but it means type errors from clients are never surfaced. | `autocomplete-wrong-type` | Insight | Low |
| O7 | `GET /autocomplete.json` returns `200 {"results":[]}` rather than `405`. A wrong method looks like "no results". | `autocomplete-get-method` | API hygiene | Low |
| O8 | Positive: correct first result for Cairo, Paris, Amsterdam, Dubai and Tokyo. Normalises case and whitespace. Arabic script and emoji are handled. Injection strings come back as inert JSON. Empty, 1-char and 512-char queries never return 5xx. Typical latency is 220-850 ms from a GitHub-hosted runner. Responses carry HSTS (2 years, preload), `nosniff` and `SAMEORIGIN`. | `AutocompleteApiTest` (all passing live) | Insight | n/a |
| O8b | The real contract is much richer than what the UI needs (`b_max_los_data.experiment`, `labels[].hl`, `nr_homes`, `photo_uri`...). Experiment identifiers leak into a public payload, and these fields will churn. | Live shape baseline | Contract risk for consumers | Low |

### Search: results page (`GET www.booking.com/searchresults.html`)

| # | Observation | Evidence | Type | Severity |
|---|---|---|---|---|
| O9 | **Every HTML request from CI networks gets an AWS WAF challenge**: `HTTP 202`, ~8 KB page with the `awswaf` challenge script, `server: CloudFront`. This happens for the front door too. HSTS is still sent on the challenge. Consequence: server-side search validation **cannot be verified from CI**. The suite reports this as `ENVIRONMENT_BLOCKED` and the run as `INCONCLUSIVE`, never as a pass. | `search-results-page`, `front-door` probes | Test-environment constraint | n/a (testability) |
| O10 | The `Server` header on the front door alternates between `CloudFront` and `istio-envoy` across runs, which suggests different edge paths per request. Header-based checks must not assume one stack. | Two consecutive live reports | Insight | Low |

### Checkout: Demand API trust boundary (`demandapi.booking.com/3.1`)

| # | Observation | Evidence | Type | Severity |
|---|---|---|---|---|
| O11 | Positive: `accommodations/search`, `orders/preview` and a wrong-method `GET` all return **401 without credentials**. Authentication is checked *before* the method, so routes aren't enumerable. An invalid token is not echoed. No stack traces. | `DemandApiAuthTest` (passing live), `demand-get-method-anonymous` | Insight | n/a |
| O12 | **The 401 differs from the documented error contract.** The docs describe `{"request_id", "errors":[{"id","message"}]}`. The live 401 has an **empty body and no content type**, served by envoy with `x-cache: Error from cloudfront`. Clients that rely on `errors[].id` for auth failures will break, and support loses the `request_id`. | `demand-*-anonymous` probes (`$:empty` baseline) | Contract ambiguity | Medium |
| O13 | Checkout invariants worth protecting (from the documented flow): availability price == preview total; booker currency is consistent; the previewed product == the selected product; the cancellation policy is unchanged between availability and preview; an unknown product is rejected, not priced. The first four are automated against the mock and ready for sandbox. | `DemandCheckoutFlowTest`, `checkout.feature` | Risk model | High (if violated) |

### Risks and open questions to take to the team

1. **O1/O2:** Is the autocomplete `size` limit documented anywhere? If an internal client ever
   sends more than the hidden maximum, users see "no destinations". What is the maximum?
2. **O3:** Is typo tolerance intentionally conservative (precision over recall)? Do we have
   conversion data for misspelled top-50 destinations?
3. **O12:** Is the empty-body 401 intended, an edge-layer artefact, or a regression of the
   documented envelope?
4. **O9:** Is there an allow-listed egress or a test header for QA traffic? Without one, the
   HTML surfaces can only be tested from a browser on a residential network.

---

## 4. How the observations shaped the automation

| Observation | Automation decision |
|---|---|
| O8 | Autocomplete is the backbone of the live suite: data-driven ranking, contract, normalisation, robustness, injection and header checks run on every live build |
| O9 | `BotProtectionDetector` + `EnvironmentGuard` + planner exclusion + `INCONCLUSIVE` verdict: a blocked surface is never reported as green |
| O8b, O12 | Shape baselines per environment with drift detection, a human-accepted healing loop, and `$:empty` fingerprints for the edge 401 |
| O11 | Auth gate checks run live without credentials (no token, bad token, wrong method, no leakage) |
| O13 | Price-integrity flow test + BDD scenario against the contract mock, ready to run on sandbox |
| O1-O7 | Kept as **explorer probes** (observed on every run, shown in "Look here first" when behaviour changes) rather than blocking tests, until the intended behaviour is agreed |

---

## 5. Next session

* C7 in a real browser (residential IP): results-page filters and sort consistency, date picker
  boundaries, client-side vs server-side validation of party size.
* Sandbox credentials: run `DemandSearchTest` and `DemandCheckoutFlowTest` against
  `demandapi-sandbox.booking.com` and add the policy-consistency check (backlog in
  `AUTOMATION_STRATEGY.md`).
* Repeat O4 from an Egyptian network to separate locale ranking from IP-geo ranking.
