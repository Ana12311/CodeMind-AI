package com.example.codemindaibackend.dto.file;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询文件请求
 *
 * @author CodeMind
 */
public class FileQueryRequest {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;

    /** 所属项目 ID */
    private Long projectId;

    /** 文件名（模糊） */
    private String fileName;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
