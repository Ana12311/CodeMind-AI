package com.example.codemindaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.codemindaibackend.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-角色关联 Mapper
 *
 * @author CodeMind
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 物理删除用户的所有角色映射
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
