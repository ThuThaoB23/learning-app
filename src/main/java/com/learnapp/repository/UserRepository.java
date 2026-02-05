package com.learnapp.repository;

import com.learnapp.entities.User;
import com.learnapp.entities.UserRole;
import com.learnapp.entities.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByDeletedAtIsNull(Pageable pageable);

    @Query("""
            select u
            from User u
            where u.deletedAt is null
              and (:email is null or lower(u.email) like concat('%', :email, '%'))
              and (:username is null or lower(u.username) like concat('%', :username, '%'))
              and (:displayName is null or lower(u.displayName) like concat('%', :displayName, '%'))
              and (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
            """)
    Page<User> searchUsers(
            @Param("email") String email,
            @Param("username") String username,
            @Param("displayName") String displayName,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
