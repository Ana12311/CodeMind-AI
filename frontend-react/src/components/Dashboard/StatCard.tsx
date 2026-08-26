import { Card, Statistic } from 'antd'
import type { ReactNode } from 'react'

interface StatCardProps {
  title: string
  value: number | string
  loading?: boolean
  prefix?: ReactNode
  color?: string
}

function StatCard({ title, value, loading, prefix, color }: StatCardProps) {
  return (
    <Card loading={loading}>
      <Statistic
        title={title}
        value={value}
        prefix={prefix}
        valueStyle={color ? { color } : undefined}
      />
    </Card>
  )
}

export default StatCard
