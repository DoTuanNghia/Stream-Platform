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
      // Nếu backend chưa có POST /members thì tạm alert
      // await memberApi.create(form);
      alert("Chưa có API tạo user trên backend. UI đã sẵn sàng.");
      navigate("/admin/users", { replace: true });
    } catch (err) {
      console.error(err);
      alert("Tạo người dùng thất bại.");
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

        <div className="actions">
          <button
            type="button"
            className="btn"
            onClick={() => navigate("/admin/users")}
            disabled={saving}
          >
            Huỷ
          </button>
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? "Đang lưu..." : "Lưu"}
          </button>
        </div>
      </form>
    </div>
  );
}
