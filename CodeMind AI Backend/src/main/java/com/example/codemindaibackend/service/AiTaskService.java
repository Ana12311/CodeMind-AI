package com.example.codemindaibackend.service;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.ai.AiTaskCallbackRequest;
import com.example.codemindaibackend.dto.ai.TaskCreateRequest;
import com.example.codemindaibackend.dto.ai.TaskQueryRequest;
import com.example.codemindaibackend.dto.ai.TaskResultUpdateRequest;
import com.example.codemindaibackend.dto.ai.TaskStatusUpdateRequest;
import com.example.codemindaibackend.vo.ai.TaskVO;

import java.util.List;

/**
 * AI 任务业务接口
 *
 * @author CodeMind
 */
public interface AiTaskService {

    /**
     * 分页查询任务
     */
    PageResult<TaskVO> pageTasks(TaskQueryRequest request);

    /**
     * 任务详情
     */
    TaskVO getTask(Long id);

    /**
     * 创建任务
     */
    TaskVO createTask(TaskCreateRequest request);

    /**
     * 更新任务状态
     */
    TaskVO updateStatus(Long id, TaskStatusUpdateRequest request);

    /**
     * 保存任务结果
     */
    TaskVO saveResult(Long id, TaskResultUpdateRequest request);

    /**
     * 处理 AI 结果回调（FastAPI → 本服务）：更新任务状态并保存 AI 结果
     */
    TaskVO handleCallback(AiTaskCallbackRequest request);

    /**
     * 拉取待处理任务（内部 AI 服务调用）
     */
    List<TaskVO> listPendingTasks(Integer limit);
}
