// src/components/streamSession/streamSession.jsx
import React from "react";
import "./streamSession.scss";

const mockSessions = [
  {
    id: 1,
    note: "DC 2",
    device: "R16RK4U17-3",
    start: "02/12 13:00",
    end: "02/12 16:00",
    stats: "25 | 3200.9kbits/s | 1x",
    keyLive: "5eh1-1g7-s9hj-s9o9-fjax",
    status: "Đang live SANGDIOS02",
  },
  {
    id: 2,
    note: "kenh 5 - 2",
    device: "R16RK4U17-3",
    start: "02/12 07:30",
    end: "n/a",
    stats: "30 | 12590.2kbits/s | 1x",
    keyLive: "sfb1-mtxu-hyrw-bbq1-8zg2",
    status: "Đang live kênh 05 33111",
  },
];

const StreamSession = () => {
  return (
    <section className="card">
      <div className="card__header">
        <div>
          <h2 className="card__title">Luồng đang hoạt động</h2>
          <p className="card__subtitle">
            Reload ngay hoặc tự động sau <strong>7</strong> giây
          </p>
        </div>
        <button className="btn btn--ghost">Reload ngay</button>
      </div>

      <div className="table-wrapper">
        <table className="table table--small">
          <thead>
            <tr>
              <th>STT</th>
              <th>Ghi chú</th>
              <th>Máy đang live</th>
              <th>Start</th>
              <th>End dự kiến</th>
              <th>Thông số</th>
              <th>Key live</th>
              <th>Trạng thái</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {mockSessions.map((s, index) => (
              <tr key={s.id}>
                <td>{index + 1}</td>
                <td>{s.note}</td>
                <td>{s.device}</td>
                <td>{s.start}</td>
                <td>{s.end}</td>
                <td>{s.stats}</td>
                <td className="table__mono">{s.keyLive}</td>
                <td>{s.status}</td>
                <td>
                  <button className="btn btn--danger">Ngừng Stream Ngay</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
};

export default StreamSession;
