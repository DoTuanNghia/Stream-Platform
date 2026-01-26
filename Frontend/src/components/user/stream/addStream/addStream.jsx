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
  startTime: "", // 24h "HH:mm" để submit
  startDate: "",
  streamAfter: 0,
  duration: 0,
};

const AddStream = ({ isOpen, onClose, onSave, initialData }) => {
  const [form, setForm] = useState(emptyForm);
  const [timeDigits, setTimeDigits] = useState(""); // HHMM: "2122"
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadStatus, setDownloadStatus] = useState("");

  const overlayMouseDownOnBackdropRef = useRef(false);
  const timeInputRef = useRef(null);

  const to12hDisplay = (digits) => {
    const d = (digits || "").replace(/\D/g, "").slice(0, 4);
    const hhStr = d.slice(0, 2);
    const mmStr = d.slice(2, 4);

    if (hhStr.length < 2) return hhStr; // "2" / "21"

    const hh24 = Number(hhStr);
    if (Number.isNaN(hh24) || hh24 > 23) return "";

    const ampm = hh24 >= 12 ? "PM" : "AM";
    let hh12 = hh24 % 12;
    if (hh12 === 0) hh12 = 12;

    const hh12Str = String(hh12).padStart(2, "0");
    const mmDisplay =
      mmStr.length === 0 ? "--" :
        mmStr.length === 1 ? `${mmStr}-` :
          mmStr;

    return `${hh12Str}:${mmDisplay} ${ampm}`;
  };

  const digitsTo24h = (digits) => {
    const d = (digits || "").replace(/\D/g, "").slice(0, 4);
    if (d.length !== 4) return "";
    const hh = Number(d.slice(0, 2));
    const mm = Number(d.slice(2, 4));
    if (Number.isNaN(hh) || Number.isNaN(mm)) return "";
    if (hh > 23 || mm > 59) return "";
    return `${d.slice(0, 2)}:${d.slice(2, 4)}`;
  };

  const syncDigits = (nextDigits) => {
    const d = (nextDigits || "").replace(/\D/g, "").slice(0, 4);

    // chặn giờ nếu đủ 2 số
    if (d.length >= 2) {
      const hh = Number(d.slice(0, 2));
      if (Number.isNaN(hh) || hh > 23) return;
    }

    // chặn phút nếu đủ 4 số
    if (d.length === 4) {
      const mm = Number(d.slice(2, 4));
      if (Number.isNaN(mm) || mm > 59) return;
    }

    setTimeDigits(d);

    const t24 = digitsTo24h(d);
    setForm((prev) => ({
      ...prev,
      startTime: t24, // chỉ có khi đủ 4 số hợp lệ
    }));
  };

  useEffect(() => {
    if (!isOpen) return;

    if (initialData) {
      const time = initialData.timeStart ? new Date(initialData.timeStart) : null;
      const initDate = time ? time.toISOString().slice(0, 10) : "";
      const initTime24 = time ? time.toTimeString().slice(0, 5) : ""; // "HH:mm"
      const initDigits = initTime24 ? initTime24.replace(":", "") : "";

      setForm({
        note: initialData.name || "",
        keyLive: initialData.keyStream || "",
        videoList: initialData.videoList || "",
        fullHd: 0,
        startDate: initDate,
        startTime: initTime24,
        streamAfter: 0,
        duration:
          initialData.duration !== null && initialData.duration !== undefined
            ? initialData.duration
            : 0,
      });

      setTimeDigits(initDigits);
    } else {
      setForm(emptyForm);
      setTimeDigits("");
    }

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

  // ✅ Quan trọng: không đọc e.target.value nữa (vì value đang có "PM/AM")
  // Ta bắt phím để chỉnh timeDigits
  const handleTimeKeyDown = (e) => {
    const k = e.key;

    // cho phép tab
    if (k === "Tab") return;

    // backspace/delete
    if (k === "Backspace" || k === "Delete") {
      e.preventDefault();
      syncDigits(timeDigits.slice(0, -1));
      return;
    }

    // chỉ nhận số
    if (/^\d$/.test(k)) {
      e.preventDefault();
      syncDigits((timeDigits + k).slice(0, 4));
      return;
    }

    // cho phép phím điều hướng
    if (k === "ArrowLeft" || k === "ArrowRight" || k === "Home" || k === "End") {
      e.preventDefault();
      return;
    }

    // chặn mọi thứ khác
    e.preventDefault();
  };

  const handleTimePaste = (e) => {
    e.preventDefault();
    const text = (e.clipboardData?.getData("text") || "").trim();
    const digits = text.replace(/\D/g, "").slice(0, 4);
    if (!digits) return;
    syncDigits(digits);
  };

  const handleTimeBlur = () => {
    // Nếu user bỏ dở (chưa đủ 4 số) thì clear để tránh submit rác
    if (timeDigits && timeDigits.length < 4) {
      setTimeDigits("");
      setForm((prev) => ({ ...prev, startTime: "" }));
    }
  };

  const VIDEO_BASE_DIR = "D:\\videos\\";

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.keyLive) return;

    // nếu user đã gõ timeDigits mà chưa đủ/không hợp lệ
    if (timeDigits && !form.startTime) {
      setDownloadStatus("Thời gian bắt đầu chưa hợp lệ. Ví dụ gõ 2122 => 09:22 PM (tức 21:22).");
      return;
    }

    const rawInput = (form.videoList || "").trim();

    if (!rawInput) {
      await onSave({ ...form, videoList: "" });
      onClose();
      return;
    }

    if (isGoogleDriveUrl(rawInput)) {
      setIsDownloading(true);
      setDownloadStatus("Đang chuẩn bị download...");

      try {
        setDownloadStatus("Đang download video từ Google Drive...");
        const downloadResult = await processGoogleDriveDownload(rawInput);

        if (downloadResult.success) {
          const videoPath = `${VIDEO_BASE_DIR}${downloadResult.fileName}`;
          setForm((prev) => ({ ...prev, videoList: videoPath }));

          const formData = {
            note: form.note,
            keyLive: form.keyLive,
            videoList: videoPath,
            startTime: form.startTime, // "HH:mm"
            startDate: form.startDate,
            duration: form.duration,
          };

          await onSave(formData);

          setDownloadStatus("Download thành công!");
          setIsDownloading(false);
          setTimeout(() => {
            setDownloadStatus("");
            onClose();
          }, 8000);
          return;
        } else {
          setDownloadStatus("Lỗi download!");
          setIsDownloading(false);
          return;
        }
      } catch (error) {
        console.error("Error during Google Drive download:", error);
        setDownloadStatus(`Có lỗi xảy ra: ${error.message}`);
        setIsDownloading(false);
        return;
      }
    }

    // Tên video thường
    const isFullPath = /^[a-zA-Z]:\\/.test(rawInput);
    if (isFullPath) {
      await onSave({ ...form, videoList: rawInput });
      onClose();
      return;
    }

    const filename = rawInput.toLowerCase().endsWith(".mp4") ? rawInput : `${rawInput}.mp4`;
    const fullPath = `${VIDEO_BASE_DIR}${filename}`;

    await onSave({
      ...form,
      videoList: fullPath,
    });
    onClose();
  };

  return (
    <div className="modal-overlay" onMouseDown={handleOverlayMouseDown} onMouseUp={handleOverlayMouseUp}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()} onMouseUp={(e) => e.stopPropagation()}>
        <div className="modal__header">
          <span className="modal__title">{initialData ? "Sửa Stream" : "Tạo Stream"}</span>
          <button className="modal__close" onClick={onClose} disabled={isDownloading}>×</button>
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
          </div>

          <div className="modal__field">
            <label>Thời gian bắt đầu</label>
            <div className="modal__row-inline">
              <input
                ref={timeInputRef}
                type="text"
                name="startTime"
                value={to12hDisplay(timeDigits)}
                onKeyDown={handleTimeKeyDown}
                onPaste={handleTimePaste}
                onChange={() => { /* noop: xử lý bằng keydown */ }}
                onBlur={handleTimeBlur}
                placeholder="--:--"
                inputMode="numeric"
                autoComplete="off"
                disabled={isDownloading}
                title="Gõ 4 số HHMM. Ví dụ: 2122 => 09:22 PM (tức 21:22)."
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

          {downloadStatus && (
            <div className={`modal__download-status ${isDownloading ? "modal__download-status--loading" : ""}`}>
              {isDownloading && <span className="modal__spinner"></span>}
              {downloadStatus}
            </div>
          )}

          <div className="modal__footer">
            <button type="submit" className="btn btn--primary modal__submit" disabled={isDownloading}>
              {isDownloading ? "Đang xử lý..." : "Lưu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddStream;
