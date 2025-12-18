// src/components/stream/stream.jsx
import React, { useEffect, useState } from "react";
import "./stream.scss";
import AddStream from "./addStream/addStream.jsx";
import axiosClient from "../../api/axiosClient";

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

const Stream = ({ channel }) => {
  const [streams, setStreams] = useState([]);
  const [streamStatusMap, setStreamStatusMap] = useState({}); // { [streamId]: "ACTIVE" | "SCHEDULED" | "STOPPED" }
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [editingStream, setEditingStream] = useState(null);

  const fetchStreams = async () => {
    if (!channel?.id) {
      setStreams([]);
      setStreamStatusMap({});
      return;
    }

    setLoading(true);
    setError("");

    try {
      const [streamRes, sessionRes] = await Promise.all([
        axiosClient.get(`/streams/channel/${channel.id}`),
        axiosClient.get(`/stream-sessions`),
      ]);

      const list = streamRes.streams || [];
      setStreams(list);

      const sessions = sessionRes.streamSessions || [];
      const map = {};
      sessions.forEach((s) => {
        const sid = s?.stream?.id;
        if (!sid) return;
        map[sid] = String(s.status || "").toUpperCase();
      });
      setStreamStatusMap(map);
    } catch (err) {
      console.error(err);
      if (err.response?.status === 404) {
        setStreams([]);
        setStreamStatusMap({});
        setError("Kênh này chưa có luồng nào.");
      } else {
        setError("Không tải được danh sách luồng.");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStreams();
  }, [channel?.id]);

  // Thêm / Sửa stream dùng chung 1 hàm
  const handleSaveStream = async (form) => {
    if (!channel?.id) return;

    try {
      const payload = {
        name: form.note || "Stream mới",
        keyStream: form.keyLive,
        videoList:
          form.videoList && form.videoList.trim() !== ""
            ? form.videoList
            : null,
      };

      if (form.startDate && form.startTime) {
        payload.timeStart = `${form.startDate}T${form.startTime}:00`;
      } else {
        payload.timeStart = null;
      }

      // duration: cho phép -1 (vô hạn). Không dùng if(form.duration) vì -1 là truthy nhưng "" là falsy, cần xử lý rõ
      if (
        form.duration !== undefined &&
        form.duration !== null &&
        String(form.duration).trim() !== ""
      ) {
        payload.duration = Number(form.duration);
      } else {
        payload.duration = null;
      }

      if (editingStream) {
        await axiosClient.put(`/streams/${editingStream.id}`, payload);
      } else {
        await axiosClient.post(`/streams/channel/${channel.id}`, payload);
      }

      await fetchStreams();
      setIsAddOpen(false);
      setEditingStream(null);
    } catch (err) {
      console.error(err);
      alert(editingStream ? "Sửa luồng thất bại." : "Tạo luồng thất bại.");
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Xóa luồng này?")) return;
    try {
      await axiosClient.delete(`/streams/${id}`);
      await fetchStreams();
    } catch (err) {
      console.error(err);
      alert("Xóa luồng thất bại.");
    }
  };

  const handleStreamNow = async (stream) => {
    // chỉ chặn khi ACTIVE (đúng theo yêu cầu)
    const stt = streamStatusMap[stream.id];
    if (stt === "ACTIVE") {
      alert("Luồng đang được stream (ACTIVE), không thể Stream Ngay.");
      return;
    }

    if (!window.confirm(`Stream ngay luồng: "${stream.name}"?`)) return;

    try {
      const res = await axiosClient.post(`/stream-sessions/start/${stream.id}`);
      console.log("Start stream response:", res);

      alert(
        res.message ||
          `Đã bắt đầu stream trên máy ${res.deviceName || res.deviceId}.`
      );

      // refresh để nút bị disable ngay khi chuyển ACTIVE
      await fetchStreams();
    } catch (err) {
      console.error(err);
      const msg =
        err?.response?.data?.message ||
        "Không thể bắt đầu stream. Vui lòng thử lại.";
      alert(msg);
    }
  };

  if (!channel) {
    return (
      <section className="card">
        <div className="card__header">
          <h2 className="card__title">Danh sách luồng của kênh đang chọn</h2>
        </div>
        <p className="card__subtitle">
          Hãy chọn một kênh ở mục “Danh sách kênh”.
        </p>
      </section>
    );
  }

  const openAddModal = () => {
    setEditingStream(null);
    setIsAddOpen(true);
  };

  const openEditModal = (st) => {
    setEditingStream(st);
    setIsAddOpen(true);
  };

  const closeAddModal = () => {
    setIsAddOpen(false);
    setEditingStream(null);
  };

  return (
    <>
      <section className="card">
        <div className="card__header">
          <div>
            <h2 className="card__title">Danh sách luồng của kênh đang chọn</h2>
            <p className="card__subtitle">
              Kênh: <strong>{channel.name}</strong>{" "}
              (<span className="table__mono">{channel.channelCode}</span>)
            </p>
          </div>
          <button className="btn btn--primary" onClick={openAddModal}>
            Thêm Luồng
          </button>
        </div>

        {error && (
          <p className="card__subtitle card__subtitle--error">{error}</p>
        )}

        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Ghi chú</th>
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
                  const status = streamStatusMap[st.id] || "NONE";
                  const isActive = status === "ACTIVE";

                  return (
                    <tr key={st.id}>
                      <td>{index + 1}</td>
                      <td>{st.name}</td>
                      <td>{formatTime(st.timeStart)}</td>
                      <td>
                        {st.duration === -1
                          ? "∞"
                          : st.duration != null
                          ? `${st.duration} phút`
                          : "none"}
                      </td>
                      <td className="table__mono">{st.keyStream}</td>
                      <td>{status}</td>
                      <td>
                        <div className="table__actions">
                          <button
                            className="btn btn--danger"
                            onClick={() => handleDelete(st.id)}
                          >
                            Xóa
                          </button>
                          <button
                            className="btn btn--ghost"
                            onClick={() => openEditModal(st)}
                          >
                            Sửa
                          </button>
                          <button
                            className="btn btn--success"
                            onClick={() => handleStreamNow(st)}
                            disabled={isActive}
                            title={
                              isActive
                                ? "Luồng đang ACTIVE, không thể Stream Ngay"
                                : status === "SCHEDULED"
                                ? "Luồng đang SCHEDULED, vẫn có thể Stream Ngay"
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

      <AddStream
        isOpen={isAddOpen}
        onClose={closeAddModal}
        onSave={handleSaveStream}
        channelId={channel.channelCode}
        initialData={editingStream}
      />
    </>
  );
};

export default Stream;
