import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { listProjects, createProject, getProject, deleteProject } from '@/api/project'
import { listFiles } from '@/api/file'
import { listTasks } from '@/api/task'
import { handleRequestError, formatTime } from '@/utils'
import type { Project, CreateProjectRequest } from '@/types/project'

interface ProjectDetail {
  project: Project
  fileCount: number
  taskCount: number
}

function ProjectPage() {
  const [data, setData] = useState<Project[]>([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  const [createOpen, setCreateOpen] = useState(false)
  const [createLoading, setCreateLoading] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detail, setDetail] = useState<ProjectDetail | null>(null)

  const [form] = Form.useForm<CreateProjectRequest>()

  const fetchList = useCallback(async (p: number, s: number) => {
    setLoading(true)
    try {
      const res = await listProjects({ pageNum: p, pageSize: s })
      setData(res.records)
      setTotal(Number(res.total) || 0)
    } catch (error) {
      handleRequestError(error)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchList(pageNum, pageSize)
  }, [fetchList, pageNum, pageSize])

  const onCreateFinish = async (values: CreateProjectRequest) => {
    setCreateLoading(true)
    try {
      await createProject(values)
      message.success('创建成功')
      setCreateOpen(false)
      form.resetFields()
      setPageNum(1)
      fetchList(1, pageSize)
    } catch (error) {
      handleRequestError(error)
    } finally {
      setCreateLoading(false)
    }
  }

  const handleView = async (id: string) => {
    setDetailLoading(true)
    try {
      const [project, files, tasks] = await Promise.all([
        getProject(id),
        listFiles({ projectId: id, pageNum: 1, pageSize: 1 }),
        listTasks({ projectId: id, pageNum: 1, pageSize: 1 }),
      ])
      setDetail({
        project,
        fileCount: Number(files.total) || 0,
        taskCount: Number(tasks.total) || 0,
      })
      setDetailOpen(true)
    } catch (error) {
      handleRequestError(error)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteProject(id)
      message.success('删除成功')
      fetchList(pageNum, pageSize)
    } catch (error) {
      handleRequestError(error)
    }
  }

  const columns: ColumnsType<Project> = [
    { title: '项目名称', dataIndex: 'name', key: 'name', ellipsis: true },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true, render: (v) => v || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (v: number) =>
        v === 1 ? <Tag color="green">进行中</Tag> : <Tag>归档</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180, render: (v) => formatTime(v) || '-' },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180, render: (v) => formatTime(v) || '-' },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleView(record.id)}>
            查看
          </Button>
          <Popconfirm
            title="确认删除该项目？"
            description="删除为逻辑删除，不可恢复。"
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
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Typography.Title level={4} style={{ margin: 0 }}>
          项目管理
        </Typography.Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => fetchList(pageNum, pageSize)}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            创建项目
          </Button>
        </Space>
      </div>

      <Card>
        <Table<Project>
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
        title="创建项目"
        open={createOpen}
        onOk={() => form.submit()}
        confirmLoading={createLoading}
        onCancel={() => {
          setCreateOpen(false)
          form.resetFields()
        }}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" onFinish={onCreateFinish}>
          <Form.Item
            name="name"
            label="项目名称"
            rules={[
              { required: true, message: '请输入项目名称' },
              { max: 100, message: '项目名称长度不能超过 100' },
            ]}
          >
            <Input placeholder="项目名称" />
          </Form.Item>
          <Form.Item name="description" label="描述" rules={[{ max: 500 }]}>
            <Input.TextArea rows={3} placeholder="项目描述（可选）" />
          </Form.Item>
          <Form.Item name="language" label="主语言" rules={[{ max: 50 }]}>
            <Input placeholder="如 Java / Python（可选）" />
          </Form.Item>
          <Form.Item name="repoUrl" label="仓库地址" rules={[{ max: 255 }]}>
            <Input placeholder="仓库地址（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="项目详情"
        open={detailOpen}
        footer={null}
        loading={detailLoading}
        onCancel={() => setDetailOpen(false)}
      >
        {detail && (
          <>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={12}>
                <Statistic title="代码文件数量" value={detail.fileCount} />
              </Col>
              <Col span={12}>
                <Statistic title="AI 任务数量" value={detail.taskCount} />
              </Col>
            </Row>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="项目名称">{detail.project.name}</Descriptions.Item>
              <Descriptions.Item label="描述">
                {detail.project.description || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="主语言">
                {detail.project.language || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="仓库地址">
                {detail.project.repoUrl || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                {detail.project.status === 1 ? '进行中' : '归档'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {formatTime(detail.project.createTime) || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="更新时间">
                {formatTime(detail.project.updateTime) || '-'}
              </Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Modal>
    </div>
  )
}

export default ProjectPage
