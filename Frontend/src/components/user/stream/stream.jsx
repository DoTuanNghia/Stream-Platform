// src/components/stream/stream.jsx
import React, { useEffect, useState } from "react";
import "./stream.scss";
import AddStream from "./addStream/addStream.jsx";
import axiosClient from "../../../api/axiosClient.js";

const PAGE_SIZE = 10;

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

  // Pagination (UI: 1-based)
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const fetchStreams = async (pageNumber = page) => {
    if (!channel?.id) {
      setStreams([]);
      setStreamStatusMap({});
      setPage(1);
      setTotalPages(1);
      setTotalElements(0);
      return;
    }

    setLoading(true);
    setError("");

    try {
      const bePage = Math.max(0, pageNumber - 1);

      // 1) Fetch streams paging
      const streamRes = await axiosClient.get(`/streams/channel/${channel.id}`, {
        params: { page: bePage, size: PAGE_SIZE, sort: "timeStart,desc" },
      });

      const list = streamRes.streams || [];
      const tp = Number(streamRes.totalPages ?? 1) || 1;
      const te = Number(streamRes.totalElements ?? 0) || 0;

      const safePageNumber = Math.min(Math.max(1, pageNumber), tp);

      setStreams(list);
      setTotalPages(tp);
      setTotalElements(te);
      setPage(safePageNumber);

      // 2) Fetch status map theo streamIds của trang hiện tại
      const ids = list.map((s) => s?.id).filter(Boolean);
      if (ids.length === 0) {
        setStreamStatusMap({});
        return;
      }

      const idsParam = ids.join(",");
      const statusRes = await axiosClient.get(`/stream-sessions/status-map`, {
        params: { streamIds: idsParam },
      });

      const rawMap = statusRes.statusMap || {};
      const normalized = {};

      // Normalize + fallback theo yêu cầu:
      // - nếu có trong map => dùng
      // - nếu không có nhưng timeStart != null => SCHEDULED
      // - else => NONE
      list.forEach((st) => {
        const sid = st?.id;
        if (!sid) return;

        const fromBe = rawMap[sid];
        if (fromBe) {
          normalized[sid] = String(fromBe).toUpperCase();
        } else if (st?.timeStart) {
          normalized[sid] = "SCHEDULED";
        } else {
          normalized[sid] = "NONE";
        }
      });

      setStreamStatusMap(normalized);
    } catch (err) {
      console.error(err);
      if (err.response?.status === 404) {
        setStreams([]);
        setStreamStatusMap({});
        setTotalPages(1);
        setTotalElements(0);
        setError("Kênh này chưa có luồng nào.");
      } else {
        setError("Không tải được danh sách luồng.");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setPage(1);
    fetchStreams(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [channel?.id]);

  const gotoPage = async (p) => {
    const next = Math.min(Math.max(1, p), totalPages);
    if (next === page) return;
    await fetchStreams(next);
  };

  const handleSaveStream = async (form) => {
    if (!channel?.id) return;

    try {
      const payload = {
        name: form.note || "Stream mới",
        keyStream: form.keyLive,
        videoList: form.videoList && form.videoList.trim() !== "" ? form.videoList : null,
      };

      if (form.startDate && form.startTime) {
        payload.timeStart = `${form.startDate}T${form.startTime}:00`;
      } else {
        payload.timeStart = null;
      }

      if (form.duration !== undefined && form.duration !== null && String(form.duration).trim() !== "") {
        payload.duration = Number(form.duration);
      } else {
        payload.duration = null;
      }

      if (editingStream) {
        await axiosClient.put(`/streams/${editingStream.id}`, payload);
      } else {
        await axiosClient.post(`/streams/channel/${channel.id}`, payload);
      }

      await fetchStreams(page);
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
      await fetchStreams(page);
    } catch (err) {
      console.error(err);
      alert("Xóa luồng thất bại.");
    }
  };

  const handleStreamNow = async (stream) => {
    const stt = String(streamStatusMap[stream.id] || "NONE").toUpperCase();

    // Theo yêu cầu: ACTIVE và STOPPED không được Stream Ngay
    if (stt === "ACTIVE" || stt === "STOPPED") {
      alert("Luồng không thể Stream Ngay.");
      return;
    }

    if (!window.confirm(`Stream ngay luồng: "${stream.name}"?`)) return;

    try {
      const res = await axiosClient.post(`/stream-sessions/start/${stream.id}`);
      alert(res.message || `Đã bắt đầu stream trên máy ${res.deviceName || res.deviceId}.`);

      await fetchStreams(page);
    } catch (err) {
      console.error(err);
      const msg = err?.response?.data?.message || "Không thể bắt đầu stream. Vui lòng thử lại.";
      alert(msg);
    }
  };

  if (!channel) {
    return (
      <section className="card">
        <div className="card__header">
          <h2 className="card__title">Danh sách luồng của kênh đang chọn</h2>
        </div>
        <p className="card__subtitle">Hãy chọn một kênh ở mục “Danh sách kênh”.</p>
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
              Kênh: <strong>{channel.name}</strong> (<span className="table__mono">{channel.channelCode}</span>)
            </p>
          </div>
          <button className="btn btn--primary" onClick={openAddModal}>
            Thêm Luồng
          </button>
        </div>

        {error && <p className="card__subtitle card__subtitle--error">{error}</p>}

        {!loading && totalElements > 0 && (
          <div className="table__toolbar">
            <div className="table__pagination">
              <span className="table__pageinfo">
                Tổng: <strong>{totalElements}</strong> luồng
              </span>
            </div>
          </div>
        )}

        <div className="table-wrapper">
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
                  const blocked = status === "ACTIVE" || status === "STOPPED";

                  return (
                    <tr key={st.id}>
                      <td>{(page - 1) * PAGE_SIZE + index + 1}</td>
                      <td>{st.name}</td>
                      <td>{formatTime(st.timeStart)}</td>
                      <td>
                        {st.duration === -1 ? "∞" : st.duration != null ? `${st.duration} phút` : "none"}
                      </td>
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
                            onClick={() => handleStreamNow(st)}   // vẫn gọi bình thường
                            aria-disabled={blocked}
                            title={
                              blocked
                                ? status === "ACTIVE"
                                  ? "Luồng đang ACTIVE, không thể Stream Ngay"
                                  : "Luồng đã STOPPED, không thể Stream Ngay"
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

        {!loading && totalElements > 0 && (
          <div className="table__toolbar table__toolbar--bottom">
            <div className="table__pagination">
              <button className="btn btn--ghost" onClick={() => gotoPage(1)} disabled={page <= 1}>
                First
              </button>
              <button className="btn btn--ghost" onClick={() => gotoPage(page - 1)} disabled={page <= 1}>
                Prev
              </button>
              <span className="table__pageinfo">
                <strong>{page}</strong> / {totalPages}
              </span>
              <button className="btn btn--ghost" onClick={() => gotoPage(page + 1)} disabled={page >= totalPages}>
                Next
              </button>
              <button className="btn btn--ghost" onClick={() => gotoPage(totalPages)} disabled={page >= totalPages}>
                Last
              </button>
            </div>
          </div>
        )}
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
