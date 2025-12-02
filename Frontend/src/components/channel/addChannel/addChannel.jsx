// src/components/channel/addChannel.jsx
import React, { useState, useEffect } from "react";
import "./addChannel.scss";

const AddChannel = ({ isOpen, onClose, onSave }) => {
  const [form, setForm] = useState({
    username: "",
    channelId: "",
    name: "",
  });

  // reset form mỗi lần mở
  useEffect(() => {
    if (isOpen) {
      setForm({ username: "", channelId: "", name: "" });
    }
  }, [isOpen]);

  if (!isOpen) return null; // đóng modal thì không render

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.username || !form.channelId || !form.name) return;
    onSave(form); // gửi dữ liệu lên Channel
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal"
        onClick={(e) => e.stopPropagation()} // tránh đóng khi click trong modal
      >
        <div className="modal__header">
          <span className="modal__title">Kênh :</span>
          <button className="modal__close" onClick={onClose}>
            ×
          </button>
        </div>

        <form className="modal__body" onSubmit={handleSubmit}>
          <div className="modal__field">
            <label>Username</label>
            <input
              type="text"
              name="username"
              value={form.username}
              onChange={handleChange}
              placeholder="Nhập username"
            />
          </div>

          <div className="modal__field">
            <label>ID Kênh</label>
            <input
              type="text"
              name="channelId"
              value={form.channelId}
              onChange={handleChange}
              placeholder="Ví dụ: UCxxxxxx..."
            />
          </div>

          <div className="modal__field">
            <label>Tên Kênh</label>
            <input
              type="text"
              name="name"
              value={form.name}
              onChange={handleChange}
              placeholder="Tên hiển thị của kênh"
            />
          </div>

          <div className="modal__footer">
            <button type="submit" className="btn btn--primary modal__submit">
              Lưu
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddChannel;
