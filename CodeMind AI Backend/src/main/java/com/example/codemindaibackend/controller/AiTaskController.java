package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.dto.ai.TaskCreateRequest;
import com.example.codemindaibackend.dto.ai.TaskQueryRequest;
import com.example.codemindaibackend.dto.ai.TaskResultUpdateRequest;
import com.example.codemindaibackend.dto.ai.TaskStatusUpdateRequest;
import com.example.codemindaibackend.service.AiTaskService;
import com.example.codemindaibackend.vo.ai.TaskVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 任务接口
 *
 * <p>说明：状态更新 / 结果保存接口供内部 AI 服务回调，生产环境建议改为独立鉴权
 * （内部密钥 / 网关白名单），与用户 JWT 隔离。</p>
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/v1/ai-tasks")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    /**
     * 创建 AI 任务
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<TaskVO> create(@Valid @RequestBody TaskCreateRequest request) {
        return Result.success(aiTaskService.createTask(request));
    }

    /**
     * 分页查询任务
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<TaskVO>> page(@Valid TaskQueryRequest request) {
        return Result.success(aiTaskService.pageTasks(request));
    }

    /**
     * 任务详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<TaskVO> detail(@PathVariable Long id) {
        return Result.success(aiTaskService.getTask(id));
    }

    /**
     * 拉取待处理任务（内部回调，HMAC 签名鉴权）
     */
    @GetMapping("/pending")
    public Result<List<TaskVO>> pending(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(aiTaskService.listPendingTasks(limit));
    }

    /**
     * 更新任务状态（内部回调，HMAC 签名鉴权）
     */
    @PutMapping("/{id}/status")
    public Result<TaskVO> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusUpdateRequest request) {
        return Result.success(aiTaskService.updateStatus(id, request));
    }

    /**
     * 保存任务结果（内部回调，HMAC 签名鉴权）
     */
    @PutMapping("/{id}/result")
    public Result<TaskVO> saveResult(@PathVariable Long id, @Valid @RequestBody TaskResultUpdateRequest request) {
        return Result.success(aiTaskService.saveResult(id, request));
    }
}
