// src/components/user/header/header.jsx
import React, { useEffect, useMemo, useState } from "react";
import "./Header.scss";
import { useLocation, useNavigate } from "react-router-dom";

const getCurrentUser = () => {
  try {
    const raw =
      localStorage.getItem("currentUser") || sessionStorage.getItem("currentUser");
    return JSON.parse(raw || "null");
  } catch {
    return null;
  }
};

const Header = ({ onLogout }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const [openMenu, setOpenMenu] = useState(false);
  const [user, setUser] = useState(getCurrentUser());

  // ✅ đổi route thì sync lại user (tránh stale)
  useEffect(() => {
    setUser(getCurrentUser());
  }, [location.pathname]);

  const role = useMemo(() => {
    const roleRaw = user?.role ?? user?.type ?? "";
    return String(roleRaw).trim().toUpperCase();
  }, [user]);

  const isAdmin = role === "ADMIN" || role === "ROLE_ADMIN";

  const displayName = user?.fullName || user?.name || user?.username || "User";
  const displayRole = role || "USER";

  const initials = displayName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();

  const toggleMenu = () => {
    setUser(getCurrentUser());
    setOpenMenu((p) => !p);
  };

  const handleLogout = () => {
    // gọi callback nếu có, còn không thì tự xoá storage
    if (typeof onLogout === "function") {
      onLogout();
    } else {
      localStorage.removeItem("currentUser");
      sessionStorage.removeItem("currentUser");
    }
    setOpenMenu(false);
    navigate("/login", { replace: true });
  };

  const handleGoAdmin = () => {
    setOpenMenu(false);
    navigate("/admin", { replace: true });
  };

  return (
    <header className="header">
      <div className="header__left">
        <div className="header__logo">
          <span className="header__logo-icon">▶</span>
          <span className="header__logo-text">STREAM PLATFORM</span>
        </div>
      </div>

      <div className="header__right">
        <button className="header__icon-btn" title="Thông báo" type="button">
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
              {isAdmin && (
                <button
                  type="button"
                  onClick={handleGoAdmin}
                  className="header__dropdown-item"
                >
                  Quản lý
                </button>
              )}

              <button
                type="button"
                onClick={handleLogout}
                className="header__dropdown-item header__dropdown-item--danger"
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
