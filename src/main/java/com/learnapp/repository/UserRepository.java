package com.learnapp.repository;

import com.learnapp.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    org.springframework.data.domain.Page<User> findByDeletedAtIsNull(org.springframework.data.domain.Pageable pageable);
}
