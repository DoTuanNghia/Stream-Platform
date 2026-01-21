// src/components/stream/addStream/addStream.jsx
import React, { useEffect, useRef, useState } from "react";
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

const AddStream = ({ isOpen, onClose, onSave, initialData }) => {

  const [form, setForm] = useState(emptyForm);

  const overlayMouseDownOnBackdropRef = useRef(false);

  useEffect(() => {
    if (!isOpen) return;

    if (initialData) {
      const time = initialData.timeStart ? new Date(initialData.timeStart) : null;

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

  const handleOverlayMouseDown = (e) => {
    // chỉ đánh dấu TRUE nếu mousedown trực tiếp trên backdrop (overlay), không phải con bên trong
    overlayMouseDownOnBackdropRef.current = e.target === e.currentTarget;
  };

  const handleOverlayMouseUp = (e) => {
    // chỉ đóng nếu: mousedown trên backdrop và mouseup cũng trên backdrop
    const mouseUpOnBackdrop = e.target === e.currentTarget;
    if (overlayMouseDownOnBackdropRef.current && mouseUpOnBackdrop) {
      onClose();
    }
    overlayMouseDownOnBackdropRef.current = false;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const VIDEO_BASE_DIR = "D:\\videos\\";

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.keyLive) return;

    const rawName = (form.videoList || "").trim(); // user nhập: name hoặc name.mp4

    // nếu rỗng thì thôi
    if (!rawName) {
      onSave({ ...form, videoList: "" });
      return;
    }

    // nếu user đã nhập full path rồi thì giữ nguyên (phòng khi bạn muốn)
    const isFullPath = /^[a-zA-Z]:\\/.test(rawName);
    if (isFullPath) {
      onSave({ ...form, videoList: rawName });
      return;
    }

    // thêm .mp4 nếu thiếu
    const filename = rawName.toLowerCase().endsWith(".mp4") ? rawName : `${rawName}.mp4`;

    // ghép thành D:\videos\filename
    const fullPath = `${VIDEO_BASE_DIR}${filename}`;

    onSave({
      ...form,
      videoList: fullPath,
    });
  };


  return (
    <div
      className="modal-overlay"
      onMouseDown={handleOverlayMouseDown}
      onMouseUp={handleOverlayMouseUp}
    >
      <div className="modal" onMouseDown={(e) => e.stopPropagation()} onMouseUp={(e) => e.stopPropagation()}>
        <div className="modal__header">
          <span className="modal__title">
            {initialData ? "Sửa Stream" : "Tạo Stream"}
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
            <label>Tên video</label>
            <input
              type="text"
              name="videoList"
              value={form.videoList}
              onChange={handleChange}
              placeholder='Link/Tên video (vd: "video1.mp4" hoặc "D:\videos\video1.mp4")'
            />
          </div>


          <div className="modal__field">
            <label>Thời gian bắt đầu</label>
            <div className="modal__row-inline">
              <input type="time" name="startTime" value={form.startTime} onChange={handleChange} />
              <input type="date" name="startDate" value={form.startDate} onChange={handleChange} />
            </div>
          </div>

          <div className="modal__field">
            <label>Thời lượng sẽ live</label>
            <input type="number" name="duration" value={form.duration} onChange={handleChange} min={-1} />
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
