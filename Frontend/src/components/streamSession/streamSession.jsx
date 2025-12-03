// src/components/streamSession/streamSession.jsx
import React, { useEffect, useState } from "react";
import "./streamSession.scss";
import axiosClient from "../../api/axiosClient";

const formatDateTime = (iso) => {
  if (!iso) return "n/a";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "n/a";

  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const hour = String(d.getHours()).padStart(2, "0");
  const minute = String(d.getMinutes()).padStart(2, "0");
  return `${day}/${month} ${hour}:${minute}`;
};

const calcEndTime = (timeStart, duration) => {
  if (!timeStart || !duration) return "n/a";
  const d = new Date(timeStart);
  if (Number.isNaN(d.getTime())) return "n/a";
  d.setMinutes(d.getMinutes() + duration);
  return formatDateTime(d.toISOString());
};

const StreamSession = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchSessions = async () => {
    setLoading(true);
    try {
      const data = await axiosClient.get("/stream-sessions");
      setSessions(
        (data.streamSessions || []).filter(
          (s) => s.status && s.status.toUpperCase() !== "STOPPED"
        )
      );
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };


  useEffect(() => {
    fetchSessions();
    const timer = setInterval(fetchSessions, 7000);
    return () => clearInterval(timer);
  }, []);

  const handleStop = async (id) => {
    if (!window.confirm("Ngừng Stream ngay?")) return;
    try {
      await axiosClient.post(`/stream-sessions/${id}`);
      fetchSessions();
    } catch (err) {
      console.error(err);
      alert("Ngừng Stream thất bại.");
    }
  };

  return (
    <section className="card">
      <div className="card__header">
        <div>
          <h2 className="card__title">Luồng đang hoạt động</h2>
          <p className="card__subtitle">
            Reload ngay hoặc tự động sau <strong>7</strong> giây
          </p>
        </div>
        <button className="btn btn--ghost" onClick={fetchSessions}>
          Reload ngay
        </button>
      </div>

      <div className="table-wrapper">
        <table className="table table--small">
          <thead>
            <tr>
              <th>STT</th>
              <th>Ghi Chú</th>
              <th>Máy đang live</th>
              <th>Start</th>
              <th>End dự kiến</th>
              <th>Thông số</th>
              <th>Key Live</th>
              <th>Trạng thái</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={9}>Đang tải...</td>
              </tr>
            ) : sessions.length === 0 ? (
              <tr>
                <td colSpan={9}>Không có session nào.</td>
              </tr>
            ) : (
              sessions.map((s, index) => {
                const stream = s.stream || {};
                const device = s.device || {};

                const note = stream.name || "-";
                const deviceName = device.name || "-";
                const start = formatDateTime(stream.timeStart);
                const end = calcEndTime(stream.timeStart, stream.duration);
                const stats = s.specification || "-";
                const keyLive = stream.keyStream || "-";
                const status = s.status || "-";

                return (
                  <tr key={s.id}>
                    <td>{index + 1}</td>
                    <td>{note}</td>
                    <td>{deviceName}</td>
                    <td>{start}</td>
                    <td>{end}</td>
                    <td>{stats}</td>
                    <td className="table__mono">{keyLive}</td>
                    <td>{status}</td>
                    <td>
                      <button
                        className="btn btn--danger"
                        onClick={() => handleStop(s.id)}
                      >
                        Ngừng Stream Ngay
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
};

export default StreamSession;