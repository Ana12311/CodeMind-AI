package com.example.codemindaibackend.dto.project;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询项目请求
 *
 * @author CodeMind
 */
public class ProjectQueryRequest {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;

    /** 项目名（模糊） */
    private String name;

    /** 状态：1 进行中，0 归档 */
    private Integer status;

    /** 负责人用户 ID */
    private Long ownerId;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
}
