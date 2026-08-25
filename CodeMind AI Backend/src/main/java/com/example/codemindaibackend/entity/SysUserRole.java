package com.example.codemindaibackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户-角色关联实体
 *
 * @author CodeMind
 */
@TableName("sys_user_role")
public class SysUserRole extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 角色 ID */
    private Long roleId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
