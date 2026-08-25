package com.example.codemindaibackend.dto.user;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分配角色请求
 *
 * @author CodeMind
 */
public class AssignRoleRequest {

    /** 角色 ID 列表（空列表表示清除全部角色） */
    @NotNull(message = "角色列表不能为 null")
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
