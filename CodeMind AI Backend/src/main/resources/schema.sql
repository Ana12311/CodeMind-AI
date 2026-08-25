-- =============================================================
-- CodeMind AI Backend 数据库初始化脚本
-- 执行方式：mysql -uroot -p < schema.sql
-- 编码：utf8mb4，引擎：InnoDB，主键：BIGINT 雪花 ID，逻辑删除：deleted
-- =============================================================

CREATE DATABASE IF NOT EXISTS codemind DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE codemind;

-- ---------------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------------
-- 说明：sys_user.username 唯一键与逻辑删除（deleted）存在天然冲突——
-- 用户 A 逻辑删除后，同名用户无法再插入（唯一键仍被占用）。
-- 解决：注册/新建用户前，先物理删除已逻辑删除的同名记录
-- （见 SysUserMapper.deletePhysicallyByUsername，仅删 deleted=1 行，不影响有效数据）。
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT '密码密文(BCrypt)',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    status      TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT       DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

-- ---------------------------------------------------------------
-- 角色表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL COMMENT '主键',
    role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    status      TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT       DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';

-- ---------------------------------------------------------------
-- 用户-角色关联表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT   NOT NULL COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    role_id     BIGINT   NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    create_by   BIGINT   DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT   DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT  DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户-角色关联表';

-- ---------------------------------------------------------------
-- 项目表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project (
    id          BIGINT       NOT NULL COMMENT '主键',
    name        VARCHAR(100) NOT NULL COMMENT '项目名',
    description VARCHAR(500) DEFAULT NULL COMMENT '项目描述',
    owner_id    BIGINT       NOT NULL COMMENT '负责人用户ID',
    language    VARCHAR(50)  DEFAULT NULL COMMENT '主语言',
    repo_url    VARCHAR(255) DEFAULT NULL COMMENT '仓库地址',
    status      TINYINT      DEFAULT 1 COMMENT '1进行中 0归档',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT       DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_owner_id (owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '项目表';

-- ---------------------------------------------------------------
-- 代码文件表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS code_file (
    id          BIGINT       NOT NULL COMMENT '主键',
    project_id  BIGINT       NOT NULL COMMENT '所属项目ID',
    file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path   VARCHAR(500) DEFAULT NULL COMMENT '存储路径/相对路径',
    file_type   VARCHAR(50)  DEFAULT NULL COMMENT '文件类型',
    file_size   BIGINT       DEFAULT NULL COMMENT '字节数',
    storage_url VARCHAR(500) DEFAULT NULL COMMENT '存储地址',
    checksum    VARCHAR(64)  DEFAULT NULL COMMENT '校验和',
    content     LONGTEXT     DEFAULT NULL COMMENT '文件内容(小文件直存)',
    status      TINYINT      DEFAULT 1 COMMENT '状态',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT       DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码文件表';

-- ---------------------------------------------------------------
-- AI 任务表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_task (
    id          BIGINT        NOT NULL COMMENT '主键',
    project_id  BIGINT        NOT NULL COMMENT '所属项目ID',
    task_type   VARCHAR(50)   NOT NULL COMMENT '任务类型',
    status      TINYINT       DEFAULT 0 COMMENT '0待处理 1处理中 2成功 3失败',
    params      TEXT          DEFAULT NULL COMMENT '请求参数JSON',
    result_id   BIGINT        DEFAULT NULL COMMENT '关联结果记录ID',
    error_msg   VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    submit_by   BIGINT        DEFAULT NULL COMMENT '提交人ID',
    start_time  DATETIME      DEFAULT NULL COMMENT '开始时间',
    end_time    DATETIME      DEFAULT NULL COMMENT '结束时间',
    create_time DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME      DEFAULT NULL COMMENT '更新时间',
    create_by   BIGINT        DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT        DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT       DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI任务表';

-- ---------------------------------------------------------------
-- AI 审查结果记录表
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_review_result (
    id          BIGINT       NOT NULL COMMENT '主键',
    task_id     BIGINT       DEFAULT NULL COMMENT '关联任务ID',
    project_id  BIGINT       DEFAULT NULL COMMENT '所属项目ID',
    file_id     BIGINT       DEFAULT NULL COMMENT '关联代码文件ID',
    review_type VARCHAR(50)  DEFAULT NULL COMMENT '审查类型',
    severity    VARCHAR(20)  DEFAULT NULL COMMENT '严重程度',
    line_no     INT          DEFAULT NULL COMMENT '行号',
    summary     VARCHAR(500) DEFAULT NULL COMMENT '问题摘要',
    detail      TEXT         DEFAULT NULL COMMENT '详细结果JSON',
    status      TINYINT      DEFAULT 1 COMMENT '状态',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT       DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI审查结果记录表';

-- ---------------------------------------------------------------
-- 初始化角色
-- ---------------------------------------------------------------
INSERT INTO sys_role (id, role_code, role_name, description, status, deleted)
VALUES (1, 'ADMIN', '管理员', '系统管理员', 1, 0),
       (2, 'USER', '普通用户', '普通用户', 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

-- ---------------------------------------------------------------
-- 初始化管理员账号（不再内置默认凭据）
-- 管理员账号改由环境变量/启动脚本注入：
--   ADMIN_USERNAME / ADMIN_PASSWORD（明文，启动时由 AdminInitializer 使用 BCrypt 加密）
-- 或通过注册接口 /api/v1/auth/register 创建后手动分配 ADMIN 角色。
-- 禁止在本脚本提交任何真实密码或密文。
-- ---------------------------------------------------------------
