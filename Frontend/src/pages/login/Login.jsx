import { useState } from "react";
import axiosClient from "../../api/axiosClient";

export default function Login() {
  const [username, setUser] = useState("");
  const [password, setPwd] = useState("");

  const handleLogin = async () => {
    try {
      const res = await axiosClient.post("/auth/login", { username, password });
      localStorage.setItem("token", res.token);
      alert("Login thành công!");
    } catch (err) {
      alert("Sai thông tin đăng nhập!");
    }
  };

  return (
    <div>
      <h1>Đăng nhập</h1>
      <input placeholder="Username" onChange={(e) => setUser(e.target.value)} />
      <input placeholder="Password" type="password" onChange={(e) => setPwd(e.target.value)} />
      <button onClick={handleLogin}>Login</button>
    </div>
  );
}
