package com.learnapp.repository;

import com.learnapp.entities.TestSession;
import com.learnapp.entities.TestSessionStatus;
import com.learnapp.entities.TestSessionType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestSessionRepository extends JpaRepository<TestSession, UUID> {
    Optional<TestSession> findByIdAndUserId(UUID id, UUID userId);

    Optional<TestSession> findByUserIdAndTypeAndScheduleDateAndStatus(
            UUID userId,
            TestSessionType type,
            LocalDate scheduleDate,
            TestSessionStatus status
    );
}
