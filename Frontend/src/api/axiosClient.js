// import axios from "axios";

// const axiosClient = axios.create({
//   baseURL: "http://localhost:8080/api",
//   headers: { "Content-Type": "application/json" }
// });

// axiosClient.interceptors.response.use(
//   (response) => response.data,
//   (error) => Promise.reject(error)
// );

// export default axiosClient;


import axios from "axios";

const axiosClient = axios.create({
  // Khi deploy: frontend + backend cùng domain qua Nginx
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Giữ nguyên interceptor
axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
);

export default axiosClient;
