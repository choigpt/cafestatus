package com.example.cafestatus.status.entity;

import com.example.cafestatus.cafe.entity.Cafe;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "cafe_live_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CafeLiveStatus {

    @Id
    private Long cafeId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id")
    private Cafe cafe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrowdLevel crowdLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Availability party2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Availability party3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Availability party4;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public CafeLiveStatus(Cafe cafe,
                          CrowdLevel crowdLevel,
                          Availability party2,
                          Availability party3,
                          Availability party4,
                          Instant updatedAt,
                          Instant expiresAt) {
        this.cafe = cafe;
        this.crowdLevel = crowdLevel;
        this.party2 = party2;
        this.party3 = party3;
        this.party4 = party4;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }

    public void update(CrowdLevel crowdLevel,
                       Availability party2,
                       Availability party3,
                       Availability party4,
                       Instant updatedAt,
                       Instant expiresAt) {
        this.crowdLevel = crowdLevel;
        this.party2 = party2;
        this.party3 = party3;
        this.party4 = party4;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
