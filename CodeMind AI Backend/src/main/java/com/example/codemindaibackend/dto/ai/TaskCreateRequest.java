package com.example.codemindaibackend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建 AI 任务请求
 *
 * @author CodeMind
 */
public class TaskCreateRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "任务类型不能为空")
    @Size(max = 50, message = "任务类型长度不能超过50")
    private String taskType;

    /** 请求参数 JSON（兼容旧字段） */
    private String params;

    /** 任务内容（对应 FastAPI 请求体 content 字段） */
    private String content;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
