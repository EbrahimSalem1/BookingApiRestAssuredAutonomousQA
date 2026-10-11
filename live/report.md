# Autonomous QA run: booking.com (`live`)

**Verdict: INCONCLUSIVE**: Everything that ran passed, but at least one surface could not be exercised from this network: [web, demand-partner]

Started 2026-10-11T16:00:44Z, finished 2026-10-11T16:00:59Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | HEALTHY | 200 | 568 | Matches baseline |
| `autocomplete-unicode` | search | 3 | HEALTHY | 200 | 230 | Matches baseline |
| `autocomplete-empty` | search | 2 | HEALTHY | 200 | 212 | Matches baseline |
| `search-results-page` | search | 5 | BLOCKED | 202 | 187 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `search-results-past-dates` | search | 3 | BLOCKED | 202 | 91 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `front-door` | platform | 4 | BLOCKED | 202 | 91 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 343 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 166 | Matches baseline |
| `demand-search` | checkout | 5 | NOT_PROBED | - | - | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |
| `autocomplete-typo` | search | 3 | HEALTHY | 200 | 206 | Matches baseline |
| `autocomplete-ambiguous` | search | 3 | HEALTHY | 200 | 217 | Matches baseline |
| `autocomplete-localised` | search | 2 | HEALTHY | 200 | 203 | Matches baseline |
| `autocomplete-size-zero` | search | 2 | HEALTHY | 200 | 227 | Matches baseline |
| `autocomplete-size-huge` | search | 2 | HEALTHY | 200 | 726 | Matches baseline |
| `autocomplete-wrong-type` | search | 2 | HEALTHY | 200 | 219 | Matches baseline |
| `autocomplete-get-method` | search | 1 | HEALTHY | 200 | 236 | Matches baseline |
| `demand-get-method-anonymous` | checkout | 2 | HEALTHY | 401 | 172 | Matches baseline |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `server`: CloudFront
- `x-cache`: Miss from cloudfront

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=BLOCKED  `demand-auth`=HEALTHY  `demand-partner`=NOT_PROBED  

**Risk ranking:** search (risk score 41) > checkout (risk score 17) > platform (risk score 8)

| Excluded group | Why |
|---|---|
| `web` | every probe was challenged by bot protection: Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-partner` | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |

**Look here first:**

- [BLOCKED] search-results-page (risk 5): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] front-door (risk 4): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] search-results-past-dates (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network

## 3. Execute

- Phase `smoke`: 2 passed, 0 failed, 0 skipped (pass rate 100%, threshold 50%)
- Phase `regression`: 46 passed, 0 failed, 0 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 48 | 0 | 0 | 0 |

All executed checks passed.

## 5. Heal: contract baselines

No contract drift and no missing baselines.

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
