// src/pages/user/home/home.jsx
import React, { useState } from "react";
import Header from "../../../components/user/header/header";
import Sidebar from "../../../components/user/sidebar/sidebar";
import Stream from "../../../components/user/stream/stream";
import StreamSession from "../../../components/user/streamSession/streamSession";

import "./home.scss";

const Home = ({ onLogout }) => {
  const [activeMenu, setActiveMenu] = useState("stream"); // mặc định vào Stream

  const renderContent = () => {
    switch (activeMenu) {
      case "stream":
        return <Stream />;
      case "streamSession":
        return <StreamSession />;
      default:
        return <Stream />;
    }
  };

  return (
    <div className="layout">
      <Header onLogout={onLogout} />
      <div className="layout__body">
        <Sidebar activeMenu={activeMenu} onChangeMenu={setActiveMenu} />
        <main className="layout__content">{renderContent()}</main>
      </div>
    </div>
  );
};

export default Home;
