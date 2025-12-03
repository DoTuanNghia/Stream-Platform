// src/components/Header/Header.jsx
import React, { useState, useEffect } from "react";
import "./Header.scss";
import { useNavigate } from "react-router-dom";

const Header = () => {
  const navigate = useNavigate();
  const [openMenu, setOpenMenu] = useState(false);

  const getCurrentUser = () => {
    return (
      JSON.parse(localStorage.getItem("currentUser")) ||
      JSON.parse(sessionStorage.getItem("currentUser"))
    );
  };

  const [user, setUser] = useState(getCurrentUser());

  const handleLogout = () => {
    localStorage.removeItem("currentUser");
    sessionStorage.removeItem("currentUser");
    navigate("/login");
  };

  const toggleMenu = () => setOpenMenu(prev => !prev);

  // 👉 Lấy tên từ backend: user.name
  const displayName = user?.name || user?.username || "User";

  // 👉 Lấy role
  const displayRole = user?.role || "Role";

  // 👉 Avatar initials
  const initials = displayName
    .split(" ")
    .map(w => w[0])
    .join("")
    .toUpperCase();

  return (
    <header className="header">
      <div className="header__left">
        <div className="header__logo">
          <span className="header__logo-icon">▶</span>
          <span className="header__logo-text">STREAM PLATFORM</span>
        </div>
      </div>

      <div className="header__right">
        <button className="header__icon-btn" title="Thông báo">
          🔔
          <span className="header__badge">3</span>
        </button>

        <div className="header__user" onClick={toggleMenu}>
          <div className="header__user-info">
            <span className="header__user-name">{displayName}</span>
            <span className="header__user-role">{displayRole}</span>
          </div>

          <div className="header__avatar">
            <span>{initials}</span>
          </div>

          {openMenu && (
            <div className="header__dropdown">
              <button 
                onClick={handleLogout} 
                className="header__dropdown-item"
              >
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
