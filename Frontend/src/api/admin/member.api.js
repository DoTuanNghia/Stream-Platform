import axiosClient from "@/api/axiosClient";

export const memberApi = {
  getAll: () => axiosClient.get("/members"),

  create: (payload) => axiosClient.post("/members", payload),

  update: (id, payload) =>
    axiosClient.put(`/members/${id}`, payload),

  remove: (id) =>
    axiosClient.delete(`/members/${id}`),
};
