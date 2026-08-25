package com.example.codemindaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.codemindaibackend.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper
 *
 * @author CodeMind
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
