# AI-assisted development and code review

The brief allows any AI tool and asks to *show what the tools produced and review it*. This page
lists both.

## Which AI was used, and for what

| Tool | Used for | Kept | Changed after review |
|---|---|---|---|
| **Claude (Claude Code agent)** | Scaffolding the Maven project, clients, models, the autonomous engine, WireMock stubs, schemas, workflows, first drafts of the docs | Architecture, layering, most code | See the findings below |
| **Claude API at runtime** (`LlmAdvisor`, optional) | Second-opinion triage and next exploratory charters inside the agent report | Advisory section only | Hard rule: cannot change the verdict and never receives secrets or bodies |
| **The autonomous agent itself** | The first live run produced the observations and proposed contract baselines | Live baselines in `src/main/resources/autonomous/baselines/live` | Accepted after human review, not automatically |

The code was generated, and every finding below was caught by review (self-review, compiler
lint, or evidence from the first live run). This shows why AI output needs a reviewer: most of
these defects compile and look plausible.

## Review findings: fixed

| # | Where | Finding | How it was caught | Fix |
|---|---|---|---|---|
| 1 | `RedactingAllureFilter` | Generated `.collectors()`: a non-existent Stream method | Read-through before the first build | `collect(Collectors.joining("\n"))` |
| 2 | `RunLedger.countByCategory` | Over-engineered `Function.identity().andThen(...)::apply` classifier | Review for readability | `groupingBy(TestOutcome::category, counting())` |
| 3 | `BotProtectionDetector` | A JSON `403 {"errors":[{"id":"access_denied"}]}` from the Demand API matched the "access denied" bot marker, so a real auth answer would have been skipped as `ENV_BLOCKED` (a false *inconclusive*) | Reasoning about the Demand API error contract | JSON bodies are never treated as edge challenges, with a unit test (`applicationResponsesAreNotFlagged`) |
| 4 | `AutonomousTestListener` | A skip raised in `@BeforeMethod` reaches the listener with a **null throwable**, so it was triaged `UNCLASSIFIED` | Walking the TestNG lifecycle | Null-throwable skips are triaged `PRECONDITION_NOT_MET` |
| 5 | `AutocompleteApiTest` | `results.get(0)` on an empty list throws `IndexOutOfBoundsException`, which triage reports as `TEST_DEFECT` instead of the real `FUNCTIONAL_DEFECT` ("no suggestions") | Thinking about how triage reads each failure | `top()` helper asserts non-empty first |
| 6 | `DemandApiClient` | The Demand autocomplete path differs between the v3.1 and v3.2-beta docs. The generated code guessed one | Cross-checking the docs | Removed: no unverifiable endpoints |
| 7 | `RedactingAllureFilter` | Redundant cast flagged by `-Xlint:all` in CI | CI compiler warning | `req.<Object>getBody()` |
| 8 | `DemandApiAuthTest` | Assumed the documented JSON error envelope on 401. **Live evidence: the 401 comes from the edge (envoy/CloudFront) with an empty body** | First live run (`qa-reports/live/report.json`) | The schema is checked only when the body is JSON. The difference from the docs is recorded in the exploratory notes |
| 9 | `ApiExplorer` | An empty body was fingerprinted as `$:string` (Jackson `MissingNode`), a misleading baseline | Reviewing the first healing proposals | Empty bodies are fingerprinted as `$:empty` |
| 10 | `QaConfig` | Env-var precedence meant a real `DEMAND_TOKEN` in the shell was sent to the **mock** server | Review of the config resolution order | Credentials are read from `mock.properties` only when `qa.env=mock` |
| 11 | `ApiExplorer` | HTML probes said "No baseline yet", which suggested missing work | Reading the first agent report | Note now says "HTML surface (no JSON shape)" |

## Review findings: accepted as known trade-offs

| Finding | Why it stays (for now) |
|---|---|
| Mock validation stubs use WireMock relative dates (`before: now +45 days`), coupled to `TestData.checkin()` = today + 45 | Keeps the mock stateless. A change of the offset breaks two stubs loudly in the PR gate, so it cannot fail silently |
| `RunLedger` is a static in-memory singleton | Correct for one JVM. Forked or parallel suites would need a file-based ledger |
| `TransientRetryAnalyzer` back-off sleeps on the test thread | Simple and bounded (max 2 retries, 2 s and 4 s). Acceptable for an API suite |
| Live autocomplete baseline contains experiment fields (`b_max_los_data.experiment`) | They are part of the real contract today. Additive drift is non-blocking, and only removals are flagged as breaking |
| Results-page relevance check is "page mentions the destination" | The page is WAF-challenged from CI networks. Deeper HTML parsing would only run locally |
| Demand API positive checks are verified against the mock, not production | Needs partner credentials. They run unchanged on `sandbox` once `DEMAND_TOKEN`/`DEMAND_AFFILIATE_ID` are configured |

## Review checklist applied

- [x] No secrets in code, config or reports (redaction filter, env-only secrets)
- [x] No production side effects (checkout stops at `orders/preview`)
- [x] Every non-pass carries a category and reason. No silent catch-and-pass
- [x] Retries limited to transient failures
- [x] Deterministic PR gate (mock), with the live run as a separate signal
- [x] Unit tests for the decision logic (triage, detection, drift, planning, verdict)
- [x] Compiler lint enabled (`-Xlint:all`) and warnings addressed
