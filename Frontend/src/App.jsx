// src/App.jsx
import { Routes, Route, Navigate } from "react-router-dom";

import Home from "./pages/home/home.jsx";
import Login from "./pages/login/login.jsx";

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />

        <Route path="/login" element={<Login />} />
        <Route path="/home" element={<Home />} />
      </Routes>
    </>
  );
}
