package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.dto.ai.ReviewResultCreateRequest;
import com.example.codemindaibackend.dto.ai.ReviewResultQueryRequest;
import com.example.codemindaibackend.service.AiReviewResultService;
import com.example.codemindaibackend.vo.ai.ReviewResultVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 审查结果接口
 *
 * <p>说明：保存接口供内部 AI 服务回调，生产环境建议独立鉴权。</p>
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/v1/ai-reviews")
public class AiReviewResultController {

    private final AiReviewResultService aiReviewResultService;

    public AiReviewResultController(AiReviewResultService aiReviewResultService) {
        this.aiReviewResultService = aiReviewResultService;
    }

    /**
     * 保存审查结果（内部回调，HMAC 签名鉴权）
     */
    @PostMapping
    public Result<ReviewResultVO> create(@Valid @RequestBody ReviewResultCreateRequest request) {
        return Result.success(aiReviewResultService.createResult(request));
    }

    /**
     * 分页查询结果
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ReviewResultVO>> page(@Valid ReviewResultQueryRequest request) {
        return Result.success(aiReviewResultService.pageResults(request));
    }

    /**
     * 结果详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<ReviewResultVO> detail(@PathVariable Long id) {
        return Result.success(aiReviewResultService.getResult(id));
    }
}
