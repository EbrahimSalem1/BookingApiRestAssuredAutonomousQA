# BookingApiRestAssuredAutonomousQA

**Autonomous API QA system for booking.com.** Java 17, RestAssured, TestNG, Cucumber, WireMock,
Allure and GitHub Actions.

This is the Senior SDET technical challenge, re-targeted from the original site to **booking.com**
and from UI automation to the **API layer**, and built as an **autonomous QA system**. It doesn't
just run checks. It explores the target first, decides what is worth running, executes, triages
every non-pass, proposes contract healing and writes its own report, with a reason for every
decision.

| Deliverable (from the brief) | Where |
|---|---|
| Task 1: exploratory charters, observations, prioritisation rationale | [`docs/EXPLORATORY_ANALYSIS.md`](docs/EXPLORATORY_ANALYSIS.md) |
| Task 2: high-level automation ideas (coverage strategy) | [`docs/AUTOMATION_STRATEGY.md`](docs/AUTOMATION_STRATEGY.md) |
| Task 2: framework, structure, 3-5+ sample checks, reusable abstractions | `src/` (60+ checks incl. data-driven) |
| AI-generated output demonstrated and code-reviewed | [`docs/AI_CODE_REVIEW.md`](docs/AI_CODE_REVIEW.md) |
| README: setup, execution, architecture and rationale | this file |
| Executable from the terminal | `mvn test` / `mvn exec:java` |

---

## 1. Quick start

**Prerequisites:** JDK 17+ and Maven 3.9+. Nothing else is needed; WireMock runs in-process.

```bash
git clone https://github.com/EbrahimSalem1/BookingApiRestAssuredAutonomousQA.git
cd BookingApiRestAssuredAutonomousQA

# 1) Hermetic run against the contract mock (deterministic, about 1 min, no network needed)
mvn test -Dqa.env=mock

# 2) Live run against booking.com (default environment)
mvn test

# 3) The autonomous agent: explore -> plan -> execute -> triage -> heal -> report
mvn test-compile exec:java                 # live
mvn test-compile exec:java -Dqa.env=mock   # offline dry run

# Allure report
mvn allure:serve
```

### Useful switches

| Command | Effect |
|---|---|
| `-Dqa.env=live\|sandbox\|mock` | Target environment (default `live`) |
| `-Dsuite=smoke` | Smoke suite only (`src/test/resources/suites/smoke.xml`) |
| `-Dgroups=security` | Run one intent group across all surfaces (`smoke`, `contract`, `negative`, `security`, `performance`) |
| `-Dcucumber.filter.tags="@autocomplete"` | Filter BDD scenarios |
| `-Dguard.bot.block.policy=fail` | Treat a bot-protection challenge as a failure (for allow-listed networks) |
| `-Dagent.heal.accept=true` | Let the agent write proposed contract baselines into `src/main/resources` |

### Optional secrets (env vars or GitHub secrets)

| Variable | Unlocks |
|---|---|
| `DEMAND_TOKEN`, `DEMAND_AFFILIATE_ID` | Positive Booking.com Demand API checks (search, availability, checkout preview) on `live`/`sandbox`. Without them those checks are skipped as `[NO_CREDENTIALS]`; they always run on `mock`. |
| `ANTHROPIC_API_KEY` | The agent's optional AI advisor section in the report (advisory only). |

Secrets are never committed. Any config key can be overridden by a `-D` property or by an
environment variable (`demand.token` -> `DEMAND_TOKEN`).

---

## 2. What is tested (booking.com surfaces)

| Surface | Endpoint | Why it matters | Auth |
|---|---|---|---|
| **Destination autocomplete** | `POST accommodations.booking.com/autocomplete.json` | First call of every booking journey (the search box) | public |
| **Search results page** | `GET www.booking.com/searchresults.html` | Server-side search: criteria validation, reflection, latency | public |
| **Front door** | `GET www.booking.com/` | Transport security for every session (HSTS, framing, redirects) | public |
| **Demand API: auth gate** | `POST demandapi.booking.com/3.1/{accommodations/search, orders/preview}` | The gate in front of search and checkout must refuse anonymous callers | none (checks the refusal) |
| **Demand API: search & checkout** | `/accommodations/search`, `/accommodations/availability`, `/orders/preview` | Price integrity across the funnel, validation, pagination | partner credentials |

Booking.com has no public API for consumer checkout. The **official Booking.com Demand API**
(developers.booking.com) is the documented API behind search, availability and orders, so it is
the honest target for checkout automation. Orders are **never created**: the flow stops at
`orders/preview`.

