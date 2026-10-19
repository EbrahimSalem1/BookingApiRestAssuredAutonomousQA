# Autonomous QA run: booking.com (`live`)

**Verdict: FAIL**: 12 blocking failure(s): [FUNCTIONAL_DEFECT AutocompleteApiTest#cityQueryReturnsTheCityFirst, FUNCTIONAL_DEFECT AutocompleteApiTest#arabicScriptQueryIsSupported, FUNCTIONAL_DEFECT AutocompleteApiTest#queryIsNormalised, FUNCTIONAL_DEFECT AutocompleteApiTest#respondsWithinLatencyObjective, FUNCTIONAL_DEFECT AutocompleteApiTest#resultCountHonoursRequestedSize]

Started 2026-10-19T08:52:21Z, finished 2026-10-19T08:52:36Z.

## 1. Observe: exploration probes

| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |
|---|---|---|---|---|---|---|
| `autocomplete-city` | search | 5 | UNEXPECTED_STATUS | 403 | 547 | Expected [200] but got 403 |
| `autocomplete-unicode` | search | 3 | UNEXPECTED_STATUS | 403 | 251 | Expected [200] but got 403 |
| `autocomplete-empty` | search | 2 | UNEXPECTED_STATUS | 403 | 256 | Expected [200, 400, 422] but got 403 |
| `search-results-page` | search | 5 | BLOCKED | 202 | 444 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `search-results-past-dates` | search | 3 | BLOCKED | 202 | 291 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `front-door` | platform | 4 | BLOCKED | 202 | 263 | Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-search-anonymous` | checkout | 5 | HEALTHY | 401 | 343 | Matches baseline |
| `demand-orders-preview-anonymous` | checkout | 5 | HEALTHY | 401 | 180 | Matches baseline |
| `demand-search` | checkout | 5 | NOT_PROBED | - | - | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |
| `autocomplete-typo` | search | 3 | UNEXPECTED_STATUS | 403 | 258 | Expected [200] but got 403 |
| `autocomplete-ambiguous` | search | 3 | UNEXPECTED_STATUS | 403 | 237 | Expected [200] but got 403 |
| `autocomplete-localised` | search | 2 | UNEXPECTED_STATUS | 403 | 244 | Expected [200] but got 403 |
| `autocomplete-size-zero` | search | 2 | UNEXPECTED_STATUS | 403 | 222 | Expected [200, 400, 422] but got 403 |
| `autocomplete-size-huge` | search | 2 | UNEXPECTED_STATUS | 403 | 250 | Expected [200, 400, 422] but got 403 |
| `autocomplete-wrong-type` | search | 2 | UNEXPECTED_STATUS | 403 | 250 | Expected [200, 400, 422] but got 403 |
| `autocomplete-get-method` | search | 1 | UNEXPECTED_STATUS | 403 | 221 | Expected [200, 400, 404, 405] but got 403 |
| `demand-get-method-anonymous` | checkout | 2 | HEALTHY | 401 | 193 | Matches baseline |

<details><summary>Headers observed on the front door</summary>

- `strict-transport-security`: max-age=63072000; includeSubDomains; preload
- `server`: istio-envoy
- `x-cache`: Miss from cloudfront

</details>

## 2. Plan: decisions and reasons

**Surface health:** `autocomplete`=HEALTHY  `web`=BLOCKED  `demand-auth`=HEALTHY  `demand-partner`=NOT_PROBED  

**Risk ranking:** search (risk score 66) > checkout (risk score 17) > platform (risk score 8)

| Excluded group | Why |
|---|---|
| `web` | every probe was challenged by bot protection: Bot challenge page (HTTP 202, marker 'awswaf') |
| `demand-partner` | No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID) |

**Look here first:**

- [UNEXPECTED_STATUS] autocomplete-city (risk 5): Expected [200] but got 403
- [BLOCKED] search-results-page (risk 5): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [BLOCKED] front-door (risk 4): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [UNEXPECTED_STATUS] autocomplete-unicode (risk 3): Expected [200] but got 403
- [BLOCKED] search-results-past-dates (risk 3): Edge protection challenged the probe; checks on this surface are inconclusive from this network
- [UNEXPECTED_STATUS] autocomplete-typo (risk 3): Expected [200] but got 403
- [UNEXPECTED_STATUS] autocomplete-ambiguous (risk 3): Expected [200] but got 403
- [UNEXPECTED_STATUS] autocomplete-empty (risk 2): Expected [200, 400, 422] but got 403
- [UNEXPECTED_STATUS] autocomplete-localised (risk 2): Expected [200] but got 403
- [UNEXPECTED_STATUS] autocomplete-size-zero (risk 2): Expected [200, 400, 422] but got 403
- [UNEXPECTED_STATUS] autocomplete-size-huge (risk 2): Expected [200, 400, 422] but got 403
- [UNEXPECTED_STATUS] autocomplete-wrong-type (risk 2): Expected [200, 400, 422] but got 403
- [UNEXPECTED_STATUS] autocomplete-get-method (risk 1): Expected [200, 400, 404, 405] but got 403

## 3. Execute

- Phase `smoke`: 1 passed, 1 failed, 0 skipped (pass rate 50%, threshold 50%)
- Phase `regression`: 35 passed, 11 failed, 0 skipped, 0 transient retries

## 4. Triage

| Passed | Failed | Skipped | Retried |
|---|---|---|---|
| 36 | 12 | 0 | 0 |

| Status | Category | Blocking | Check | Reason |
|---|---|---|---|---|
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#cityQueryReturnsTheCityFirst` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#arabicScriptQueryIsSupported` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#queryIsNormalised` | [suggestions for 'Cairo']  |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#respondsWithinLatencyObjective` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#resultCountHonoursRequestedSize` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#topSuggestionIsTheRightCityInTheRightCountry[Destination[name=Cairo, expectedInLabel=Egypt, countryCode=eg, demandCityId=-290692]]` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#topSuggestionIsTheRightCityInTheRightCountry[Destination[name=Paris, expectedInLabel=France, countryCode=fr, demandCityId=-1456928]]` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#topSuggestionIsTheRightCityInTheRightCountry[Destination[name=Amsterdam, expectedInLabel=Netherlands, countryCode=nl, demandCityId=-2140479]]` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#topSuggestionIsTheRightCityInTheRightCountry[Destination[name=Dubai, expectedInLabel=United Arab Emirates, countryCode=ae, demandCityId=-782831]]` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `AutocompleteApiTest#topSuggestionIsTheRightCityInTheRightCountry[Destination[name=Tokyo, expectedInLabel=Japan, countryCode=jp, demandCityId=-246227]]` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `Scenario: The search box suggests the city I typed first` | 1 expectation failed. |
| FAILED | FUNCTIONAL_DEFECT | yes | `Scenario: The search box suggests the city I typed first` | 1 expectation failed. |

## 5. Heal: contract baselines

| Probe | Finding | Proposed baseline | Accepted |
|---|---|---|---|
| `autocomplete-city` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-city.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-unicode` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-unicode.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-empty` | BREAKING drift, removed [$.results:array], added [$.message:string, $.servlet:string, $.status:string, $.url:string] | `target/autonomous/healing/live/autocomplete-empty.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-typo` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-typo.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-ambiguous` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-ambiguous.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-localised` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-localised.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-size-zero` | BREAKING drift, removed [$.results:array], added [$.message:string, $.servlet:string, $.status:string, $.url:string] | `target/autonomous/healing/live/autocomplete-size-zero.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-size-huge` | BREAKING drift, removed [$.results:array], added [$.message:string, $.servlet:string, $.status:string, $.url:string] | `target/autonomous/healing/live/autocomplete-size-huge.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-wrong-type` | BREAKING drift, removed [$.results:array, $.results[].b_max_los_data.defaultLos:number, $.results[].b_max_los_data.experiment:string, $.results[].b_max_los_data.extendedLos:number, $.results[].b_max_los_data.fullOn:boole... | `target/autonomous/healing/live/autocomplete-wrong-type.json` | no (review, then rerun with -Dagent.heal.accept=true) |
| `autocomplete-get-method` | BREAKING drift, removed [$.results:array], added [$.message:string, $.servlet:string, $.status:string, $.url:string] | `target/autonomous/healing/live/autocomplete-get-method.json` | no (review, then rerun with -Dagent.heal.accept=true) |

## 6. AI advisor (advisory only, does not change the verdict)

_Not configured: set `ANTHROPIC_API_KEY` to enable._
