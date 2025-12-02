// src/components/Header/Header.jsx
import React from "react";
import "./Header.scss";

const Header = () => {
  return (
    <header className="header">
      <div className="header__left">
        <div className="header__logo">
          {/* logo text, sau này bạn có thể đổi thành ảnh */}
          <span className="header__logo-icon">▶</span>
          <span className="header__logo-text">STREAM PLATFORM</span>
        </div>
      </div>

      <div className="header__right">
        {/* Thông báo */}
        <button className="header__icon-btn" title="Thông báo">
          🔔
          <span className="header__badge">3</span>
        </button>

        {/* User info */}
        <div className="header__user">
          <div className="header__user-info">
            <span className="header__user-name">Nguyen Admin</span>
            <span className="header__user-role">Administrator</span>
          </div>
          <div className="header__avatar">
            {/* có thể thay bằng <img src="..." /> */}
            <span>NA</span>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
