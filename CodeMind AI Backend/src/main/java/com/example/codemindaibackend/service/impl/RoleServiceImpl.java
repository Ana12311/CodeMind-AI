package com.example.codemindaibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.codemindaibackend.entity.SysRole;
import com.example.codemindaibackend.mapper.SysRoleMapper;
import com.example.codemindaibackend.service.RoleService;
import com.example.codemindaibackend.vo.role.RoleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色业务实现
 *
 * @author CodeMind
 */
@Service
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    @Override
    public List<RoleVO> listRoles() {
        return list().stream()
                .map(role -> {
                    RoleVO vo = new RoleVO();
                    BeanUtils.copyProperties(role, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
