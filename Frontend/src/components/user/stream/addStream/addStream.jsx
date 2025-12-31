// src/components/stream/addStream/addStream.jsx
import React, { useEffect, useState } from "react";
import "./addStream.scss";

const emptyForm = {
  note: "",
  keyLive: "",
  videoList: "",
  fullHd: 0,
  startTime: "",
  startDate: "",
  streamAfter: 0,
  duration: 0,
};

const AddStream = ({ isOpen, onClose, onSave, channelId, initialData }) => {
  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    if (!isOpen) return;

    if (initialData) {
      const time = initialData.timeStart
        ? new Date(initialData.timeStart)
        : null;

      setForm({
        note: initialData.name || "",
        keyLive: initialData.keyStream || "",
        videoList: initialData.videoList || "",
        fullHd: 0,
        startDate: time ? time.toISOString().slice(0, 10) : "",
        startTime: time ? time.toTimeString().slice(0, 5) : "",
        streamAfter: 0,
        duration:
          initialData.duration !== null && initialData.duration !== undefined
            ? initialData.duration
            : 0,
      });
    } else {
      setForm(emptyForm);
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.keyLive) return;
    onSave(form);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal__header">
          <span className="modal__title">
            Kênh : {channelId || "Chưa chọn"}
          </span>
          <button className="modal__close" onClick={onClose}>
            ×
          </button>
        </div>

        <form className="modal__body" onSubmit={handleSubmit}>
          <div className="modal__field">
            <label>Tên luồng</label>
            <input
              type="text"
              name="note"
              value={form.note}
              onChange={handleChange}
              placeholder="Nhập tên luồng"
            />
          </div>

          <div className="modal__field">
            <label>Key live</label>
            <input
              type="text"
              name="keyLive"
              value={form.keyLive}
              onChange={handleChange}
              placeholder="Nhập key live"
            />
          </div>

          <div className="modal__field">
            <label>Danh sách videos</label>
            <textarea
              name="videoList"
              value={form.videoList}
              onChange={handleChange}
              placeholder="Nhập danh sách video (mỗi dòng 1 video, nếu cần)"
              rows={3}
            />
          </div>

          <div className="modal__field">
            <label>Upload Full HD</label>
            <input
              type="number"
              name="fullHd"
              value={form.fullHd}
              onChange={handleChange}
              min={0}
            />
          </div>

          <div className="modal__field">
            <label>Thời gian bắt đầu</label>
            <div className="modal__row-inline">
              <input
                type="time"
                name="startTime"
                value={form.startTime}
                onChange={handleChange}
              />

              <input
                type="date"
                name="startDate"
                value={form.startDate}
                onChange={handleChange}
              />

              {/* <input
                type="number"
                name="streamAfter"
                placeholder="Sau (phút)"
                value={form.streamAfter || ""}
                onChange={handleChange}
                className="modal__small-input"
                min={0}
              /> */}
            </div>
          </div>

          <div className="modal__field">
            <label>Thời lượng sẽ live</label>
            <input
              type="number"
              name="duration"
              value={form.duration}
              onChange={handleChange}
              min={-1}
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

export default AddStream;

