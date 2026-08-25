package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.dto.user.AssignRoleRequest;
import com.example.codemindaibackend.dto.user.UserCreateRequest;
import com.example.codemindaibackend.dto.user.UserQueryRequest;
import com.example.codemindaibackend.dto.user.UserUpdateRequest;
import com.example.codemindaibackend.service.UserService;
import com.example.codemindaibackend.vo.user.UserVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（仅管理员）
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户
     */
    @GetMapping
    public Result<PageResult<UserVO>> page(@Valid UserQueryRequest request) {
        return Result.success(userService.pageUsers(request));
    }

    /**
     * 用户详情
     */
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.getUser(id));
    }

    /**
     * 新建用户
     */
    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    /**
     * 修改用户
     */
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    /**
     * 删除用户（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }

    /**
     * 分配角色
     */
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
        userService.assignRoles(id, request);
        return Result.success(null);
    }
}
