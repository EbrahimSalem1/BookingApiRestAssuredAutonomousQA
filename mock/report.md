# Autonomous QA run: booking.com (`mock`)

**Verdict: PASS**: All executed checks passed

Started 2026-10-25T11:46:49Z, finished 2026-10-25T11:46:55Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | HEALTHY | 200 | 258 | Matches baseline |
| `autocomplete-unicode` | search | 3 | HEALTHY | 200 | 12 | Matches baseline |
| `autocomplete-empty` | search | 2 | HEALTHY | 200 | 13 | Matches baseline |
| `search-results-page` | search | 5 | HEALTHY | 200 | 20 | HTML surface (status, headers and latency tracked; no JSON shape) |
| `search-results-past-dates` | search | 3 | HEALTHY | 200 | 9 | HTML surface (status, headers and latency tracked; no JSON shape) |
| `front-door` | platform | 4 | HEALTHY | 200 | 9 | HTML surface (status, headers and latency tracked; no JSON shape) |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 11 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 9 | Matches baseline |
| `demand-search` | checkout | 5 | HEALTHY | 200 | 23 | Matches baseline |
| `autocomplete-typo` | search | 3 | HEALTHY | 200 | 9 | No baseline yet |
| `autocomplete-ambiguous` | search | 3 | HEALTHY | 200 | 9 | No baseline yet |
| `autocomplete-localised` | search | 2 | HEALTHY | 200 | 9 | No baseline yet |
| `autocomplete-size-zero` | search | 2 | HEALTHY | 200 | 9 | No baseline yet |
| `autocomplete-size-huge` | search | 2 | HEALTHY | 200 | 8 | No baseline yet |
| `autocomplete-wrong-type` | search | 2 | HEALTHY | 200 | 7 | No baseline yet |
| `autocomplete-get-method` | search | 1 | HEALTHY | 404 | 39 | No baseline yet |
| `demand-get-method-anonymous` | checkout | 2 | HEALTHY | 405 | 9 | No baseline yet |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `x-content-type-options`: nosniff
- `x-frame-options`: SAMEORIGIN
- `server`: nginx

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=HEALTHY  `demand-auth`=HEALTHY  `demand-partner`=HEALTHY  

**Risk ranking:** search (risk score 33) > checkout (risk score 17) > platform (risk score 4)

No surface excluded.

## 3. Execute

- Phase `smoke`: 6 passed, 0 failed, 0 skipped (pass rate 100%, threshold 50%)
- Phase `regression`: 66 passed, 0 failed, 1 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 72 | 0 | 1 | 0 |

| Status | Category | Blocking | Check | Reason |
|---|---|---|---|---|
| SKIPPED | PRECONDITION_NOT_MET | no | `WebPlatformSecurityTest#plainHttpRedirectsToHttps` | Skipped: [NOT_APPLICABLE] HTTP->HTTPS redirect only meaningful against the real host (base http://localhost:35925) |

## 5. Heal: contract baselines

| Probe | Finding | Proposed baseline | Accepted |
|---|---|---|---|
| `autocomplete-typo` | no baseline recorded yet | `target/autonomous/healing/mock/autocomplete-typo.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-ambiguous` | no baseline recorded yet | `target/autonomous/healing/mock/autocomplete-ambiguous.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-localised` | no baseline recorded yet | `target/autonomous/healing/mock/autocomplete-localised.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-size-zero` | no baseline recorded yet | `target/autonomous/healing/mock/autocomplete-size-zero.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-size-huge` | no baseline recorded yet | `target/autonomous/healing/mock/autocomplete-size-huge.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-wrong-type` | no baseline recorded yet | `target/autonomous/healing/mock/autocomplete-wrong-type.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `demand-get-method-anonymous` | no baseline recorded yet | `target/autonomous/healing/mock/demand-get-method-anonymous.json` | no (review, then rerun with -Dagent.heal.accept=true) |

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
