package com.learnapp.dto;

import java.util.List;

public record FlashcardDeckResponse(
        Integer requestedLimit,
        Integer totalItems,
        List<FlashcardDeckGroupResponse> groups,
        List<FlashcardItemResponse> items
) {}
