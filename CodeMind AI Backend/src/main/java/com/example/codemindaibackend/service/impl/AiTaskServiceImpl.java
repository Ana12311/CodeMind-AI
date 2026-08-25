package com.example.codemindaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.codemindaibackend.client.AiServiceClient;
import com.example.codemindaibackend.common.enums.TaskStatus;
import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.ai.AiTaskCallbackRequest;
import com.example.codemindaibackend.dto.ai.TaskCreateRequest;
import com.example.codemindaibackend.dto.ai.TaskQueryRequest;
import com.example.codemindaibackend.dto.ai.TaskResultUpdateRequest;
import com.example.codemindaibackend.dto.ai.TaskStatusUpdateRequest;
import com.example.codemindaibackend.entity.AiReviewResult;
import com.example.codemindaibackend.entity.AiTask;
import com.example.codemindaibackend.mapper.AiReviewResultMapper;
import com.example.codemindaibackend.mapper.AiTaskMapper;
import com.example.codemindaibackend.security.SecurityUtils;
import com.example.codemindaibackend.service.AiTaskService;
import com.example.codemindaibackend.service.ProjectService;
import com.example.codemindaibackend.vo.ai.TaskVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 任务业务实现
 *
 * @author CodeMind
 */
@Service
public class AiTaskServiceImpl extends ServiceImpl<AiTaskMapper, AiTask> implements AiTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiTaskServiceImpl.class);

    private final ProjectService projectService;

    private final AiReviewResultMapper reviewResultMapper;

    private final AiServiceClient aiServiceClient;

    public AiTaskServiceImpl(ProjectService projectService,
                             AiReviewResultMapper reviewResultMapper,
                             AiServiceClient aiServiceClient) {
        this.projectService = projectService;
        this.reviewResultMapper = reviewResultMapper;
        this.aiServiceClient = aiServiceClient;
    }

    /**
     * 状态机：合法流转 WAITING → PROCESSING → SUCCESS / FAILED
     */
    private static final Map<Integer, Set<Integer>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        ALLOWED_TRANSITIONS.put(TaskStatus.WAITING.getCode(),
                new HashSet<>(Arrays.asList(TaskStatus.PROCESSING.getCode(), TaskStatus.FAILED.getCode())));
        ALLOWED_TRANSITIONS.put(TaskStatus.PROCESSING.getCode(),
                new HashSet<>(Arrays.asList(TaskStatus.SUCCESS.getCode(), TaskStatus.FAILED.getCode())));
    }

    @Override
    public PageResult<TaskVO> pageTasks(TaskQueryRequest request) {
        Page<AiTask> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<AiTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getProjectId() != null, AiTask::getProjectId, request.getProjectId())
                .eq(StringUtils.hasText(request.getTaskType()), AiTask::getTaskType, request.getTaskType())
                .eq(request.getStatus() != null, AiTask::getStatus, request.getStatus())
                .orderByDesc(AiTask::getCreateTime);

        // 数据隔离：普通用户仅见本人项目下的任务
        if (!SecurityUtils.isAdmin()) {
            List<Long> ownedIds = projectService.listOwnedProjectIds();
            if (ownedIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            wrapper.in(AiTask::getProjectId, ownedIds);
        }

        IPage<AiTask> result = page(page, wrapper);
        List<TaskVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public TaskVO getTask(Long id) {
        AiTask task = getById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        projectService.checkProjectAccess(task.getProjectId());
        return toVO(task);
    }

    @Override
    public TaskVO createTask(TaskCreateRequest request) {
        // 校验项目存在且有权限
        projectService.checkProjectAccess(request.getProjectId());

        AiTask task = new AiTask();
        task.setProjectId(request.getProjectId());
        task.setTaskType(request.getTaskType());
        // content 优先，兼容旧字段 params；落库复用 params 列（不改库）
        String content = StringUtils.hasText(request.getContent()) ? request.getContent() : request.getParams();
        task.setParams(content);
        task.setStatus(TaskStatus.WAITING.getCode());
        task.setSubmitBy(SecurityUtils.getCurrentUserId());
        save(task);

        // 调用 FastAPI：仅获取 PROCESSING 确认，不等待最终结果（最终结果走回调）
        try {
            aiServiceClient.submitTask(task.getId(), task.getTaskType(), task.getProjectId(), content);
            task.setStatus(TaskStatus.PROCESSING.getCode());
            task.setStartTime(LocalDateTime.now());
            updateById(task);
        } catch (Exception e) {
            // AI 服务不可用不影响业务，任务置为失败并记录日志
            log.error("AI 服务提交失败: taskId={}", task.getId(), e);
            task.setStatus(TaskStatus.FAILED.getCode());
            task.setErrorMsg("AI 服务调用失败：" + e.getMessage());
            task.setEndTime(LocalDateTime.now());
            updateById(task);
        }
        return toVO(task);
    }

    @Override
    public TaskVO updateStatus(Long id, TaskStatusUpdateRequest request) {
        AiTask task = getById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        if (!TaskStatus.isValid(request.getStatus())) {
            throw new BusinessException("无效的任务状态");
        }
        // 状态机校验：仅允许合法流转
        Integer current = task.getStatus();
        Integer target = request.getStatus();
        if (!target.equals(current)) {
            Set<Integer> allowed = ALLOWED_TRANSITIONS.get(current);
            if (allowed == null || !allowed.contains(target)) {
                throw new BusinessException("非法的任务状态流转");
            }
        }

        task.setStatus(request.getStatus());
        LocalDateTime now = LocalDateTime.now();
        if (TaskStatus.PROCESSING.getCode().equals(request.getStatus()) && task.getStartTime() == null) {
            task.setStartTime(now);
        }
        if (TaskStatus.SUCCESS.getCode().equals(request.getStatus())
                || TaskStatus.FAILED.getCode().equals(request.getStatus())) {
            task.setEndTime(now);
        }
        if (StringUtils.hasText(request.getErrorMsg())) {
            task.setErrorMsg(request.getErrorMsg());
        }

        updateById(task);
        return toVO(task);
    }

    @Override
    public TaskVO saveResult(Long id, TaskResultUpdateRequest request) {
        AiTask task = getById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        if (!TaskStatus.SUCCESS.getCode().equals(request.getStatus())
                && !TaskStatus.FAILED.getCode().equals(request.getStatus())) {
            throw new BusinessException("结果状态只能是成功或失败");
        }
        // 仅处理中的任务可保存最终结果
        if (!TaskStatus.PROCESSING.getCode().equals(task.getStatus())) {
            throw new BusinessException("仅处理中的任务可保存结果");
        }
        // 校验关联结果记录存在
        if (request.getResultId() != null && reviewResultMapper.selectById(request.getResultId()) == null) {
            throw BusinessException.notFound("结果记录不存在");
        }

        task.setStatus(request.getStatus());
        task.setResultId(request.getResultId());
        task.setErrorMsg(request.getErrorMsg());
        task.setEndTime(LocalDateTime.now());

        updateById(task);
        return toVO(task);
    }

    @Override
    @Transactional
    public TaskVO handleCallback(AiTaskCallbackRequest request) {
        AiTask task = getById(request.getTaskId());
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        // 仅处理中的任务可接收最终结果回调
        if (!TaskStatus.PROCESSING.getCode().equals(task.getStatus())) {
            throw new BusinessException("仅处理中的任务可接收回调");
        }

        String status = request.getStatus();
        boolean success;
        if ("SUCCESS".equalsIgnoreCase(status)) {
            success = true;
        } else if ("FAILED".equalsIgnoreCase(status)) {
            success = false;
        } else {
            throw new BusinessException("无效的回调状态");
        }

        task.setStatus(success ? TaskStatus.SUCCESS.getCode() : TaskStatus.FAILED.getCode());
        task.setEndTime(LocalDateTime.now());

        if (success) {
            // 保存 AI 结果
            AiReviewResult result = new AiReviewResult();
            result.setTaskId(task.getId());
            result.setProjectId(task.getProjectId());
            result.setReviewType(task.getTaskType());
            result.setDetail(request.getResult());
            result.setStatus(1);
            reviewResultMapper.insert(result);
            task.setResultId(result.getId());
        } else {
            task.setErrorMsg(request.getResult());
        }

        updateById(task);
        return toVO(task);
    }

    @Override
    public List<TaskVO> listPendingTasks(Integer limit) {
        int size = limit == null ? 10 : Math.min(Math.max(limit, 1), 50);
        IPage<AiTask> result = page(new Page<>(1, size),
                new LambdaQueryWrapper<AiTask>()
                        .eq(AiTask::getStatus, TaskStatus.WAITING.getCode())
                        .orderByAsc(AiTask::getCreateTime));
        return result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 实体转 VO（含状态描述）
     */
    private TaskVO toVO(AiTask task) {
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        for (TaskStatus status : TaskStatus.values()) {
            if (status.getCode().equals(task.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}
