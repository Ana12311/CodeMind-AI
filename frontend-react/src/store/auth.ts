import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types/user'

interface AuthState {
  token: string | null
  user: User | null
  setToken: (token: string) => void
  setUser: (user: User) => void
  login: (token: string, user: User) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setToken: (token) => set({ token }),
      setUser: (user) => set({ user }),
      login: (token, user) => set({ token, user }),
      logout: () => set({ token: null, user: null }),
    }),
    {
      name: 'codemind-auth',
      // 只持久化 token/user，方法不序列化
      partialize: (state) => ({
        token: state.token,
        user: state.user,
      }),
    },
  ),
)
