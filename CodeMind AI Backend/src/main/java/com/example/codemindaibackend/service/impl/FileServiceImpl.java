package com.example.codemindaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.common.exception.ErrorCode;
import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.file.FileQueryRequest;
import com.example.codemindaibackend.entity.CodeFile;
import com.example.codemindaibackend.entity.Project;
import com.example.codemindaibackend.mapper.CodeFileMapper;
import com.example.codemindaibackend.security.SecurityUtils;
import com.example.codemindaibackend.service.FileService;
import com.example.codemindaibackend.service.ProjectService;
import com.example.codemindaibackend.service.StorageService;
import com.example.codemindaibackend.vo.file.FileVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代码文件业务实现
 *
 * @author CodeMind
 */
@Service
public class FileServiceImpl extends ServiceImpl<CodeFileMapper, CodeFile> implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    /** 上传大小上限：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /** 直存内容的小文本文件阈值：512KB */
    private static final int MAX_TEXT_CONTENT_SIZE = 512 * 1024;

    /** 同步到 AI 评审共享目录的代码文件扩展名（与 FastAPI CodeLoader 支持一致） */
    private static final Set<String> CODE_SYNC_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".ts", ".kt", ".go", ".cs", ".cpp", ".c", ".h");

    private final ProjectService projectService;

    private final StorageService storageService;

    /** 与 AI 服务共享的代码目录（挂载同一宿主目录），空=不同步 */
    @Value("${code.sync-dir:}")
    private String codeSyncDir;

    public FileServiceImpl(ProjectService projectService, StorageService storageService) {
        this.projectService = projectService;
        this.storageService = storageService;
    }

    @Override
    public PageResult<FileVO> pageFiles(FileQueryRequest request) {
        Page<CodeFile> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<CodeFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getProjectId() != null, CodeFile::getProjectId, request.getProjectId())
                .like(StringUtils.hasText(request.getFileName()), CodeFile::getFileName, request.getFileName())
                .orderByDesc(CodeFile::getCreateTime);

        // 数据隔离：普通用户仅见本人项目下的文件
        if (!SecurityUtils.isAdmin()) {
            List<Long> ownedIds = projectService.listOwnedProjectIds();
            if (ownedIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            wrapper.in(CodeFile::getProjectId, ownedIds);
        }

        IPage<CodeFile> result = page(page, wrapper);
        List<FileVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public FileVO getFile(Long id) {
        CodeFile file = getById(id);
        if (file == null) {
            throw BusinessException.notFound("文件不存在");
        }
        checkFileAccess(file);
        return toVO(file);
    }

    @Override
    public String getFileContent(Long id) {
        CodeFile file = getById(id);
        if (file == null) {
            throw BusinessException.notFound("文件不存在");
        }
        checkFileAccess(file);
        // 小文本文件直存于 DB，大文件回退存储读取
        if (StringUtils.hasText(file.getContent())) {
            return file.getContent();
        }
        byte[] bytes = storageService.load(file.getFilePath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public FileVO upload(MultipartFile file, Long projectId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        // 校验项目存在且有权限
        projectService.checkProjectAccess(projectId);
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过10MB");
        }

        try {
            byte[] bytes = file.getBytes();
            String checksum = sha256(bytes);
            String fileType = extractExtension(file.getOriginalFilename());
            // 委托存储抽象，本地/对象存储可替换
            String key = storageService.store(bytes, file.getOriginalFilename());

            CodeFile codeFile = new CodeFile();
            codeFile.setProjectId(projectId);
            codeFile.setFileName(file.getOriginalFilename());
            codeFile.setFilePath(key);
            codeFile.setFileType(fileType);
            codeFile.setFileSize(file.getSize());
            codeFile.setStorageUrl(key);
            codeFile.setChecksum(checksum);
            codeFile.setStatus(1);
            // 小文本文件直存内容
            if (bytes.length <= MAX_TEXT_CONTENT_SIZE && isTextFile(bytes)) {
                codeFile.setContent(new String(bytes, StandardCharsets.UTF_8));
            }
            save(codeFile);
            // 同步到 AI 评审共享目录，打通「上传 → 评审」链路
            syncToCodeDir(projectId, file.getOriginalFilename(), bytes);
            return toVO(codeFile);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public void deleteFile(Long id) {
        CodeFile file = getById(id);
        if (file == null) {
            throw BusinessException.notFound("文件不存在");
        }
        checkFileAccess(file);
        removeById(id);
        // 委托存储抽象删除物理文件
        storageService.delete(file.getFilePath());
    }

    /**
     * 数据权限：项目存在则校验负责人/管理员；项目已删除退化为校验文件上传人/管理员，保证孤儿文件可访问/删除。
     */
    private void checkFileAccess(CodeFile file) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Project project = projectService.getProjectRaw(file.getProjectId());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (project != null) {
            if (!Objects.equals(currentUserId, project.getOwnerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作该项目");
            }
        } else if (!Objects.equals(currentUserId, file.getCreateBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该文件");
        }
    }

    /**
     * 将上传的代码文件同步到与 AI 服务共享的评审目录（按项目分目录，保留原始文件名）。
     * 未配置 code.sync-dir 或非代码文件时跳过；失败仅告警，不阻断上传。
     */
    private void syncToCodeDir(Long projectId, String fileName, byte[] bytes) {
        if (!StringUtils.hasText(codeSyncDir)) {
            return;
        }
        String ext = extractExtension(fileName);
        if (!CODE_SYNC_EXTENSIONS.contains(ext)) {
            return;
        }
        // 仅取文件名（去路径分隔符），防目录穿越
        String safeName = fileName.replace('\\', '/');
        int slash = safeName.lastIndexOf('/');
        safeName = slash >= 0 ? safeName.substring(slash + 1) : safeName;
        if (!StringUtils.hasText(safeName) || ".".equals(safeName) || "..".equals(safeName)) {
            return;
        }
        Path baseDir = Paths.get(codeSyncDir).toAbsolutePath().normalize();
        Path target = baseDir.resolve(String.valueOf(projectId)).resolve(safeName).normalize();
        if (!target.startsWith(baseDir)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            log.warn("代码同步到评审目录失败: {}", target, e);
        }
    }

    /**
     * SHA-256 校验和
     */
    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
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

    /**
     * 判断是否文本文件（不含 NUL 字节）
     */
    private boolean isTextFile(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 实体转 VO
     */
    private FileVO toVO(CodeFile file) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(file, vo);
        return vo;
    }
}
