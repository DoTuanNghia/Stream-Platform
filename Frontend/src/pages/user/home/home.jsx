// src/pages/user/home/home.jsx
import React, { useState } from "react";
import Header from "../../../components/user/header/header";
import Sidebar from "../../../components/user/sidebar/sidebar";
import Device from "../../../components/user/device/device";
import Channel from "../../../components/user/channel/channel";
import Stream from "../../../components/user/stream/stream";
import StreamSession from "../../../components/user/streamSession/streamSession";

import "./home.scss";

const Home = ({ onLogout }) => {
  const [activeMenu, setActiveMenu] = useState("device");
  const [selectedChannel, setSelectedChannel] = useState(null);

  const renderContent = () => {
    switch (activeMenu) {
      case "device":
        return <Device />;
      case "channel":
        return (
          <Channel
            selectedChannel={selectedChannel}
            onSelectChannel={(ch) => {
              setSelectedChannel(ch);
              setActiveMenu("stream");
            }}
          />
        );
      case "stream":
        return <Stream channel={selectedChannel} />;
      case "streamSession":
        return <StreamSession />;
      default:
        return <Device />;
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
