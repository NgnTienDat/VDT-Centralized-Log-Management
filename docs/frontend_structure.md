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



src/
│
├── api/                          ← tất cả HTTP call, tách khỏi component
│   ├── logApi.ts                 ← GET /api/logs, GET /api/logs/:id
│   ├── alertApi.ts               ← GET /api/alerts/active, POST /api/alerts/:id/dismiss
│   ├── statsApi.ts               ← GET /api/stats/summary
│   └── axiosClient.ts            ← axios instance, baseURL, interceptor
│
├── hooks/                        ← custom hooks — logic không phải UI
│   ├── useLogQuery.ts            ← fetch + cursor pagination + debounce search
│   ├── useLogStream.ts           ← STOMP WebSocket, subscribe/unsubscribe topic
│   ├── useAlerts.ts              ← load active alerts, dismiss, nhận WS push
│   └── useStats.ts               ← fetch summary stats, polling 30s
│
├── components/
│   ├── layout/
│   │   ├── AppHeader.tsx         ← logo, live toggle, theme toggle, alert badge
│   │   └── AppFooter.tsx         ← env summary, version info
│   │
│   ├── log/
│   │   ├── LogTable.tsx          ← container: header + scrollable rows + load more
│   │   ├── LogRow.tsx            ← 1 dòng log trong bảng
│   │   ├── LogDetail.tsx         ← panel chi tiết khi click vào row
│   │   └── LogTableSkeleton.tsx  ← loading state khi đang fetch
│   │
│   ├── filter/
│   │   ├── FilterBar.tsx         ← container 2 row filter
│   │   ├── LevelFilter.tsx       ← các nút ALL/INFO/WARN/ERROR/DEBUG
│   │   ├── EnvFilter.tsx         ← các nút ALL/DEV/STAGING/TEST/PROD
│   │   ├── ServiceFilter.tsx     ← dropdown chọn service
│   │   └── SearchInput.tsx       ← input có debounce, trigger server search
│   │
│   ├── stats/
│   │   ├── StatsRow.tsx          ← 4 card: total, error, warn, error chart
│   │   ├── StatCard.tsx          ← 1 card số liệu
│   │   └── MiniChart.tsx         ← bar chart error rate 20m
│   │
│   └── alert/
│       └── AlertBanner.tsx       ← danh sách alert có thể dismiss
│
├── store/                        ← global state — dùng Zustand (nhẹ, đủ dùng)
│   ├── useFilterStore.ts         ← filterLevel, filterEnv, filterService, search
│   ├── useLogStore.ts            ← displayLogs, rawBuffer, hasMore, nextCursor
│   └── useThemeStore.ts          ← isDark, toggle
│
├── types/
│   ├── log.types.ts              ← LogEntry, LogDetail, CursorPage<T>
│   ├── alert.types.ts            ← Alert
│   └── stats.types.ts            ← StatsSummary, ErrorTimeSeries
│
├── constants/
│   └── theme.ts                  ← THEMES object (dark/light tokens), levelConfig, envConfig
│
├── utils/
│   ├── formatTs.ts               ← format timestamp → HH:mm:ss.SSS
│   └── topicResolver.ts          ← tính STOMP topic từ env + level
│
└── pages/
    └── Dashboard.tsx             ← ghép tất cả lại, không có logic riêng