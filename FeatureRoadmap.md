# LibreFocus — Feature Roadmap (What to build first + approach)

Current date: **2026-03-13**

This document prioritizes analytics features that can be built **using existing collected data** and outlines a practical approach for each (data source → computation → UI).

## Goals / success criteria
- Give users **clear, actionable insights** that help reduce screen time.
- Prefer features that are:
  - Computable from existing tables (`HourlyAppUsageEntity`, `DailyDeviceUsageEntity`, `AppEntity`, `AppCategoryEntity`)
  - Easy to explain in UI (“why am I seeing this?”)
  - Cheap to compute (can run fast on-device)

## Constraints / assumptions
- No new data collection yet (no raw event storage, no notification listener, no accessibility scraping).
- Use existing bucketing (hourly app usage + daily device unlock counts).
- Stats screen already supports range selection and charting; we’ll reuse that.

---

## Summary: what to build first
### Phase 1 (Quick wins, highest ROI)
1. **Week-over-week change + highlights**
2. **Peak hours + late-night usage score**
3. **Unlock efficiency + checking fragmentation proxy**
4. **Top-app concentration (“Top 3 = X% of your time”)**

### Phase 2 (Medium effort, high differentiation)
5. **Usage heatmap (hour × weekday)**
6. **Streaks + consistency (volatility)**
7. **Distinct apps used ("app sprawl")**

### Phase 3 (Heavier / experimental)
8. **Forecast end-of-day usage**
9. **Correlation insights (unlocks ↔ usage, category substitution)**

---

## Phase 1 — Build these first (recommended order)

### 1) Week-over-week change + highlights
**Why first:** It’s instantly useful, simple to compute, and encourages behavior change (“You improved 12%”).

**Data used**
- `HourlyAppUsageEntity`: to compute total usage time and total launches per day/week
- `DailyDeviceUsageEntity`: to compute total unlocks per day/week

**Approach (computation)**
- Choose a window from the current Stats period (e.g., “this week”).
- Compute the identical **previous** window (previous week).
- For each metric (time, launches, unlocks):
  - `currentTotal = sum(metric in window)`
  - `previousTotal = sum(metric in prevWindow)`
  - `delta = currentTotal - previousTotal`
  - `pct = delta / max(previousTotal, epsilon)`
- Highlights:
  - “Most used day”: daily totals, pick max
  - “Biggest improvement day”: compare day-by-day vs previous period’s matching weekday (optional) or just min day-over-day within current period

**Edge cases**
- Previous window missing data ⇒ show “Not enough history yet”.
- previousTotal = 0 ⇒ avoid division by zero; show absolute delta only.

**UI placement**
- A compact “Change vs last period” card above charts.
- Show per-metric chips (Time / Launches / Unlocks) or reuse existing metric selection.

**Acceptance checks**
- If user selects a custom range, the app still finds the previous same-length range.
- Works when the range spans month/year boundaries.

---

### 2) Peak hours + late-night usage score
**Why second:** You already store hourly data; this turns it into a strong insight (“Your danger window is 10pm–12am”).

**Data used**
- `HourlyAppUsageEntity`

**Approach (computation)**
- Aggregate across apps per hour bucket for selected range:
  - `hourTotal[hourBucket] = sum(usageTime for all apps at hourBucket)`
- Compute:
  - **Top N hours** by `hourTotal` (N=3 is usually enough)
  - **Late-night %**: `sum(hours >= cutoff) / sum(all hours)`
    - cutoff default: 22:00 or user-configurable (later feature)

**Edge cases**
- If total usage is 0 ⇒ show empty state.
- DST shifts: hour buckets might be weird on some days; rely on your existing bucketing logic.

**UI placement**
- Insight card(s) on Stats screen:
  - “Peak usage window: 22:00–01:00”
  - “Late-night usage: 34%”

**Optional extra (still Phase 1)**
- Provide a “View by hour” mini bar chart for the selected day.

---

### 3) Unlock efficiency + checking fragmentation proxy
**Why third:** It approximates compulsive checking without new sensors.

**Data used**
- `DailyDeviceUsageEntity`: unlock counts per day
- `HourlyAppUsageEntity`: daily total usage time (sum all hours, all apps)
- `HourlyAppUsageEntity`: launches (sum all apps)

