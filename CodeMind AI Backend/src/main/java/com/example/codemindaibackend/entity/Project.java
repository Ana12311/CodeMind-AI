package com.example.codemindaibackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 项目实体
 *
 * @author CodeMind
 */
@TableName("project")
public class Project extends BaseEntity {

    /** 项目名 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 负责人用户 ID */
    private Long ownerId;

    /** 主语言，如 Java/Python */
    private String language;

    /** 仓库地址 */
    private String repoUrl;

    /** 状态：1 进行中，0 归档 */
    private Integer status;

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
}
