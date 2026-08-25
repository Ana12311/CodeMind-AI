package com.example.codemindaibackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * AI 任务实体
 *
 * @author CodeMind
 */
@TableName("ai_task")
public class AiTask extends BaseEntity {

    /** 所属项目 ID */
    private Long projectId;

    /** 任务类型，如 CODE_REVIEW */
    private String taskType;

    /** 状态：0 待处理 1 处理中 2 成功 3 失败 */
    private Integer status;

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
}
