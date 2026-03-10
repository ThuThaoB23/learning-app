package com.learnapp.repository;

import com.learnapp.entities.UserFeedbackAttachment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackAttachmentRepository extends JpaRepository<UserFeedbackAttachment, UUID> {
    List<UserFeedbackAttachment> findByFeedbackIdOrderByPositionAscCreatedAtAsc(UUID feedbackId);

    List<UserFeedbackAttachment> findByFeedbackIdInOrderByFeedbackIdAscPositionAscCreatedAtAsc(Collection<UUID> feedbackIds);
}
