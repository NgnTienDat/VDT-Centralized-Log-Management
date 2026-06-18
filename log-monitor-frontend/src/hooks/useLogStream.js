import { useEffect } from "react";
import { Client } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";
import { useFilterStore } from "../stores/useFilterStore";

/**
 * Custom hook to manage real-time log streaming via STOMP WebSockets.
 *
 * Signature: export function useLogStream(liveMode, isFetching)
 */
export function useLogStream(liveMode, isFetching) {
    const queryClient = useQueryClient();
    const { environment, logLevel, serviceName, appName, q } = useFilterStore();

    useEffect(() => {
        // 1. If liveMode is false OR isFetching is true, safely exit early.
        // The return/cleanup function of the previous render's useEffect will deactivate the client.
        if (!liveMode || isFetching) {
            return;
        }

        console.log("Starting STOMP client connection...");

        const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

        let wsUrl = "";

        if (apiBaseUrl) {
            // 1. Môi trường Local: Chuyển http://localhost:8082 thành ws://localhost:8082/ws
            wsUrl = apiBaseUrl.replace(/^http/, "ws") + "/ws";
        } else {
            // 2. Môi trường Production: Tự động lấy IP/Domain và Protocol hiện tại của trình duyệt
            const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
            const host = window.location.host; // Bao gồm cả port nếu có (ví dụ: 13.250.xx.xx)

            // Lúc này wsUrl trên Prod sẽ tự động là: ws://<IP_EC2>/ws
            wsUrl = `${protocol}//${host}/ws`;
        }


        // 2. Smart Topic Routing Matrix
        const env = environment?.toLowerCase();
        const service = serviceName?.toLowerCase();
        const level = logLevel?.toLowerCase();

        let topic = "/topic/logs.all";
        if (env && service && level) {
            topic = `/topic/logs.${env}.${service}.${level}`;
        } else if (env && service && !level) {
            topic = `/topic/logs.${env}.${service}.all`;
        }

        console.log(`Computed target STOMP topic: ${topic}`);

        let subscription = null;

        const client = new Client({
            brokerURL: wsUrl,
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: () => {
                console.log(`STOMP connected! Subscribing to topic: ${topic}`);
                subscription = client.subscribe(topic, (message) => {
                    try {
                        const rawLog = JSON.parse(message.body);

                        // 3. Client-Side Fallback Matching (Hybrid Filtering)
                        const envMatch = !environment || rawLog.environment?.toUpperCase() === environment.toUpperCase();
                        const levelMatch = !logLevel || rawLog.logLevel?.toUpperCase() === logLevel.toUpperCase();
                        const serviceMatch = !serviceName || rawLog.serviceName?.toLowerCase() === serviceName.toLowerCase();
                        const appMatch = !appName || rawLog.appName?.toLowerCase() === appName.toLowerCase();

                        let qMatch = true;
                        if (q) {
                            const searchStr = q.toLowerCase();
                            const msg = rawLog.logMessage ? rawLog.logMessage.toLowerCase() : "";
                            qMatch = msg.includes(searchStr);
                        }

                        if (envMatch && levelMatch && serviceMatch && appMatch && qMatch) {
                            // 4. Data Structuring & Mapping
                            // Thay vì lấy rawLog.id luôn bị null, ưu tiên lấy rawLog.docId từ Logstash
                            const mappedLog = {
                                id: rawLog.docId || rawLog.id,
                                docId: rawLog.docId,
                                timestamp: rawLog.eventTimestamp,
                                level: rawLog.logLevel?.toUpperCase() || "INFO",
                                env: rawLog.environment?.toUpperCase() || "DEV",
                                service: rawLog.serviceName,
                                message: rawLog.logMessage,
                                thread: rawLog.thread,
                                traceId: rawLog.traceId,
                                appName: rawLog.appName,
                                hostName: rawLog.hostName,
                                logger: rawLog.logger,
                                stackTrace: rawLog.stackTrace,
                            };

                            // 5. TanStack Query Cache Manipulation
                            queryClient.setQueryData(
                                ["logs", { environment, logLevel, serviceName, appName, q }],
                                (oldData) => {
                                    if (!oldData) return oldData;

                                    // Prevent duplicates (Sử dụng mappedLog.id mới cập nhật để check trùng)
                                    const isDuplicate = oldData.pages.some((page) =>
                                        page?.data?.some((item) => item.id === mappedLog.id)
                                    );
                                    if (isDuplicate) return oldData;

                                    const updatedPages = [...oldData.pages];
                                    if (updatedPages.length > 0) {
                                        updatedPages[0] = {
                                            ...updatedPages[0],
                                            data: [mappedLog, ...updatedPages[0].data]
                                        };
                                    }
                                    return {
                                        ...oldData,
                                        pages: updatedPages
                                    };
                                }
                            );
                        }
                    } catch (err) {
                        console.error("Failed to parse STOMP message body:", err);
                    }
                });
            },
            onStompError: (frame) => {
                console.error("STOMP broker reported error: ", frame.headers["message"]);
                console.error("STOMP error details: ", frame.body);
            }
        });

        client.activate();

        // 6. Connection Cleanup
        return () => {
            console.log("Cleaning up STOMP client subscription and connection...");
            if (subscription) {
                subscription.unsubscribe();
            }
            client.deactivate();
        };
    }, [liveMode, isFetching, environment, serviceName, logLevel, appName, q, queryClient]);
}