frontend/
└── dashboard/                       ← React hoặc Vue
    ├── src/
    │   ├── components/
    │   │   ├── LogTable/             ← Bảng hiển thị log, lọc level/app
    │   │   ├── LiveStream/           ← WebSocket consumer, real-time feed
    │   │   ├── AlertBanner/          ← Toast/popup khi có ERROR
    │   │   └── Charts/              ← Error rate theo giờ, theo app
    │   ├── services/
    │   │   ├── queryApi.js           ← Gọi query-api REST
    │   │   └── websocket.js          ← Kết nối WebSocket
    │   ├── pages/
    │   │   ├── Dashboard.jsx
    │   │   ├── LiveMonitor.jsx
    │   │   └── Analytics.jsx
    │   └── main.jsx
    ├── Dockerfile
    ├── nginx.conf                    ← Serve static + proxy /api → backend
    └── package.json