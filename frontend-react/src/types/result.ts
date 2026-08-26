// AI 审查结果 detail JSON 结构（对应 FastAPI WorkflowResult 序列化后写入 ReviewResultVO.detail）

/** 单个代码问题项（对应 FastAPI Issue 模型） */
export interface Issue {
  /** 问题所在文件 */
  file?: string
  /** 行号（字符串，可能为 "45" / "45-50" / ""） */
  line?: string
  /** 风险等级：P0 | P1 | P2 */
  level?: string
  /** 问题描述 */
  problem?: string
  /** 修复建议 */
  suggestion?: string
}

export interface Step {
  id?: number
  description?: string
}

export interface Plan {
  steps?: Step[]
}

export interface StepResult {
  step?: Step
  output?: string
}

export interface ReviewSummary {
  /** 是否通过审核 */
  approved?: boolean
  /** 评审总结 */
  summary?: string
  /** 问题列表 */
  issues?: Issue[]
}

/** detail JSON 解析后的完整结构（WorkflowResult + projectId） */
export interface ReviewDetail {
  task_id?: number | string
  status?: string
  plan?: Plan
  step_results?: StepResult[]
  review?: ReviewSummary
  projectId?: string | number
}

/** 解析后端 detail JSON 字符串（兼容字符串二次包裹） */
export function parseReviewDetail(detail?: string | null): ReviewDetail | null {
  if (!detail) return null
  try {
    const obj = JSON.parse(detail)
    if (typeof obj === 'string') return parseReviewDetail(obj)
    return obj as ReviewDetail
  } catch {
    return null
  }
}

/** 从 issue.line（字符串）提取首个行号数字，非法返回 null */
export function parseLineNumber(line?: string): number | null {
  if (!line) return null
  const m = line.match(/\d+/)
  return m ? Number(m[0]) : null
}
