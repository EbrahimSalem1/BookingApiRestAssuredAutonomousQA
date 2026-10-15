# Autonomous QA run: booking.com (`live`)

**Verdict: FAIL**: 5 blocking failure(s): [FUNCTIONAL_DEFECT AutocompleteApiTest#arabicScriptQueryIsSupported, FUNCTIONAL_DEFECT AutocompleteApiTest#queryIsNormalised, FUNCTIONAL_DEFECT AutocompleteApiTest#resultCountHonoursRequestedSize, FUNCTIONAL_DEFECT AutocompleteApiTest#topSuggestionIsTheRightCityInTheRightCountry[Destination[name=Dubai, expectedInLabel=United Arab Emirates, countryCode=ae, demandCityId=-782831]], FUNCTIONAL_DEFECT Scenario: The search box suggests the city I typed first]

Started 2026-10-15T08:17:47Z, finished 2026-10-15T08:18:13Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | UNEXPECTED_STATUS | 403 | 449 | Expected [200] but got 403 |
| `autocomplete-unicode` | search | 3 | HEALTHY | 200 | 254 | Matches baseline |
| `autocomplete-empty` | search | 2 | UNEXPECTED_STATUS | 403 | 221 | Expected [200, 400, 422] but got 403 |
| `search-results-page` | search | 5 | HEALTHY | 200 | 1919 | HTML surface (status, headers and latency tracked; no JSON shape) |
| `search-results-past-dates` | search | 3 | BLOCKED | 202 | 282 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `front-door` | platform | 4 | BLOCKED | 202 | 63 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 807 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 695 | Matches baseline |
| `demand-search` | checkout | 5 | NOT_PROBED | - | - | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |
| `autocomplete-typo` | search | 3 | UNEXPECTED_STATUS | 403 | 220 | Expected [200] but got 403 |
| `autocomplete-ambiguous` | search | 3 | UNEXPECTED_STATUS | 410 | 255 | Expected [200] but got 410 |
| `autocomplete-localised` | search | 2 | UNEXPECTED_STATUS | 403 | 217 | Expected [200] but got 403 |
| `autocomplete-size-zero` | search | 2 | HEALTHY | 200 | 240 | Matches baseline |
| `autocomplete-size-huge` | search | 2 | HEALTHY | 200 | 742 | Matches baseline |
| `autocomplete-wrong-type` | search | 2 | UNEXPECTED_STATUS | 410 | 258 | Expected [200, 400, 422] but got 410 |
| `autocomplete-get-method` | search | 1 | UNEXPECTED_STATUS | 410 | 208 | Expected [200, 400, 404, 405] but got 410 |
| `demand-get-method-anonymous` | checkout | 2 | HEALTHY | 401 | 683 | Matches baseline |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `server`: CloudFront
- `x-cache`: Miss from cloudfront

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=DEGRADED  `demand-auth`=HEALTHY  `demand-partner`=NOT_PROBED  

**Risk ranking:** search (risk score 54) > checkout (risk score 17) > platform (risk score 8)

| Excluded group | Why |
|---|---|
| `demand-partner` | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |

**Look here first:**

- [UNEXPECTED_STATUS] autocomplete-city (risk 5): Expected [200] but got 403
- [BLOCKED] front-door (risk 4): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] search-results-past-dates (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [UNEXPECTED_STATUS] autocomplete-typo (risk 3): Expected [200] but got 403
- [UNEXPECTED_STATUS] autocomplete-ambiguous (risk 3): Expected [200] but got 410
- [UNEXPECTED_STATUS] autocomplete-empty (risk 2): Expected [200, 400, 422] but got 403
- [UNEXPECTED_STATUS] autocomplete-localised (risk 2): Expected [200] but got 403
- [UNEXPECTED_STATUS] autocomplete-wrong-type (risk 2): Expected [200, 400, 422] but got 410
- [UNEXPECTED_STATUS] autocomplete-get-method (risk 1): Expected [200, 400, 404, 405] but got 410

## 3. Execute

- Phase `smoke`: 3 passed, 0 failed, 1 skipped (pass rate 100%, threshold 50%)
- Phase `regression`: 49 passed, 5 failed, 6 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 52 | 5 | 7 | 0 |

| Status | Category | Blocking | Check | Reason |
|---|---|---|---|---|
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#arabicScriptQueryIsSupported` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#queryIsNormalised` | [suggestions for '  cAiRo  ']  |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#resultCountHonoursRequestedSize` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#topSuggestionIsTheRightCityInTheRightCountry[Destination[name=Dubai, expectedInLabel=United Arab Emirates, countryCode=ae, demandCityId=-782831]]` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `Scenario: The search box suggests the city I typed first` | 1 expectation failed. |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `WebPlatformSecurityTest#strictTransportSecurityIsEnforced` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#invalidCriteriaAreHandledGracefully[checkout before checkin, 'Cairo' 2026-11-29 -> 2026-11-27, 2 adults]` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#invalidCriteriaAreHandledGracefully[empty destination, '' 2026-11-29 -> 2026-12-01, 2 adults]` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `SearchResultsPageTest#malformedParametersAreHandledGracefully` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `WebPlatformSecurityTest#framingIsRestricted` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `WebPlatformSecurityTest#serverVersionIsNotDisclosed` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |
| SKIPPED | ENVIRONMENT_BLOCKED | no | `Scenario: Searching a destination serves its results page` | [ENV_BLOCKED] booking.com edge protection answered instead of the application: Bot challenge page (HTTP 202, marker 'awswaf') |

## 5. Heal: contract baselines

| Probe | Finding | Proposed baseline | Accepted |
|---|---|---|---|
| `autocomplete-city` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-city.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-empty` | BREAKING drift, removed [$.results:array], added [$.message:string, $.servlet:string, $.status:string, $.url:string] | `target/autonomous/healing/live/autocomplete-empty.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-typo` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-typo.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-ambiguous` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-ambiguous.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-localised` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-localised.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-wrong-type` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-wrong-type.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-get-method` | BREAKING drift, removed [$.results:array], added [$.message:string, $.servlet:string, $.status:string, $.url:string] | `target/autonomous/healing/live/autocomplete-get-method.json` | no (review, then rerun with -Dagent.heal.accept=true) |

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
