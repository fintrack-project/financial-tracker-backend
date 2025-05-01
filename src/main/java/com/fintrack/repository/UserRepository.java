package com.fintrack.repository;

import com.fintrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserId(String userId);
    Optional<User> findByAccountId(UUID accountId);
    Optional<User> findByEmail(String email);

    @Query(value = "SELECT u.account_id FROM users u WHERE u.user_id = :userId", nativeQuery = true)
    Optional<UUID> findAccountIdByUserId(@Param("userId") String userId);
}