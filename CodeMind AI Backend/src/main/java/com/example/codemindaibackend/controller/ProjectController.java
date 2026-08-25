package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.dto.project.ProjectCreateRequest;
import com.example.codemindaibackend.dto.project.ProjectQueryRequest;
import com.example.codemindaibackend.dto.project.ProjectUpdateRequest;
import com.example.codemindaibackend.service.ProjectService;
import com.example.codemindaibackend.vo.project.ProjectVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目管理接口
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 创建项目
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<ProjectVO> create(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success(projectService.createProject(request));
    }

    /**
     * 分页查询项目
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ProjectVO>> page(@Valid ProjectQueryRequest request) {
        return Result.success(projectService.pageProjects(request));
    }

    /**
     * 项目详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<ProjectVO> detail(@PathVariable Long id) {
        return Result.success(projectService.getProject(id));
    }

    /**
     * 修改项目
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<ProjectVO> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        return Result.success(projectService.updateProject(id, request));
    }

    /**
     * 删除项目（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.success(null);
    }
}
