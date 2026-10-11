# Autonomous QA run: booking.com (`live`)

**Verdict: INCONCLUSIVE**: A critical-path (smoke) check was blocked by bot protection, or too few checks could exercise the product; see triage and excluded surfaces [demand-partner]

Started 2026-10-11T15:51:44Z, finished 2026-10-11T15:52:08Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | HEALTHY | 200 | 627 | Matches baseline |
| `autocomplete-unicode` | search | 3 | HEALTHY | 200 | 244 | Matches baseline |
| `autocomplete-empty` | search | 2 | HEALTHY | 200 | 230 | Matches baseline |
| `search-results-page` | search | 5 | HEALTHY | 200 | 1354 | HTML surface (status, headers and latency tracked; no JSON shape) |
| `search-results-past-dates` | search | 3 | HEALTHY | 200 | 439 | HTML surface (status, headers and latency tracked; no JSON shape) |
| `front-door` | platform | 4 | BLOCKED | 202 | 282 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 890 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 350 | Matches baseline |
| `demand-search` | checkout | 5 | NOT_PROBED | - | - | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |
| `autocomplete-typo` | search | 3 | HEALTHY | 200 | 240 | Matches baseline |
| `autocomplete-ambiguous` | search | 3 | HEALTHY | 200 | 231 | Matches baseline |
| `autocomplete-localised` | search | 2 | HEALTHY | 200 | 221 | Matches baseline |
| `autocomplete-size-zero` | search | 2 | HEALTHY | 200 | 237 | Matches baseline |
| `autocomplete-size-huge` | search | 2 | HEALTHY | 200 | 711 | Matches baseline |
| `autocomplete-wrong-type` | search | 2 | HEALTHY | 200 | 233 | Matches baseline |
| `autocomplete-get-method` | search | 1 | HEALTHY | 200 | 244 | Matches baseline |
| `demand-get-method-anonymous` | checkout | 2 | HEALTHY | 401 | 613 | Matches baseline |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `server`: istio-envoy
- `x-cache`: Miss from cloudfront

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=DEGRADED  `demand-auth`=HEALTHY  `demand-partner`=NOT_PROBED  

**Risk ranking:** search (risk score 33) > checkout (risk score 17) > platform (risk score 8)

| Excluded group | Why |
|---|---|
| `demand-partner` | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |

**Look here first:**

- [BLOCKED] front-door (risk 4): Edge protection challenged the probe; checks on this surface are inconclusive from this network

## 3. Execute

- Phase `smoke`: 2 passed, 0 failed, 2 skipped (pass rate 100%, threshold 50%)
- Phase `regression`: 51 passed, 0 failed, 9 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 53 | 0 | 11 | 0 |

| Status | Category | Blocking | Check | Reason |
|---|---|---|---|---|
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#validSearchReturnsResultsPage` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `WebPlatformSecurityTest#strictTransportSecurityIsEnforced` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#destinationIsNotReflectedAsMarkup` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#invalidCriteriaAreHandledGracefully[checkout before checkin, 'Cairo' 2026-11-25 -> 2026-11-23, 2 adults]` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#invalidCriteriaAreHandledGracefully[zero nights, 'Cairo' 2026-11-25 -> 2026-11-25, 2 adults]` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#invalidCriteriaAreHandledGracefully[check-in in the past, 'Cairo' 2025-10-11 -> 2025-10-13, 2 adults]` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#invalidCriteriaAreHandledGracefully[absurd party size, 'Cairo' 2026-11-25 -> 2026-11-27, 999 adults]` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#malformedParametersAreHandledGracefully` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `WebPlatformSecurityTest#framingIsRestricted` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `WebPlatformSecurityTest#serverVersionIsNotDisclosed` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `Scenario: Searching a destination serves its results page` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |

## 5. Heal: contract baselines

No contract drift and no missing baselines.

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
