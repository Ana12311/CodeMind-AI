package com.example.codemindaibackend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建项目请求
 *
 * @author CodeMind
 */
public class ProjectCreateRequest {

    /** 项目名 */
    @NotBlank(message = "项目名不能为空")
    @Size(max = 100, message = "项目名长度不能超过100")
    private String name;

    /** 项目描述 */
    @Size(max = 500, message = "项目描述长度不能超过500")
    private String description;

    /** 主语言 */
    @Size(max = 50, message = "语言长度不能超过50")
    private String language;

    /** 仓库地址 */
    @Size(max = 255, message = "仓库地址长度不能超过255")
    private String repoUrl;

    /** 状态：1 进行中，0 归档（缺省 1） */
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
