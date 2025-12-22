// src/components/streamSession/streamSession.jsx
import React, { useEffect, useMemo, useState } from "react";
import "./streamSession.scss";
import axiosClient from "../../api/axiosClient";

const PAGE_SIZE = 10;

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

const StreamSession = () => {
  const [sessions, setSessions] = useState([]);
  const [ffStats, setFfStats] = useState({}); // { [streamKey]: stat }
  const [loading, setLoading] = useState(false);

  // Pagination (UI 1-based)
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const buildStatText = (stat) => {
    if (!stat) return "-";
    const frame = stat.frame ?? "-";
    const fps = stat.fps ?? "-";
    const q = stat.q ?? "-";
    const size = stat.size ?? "-";
    const bitrate = stat.bitrate ?? "-";
    return `frame=${frame} fps=${fps} q=${q} size=${size} bitrate=${bitrate}`;
  };

  const fetchSessions = async (pageNumber = page) => {
    setLoading(true);
    try {
      const bePage = Math.max(0, pageNumber - 1);

      // Paging backend
      const data = await axiosClient.get("/stream-sessions", {
        params: { page: bePage, size: PAGE_SIZE, sort: "id,desc" },
      });

      const rawList = data.streamSessions || [];

      // Hiển thị: ACTIVE + SCHEDULED (ẩn STOPPED)
      const list = rawList.filter((s) => {
        const st = (s.status || "").toUpperCase();
        return st !== "STOPPED";
      });

      const tp = Number(data.totalPages ?? 1) || 1;
      const te = Number(data.totalElements ?? 0) || 0;

      const safePage = Math.min(Math.max(1, pageNumber), tp);

      setSessions(list);
      setTotalPages(tp);
      setTotalElements(te);
      setPage(safePage);

      // Chỉ lấy stat cho ACTIVE ở trang hiện tại (tối ưu)
      const activeKeys = list
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
            const r = await axiosClient.get(
              `/stream-sessions/ffmpeg-stat/${encodeURIComponent(k)}`
            );
            return [k, r.stat || null];
          } catch (e) {
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
    fetchSessions(1);
    // auto refresh: giữ nguyên trang hiện tại
    const timer = setInterval(() => fetchSessions(page), 7000);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const gotoPage = async (p) => {
    const next = Math.min(Math.max(1, p), totalPages);
    if (next === page) return;
    setPage(next); // trigger useEffect fetch
  };

  const handleStop = async (id) => {
    if (!window.confirm("Ngừng Stream ngay?")) return;
    try {
      await axiosClient.post(`/stream-sessions/${id}`);
      await fetchSessions(page);
    } catch (err) {
      console.error(err);
      alert("Ngừng Stream thất bại.");
    }
  };

  const pageInfoText = useMemo(() => {
    if (!totalElements) return "";
    return `Trang ${page}/${totalPages} — Tổng ${totalElements} sessions`;
  }, [page, totalPages, totalElements]);

  return (
    <section className="card">
      <div className="card__header">
        <div>
          <h2 className="card__title">Luồng đang hoạt động</h2>
          <p className="card__subtitle">
            {pageInfoText || (
              <>
                Reload ngay hoặc tự động sau <strong>7</strong> giây
              </>
            )}
          </p>
        </div>

        <div className="card__headerActions">
          <button className="btn btn--ghost" onClick={() => fetchSessions(page)}>
            Reload ngay
          </button>
        </div>
      </div>

      {/* Pagination */}
      {!loading && totalElements > 0 && (
        <div className="table__toolbar">
          <div className="table__pagination">
            <button
              className="btn btn--ghost"
              onClick={() => gotoPage(1)}
              disabled={page <= 1}
            >
              First
            </button>
            <button
              className="btn btn--ghost"
              onClick={() => gotoPage(page - 1)}
              disabled={page <= 1}
            >
              Prev
            </button>

            <span className="table__pageinfo">
              <strong>{page}</strong> / {totalPages}
            </span>

            <button
              className="btn btn--ghost"
              onClick={() => gotoPage(page + 1)}
              disabled={page >= totalPages}
            >
              Next
            </button>
            <button
              className="btn btn--ghost"
              onClick={() => gotoPage(totalPages)}
              disabled={page >= totalPages}
            >
              Last
            </button>
          </div>
        </div>
      )}

      <div className="table-wrapper">
        <table className="table table--small">
          <thead>
            <tr>
              <th>STT</th>
              <th>Tên luồng</th>
              <th>Máy</th>
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

                const startIso = getDisplayStart(s);
                const start = formatDateTime(startIso);
                const end = getDisplayEnd(s);

                const keyLive = stream.keyStream || "-";
                const status = s.status || "-";
                const upperStatus = String(status).toUpperCase();

                const stats =
                  upperStatus === "ACTIVE"
                    ? buildStatText(ffStats[keyLive])
                    : s.specification || "-";

                const canStop = upperStatus === "ACTIVE";

                return (
                  <tr key={s.id}>
                    {/* STT theo toàn cục */}
                    <td>{(page - 1) * PAGE_SIZE + index + 1}</td>
                    <td>{note}</td>
                    <td>{deviceName}</td>
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
