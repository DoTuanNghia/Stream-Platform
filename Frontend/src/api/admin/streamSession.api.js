// src/api/admin/streamSession.api.js
import axiosClient from "../axiosClient";

export const adminStreamSessionApi = {
  getAll: (params) =>
    axiosClient.get("/stream-sessions/admin/all", { params }),
  getStats: () => axiosClient.get("/stream-sessions/admin/stats"),
};
