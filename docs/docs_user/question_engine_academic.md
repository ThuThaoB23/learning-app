# Question Engine (Academic Mode) – Selecting Question Types by Mastery

Generated at: 2026-02-11T03:00:49.995612 UTC

---

## 1. Goal

Choose question types to maximize **active recall** while maintaining a manageable difficulty curve.

Key principles:

- Early: recognition → build familiarity (minimal frustration)
- Mid: partial recall → spelling + meaning alignment
- Late: full recall → production (typing) and contextual usage
- Avoid overusing multiple choice at high mastery
- Keep backend as source of truth for grading

---

## 2. Inputs

Per item (`user_vocab`):

- process (0..100)
- streak
- right_count / wrong_count
- last_reviewed_at / next_due_at
- optional: average response time

Per session (`test_session`):

- type (DAILY / REVIEW / NEW_WORDS / CUSTOM)
- goal emphasis (optional): meaning-first / spelling-first / balanced

---

## 3. Supported Question Types

Recommended academic set:

1. MULTIPLE_CHOICE (Recognition)
2. TRUE_FALSE (Light recognition)
3. FILL_MISSING_CHARS (Partial production)
4. TRANSLATE_TO_VI (Meaning recall)
5. TRANSLATE_TO_EN (Production recall)
6. ACTIVE_RECALL_FULL_WORD (Full production from meaning/hint)
7. CONTEXT_GAP (Choose/Type in sentence) – optional later

Notes:
- Academic mode prioritizes #4–#6 long-term.
- #1–#2 are scaffolding tools for low mastery or remediation.

---

## 4. Difficulty Ladder

From easiest → hardest:

MULTIPLE_CHOICE  
TRUE_FALSE  
FILL_MISSING_CHARS  
TRANSLATE_TO_VI  
TRANSLATE_TO_EN  
ACTIVE_RECALL_FULL_WORD  
CONTEXT_GAP (advanced)

---

## 5. Selection Policy (Default)

### 5.1 Mastery Buckets

- Bucket A: process 0–30 (novice)
- Bucket B: process 31–70 (intermediate)
- Bucket C: process 71–100 (advanced)

### 5.2 Probability Mix by Bucket

**A (0–30):**
- 60% MULTIPLE_CHOICE
- 25% FILL_MISSING_CHARS
- 15% TRANSLATE_TO_VI

**B (31–70):**
- 40% FILL_MISSING_CHARS
- 30% TRANSLATE_TO_VI
- 25% TRANSLATE_TO_EN
- 5% MULTIPLE_CHOICE (only for hard/overdue items)

**C (71–100):**
- 55% TRANSLATE_TO_EN
- 35% ACTIVE_RECALL_FULL_WORD
- 10% TRANSLATE_TO_VI
- 0% MULTIPLE_CHOICE (unless remediation trigger)

---

## 6. Remediation Rules (When to downgrade difficulty)

If any of the following is true:
- streak == 0 and wrong_count recently increased
- item is heavily overdue (overdueDays >= 7)
- user answered wrong last time

Then temporarily shift one step easier:

Example:
- ACTIVE_RECALL_FULL_WORD → TRANSLATE_TO_EN
- TRANSLATE_TO_EN → TRANSLATE_TO_VI
- TRANSLATE_TO_VI → FILL_MISSING_CHARS
- FILL_MISSING_CHARS → MULTIPLE_CHOICE

Pseudo-code:

```pseudo
function adjustForRemediation(type, uv, overdueDays, lastAttemptCorrect):
  if lastAttemptCorrect == false or overdueDays >= 7 or uv.streak == 0:
    return oneStepEasier(type)
  return type
```

---

## 7. Choosing Type Per Item (Pseudo-code)

```pseudo
function chooseQuestionType(uv, sessionType, today, zone):
  p = uv.process
  overdueDays = computeOverdueDays(uv, today, zone)

  baseType = sampleFromBucket(p)

  // In NEW_WORDS session, bias toward easier scaffolding
  if sessionType == NEW_WORDS:
    baseType = biasEasier(baseType)

  // In REVIEW session, bias toward recall types
  if sessionType == REVIEW:
    baseType = biasHarder(baseType)

  // Remediation adjustment
  lastCorrect = getLastAttemptCorrect(uv) // optional if you log attempts
  finalType = adjustForRemediation(baseType, uv, overdueDays, lastCorrect)

  return finalType
```

Where:

```pseudo
function sampleFromBucket(process):
  if process <= 30:
    return weightedSample({MC:0.60, FILL:0.25, T2VI:0.15})
  if process <= 70:
    return weightedSample({FILL:0.40, T2VI:0.30, T2EN:0.25, MC:0.05})
  return weightedSample({T2EN:0.55, RECALL:0.35, T2VI:0.10})
```

---

## 8. Question Payload Generation (Guidelines)

### MULTIPLE_CHOICE
- prompt: term or meaning
- options: correct + 3 distractors
- distractors should be:
  - same topic/category if possible
  - similar length/part-of-speech
  - not trivially wrong

### FILL_MISSING_CHARS
- masked string from term
- mask ratio depends on mastery:
  - process < 30: mask 20–30%
  - 30–70: mask 35–45%
  - >70: mask 50–60%
- avoid masking the first letter for low mastery

### TRANSLATE
- prompt only
- grading uses normalized comparison (trim/lower/space collapse)
- optional acceptance: synonyms list (later)

### ACTIVE_RECALL_FULL_WORD
- prompt: meaning + optional 1-letter hint
- academic mode: allow minimal hint only when overdue or recently wrong

---

## 9. Grading Policy (Backend)

Frontend submits:
- answer (text or option index)
- timeMs

Backend:
- computes correctness from vocab truth
- returns response fields like: correct, expected, and optional explain
- updates spaced repetition state

NEVER trust frontend correctness flags.

---

## 10. Tuning & Research Notes (Practical)

- Too hard early → users churn: increase MC share in Bucket A
- Too easy late → stagnation: increase ACTIVE_RECALL share in Bucket C
- Spelling pain → reduce mask ratio or accept small typos (edit distance) later
- Ensure session contains mix to reduce boredom but keep academic integrity
