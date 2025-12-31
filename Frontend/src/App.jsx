import React, { useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";

import AdminRoutes from "./routes/admin.routes";
import Login from "./pages/auth/login/login";
import Home from "./pages/user/home/home";

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(
    !!localStorage.getItem("currentUser") ||
    !!sessionStorage.getItem("currentUser")
  );

  const onLogout = () => {
    localStorage.removeItem("currentUser");
    sessionStorage.removeItem("currentUser");
    setIsLoggedIn(false); // ✅ QUAN TRỌNG
  };

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route
        path="/login"
        element={
          isLoggedIn ? (
            <Navigate to="/home" replace />
          ) : (
            <Login onLoginSuccess={() => setIsLoggedIn(true)} />
          )
        }
      />

      <Route
        path="/home"
        element={
          isLoggedIn ? <Home onLogout={onLogout} /> : <Navigate to="/login" replace />
        }
      />

      {/* admin cũng có thể cần logout */}
      {AdminRoutes({ onLogout })}

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
