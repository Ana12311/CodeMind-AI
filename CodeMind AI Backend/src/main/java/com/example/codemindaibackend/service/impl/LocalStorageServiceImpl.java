package com.example.codemindaibackend.service.impl;

import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储实现（开发/单机）。多实例部署请替换为对象存储实现。
 *
 * @author CodeMind
 */
@Service
public class LocalStorageServiceImpl implements StorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public String store(byte[] bytes, String originalName) {
        String ext = extractExtension(originalName);
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        String key = dateDir + "/" + storedName;

        Path target = resolveSafe(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }
        return key;
    }

    @Override
    public byte[] load(String key) {
        if (!StringUtils.hasText(key)) {
            throw BusinessException.notFound("文件不存在");
        }
        Path target = resolveSafe(key);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw BusinessException.notFound("文件不存在");
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(key));
        } catch (IOException ignored) {
            // 删除失败不影响业务
        }
    }

    /**
     * 解析存储 key 为安全绝对路径，防目录穿越
     */
    private Path resolveSafe(String key) {
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = baseDir.resolve(key).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BusinessException("非法存储路径");
        }
        return target;
    }

    /**
     * 提取扩展名（含点，小写）
     */
    private String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot).toLowerCase() : "";
    }
}
