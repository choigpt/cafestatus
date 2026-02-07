package com.example.cafestatus.status.repository;

import com.example.cafestatus.status.entity.CafeLiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CafeLiveStatusRepository extends JpaRepository<CafeLiveStatus, Long> {
    List<CafeLiveStatus> findByCafeIdIn(List<Long> cafeIds);
}
