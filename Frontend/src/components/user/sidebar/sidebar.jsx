// src/components/Sidebar/Sidebar.jsx
import React from "react";
import "./sidebar.scss";

const Sidebar = ({ activeMenu, onChangeMenu }) => {
  const menuItems = [
    { key: "stream", label: "Danh sách luồng" },
    { key: "streamSession", label: "Luồng đang hoạt động" },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar__section-title">Dashboard</div>

      <nav className="sidebar__nav">
        {menuItems.map((item) => (
          <button
            key={item.key}
            className={
              "sidebar__nav-item" +
              (activeMenu === item.key ? " sidebar__nav-item--active" : "")
            }
            onClick={() => onChangeMenu(item.key)}
          >
            <span className="sidebar__nav-dot" />
            <span>{item.label}</span>
          </button>
        ))}
      </nav>
    </aside>
  );
};

export default Sidebar;
