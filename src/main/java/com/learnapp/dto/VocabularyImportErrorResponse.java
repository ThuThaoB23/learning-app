package com.learnapp.dto;

public record VocabularyImportErrorResponse(
        long row,
        String message
) {}
