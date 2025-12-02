// src/components/device/device.jsx
import React from "react";
import "./device.scss";

const mockDevices = [
  {
    id: 1,
    name: "R16RK4U17-3",
    lastActive: "119 phút trước",
    liveCount: 7,
  },
  {
    id: 2,
    name: "R16RK4U17-4",
    lastActive: "3 phút trước",
    liveCount: 2,
  },
];

const Device = () => {
  return (
    <section className="card">
      <div className="card__header">
        <h2 className="card__title">Danh sách máy đang hoạt động</h2>
        <span className="card__subtitle">
          Tổng số: <strong>{mockDevices.length}</strong> máy
        </span>
      </div>

      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Tên</th>
              <th>Lần hoạt động cuối</th>
              <th>Số video đang live</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {mockDevices.map((device, index) => (
              <tr key={device.id}>
                <td>{index + 1}</td>
                <td>{device.name}</td>
                <td>{device.lastActive}</td>
                <td>{device.liveCount}</td>
                <td>
                  <button className="btn btn--danger">Xóa</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
};

export default Device;
