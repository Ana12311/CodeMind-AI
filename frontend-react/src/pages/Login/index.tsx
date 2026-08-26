import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { Button, Form, Input, message } from 'antd'
import { UserOutlined, LockOutlined, CodeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login as loginApi, fetchCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/store/auth'
import type { ApiResult } from '@/types'
import type { LoginRequest, User } from '@/types/user'
import './index.css'

// 上升代码符号粒子（静态数组，保证每次渲染一致）
const PARTICLES: { c: string; left: string; delay: string; dur: string; size: number }[] = [
  { c: '</>', left: '3%', delay: '0s', dur: '16s', size: 16 },
  { c: '{ }', left: '8%', delay: '3s', dur: '20s', size: 14 },
  { c: '=>', left: '12%', delay: '7s', dur: '18s', size: 18 },
  { c: 'def', left: '16%', delay: '1s', dur: '22s', size: 13 },
  { c: '()', left: '21%', delay: '5s', dur: '17s', size: 15 },
  { c: 'class', left: '26%', delay: '9s', dur: '24s', size: 13 },
  { c: 'import', left: '31%', delay: '2s', dur: '19s', size: 12 },
  { c: 'const', left: '36%', delay: '6s', dur: '21s', size: 12 },
  { c: '< >', left: '41%', delay: '4s', dur: '16s', size: 16 },
  { c: 'async', left: '46%', delay: '8s', dur: '23s', size: 12 },
  { c: 'await', left: '51%', delay: '1.5s', dur: '20s', size: 12 },
  { c: '[]', left: '56%', delay: '5.5s', dur: '18s', size: 14 },
  { c: ';', left: '60%', delay: '3.5s', dur: '15s', size: 22 },
  { c: 'return', left: '65%', delay: '7.5s', dur: '22s', size: 12 },
  { c: 'func', left: '70%', delay: '0.8s', dur: '19s', size: 13 },
  { c: '==', left: '74%', delay: '4.5s', dur: '17s', size: 16 },
  { c: '&&', left: '79%', delay: '8.5s', dur: '21s', size: 16 },
  { c: 'type', left: '83%', delay: '2.5s', dur: '23s', size: 12 },
  { c: '#', left: '87%', delay: '6.8s', dur: '18s', size: 20 },
  { c: 'interface', left: '91%', delay: '1.2s', dur: '24s', size: 11 },
  { c: '() =>', left: '95%', delay: '5.8s', dur: '19s', size: 13 },
  { c: '<div>', left: '19%', delay: '9.5s', dur: '25s', size: 12 },
  { c: 'SELECT', left: '38%', delay: '10s', dur: '26s', size: 11 },
  { c: 'public', left: '58%', delay: '7.2s', dur: '24s', size: 11 },
  { c: 'fn', left: '68%', delay: '3.2s', dur: '20s', size: 14 },
  { c: '!=', left: '77%', delay: '9s', dur: '18s', size: 16 },
  { c: '{}', left: '89%', delay: '2.8s', dur: '21s', size: 14 },
  { c: '::', left: '10%', delay: '6.2s', dur: '22s', size: 18 },
]

function Login() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const wrapRef = useRef<HTMLDivElement>(null)
  const glowRef = useRef<HTMLDivElement>(null)
  const cardRef = useRef<HTMLDivElement>(null)

  // 鼠标跟随光斑：直接改 transform，避免 React 重渲染；靠近登录卡时隐藏，避免覆盖卡片
  useEffect(() => {
    const wrap = wrapRef.current
    const glow = glowRef.current
    const card = cardRef.current
    if (!wrap || !glow) return
    const R = 150 // 光斑半径（直径 300px）
    const onMove = (e: MouseEvent) => {
      const rect = wrap.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      glow.style.transform = `translate(${x - R}px, ${y - R}px)`
      if (card) {
        const c = card.getBoundingClientRect()
        const over =
          e.clientX > c.left - R &&
          e.clientX < c.right + R &&
          e.clientY > c.top - R &&
          e.clientY < c.bottom + R
        glow.style.opacity = over ? '0' : '1'
      } else {
        glow.style.opacity = '1'
      }
    }
    wrap.addEventListener('mousemove', onMove, { passive: true })
    return () => wrap.removeEventListener('mousemove', onMove)
  }, [])

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
    <div className="login-wrap" ref={wrapRef}>
      <div className="login-bg" />
      <div className="blob blob-1" />
      <div className="blob blob-2" />
      <div className="blob blob-3" />

      {PARTICLES.map((p, i) => (
        <span
          key={i}
          className="particle"
          style={{
            left: p.left,
            animationDelay: p.delay,
            animationDuration: p.dur,
            fontSize: p.size,
          }}
        >
          {p.c}
        </span>
      ))}

      <div className="cursor-glow" ref={glowRef} />

      <div className="login-card" ref={cardRef}>
        <div className="brand">
          <div className="brand-logo">
            <CodeOutlined />
          </div>
          <div className="brand-title">CodeMind AI</div>
          <div className="brand-sub">AI 代码评审平台</div>
          <div className="brand-slogan">让每一行代码都经得起审查</div>
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

        <div className="login-footer">CODEMIND · 代码智能评审</div>
      </div>
    </div>
  )
}

export default Login
