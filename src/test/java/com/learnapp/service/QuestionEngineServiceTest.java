package com.learnapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learnapp.entities.QuestionType;
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
}
