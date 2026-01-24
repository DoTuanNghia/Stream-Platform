// src/components/stream/addStream/addStream.jsx
import React, { useEffect, useRef, useState } from "react";
import "./addStream.scss";
import {
  isGoogleDriveUrl,
  processGoogleDriveDownload,
} from "../../../../utils/googleDriveDownloader.js";

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
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadStatus, setDownloadStatus] = useState("");

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
    // Reset download status khi mở/đóng modal
    setDownloadStatus("");
    setIsDownloading(false);
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const handleOverlayMouseDown = (e) => {
    overlayMouseDownOnBackdropRef.current = e.target === e.currentTarget;
  };

  const handleOverlayMouseUp = (e) => {
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


  /**
   * Xử lý submit form - có tích hợp auto download từ Google Drive
   * Flow mới: Download TRƯỚC, sau đó mới tạo stream với video path thực
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.keyLive) return;

    const rawInput = (form.videoList || "").trim();

    // Nếu rỗng thì lưu như bình thường
    if (!rawInput) {
      onSave({ ...form, videoList: "" });
      return;
    }

    // Kiểm tra có phải là Google Drive URL không
    if (isGoogleDriveUrl(rawInput)) {
      // ---- FLOW: Google Drive URL ----
      // Download TRƯỚC, sau đó mới tạo/cập nhật stream với video path thực
      setIsDownloading(true);
      setDownloadStatus("Đang chuẩn bị download...");

      try {
        // 1. Bắt đầu download từ Google Drive TRƯỚC
        setDownloadStatus("Đang download video từ Google Drive...");
        const downloadResult = await processGoogleDriveDownload(rawInput);

        if (downloadResult.success) {
          // 2. Download thành công - tạo/cập nhật stream với video path thực
          const videoPath = `${VIDEO_BASE_DIR}${downloadResult.fileName}`;
          setDownloadStatus(`Download thành công: ${downloadResult.fileName}. Đang lưu luồng...`);

          // 3. Cập nhật form để hiển thị video path trong input
          setForm((prev) => ({ ...prev, videoList: videoPath }));

          // 4. Tạo formData với video path thực (QUAN TRỌNG: dùng videoPath trực tiếp, không phải form.videoList)
          const formData = {
            note: form.note,
            keyLive: form.keyLive,
            videoList: videoPath, // <-- Video path thực, không phải URL Drive
            startTime: form.startTime,
            startDate: form.startDate,
            duration: form.duration,
          };

          console.log("Saving stream with formData:", formData); // Debug log

          // 5. Await onSave để đảm bảo API call hoàn thành
          await onSave(formData);

          const actionText = initialData ? "cập nhật" : "tạo";
          alert(`Download thành công!\nFile: ${downloadResult.fileName}\nLuồng đã được ${actionText} với video: ${videoPath}`);
        } else {
          setDownloadStatus(`Lỗi download: ${downloadResult.message}`);
          alert(`Download thất bại: ${downloadResult.message}\nLuồng không được lưu.`);
        }
      } catch (error) {
        console.error("Error during Google Drive download:", error);
        setDownloadStatus(`Lỗi: ${error.message}`);
        alert(`Có lỗi xảy ra: ${error.message}`);
      } finally {
        setIsDownloading(false);
      }

      return;
    }

    // ---- FLOW: Tên video thông thường (không phải Google Drive) ----
    // Nếu đã là full path thì giữ nguyên
    const isFullPath = /^[a-zA-Z]:\\/.test(rawInput);
    if (isFullPath) {
      onSave({ ...form, videoList: rawInput });
      return;
    }

    // Thêm .mp4 nếu thiếu
    const filename = rawInput.toLowerCase().endsWith(".mp4") ? rawInput : `${rawInput}.mp4`;

    // Ghép thành D:\videos\filename
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

          <button className="modal__close" onClick={onClose} disabled={isDownloading}>
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
              disabled={isDownloading}
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
              disabled={isDownloading}
            />
          </div>

          <div className="modal__field">
            <label>Tên video / Link Google Drive</label>
            <input
              type="text"
              name="videoList"
              value={form.videoList}
              onChange={handleChange}
              placeholder='Tên video (vd: "video1.mp4") hoặc URL Google Drive'
              disabled={isDownloading}
            />
            {/* <small className="modal__hint">
              💡 Nếu nhập link Google Drive, video sẽ tự động được tải về server với tên tự động.
            </small> */}
          </div>

          <div className="modal__field">
            <label>Thời gian bắt đầu</label>
            <div className="modal__row-inline">
              <input
                type="time"
                name="startTime"
                value={form.startTime}
                onChange={handleChange}
                disabled={isDownloading}
              />
              <input
                type="date"
                name="startDate"
                value={form.startDate}
                onChange={handleChange}
                disabled={isDownloading}
              />
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
              disabled={isDownloading}
            />
          </div>

          {/* Hiển thị trạng thái download */}
          {downloadStatus && (
            <div className={`modal__download-status ${isDownloading ? "modal__download-status--loading" : ""}`}>
              {isDownloading && <span className="modal__spinner"></span>}
              {downloadStatus}
            </div>
          )}

          <div className="modal__footer">
            <button
              type="submit"
              className="btn btn--primary modal__submit"
              disabled={isDownloading}
            >
              {isDownloading ? "Đang xử lý..." : "Lưu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddStream;
