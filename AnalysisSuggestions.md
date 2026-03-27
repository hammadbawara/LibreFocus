# LibreFocus Data Analysis Overview

## Checklist
- [x] Identify what data is currently collected/stored (from architecture + DB entities/models)
- [x] Describe the analyses/metrics the app is already doing in product terms (what the user sees)
- [x] Propose deeper analyses that reuse existing data (hourly usage, daily device usage/unlocks, launches, categories if/when present)
- [x] Call out any "needs new data" ideas separately (so you can ignore them for now)
- [x] Provide a prioritized roadmap (quick wins → heavier work)
- [x] Tie each suggestion to the exact existing tables/entities

---

## 1) What data is collected (as-is)
From `Agents.md` and the current Room/data layer, LibreFocus is collecting and persisting:

### Per-app usage (hourly aggregation)
Stored via `HourlyAppUsageEntity` (through `HourlyAppUsageDao`) and populated by `UsageTrackingRepository.syncUsageStats()` using `UsageStatsProvider`.

This implies you have, per app + hour bucket:
- **Total foreground time** (session duration aggregated)
- **Launch count** (app opens)
- Timestamps normalized to **hour boundaries** (see `TimeUtils.roundToHourStart()`)

### Per-day device-level usage signals
Stored via `DailyDeviceUsageEntity` (through `DailyDeviceUsageDao`), including:
- **Unlock count** (device unlock statistics)
- (Likely) other daily totals if present in entity (at minimum unlocks are explicitly mentioned)

### Metadata to support incremental sync
Stored in `SyncMetadataEntity` to enable:
- "Fetch only what's new since last sync"
- Resilience across restarts / background sync

### App dictionary / categorization scaffolding
Stored via:
- `AppEntity` (app identity/package name, label, etc.)
- `AppCategoryEntity` (predefined/custom categories)
However, category-based insights are listed as not done yet, so category data may exist but not fully used in analytics UI.

---

## 2) What analysis the app is already doing
Based on the "Stats/Analytics Screen" and repository responsibilities, the app currently computes and visualizes:

### A) Time series summaries over selected ranges
In `StatsScreen` (daily/weekly/monthly/custom):
- **Total usage time** over the selected period
- **Average** usage over the selected period (e.g., avg per day)
- Same concept applies to **launches** and **unlocks** when those metrics are selected

This is essentially:
- Aggregate function: `sum(metric)` over buckets
- Average function: `sum(metric) / number_of_days(or buckets)`

### B) Per-app ranking ("Top apps")
Also in stats:
- "Top apps list" for the selected period, for the selected metric (time/launches)

That implies:
- `groupBy(app).sum(metric)` then sort desc, take top N

### C) Charting / bucketing logic
The chart supports:
- Daily / weekly / monthly views (so it computes bucket boundaries and labels)
- Custom ranges and period navigation (prev/next)
- Axis label formatting via `DateTimeFormatterManager`

So analysis includes "resampling" into user-facing buckets, and formatting.

### D) Basic derived usage calculations at ingestion time
In `UsageStatsProvider` + `UsageTrackingRepository`:
- App session duration calculation (turning raw usage events into time in foreground)
- Launch counts (counting transitions)
- Unlock counts (device-level metric)

---

## 3) Deeper analyses you can do *using only existing collected data*
Everything below can be built from:
- hourly per-app usage time + launches
- daily unlock counts
- app identity + (optional) category labels

### 3.1 Habit + consistency metrics (behavior change friendly)
These are strong for "reduce screen time" because they speak to stability and progress.

- **Streaks**
  - "Days under X minutes total"
  - "Days where social category under Y"
  - "Days with <= N unlocks"
- **Volatility / routine stability**
  - Std-dev of daily usage (lower volatility can indicate better control)
  - "Most consistent day-of-week" patterns
- **Week-over-week deltas**
  - % change vs previous week for total time, unlocks, launches
  - Best improvement day ("biggest drop day")

### 3.2 Peaks, patterns, and "danger windows" (great from hourly data)
Hourly aggregation is perfect for this.

- **Heatmaps**
  - Hour-of-day × day-of-week usage heatmap (total + per category + per top app)
- **Peak usage hours**
  - Identify top 3 hours contributing to weekly usage ("Your top usage window is 10pm–1am")
