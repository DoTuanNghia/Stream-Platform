// src/api/admin/streamSession.api.js
import axios from "axios";

const BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const adminStreamSessionApi = {
  getAll: (params) =>
    axios.get(`${BASE}/api/stream-sessions/admin/all`, { params }),
};
