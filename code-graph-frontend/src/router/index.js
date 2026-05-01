import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'overview',
    component: () => import('../pages/OverviewPage.vue')
  },
  {
    path: '/explore',
    name: 'explore',
    component: () => import('../pages/ExplorePage.vue')
  },
  {
    path: '/docs',
    name: 'docs',
    component: () => import('../pages/DocsPage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
