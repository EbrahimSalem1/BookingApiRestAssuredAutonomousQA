# Autonomous QA run: booking.com (`live`)

**Verdict: INCONCLUSIVE**: Everything that ran passed, but at least one surface could not be exercised from this network: [web, demand-partner]

Started 2026-10-11T15:44:50Z, finished 2026-10-11T15:45:10Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | HEALTHY | 200 | 699 | Matches baseline |
| `autocomplete-unicode` | search | 3 | HEALTHY | 200 | 670 | Matches baseline |
| `autocomplete-empty` | search | 2 | HEALTHY | 200 | 248 | Matches baseline |
| `search-results-page` | search | 5 | BLOCKED | 202 | 192 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `search-results-past-dates` | search | 3 | BLOCKED | 202 | 287 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `front-door` | platform | 4 | BLOCKED | 202 | 267 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 977 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 361 | Matches baseline |
| `demand-search` | checkout | 5 | NOT_PROBED | - | - | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |
| `autocomplete-typo` | search | 3 | HEALTHY | 200 | 224 | No baseline yet |
| `autocomplete-ambiguous` | search | 3 | HEALTHY | 200 | 230 | No baseline yet |
| `autocomplete-localised` | search | 2 | HEALTHY | 200 | 242 | No baseline yet |
| `autocomplete-size-zero` | search | 2 | HEALTHY | 200 | 234 | No baseline yet |
| `autocomplete-size-huge` | search | 2 | HEALTHY | 200 | 724 | No baseline yet |
| `autocomplete-wrong-type` | search | 2 | HEALTHY | 200 | 658 | No baseline yet |
| `autocomplete-get-method` | search | 1 | UNEXPECTED_STATUS | 200 | 227 | Expected [400, 404, 405] but got 200 |
| `demand-get-method-anonymous` | checkout | 2 | HEALTHY | 401 | 898 | No baseline yet |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `server`: istio-envoy
- `x-cache`: Miss from cloudfront

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=BLOCKED  `demand-auth`=HEALTHY  `demand-partner`=NOT_PROBED  

**Risk ranking:** search (risk score 42) > checkout (risk score 17) > platform (risk score 8)

| Excluded group | Why |
|---|---|
| `web` | every probe was challenged by bot protection: Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-partner` | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |

**Look here first:**

- [BLOCKED] search-results-page (risk 5): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] front-door (risk 4): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] search-results-past-dates (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [UNEXPECTED_STATUS] autocomplete-get-method (risk 1): Expected [400, 404, 405] but got 200

## 3. Execute

- Phase `smoke`: 2 passed, 0 failed, 0 skipped (pass rate 100%, threshold 50%)
- Phase `regression`: 46 passed, 0 failed, 0 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 48 | 0 | 0 | 0 |

All executed checks passed.

## 5. Heal: contract baselines

| Probe | Finding | Proposed baseline | Accepted |
|---|---|---|---|
| `autocomplete-typo` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-typo.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-ambiguous` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-ambiguous.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-localised` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-localised.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-size-zero` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-size-zero.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-size-huge` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-size-huge.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-wrong-type` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-wrong-type.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-get-method` | no baseline recorded yet | `target/autonomous/healing/live/autocomplete-get-method.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `demand-get-method-anonymous` | no baseline recorded yet | `target/autonomous/healing/live/demand-get-method-anonymous.json` | no (review, then rerun with -Dagent.heal.accept=true) |

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
