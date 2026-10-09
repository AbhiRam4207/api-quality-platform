package api_quality_platform.repository;

import api_quality_platform.entity.ContractComparisonHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractComparisonHistoryRepository extends JpaRepository<ContractComparisonHistory, Long> {
    List<ContractComparisonHistory> findByPreviousExecutionIdOrCurrentExecutionIdOrderByComparedAtDesc(
            Long previousExecutionId,
            Long currentExecutionId
    );
}
