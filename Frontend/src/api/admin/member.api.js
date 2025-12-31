import axiosClient from "@/api/axiosClient";

export const memberApi = {
  getAll: () => axiosClient.get("/members"), // GET /api/members
  create: (payload) => axiosClient.post("/members", payload), // nếu backend chưa có thì bạn sẽ cần bổ sung
  update: (id, payload) => axiosClient.put(`/members/${id}`, payload),
  remove: (id) => axiosClient.delete(`/members/${id}`),
};
