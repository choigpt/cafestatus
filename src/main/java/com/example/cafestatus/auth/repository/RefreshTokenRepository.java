package com.example.cafestatus.auth.repository;

import com.example.cafestatus.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("delete from RefreshToken rt where rt.owner.id = :ownerId")
    void deleteByOwnerId(Long ownerId);
}
