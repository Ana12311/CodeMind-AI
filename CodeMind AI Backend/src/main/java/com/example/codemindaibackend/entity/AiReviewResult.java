package com.example.codemindaibackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * AI 审查结果记录实体
 *
 * @author CodeMind
 */
@TableName("ai_review_result")
public class AiReviewResult extends BaseEntity {

    /** 关联任务 ID */
    private Long taskId;

    /** 所属项目 ID */
    private Long projectId;

    /** 关联代码文件 ID */
    private Long fileId;

    /** 审查类型，如 BUG/SECURITY/STYLE */
    private String reviewType;

    /** 严重程度，如 LOW/MID/HIGH/CRITICAL */
    private String severity;

    /** 行号 */
    private Integer lineNo;

    /** 问题摘要 */
    private String summary;

    /** 详细结果 JSON */
    private String detail;

    /** 状态 */
    private Integer status;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
