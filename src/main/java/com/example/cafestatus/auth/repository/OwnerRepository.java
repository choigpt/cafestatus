package com.example.cafestatus.auth.repository;

import com.example.cafestatus.auth.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    Optional<Owner> findByEmail(String email);

    boolean existsByEmail(String email);
}
