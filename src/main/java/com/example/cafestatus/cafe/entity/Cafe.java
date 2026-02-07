package com.example.cafestatus.cafe.entity;

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

    @Column(nullable = false)
    private String ownerToken;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Cafe(String name, Double latitude, Double longitude, String address, String ownerToken) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.ownerToken = ownerToken;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void rotateOwnerToken(String newToken) {
        this.ownerToken = newToken;
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
