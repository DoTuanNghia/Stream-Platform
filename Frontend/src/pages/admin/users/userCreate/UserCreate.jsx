import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { memberApi } from "@/api/admin/member.api";
import "./userCreate.scss";

export default function UserCreate() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: "",
    username: "",
    password: "",
    role: "USER", // mặc định
  });
  const [saving, setSaving] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((p) => ({ ...p, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await memberApi.create({
        fullName: form.fullName,
        username: form.username,
        password: form.password,
        role: form.role, // "ADMIN" | "USER"
      });

      window.alert("Tạo tài khoản thành công");
      navigate("/admin/users", { replace: true });
    } catch (err) {
      console.error(err);
      const msg = err?.response?.data || "Tạo người dùng thất bại.";
      window.alert(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-user-create">
      <div className="admin-user-create__header">
        <h2>Thêm mới người dùng</h2>
      </div>

      <form className="admin-user-create__card" onSubmit={handleSubmit}>
        <div className="field">
          <label>Tên người dùng</label>
          <input
            name="fullName"
            value={form.fullName}
            onChange={handleChange}
            placeholder="Ví dụ: Nguyễn Văn A"
          />
        </div>

        <div className="field">
          <label>Tài khoản</label>
          <input
            name="username"
            value={form.username}
            onChange={handleChange}
            placeholder="Ví dụ: nguyenvana"
          />
        </div>

        <div className="field">
          <label>Mật khẩu</label>
          <input
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            placeholder="Nhập mật khẩu"
          />
        </div>

        {/* ✅ Dropdown chọn quyền ngay dưới mật khẩu */}
        <div className="field">
          <label>Quyền</label>
          <select name="role" value={form.role} onChange={handleChange}>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>

        <div className="actions">
          <button
            type="button"
            className="btn"
            onClick={() => navigate("/admin/users")}
            disabled={saving}
          >
            Huỷ
          </button>
          {/* <br /> */}
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? "Đang lưu..." : "Lưu"}
          </button>
        </div>
      </form>
    </div>
  );
}
