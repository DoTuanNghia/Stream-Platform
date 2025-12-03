// src/pages/login/login.jsx
import React, { useState } from "react";
import "./login.scss";
import axiosClient from "../../api/axiosClient";
import { useNavigate } from "react-router-dom";

const Login = () => {
  const [form, setForm] = useState({
    username: "",
    password: "",
    remember: false,
  });
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      // AuthController dùng @RequestParam username/password 
      const member = await axiosClient.post(
        "/auth/login",
        null,
        {
          params: {
            username: form.username,
            password: form.password,
          },
        }
      );

      const storage = form.remember ? localStorage : sessionStorage;
      storage.setItem("currentUser", JSON.stringify(member));

      navigate("/"); // sang Home
    } catch (err) {
      console.error(err);
      const msg =
        err.response?.data || "Đăng nhập thất bại, vui lòng kiểm tra lại.";
      setError(typeof msg === "string" ? msg : "Đăng nhập thất bại.");
    }
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
              onClick={() =>
                alert("Tính năng quên mật khẩu đang xây dựng.")
              }
            >
              Quên mật khẩu?
            </button>
          </div>

          {error && <p className="login-form__error">{error}</p>}

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
