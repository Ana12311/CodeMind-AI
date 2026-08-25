package com.example.codemindaibackend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 修改用户请求（局部更新，null 字段不覆盖）
 *
 * @author CodeMind
 */
public class UserUpdateRequest {

    /** 重置密码（可选） */
    @Size(min = 6, max = 100, message = "密码长度须在6-100之间")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @Size(max = 20, message = "手机号长度不能超过20")
    private String phone;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
