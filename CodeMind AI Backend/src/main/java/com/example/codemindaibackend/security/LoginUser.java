package com.example.codemindaibackend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 登录用户主体，实现 {@link UserDetails}
 *
 * @author CodeMind
 */
public class LoginUser implements UserDetails {

    private final Long userId;

    private final String username;

    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    private final boolean enabled;

    public LoginUser(Long userId, String username, String password,
                     Collection<? extends GrantedAuthority> authorities, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
