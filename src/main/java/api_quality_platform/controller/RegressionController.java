package api_quality_platform.controller;

import api_quality_platform.dto.RegressionResponse;
import api_quality_platform.service.RegressionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/regressions")
public class RegressionController {

    private final RegressionService regressionService;

    public RegressionController(RegressionService regressionService) {
        this.regressionService = regressionService;
    }

    @GetMapping("/test/{testId}/execution/{executionId}")
    public RegressionResponse checkRegression(
            @PathVariable Long testId,
            @PathVariable Long executionId) {

        return regressionService.checkRegression(
                testId,
                executionId
        );
    }
}