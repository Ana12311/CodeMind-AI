import { useRoutes, Navigate } from 'react-router-dom'
import RequireAuth from '@/components/RequireAuth'
import MainLayout from '@/components/layout/MainLayout'
import Login from '@/pages/Login'
import Dashboard from '@/pages/Dashboard'
import Project from '@/pages/Project'
import File from '@/pages/File'
import Task from '@/pages/Task'
import Result from '@/pages/Result'

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
        { path: 'result', element: <Result /> },
      ],
    },
    { path: '*', element: <Navigate to="/" replace /> },
  ])
}

export default AppRouter
