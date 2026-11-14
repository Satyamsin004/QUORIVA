package com.csscholar.backend.learning.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.learning.dto.DailyActivityDto;
import com.csscholar.backend.learning.dto.LearningAnalyticsOverviewDto;
import com.csscholar.backend.learning.dto.TopicPerformanceDto;
import com.csscholar.backend.learning.service.LearningAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/analytics")
public class LearningAnalyticsController {

    private final LearningAnalyticsService analyticsService;

    public LearningAnalyticsController(LearningAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<LearningAnalyticsOverviewDto>> getOverview(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LearningAnalyticsOverviewDto overview = analyticsService.getOverview(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Learning analytics overview retrieved.", overview));
    }

    @GetMapping("/topics")
    public ResponseEntity<ApiResponse<List<TopicPerformanceDto>>> getTopicPerformance(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TopicPerformanceDto> topics = analyticsService.getTopicPerformance(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Topic learning performance retrieved.", topics));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<DailyActivityDto>>> getActivityHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "14") int days
    ) {
        List<DailyActivityDto> activity = analyticsService.getActivityHistory(principal.getId(), days);
        return ResponseEntity.ok(ApiResponse.success("Daily learning activity retrieved.", activity));
    }
}
