package com.learnapp.entities;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TopicVocabularyId implements Serializable {
    private UUID topicId;
    private UUID vocabularyId;
}
