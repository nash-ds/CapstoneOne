package com.hdfc.utility;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String message;
    private T data;
    private int status;
    
    public ApiResponse( int status, String message, T data) {
        this.message = message;
        this.data = data;
        this.status = status;
    }

    public ApiResponse( int status, String message) {
        this.message = message;
        this.status = status;
    }

    public static <T> ApiResponse<T> success( int status,String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    // Static helper for error responses
    public static <T> ApiResponse<T> error(int status,String message) {
        return new ApiResponse<>(status, message);
    }
}
