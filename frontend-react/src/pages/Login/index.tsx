import { useState } from 'react'
import axios from 'axios'
import { Button, Card, Form, Input, Typography, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login as loginApi, fetchCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/store/auth'
import type { ApiResult } from '@/types'
import type { LoginRequest, User } from '@/types/user'

function Login() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const onFinish = async (values: LoginRequest) => {
    setLoading(true)
    try {
      const { accessToken } = await loginApi(values)

      // 先落 token，使后续 /me 携带鉴权头；再用表单用户名占位用户信息
      useAuthStore.getState().setToken(accessToken)
      useAuthStore.getState().setUser({ id: '', username: values.username } as User)

      try {
        const me = await fetchCurrentUser()
        useAuthStore.getState().setUser(me)
      } catch {
        // 拉取用户详情失败不阻断登录，重新确保 token 仍在（避免被 401 拦截器登出）
        useAuthStore.getState().setToken(accessToken)
      }

      message.success('登录成功')
      navigate('/dashboard', { replace: true })
    } catch (error) {
      if (axios.isAxiosError(error)) {
        if (!error.response) {
          message.error('网络异常，请检查后端服务是否启动')
        } else if (error.response.status === 401) {
          message.error((error.response.data as ApiResult)?.message || '用户名或密码错误')
        } else {
          message.error((error.response.data as ApiResult)?.message || '登录失败')
        }
      } else {
        message.error('登录失败')
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
