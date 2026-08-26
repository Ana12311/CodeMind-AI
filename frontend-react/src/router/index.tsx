import { lazy, Suspense } from 'react'
import { useRoutes, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import RequireAuth from '@/components/RequireAuth'
import MainLayout from '@/components/layout/MainLayout'
import Login from '@/pages/Login'
import Dashboard from '@/pages/Dashboard'
import Project from '@/pages/Project'
import File from '@/pages/File'
import Task from '@/pages/Task'

// Result 页依赖 Monaco，体积大，按需懒加载避免首屏加载编辑器
const Result = lazy(() => import('@/pages/Result'))

function AppRouter() {
  return useRoutes([
    { path: '/login', element: <Login /> },
    {
      path: '/',
      element: (
        <RequireAuth>
          <MainLayout />
        </RequireAuth>
      ),
      children: [
        { index: true, element: <Navigate to="/dashboard" replace /> },
        { path: 'dashboard', element: <Dashboard /> },
        { path: 'project', element: <Project /> },
        { path: 'file', element: <File /> },
        { path: 'task', element: <Task /> },
        {
          path: 'result',
          element: (
            <Suspense fallback={<Spin style={{ display: 'block', margin: '120px auto' }} />}>
              <Result />
            </Suspense>
          ),
        },
      ],
    },
    { path: '*', element: <Navigate to="/" replace /> },
  ])
}

export default AppRouter
