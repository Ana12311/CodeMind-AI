package com.example.codemindaibackend.common.exception;

/**
 * 错误码枚举
 *
 * @author CodeMind
 */
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(400, "业务处理失败"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
