package com.example.codemindaibackend.service;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.user.AssignRoleRequest;
import com.example.codemindaibackend.dto.user.UserCreateRequest;
import com.example.codemindaibackend.dto.user.UserQueryRequest;
import com.example.codemindaibackend.dto.user.UserUpdateRequest;
import com.example.codemindaibackend.vo.user.UserVO;

/**
 * 用户业务接口
 *
 * @author CodeMind
 */
public interface UserService {

    /**
     * 分页查询用户
     */
    PageResult<UserVO> pageUsers(UserQueryRequest request);

    /**
     * 用户详情
     */
    UserVO getUser(Long id);

    /**
     * 新建用户
     */
    UserVO createUser(UserCreateRequest request);

    /**
     * 修改用户
     */
    UserVO updateUser(Long id, UserUpdateRequest request);

    /**
     * 删除用户（逻辑删除）
     */
    void deleteUser(Long id);

    /**
     * 分配角色
     */
    void assignRoles(Long id, AssignRoleRequest request);
}
