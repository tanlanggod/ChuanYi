package com.chuanyi.backend.common.api;

public class ApiResponse<T> {

    private int code;
    private String message;
    private String requestId;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<T>();
        resp.setCode(0);
        resp.setMessage("success");
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> resp = new ApiResponse<T>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
