import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import { PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { createTask, listTasks, getTask, deleteTask } from '@/api/task'
import { listProjects } from '@/api/project'
import { handleRequestError, formatTime } from '@/utils'
import type { CreateTaskRequest, Task } from '@/types/task'
import type { Project } from '@/types/project'

const STATUS_META: Record<number, { color: string; label: string }> = {
  0: { color: 'default', label: '等待处理' },
  1: { color: 'processing', label: '处理中' },
  2: { color: 'success', label: '成功' },
  3: { color: 'error', label: '失败' },
}

const TASK_TYPES = [{ label: 'CODE_REVIEW 代码审查', value: 'CODE_REVIEW' }]

function TaskPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [projectMap, setProjectMap] = useState<Record<string, string>>({})

  const [data, setData] = useState<Task[]>([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  const [createLoading, setCreateLoading] = useState(false)
  const [form] = Form.useForm<CreateTaskRequest>()

  const [detail, setDetail] = useState<Task | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)

  const timerRef = useRef<number | null>(null)

  const fetchList = useCallback(async (): Promise<Task[]> => {
    setLoading(true)
    try {
      const res = await listTasks({ pageNum, pageSize })
      setData(res.records)
      setTotal(Number(res.total) || 0)
      return res.records
    } catch (error) {
      handleRequestError(error)
      return []
    } finally {
      setLoading(false)
    }
  }, [pageNum, pageSize])

  useEffect(() => {
    listProjects({ pageNum: 1, pageSize: 100 })
      .then((res) => {
        setProjects(res.records)
        const map: Record<string, string> = {}
        res.records.forEach((p) => {
          map[p.id] = p.name
        })
        setProjectMap(map)
      })
      .catch(() => {
        /* 项目下拉加载失败不阻断 */
      })
  }, [])

  // 状态轮询：存在 WAITING/PROCESSING 任务时每 3s 刷新一次，否则停止
  useEffect(() => {
    let cancelled = false

    const run = async () => {
      const records = await fetchList()
      if (cancelled) return
      const pending = records.some((t) => t.status === 0 || t.status === 1)
      if (pending) {
        timerRef.current = window.setTimeout(run, 3000)
      }
    }

    run()
    return () => {
      cancelled = true
      if (timerRef.current) window.clearTimeout(timerRef.current)
    }
  }, [fetchList])

  const onCreateFinish = async (values: CreateTaskRequest) => {
    setCreateLoading(true)
    try {
      await createTask(values)
      message.success('任务已创建')
      form.resetFields()
      setPageNum(1)
      fetchList()
    } catch (error) {
      handleRequestError(error)
    } finally {
      setCreateLoading(false)
    }
  }

  const handleDetail = async (id: string) => {
    setDetailLoading(true)
    try {
      const t = await getTask(id)
      setDetail(t)
      setDetailOpen(true)
    } catch (error) {
      handleRequestError(error)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteTask(id)
      message.success('删除成功')
      fetchList()
    } catch (error) {
      handleRequestError(error)
    }
  }

  const columns: ColumnsType<Task> = [
    { title: '任务 ID', dataIndex: 'id', key: 'id', ellipsis: true, width: 200 },
    {
      title: '项目名称',
      dataIndex: 'projectId',
      key: 'projectId',
      width: 180,
      ellipsis: true,
      render: (v: string) => projectMap[v] || v || '-',
    },
    { title: '任务类型', dataIndex: 'taskType', key: 'taskType', width: 130 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (v: number, record) => {
        const meta = STATUS_META[v] ?? { color: 'default', label: '-' }
        const tag = <Tag color={meta.color}>{meta.label}</Tag>
        return v === 3 && record.errorMsg ? (
          <Tooltip title={record.errorMsg}>{tag}</Tooltip>
        ) : (
          tag
        )
      },
    },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180, render: (v) => formatTime(v) || '-' },
    { title: '完成时间', dataIndex: 'endTime', key: 'endTime', width: 180, render: (v) => formatTime(v) || '-' },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleDetail(record.id)}>
            详情
          </Button>
          {record.status === 2 && record.resultId ? (
            <Button
              type="link"
              size="small"
              onClick={() => (window.location.href = `/result?taskId=${record.id}`)}
            >
              查看结果
            </Button>
          ) : null}
          <Popconfirm
            title="确认删除该任务？"
            description={record.status === 1 ? '任务正在处理中，删除将中断执行' : undefined}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={4}>AI 任务管理</Typography.Title>

      <Card title="创建 AI 任务" style={{ marginBottom: 16 }}>
        <Form<CreateTaskRequest>
          form={form}
          layout="inline"
          onFinish={onCreateFinish}
          style={{ rowGap: 12 }}
        >
          <Form.Item
            name="projectId"
            label="项目"
            rules={[{ required: true, message: '请选择项目' }]}
          >
            <Select
              style={{ width: 220 }}
              placeholder="选择项目"
              options={projects.map((p) => ({ label: p.name, value: p.id }))}
            />
          </Form.Item>
          <Form.Item
            name="taskType"
            label="任务类型"
            initialValue="CODE_REVIEW"
            rules={[{ required: true, message: '请选择任务类型' }]}
          >
            <Select style={{ width: 220 }} options={TASK_TYPES} />
          </Form.Item>
          <Form.Item name="content" label="任务内容">
            <Input style={{ width: 260 }} placeholder="审查要求（可选）" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<PlayCircleOutlined />} loading={createLoading}>
              开始 AI 分析
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="任务列表"
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchList}>
            刷新
          </Button>
        }
      >
        <Table<Task>
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
          }}
          onChange={(pag) => {
            setPageNum(pag.current ?? 1)
            setPageSize(pag.pageSize ?? 10)
          }}
        />
      </Card>

      <Modal
        title="任务详情"
        open={detailOpen}
        footer={null}
        loading={detailLoading}
        onCancel={() => setDetailOpen(false)}
      >
        {detail && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="任务 ID">{detail.id}</Descriptions.Item>
            <Descriptions.Item label="项目">
              {projectMap[detail.projectId ?? ''] || detail.projectId || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="任务类型">{detail.taskType || '-'}</Descriptions.Item>
            <Descriptions.Item label="状态">
              {STATUS_META[detail.status ?? 0]?.label ?? '-'}
            </Descriptions.Item>
            <Descriptions.Item label="请求参数">{detail.params ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="失败原因">{detail.errorMsg || '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatTime(detail.createTime) || '-'}</Descriptions.Item>
            <Descriptions.Item label="开始时间">{formatTime(detail.startTime) || '-'}</Descriptions.Item>
            <Descriptions.Item label="完成时间">{formatTime(detail.endTime) || '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default TaskPage
