package com.example.codemindaibackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色实体
 *
 * @author CodeMind
 */
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色编码，如 ADMIN/USER */
    private String roleCode;

    /** 角色名 */
    private String roleName;

    /** 描述 */
    private String description;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
