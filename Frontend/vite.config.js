import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "src") },
  },
  css: {
    preprocessorOptions: { scss: { additionalData: "" } },
  },

  server: {
    proxy: {
      // Spring Boot
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },

      // Flask download tool
      "/dlapi": {
        target: "http://localhost:5001",
        changeOrigin: true,
        secure: false,

        // QUAN TRỌNG:
        // Nginx của bạn đang proxy /dlapi/ -> http://127.0.0.1:5001/
        // tức là bỏ prefix /dlapi trước khi sang Flask
        rewrite: (p) => p.replace(/^\/dlapi/, ""),
      },
    },
  },
});
