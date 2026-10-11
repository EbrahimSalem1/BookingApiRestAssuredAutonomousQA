#!/usr/bin/env bash
# Publishes the autonomous QA outputs of this run to the `qa-reports` branch, which acts as a
# living, versioned observation log (latest + history per environment).
# Usage: scripts/publish-report.sh <environment> <verdict>
# Needs: GH_TOKEN with contents:write, GITHUB_REPOSITORY, GITHUB_RUN_ID (all set in Actions).
set -euo pipefail

ENV_NAME="${1:?environment}"
VERDICT="${2:-UNKNOWN}"
SRC="target/autonomous"
[ -d "$SRC" ] || { echo "No $SRC directory, nothing to publish"; exit 0; }

remote="https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
work="$(mktemp -d)"
if ! git clone -q --depth 1 --branch qa-reports "$remote" "$work" 2>/dev/null; then
  git -C "$work" init -q -b qa-reports
  git -C "$work" remote add origin "$remote"
fi

dest="$work/$ENV_NAME"
mkdir -p "$dest/history"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
for f in report.md report.json triage-summary.md run-ledger.json verdict.txt; do
  [ -f "$SRC/$f" ] && cp "$SRC/$f" "$dest/$f"
done
[ -f "$SRC/report.md" ] && cp "$SRC/report.md" "$dest/history/${stamp}-${VERDICT}.md"
if [ -d "$SRC/healing" ]; then rm -rf "$dest/healing"; cp -r "$SRC/healing" "$dest/healing"; fi
[ -f target/surefire-reports/testng-results.xml ] && \
  grep -o '<testng-results[^>]*>' target/surefire-reports/testng-results.xml > "$dest/surefire-totals.txt" || true

cd "$work"
git config user.name "autonomous-qa[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add -A
git commit -q -m "QA report ($ENV_NAME): $VERDICT [run ${GITHUB_RUN_ID:-local}]" || { echo "Nothing new to publish"; exit 0; }
for attempt in 1 2 3; do
  if git push -q origin qa-reports; then echo "Published to qa-reports/$ENV_NAME"; exit 0; fi
  git pull -q --rebase origin qa-reports || true
  sleep $((attempt * 3))
done
echo "::warning::Could not publish report to qa-reports"
