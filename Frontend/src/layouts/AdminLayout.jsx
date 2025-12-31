import React from "react";
import { Outlet } from "react-router-dom";
import AdminSidebar from "../components/admin/sidebar/AdminSidebar";
import AdminHeader from "../components/admin/header/AdminHeader";
import "./adminLayout.scss";

export default function AdminLayout() {
  return (
    <div className="admin-layout">
      <AdminSidebar />

      <div className="admin-layout__main">
        <AdminHeader />
        <main className="admin-layout__content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
