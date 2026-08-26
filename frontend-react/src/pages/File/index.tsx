import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Descriptions,
  Modal,
  Popconfirm,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd'
import { InboxOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { listFiles, uploadFile, getFile, getFileContent, deleteFile } from '@/api/file'
import CodeViewer from '@/components/CodeViewer'
import { listProjects } from '@/api/project'
import { handleRequestError, formatFileSize, formatTime } from '@/utils'
import type { FileItem } from '@/types/file'
import type { Project } from '@/types/project'

const ALLOWED_EXT = [
  'java', 'py', 'js', 'ts', 'jsx', 'tsx', 'go', 'c', 'cpp', 'h',
  'cs', 'rb', 'php', 'kt', 'swift', 'rs', 'sql', 'sh', 'vue', 'html', 'css',
]
const MAX_SIZE = 10 * 1024 * 1024 // 10MB 前端软限制，后端仍做权威校验

function FilePage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [uploadProjectId, setUploadProjectId] = useState<string>()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [percent, setPercent] = useState(0)

  const [data, setData] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(false)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [filterProjectId, setFilterProjectId] = useState<string>()

  const [detail, setDetail] = useState<FileItem | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailContent, setDetailContent] = useState('')

  const fetchList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await listFiles({ pageNum, pageSize, projectId: filterProjectId })
      setData(res.records)
      setTotal(Number(res.total) || 0)
    } catch (error) {
      handleRequestError(error)
    } finally {
      setLoading(false)
    }
  }, [pageNum, pageSize, filterProjectId])

  useEffect(() => {
    fetchList()
  }, [fetchList])

  useEffect(() => {
    listProjects({ pageNum: 1, pageSize: 100 })
      .then((res) => setProjects(res.records))
      .catch(() => {
        /* 项目下拉加载失败不阻断 */
      })
  }, [])

  const validateFile = (file: File): boolean => {
    const ext = file.name.split('.').pop()?.toLowerCase() ?? ''
    if (!ALLOWED_EXT.includes(ext)) {
      message.error(`不支持的文件类型：.${ext}`)
      return false
    }
    if (file.size > MAX_SIZE) {
      message.error('文件大小超过 10MB 限制')
      return false
    }
    return true
  }

  const handleUpload = async () => {
    if (!selectedFile || !uploadProjectId) return
    setUploading(true)
    setPercent(0)
    try {
      await uploadFile(selectedFile, uploadProjectId, setPercent)
      message.success('上传成功')
      setSelectedFile(null)
      setPercent(0)
      fetchList()
    } catch (error) {
      handleRequestError(error)
    } finally {
      setUploading(false)
    }
  }

  const handleView = async (id: string) => {
    setDetailLoading(true)
    setDetailContent('')
    try {
      const f = await getFile(id)
      setDetail(f)
      setDetailOpen(true)
      const content = await getFileContent(id).catch(() => '')
      setDetailContent(content ?? '')
    } catch (error) {
      handleRequestError(error)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteFile(id)
      message.success('删除成功')
      fetchList()
    } catch (error) {
      handleRequestError(error)
    }
  }

  const columns: ColumnsType<FileItem> = [
    { title: '文件名', dataIndex: 'fileName', key: 'fileName', ellipsis: true },
    {
      title: '类型',
      dataIndex: 'fileType',
      key: 'fileType',
      width: 90,
      render: (v: string) => (v ? <Tag>{v}</Tag> : '-'),
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 110,
      render: (v) => formatFileSize(v),
    },
    { title: '上传时间', dataIndex: 'createTime', key: 'createTime', width: 180, render: (v) => formatTime(v) || '-' },
    { title: 'checksum', dataIndex: 'checksum', key: 'checksum', ellipsis: true },
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
            title="确认删除该文件？"
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
      <Typography.Title level={4}>代码文件管理</Typography.Title>

      <Card title="上传代码文件" style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Select
            style={{ width: 320 }}
            placeholder="选择目标项目"
            value={uploadProjectId}
            onChange={(v) => setUploadProjectId(v)}
            options={projects.map((p) => ({ label: p.name, value: p.id }))}
          />
          <Upload.Dragger
            accept={ALLOWED_EXT.map((e) => `.${e}`).join(',')}
            multiple={false}
            showUploadList={false}
            beforeUpload={(file) => {
              if (!uploadProjectId) {
                message.warning('请先选择目标项目')
                return Upload.LIST_IGNORE
              }
              if (!validateFile(file)) return Upload.LIST_IGNORE
              setSelectedFile(file)
              return false // 手动提交
            }}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽文件到此区域</p>
            <p className="ant-upload-hint">
              {selectedFile
                ? `已选择：${selectedFile.name}`
                : '支持 .java/.py/.js/.ts 等代码文件，单个不超过 10MB'}
            </p>
          </Upload.Dragger>
          {uploading && <Progress percent={percent} status="active" />}
          <Button
            type="primary"
            onClick={handleUpload}
            loading={uploading}
            disabled={!selectedFile || !uploadProjectId}
          >
            上传
          </Button>
        </Space>
      </Card>

      <Card
        title="文件列表"
        extra={
          <Space>
            <Select
              allowClear
              style={{ width: 200 }}
              placeholder="按项目筛选"
              value={filterProjectId}
              onChange={(v) => {
                setFilterProjectId(v)
                setPageNum(1)
              }}
              options={projects.map((p) => ({ label: p.name, value: p.id }))}
            />
            <Button icon={<ReloadOutlined />} onClick={fetchList}>
              刷新
            </Button>
          </Space>
        }
      >
        <Table<FileItem>
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
        title="文件详情"
        open={detailOpen}
        footer={null}
        loading={detailLoading}
        onCancel={() => setDetailOpen(false)}
      >
        {detail && (
          <>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="文件名">{detail.fileName}</Descriptions.Item>
              <Descriptions.Item label="类型">{detail.fileType || '-'}</Descriptions.Item>
              <Descriptions.Item label="大小">{formatFileSize(detail.fileSize)}</Descriptions.Item>
              <Descriptions.Item label="存储地址">{detail.storageUrl || '-'}</Descriptions.Item>
              <Descriptions.Item label="checksum">{detail.checksum || '-'}</Descriptions.Item>
              <Descriptions.Item label="上传时间">{formatTime(detail.createTime) || '-'}</Descriptions.Item>
            </Descriptions>
            <div style={{ height: 320, marginTop: 12 }}>
              <CodeViewer code={detailContent} fileName={detail.fileName} height="100%" />
            </div>
          </>
        )}
      </Modal>
    </div>
  )
}

export default FilePage
