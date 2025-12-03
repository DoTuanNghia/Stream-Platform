// src/components/channel/channel.jsx
import React, { useEffect, useState } from "react";
import "./channel.scss";
import AddChannel from "./addChannel/addChannel.jsx";
import axiosClient from "../../api/axiosClient";

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

  const currentUser = getCurrentUser();
  const userId = currentUser?.id;

  const fetchChannels = async () => {
    if (!userId) {
      setError("Chưa đăng nhập, không lấy được danh sách kênh.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      // GET /api/channels/user/{userId}
      const data = await axiosClient.get(`/channels/user/${userId}`);
      setChannels(data.channels || []);
    } catch (err) {
      console.error(err);
      if (err.response?.status === 404) {
        setChannels([]);
        setError("User này chưa có kênh nào, hãy thêm kênh mới.");
      } else {
        setError("Không tải được danh sách kênh.");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchChannels();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const handleAddChannel = async (form) => {
    if (!userId) return;
    try {
      const payload = {
        name: form.name,
        channelCode: form.channelId,
      };
      await axiosClient.post(`/channels/user/${userId}`, payload);
      await fetchChannels();
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
      await fetchChannels();
      if (selectedChannel?.id === channelId && onSelectChannel) {
        onSelectChannel(null);
      }
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
          <h2 className="card__title">Danh sách kênh</h2>
          <button className="btn btn--primary" onClick={openAddModal}>
            Thêm kênh
          </button>
        </div>

        {currentUser && (
          <p className="card__subtitle">
            User: <strong>{currentUser.username}</strong> (ID: {currentUser.id})
          </p>
        )}
        {error && <p className="card__subtitle card__subtitle--error">{error}</p>}

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
                    <td>{index + 1}</td>
                    <td>{ch.name}</td>
                    <td className="table__mono"><a href={`https://www.youtube.com/channel/${ch.channelCode}`}>{ch.channelCode}</a></td>
                    <td>
                      <div className="table__actions">
                        <button
                          className="btn btn--danger"
                          onClick={() => handleDelete(ch.id)}
                        >
                          Xóa
                        </button>
                        <button
                          className={`btn btn--ghost ${selectedChannel?.id === ch.id ? "is-active" : ""
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
