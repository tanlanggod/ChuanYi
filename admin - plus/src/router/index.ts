import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import AdminLayout from "../layouts/AdminLayout.vue";
import LoginView from "../views/LoginView.vue";
import DashboardView from "../views/DashboardView.vue";
import OrderView from "../views/OrderView.vue";
import GoodsView from "../views/GoodsView.vue";
import ContentView from "../views/ContentView.vue";
import ComponentView from "../views/ComponentView.vue";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "login",
    component: LoginView,
  },
  {
    path: "/",
    component: AdminLayout,
    children: [
      {
        path: "",
        name: "dashboard",
        component: DashboardView,
      },
      {
        path: "orders",
        name: "orders",
        component: OrderView,
      },
      {
        path: "goods",
        name: "goods",
        component: GoodsView,
      },
      {
        path: "content",
        name: "content",
        component: ContentView,
      },
      {
        path: "components",
        name: "components",
        component: ComponentView,
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("admin_token");
  if (to.path !== "/login" && !token) {
    next("/login");
    return;
  }
  if (to.path === "/login" && token) {
    next("/");
    return;
  }
  next();
});

export default router;
