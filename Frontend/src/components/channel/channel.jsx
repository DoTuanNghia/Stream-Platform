// src/components/channel/channel.jsx
import React, { useState } from "react";
import "./channel.scss";
import AddChannel from "./addChannel/addChannel.jsx";


const initialChannels = [
  { id: 1, name: "Đạt", channelId: "UCZsc6EuGxCw5poecnsAbC3A", username: "user1" },
  { id: 2, name: "Nghĩa Lớn", channelId: "UCMecpcZoYkZdimRyEH2Wjvg", username: "user2" },
  { id: 3, name: "Uy", channelId: "UCbxQDNVsCYPBRvGQ4cLxZfw", username: "user3" },
];

const Channel = () => {
  const [channels, setChannels] = useState(initialChannels);
  const [isAddOpen, setIsAddOpen] = useState(false);

  const openAddModal = () => setIsAddOpen(true);
  const closeAddModal = () => setIsAddOpen(false);

  const handleAddChannel = (data) => {
    const newChannel = {
      id: Date.now(),
      username: data.username,
      channelId: data.channelId,
      name: data.name,
    };
    setChannels((prev) => [...prev, newChannel]);
    closeAddModal();
  };

  return (
    <>
      <section className="card">
        <div className="card__header">
          <h2 className="card__title">Danh sách kênh</h2>
          <button className="btn btn--primary" onClick={openAddModal}>
            Thêm kênh
          </button>
        </div>

        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Kênh</th>
                <th>ID kênh</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {channels.map((ch, index) => (
                <tr key={ch.id}>
                  <td>{index + 1}</td>
                  <td>{ch.name}</td>
                  <td className="table__mono">{ch.channelId}</td>
                  <td>
                    <div className="table__actions">
                      <button className="btn btn--danger">Xóa</button>
                      <button className="btn btn--ghost">Chọn</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <AddChannel
        isOpen={isAddOpen}
        onClose={closeAddModal}
        onSave={handleAddChannel}
      />
    </>
  );
};

export default Channel;
