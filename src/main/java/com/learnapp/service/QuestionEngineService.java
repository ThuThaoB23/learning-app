package com.learnapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learnapp.entities.QuestionType;
import com.learnapp.entities.TestSessionType;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.entities.Vocabulary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class QuestionEngineService {

    private static final List<QuestionType> DIFFICULTY_LADDER = List.of(
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.FILL_MISSING_CHARS,
            QuestionType.TRANSLATE_TO_VI,
            QuestionType.TRANSLATE_TO_EN,
            QuestionType.ACTIVE_RECALL_FULL_WORD
    );

    private final SpacedRepetitionService spacedRepetitionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuestionEngineService(SpacedRepetitionService spacedRepetitionService) {
        this.spacedRepetitionService = spacedRepetitionService;
    }

    public GeneratedQuestion generateQuestion(
            UserVocabulary userVocabulary,
            Vocabulary vocabulary,
            TestSessionType sessionType,
            LocalDate today,
            ZoneId zoneId,
            List<Vocabulary> distractors
    ) {
        QuestionType chosenType = chooseQuestionType(userVocabulary, sessionType, today, zoneId);
        ObjectNode payload = switch (chosenType) {
            case MULTIPLE_CHOICE -> buildMultipleChoicePayload(vocabulary, distractors);
            case FILL_MISSING_CHARS -> buildFillMissingPayload(userVocabulary, vocabulary);
            case TRANSLATE_TO_VI -> buildTranslateToViPayload(vocabulary);
            case ACTIVE_RECALL_FULL_WORD -> buildActiveRecallPayload(userVocabulary, vocabulary, today, zoneId);
            case TRANSLATE_TO_EN, TRUE_FALSE, CONTEXT_GAP -> buildTranslateToEnPayload(vocabulary);
        };

        if (payload == null) {
            payload = buildTranslateToEnPayload(vocabulary);
            chosenType = QuestionType.TRANSLATE_TO_EN;
        }

        return new GeneratedQuestion(chosenType, payload);
    }

    public QuestionType chooseQuestionType(
            UserVocabulary userVocabulary,
            TestSessionType sessionType,
            LocalDate today,
            ZoneId zoneId
    ) {
        int process = nullSafe(userVocabulary.getProcess());
        QuestionType base = sampleFromBucket(process);

        if (sessionType == TestSessionType.NEW_WORDS) {
            base = oneStepEasier(base);
        }
        if (sessionType == TestSessionType.REVIEW) {
            base = oneStepHarder(base);
        }

        long overdue = spacedRepetitionService.overdueDays(userVocabulary, today, zoneId);
        boolean likelyRecentlyWrong = nullSafe(userVocabulary.getStreak()) == 0 && nullSafe(userVocabulary.getWrongCount()) > 0;
        if (likelyRecentlyWrong || overdue >= 7) {
            base = oneStepEasier(base);
        }

        return base;
    }

    public GradeResult grade(QuestionType questionType, JsonNode payload, String rawAnswer) {
        String answer = normalize(rawAnswer);
        String expected = payload.path("expected").asText("");
        String normalizedExpected = normalize(expected);

        if (questionType == QuestionType.MULTIPLE_CHOICE) {
            int correctOption = payload.path("correctOption").asInt(-1);
            JsonNode optionsNode = payload.path("options");
            String chosenText = "";
            if (isInteger(answer)) {
                int selected = Integer.parseInt(answer);
                if (selected >= 0 && optionsNode.isArray() && selected < optionsNode.size()) {
                    chosenText = normalize(optionsNode.get(selected).asText());
                }
            }
            if (chosenText.isEmpty() && optionsNode.isArray()) {
                for (int i = 0; i < optionsNode.size(); i++) {
                    if (normalize(optionsNode.get(i).asText()).equals(answer)) {
                        chosenText = answer;
                        break;
                    }
                }
            }
            boolean correctByIndex = isInteger(answer) && Integer.parseInt(answer) == correctOption;
            boolean correctByText = !chosenText.isEmpty() && chosenText.equals(normalizedExpected);
            return buildGradeResult(correctByIndex || correctByText, expected);
        }

        boolean correct = answer.equals(normalizedExpected);
        return buildGradeResult(correct, expected);
    }

    public ObjectNode sanitizeForClient(JsonNode payload) {
        ObjectNode sanitized = payload == null || payload.isNull()
                ? objectMapper.createObjectNode()
                : payload.deepCopy();
        sanitized.remove(List.of("expected", "correctOption"));
        return sanitized;
    }

    public Object toPlainJsonPayload(JsonNode payload) {
        return objectMapper.convertValue(sanitizeForClient(payload), Object.class);
    }

    private QuestionType sampleFromBucket(int process) {
        if (process <= 30) {
            return weightedSample(List.of(
                    weighted(QuestionType.MULTIPLE_CHOICE, 0.60),
                    weighted(QuestionType.FILL_MISSING_CHARS, 0.25),
                    weighted(QuestionType.TRANSLATE_TO_VI, 0.15)
            ));
        }
        if (process <= 70) {
            return weightedSample(List.of(
                    weighted(QuestionType.FILL_MISSING_CHARS, 0.40),
                    weighted(QuestionType.TRANSLATE_TO_VI, 0.30),
                    weighted(QuestionType.TRANSLATE_TO_EN, 0.25),
                    weighted(QuestionType.MULTIPLE_CHOICE, 0.05)
            ));
        }
        return weightedSample(List.of(
                weighted(QuestionType.TRANSLATE_TO_EN, 0.55),
                weighted(QuestionType.ACTIVE_RECALL_FULL_WORD, 0.35),
                weighted(QuestionType.TRANSLATE_TO_VI, 0.10)
        ));
    }

    private ObjectNode buildMultipleChoicePayload(Vocabulary vocabulary, List<Vocabulary> distractors) {
        Set<String> optionsSet = new LinkedHashSet<>();
        optionsSet.add(vocabulary.getTerm());

        for (Vocabulary distractor : distractors) {
            if (optionsSet.size() >= 4) {
                break;
            }
            if (distractor == null || distractor.getId().equals(vocabulary.getId())) {
                continue;
            }
            if (distractor.getTerm() != null && !distractor.getTerm().isBlank()) {
                optionsSet.add(distractor.getTerm());
            }
        }

        if (optionsSet.size() < 4) {
            return null;
        }

        List<String> options = new ArrayList<>(optionsSet);
        Collections.shuffle(options);
        int correctIndex = options.indexOf(vocabulary.getTerm());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", nonBlank(vocabulary.getDefinitionVi(), vocabulary.getDefinition()));
        ArrayNode arrayNode = payload.putArray("options");
        options.forEach(arrayNode::add);
        payload.put("correctOption", correctIndex);
        payload.put("expected", vocabulary.getTerm());
        return payload;
    }

    private ObjectNode buildFillMissingPayload(UserVocabulary userVocabulary, Vocabulary vocabulary) {
        String term = vocabulary.getTerm();
        if (term == null || term.isBlank()) {
            return null;
        }

        int process = nullSafe(userVocabulary.getProcess());
        double maskRatio = process < 30 ? 0.25 : (process <= 70 ? 0.40 : 0.55);
        String masked = maskTerm(term, maskRatio, process < 30);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", nonBlank(vocabulary.getDefinitionVi(), vocabulary.getDefinition()));
        payload.put("maskedTerm", masked);
        payload.put("expected", term);
        return payload;
    }

    private ObjectNode buildTranslateToViPayload(Vocabulary vocabulary) {
        String expected = nonBlank(vocabulary.getDefinitionVi(), vocabulary.getDefinition());
        if (expected == null) {
            return null;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", vocabulary.getTerm());
        payload.put("expected", expected);
        return payload;
    }

    private ObjectNode buildTranslateToEnPayload(Vocabulary vocabulary) {
        if (vocabulary.getTerm() == null || vocabulary.getTerm().isBlank()) {
            return null;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", nonBlank(vocabulary.getDefinitionVi(), vocabulary.getDefinition()));
        payload.put("expected", vocabulary.getTerm());
        return payload;
    }

    private ObjectNode buildActiveRecallPayload(
            UserVocabulary userVocabulary,
            Vocabulary vocabulary,
            LocalDate today,
            ZoneId zoneId
    ) {
        ObjectNode payload = buildTranslateToEnPayload(vocabulary);
        if (payload == null) {
            return null;
        }
        boolean shouldHint = spacedRepetitionService.overdueDays(userVocabulary, today, zoneId) >= 7
                || (nullSafe(userVocabulary.getStreak()) == 0 && nullSafe(userVocabulary.getWrongCount()) > 0);
        if (shouldHint) {
            String term = vocabulary.getTerm();
            if (term != null && !term.isBlank()) {
                payload.put("hint", term.substring(0, 1));
            }
        }
        return payload;
    }

    private String maskTerm(String term, double maskRatio, boolean keepFirst) {
        char[] chars = term.toCharArray();
        int toMask = Math.max(1, (int) Math.floor(chars.length * maskRatio));
        int start = keepFirst && chars.length > 1 ? 1 : 0;
        List<Integer> candidates = new ArrayList<>();
        for (int i = start; i < chars.length; i++) {
            if (Character.isLetterOrDigit(chars[i])) {
                candidates.add(i);
            }
        }
        Collections.shuffle(candidates);
        for (int i = 0; i < Math.min(toMask, candidates.size()); i++) {
            chars[candidates.get(i)] = '_';
        }
        return new String(chars);
    }

    private QuestionType oneStepEasier(QuestionType type) {
        int idx = DIFFICULTY_LADDER.indexOf(type);
        if (idx <= 0) {
            return DIFFICULTY_LADDER.get(0);
        }
        return DIFFICULTY_LADDER.get(idx - 1);
    }

    private QuestionType oneStepHarder(QuestionType type) {
        int idx = DIFFICULTY_LADDER.indexOf(type);
        if (idx < 0 || idx >= DIFFICULTY_LADDER.size() - 1) {
            return DIFFICULTY_LADDER.get(DIFFICULTY_LADDER.size() - 1);
        }
        return DIFFICULTY_LADDER.get(idx + 1);
    }

    private QuestionType weightedSample(List<WeightedQuestionType> weights) {
        double roll = ThreadLocalRandom.current().nextDouble();
        double acc = 0.0;
        for (WeightedQuestionType weight : weights) {
            acc += weight.weight();
            if (roll <= acc) {
                return weight.type();
            }
        }
        return weights.get(weights.size() - 1).type();
    }

    private WeightedQuestionType weighted(QuestionType type, double weight) {
        return new WeightedQuestionType(type, weight);
    }

    private GradeResult buildGradeResult(boolean correct, String expected) {
        String feedback = correct ? "Correct" : "Incorrect";
        return new GradeResult(correct, expected, feedback);
    }

    private String nonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private boolean isInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    public record GeneratedQuestion(QuestionType type, ObjectNode payload) {}

    public record GradeResult(boolean correct, String expected, String feedback) {}

    private record WeightedQuestionType(QuestionType type, double weight) {}
}
