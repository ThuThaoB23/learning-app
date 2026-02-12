# Spaced Repetition Engine (Academic Mode)

Generated at: 2026-02-11T03:00:49.995612 UTC

---

## 1. Goal

Implement a **research-aligned spaced repetition engine** suitable for serious learning:

- Predictable scheduling (no random magic)
- Strong penalties for forgetting
- Gradual increase in interval as mastery improves
- Stable behavior and easy tuning
- Backend-validated answers

This engine outputs:

- Whether an item is **due** today
- A **priority score** for selection
- How to update `process` and compute `next_due_at` after an attempt

---

## 2. Data Model Requirements

Per `user_vocab` (per-user per-item state):

- `process` (int 0..100) — mastery score
- `last_reviewed_at` (Instant, nullable)
- `next_due_at` (Instant, nullable)
- `streak` (int, optional but recommended)
- `right_count`, `wrong_count` (optional)
- `added_at` (Instant)

Optional but helpful:
- `ease` (float, default 2.3) — if later you want SM-2-like tuning
- `lapses` (int) — number of times user forgot after being “learned”

---

## 3. Key Definitions

- **New item**: `last_reviewed_at == null` OR `next_due_at == null`
- **Due item**: `next_due_at <= endOfToday(userTimezone)`
- **Overdue days**: `max(0, daysBetween(next_due_date, today))`
- **Weakness**: `100 - process`
- **Days since last**: if last is null → treat as large (e.g., 30)

---

## 4. Interval Mapping (MVP-Academic)

A stable, interpretable mapping from mastery to repetition interval:

| process range | interval days |
|---|---:|
| 0–20 | 1 |
| 21–40 | 2 |
| 41–60 | 4 |
| 61–75 | 7 |
| 76–90 | 14 |
| 91–100 | 30 |

Function:

```pseudo
function intervalDays(process):
  if process <= 20: return 1
  if process <= 40: return 2
  if process <= 60: return 4
  if process <= 75: return 7
  if process <= 90: return 14
  return 30
```

Notes (academic rationale):
- Early stages require tight feedback loops (1–2 days)
- Mid stage doubles spacing to consolidate
- High mastery moves to long intervals (2–4 weeks)

---

## 5. Process Update Rule

### 5.1 Normalization Helpers

```pseudo
function clampProcess(x):
  return min(100, max(0, x))
```

### 5.2 Reward/Penalty

We want:
- Increment becomes smaller as mastery increases (diminishing returns)
- Penalty is stronger to quickly resurface forgotten items

```pseudo
function reward(process):
  // higher process => smaller reward
  return max(2, 10 - floor(process / 15))

function penalty(process):
  // penalty slightly decreases as process increases, but stays strong
  return max(6, 12 - floor(process / 20))
```

### 5.3 Update on Attempt

```pseudo
function applyAttempt(userVocab, isCorrect, now):
  p = userVocab.process

  if isCorrect:
    p = clampProcess(p + reward(p))
    userVocab.streak += 1
    userVocab.right_count += 1
  else:
    p = clampProcess(p - penalty(p))
    userVocab.streak = 0
    userVocab.wrong_count += 1

  userVocab.process = p
  userVocab.last_reviewed_at = now

  days = intervalDays(p)
  userVocab.next_due_at = now + days

  return userVocab
```

### 5.4 Optional: “Hard Correct” vs “Easy Correct”

If you collect response time or confidence, you can refine correctness:

- EASY (fast correct) → bigger reward
- HARD (slow correct) → normal reward
- WRONG → normal penalty

```pseudo
function rewardWithSpeed(process, timeMs):
  base = reward(process)
  if timeMs <= 2500: return base + 1
  if timeMs >= 12000: return max(2, base - 1)
  return base
```

---

## 6. Due & Priority Scoring

### 6.1 Due Check

```pseudo
function isDue(userVocab, todayEndInstant):
  if userVocab.next_due_at == null: return true // treat new as due-candidate
  return userVocab.next_due_at <= todayEndInstant
```

### 6.2 Priority Score

A simple, tunable score to rank candidates for selection:

```pseudo
function priorityScore(userVocab, today, zone):
  // overdueDays
  overdue = 0
  if userVocab.next_due_at != null:
    dueDate = toLocalDate(userVocab.next_due_at, zone)
    if dueDate < today:
      overdue = daysBetween(dueDate, today)

  // daysSinceLast
  if userVocab.last_reviewed_at == null:
    sinceLast = 30
  else:
    lastDate = toLocalDate(userVocab.last_reviewed_at, zone)
    sinceLast = daysBetween(lastDate, today)

  weakness = 100 - userVocab.process

  // weights: overdue dominates, then weakness, then spacing
  return 3.0*overdue + 0.05*weakness + 0.1*sinceLast
```

Academic rationale:
- Overdue items should dominate selection
- Weakness prevents “plateau” and supports targeted remediation
- Time since last prevents neglect of long-interval items

---

## 7. Daily Selection Policy (Default)

For a daily session size **N=20**:

- 70% Due items (14)
- 20% Weak items (4)
- 10% New items (2)

Algorithm:

```pseudo
function selectForDailyTest(userId, date, zone, N=20):
  dueQuota=14, weakQuota=4, newQuota=2

  dueList = queryDue(userId, endOfDay(date, zone))
  weakList = queryWeak(userId, threshold=50)
  newList = queryNew(userId)

  sort dueList by priorityScore desc
  sort weakList by priorityScore desc
  sort newList by added_at asc

  picked = set()
  items = []

  pick(dueList, dueQuota, picked, items)
  pick(weakList, weakQuota, picked, items)
  pick(newList, newQuota, picked, items)

  if len(items) < N:
    candidates = queryActive(userId) excluding picked
    sort candidates by priorityScore desc
    pick(candidates, N - len(items), picked, items)

  return items
```

Where:

```pseudo
function pick(source, need, picked, items):
  added=0
  for uv in source:
    if added >= need: break
    if uv.status != ACTIVE: continue
    if uv.id not in picked:
      picked.add(uv.id)
      items.add(uv)
      added += 1
```

---

## 8. Tuning Guidelines

- If users complain “too many repeats”:
  - reduce penalty slightly OR increase intervals in low/mid ranges
- If users forget too often:
  - increase penalty OR reduce interval at process 40–75
- If daily test feels too hard:
  - lower proportion of active recall question types (handled by Question Engine)

Recommended first tuning axis:
- quota distribution (due/weak/new)
- interval mapping thresholds
- penalty scaling

---

## 9. Edge Cases

- If user has < N items total:
  - session contains all items
- If user has no new items:
  - allocate newQuota to weak or due
- If user stops learning for weeks:
  - overdueDays becomes large, score pulls those items back quickly

---

## 10. Next Extensions (Optional)

To approach SM-2 without complexity explosion:
- Introduce `ease` per item
- Update `ease` based on correctness + speed
- Compute interval as `prevInterval * ease` after streak >= 2

This is optional; MVP mapping above is adequate and stable.
