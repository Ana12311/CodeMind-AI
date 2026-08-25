package com.example.codemindaibackend.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.codemindaibackend.entity.SysRole;
import com.example.codemindaibackend.entity.SysUser;
import com.example.codemindaibackend.mapper.SysRoleMapper;
import com.example.codemindaibackend.mapper.SysUserMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 加载用户及角色
 *
 * @author CodeMind
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    public UserDetailsServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new DisabledException("用户已被禁用");
        }

        List<SimpleGrantedAuthority> authorities = roleMapper.selectByUserId(user.getId()).stream()
                .map(SysRole::getRoleCode)
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toList());

        boolean enabled = user.getStatus() == null || user.getStatus() == 1;
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), authorities, enabled);
    }
}
