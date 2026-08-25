package com.example.codemindaibackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 代码文件实体
 *
 * @author CodeMind
 */
@TableName("code_file")
public class CodeFile extends BaseEntity {

    /** 所属项目 ID */
    private Long projectId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径 / 相对路径 */
    private String filePath;

    /** 文件类型（扩展名，含点） */
    private String fileType;

    /** 字节数 */
    private Long fileSize;

    /** 存储地址 */
    private String storageUrl;

    /** SHA-256 校验和 */
    private String checksum;

    /** 文件内容（小文本文件直存） */
    private String content;

    /** 状态 */
    private Integer status;

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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
