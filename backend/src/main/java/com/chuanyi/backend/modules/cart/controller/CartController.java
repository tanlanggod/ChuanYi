package com.chuanyi.backend.modules.cart.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.modules.cart.service.CartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> detail() {
        return ApiResponse.ok(cartService.detail());
    }

    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> addItem(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(cartService.addItem(body));
    }

    @PutMapping("/items/{cartItemId}")
    public ApiResponse<Map<String, Object>> updateQty(@PathVariable Long cartItemId,
                                                      @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(cartService.updateQty(cartItemId, body));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long cartItemId) {
        return ApiResponse.ok(cartService.delete(cartItemId));
    }

    @PutMapping("/items/{cartItemId}/select")
    public ApiResponse<Map<String, Object>> select(@PathVariable Long cartItemId,
                                                   @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(cartService.select(cartItemId, body));
    }

    @PostMapping("/items/select-all")
    public ApiResponse<Map<String, Object>> selectAll(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(cartService.selectAll(body));
    }

    @PostMapping("/settlement/preview")
    public ApiResponse<Map<String, Object>> preview(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(cartService.settlementPreview(body));
    }
}