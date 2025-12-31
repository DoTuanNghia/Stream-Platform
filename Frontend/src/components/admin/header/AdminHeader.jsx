import React, { useEffect, useState } from "react";
import "./adminHeader.scss";
import { useNavigate } from "react-router-dom";

const getCurrentUser = () => {
  try {
    const ls = localStorage.getItem("currentUser");
    const ss = sessionStorage.getItem("currentUser");
    return JSON.parse(ls || ss || "null");
  } catch {
    return null;
  }
};

export default function AdminHeader() {
  const navigate = useNavigate();
  const [openMenu, setOpenMenu] = useState(false);
  const [user, setUser] = useState(getCurrentUser());

  useEffect(() => {
    const onStorage = () => setUser(getCurrentUser());
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("currentUser");
    sessionStorage.removeItem("currentUser");
    navigate("/login", { replace: true });
  };

  const toggleMenu = () => setOpenMenu((p) => !p);

  const displayName = user?.fullName || user?.name || user?.username || "Admin";
  const displayRole = (user?.role || "ADMIN").toUpperCase();

  const initials = displayName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();

  return (
    <header className="admin-header">
      <div className="admin-header__left">
        <div className="admin-header__title">
          <span className="admin-header__title-icon">⚙</span>
          <span className="admin-header__title-text">ADMIN PANEL</span>
        </div>
      </div>

      <div className="admin-header__right">
        <div className="admin-header__user" onClick={toggleMenu}>
          <div className="admin-header__user-info">
            <span className="admin-header__user-name">{displayName}</span>
            <span className="admin-header__user-role">{displayRole}</span>
          </div>

          <div className="admin-header__avatar">
            <span>{initials}</span>
          </div>

          {openMenu && (
            <div className="admin-header__dropdown">
              <button
                type="button"
                onClick={handleLogout}
                className="admin-header__dropdown-item"
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
