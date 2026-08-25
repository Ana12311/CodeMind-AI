package com.example.codemindaibackend.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FastAPI AI 服务客户端。
 * 通过 WebClient 提交 AI 任务，仅获取 PROCESSING 确认，不等待最终结果
 * （最终结果由 FastAPI 通过回调接口推送）。调用失败以异常抛出，由上层处理。
 *
 * @author CodeMind
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    /** 单次提交超时，避免 AI 服务无响应时阻塞业务线程 */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public AiServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 提交任务到 AI 服务。
     *
     * @param taskId    已落库的 AI 任务 ID
     * @param taskType  任务类型
     * @param projectId 所属项目 ID
     * @param content   任务内容
     * @return FastAPI 返回的任务状态字符串
     */
    public String submitTask(Long taskId, String taskType, Long projectId, String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("taskType", taskType == null ? "" : taskType);
        body.put("projectId", projectId);
        body.put("content", content == null ? "" : content);

        Map<?, ?> response = webClient.post()
                .uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .block();

        String status = response == null ? null : String.valueOf(response.get("status"));
        log.info("AI 服务提交成功: taskId={}, status={}", taskId, status);
        return status;
    }
}
