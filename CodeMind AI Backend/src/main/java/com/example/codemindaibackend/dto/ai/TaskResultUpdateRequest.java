package com.example.codemindaibackend.dto.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 保存任务结果请求
 *
 * @author CodeMind
 */
public class TaskResultUpdateRequest {

    /** 状态：仅 2 成功 / 3 失败 */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 关联结果记录 ID */
    private Long resultId;

    /** 失败原因 */
    @Size(max = 1000, message = "失败原因长度不能超过1000")
    private String errorMsg;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
