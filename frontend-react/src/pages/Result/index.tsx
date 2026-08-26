import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Col,
  Collapse,
  Descriptions,
  Empty,
  Result as AntResult,
  Row,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd'
import { AimOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import CodeViewer from '@/components/CodeViewer'
import { getReviewResult, listReviewResults } from '@/api/result'
import { getTask } from '@/api/task'
import { listProjects } from '@/api/project'
import { listFiles, getFileContent } from '@/api/file'
import { handleRequestError, formatTime } from '@/utils'
import { parseReviewDetail, parseLineNumber } from '@/types/result'
import type { ReviewDetail } from '@/types/result'
import type { ReviewResult } from '@/types/review'
import type { Task } from '@/types/task'

const LEVEL_META: Record<string, { color: string; label: string }> = {
  P0: { color: 'red', label: 'P0' },
  P1: { color: 'orange', label: 'P1' },
  P2: { color: 'gold', label: 'P2' },
}

const TASK_STATUS_META: Record<number, { color: string; label: string }> = {
  0: { color: 'default', label: '等待处理' },
  1: { color: 'processing', label: '处理中' },
  2: { color: 'success', label: '成功' },
  3: { color: 'error', label: '失败' },
}

function levelMeta(level?: string): { color: string; label: string } {
  return LEVEL_META[level ?? ''] ?? { color: 'default', label: level || '未知' }
}

function ResultPage() {
  const [searchParams] = useSearchParams()
  const taskId = searchParams.get('taskId')
  const resultId = searchParams.get('id')

  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [task, setTask] = useState<Task | null>(null)
  const [result, setResult] = useState<ReviewResult | null>(null)
  const [detail, setDetail] = useState<ReviewDetail | null>(null)
  const [activeLine, setActiveLine] = useState<number>()
  const [projectMap, setProjectMap] = useState<Record<string, string>>({})
  const [code, setCode] = useState('')
  const [codeFileName, setCodeFileName] = useState('')

  useEffect(() => {
    listProjects({ pageNum: 1, pageSize: 100 })
      .then((res) => {
        const map: Record<string, string> = {}
        res.records.forEach((p) => {
          map[p.id] = p.name
        })
        setProjectMap(map)
      })
      .catch(() => {
        /* 项目名加载失败不阻断 */
      })
  }, [])

  useEffect(() => {
    if (!taskId && !resultId) return
    let cancelled = false
    setLoading(true)
    setLoadError(false)
    ;(async () => {
      try {
        let r: ReviewResult | null = null
        if (resultId) {
          r = await getReviewResult(resultId)
        } else if (taskId) {
          const page = await listReviewResults({ taskId, pageNum: 1, pageSize: 1 })
          r = page.records[0] ?? null
        }

        // 任务信息：优先用 URL taskId，否则用结果里的 taskId
        let t: Task | null = null
        const effectiveTaskId = taskId ?? r?.taskId
        if (effectiveTaskId) {
          t = await getTask(effectiveTaskId)
        }

        if (cancelled) return
        setResult(r)
        setDetail(parseReviewDetail(r?.detail ?? null))
        setTask(t)
      } catch (error) {
        if (cancelled) return
        handleRequestError(error)
        setLoadError(true)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [taskId, resultId])

  const issues = detail?.review?.issues ?? []
  const summary = detail?.review?.summary
  const approved = detail?.review?.approved
  const projectId = result?.projectId ?? task?.projectId
  const projectName = projectId ? (projectMap[projectId] ?? projectId) : '-'
  const targetFileName = issues[0]?.file

  // 加载与首个问题匹配的代码文件原文，供 Monaco 展示
  useEffect(() => {
    if (!projectId) return
    let cancelled = false
    ;(async () => {
      try {
        const files = await listFiles({ projectId, pageNum: 1, pageSize: 100 })
        const match =
          files.records.find((f) => f.fileName === targetFileName) ?? files.records[0]
        if (!match || cancelled) return
        const content = await getFileContent(match.id)
        if (!cancelled) {
          setCode(content ?? '')
          setCodeFileName(match.fileName ?? targetFileName ?? '')
        }
      } catch {
        /* 内容加载失败不阻断结果展示 */
      }
    })()
    return () => {
      cancelled = true
    }
  }, [projectId, targetFileName])

  if (loading) {
    return (
      <Card>
        <Spin tip="加载结果中..." style={{ display: 'block', margin: '64px auto' }}>
          <div style={{ height: 120 }} />
        </Spin>
      </Card>
    )
  }

  if (!taskId && !resultId) {
    return (
      <Card>
        <AntResult status="warning" title="缺少参数" subTitle="请从 AI 任务列表点击「查看结果」进入本页" />
      </Card>
    )
  }

  if (loadError) {
    return (
      <Card>
        <AntResult status="error" title="加载失败" subTitle="接口错误或结果不存在，请返回任务列表重试" />
      </Card>
    )
  }

  if (task && task.status === 3) {
    return (
      <Card>
        <AntResult status="error" title="AI 任务失败" subTitle={task.errorMsg || '任务执行失败，请查看任务详情'} />
      </Card>
    )
  }

  if (!result) {
    return (
      <Card>
        <AntResult status="info" title="暂无结果" subTitle="任务可能仍在处理中，或尚未生成审查结果" />
      </Card>
    )
  }

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => window.history.back()}>
          返回
        </Button>
        <Typography.Title level={4} style={{ margin: 0 }}>
          AI 审查结果
        </Typography.Title>
      </Space>

      {/* 顶部：任务信息 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Descriptions column={4} size="small">
          <Descriptions.Item label="任务 ID">{task?.id ?? taskId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="项目">{projectName}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {task ? (
              <Tag color={TASK_STATUS_META[task.status ?? 0]?.color}>
                {TASK_STATUS_META[task.status ?? 0]?.label ?? '-'}
              </Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="执行时间">
            {formatTime(task?.endTime ?? task?.startTime ?? task?.createTime) || '-'}
          </Descriptions.Item>
        </Descriptions>
        {summary && (
          <Alert
            type={approved ? 'success' : 'warning'}
            showIcon
            message="评审结论"
            description={summary}
            style={{ marginTop: 8 }}
          />
        )}
      </Card>

      <Row gutter={16}>
        {/* 左侧：代码查看 */}
        <Col span={14}>
          <Card title="代码" size="small" styles={{ body: { height: 560, padding: 12 } }}>
            <div style={{ height: 520 }}>
              <CodeViewer
                code={code}
                fileName={codeFileName || issues[0]?.file}
                activeLine={activeLine}
                height="100%"
              />
            </div>
          </Card>
        </Col>

        {/* 右侧：AI 审查结果 */}
        <Col span={10}>
          <Card
            title={`AI 问题（${issues.length}）`}
            size="small"
            styles={{ body: { height: 560, overflow: 'auto' } }}
          >
            {issues.length === 0 ? (
              <Empty description="未发现问题" style={{ marginTop: 180 }} />
            ) : (
              <Collapse
                defaultActiveKey={issues.map((_, i) => String(i))}
                items={issues.map((issue, i) => {
                  const meta = levelMeta(issue.level)
                  const line = parseLineNumber(issue.line)
                  return {
                    key: String(i),
                    label: (
                      <Space size={8}>
                        <Tag color={meta.color}>{meta.label}</Tag>
                        <Typography.Text code>
                          {issue.file || '未知文件'}
                          {issue.line ? `:${issue.line}` : ''}
                        </Typography.Text>
                      </Space>
                    ),
                    children: (
                      <div>
                        <Typography.Paragraph style={{ marginBottom: 8 }}>
                          <Typography.Text strong>问题：</Typography.Text>
                          {issue.problem || '-'}
                        </Typography.Paragraph>
                        <Typography.Paragraph style={{ marginBottom: 8 }}>
                          <Typography.Text strong>建议：</Typography.Text>
                          {issue.suggestion || '-'}
                        </Typography.Paragraph>
                        <Button
                          size="small"
                          type="link"
                          icon={<AimOutlined />}
                          disabled={line === null}
                          onClick={() => setActiveLine(line ?? undefined)}
                        >
                          定位到代码
                        </Button>
                      </div>
                    ),
                  }
                })}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default ResultPage
