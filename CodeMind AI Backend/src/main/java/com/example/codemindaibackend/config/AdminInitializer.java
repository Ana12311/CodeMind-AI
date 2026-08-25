package com.example.codemindaibackend.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.codemindaibackend.entity.SysRole;
import com.example.codemindaibackend.entity.SysUser;
import com.example.codemindaibackend.entity.SysUserRole;
import com.example.codemindaibackend.mapper.SysRoleMapper;
import com.example.codemindaibackend.mapper.SysUserMapper;
import com.example.codemindaibackend.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理员初始化组件。
 *
 * 应用启动后检查管理员账号是否存在：
 *   - 存在：确保具备 ADMIN 角色，缺失则补绑。
 *   - 不存在：读取环境变量创建，密码用现有 BCrypt 机制加密，并分配 ADMIN 角色。
 * 用于替代 schema.sql 中内置固定管理员凭据的做法。
 *
 * 环境变量：
 *   ADMIN_USERNAME 管理员登录名（默认 admin）
 *   ADMIN_PASSWORD 管理员明文密码（至少 8 位，含字母和数字）
 *
 * 注意：全新库且未配置 ADMIN_PASSWORD 时，启动直接失败（fail-fast），
 * 避免应用空跑却无管理员可登录。
 *
 * @author CodeMind
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private static final String DEFAULT_NICKNAME = "管理员";

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final TransactionTemplate transactionTemplate;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    public AdminInitializer(SysUserMapper userMapper,
                            SysRoleMapper roleMapper,
                            SysUserRoleMapper userRoleMapper,
                            PasswordEncoder passwordEncoder,
                            PlatformTransactionManager transactionManager) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        SysUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, adminUsername));
        if (admin != null) {
            ensureAdminRole(admin);
            return;
        }

        // fail-fast：全新库却未配置密码，直接终止启动，避免无管理员可用
        if (!StringUtils.hasText(adminPassword)) {
            throw new IllegalStateException(
                    "管理员账号不存在，且未配置环境变量 ADMIN_PASSWORD。请设置 ADMIN_USERNAME / ADMIN_PASSWORD 后重启。");
        }
        validatePassword(adminPassword);

        transactionTemplate.executeWithoutResult(status -> createAdmin());
    }

    /**
     * 管理员已存在时，确保其具备 ADMIN 角色，缺失则补绑。
     */
    private void ensureAdminRole(SysUser admin) {
        List<SysRole> roles = roleMapper.selectByUserId(admin.getId());
        boolean hasAdmin = roles.stream().anyMatch(r -> ADMIN_ROLE_CODE.equals(r.getRoleCode()));
        if (hasAdmin) {
            log.info("管理员账号已存在，跳过初始化: {}", adminUsername);
            return;
        }
        if (bindAdminRole(admin.getId())) {
            log.info("管理员已存在但缺少 {} 角色，已补绑: {}", ADMIN_ROLE_CODE, adminUsername);
        }
    }

    /**
     * 在事务中创建管理员并分配 ADMIN 角色。
     */
    private void createAdmin() {
        // 清理历史软删同名记录，释放唯一键（与注册逻辑一致）
        userMapper.deletePhysicallyByUsername(adminUsername);

        SysUser user = new SysUser();
        user.setUsername(adminUsername);
        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setNickname(DEFAULT_NICKNAME);
        user.setStatus(1);
        userMapper.insert(user);

        bindAdminRole(user.getId());
        log.info("管理员初始化完成: username={}, role={}", adminUsername, ADMIN_ROLE_CODE);
    }

    /**
     * 绑定 ADMIN 角色。
     *
     * @return 是否成功绑定（ADMIN 角色不存在时返回 false）
     */
    private boolean bindAdminRole(Long userId) {
        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, ADMIN_ROLE_CODE));
        if (adminRole == null) {
            log.warn("未找到 {} 角色（请确认 schema.sql 已执行）", ADMIN_ROLE_CODE);
            return false;
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(userRole);
        return true;
    }

    /**
     * 密码强度校验：至少 8 位，且同时包含字母和数字。
     */
    private void validatePassword(String password) {
        boolean valid = password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*[0-9].*");
        if (!valid) {
            throw new IllegalStateException("ADMIN_PASSWORD 强度不足：至少 8 位，且需包含字母和数字");
        }
    }
}
