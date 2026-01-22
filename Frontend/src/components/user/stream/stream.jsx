// src/components/stream/stream.jsx
import React, { useEffect, useMemo, useRef, useState } from "react";
import "./stream.scss";
import AddStream from "./addStream/addStream.jsx";
import AddStreamsBulk from "./addStream/addStreamsBulk.jsx";
import axiosClient from "../../../api/axiosClient.js";

const formatTime = (iso) => {
  if (!iso) return "none";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "none";
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const hour = String(d.getHours()).padStart(2, "0");
  const minute = String(d.getMinutes()).padStart(2, "0");
  return `${day}/${month} ${hour}:${minute}`;
};

const getCurrentUserId = () => {
  try {
    const raw = localStorage.getItem("currentUser") || sessionStorage.getItem("currentUser");
    const user = JSON.parse(raw || "null");
    return user?.id ?? user?.userId ?? null;
  } catch {
    return null;
  }
};

// ✅ Natural sort: Demo 2 < Demo 10
const collator = new Intl.Collator("vi", {
  numeric: true,
  sensitivity: "base",
});

const Stream = () => {
  const [streams, setStreams] = useState([]);
  const [streamStatusMap, setStreamStatusMap] = useState({});
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isBulkOpen, setIsBulkOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [editingStream, setEditingStream] = useState(null);
  const [totalElements, setTotalElements] = useState(0);

  const wrapperRef = useRef(null);
  const userId = useMemo(() => getCurrentUserId(), []);

  const fetchStatusForList = async (list) => {
    const ids = list.map((s) => s?.id).filter(Boolean);
    if (ids.length === 0) return {};

    const statusRes = await axiosClient.get(`/stream-sessions/status-map`, {
      params: { streamIds: ids.join(",") },
    });

    const rawMap = statusRes.statusMap || {};
    const normalized = {};
    list.forEach((st) => {
      const sid = st?.id;
      if (!sid) return;

      const fromBe = rawMap[sid];
      if (fromBe) normalized[sid] = String(fromBe).toUpperCase();
      else if (st?.timeStart) normalized[sid] = "SCHEDULED";
      else normalized[sid] = "NONE";
    });

    return normalized;
  };

  const fetchAll = async () => {
    if (!userId) {
      setError("Không xác định được userId (chưa đăng nhập?).");
      return;
    }

    try {
      setLoading(true);
      setError("");

      // ✅ gọi endpoint all (không page/size)
      const streamRes = await axiosClient.get(`/streams/all`, {
        params: { userId, sort: "name,asc" }, // BE sort chuỗi, FE sẽ sort lại natural
      });

      const list = streamRes.streams || [];

      // ✅ Natural sort ở FE theo name
      const sorted = [...list].sort((a, b) => collator.compare(a?.name || "", b?.name || ""));

      setStreams(sorted);
      setTotalElements(Number(streamRes.totalElements ?? sorted.length) || sorted.length);

      const mapNew = await fetchStatusForList(sorted);
      setStreamStatusMap(mapNew);

      // scroll về top (khi refresh)
      if (wrapperRef.current) wrapperRef.current.scrollTop = 0;
    } catch (err) {
      console.error(err);
      setStreams([]);
      setStreamStatusMap({});
      setTotalElements(0);
      setError("Không tải được danh sách luồng.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSaveStream = async (form) => {
    try {
      const payload = {
        name: form.note || "Stream mới",
        keyStream: form.keyLive,
        videoList: form.videoList && form.videoList.trim() !== "" ? form.videoList : null,
        timeStart: form.startDate && form.startTime ? `${form.startDate}T${form.startTime}:00` : null,
        duration:
          form.duration !== undefined && form.duration !== null && String(form.duration).trim() !== ""
            ? Number(form.duration)
            : null,
      };

      if (editingStream) await axiosClient.put(`/streams/${editingStream.id}`, payload);
      else await axiosClient.post(`/streams`, payload, { params: { userId } });

      setIsAddOpen(false);
      setEditingStream(null);
      await fetchAll();
    } catch (err) {
      console.error(err);
      alert(editingStream ? "Sửa luồng thất bại." : "Tạo luồng thất bại.");
    }
  };

  const handleSaveStreamsBulk = async (items) => {
    if (!userId) {
      alert("Không xác định được userId (chưa đăng nhập?).");
      return;
    }
    try {
      const payload = { items };
      await axiosClient.post(`/streams/bulk`, payload, { params: { userId } });
      setIsBulkOpen(false);
      await fetchAll();
    } catch (err) {
      console.error(err);
      const msg = err?.response?.data?.message || "Tạo nhiều luồng thất bại.";
      alert(msg);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Xóa luồng này?")) return;
    try {
      await axiosClient.delete(`/streams/${id}`);
      await fetchAll();
    } catch (err) {
      console.error(err);
      alert("Xóa luồng thất bại.");
    }
  };

  const handleStreamNow = async (stream) => {
    const stt = String(streamStatusMap[stream.id] || "NONE").toUpperCase();
    if (stt === "ACTIVE") {
      alert("Luồng đang chạy (ACTIVE), không thể Stream Ngay.");
      return;
    }

    if (!window.confirm(`Stream ngay luồng: "${stream.name}"?`)) return;

    try {
      const res = await axiosClient.post(`/stream-sessions/start/${stream.id}`);
      alert(res.message || "Đã bắt đầu stream.");
      await fetchAll();
    } catch (err) {
      console.error(err);
      const msg = err?.response?.data?.message || "Không thể bắt đầu stream. Vui lòng thử lại.";
      alert(msg);
    }
  };

  const openAddModal = () => {
    setEditingStream(null);
    setIsAddOpen(true);
  };
  const openBulkModal = () => {
    setIsBulkOpen(true);
  };
  const openEditModal = (st) => {
    setEditingStream(st);
    setIsAddOpen(true);
  };
  const closeAddModal = () => {
    setIsAddOpen(false);
    setEditingStream(null);
  };
  const closeBulkModal = () => {
    setIsBulkOpen(false);
  };

  return (
    <>
      <section className="card card--fill">
        <div className="card__header card__header--stack">
          <h2 className="card__title">Danh sách luồng của bạn</h2>

          <div className="card__headerRow">
            {!loading && totalElements > 0 && (
              <span className="header__total">
                Tổng: <strong>{totalElements}</strong> luồng
              </span>
            )}

            <button className="btn btn--primary btn--lg" onClick={openAddModal}>
              Thêm Luồng
            </button>
            <button className="btn btn--ghost btn--lg" onClick={openBulkModal}>
              Thêm nhiều luồng
            </button>
          </div>
        </div>

        {error && <p className="card__subtitle card__subtitle--error">{error}</p>}

        <div className="table-wrapper table-wrapper--vscroll" ref={wrapperRef}>
          <table className="table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Tên luồng</th>
                <th>Hẹn giờ</th>
                <th>Thời gian stream</th>
                <th>Key live</th>
                <th>Trạng thái</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7}>Đang tải...</td>
                </tr>
              ) : streams.length === 0 ? (
                <tr>
                  <td colSpan={7}>Không có luồng nào.</td>
                </tr>
              ) : (
                streams.map((st, index) => {
                  const status = String(streamStatusMap[st.id] || "NONE").toUpperCase();

                  // ✅ Disable Stream Ngay khi ACTIVE hoặc STOPPED
                  const blocked = status === "ACTIVE" || status === "STOPPED";

                  return (
                    <tr key={st.id}>
                      <td>{index + 1}</td>
                      <td>{st.name}</td>
                      <td>{formatTime(st.timeStart)}</td>
                      <td>{st.duration === -1 ? "∞" : st.duration != null ? `${st.duration} phút` : "none"}</td>
                      <td className="table__mono">{st.keyStream}</td>
                      <td>{status}</td>
                      <td>
                        <div className="table__actions">
                          <button className="btn btn--danger" onClick={() => handleDelete(st.id)}>
                            Xóa
                          </button>
                          <button className="btn btn--ghost" onClick={() => openEditModal(st)}>
                            Sửa
                          </button>

                          <button
                            className={`btn btn--success ${blocked ? "btn--disabled" : ""}`}
                            onClick={() => {
                              if (blocked) return; 
                              handleStreamNow(st);
                            }}
                            aria-disabled={blocked}
                            title={
                              status === "ACTIVE"
                                ? "Luồng đang ACTIVE, không thể Stream Ngay"
                                : status === "STOPPED"
                                  ? "Luồng đã STOPPED, hãy bấm Sửa để chuyển lại SCHEDULED rồi mới Stream"
                                  : status === "SCHEDULED"
                                    ? "Luồng đang SCHEDULED, có thể Stream Ngay"
                                    : ""
                            }
                          >
                            Stream Ngay
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>

      <AddStream isOpen={isAddOpen} onClose={closeAddModal} onSave={handleSaveStream} initialData={editingStream} />
      <AddStreamsBulk isOpen={isBulkOpen} onClose={closeBulkModal} onSave={handleSaveStreamsBulk} />
    </>
  );
};

export default Stream;
