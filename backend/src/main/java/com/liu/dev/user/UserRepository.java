package com.liu.dev.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 方式一：JPA */
public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
}
