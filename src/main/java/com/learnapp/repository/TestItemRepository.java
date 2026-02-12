package com.learnapp.repository;

import com.learnapp.entities.TestItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestItemRepository extends JpaRepository<TestItem, UUID> {
    List<TestItem> findByTestSessionIdOrderByPositionAsc(UUID testSessionId);

    Optional<TestItem> findByIdAndTestSessionId(UUID id, UUID testSessionId);
}
