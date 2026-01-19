import React from "react";
import { NavLink } from "react-router-dom";
import "./adminSidebar.scss";

export default function AdminSidebar() {
  return (
    <aside className="admin-sidebar">
      <div className="admin-sidebar__brand">Dashboard</div>

      <nav className="admin-sidebar__nav">
        <NavLink
          to="/admin/users"
          className={({ isActive }) =>
            "admin-sidebar__link" + (isActive ? " is-active" : "")
          }
        >
          Quản lý người dùng
        </NavLink>
        <NavLink
          to="/admin/streams"
          className={({ isActive }) =>
            "admin-sidebar__link" + (isActive ? " is-active" : "")
          }
        >
          Thống kê luồng
        </NavLink>

      </nav>
    </aside>
  );
}
