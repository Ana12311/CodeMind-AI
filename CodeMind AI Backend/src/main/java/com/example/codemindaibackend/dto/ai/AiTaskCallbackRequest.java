package com.example.codemindaibackend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AI 结果回调请求（FastAPI → 本服务）
 *
 * @author CodeMind
 */
public class AiTaskCallbackRequest {

    /** 任务 ID */
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    /** 回调状态：SUCCESS / FAILED */
    @NotBlank(message = "回调状态不能为空")
    private String status;

    /** 结果内容（JSON / 文本），失败时为错误信息 */
    private String result;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
