package com.example.cafestatus.status.mapper;

import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.entity.Availability;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import com.example.cafestatus.status.entity.CrowdLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatusViewMapper 단위 테스트")
class StatusViewMapperTest {

    @Test
    @DisplayName("unknown()은 모든 필드가 UNKNOWN이고 stale=true, ageMinutes=-1이다")
    void unknown_returnsAllUnknown() {
        StatusSummary summary = StatusViewMapper.unknown();

        assertThat(summary.crowdLevel()).isEqualTo("UNKNOWN");
        assertThat(summary.party2()).isEqualTo("UNKNOWN");
        assertThat(summary.party3()).isEqualTo("UNKNOWN");
        assertThat(summary.party4()).isEqualTo("UNKNOWN");
        assertThat(summary.updatedAt()).isNull();
        assertThat(summary.expiresAt()).isNull();
        assertThat(summary.stale()).isTrue();
        assertThat(summary.ageMinutes()).isEqualTo(-1);
    }

    @Test
    @DisplayName("방금 업데이트된 상태는 stale=false이고 ageMinutes=0이다")
    void from_freshStatus_notStale() {
        Instant now = Instant.now();
        Cafe cafe = new Cafe("테스트카페", 37.5665, 126.9780, null, "token");
        CafeLiveStatus status = new CafeLiveStatus(
                cafe, CrowdLevel.NORMAL, Availability.YES, Availability.MAYBE, Availability.NO,
                now, now.plusSeconds(1800)
        );

        StatusSummary summary = StatusViewMapper.from(status, now);

        assertThat(summary.crowdLevel()).isEqualTo("NORMAL");
        assertThat(summary.party2()).isEqualTo("YES");
        assertThat(summary.party3()).isEqualTo("MAYBE");
        assertThat(summary.party4()).isEqualTo("NO");
        assertThat(summary.stale()).isFalse();
        assertThat(summary.ageMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("30분 이상 지난 상태는 stale=true이다")
    void from_oldStatus_isStale() {
        Instant updatedAt = Instant.now().minusSeconds(1801);
        Cafe cafe = new Cafe("테스트카페", 37.5665, 126.9780, null, "token");
        CafeLiveStatus status = new CafeLiveStatus(
                cafe, CrowdLevel.FULL, Availability.NO, Availability.NO, Availability.NO,
                updatedAt, updatedAt.plusSeconds(1800)
        );

        StatusSummary summary = StatusViewMapper.from(status, Instant.now());

        assertThat(summary.stale()).isTrue();
        assertThat(summary.ageMinutes()).isGreaterThanOrEqualTo(30);
    }
}
