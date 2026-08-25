package com.example.codemindaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.ai.ReviewResultCreateRequest;
import com.example.codemindaibackend.dto.ai.ReviewResultQueryRequest;
import com.example.codemindaibackend.entity.AiReviewResult;
import com.example.codemindaibackend.entity.AiTask;
import com.example.codemindaibackend.mapper.AiReviewResultMapper;
import com.example.codemindaibackend.mapper.AiTaskMapper;
import com.example.codemindaibackend.security.SecurityUtils;
import com.example.codemindaibackend.service.AiReviewResultService;
import com.example.codemindaibackend.service.ProjectService;
import com.example.codemindaibackend.vo.ai.ReviewResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 审查结果业务实现
 *
 * @author CodeMind
 */
@Service
public class AiReviewResultServiceImpl extends ServiceImpl<AiReviewResultMapper, AiReviewResult>
        implements AiReviewResultService {

    private final ProjectService projectService;

    private final AiTaskMapper aiTaskMapper;

    public AiReviewResultServiceImpl(ProjectService projectService, AiTaskMapper aiTaskMapper) {
        this.projectService = projectService;
        this.aiTaskMapper = aiTaskMapper;
    }

    @Override
    public PageResult<ReviewResultVO> pageResults(ReviewResultQueryRequest request) {
        Page<AiReviewResult> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<AiReviewResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getProjectId() != null, AiReviewResult::getProjectId, request.getProjectId())
                .eq(request.getTaskId() != null, AiReviewResult::getTaskId, request.getTaskId())
                .eq(StringUtils.hasText(request.getReviewType()), AiReviewResult::getReviewType, request.getReviewType())
                .eq(StringUtils.hasText(request.getSeverity()), AiReviewResult::getSeverity, request.getSeverity())
                .orderByDesc(AiReviewResult::getCreateTime);

        // 数据隔离：普通用户仅见本人项目下的审查结果
        if (!SecurityUtils.isAdmin()) {
            List<Long> ownedIds = projectService.listOwnedProjectIds();
            if (ownedIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            wrapper.in(AiReviewResult::getProjectId, ownedIds);
        }

        IPage<AiReviewResult> result = page(page, wrapper);
        List<ReviewResultVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public ReviewResultVO getResult(Long id) {
        AiReviewResult result = getById(id);
        if (result == null) {
            throw BusinessException.notFound("审查结果不存在");
        }
        projectService.checkProjectAccess(result.getProjectId());
        return toVO(result);
    }

    @Override
    public ReviewResultVO createResult(ReviewResultCreateRequest request) {
        if (request.getTaskId() == null) {
            throw new BusinessException("任务ID不能为空");
        }
        AiTask task = aiTaskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }

        AiReviewResult result = new AiReviewResult();
        BeanUtils.copyProperties(request, result);
        // 项目归属以任务为准，防伪造
        result.setProjectId(task.getProjectId());
        if (result.getStatus() == null) {
            result.setStatus(1);
        }
        save(result);
        return toVO(result);
    }

    /**
     * 实体转 VO
     */
    private ReviewResultVO toVO(AiReviewResult result) {
        ReviewResultVO vo = new ReviewResultVO();
        BeanUtils.copyProperties(result, vo);
        return vo;
    }
}
