import React from "react";
import { Route, Navigate } from "react-router-dom";

import AdminLayout from "../layouts/AdminLayout";
import Users from "../pages/admin/users/Users";
import UserCreate from "../pages/admin/users/userCreate/UserCreate";
import AdminGuard from "../guards/AdminGuard";

export default function AdminRoutes() {
  return (
    <Route element={<AdminGuard />}>
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<Navigate to="users" replace />} />

        <Route path="users" element={<Users />} />
        <Route path="users/new" element={<UserCreate />} />
      </Route>
    </Route>
  );
}
