import { Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Task } from '@/types/task'

const statusColor: Record<number, string> = {
  0: 'default',
  1: 'processing',
  2: 'success',
  3: 'error',
}

const columns: ColumnsType<Task> = [
  { title: '任务 ID', dataIndex: 'id', key: 'id', ellipsis: true, width: 220 },
  { title: '类型', dataIndex: 'taskType', key: 'taskType', width: 140 },
  {
    title: '状态',
    dataIndex: 'statusDesc',
    key: 'statusDesc',
    width: 100,
    render: (_, record) => (
      <Tag color={statusColor[record.status ?? 0]}>{record.statusDesc ?? '-'}</Tag>
    ),
  },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 200 },
]

interface RecentTaskTableProps {
  data: Task[]
  loading: boolean
}

function RecentTaskTable({ data, loading }: RecentTaskTableProps) {
  return (
    <Table<Task>
      rowKey="id"
      columns={columns}
      dataSource={data}
      loading={loading}
      pagination={false}
      size="small"
    />
  )
}

export default RecentTaskTable
