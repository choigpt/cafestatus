package com.example.cafestatus.cafe.entity;

import com.example.cafestatus.auth.entity.Owner;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "cafes",
        indexes = {
                @Index(name = "idx_cafes_lat_lng", columnList = "latitude,longitude")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cafes_name_lat_lng", columnNames = {"name", "latitude", "longitude"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cafe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Cafe(String name, Double latitude, Double longitude, String address, Owner owner) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.owner = owner;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void update(String name, String address) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
    }
}
