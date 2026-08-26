import { Card, Typography } from 'antd'

function Task() {
  return (
    <Card>
      <Typography.Title level={4}>AI 任务</Typography.Title>
      <Typography.Text type="secondary">
        功能开发中，当前仅完成前端工程初始化。
      </Typography.Text>
    </Card>
  )
}

export default Task
