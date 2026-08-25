package com.example.codemindaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.user.AssignRoleRequest;
import com.example.codemindaibackend.dto.user.UserCreateRequest;
import com.example.codemindaibackend.dto.user.UserQueryRequest;
import com.example.codemindaibackend.dto.user.UserUpdateRequest;
import com.example.codemindaibackend.entity.SysRole;
import com.example.codemindaibackend.entity.SysUser;
import com.example.codemindaibackend.entity.SysUserRole;
import com.example.codemindaibackend.mapper.SysRoleMapper;
import com.example.codemindaibackend.mapper.SysUserMapper;
import com.example.codemindaibackend.mapper.SysUserRoleMapper;
import com.example.codemindaibackend.service.UserService;
import com.example.codemindaibackend.vo.user.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务实现
 *
 * @author CodeMind
 */
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<UserVO> pageUsers(UserQueryRequest request) {
        Page<SysUser> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getUsername()), SysUser::getUsername, request.getUsername())
                .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
                .orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> result = page(page, wrapper);
        List<UserVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public UserVO getUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO createUser(UserCreateRequest request) {
        Long exists = this.lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername())
                .count();
        if (exists > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 清理历史软删的同名记录，释放唯一键
        this.baseMapper.deletePhysicallyByUsername(request.getUsername());

        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
        return toVO(user);
    }

    @Override
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        // 局部更新：仅非 null 字段覆盖
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        updateById(user);
        return toVO(user);
    }

    @Override
    public void deleteUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long id, AssignRoleRequest request) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        List<Long> roleIds = request.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            Long count = roleMapper.selectCount(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
            if (count == null || count.intValue() != roleIds.size()) {
                throw new BusinessException("存在无效角色");
            }
        }

        // 物理删除旧映射，避免逻辑删除残留撞唯一键
        userRoleMapper.deleteByUserId(id);
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }

    /**
     * 实体转 VO（含角色编码）
     */
    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        List<String> roles = roleMapper.selectByUserId(user.getId()).stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
        vo.setRoles(roles);
        return vo;
    }
}