- **Late-night usage score**
  - % of total usage after a user-defined hour (or 10pm default)
- **Fragmentation / compulsive-checking proxy**
  - High launches with low total time suggests frequent checking
  - Metric: `launches_per_hour`, `launches_per_minute_of_use`, or `median_session_length` (approximate from hourly: `usageTime / launches` as a proxy)

### 3.3 "Pickups" analysis using unlocks + launches (still no new data)
Even without notification data, unlocks are a strong signal.

- **Unlock-to-use coupling**
  - Correlate daily unlocks with daily total usage
  - "On high-unlock days, usage is X% higher"
- **First-hour-after-unlock impact (approx)**
  - If you have hourly usage and daily unlocks only, you can still do:
    - "Morning usage block" (e.g., 7–10am) vs unlock-heavy days
- **Unlock efficiency**
  - `total_usage_time / unlocks` (minutes of use per unlock)
  - Flag "many unlocks with little usage time" as impulsive checking

### 3.4 Concentration and the "attention economy" view
These are interpretable insights for users.

- **App concentration index**
  - What % of your time is in top 1 / top 3 / top 5 apps
  - A simple inequality measure (e.g., Gini-like) can show if one app dominates
- **Long-tail analysis**
  - Count of distinct apps used per day/week
  - "App sprawl" trends (more apps touched often correlates with distraction)

### 3.5 Goal-aware analytics (still uses same data)
Even before you implement blocking/gamification, you can do analytics-as-goals:

- **Budget burn-down**
  - For a daily limit: show cumulative usage through the day vs budget
  - Needs only hourly totals (sum up to current hour)
- **Forecasting end-of-day usage**
  - Simple forecast: based on same weekday historical hourly curve
  - "At this pace, you'll end around 3h 20m"

### 3.6 Category-based insights (once categories are populated)
No new collection; just use `AppCategoryEntity` mapping.

- **Category split**
  - Percent time by category over period
  - Category trends over weeks ("Social down 12%, Productivity up 5%")
- **Category danger windows**
  - "Social spikes at 11pm–1am"
- **Category substitution**
  - If Social decreases, does another category rise? (simple correlation between category totals)

### 3.7 Lightweight "personalization" without ML
You can generate "nudges" from heuristics.

- Identify top distraction app(s) and their peak hours
- Suggest one change:
  - "Try a 30-minute cap on App X between 10pm–12am"
  - "Your unlocks peak around lunchtime; consider a no-phone lunch goal"

---

## 4) Ideas that would require *new* data (optional, not "existing collected data")
Just to separate concerns:
- Notification pickup causes (needs notification listener / event source)
- Content-type tracking like Shorts/Reels (needs accessibility or app-specific parsing)
- True session boundaries / pickups per unlock with precision (needs more granular event storage than hourly aggregates, or store raw events)

---

## 5) Prioritized roadmap (quick wins → heavier work)
Assumptions:
- You keep using the existing aggregates (`HourlyAppUsageEntity`, `DailyDeviceUsageEntity`) and don’t start storing raw events.
- Existing Stats UI already supports selecting a range and plotting bucketed time series.

### Phase 1 — Quick wins (1–3 days each, high user value)
These are mostly **new aggregations on existing tables**, plus minor UI additions.

1) **Week-over-week change + highlights**
   - Metrics: total usage time, total launches, total unlocks
   - Outputs: % change vs previous period, “biggest improvement day”, “most used day”

2) **Peak hours + late-night %**
   - Metrics: peak 1–3 hours in a period, “% of usage after 22:00” (or configurable)
   - Outputs: small cards + optional overlay on existing charts

3) **Unlock efficiency + checking-fragmentation proxy**
   - Metrics: minutes per unlock; `usageTime / launches` proxy for “average use per open”
   - Outputs: “You unlocked 84 times for 2h 10m total → 1.55 min/unlock”

4) **Top-app concentration**
   - Metrics: share of time in Top 1/3/5 apps
   - Outputs: “Top 3 apps account for 72% of your time this week”

### Phase 2 — Medium effort (3–7 days, bigger UI work)
These likely require **new visualizations** (but still use the same collected data).

5) **Hour-of-day × day-of-week heatmap**
   - Variants: 전체 usage, per top app, per category
   - Outputs: heatmap grid + tap-to-drill-down

