package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.dto.file.FileQueryRequest;
import com.example.codemindaibackend.service.FileService;
import com.example.codemindaibackend.vo.file.FileVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 代码文件接口
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("projectId") Long projectId) {
        return Result.success(fileService.upload(file, projectId));
    }

    /**
     * 分页查询文件
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<FileVO>> page(@Valid FileQueryRequest request) {
        return Result.success(fileService.pageFiles(request));
    }

    /**
     * 文件详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<FileVO> detail(@PathVariable Long id) {
        return Result.success(fileService.getFile(id));
    }

    /**
     * 删除文件（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.success(null);
    }
}
