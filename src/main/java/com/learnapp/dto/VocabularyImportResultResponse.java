package com.learnapp.dto;

import java.util.List;

public record VocabularyImportResultResponse(
        int totalRows,
        int importedRows,
        int failedRows,
        List<VocabularyImportErrorResponse> errors
) {}