---

## 3. Architecture

```
                     ┌──────────────────────── AutonomousQaAgent ───────────────────────────┐
                     │                                                                       │
 probes.yaml ──► 1 OBSERVE ──► 2 PLAN ──► 3 EXECUTE ─────────────► 4 TRIAGE ──► 5 HEAL ──► 6 REPORT
                 ApiExplorer   RiskBased   TestNgExecutor           FailureTriager  ContractDrift  AgentReport
                 (status,      Planner     phase 1: smoke           (rule-based,    Healer         (md + json +
                 latency, WAF, (surface    └ circuit breaker        deterministic)  (proposes      GitHub summary,
                 headers, JSON health,     phase 2: regression       + Verdict       baselines,     qa-reports branch,
                 shape drift)  exclusions, └ TransientRetryAnalyzer                  human accepts) issue on FAIL)
                               risk rank)                                                     + LlmAdvisor (optional,
                     │                                                                          advisory only)
                     └──────────────────────────────────────────────────────────────────────┘
                                                │ same suite, same listeners
                                                ▼
             TestNG checks (tests/web, tests/demand, tests/unit) + Cucumber (features/*.feature)
                                                │
                               Clients (AutocompleteClient, SearchResultsClient, DemandApiClient)
                                                │
                     SpecFactory (timeouts, headers, auth) + RedactingAllureFilter + EnvironmentGuard
                                                │
                    QaConfig ──► live: booking.com │ sandbox: Demand sandbox │ mock: in-process WireMock
```

### Project layout

```
src/main/java/.../bookingqa
├── config/            QaConfig (layered: -D > env var > <env>.properties > default), Environment
├── core/http/         SpecFactory (one spec per surface), RedactingAllureFilter (secrets never reach reports)
├── core/guard/        BotProtectionDetector (AWS WAF / captcha / edge), EnvironmentGuard (skip vs fail)
├── clients/           AutocompleteClient, SearchResultsClient, DemandApiClient
├── models/            Records for request/response bodies (Jackson)
├── data/              TestData (relative dates, destinations from JSON)
├── mock/              MockBookingServer (WireMock lifecycle)
└── autonomous/
    ├── explore/       ApiExplorer, Probe, Observation, ShapeFingerprint (JSON shape drift)
    ├── plan/          RiskBasedPlanner, ExecutionPlan
    ├── execute/       TestNgExecutor, AutonomousTestListener, TransientRetryAnalyzer, RetryTransformer
    ├── triage/        FailureTriager, FailureCategory
    ├── heal/          ContractDriftHealer
    ├── report/        AgentReport, Verdict, LedgerWriter
    ├── llm/           LlmAdvisor (Anthropic Messages API, optional)
    └── AutonomousQaAgent.java
src/main/resources/    config/*.properties, autonomous/probes.yaml, autonomous/baselines/<env>/
src/test/java/.../     tests/web, tests/demand, tests/unit (framework self-tests), bdd (runner, steps, context)
src/test/resources/    suites/*.xml, features/*.feature, schemas/**, testdata/, wiremock/{mappings,__files}
.github/workflows/     ci.yml (mock gate -> live agent), autonomous-qa.yml (nightly + on demand)
scripts/               publish-report.sh (qa-reports branch)
```

### Architectural decisions and rationale

| Decision | Why | Trade-off accepted |
|---|---|---|
| **API layer, not UI** | Search relevance, price integrity and auth are API facts. Checks run in seconds, are stable, and run on every commit. | Rendering and interaction are covered by the sibling iOS UI project, not here. |
| **Java 17 + RestAssured + TestNG** | The team's existing stack (Java/TestNG/Cucumber). RestAssured gives a fluent given/when/then, schema validation and filters. TestNG gives groups, data providers, listeners, retry hooks and a programmatic API the agent drives. | Playwright/TS was suggested in the brief, but the brief allows any stack that shows one's strengths. |
| **Cucumber on top of the same clients** | Business-readable scenarios for stakeholders without a second framework. Tags mirror TestNG groups so the planner controls both. | Two styles to maintain; kept small on purpose. |
| **Three environments: live / sandbox / mock** | `mock` is a hermetic PR gate (no flakiness, no network). `live` is the product signal. `sandbox` is the safe place for credentialed checkout. | Mocks can drift from reality. Mitigated by schemas plus live shape baselines that detect drift. |
| **Bot protection is a first-class outcome** | booking.com challenges automated traffic. Treating a challenge as a failure is a false alarm; treating it as a pass is a false green. `EnvironmentGuard` skips with `[ENV_BLOCKED]`, and the triage and verdict (`INCONCLUSIVE`) make it visible. | Coverage from datacenter IPs can be lower; the report says so explicitly. |
| **Rule-based triage, LLM only advisory** | Verdicts must be reproducible and explainable in a pipeline. An LLM adds root-cause hypotheses and next charters, but it cannot flip a verdict. | Less "magic"; more trust. |
| **Retry only transient failures** | Blind retries hide real bugs. Only network/429/502/503/504 are retried, with back-off. | A genuinely flaky product bug still surfaces, as it should. |
| **Self-healing with a human in the loop** | Shape drift produces a proposed baseline. Breaking drift is never auto-accepted. | One manual review step per contract change. |
| **Smoke circuit breaker** | If smoke is mostly red, the regression run would only add noise. | Fewer data points on a badly broken day, by design. |
| **Secrets redacted at the HTTP filter** | Allure attachments and logs are shareable artefacts; tokens never reach them. | Slightly less raw debugging detail. |
| **Relative dates, external data** | Hard-coded dates rot into accidental negative tests. | None. |

