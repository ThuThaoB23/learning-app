package com.learnapp.dto;

public record FlashcardDeckGroupResponse(
        FlashcardDeckBucket bucket,
        Integer count
) {}
