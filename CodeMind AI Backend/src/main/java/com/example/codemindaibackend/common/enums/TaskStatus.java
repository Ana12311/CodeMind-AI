package com.example.codemindaibackend.common.enums;

/**
 * AI 任务状态
 *
 * @author CodeMind
 */
public enum TaskStatus {

    WAITING(0, "等待处理"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final Integer code;

    private final String desc;

    TaskStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 状态码是否合法
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
