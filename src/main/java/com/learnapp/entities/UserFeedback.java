package com.learnapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "user_feedbacks",
        indexes = {
            @Index(name = "idx_user_feedback_user_id", columnList = "user_id"),
            @Index(name = "idx_user_feedback_status", columnList = "status"),
            @Index(name = "idx_user_feedback_category", columnList = "category"),
            @Index(name = "idx_user_feedback_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedback {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private UserFeedbackCategory category;

    @Column(name = "title", length = 120)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserFeedbackStatus status = UserFeedbackStatus.NEW;

    @Column(name = "source_screen", length = 120)
    private String sourceScreen;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "locale", length = 20)
    private String locale;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "read_by", length = 36)
    private UUID readBy;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "archived_by", length = 36)
    private UUID archivedBy;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
