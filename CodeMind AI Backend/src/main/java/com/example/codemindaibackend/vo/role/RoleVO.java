package com.example.codemindaibackend.vo.role;

/**
 * 角色返回对象
 *
 * @author CodeMind
 */
public class RoleVO {

    /** 角色 ID */
    private Long id;

    /** 角色编码 */
    private String roleCode;

    /** 角色名 */
    private String roleName;

    /** 描述 */
    private String description;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
