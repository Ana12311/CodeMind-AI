package com.example.codemindaibackend.dto.ai;

import jakarta.validation.constraints.Size;

/**
 * 保存审查结果请求
 *
 * @author CodeMind
 */
public class ReviewResultCreateRequest {

    /** 关联任务 ID */
    private Long taskId;

    /** 所属项目 ID */
    private Long projectId;

    /** 关联代码文件 ID */
    private Long fileId;

    /** 审查类型 */
    @Size(max = 50, message = "审查类型长度不能超过50")
    private String reviewType;

    /** 严重程度 */
    @Size(max = 20, message = "严重程度长度不能超过20")
    private String severity;

    /** 行号 */
    private Integer lineNo;

    /** 问题摘要 */
    @Size(max = 500, message = "问题摘要长度不能超过500")
    private String summary;

    /** 详细结果 JSON */
    private String detail;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
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

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
