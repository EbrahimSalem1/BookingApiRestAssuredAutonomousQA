# Autonomous QA run: booking.com (`live`)

**Verdict: INCONCLUSIVE**: Everything that ran passed, but at least one surface could not be exercised from this network: [autocomplete, web, demand-partner]

Started 2026-10-30T10:00:12Z, finished 2026-10-30T10:00:19Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | BLOCKED | 403 | 780 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-unicode` | search | 3 | BLOCKED | 403 | 173 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-empty` | search | 2 | BLOCKED | 403 | 401 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `search-results-page` | search | 5 | BLOCKED | 202 | 151 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `search-results-past-dates` | search | 3 | BLOCKED | 202 | 81 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `front-door` | platform | 4 | BLOCKED | 202 | 52 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 255 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 111 | Matches baseline |
| `demand-search` | checkout | 5 | NOT_PROBED | - | - | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |
| `autocomplete-typo` | search | 3 | BLOCKED | 403 | 177 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-ambiguous` | search | 3 | BLOCKED | 403 | 173 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-localised` | search | 2 | BLOCKED | 403 | 169 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-size-zero` | search | 2 | BLOCKED | 403 | 454 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-size-huge` | search | 2 | BLOCKED | 403 | 161 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-wrong-type` | search | 2 | BLOCKED | 403 | 164 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `autocomplete-get-method` | search | 1 | BLOCKED | 403 | 163 | Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `demand-get-method-anonymous` | checkout | 2 | HEALTHY | 401 | 116 | Matches baseline |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `server`: CloudFront
- `x-cache`: Miss from cloudfront

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=BLOCKED  `web`=BLOCKED  `demand-auth`=HEALTHY  `demand-partner`=NOT_PROBED  

**Risk ranking:** search (risk score 66) > checkout (risk score 17) > platform (risk score 8)

| Excluded group | Why |
|---|---|
| `autocomplete` | every probe was challenged by bot protection: Caller refused: generic JSON 403 'Forbidden' via CloudFront (HTTP 403) |
| `web` | every probe was challenged by bot protection: Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-partner` | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |

**Look here first:**

- [BLOCKED] autocomplete-city (risk 5): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] search-results-page (risk 5): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] front-door (risk 4): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-unicode (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] search-results-past-dates (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-typo (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-ambiguous (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-empty (risk 2): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-localised (risk 2): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-size-zero (risk 2): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-size-huge (risk 2): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-wrong-type (risk 2): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] autocomplete-get-method (risk 1): Edge protection challenged the probe; checks on this surface are inconclusive from this network

## 3. Execute

- Phase `smoke`: 1 passed, 0 failed, 0 skipped (pass rate 100%, threshold 50%)
- Phase `regression`: 23 passed, 0 failed, 0 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 24 | 0 | 0 | 0 |

All executed checks passed.

## 5. Heal: contract baselines

No contract drift and no missing baselines.

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
