package com.example.codemindaibackend.service;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.project.ProjectCreateRequest;
import com.example.codemindaibackend.dto.project.ProjectQueryRequest;
import com.example.codemindaibackend.dto.project.ProjectUpdateRequest;
import com.example.codemindaibackend.entity.Project;
import com.example.codemindaibackend.vo.project.ProjectVO;

import java.util.List;

/**
 * 项目业务接口
 *
 * @author CodeMind
 */
public interface ProjectService {

    /**
     * 分页查询项目
     */
    PageResult<ProjectVO> pageProjects(ProjectQueryRequest request);

    /**
     * 查询项目详情
     */
    ProjectVO getProject(Long id);

    /**
     * 创建项目
     */
    ProjectVO createProject(ProjectCreateRequest request);

    /**
     * 修改项目
     */
    ProjectVO updateProject(Long id, ProjectUpdateRequest request);

    /**
     * 删除项目（逻辑删除）
     */
    void deleteProject(Long id);

    /**
     * 校验项目访问权限（负责人或管理员），无权限/不存在抛异常
     */
    void checkProjectAccess(Long projectId);

    /**
     * 查询项目实体（不校验权限，项目已逻辑删除时返回 null）
     */
    Project getProjectRaw(Long id);

    /**
     * 当前用户负责的项目 ID 列表
     */
    List<Long> listOwnedProjectIds();
}
