package com.example.codemindaibackend.service;

/**
 * 文件存储抽象：本地磁盘 / 对象存储（S3 / MinIO / OSS）可插拔
 *
 * @author CodeMind
 */
public interface StorageService {

    /**
     * 存储文件，返回存储 key（相对路径）
     */
    String store(byte[] bytes, String originalName);

    /**
     * 读取文件内容
     */
    byte[] load(String key);

    /**
     * 删除文件，key 不存在不报错
     */
    void delete(String key);
}
