// src/components/channel/channel.jsx
import React, { useEffect, useState } from "react";
import "./channel.scss";
import AddChannel from "./addChannel/addChannel.jsx";
import axiosClient from "../../../api/axiosClient.js";

const PAGE_SIZE = 10;

const getCurrentUser = () => {
  const raw =
    localStorage.getItem("currentUser") ||
    sessionStorage.getItem("currentUser");
  return raw ? JSON.parse(raw) : null;
};

const Channel = ({ onSelectChannel, selectedChannel }) => {
  const [channels, setChannels] = useState([]);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Pagination (UI 1-based)
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const currentUser = getCurrentUser();
  const userId = currentUser?.id;

  const fetchChannels = async (pageNumber = page) => {
    if (!userId) {
      setError("Chưa đăng nhập, không lấy được danh sách kênh.");
      setChannels([]);
      setPage(1);
      setTotalPages(1);
      setTotalElements(0);
      return;
    }

    setLoading(true);
    setError("");

    try {
      const bePage = Math.max(0, pageNumber - 1);

      const data = await axiosClient.get(`/channels/user/${userId}`, {
        params: { page: bePage, size: PAGE_SIZE, sort: "id,desc" },
      });

      const list = data.channels || [];
      const tp = Number(data.totalPages ?? 1) || 1;
      const te = Number(data.totalElements ?? 0) || 0;

      // nếu sau thao tác (xóa) mà page vượt tp, tự lùi về trang cuối hợp lệ
      const safePage = Math.min(Math.max(1, pageNumber), tp);

      setChannels(list);
      setTotalPages(tp);
      setTotalElements(te);
      setPage(safePage);
    } catch (err) {
      console.error(err);
      if (err.response?.status === 404) {
        setChannels([]);
        setTotalPages(1);
        setTotalElements(0);
        setError("User này chưa có kênh nào, hãy thêm kênh mới.");
      } else {
        setError("Không tải được danh sách kênh.");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // đổi user -> về trang 1
    setPage(1);
    fetchChannels(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const gotoPage = async (p) => {
    const next = Math.min(Math.max(1, p), totalPages);
    if (next === page) return;
    await fetchChannels(next);
  };

  const handleAddChannel = async (form) => {
    if (!userId) return;
    try {
      const payload = {
        name: form.name,
        channelCode: form.channelId,
      };
      await axiosClient.post(`/channels/user/${userId}`, payload);

      // Sau khi thêm: về trang 1 để thấy kênh mới nếu sort id,desc
      await fetchChannels(1);
      setIsAddOpen(false);
    } catch (err) {
      console.error(err);
      alert("Tạo kênh thất bại.");
    }
  };

  const handleDelete = async (channelId) => {
    if (!window.confirm("Xóa kênh này?")) return;
    try {
      await axiosClient.delete(`/channels/${channelId}`);

      // nếu đang chọn kênh bị xóa -> clear selection
      if (selectedChannel?.id === channelId && onSelectChannel) {
        onSelectChannel(null);
      }

      // reload lại trang hiện tại
      await fetchChannels(page);
    } catch (err) {
      console.error(err);
      alert("Xóa kênh thất bại.");
    }
  };

  const handleSelect = (ch) => {
    if (onSelectChannel) {
      onSelectChannel(ch);
    }
  };

  const openAddModal = () => setIsAddOpen(true);
  const closeAddModal = () => setIsAddOpen(false);

  return (
    <>
      <section className="card">
        <div className="card__header">
          <div>
            <h2 className="card__title">Danh sách kênh</h2>
            {currentUser && (
              <p className="card__subtitle">
                User: <strong>{currentUser.username}</strong> (ID:{" "}
                {currentUser.id}) — Tổng: <strong>{totalElements}</strong> kênh
              </p>
            )}
          </div>

          <button className="btn btn--primary" onClick={openAddModal}>
            Thêm kênh
          </button>
        </div>

        {error && (
          <p className="card__subtitle card__subtitle--error">{error}</p>
        )}

        {/* Pagination */}
        {!loading && totalElements > 0 && (
          <div className="table__toolbar">
            <div className="table__pagination">
              <button
                className="btn btn--ghost"
                onClick={() => gotoPage(1)}
                disabled={page <= 1}
              >
                First
              </button>
              <button
                className="btn btn--ghost"
                onClick={() => gotoPage(page - 1)}
                disabled={page <= 1}
              >
                Prev
              </button>

              <span className="table__pageinfo">
                <strong>{page}</strong> / {totalPages}
              </span>

              <button
                className="btn btn--ghost"
                onClick={() => gotoPage(page + 1)}
                disabled={page >= totalPages}
              >
                Next
              </button>
              <button
                className="btn btn--ghost"
                onClick={() => gotoPage(totalPages)}
                disabled={page >= totalPages}
              >
                Last
              </button>
            </div>
          </div>
        )}

        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Kênh</th>
                <th>ID kênh</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={4}>Đang tải...</td>
                </tr>
              ) : channels.length === 0 ? (
                <tr>
                  <td colSpan={4}>Không có kênh nào.</td>
                </tr>
              ) : (
                channels.map((ch, index) => (
                  <tr
                    key={ch.id}
                    className={
                      selectedChannel?.id === ch.id ? "table__row--active" : ""
                    }
                  >
                    {/* STT theo toàn cục */}
                    <td>{(page - 1) * PAGE_SIZE + index + 1}</td>
                    <td>{ch.name}</td>
                    <td className="table__mono">
                      <a
                        href={`https://www.youtube.com/channel/${ch.channelCode}`}
                        target="_blank"
                        rel="noreferrer"
                      >
                        {ch.channelCode}
                      </a>
                    </td>
                    <td>
                      <div className="table__actions">
                        <button
                          className="btn btn--danger"
                          onClick={() => handleDelete(ch.id)}
                        >
                          Xóa
                        </button>
                        <button
                          className={`btn btn--ghost ${
                            selectedChannel?.id === ch.id ? "is-active" : ""
                          }`}
                          onClick={() => handleSelect(ch)}
                        >
                          Chọn
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <AddChannel
        isOpen={isAddOpen}
        onClose={closeAddModal}
        onSave={handleAddChannel}
      />
    </>
  );
};

export default Channel;
