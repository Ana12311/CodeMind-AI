package com.example.codemindaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.common.exception.ErrorCode;
import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.project.ProjectCreateRequest;
import com.example.codemindaibackend.dto.project.ProjectQueryRequest;
import com.example.codemindaibackend.dto.project.ProjectUpdateRequest;
import com.example.codemindaibackend.entity.Project;
import com.example.codemindaibackend.mapper.ProjectMapper;
import com.example.codemindaibackend.security.SecurityUtils;
import com.example.codemindaibackend.service.ProjectService;
import com.example.codemindaibackend.vo.project.ProjectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目业务实现
 *
 * @author CodeMind
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Override
    public PageResult<ProjectVO> pageProjects(ProjectQueryRequest request) {
        Page<Project> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getName()), Project::getName, request.getName())
                .eq(request.getStatus() != null, Project::getStatus, request.getStatus())
                .eq(request.getOwnerId() != null, Project::getOwnerId, request.getOwnerId());
        // 数据隔离：普通用户仅见本人项目，管理员全量
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(Project::getOwnerId, SecurityUtils.getCurrentUserId());
        }
        wrapper.orderByDesc(Project::getCreateTime);

        IPage<Project> result = page(page, wrapper);
        List<ProjectVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public ProjectVO getProject(Long id) {
        Project project = getById(id);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        checkPermission(project);
        return toVO(project);
    }

    @Override
    public ProjectVO createProject(ProjectCreateRequest request) {
        // 项目名唯一校验
        Long exists = this.lambdaQuery()
                .eq(Project::getName, request.getName())
                .count();
        if (exists > 0) {
            throw new BusinessException("项目名已存在");
        }

        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        // 负责人为当前登录用户
        project.setOwnerId(SecurityUtils.getCurrentUserId());
        if (request.getStatus() == null) {
            project.setStatus(1);
        }
        save(project);
        return toVO(project);
    }

    @Override
    public ProjectVO updateProject(Long id, ProjectUpdateRequest request) {
        Project project = getById(id);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        checkPermission(project);

        // 项目名变更时校验唯一
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(project.getName())) {
            Long exists = this.lambdaQuery()
                    .eq(Project::getName, request.getName())
                    .ne(Project::getId, id)
                    .count();
            if (exists > 0) {
                throw new BusinessException("项目名已存在");
            }
            project.setName(request.getName());
        }
        // 局部更新：仅非 null 字段覆盖
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getLanguage() != null) {
            project.setLanguage(request.getLanguage());
        }
        if (request.getRepoUrl() != null) {
            project.setRepoUrl(request.getRepoUrl());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        updateById(project);
        return toVO(project);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = getById(id);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        checkPermission(project);
        // 逻辑删除，依赖 BaseEntity.deleted 上的 @TableLogic
        removeById(id);
    }

    @Override
    public void checkProjectAccess(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        checkPermission(project);
    }

    @Override
    public Project getProjectRaw(Long id) {
        return getById(id);
    }

    @Override
    public List<Long> listOwnedProjectIds() {
        return this.lambdaQuery()
                .eq(Project::getOwnerId, SecurityUtils.getCurrentUserId())
                .list().stream().map(Project::getId).collect(Collectors.toList());
    }

    /**
     * 数据权限校验：仅负责人或管理员可操作
     */
    private void checkPermission(Project project) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(project.getOwnerId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作该项目");
        }
    }

    /**
     * 实体转 VO
     */
    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(project, vo);
        return vo;
    }
}
