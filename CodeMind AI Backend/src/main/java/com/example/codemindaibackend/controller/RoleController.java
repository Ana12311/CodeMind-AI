package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.service.RoleService;
import com.example.codemindaibackend.vo.role.RoleVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色接口（仅管理员）
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 角色列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.listRoles());
    }
}
