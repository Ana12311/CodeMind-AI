package com.example.codemindaibackend.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询审查结果请求
 *
 * @author CodeMind
 */
public class ReviewResultQueryRequest {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;

    /** 所属项目 ID */
    private Long projectId;

    /** 关联任务 ID */
    private Long taskId;

    /** 审查类型 */
    private String reviewType;

    /** 严重程度 */
    private String severity;

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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
