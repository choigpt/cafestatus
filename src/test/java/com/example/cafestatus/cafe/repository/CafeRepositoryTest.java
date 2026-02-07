package com.example.cafestatus.cafe.repository;

import com.example.cafestatus.cafe.entity.Cafe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("카페 Repository 테스트")
class CafeRepositoryTest {

    @Autowired CafeRepository cafeRepository;

    @Test
    @DisplayName("위경도 bounding box 범위 내 카페만 조회된다")
    void findInBoundingBox_returnsOnlyCafesInRange() {
        cafeRepository.save(new Cafe("A", 37.5665, 126.9780, null, "t1"));
        cafeRepository.save(new Cafe("B", 37.5667, 126.9782, null, "t2"));
        cafeRepository.save(new Cafe("C", 35.1796, 129.0756, null, "t3"));

        double minLat = 37.0, maxLat = 38.0;
        double minLng = 126.0, maxLng = 127.5;

        List<Cafe> result = cafeRepository.findInBoundingBox(minLat, maxLat, minLng, maxLng);

        assertThat(result).extracting(Cafe::getName).contains("A", "B");
        assertThat(result).extracting(Cafe::getName).doesNotContain("C");
    }
}
