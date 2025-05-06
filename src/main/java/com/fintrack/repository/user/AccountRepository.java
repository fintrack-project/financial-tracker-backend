package com.fintrack.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fintrack.model.user.Account;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    @Query(value = "SELECT u.account_id FROM users u WHERE u.user_id = :userId", nativeQuery = true)
    Optional<UUID> findAccountIdByUserId(@Param("userId") String userId);
}