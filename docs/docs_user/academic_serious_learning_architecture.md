# Academic Serious Learning -- Vocabulary System Architecture

Generated at: 2026-02-11T02:57:55.379901 UTC

------------------------------------------------------------------------

# 1. Learning Philosophy

This system is designed for **serious, long-term retention**, not casual
gamification.

Core principles:

-   Spaced repetition
-   Active recall over recognition
-   Gradual difficulty increase
-   Data-driven scheduling
-   Backend-validated answers
-   Measurable progress tracking

------------------------------------------------------------------------

# 2. Core Concepts

## 2.1 TestSession

A TestSession represents one learning session.

Types:

-   DAILY
-   REVIEW
-   NEW_WORDS
-   CUSTOM
-   SET_PRACTICE

Daily test is only one specific type of TestSession.

------------------------------------------------------------------------

# 3. Database Design

## 3.1 Existing Tables

### users

### user_vocab

Each vocabulary item added by the user.

Required fields:

-   process (0--100 mastery score)
-   last_reviewed_at
-   next_due_at
-   streak
-   right_count
-   wrong_count

These fields power the spaced repetition algorithm.

------------------------------------------------------------------------

## 3.2 test_sessions

One record = one learning session.

Fields:

-   id (UUID)
-   user_id
-   type (DAILY / REVIEW / NEW_WORDS / CUSTOM / SET_PRACTICE)
-   status (ACTIVE / COMPLETED / ABANDONED)
-   title
-   schedule_date (DATE, nullable)
-   source_type (DAILY_RULE / FILTER / MANUAL_PICK / USER_SET)
-   source_ref_id (nullable)
-   source_params (JSON)
-   created_at
-   started_at
-   completed_at

Business Rule:

For DAILY type → one session per user per day.

------------------------------------------------------------------------

## 3.3 test_items

One record = one question.

Fields:

-   id
-   test_session_id
-   user_vocab_id
-   question_type
-   question_payload (JSON)
-   position
-   status (PENDING / CORRECT / WRONG / SKIPPED)
-   user_answer
-   answered_at
-   time_ms

Constraints:

-   UNIQUE(test_session_id, position)

------------------------------------------------------------------------

# 4. Question Strategy (Academic Mode)

Question types (prioritized for active recall):

1.  TRANSLATE_TO_EN
2.  TRANSLATE_TO_VI
3.  FILL_MISSING_CHARS
4.  ACTIVE_RECALL_FULL_WORD
5.  MULTIPLE_CHOICE (used mainly for low mastery)

Difficulty progression based on process:

Process 0--30: - Mostly Multiple Choice - Some Fill Missing

Process 30--70: - Fill Missing - Translate

Process 70--100: - Active Recall - No Multiple Choice

------------------------------------------------------------------------

# 5. Spaced Repetition Algorithm

Interval mapping:

Process 0--20 → 1 day\
Process 21--40 → 2 days\
Process 41--60 → 4 days\
Process 61--75 → 7 days\
Process 76--90 → 14 days\
Process 91--100 → 30 days

On correct answer: - process += reward (smaller increment at higher
mastery)

On wrong answer: - process -= penalty (stronger penalty to resurface
earlier)

Then:

-   last_reviewed_at = now
-   next_due_at = now + interval(process)

------------------------------------------------------------------------

# 6. Daily Test Generation (Serious Mode)

Daily session size: 20 questions

Distribution:

-   70% Due words (next_due_at \<= today)
-   20% Weak words (low process)
-   10% New words

Ranking formula:

score = 3 × overdueDays + 0.05 × weakness + 0.1 × daysSinceLast

Top N selected by descending score.

------------------------------------------------------------------------

# 7. Backend Answer Validation

Frontend sends:

{ "answer": "...", "timeMs": 3200 }

Backend:

-   Normalizes answer
-   Validates against vocabulary data
-   Updates process
-   Updates next_due_at
-   Returns correctness + feedback

Frontend NEVER determines correctness.

------------------------------------------------------------------------

# 8. Session State Machine

TestSession:

ACTIVE → COMPLETED\
ACTIVE → ABANDONED

TestItem:

PENDING → CORRECT\
PENDING → WRONG\
PENDING → SKIPPED

------------------------------------------------------------------------

# 9. Long-Term Extensibility

This architecture supports:

-   Research-based spaced repetition tuning
-   Advanced analytics
-   AI-powered writing correction
-   Listening modules
-   Academic reporting dashboards
-   Performance metrics per skill type

------------------------------------------------------------------------

# 10. Design Philosophy Summary

This system is designed for:

-   Deep memory consolidation
-   Structured learning
-   Long-term vocabulary retention
-   Measurable academic progress

Not designed primarily for: - Casual gamification - Pure
entertainment-based learning
