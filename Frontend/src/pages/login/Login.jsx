// src/pages/login/login.jsx
import React, { useState } from "react";
import "./login.scss";

const Login = () => {
  const [form, setForm] = useState({
    username: "",
    password: "",
    remember: false,
  });

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // TODO: gọi API login ở đây (axiosClient...)
    console.log("Login data:", form);
  };

  return (
    <div className="login-page">
      <div className="login-card">

        <h2 className="login-card__title">Đăng nhập</h2>

        <form className="login-form" onSubmit={handleSubmit}>
          <div className="login-form__field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              name="username"
              type="text"
              placeholder="Nhập username"
              value={form.username}
              onChange={handleChange}
              autoComplete="username"
            />
          </div>

          <div className="login-form__field">
            <label htmlFor="password">Mật khẩu</label>
            <input
              id="password"
              name="password"
              type="password"
              placeholder="Nhập mật khẩu"
              value={form.password}
              onChange={handleChange}
              autoComplete="current-password"
            />
          </div>

          <div className="login-form__options">
            <label className="checkbox">
              <input
                type="checkbox"
                name="remember"
                checked={form.remember}
                onChange={handleChange}
              />
              <span>Ghi nhớ đăng nhập</span>
            </label>

            <button
              type="button"
              className="link-button"
              onClick={() => alert("Tính năng quên mật khẩu đang xây dựng")}
            >
              Quên mật khẩu?
            </button>
          </div>

          <button type="submit" className="btn btn--primary login-form__submit">
            Đăng nhập
          </button>
        </form>

        <p className="login-card__footer">
          © {new Date().getFullYear()} Stream Platform. All rights reserved.
        </p>
      </div>
    </div>
  );
};

export default Login;
