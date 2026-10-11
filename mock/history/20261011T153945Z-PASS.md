# Autonomous QA run: booking.com (`mock`)

**Verdict: PASS**: All executed checks passed

Started 2026-10-11T15:39:37Z, finished 2026-10-11T15:39:45Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | HEALTHY | 200 | 377 | Matches baseline |
| `autocomplete-unicode` | search | 3 | HEALTHY | 200 | 16 | Matches baseline |
| `autocomplete-empty` | search | 2 | HEALTHY | 200 | 13 | Matches baseline |
| `search-results-page` | search | 5 | HEALTHY | 200 | 43 | No baseline yet |
| `search-results-past-dates` | search | 3 | HEALTHY | 200 | 12 | No baseline yet |
| `front-door` | platform | 4 | HEALTHY | 200 | 13 | No baseline yet |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 10 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 16 | Matches baseline |
| `demand-search` | checkout | 5 | HEALTHY | 200 | 33 | Matches baseline |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `x-content-type-options`: nosniff
- `x-frame-options`: SAMEORIGIN
- `server`: nginx

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=HEALTHY  `demand-auth`=HEALTHY  `demand-partner`=HEALTHY  

**Risk ranking:** search (risk score 18) > checkout (risk score 15) > platform (risk score 4)

No surface excluded.

## 3. Execute

- Phase `smoke`: 6 passed, 0 failed, 0 skipped (pass rate 100%, threshold 50%)
- Phase `regression`: 65 passed, 0 failed, 1 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 71 | 0 | 1 | 0 |

| Status | Category | Blocking | Check | Reason |
|---|---|---|---|---|
| SKIPPED | PRECONDITION_NOT_MET | no | `WebPlatformSecurityTest#plainHttpRedirectsToHttps` | Skipped: [NOT_APPLICABLE] HTTP->HTTPS redirect only meaningful against the real host (base http://localhost:45353) |

## 5. Heal: contract baselines

No contract drift and no missing baselines.

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
