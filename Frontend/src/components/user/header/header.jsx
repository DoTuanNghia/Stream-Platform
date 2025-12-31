// src/components/user/header/header.jsx (hoặc đúng path bạn đang dùng)
import React, { useState } from "react";
import "./Header.scss";
import { useNavigate } from "react-router-dom";

const Header = ({ onLogout }) => {
  const navigate = useNavigate();
  const [openMenu, setOpenMenu] = useState(false);

  const getCurrentUser = () => {
    try {
      const raw =
        localStorage.getItem("currentUser") ||
        sessionStorage.getItem("currentUser");
      return JSON.parse(raw || "null");
    } catch {
      return null;
    }
  };

  const user = getCurrentUser();

  const handleLogout = () => {
    // ✅ QUAN TRỌNG: báo cho App đổi state isLoggedIn = false
    if (typeof onLogout === "function") onLogout();
    else {
      // fallback an toàn nếu quên truyền prop
      localStorage.removeItem("currentUser");
      sessionStorage.removeItem("currentUser");
    }
    navigate("/login", { replace: true });
  };

  const toggleMenu = () => setOpenMenu((prev) => !prev);

  const displayName = user?.name || user?.username || "User";
  const displayRole = user?.role || "Role";

  const initials = displayName
    .split(" ")
    .filter(Boolean)
    .map((w) => w[0])
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
              <button onClick={handleLogout} className="header__dropdown-item">
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
