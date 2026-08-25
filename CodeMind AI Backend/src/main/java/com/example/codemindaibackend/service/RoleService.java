package com.example.codemindaibackend.service;

import com.example.codemindaibackend.vo.role.RoleVO;

import java.util.List;

/**
 * 角色业务接口
 *
 * @author CodeMind
 */
public interface RoleService {

    /**
     * 角色列表
     */
    List<RoleVO> listRoles();
}
