package com.example.codemindaibackend.vo.project;

import java.time.LocalDateTime;

/**
 * 项目返回对象
 *
 * @author CodeMind
 */
public class ProjectVO {

    /** 主键 */
    private Long id;

    /** 项目名 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 负责人用户 ID */
    private Long ownerId;

    /** 主语言 */
    private String language;

    /** 仓库地址 */
    private String repoUrl;

    /** 状态：1 进行中，0 归档 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建人 ID */
    private Long createBy;

    /** 更新人 ID */
    private Long updateBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }
}
