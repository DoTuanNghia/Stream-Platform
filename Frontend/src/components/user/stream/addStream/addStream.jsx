import React, { useEffect, useRef, useState } from "react";
import "./addStream.scss";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import {
  isDownloadableInput,
  processDownload,
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

const CustomDateInput = React.forwardRef(({ value, onChange, onClick, onKeyDown, onBlur, placeholder, disabled, className }, ref) => (
  <div style={{ display: "flex", flex: 1, position: "relative", width: "100%" }}>
    <input
      ref={ref}
      value={value}
      onChange={onChange}
      onKeyDown={onKeyDown}
      onBlur={onBlur}
      placeholder={placeholder}
      disabled={disabled}
      className={className || "modal__datepicker-input"}
      style={{ paddingRight: "34px", width: "100%", boxSizing: "border-box" }}
    />
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title="Chọn ngày từ lịch"
      style={{
        position: "absolute",
        right: "6px",
        top: "50%",
        transform: "translateY(-50%)",
        background: "none",
        border: "none",
        cursor: disabled ? "not-allowed" : "pointer",
        color: "#9ca3af",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "4px"
      }}
    >
      <svg fill="currentColor" viewBox="0 0 24 24" width="18" height="18">
        <path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2zm-7 5h5v5h-5z"/>
      </svg>
    </button>
  </div>
));

const AddStream = ({ isOpen, onClose, onSave, initialData }) => {
  const [form, setForm] = useState(emptyForm);
  const [timeDigits, setTimeDigits] = useState(""); // HHMM: "2122"
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadStatus, setDownloadStatus] = useState("");

  const overlayMouseDownOnBackdropRef = useRef(false);
  const timeInputRef = useRef(null);

  const to24hDisplay = (digits) => {
    const d = (digits || "").replace(/\D/g, "").slice(0, 4);
    const hhStr = d.slice(0, 2);
    const mmStr = d.slice(2, 4);

    if (hhStr.length < 2) return hhStr; // "2" / "21"

    const hh24 = Number(hhStr);
    if (Number.isNaN(hh24) || hh24 > 23) return "";

    const mmDisplay =
      mmStr.length === 0 ? "--" :
        mmStr.length === 1 ? `${mmStr}-` :
          mmStr;

    return `${hhStr}:${mmDisplay}`;
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
      let initDate = "";
      let initTime24 = "";
      let initDigits = "";
      if (time && !Number.isNaN(time.getTime())) {
        const yyyy = time.getFullYear();
        const mm = String(time.getMonth() + 1).padStart(2, "0");
        const dd = String(time.getDate()).padStart(2, "0");
        initDate = `${yyyy}-${mm}-${dd}`;
        initTime24 = time.toTimeString().slice(0, 5); // "HH:mm"
        initDigits = initTime24.replace(":", "");
      }

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

  // Đang sửa luồng ACTIVE → chỉ cho sửa duration
  const isLiveEdit = initialData && String(initialData._liveStatus || "").toUpperCase() === "ACTIVE";

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
      setDownloadStatus("Thời gian bắt đầu chưa hợp lệ. Ví dụ gõ 2122 => 21:22.");
      return;
    }

    // Bỏ ngoặc kép và tách theo dòng
    const rawLines = (form.videoList || "")
      .split(/\r?\n/)
      .map(line => line.trim().replace(/^"|"$/g, "").trim())
      .filter(line => line.length > 0);

    if (rawLines.length === 0) {
      await onSave({ ...form, videoList: "" });
      onClose();
      return;
    }

    setIsDownloading(true);
    setDownloadStatus("Đang kiểm tra danh sách video...");

    const finalPaths = [];
    let hasError = false;

    for (let i = 0; i < rawLines.length; i++) {
        const line = rawLines[i];
        
        if (isDownloadableInput(line)) {
            setDownloadStatus(`Đang tải video ${i + 1}/${rawLines.length}...`);
            try {
                const downloadResult = await processDownload(line);
                if (downloadResult.success) {
                    const videoPath = `${VIDEO_BASE_DIR}${downloadResult.fileName}`;
                    finalPaths.push(videoPath);
                } else {
                    setDownloadStatus(`Lỗi tải video thứ ${i + 1}!`);
                    hasError = true;
                    break;
                }
            } catch (error) {
                console.error("Error during download:", error);
                setDownloadStatus(`Có lỗi xảy ra ở video ${i + 1}: ${error.message}`);
                hasError = true;
                break;
            }
        } else {
            // Tên video thường hoặc path NAS
            const isFullPath = /^[a-zA-Z]:\\/.test(line) || line.startsWith("\\\\") || line.startsWith("/");
            if (isFullPath) {
                finalPaths.push(line);
            } else {
                const filename = line.toLowerCase().endsWith(".mp4") ? line : `${line}.mp4`;
                finalPaths.push(`${VIDEO_BASE_DIR}${filename}`);
            }
        }
    }

    if (hasError) {
        setIsDownloading(false);
        return; // Dừng lại không lưu nếu có lỗi
    }

    setDownloadStatus("Xử lý thành công, đang lưu...");
    
    // Gộp tất cả đường dẫn bằng \n
    const finalVideoList = finalPaths.join("\n");
    setForm((prev) => ({ ...prev, videoList: finalVideoList }));

    const formData = {
      note: form.note,
      keyLive: form.keyLive,
      videoList: finalVideoList,
      startTime: form.startTime, // "HH:mm"
      startDate: form.startDate,
      duration: form.duration,
    };

    await onSave(formData);
    
    setDownloadStatus("");
    setIsDownloading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onMouseDown={handleOverlayMouseDown} onMouseUp={handleOverlayMouseUp}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()} onMouseUp={(e) => e.stopPropagation()}>
        <div className="modal__header">
          <span className="modal__title">{isLiveEdit ? "Sửa như ban đầu (đang Live)" : initialData ? "Sửa Stream" : "Tạo Stream"}</span>
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
              disabled={isDownloading || isLiveEdit}
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
              disabled={isDownloading || isLiveEdit}
            />
          </div>

          <div className="modal__field">
            <label>Danh sách Video</label>
            <textarea
              name="videoList"
              value={form.videoList}
              onChange={handleChange}
              placeholder='Nhập tên video, URL Google Drive, hoặc đường dẫn NAS (vd: \\NAS\folder\file.mp4).&#10;Mỗi link/đường dẫn một dòng để phát tuần tự.'
              disabled={isDownloading}
              rows={4}
              style={{
                width: "100%",
                padding: "10px",
                border: "1px solid #d1d5db",
                borderRadius: "6px",
                fontSize: "14px",
                resize: "vertical"
              }}
            />
          </div>

          <div className="modal__field">
            <label>Thời gian bắt đầu</label>
            <div className="modal__row-inline">
              <input
                ref={timeInputRef}
                type="text"
                name="startTime"
                value={to24hDisplay(timeDigits)}
                onKeyDown={handleTimeKeyDown}
                onPaste={handleTimePaste}
                onChange={() => { /* noop: xử lý bằng keydown */ }}
                onBlur={handleTimeBlur}
                placeholder="--:--"
                inputMode="numeric"
                autoComplete="off"
                disabled={isDownloading || isLiveEdit}
                title="Gõ 4 số HHMM. Ví dụ: 2122 => 21:22."
              />

              <DatePicker
                selected={form.startDate && !Number.isNaN(new Date(form.startDate).getTime()) ? new Date(form.startDate) : null}
                onChange={(date) => {
                  if (!date) {
                    setForm((prev) => ({ ...prev, startDate: "" }));
                    return;
                  }
                  if (date instanceof Date && !Number.isNaN(date.getTime())) {
                    const yyyy = date.getFullYear();
                    const mm = String(date.getMonth() + 1).padStart(2, "0");
                    const dd = String(date.getDate()).padStart(2, "0");
                    setForm((prev) => ({ ...prev, startDate: `${yyyy}-${mm}-${dd}` }));
                  }
                }}
                dateFormat="dd/MM/yyyy"
                placeholderText="dd/mm/yyyy"
                disabled={isDownloading || isLiveEdit}
                className="modal__datepicker-input"
                customInput={<CustomDateInput />}
                preventOpenOnFocus
                strictParsing
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
