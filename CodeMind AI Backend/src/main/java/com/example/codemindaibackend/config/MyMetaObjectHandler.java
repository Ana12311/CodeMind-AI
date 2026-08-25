package com.example.codemindaibackend.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.codemindaibackend.security.LoginUser;
import com.example.codemindaibackend.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充
 *
 * @author CodeMind
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, loginUser.getUserId());
            this.strictInsertFill(metaObject, "updateBy", Long.class, loginUser.getUserId());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, loginUser.getUserId());
        }
    }
}
