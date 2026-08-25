package com.example.codemindaibackend.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询 AI 任务请求
 *
 * @author CodeMind
 */
public class TaskQueryRequest {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;

    /** 所属项目 ID */
    private Long projectId;

    /** 任务类型 */
    private String taskType;

    /** 状态：0 待处理 1 处理中 2 成功 3 失败 */
    private Integer status;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
