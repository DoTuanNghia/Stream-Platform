import axios from "axios";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL + "/api",
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: false, // đổi true nếu dùng cookie/session
});

axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
);

export default axiosClient;
