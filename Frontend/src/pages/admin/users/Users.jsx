import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { memberApi } from "../../../api/admin/member.api";
import { channelApi } from "../../../api/admin/channel.api.js";

import "./users.scss";

export default function Users() {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      // 1) lấy users
      const res = await memberApi.getAll();
      const data = res?.data ?? res;
      const users = Array.isArray(data) ? data : [];

      // 2) lọc USER trước để giảm số request
      const onlyUsers = users.filter(
        (m) => String(m?.role ?? "").toUpperCase() === "USER"
      );

      // 3) gọi count cho từng user (song song)
      const counts = await Promise.all(
        onlyUsers.map(async (u) => {
          try {
            const r = await channelApi.countByUserId(u.id);
            const d = r?.data ?? r;

            // backend trả { channelCount: n }
            return { userId: u.id, channelCount: Number(d?.channelCount ?? 0) };
          } catch (e) {
            console.error("COUNT CHANNEL ERROR userId=" + u.id, e);
            return { userId: u.id, channelCount: 0 };
          }
        })
      );

      // 4) map userId -> channelCount
      const countMap = new Map(counts.map((x) => [x.userId, x.channelCount]));

      // 5) setRows = onlyUsers + channelCount
      const merged = onlyUsers.map((u) => ({
        ...u,
        channelCount: countMap.get(u.id) ?? 0,
      }));

      setRows(merged);
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

  const visibleRows = useMemo(() => {
    return rows.map((m) => ({
      ...m,
      channelCount: m.channelCount ?? 0,
    }));
  }, [rows]);

  const onEdit = (id) => {
    navigate(`/admin/users/${id}/edit`);
  };

  const onDelete = (id) => {
    navigate(`/admin/users/${id}/delete`);
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
                          <button
                            className="btn btn--ghost"
                            onClick={() => onEdit(m.id)}
                          >
                            Sửa
                          </button>
                          <button
                            className="btn btn--danger"
                            onClick={() => onDelete(m.id)}
                          >
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
