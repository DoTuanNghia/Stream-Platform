import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { memberApi } from "@/api/admin/member.api";
import "./userUpdate.scss";

export default function UserUpdate() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    fullName: "",
    username: "",
    password: "",
    role: "USER",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((p) => ({ ...p, [name]: value }));
  };

  const fetchMember = async () => {
    setLoading(true);
    try {
      const res = await memberApi.getById(id);
      const m = res?.data ?? res;

      setForm({
        fullName: m?.fullName ?? m?.name ?? "",
        username: m?.username ?? "",
        password: "", // luôn để trống
        role: String(m?.role ?? "USER").toUpperCase(),
      });
    } catch (err) {
      console.error(err);
      window.alert(err?.response?.data || "Không tải được dữ liệu người dùng.");
      navigate("/admin/users", { replace: true });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMember();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        fullName: form.fullName,
        role: form.role,
      };

      // chỉ gửi password nếu có nhập
      if (form.password?.trim()) payload.password = form.password;

      await memberApi.update(id, payload);

      window.alert("Cập nhật tài khoản thành công");
      navigate("/admin/users", { replace: true });
    } catch (err) {
      console.error(err);
      window.alert(err?.response?.data || "Cập nhật thất bại.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="admin-user-update__loading">Đang tải...</div>;
  }

  return (
    <div className="admin-user-update">
      <div className="admin-user-update__header">
        <h2>Cập nhật người dùng</h2>
      </div>

      <form className="admin-user-update__card" onSubmit={handleSubmit}>
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
          <label>Tài khoản (không sửa)</label>
          <input
            name="username"
            value={form.username}
            disabled
            placeholder="username"
          />
        </div>

        <div className="field">
          <label>Mật khẩu mới (bỏ trống nếu không đổi)</label>
          <input
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            placeholder="Nhập mật khẩu mới nếu muốn đổi"
          />
        </div>

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
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? "Đang lưu..." : "Lưu"}
          </button>
        </div>
      </form>
    </div>
  );
}