### Triage categories

| Category | Blocking | Retried | Meaning |
|---|---|---|---|
| `FUNCTIONAL_DEFECT` | yes | no | Business rule violated |
| `CONTRACT_DRIFT` | yes | no | Schema / shape changed |
| `SERVER_ERROR` | yes | no | 5xx on valid or invalid input |
| `TEST_DEFECT` | yes | no | Bug in automation code (NPE, cast...) |
| `PERFORMANCE_DEGRADATION` | no | no | Slower than objective |
| `TRANSIENT_INFRA` | no | yes | Network, 502/503/504 |
| `RATE_LIMITED` | no | yes | 429 |
| `ENVIRONMENT_BLOCKED` | no | no | Bot protection answered instead of the app |
| `PRECONDITION_NOT_MET` | no | no | Missing credentials / not applicable |

**Verdict:** `FAIL` if anything blocking failed. `INCONCLUSIVE` if most product checks could not
run, or a surface was unreachable or fully challenged. Otherwise `PASS`.

---

## 4. CI/CD

* **`ci.yml`** runs on every push and PR. The **quality gate** compiles, runs the full suite against
  the mock (framework self-tests first), runs the agent against the mock, and publishes triage. On
  success it calls the **live autonomous run**.
* **`autonomous-qa.yml`** runs nightly at 05:17 Cairo time, on demand (choose `live`, `sandbox` or
  `mock`), or when CI calls it. It publishes `report.md` / `report.json` to the **`qa-reports`
  branch** (latest plus history per environment) and **opens or updates a GitHub issue on FAIL**.
* Every run writes a Markdown summary to the GitHub Actions job summary.

---

## 5. Outputs of a run

| File | Content |
|---|---|
| `target/autonomous/report.md` | Human report: observations, plan with reasons, phases, triage, healing, AI advice |
| `target/autonomous/report.json` | Same, machine-readable |
| `target/autonomous/run-ledger.json` | Every check outcome with category, reason, HTTP status, attempt |
| `target/autonomous/healing/<env>/*.json` | Proposed contract baselines |
| `target/allure-results` | Allure results with redacted request/response attachments |
| `target/cucumber/index.html` | Cucumber HTML report |

### Latest results (2026-10-11)

| Run | Checks | Outcome |
|---|---|---|
| Quality gate, `mock` | 73 | 72 passed, 1 skipped (HTTP->HTTPS redirect is not applicable to a localhost mock). Agent verdict **PASS** |
| Autonomous, `live` | 48 executed + 17 probes | 48 passed. Verdict **INCONCLUSIVE**: the HTML pages are AWS-WAF-challenged from GitHub runners (`web` excluded with a reason), and there are no Demand API partner credentials (`demand-partner` excluded). Autocomplete and the Demand API auth gate are fully verified live. |

Every run's report is versioned on the [`qa-reports`](../../tree/qa-reports) branch
(`mock/`, `live/`: `report.md`, `report.json`, history, healing proposals).

---

## 6. Extending

* **New check:** add a method to a test class with the right groups. It inherits retry, triage,
  reporting and redaction automatically.
* **New surface:** add a client method, a probe in `probes.yaml` (target, area, risk), a mapping in
  `RiskBasedPlanner.TARGET_TO_GROUP`, and WireMock stubs.
* **New market:** add a line to `src/test/resources/testdata/destinations.json`.

---

*Submitted privately as requested by the brief. booking.com is used only as a publicly
accessible surface for skills assessment.*
