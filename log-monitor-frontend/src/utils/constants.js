export const ENVIRONMENTS = ["ALL", "DEV", "STAGING", "TEST", "PROD"];
export const LOG_LEVELS = ["ALL", "INFO", "WARN", "ERROR", "DEBUG"];
export const SERVICES = [
  "ALL",
  "auth-service",
  "chat-service",
  "user-service",
  "payment-service",
  "notification-service",
  "logs-service",
];

export const MOCK_LOGS = [
  { id: 1,  timestamp: "2025-06-05T08:01:12.412Z", level: "ERROR", env: "DEV",     service: "auth-service",         message: "NullPointerException at AuthController.java:142 — user token is null",                           traceId: "a1b2c3d4", thread: "http-nio-8080-exec-3"  },
  { id: 2,  timestamp: "2025-06-05T08:01:13.001Z", level: "ERROR", env: "DEV",     service: "auth-service",         message: "Failed to validate JWT: signature does not match",                                              traceId: "a1b2c3d4", thread: "http-nio-8080-exec-3"  },
  { id: 3,  timestamp: "2025-06-05T08:01:14.880Z", level: "WARN",  env: "STAGING", service: "payment-service",      message: "Payment gateway timeout after 5000ms, retrying... (attempt 2/3)",                               traceId: "f9e8d7c6", thread: "payment-executor-1"    },
  { id: 4,  timestamp: "2025-06-05T08:01:15.321Z", level: "INFO",  env: "DEV",     service: "chat-service",         message: "WebSocket connection established for user #88421",                                              traceId: "b3c4d5e6", thread: "ws-handler-12"         },
  { id: 5,  timestamp: "2025-06-05T08:01:16.001Z", level: "DEBUG", env: "TEST",    service: "user-service",         message: "Cache HIT for key: user:profile:88421, TTL remaining: 287s",                                   traceId: "c5d6e7f8", thread: "redis-pool-2"          },
  { id: 6,  timestamp: "2025-06-05T08:01:17.512Z", level: "ERROR", env: "STAGING", service: "notification-service", message: "SMTP connection refused — host: smtp.internal:587, check firewall rules",                      traceId: "d7e8f9a0", thread: "mail-sender-1"         },
  { id: 7,  timestamp: "2025-06-05T08:01:18.004Z", level: "INFO",  env: "DEV",     service: "user-service",         message: "User #88421 updated profile successfully, 3 fields changed",                                   traceId: "e9f0a1b2", thread: "http-nio-8080-exec-7"  },
  { id: 8,  timestamp: "2025-06-05T08:01:19.200Z", level: "WARN",  env: "DEV",     service: "chat-service",         message: "Message queue depth: 1842 — approaching threshold of 2000",                                    traceId: "f1a2b3c4", thread: "queue-monitor-1"       },
  { id: 9,  timestamp: "2025-06-05T08:01:20.777Z", level: "ERROR", env: "TEST",    service: "payment-service",      message: "Database connection pool exhausted — all 20 connections in use, request queued",               traceId: "a3b4c5d6", thread: "db-pool-monitor"       },
  { id: 10, timestamp: "2025-06-05T08:01:21.333Z", level: "INFO",  env: "STAGING", service: "auth-service",         message: "Session cleanup completed — 142 expired sessions removed",                                     traceId: "b5c6d7e8", thread: "scheduler-1"           },
  { id: 11, timestamp: "2025-06-05T08:01:22.001Z", level: "DEBUG", env: "DEV",     service: "payment-service",      message: "Transaction rollback initiated — reason: constraint violation on orders table",                 traceId: "c7d8e9f0", thread: "tx-manager-3"          },
  { id: 12, timestamp: "2025-06-05T08:01:23.444Z", level: "WARN",  env: "TEST",    service: "auth-service",         message: "Rate limit almost reached for IP 192.168.1.105 — 95/100 requests in 60s window",               traceId: "d9e0f1a2", thread: "rate-limiter-1"        },
  { id: 13, timestamp: "2025-06-05T08:01:24.888Z", level: "ERROR", env: "DEV",     service: "notification-service", message: "Failed to send push notification — FCM responded: InvalidRegistration for token abc123xyz",    traceId: "e1f2a3b4", thread: "push-sender-2"         },
  { id: 14, timestamp: "2025-06-05T08:01:25.100Z", level: "INFO",  env: "DEV",     service: "chat-service",         message: "Room #5512 archived after 30 days of inactivity",                                              traceId: "f3a4b5c6", thread: "archival-job-1"        },
  { id: 15, timestamp: "2025-06-05T08:01:26.200Z", level: "INFO",  env: "STAGING", service: "user-service",         message: "Bulk import completed — 1024 records processed, 3 skipped",                                   traceId: "a5b6c7d8", thread: "import-executor-1"     },
  { id: 16, timestamp: "2025-06-05T08:01:27.600Z", level: "ERROR", env: "STAGING", service: "auth-service",         message: "StackOverflowError in recursive token refresh loop — possible circular dependency",             traceId: "b7c8d9e0", thread: "http-nio-8080-exec-1"  },
  { id: 17, timestamp: "2025-06-05T08:01:28.010Z", level: "WARN",  env: "DEV",     service: "user-service",         message: "Slow query detected: 4821ms for SELECT * FROM audit_logs — missing index on created_at",        traceId: "c9d0e1f2", thread: "sql-monitor-1"         },
  { id: 18, timestamp: "2025-06-05T08:01:29.500Z", level: "DEBUG", env: "STAGING", service: "chat-service",         message: "Reconnect attempt 1/5 for WebSocket session ws_9981",                                          traceId: "d1e2f3a4", thread: "ws-reconnect-pool"     },
  { id: 19, timestamp: "2025-06-05T08:01:30.333Z", level: "ERROR", env: "TEST",    service: "chat-service",         message: "Message delivery failed — recipient user #99012 not found in active sessions",                 traceId: "e3f4a5b6", thread: "msg-dispatcher-4"      },
  { id: 20, timestamp: "2025-06-05T08:01:31.222Z", level: "INFO",  env: "DEV",     service: "payment-service",      message: "Stripe webhook received — event: payment_intent.succeeded, amount: 250000 VND",                traceId: "f5a6b7c8", thread: "webhook-handler-1"     },
];

export const levelConfig = {
  ERROR: { color: "text-red-400",    bg: "bg-red-500/10",    border: "border-red-500/35",    hex: "#ff4d4d" },
  WARN:  { color: "text-amber-400",  bg: "bg-amber-500/10",  border: "border-amber-500/35",  hex: "#f59e0b" },
  INFO:  { color: "text-sky-400",    bg: "bg-sky-500/10",    border: "border-sky-500/30",    hex: "#0ea5e9" },
  DEBUG: { color: "text-slate-400",  bg: "bg-slate-500/10",  border: "border-slate-500/25",  hex: "#8b95b0" },
};

export const envConfig = {
  DEV:     { color: "text-violet-400", bg: "bg-violet-500/10", hex: "#7c3aed" },
  STAGING: { color: "text-emerald-400",bg: "bg-emerald-500/10",hex: "#059669" },
  TEST:    { color: "text-amber-500",  bg: "bg-amber-500/10",  hex: "#d97706" },
  PROD:    { color: "text-pink-500",   bg: "bg-pink-500/10",   hex: "#db2777" },
};

export function formatTs(ts) {
  const d = new Date(ts);
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}:${String(d.getSeconds()).padStart(2, "0")}.${String(d.getMilliseconds()).padStart(3, "0")}`;
}