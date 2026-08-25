package com.example.codemindaibackend.dto.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新任务状态请求
 *
 * @author CodeMind
 */
public class TaskStatusUpdateRequest {

    /** 状态：0 待处理 1 处理中 2 成功 3 失败 */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 失败原因 */
    @Size(max = 1000, message = "失败原因长度不能超过1000")
    private String errorMsg;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
