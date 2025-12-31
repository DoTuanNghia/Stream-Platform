import React from "react";
import { Route } from "react-router-dom";
import AdminLayout from "@/layouts/AdminLayout";
import Users from "@/pages/admin/users/Users";
import UserCreate from "@/pages/admin/users/userCreate/UserCreate";

export default function AdminRoutes() {
  return (
    <Route path="/admin" element={<AdminLayout />}>
      <Route path="users" element={<Users />} />
      <Route path="users/new" element={<UserCreate />} />
    </Route>
  );
}