6) **Streaks & consistency**
   - Streaks: “days under X”, “days with <= N unlocks”
   - Consistency: simple volatility score (variance/std-dev) and “most consistent weekday”

7) **Distinct apps used (app sprawl)**
   - Metric: count distinct packages with non-zero usage per day/week
   - Output: time series + “sprawl up/down” insight

### Phase 3 — Heavier / more experimental (1–3+ weeks)
Still possible with existing data, but you’ll want careful design and validation.

8) **Daily forecast / pace-of-day projection**
   - “At this pace, you’ll end the day around …”
   - Basic methods: same-weekday historical hourly curve or trailing average

9) **Simple correlations**
   - Examples:
     - unlocks vs usage time correlation across last 14/30 days
     - category A vs category B substitution patterns
   - Output: “Your unlock count and usage time are strongly linked (r≈0.7)” (if you choose to show a number) or a simpler “highly linked” label.

---

## 6) Data-to-analysis mapping (what tables power what insights)
This is the “wiring diagram” from your current storage layer to possible insights.

### Core entities
- `HourlyAppUsageEntity`
  - Keys: app (package), hour bucket timestamp
  - Measures: foreground time, launches
- `DailyDeviceUsageEntity`
  - Keys: day
  - Measures: unlock count (and any other daily device totals present)
- `AppEntity`
  - App metadata (label/icon/package)
- `AppCategoryEntity`
  - Category definitions + app→category mapping (if populated)
- `SyncMetadataEntity`
  - Only for sync bookkeeping (not analytics directly)

### Insight mapping

| Insight / Feature | Uses `HourlyAppUsageEntity` | Uses `DailyDeviceUsageEntity` | Uses `AppEntity` | Uses `AppCategoryEntity` | Notes |
|---|:---:|:---:|:---:|:---:|---|
| Totals/averages by range (existing) | ✅ | ✅ (unlocks) | ✅ | ◻️ | Already in StatsScreen. |
| Top apps (existing) | ✅ | ❌ | ✅ | ◻️ | Ranking by sum(time) or sum(launches). |
| Week-over-week change | ✅ | ✅ | ◻️ | ◻️ | Compare current vs previous window. |
| Peak hours | ✅ | ❌ | ◻️ | ◻️ | Sum across apps per hour bucket. |
| Late-night usage % | ✅ | ❌ | ◻️ | ◻️ | Define a “late” cutoff hour. |
| Heatmap (hour × weekday) | ✅ | ❌ | ◻️ | ◻️ / ✅ | Category variant needs category mapping. |
| Fragmentation proxy (avg use per open) | ✅ | ❌ | ◻️ | ◻️ | Use `usageTime / launches` per app or overall. |
| Unlock efficiency (min/unlock) | ✅ (daily sum) | ✅ | ◻️ | ◻️ | Requires daily sum of usage time from hourly buckets. |
| Unlock-to-use coupling | ✅ (daily sum) | ✅ | ◻️ | ◻️ | Correlate series over N days. |
| Streaks (under X, under Y unlocks) | ✅ (daily sum) | ✅ | ◻️ | ✅ (optional) | Category streaks need category mapping. |
| Volatility / consistency | ✅ (daily sum) | ✅ | ◻️ | ◻️ | Std-dev/variance across days. |
| Top-app concentration (Top 1/3/5 share) | ✅ | ❌ | ✅ | ◻️ | Needs app names for display. |
| Distinct apps used (“sprawl”) | ✅ | ❌ | ◻️ | ◻️ | Count distinct packages with usage > 0 per day. |
| End-of-day forecast | ✅ | ❌ | ◻️ | ◻️ | Needs intra-day cumulative curve from hourly buckets. |
| Category split + trends | ✅ | ❌ | ❌ | ✅ | Requires app→category mapping coverage. |
| Category danger windows | ✅ | ❌ | ❌ | ✅ | Heatmap but grouped by category. |

Legend: ✅ required, ◻️ optional, ❌ not needed.

---

## Requirements coverage
- What analysis is being done now: **Done** (charts, totals/averages, top apps, metric selection, hourly/daily aggregation, incremental sync)
- Suggestions for deeper analysis using current data: **Done** (habit metrics, heatmaps/peaks, unlock coupling, concentration, forecasting, category insights)
- Prioritized roadmap + mapping to existing tables: **Done**
