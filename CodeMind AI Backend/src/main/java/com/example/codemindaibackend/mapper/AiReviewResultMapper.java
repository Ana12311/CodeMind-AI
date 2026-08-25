package com.example.codemindaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.codemindaibackend.entity.AiReviewResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 审查结果记录 Mapper
 *
 * @author CodeMind
 */
@Mapper
public interface AiReviewResultMapper extends BaseMapper<AiReviewResult> {
}
