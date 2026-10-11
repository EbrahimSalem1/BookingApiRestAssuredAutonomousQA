# Autonomous QA run: booking.com (`live`)

**Verdict: INCONCLUSIVE**: Everything that ran passed, but at least one surface could not be exercised from this network: [web, demand-partner]

Started 2026-10-11T15:40:29Z, finished 2026-10-11T15:40:44Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | HEALTHY | 200 | 543 | No baseline yet |
| `autocomplete-unicode` | search | 3 | HEALTHY | 200 | 270 | No baseline yet |
| `autocomplete-empty` | search | 2 | HEALTHY | 200 | 233 | No baseline yet |
| `search-results-page` | search | 5 | BLOCKED | 202 | 101 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `search-results-past-dates` | search | 3 | BLOCKED | 202 | 53 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `front-door` | platform | 4 | BLOCKED | 202 | 47 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 204 | No baseline yet |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 167 | No baseline yet |
| `demand-search` | checkout | 5 | NOT_PROBED | - | - | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `server`: CloudFront
- `x-cache`: Miss from cloudfront

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=BLOCKED  `demand-auth`=HEALTHY  `demand-partner`=NOT_PROBED  

**Risk ranking:** search (risk score 26) > checkout (risk score 15) > platform (risk score 8)

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
- Phase `regression`: 45 passed, 0 failed, 0 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 47 | 0 | 0 | 0 |

All executed checks passed.

## 5. Heal: contract baselines

| Probe | Finding | Proposed baseline | Accepted |
|---|---|---|---|
| `autocomplete-city` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-city.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-unicode` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-unicode.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-empty` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-empty.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `demand-search-anonymous` | no baseline recorded yet | `target/autonomous/healing/live/demand-search-anonymous.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `demand-orders-preview-anonymous` | no baseline recorded yet | `target/autonomous/healing/live/demand-orders-preview-anonymous.json` | no (review, then rerun with -Dagent.heal.accept=true) |

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
