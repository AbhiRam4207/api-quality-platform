package api_quality_platform.repository;

import api_quality_platform.entity.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findByTestIdOrderByExecutedAtDesc(Long testId);
    TestExecution findFirstByTestIdAndIdLessThanOrderByIdDesc(
            Long testId,
            Long executionId
    );
}
