package com.fintrack.dto;

public class ErrorResponse {
    private String type;
    private String message;
    private String code;

    public ErrorResponse(String type, String message, String code) {
        this.type = type;
        this.message = message;
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
} 