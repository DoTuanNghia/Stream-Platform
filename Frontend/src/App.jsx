// src/App.jsx
import { Routes, Route, Link } from "react-router-dom";

import Home from "./pages/home/home.jsx";
import Login from "./pages/login/Login.jsx";

export default function App() {
  return (
    <div style={{ padding: 20 }}>

      {/* Khu vực render page */}
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
      </Routes>
    </div>
  );
}


