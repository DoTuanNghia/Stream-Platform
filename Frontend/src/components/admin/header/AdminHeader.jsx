import React, { useEffect, useMemo, useState } from "react";
import "./adminHeader.scss";
import { useLocation, useNavigate } from "react-router-dom";

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

export default function AdminHeader() {
  const navigate = useNavigate();
  const location = useLocation();

  const [openMenu, setOpenMenu] = useState(false);
  const [user, setUser] = useState(getCurrentUser());

  // ✅ mỗi khi đổi route admin → sync user
  useEffect(() => {
    setUser(getCurrentUser());
  }, [location.pathname]);

  const role = useMemo(() => {
    const roleRaw = user?.role ?? user?.type ?? "";
    return String(roleRaw).trim().toUpperCase();
  }, [user]);

  const displayName =
    user?.fullName || user?.name || user?.username || "Admin";

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
  localStorage.removeItem("currentUser");
  sessionStorage.removeItem("currentUser");
  window.location.replace("/login");
};

  const handleGoHome = () => {
    setOpenMenu(false);
    navigate("/home", { replace: true });
  };

  return (
    <header className="admin-header">
      <div className="admin-header__left">
        <div className="admin-header__title">
          <span className="admin-header__title-icon">▶</span>
          <span className="admin-header__title-text">STREAM PLATFORM</span>
        </div>
      </div>

      <div className="admin-header__right">
        <div className="admin-header__user" onClick={toggleMenu}>
          <div className="admin-header__user-info">
            <span className="admin-header__user-name">{displayName}</span>
            <span className="admin-header__user-role">
              {role || "ADMIN"}
            </span>
          </div>

          <div className="admin-header__avatar">
            <span>{initials}</span>
          </div>

          {openMenu && (
            <div className="admin-header__dropdown">
              {/* ✅ Trang chủ */}
              <button
                type="button"
                onClick={handleGoHome}
                className="admin-header__dropdown-item"
              >
                Trang chủ
              </button>

              {/* ✅ Đăng xuất */}
              <button
                type="button"
                onClick={handleLogout}
                className="admin-header__dropdown-item admin-header__dropdown-item--danger"
              >
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
