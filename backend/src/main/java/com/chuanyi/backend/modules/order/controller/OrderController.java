package com.chuanyi.backend.modules.order.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.modules.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.createFromSnapshot(body));
    }

    @PostMapping("/create-from-cart")
    public ApiResponse<Map<String, Object>> createFromCart(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.createFromCart(body));
    }

    @PostMapping("/create-direct-goods")
    public ApiResponse<Map<String, Object>> createDirectGoods(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.createDirectGoods(body));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(orderService.list(status, pageNo, pageSize));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable String orderNo,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(orderService.cancel(orderNo, body));
    }

    @PostMapping("/{orderNo}/ship")
    public ApiResponse<Map<String, Object>> ship(@PathVariable String orderNo,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(orderService.ship(orderNo, body));
    }

    @PostMapping("/{orderNo}/confirm-receipt")
    public ApiResponse<Map<String, Object>> confirmReceipt(@PathVariable String orderNo,
                                                           @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(orderService.confirmReceipt(orderNo, body));
    }
}
