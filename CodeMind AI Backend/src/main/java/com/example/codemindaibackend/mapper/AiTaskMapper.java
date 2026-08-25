package com.example.codemindaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.codemindaibackend.entity.AiTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 任务 Mapper
 *
 * @author CodeMind
 */
@Mapper
public interface AiTaskMapper extends BaseMapper<AiTask> {
}
