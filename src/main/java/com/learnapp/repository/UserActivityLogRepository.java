package com.learnapp.repository;

import com.learnapp.entities.UserActivityLog;
import com.learnapp.entities.UserActivityTargetType;
import com.learnapp.entities.UserActivityType;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, UUID> {

    @Query("""
            select l
            from UserActivityLog l
            where (:userId is null or l.userId = :userId)
              and (:activityType is null or l.activityType = :activityType)
              and (:targetType is null or l.targetType = :targetType)
              and (:fromTime is null or l.createdAt >= :fromTime)
              and (:toTime is null or l.createdAt <= :toTime)
            order by l.createdAt desc
            """)
    Page<UserActivityLog> search(
            @Param("userId") UUID userId,
            @Param("activityType") UserActivityType activityType,
            @Param("targetType") UserActivityTargetType targetType,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable
    );
}
