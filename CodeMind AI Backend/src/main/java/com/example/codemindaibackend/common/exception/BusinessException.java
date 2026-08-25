package com.example.codemindaibackend.common.exception;

/**
 * 业务异常
 *
 * @author CodeMind
 */
public class BusinessException extends RuntimeException {

    /** 状态码 */
    private final Integer code;

    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_ERROR, message);
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public Integer getCode() {
        return code;
    }

    /**
     * 资源不存在异常（404）
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message);
    }
}
