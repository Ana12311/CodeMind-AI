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

/**
 * 管理员初始化组件。
 *
 * 应用启动后检查管理员账号是否存在；不存在则依据环境变量创建，
 * 密码使用现有 BCrypt 机制加密，并分配 ADMIN 角色。
 * 用于替代 schema.sql 中内置固定管理员凭据的做法。
 *
 * 环境变量：
 *   ADMIN_USERNAME 管理员登录名（默认 admin）
 *   ADMIN_PASSWORD 管理员明文密码（未配置则跳过创建）
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
        if (!StringUtils.hasText(adminPassword)) {
            log.warn("未配置环境变量 ADMIN_PASSWORD，跳过管理员初始化");
            return;
        }

        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, adminUsername));
        if (exists != null && exists > 0) {
            log.info("管理员账号已存在，跳过初始化: {}", adminUsername);
            return;
        }

        transactionTemplate.executeWithoutResult(status -> createAdmin());
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

        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, ADMIN_ROLE_CODE));
        if (adminRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(adminRole.getId());
            userRoleMapper.insert(userRole);
            log.info("管理员初始化完成: username={}, role={}", adminUsername, ADMIN_ROLE_CODE);
        } else {
            log.warn("未找到 {} 角色（请确认 schema.sql 已执行），管理员已创建但未分配角色", ADMIN_ROLE_CODE);
        }
    }
}
