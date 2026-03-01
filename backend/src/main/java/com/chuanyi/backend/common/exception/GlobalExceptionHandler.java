package com.chuanyi.backend.common.exception;

import com.chuanyi.backend.common.api.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Object> handleBiz(BizException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Object> handleValid(MethodArgumentNotValidException ex) {
        String message = "invalid request";
        if (ex.getBindingResult().getFieldError() != null) {
            message = ex.getBindingResult().getFieldError().getField()
                    + ": " + ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleUnknown(Exception ex) {
        return ApiResponse.fail(500, ex.getMessage());
    }
}
