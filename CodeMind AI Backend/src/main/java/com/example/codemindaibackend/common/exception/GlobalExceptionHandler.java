package com.example.codemindaibackend.common.exception;

import com.example.codemindaibackend.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author CodeMind
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 权限不足（方法级 @PreAuthorize 拒绝，返回 HTTP 403）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.failure(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }

    /**
     * 认证失败（登录用户名/密码错误等，返回 HTTP 401）
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.failure(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误"));
    }

    /**
     * 唯一键冲突（并发重复提交）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据重复", e);
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), "数据已存在，请勿重复提交");
    }

    /**
     * 上传文件超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), "上传文件超过大小限制");
    }

    /**
     * 参数类型不匹配（如 id 传 abc）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), "参数类型错误");
    }

    /**
     * 请求体不可读（JSON 格式错误）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.failure(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
