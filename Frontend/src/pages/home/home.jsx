// src/pages/home/home.jsx
import React, { useState } from "react";
import Header from "../../components/header/header";
import Sidebar from "../../components/sidebar/sidebar";
import Device from "../../components/device/device";
import Channel from "../../components/channel/channel";
import Stream from "../../components/stream/stream";
import StreamSession from "../../components/streamSession/streamSession";

import "./home.scss";

const Home = () => {
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
      <Header />
      <div className="layout__body">
        <Sidebar activeMenu={activeMenu} onChangeMenu={setActiveMenu} />
        <main className="layout__content">{renderContent()}</main>
      </div>
    </div>
  );
};

export default Home;
