import { useEffect, useState } from 'react'
import { Card, Col, Row, Typography } from 'antd'
import {
  ProjectOutlined,
  UnorderedListOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import StatCard from '@/components/Dashboard/StatCard'
import RecentTaskTable from '@/components/Dashboard/RecentTaskTable'
import { listProjects } from '@/api/project'
import { listTasks } from '@/api/task'
import { handleRequestError } from '@/utils'
import type { Task } from '@/types/task'

function Dashboard() {
  const [projectCount, setProjectCount] = useState(0)
  const [taskCount, setTaskCount] = useState(0)
  const [successCount, setSuccessCount] = useState(0)
  const [recentTasks, setRecentTasks] = useState<Task[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      try {
        const [projects, tasks, success, recent] = await Promise.all([
          listProjects({ pageNum: 1, pageSize: 1 }),
          listTasks({ pageNum: 1, pageSize: 1 }),
          listTasks({ pageNum: 1, pageSize: 1, status: 2 }),
          listTasks({ pageNum: 1, pageSize: 5 }),
        ])
        if (cancelled) return
        setProjectCount(Number(projects.total) || 0)
        setTaskCount(Number(tasks.total) || 0)
        setSuccessCount(Number(success.total) || 0)
        setRecentTasks(recent.records)
      } catch (error) {
        if (!cancelled) handleRequestError(error)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div>
      <Typography.Title level={4}>仪表盘</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8}>
          <StatCard
            title="我的项目数量"
            value={projectCount}
            loading={loading}
            prefix={<ProjectOutlined />}
            color="#1677ff"
          />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <StatCard
            title="AI 任务数量"
            value={taskCount}
            loading={loading}
            prefix={<UnorderedListOutlined />}
            color="#722ed1"
          />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <StatCard
            title="成功任务数量"
            value={successCount}
            loading={loading}
            prefix={<CheckCircleOutlined />}
            color="#52c41a"
          />
        </Col>
      </Row>
      <Card title="最近审查记录" style={{ marginTop: 16 }}>
        <RecentTaskTable data={recentTasks} loading={loading} />
      </Card>
    </div>
  )
}

export default Dashboard
