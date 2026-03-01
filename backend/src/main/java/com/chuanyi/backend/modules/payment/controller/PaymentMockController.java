package com.chuanyi.backend.modules.payment.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.modules.payment.service.PaymentMockService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/mock")
public class PaymentMockController {

    private final PaymentMockService paymentMockService;

    public PaymentMockController(PaymentMockService paymentMockService) {
        this.paymentMockService = paymentMockService;
    }

    @PostMapping("/pay")
    public ApiResponse<Map<String, Object>> pay(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(paymentMockService.pay(body));
    }

    @PostMapping("/callback")
    public ApiResponse<Boolean> callback(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(paymentMockService.callback(body));
    }
}
