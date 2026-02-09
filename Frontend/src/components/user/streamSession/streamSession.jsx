// src/components/streamSession/streamSession.jsx
import React, { useEffect, useMemo, useRef, useState } from "react";
import "./streamSession.scss";
import axiosClient from "../../../api/axiosClient";

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

const calcEndTime = (baseIso, duration) => {
  if (!baseIso || duration == null) return "n/a";
  if (duration === -1) return "∞";
  const d = new Date(baseIso);
  if (Number.isNaN(d.getTime())) return "n/a";
  d.setMinutes(d.getMinutes() + Number(duration));
  return formatDateTime(d.toISOString());
};

const getDisplayStart = (session) => {
  const stream = session?.stream || {};
  const status = (session?.status || "").toUpperCase();
  if (status === "ACTIVE") return session?.startedAt || stream?.timeStart || null;
  return stream?.timeStart || null;
};

const getDisplayEnd = (session) => {
  const stream = session?.stream || {};
  const status = (session?.status || "").toUpperCase();
  const duration = stream?.duration;
  if (status === "ACTIVE") return calcEndTime(session?.startedAt || stream?.timeStart, duration);
  return calcEndTime(stream?.timeStart, duration);
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

const StreamSession = () => {
  const [sessions, setSessions] = useState([]);
  const [ffStats, setFfStats] = useState({});
  const [loading, setLoading] = useState(false);

  const wrapperRef = useRef(null);

  const buildStatText = (stat) => {
    if (!stat) return "-";
    const fps = stat.fps != null ? Number(stat.fps).toFixed(1) : "-";
    const bitrate = stat.bitrate ?? "-";
    const speed = stat.speed ?? "-";
    return `fps=${fps} bitrate=${bitrate} speed=${speed}`;
  };

  const fetchSessions = async () => {
    setLoading(true);
    try {
      const userId = getCurrentUserId();

      const data = await axiosClient.get("/stream-sessions/list", {
        params: { userId, sort: "id,desc" }, // BE trả gì cũng được, FE sẽ sort lại
      });

      const list = data.streamSessions || [];

      // ✅ Chỉ lọc các session có trạng thái ACTIVE (đang livestream)
      const activeOnly = list.filter(
        (s) => String(s?.status || "").toUpperCase() === "ACTIVE"
      );

      // ✅ Natural sort theo stream.name
      const sorted = [...activeOnly].sort((a, b) =>
        collator.compare(a?.stream?.name || "", b?.stream?.name || "")
      );

      setSessions(sorted);

      // lấy stat cho ACTIVE
      const activeKeys = sorted
        .filter((s) => String(s.status || "").toUpperCase() === "ACTIVE")
        .map((s) => (s.stream?.keyStream || "").trim())
        .filter(Boolean);

      if (activeKeys.length === 0) {
        setFfStats({});
        return;
      }

      const entries = await Promise.all(
        activeKeys.map(async (k) => {
          try {
            const r = await axiosClient.get(`/stream-sessions/ffmpeg-stat/${encodeURIComponent(k)}`);
            return [k, r.stat || null];
          } catch {
            return [k, null];
          }
        })
      );

      const map = {};
      entries.forEach(([k, v]) => (map[k] = v));
      setFfStats(map);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessions();

    const timer = setInterval(() => {
      fetchSessions();
    }, 30000); // 30 giây - đồng bộ với backend stats update

    return () => clearInterval(timer);
  }, []);

  const handleStop = async (id) => {
    if (!window.confirm("Ngừng Stream ngay?")) return;
    try {
      await axiosClient.post(`/stream-sessions/${id}`);
      await fetchSessions();
    } catch (err) {
      console.error(err);
      alert("Ngừng Stream thất bại.");
    }
  };

  const totalText = useMemo(() => {
    if (!sessions.length) return "";
    return `Tổng ${sessions.length} sessions`;
  }, [sessions.length]);

  return (
    <section className="card card--fill">
      <div className="card__header">
        <div className="card__headerLeft">
          <h2 className="card__title">Luồng đang hoạt động</h2>

          <div className="card__headerRow">
            <span className="header__total">
              {totalText ? (
                totalText
              ) : (
                <>
                  Reload ngay hoặc tự động sau <strong>30</strong> giây
                </>
              )}
            </span>

            <button className="btn btn--ghost btn--lg" onClick={fetchSessions}>
              Reload ngay
            </button>
          </div>
        </div>
      </div>

      {/* ✅ scroll chỉ trong vùng này */}
      <div className="table-wrapper table-wrapper--vscroll" ref={wrapperRef}>
        <table className="table table--small">
          <thead>
            <tr>
              <th>STT</th>
              <th>Tên luồng</th>
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
                <td colSpan={8}>Đang tải...</td>
              </tr>
            ) : sessions.length === 0 ? (
              <tr>
                <td colSpan={8}>Không có session nào.</td>
              </tr>
            ) : (
              sessions.map((s, index) => {
                const stream = s.stream || {};
                const note = stream.name || "-";

                const startIso = getDisplayStart(s);
                const start = formatDateTime(startIso);
                const end = getDisplayEnd(s);

                const keyLive = stream.keyStream || "-";
                const status = s.status || "-";
                const upperStatus = String(status).toUpperCase();

                const stats =
                  upperStatus === "ACTIVE" ? buildStatText(ffStats[keyLive]) : s.specification || "-";

                const canStop = upperStatus === "ACTIVE";

                return (
                  <tr key={s.id}>
                    <td>{index + 1}</td>
                    <td>{note}</td>
                    <td>{start}</td>
                    <td>{end}</td>
                    <td>{stats}</td>
                    <td className="table__mono">{keyLive}</td>
                    <td>{status}</td>
                    <td>
                      <button
                        className={`btn ${canStop ? "btn--danger" : "btn--ghost"}`}
                        onClick={() => handleStop(s.id)}
                        disabled={!canStop}
                        title={!canStop ? "Chỉ dừng được khi session đang ACTIVE" : ""}
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
