package com.example.codemindaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.codemindaibackend.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper
 *
 * @author CodeMind
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 物理删除已逻辑删除的同名用户，释放唯一键
     */
    @Delete("DELETE FROM sys_user WHERE username = #{username} AND deleted = 1")
    int deletePhysicallyByUsername(@Param("username") String username);
}
