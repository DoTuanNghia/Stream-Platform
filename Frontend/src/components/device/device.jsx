import React, { useEffect, useState } from "react";
import "./device.scss";
import axiosClient from "../../api/axiosClient";

const Device = () => {
  const [devices, setDevices] = useState([]);

  useEffect(() => {
    const fetchDevices = async () => {
      try {
        const data = await axiosClient.get("/devices");

        // Chỉ giữ device có currentSession > 0
        const filtered = (data || []).filter(
          d => (d.currentSession ?? 0) > 0
        );

        setDevices(filtered);
      } catch (err) {
        console.error(err);
        setDevices([]);
      }
    };

    fetchDevices();
  }, []);

  return (
    <section className="card">
      <div className="card__header">
        <h2 className="card__title">Danh sách máy đang hoạt động</h2>
        <span className="card__subtitle">
          Tổng số: <strong>{devices.length}</strong> máy
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
            {devices.length === 0 ? (
              <tr>
                <td colSpan={5}>Không có máy nào.</td>
              </tr>
            ) : (
              devices.map((device, index) => (
                <tr key={device.id}>
                  <td>{index + 1}</td>
                  <td>{device.name}</td>
                  <td>{device.lastActive || "-"}</td>
                  <td>{device.currentSession}</td>
                  <td>
                    <button className="btn btn--danger">Xóa</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
};

export default Device;
