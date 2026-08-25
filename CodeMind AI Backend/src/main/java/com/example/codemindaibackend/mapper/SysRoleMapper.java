package com.example.codemindaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.codemindaibackend.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 *
 * @author CodeMind
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户拥有的角色
     */
    @Select("SELECT r.id, r.role_code AS roleCode, r.role_name AS roleName, r.description, r.status " +
            "FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0 AND ur.deleted = 0")
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
