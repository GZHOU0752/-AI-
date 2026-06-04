import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    redirect: '/code-review',
    children: [
      {
        path: 'code-review',
        name: 'CodeReview',
        component: () => import('@/views/CodeReview.vue'),
        meta: { title: 'AI代码评审', icon: 'DocumentChecked' }
      },
      {
        path: 'knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/KnowledgeBase.vue'),
        meta: { title: '知识库问答', icon: 'ChatDotRound' }
      },
      {
        path: 'memory',
        name: 'Memory',
        component: () => import('@/views/MemoryManage.vue'),
        meta: { title: '记忆管理', icon: 'Connection' }
      },
      {
        path: 'unit-test',
        name: 'UnitTest',
        component: () => import('@/views/UnitTest.vue'),
        meta: { title: '单元测试', icon: 'Notebook' }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to) => {
  if (to.name !== 'Login') {
    try {
      const raw = localStorage.getItem('ruima_auth')
      if (!raw) return { name: 'Login' }
      const auth = JSON.parse(raw)
      if (!auth.loggedIn) return { name: 'Login' }
    } catch (e) {
      return { name: 'Login' }
    }
  }
})

export default router
