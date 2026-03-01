package com.chuanyi.backend.modules.logistics.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.modules.logistics.service.LogisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @GetMapping("/track")
    public ApiResponse<Map<String, Object>> track(@RequestParam String carrierCode,
                                                  @RequestParam String trackingNo) {
        return ApiResponse.ok(logisticsService.track(carrierCode, trackingNo));
    }
}
