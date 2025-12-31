import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { memberApi } from "@/api/admin/member.api";
import "./users.scss";

export default function Users() {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await memberApi.getAll();
      const data = res?.data ?? res;
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Chỉ lấy USER (không hiển thị ADMIN)
  const visibleRows = useMemo(() => {
    return rows
      .filter((m) => String(m?.role ?? "").toUpperCase() === "USER")
      .map((m) => ({
        ...m,
        channelCount: m.channelCount ?? 0,
      }));
  }, [rows]);

  const onDelete = async (id) => {
    const ok = window.confirm("Bạn chắc chắn muốn xoá người dùng này?");
    if (!ok) return;

    try {
      alert("Chưa có API xoá trên backend. UI đã sẵn sàng.");
    } catch (e) {
      console.error(e);
      alert("Xoá thất bại.");
    }
  };

  const onEdit = (id) => {
    alert(`Màn sửa sẽ làm sau. User ID: ${id}`);
  };

  return (
    <div className="admin-users">
      <div className="admin-users__header">
        <h2>Quản lý người dùng</h2>
        <button
          className="btn btn--primary"
          onClick={() => navigate("/admin/users/new")}
        >
          Thêm mới
        </button>
      </div>

      <div className="admin-users__card">
        {loading ? (
          <div className="admin-users__loading">Đang tải...</div>
        ) : (
          <div className="table-wrapper">
            <table className="table table--small">
              <thead>
                <tr>
                  <th className="col-stt">STT</th>
                  <th>Tên người dùng</th>
                  <th className="col-channels">Số kênh</th>
                  <th className="col-actions">Tuỳ chọn</th>
                </tr>
              </thead>

              <tbody>
                {visibleRows.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="admin-users__empty">
                      Không có dữ liệu
                    </td>
                  </tr>
                ) : (
                  visibleRows.map((m, idx) => (
                    <tr key={m.id ?? idx}>
                      <td className="col-stt">{idx + 1}</td>
                      <td className="admin-users__name">
                        {m.fullName ?? m.name ?? m.username ?? "n/a"}
                      </td>
                      <td className="col-channels">{m.channelCount}</td>
                      <td className="col-actions">
                        <div className="table__actions">
                          <button className="btn btn--ghost" onClick={() => onEdit(m.id)}>
                            Sửa
                          </button>
                          <button className="btn btn--danger" onClick={() => onDelete(m.id)}>
                            Xoá
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
