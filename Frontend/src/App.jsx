// src/App.jsx
import { Routes, Route, Navigate } from "react-router-dom";

import Home from "./pages/user/home/home.jsx";
import Login from "./pages/auth/login/login.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/home" element={<Home />} />

      {/* default */}
      <Route path="/" element={<Home />} />

      {/* fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
