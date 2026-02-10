package com.example.cafestatus.cafe.repository;

import com.example.cafestatus.cafe.entity.Cafe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CafeRepository extends JpaRepository<Cafe, Long> {

    @Query("""
        select c from Cafe c
        where c.latitude between :minLat and :maxLat
          and c.longitude between :minLng and :maxLng
    """)
    List<Cafe> findInBoundingBox(double minLat, double maxLat, double minLng, double maxLng);

    Page<Cafe> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Cafe> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("select c from Cafe c left join fetch c.owner where c.id = :id")
    Optional<Cafe> findByIdWithOwner(Long id);
}