**Approach (computation)**
Compute per day (or per selected range as average):
- `minutesPerUnlock = totalUsageMinutes / unlockCount`
- `minutesPerLaunch = totalUsageMinutes / totalLaunches` (proxy for “avg time per open”)
- Flag patterns:
  - high unlocks + low minutesPerUnlock ⇒ “Frequent checking”
  - high launches + low minutesPerLaunch ⇒ “Short sessions / lots of opens”

Define simple thresholds (tunable later):
- “Checking-heavy day” if `unlockCount >= p75` and `minutesPerUnlock <= p25` over last 14 days.

**Edge cases**
- unlockCount = 0: show “No unlocks recorded” or skip that day.
- launches = 0: avoid division by zero.

**UI placement**
- A card: “You averaged 1.6 min per unlock this week”
- Optional: show distribution / trend line over days.

---

### 4) Top-app concentration
**Why fourth:** Helps users realize a small number of apps dominate attention.

**Data used**
- `HourlyAppUsageEntity`
- `AppEntity` for display names

**Approach (computation)**
- For selected range:
  - `timeByApp = groupBy(package).sum(usageTime)`
  - Sort desc, compute:
    - `top1Share = top1 / total`
    - `top3Share = (top1+top2+top3) / total`
    - `top5Share` similarly
- Optional: compute a simple concentration score (e.g., Herfindahl index `sum((share)^2)`).

**Edge cases**
- total = 0 ⇒ empty state.
- fewer than 3 apps used ⇒ compute with available apps.

**UI placement**
- “Top 3 apps = 72% of your time” card.
- Tap opens the existing Top Apps list.

---

## Phase 2 — Next (medium effort, more UI)

### 5) Heatmap (hour × weekday)
**Why:** Very expressive visualization; makes routines obvious.

**Data used**
- `HourlyAppUsageEntity`
- Optional: `AppCategoryEntity` (heatmap per category), `AppEntity` (per app)

**Approach**
- Build a 7×24 grid for the selected multi-week window.
- For each cell: sum usage minutes for that weekday/hour.
- Normalize colors by percentile to avoid outlier domination.

**UI**
- New tab/section: “Patterns”.
- Tap a cell shows top apps in that cell.

---

### 6) Streaks + consistency (volatility)
**Data used**
- `HourlyAppUsageEntity` (daily totals)
- `DailyDeviceUsageEntity` (daily unlock totals)
- Optional: `AppCategoryEntity`

**Approach**
- Streak: consecutive days meeting a condition.
- Consistency: standard deviation of daily totals over last N days.

**UI**
- “Streaks” page + small summary card on Stats.

---

### 7) Distinct apps used ("app sprawl")
**Data used**
- `HourlyAppUsageEntity`

**Approach**
- Per day: count distinct packages with usageTime>0.
- Trend vs previous period.

**UI**
- Chart line + explanatory copy.

---

## Phase 3 — Later (more polish/validation)

### 8) End-of-day forecast
**Data used**
- `HourlyAppUsageEntity`

**Approach**
- For the current day, compute usage so far.
- Forecast remaining usage using:
  - same weekday median by hour (last 4 weeks), or
  - trailing average hourly curve.

**UI**
- “Today” card: “Projected: 3h 20m”.

---

### 9) Correlation insights (be careful with messaging)
**Data used**
- `DailyDeviceUsageEntity` + daily totals from `HourlyAppUsageEntity`
- Optional: categories

**Approach**
- Compute correlation over last 14/30 days.
- Prefer soft language ("tend to") over causal claims.

---

## Implementation notes (no code yet, but how to approach safely)
- Create a small analytics layer that operates on **time series**:
  - input: `List<HourlyAppUsageEntity>`, `List<DailyDeviceUsageEntity>`
  - output: view-ready DTOs (cards + chart series)
- Keep calculations deterministic and testable.
- Use the existing date/time utilities for bucketing and period math.

## Suggested next step
Start Phase 1 with:
1) Week-over-week + highlights
2) Peak hours + late-night %

They’re the easiest to validate and will immediately make Stats feel “smarter” without changing data collection.

