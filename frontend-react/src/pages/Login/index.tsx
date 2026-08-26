import { useState } from 'react'
import axios from 'axios'
import { Button, Card, Form, Input, Typography, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { http } from '@/api/request'
import { useAuthStore } from '@/store/auth'
import type { LoginRequest, LoginResponse } from '@/types'

function Login() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const setToken = useAuthStore((state) => state.setToken)
  const setUser = useAuthStore((state) => state.setUser)

  const onFinish = async (values: LoginRequest) => {
    setLoading(true)
    try {
      const data = await http<LoginResponse>({
        method: 'post',
        url: '/v1/auth/login',
        data: values,
      })
      setToken(data.accessToken)
      setUser({ username: values.username })
      message.success('登录成功')
      navigate('/dashboard', { replace: true })
    } catch (error) {
      // 业务/HTTP 错误已由拦截器提示，这里只兜底网络异常
      if (axios.isAxiosError(error) && !error.response) {
        message.error('网络异常，请检查后端服务是否启动')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 360 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Typography.Title level={3} style={{ marginBottom: 4 }}>
            CodeMind AI
          </Typography.Title>
          <Typography.Text type="secondary">AI 代码评审平台</Typography.Text>
        </div>
        <Form<LoginRequest> onFinish={onFinish} size="large">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" block loading={loading}>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Login
