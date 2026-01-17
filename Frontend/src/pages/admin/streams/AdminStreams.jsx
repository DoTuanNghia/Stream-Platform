import React, { useEffect, useMemo, useState } from "react";
import "./AdminStreams.scss";
import { adminStreamSessionApi } from "../../../api/admin/streamSession.api";

const TABS = [
  { key: "SCHEDULED", label: "Luồng SCHEDULED" },
  { key: "ACTIVE", label: "Luồng ACTIVE" },
  { key: "STOPPED", label: "Luồng STOPPED" },
];

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatLocalDateTime(v) {
  if (!v) return "-";
  const s = String(v).trim();
  const noMs = s.split(".")[0]; // "yyyy-MM-ddTHH:mm:ss"
  const [datePart, timePart] = noMs.split("T");
  if (!datePart || !timePart) return s;

  const [yyyy, MM, dd] = datePart.split("-");
  const [HH, mm, ss] = timePart.split(":");
  if (!yyyy || !MM || !dd || !HH || !mm || !ss) return s;

  return `${pad2(HH)}:${pad2(mm)}:${pad2(ss)} ${pad2(dd)}/${pad2(MM)}/${yyyy}`;
}

export default function AdminStreams() {
  const [tab, setTab] = useState("ACTIVE");

  // paging
  const [page, setPage] = useState(0);
  const size = 15; // ✅ 15 dòng / trang

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await adminStreamSessionApi.getAll({
        status: tab,
        page,
        size,
        sort: "id,desc",
      });

      const data = res?.data ?? res;
      const list = data?.streamSessions ?? [];

      setRows(Array.isArray(list) ? list : []);
      setTotalElements(Number(data?.totalElements ?? 0));
      setTotalPages(Number(data?.totalPages ?? 0));
    } catch (e) {
      console.error(e);
      setRows([]);
      setTotalElements(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  };

  // đổi tab => reset về trang 0
  useEffect(() => {
    setPage(0);
  }, [tab]);

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, page]);

  const visibleRows = useMemo(() => {
    return rows.map((x) => ({
      ...x,
      status: String(x?.status ?? "").toUpperCase(),
      streamKey: x?.stream?.keyStream ?? "n/a",
      streamName: x?.stream?.name ?? x?.stream?.title ?? "n/a",
      deviceName: x?.device?.name ?? "n/a",
      startedText: formatLocalDateTime(x?.startedAt),
      stoppedText: formatLocalDateTime(x?.stoppedAt),
    }));
  }, [rows]);

  const pageDisplay = totalPages > 0 ? page + 1 : 0;

  const canFirst = page > 0;
  const canPrev = page > 0;
  const canNext = totalPages > 0 && page < totalPages - 1;
  const canLast = totalPages > 0 && page < totalPages - 1;

  const goFirst = () => canFirst && setPage(0);
  const goPrev = () => canPrev && setPage((p) => p - 1);
  const goNext = () => canNext && setPage((p) => p + 1);
  const goLast = () => canLast && setPage(totalPages - 1);

  return (
    <div className="admin-streams">
      <div className="admin-streams__header">
        <div>
          <h2>Thống kê luồng</h2>
          <div className="admin-streams__meta">
            Số lượng: <b>{totalElements}</b>
          </div>
        </div>

        <div className="admin-streams__tabs">
          {TABS.map((t) => (
            <button
              key={t.key}
              className={"tab-btn" + (tab === t.key ? " is-active" : "")}
              onClick={() => setTab(t.key)}
              type="button"
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="admin-streams__card">
        {loading ? (
          <div className="admin-streams__loading">Đang tải...</div>
        ) : (
          <>
            <div className="table-wrapper">
              <table className="table table--small">
                <thead>
                  <tr>
                    <th className="col-stt">STT</th>
                    <th>Stream</th>
                    <th>StreamKey</th>
                    <th>Device</th>
                    <th className="col-status">Status</th>
                    <th className="col-time">Started</th>
                    <th className="col-time">Stopped</th>
                  </tr>
                </thead>

                <tbody>
                  {visibleRows.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="admin-streams__empty">
                        Không có dữ liệu
                      </td>
                    </tr>
                  ) : (
                    visibleRows.map((m, idx) => (
                      <tr key={m.id ?? idx}>
                        {/* ✅ STT đúng theo trang */}
                        <td className="col-stt">{page * size + idx + 1}</td>
                        <td className="admin-streams__name">{m.streamName}</td>
                        <td className="mono">{m.streamKey}</td>
                        <td>{m.deviceName}</td>
                        <td className="col-status">
                          <span className={"badge badge--" + m.status.toLowerCase()}>
                            {m.status}
                          </span>
                        </td>
                        <td className="col-time">{m.startedText}</td>
                        <td className="col-time">{m.stoppedText}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* ✅ Pagination: First / Prev / Next / Last */}
            <div className="admin-streams__pager">
              <button
                className="btn btn--ghost"
                onClick={goFirst}
                disabled={!canFirst}
                type="button"
              >
                First
              </button>

              <button
                className="btn btn--ghost"
                onClick={goPrev}
                disabled={!canPrev}
                type="button"
              >
                Prev
              </button>

              <div className="admin-streams__pagerText">
                Trang <b>{pageDisplay}</b> / <b>{totalPages}</b>
              </div>

              <button
                className="btn btn--ghost"
                onClick={goNext}
                disabled={!canNext}
                type="button"
              >
                Next
              </button>

              <button
                className="btn btn--ghost"
                onClick={goLast}
                disabled={!canLast}
                type="button"
              >
                Last
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
