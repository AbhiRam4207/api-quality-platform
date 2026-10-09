package api_quality_platform.repository;

import api_quality_platform.entity.ApiTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiTestRepository extends JpaRepository<ApiTest, Long> {
    List<ApiTest> findByApiId(Long apiId);
}
