package com.learnapp.entities;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VocabularyContributionTopicId implements Serializable {
    private UUID contributionId;
    private UUID topicId;
}
