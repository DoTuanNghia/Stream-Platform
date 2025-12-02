// src/App.jsx
import { Routes, Route } from "react-router-dom";

import Home from "./pages/home/home.jsx";
import Login from "./pages/login/login.jsx";

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
      </Routes>
    </>
  );
}
