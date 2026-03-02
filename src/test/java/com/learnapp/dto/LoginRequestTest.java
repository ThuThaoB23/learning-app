package com.learnapp.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptLegacyEmailField() throws Exception {
        LoginRequest request = objectMapper.readValue("""
                {
                  "email": "user@example.com",
                  "password": "secret123"
                }
                """, LoginRequest.class);

        assertEquals("user@example.com", request.identifier());
        assertEquals(0, validator.validate(request).size());
    }

    @Test
    void shouldAcceptUsernameFieldAsAlias() throws Exception {
        LoginRequest request = objectMapper.readValue("""
                {
                  "username": "nguyenvanb",
                  "password": "secret123"
                }
                """, LoginRequest.class);

        assertEquals("nguyenvanb", request.identifier());
        assertEquals(0, validator.validate(request).size());
    }
}
