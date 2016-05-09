package com.magnet.max.common;


public class ApiException extends RuntimeException {
    private int errorCode;
    private String errorMessage;

    public ApiException() {

    }
    public ApiException (int code, String errorMessage) {
        super(errorMessage);
        this.setErrorCode(code);
        this.setErrorMessage(errorMessage);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
