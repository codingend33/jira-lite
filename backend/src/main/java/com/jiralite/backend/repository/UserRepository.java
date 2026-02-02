package com.jiralite.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findById(UUID id);
    Optional<UserEntity> findByCognitoSub(String sub);
    Optional<UserEntity> findByEmail(String email);
}
