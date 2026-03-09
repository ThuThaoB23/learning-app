package com.learnapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learnapp.dto.VocabularyAudioResponse;
import com.learnapp.entities.QuestionType;
import com.learnapp.entities.TestSessionType;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.entities.Vocabulary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionEngineServiceTest {

    private QuestionEngineService service;

    @BeforeEach
    void setUp() {
        service = new QuestionEngineService(new SpacedRepetitionService());
    }

    @Test
    void gradeShouldValidateMultipleChoiceByIndex() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("prompt", "A fruit");
        payload.putArray("options").add("banana").add("apple").add("orange").add("grape");
        payload.put("correctOption", 1);
        payload.put("expected", "apple");

        QuestionEngineService.GradeResult result = service.grade(QuestionType.MULTIPLE_CHOICE, payload, "1");

        assertTrue(result.correct());
        assertEquals("apple", result.expected());
    }

    @Test
    void gradeShouldNormalizeFreeTextAnswers() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("prompt", "A fruit");
        payload.put("expected", "Green Apple");

        QuestionEngineService.GradeResult result = service.grade(QuestionType.TRANSLATE_TO_EN, payload, "  green   apple ");

        assertTrue(result.correct());
    }

    @Test
    void sanitizeShouldHideInternalKeys() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("prompt", "A fruit");
        payload.put("expected", "apple");
        payload.put("correctOption", 2);

        ObjectNode sanitized = service.sanitizeForClient(payload);

        assertFalse(sanitized.has("expected"));
        assertFalse(sanitized.has("correctOption"));
        assertEquals("A fruit", sanitized.path("prompt").asText());
    }

    @Test
    void gradeShouldValidateListenAndChooseByIndex() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("prompt", "Listen and choose the correct word");
        payload.put("audioUrl", "https://cdn.example.com/apple.mp3");
        payload.putArray("options").add("banana").add("apple").add("orange").add("grape");
        payload.put("correctOption", 1);
        payload.put("expected", "apple");

        QuestionEngineService.GradeResult result = service.grade(QuestionType.LISTEN_AND_CHOOSE, payload, "1");

        assertTrue(result.correct());
        assertEquals("apple", result.expected());
    }

    @Test
    void generateQuestionShouldBeAbleToProduceListenAndChooseWhenAudioExists() {
        UserVocabulary userVocabulary = UserVocabulary.builder()
                .process(25)
                .build();
        Vocabulary vocabulary = Vocabulary.builder()
                .id(UUID.randomUUID())
                .term("apple")
                .definition("A fruit")
                .definitionVi("Qua tao")
                .language("en")
                .build();
        List<Vocabulary> distractors = List.of(
                Vocabulary.builder().id(UUID.randomUUID()).term("apply").language("en").build(),
                Vocabulary.builder().id(UUID.randomUUID()).term("ample").language("en").build(),
                Vocabulary.builder().id(UUID.randomUUID()).term("maple").language("en").build()
        );
        List<VocabularyAudioResponse> audios = List.of(
                new VocabularyAudioResponse(UUID.randomUUID(), "https://cdn.example.com/apple.mp3", "us", 1)
        );

        boolean producedListenAndChoose = false;
        for (int i = 0; i < 50; i++) {
            QuestionEngineService.GeneratedQuestion question = service.generateQuestion(
                    userVocabulary,
                    vocabulary,
                    TestSessionType.CUSTOM,
                    LocalDate.of(2026, 3, 9),
                    ZoneId.of("UTC"),
                    distractors,
                    audios
            );
            if (question.type() == QuestionType.LISTEN_AND_CHOOSE) {
                producedListenAndChoose = true;
                assertEquals("https://cdn.example.com/apple.mp3", question.payload().path("audioUrl").asText());
                assertEquals("apple", question.payload().path("expected").asText());
                assertTrue(question.payload().path("options").isArray());
                break;
            }
        }

        assertTrue(producedListenAndChoose);
    }

    @Test
    void generateQuestionShouldFallbackWithinPreferredQuestionTypes() {
        UserVocabulary userVocabulary = UserVocabulary.builder()
                .process(25)
                .build();
        Vocabulary vocabulary = Vocabulary.builder()
                .id(UUID.randomUUID())
                .term("apple")
                .definition("A fruit")
                .definitionVi("Qua tao")
                .language("en")
                .build();
        List<Vocabulary> distractors = List.of(
                Vocabulary.builder().id(UUID.randomUUID()).term("apply").language("en").build(),
                Vocabulary.builder().id(UUID.randomUUID()).term("ample").language("en").build(),
                Vocabulary.builder().id(UUID.randomUUID()).term("maple").language("en").build()
        );

        QuestionEngineService.GeneratedQuestion question = service.generateQuestion(
                userVocabulary,
                vocabulary,
                TestSessionType.SET_PRACTICE,
                LocalDate.of(2026, 3, 9),
                ZoneId.of("UTC"),
                distractors,
                List.of(),
                List.of(QuestionType.LISTEN_AND_CHOOSE, QuestionType.FILL_MISSING_CHARS)
        );

        assertEquals(QuestionType.FILL_MISSING_CHARS, question.type());
        assertEquals("apple", question.payload().path("expected").asText());
        assertTrue(question.payload().has("maskedTerm"));
    }
}
