package com.example.codemindaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.common.exception.ErrorCode;
import com.example.codemindaibackend.dto.auth.LoginRequest;
import com.example.codemindaibackend.dto.auth.RefreshTokenRequest;
import com.example.codemindaibackend.dto.auth.RegisterRequest;
import com.example.codemindaibackend.entity.SysRole;
import com.example.codemindaibackend.entity.SysUser;
import com.example.codemindaibackend.entity.SysUserRole;
import com.example.codemindaibackend.mapper.SysRoleMapper;
import com.example.codemindaibackend.mapper.SysUserMapper;
import com.example.codemindaibackend.mapper.SysUserRoleMapper;
import com.example.codemindaibackend.security.JwtTokenProvider;
import com.example.codemindaibackend.security.LoginUser;
import com.example.codemindaibackend.security.SecurityUtils;
import com.example.codemindaibackend.service.AuthService;
import com.example.codemindaibackend.vo.auth.LoginResponse;
import com.example.codemindaibackend.vo.auth.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证业务实现
 *
 * @author CodeMind
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:user:";

    private static final String TOKEN_TYPE = "Bearer";

    private static final String DEFAULT_ROLE_CODE = "USER";

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider tokenProvider;

    private final StringRedisTemplate stringRedisTemplate;

    public AuthServiceImpl(SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider,
                           StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 清理历史软删的同名记录，释放唯一键
        userMapper.deletePhysicallyByUsername(request.getUsername());

        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);

        // 绑定默认角色 USER
        SysRole defaultRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE));
        if (defaultRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(defaultRole.getId());
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return issueToken(loginUser.getUserId(), loginUser.getUsername());
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }

        Long userId = tokenProvider.getUserId(refreshToken);
        String username = tokenProvider.getUsername(refreshToken);

        String cached = stringRedisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (cached == null || !cached.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌已失效");
        }

        return issueToken(userId, username);
    }

    @Override
    public void logout(String accessToken) {
        Long userId = SecurityUtils.getCurrentUserId();
        stringRedisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        // Access Token 加入黑名单，剩余有效期内拒绝
        if (StringUtils.hasText(accessToken)) {
            long remaining = tokenProvider.getRemainingMillis(accessToken);
            if (remaining > 0) {
                stringRedisTemplate.opsForValue().set(
                        JwtTokenProvider.BLACKLIST_KEY_PREFIX + accessToken, "1", Duration.ofMillis(remaining));
            }
        }
    }

    @Override
    public UserInfoVO getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        List<String> roles = roleMapper.selectByUserId(userId).stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoles(roles);
        return vo;
    }

    /**
     * 签发访问令牌 + 刷新令牌，刷新令牌存 Redis
     */
    private LoginResponse issueToken(Long userId, String username) {
        String accessToken = tokenProvider.generateAccessToken(userId, username);
        String refreshToken = tokenProvider.generateRefreshToken(userId, username);
        stringRedisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(tokenProvider.getRefreshTokenExpiration()));
        return new LoginResponse(accessToken, refreshToken, TOKEN_TYPE,
                tokenProvider.getAccessTokenExpiration() / 1000);
    }
}
