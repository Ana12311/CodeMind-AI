package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.dto.ai.AiTaskCallbackRequest;
import com.example.codemindaibackend.service.AiTaskService;
import com.example.codemindaibackend.vo.ai.TaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 结果回调接口（内部调用，HMAC 签名鉴权）
 *
 * <p>FastAPI 完成 AI 任务后回调，携带最终状态与结果，
 * 本服务据此更新 ai_task 状态并保存 ai_result。</p>
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/ai/task")
public class AiCallbackController {

    private final AiTaskService aiTaskService;

    public AiCallbackController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    /**
     * AI 结果回调
     */
    @PostMapping("/callback")
    public Result<TaskVO> callback(@Valid @RequestBody AiTaskCallbackRequest request) {
        return Result.success(aiTaskService.handleCallback(request));
    }
}
