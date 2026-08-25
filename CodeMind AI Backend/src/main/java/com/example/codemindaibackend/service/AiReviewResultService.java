package com.example.codemindaibackend.service;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.ai.ReviewResultCreateRequest;
import com.example.codemindaibackend.dto.ai.ReviewResultQueryRequest;
import com.example.codemindaibackend.vo.ai.ReviewResultVO;

/**
 * AI 审查结果业务接口
 *
 * @author CodeMind
 */
public interface AiReviewResultService {

    /**
     * 分页查询结果
     */
    PageResult<ReviewResultVO> pageResults(ReviewResultQueryRequest request);

    /**
     * 结果详情
     */
    ReviewResultVO getResult(Long id);

    /**
     * 保存审查结果
     */
    ReviewResultVO createResult(ReviewResultCreateRequest request);
}
