package com.jiangyang.datacalculation.exception;

/**
 * 计算服务异常
 * 
 * @author jiangyang
 * @since 1.0.0
 */
public class CalculationServiceException extends RuntimeException {

    private final int errorCode;

    public CalculationServiceException(String message) {
        super(message);
        this.errorCode = 500;
    }

    public CalculationServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 500;
    }

    public CalculationServiceException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CalculationServiceException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
