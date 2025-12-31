// src/guards/AdminGuard.jsx
import React from "react";
import { Navigate, Outlet } from "react-router-dom";

const getCurrentUser = () => {
  try {
    const raw =
      localStorage.getItem("currentUser") || sessionStorage.getItem("currentUser");
    return JSON.parse(raw || "null");
  } catch {
    return null;
  }
};

export default function AdminGuard() {
  const user = getCurrentUser();

  if (!user) return <Navigate to="/login" replace />;

  const roleRaw = user?.role ?? user?.type ?? "";
  const role = String(roleRaw).trim().toUpperCase();

  // chấp nhận cả ADMIN và ROLE_ADMIN
  const isAdmin = role === "ADMIN" || role === "ROLE_ADMIN";

  if (!isAdmin) return <Navigate to="/home" replace />;

  return <Outlet />;
}
