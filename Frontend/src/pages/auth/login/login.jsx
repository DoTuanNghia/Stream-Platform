// src/pages/auth/login/login.jsx
import React, { useState } from "react";
import "./login.scss";
import axiosClient from "@/api/axiosClient";
import { useNavigate } from "react-router-dom";

const Login = ({ onLoginSuccess }) => {
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
      const res = await axiosClient.post("/auth/login", null, {
        params: {
          username: form.username,
          password: form.password,
        },
      });

      // axiosClient có thể trả về response hoặc trả thẳng data
      const member = res?.data ?? res;

      // Bắt buộc lưu 1 object "gọn" để tránh stringify lỗi
      const safeUser = {
        id: member?.id ?? null,
        username: member?.username ?? form.username,
        fullName: member?.fullName ?? member?.name ?? "",
        role: member?.role ?? member?.type ?? "USER",
      };

      // Nếu remember -> localStorage, không remember -> sessionStorage
      if (form.remember) {
        localStorage.setItem("currentUser", JSON.stringify(safeUser));
        sessionStorage.removeItem("currentUser");
      } else {
        sessionStorage.setItem("currentUser", JSON.stringify(safeUser));
        localStorage.removeItem("currentUser");
      }

      onLoginSuccess();          // 🔥 DÒNG QUAN TRỌNG
      navigate("/home", { replace: true });

    } catch (err) {
      console.error("LOGIN ERROR:", err);
      console.error("STATUS:", err?.response?.status);
      const msg =
        err?.response?.data || "Đăng nhập thất bại, vui lòng kiểm tra lại.";
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

          {/* <div className="login-form__options">
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
              onClick={() => alert("Tính năng quên mật khẩu đang xây dựng.")}
            >
              Quên mật khẩu?
            </button>
          </div> */}

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
