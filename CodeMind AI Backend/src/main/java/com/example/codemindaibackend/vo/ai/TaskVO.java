package com.example.codemindaibackend.vo.ai;

import java.time.LocalDateTime;

/**
 * AI 任务返回对象
 *
 * @author CodeMind
 */
public class TaskVO {

    /** 任务 ID */
    private Long id;

    /** 所属项目 ID */
    private Long projectId;

    /** 任务类型 */
    private String taskType;

    /** 状态码：0 待处理 1 处理中 2 成功 3 失败 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 请求参数 JSON */
    private String params;

    /** 关联结果记录 ID */
    private Long resultId;

    /** 失败原因 */
    private String errorMsg;

    /** 提交人 ID */
    private Long submitBy;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
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

    public Long getSubmitBy() {
        return submitBy;
    }

    public void setSubmitBy(Long submitBy) {
        this.submitBy = submitBy;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
