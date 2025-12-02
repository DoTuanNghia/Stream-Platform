// src/components/stream/stream.jsx
import React, { useState } from "react";
import "./stream.scss";
import AddStream from "./addStream/addStream.jsx";

const initialStreams = [
  {
    id: 1,
    note: "Oraciones amor a Católica 14",
    schedule: "none",
    streamTime: "none",
    keyLive: "rgvx-ht0e-3srv-q0ft-7yxs",
  },
  {
    id: 2,
    note: "Las Enseñanzas De Dios 03",
    schedule: "none",
    streamTime: "none",
    keyLive: "zz4s-twtj-rewy-z6f0-6dge",
  },
];

const CURRENT_CHANNEL_ID = "UCZsc6EuGxCw5poecnsAbC3A"; // fake để hiển thị trên popup

const Stream = () => {
  const [streams, setStreams] = useState(initialStreams);
  const [isAddOpen, setIsAddOpen] = useState(false);

  const openAddModal = () => setIsAddOpen(true);
  const closeAddModal = () => setIsAddOpen(false);

  const handleAddStream = (data) => {
    const newStream = {
      id: Date.now(),
      note: data.note,
      schedule: `${data.startTime} ${data.startDate}`,
      streamTime: `${data.duration} phút`,
      keyLive: data.keyLive,
    };
    setStreams((prev) => [...prev, newStream]);
    closeAddModal();
  };

  return (
    <>
      <section className="card">
        <div className="card__header">
          <h2 className="card__title">Danh sách luồng của kênh đang chọn</h2>
          <button className="btn btn--primary" onClick={openAddModal}>
            Thêm Luồng
          </button>
        </div>

        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Ghi chú</th>
                <th>Hẹn giờ</th>
                <th>Thời gian stream</th>
                <th>Key live</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {streams.map((st, index) => (
                <tr key={st.id}>
                  <td>{index + 1}</td>
                  <td>{st.note}</td>
                  <td>{st.schedule}</td>
                  <td>{st.streamTime}</td>
                  <td className="table__mono">{st.keyLive}</td>
                  <td>
                    <div className="table__actions">
                      <button className="btn btn--danger">Xóa</button>
                      <button className="btn btn--ghost">Sửa</button>
                      <button className="btn btn--success">Stream Ngay</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <AddStream
        isOpen={isAddOpen}
        onClose={closeAddModal}
        onSave={handleAddStream}
        channelId={CURRENT_CHANNEL_ID}
      />
    </>
  );
};

export default Stream;
