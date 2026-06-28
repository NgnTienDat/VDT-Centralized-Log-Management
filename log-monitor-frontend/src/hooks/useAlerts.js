import { useEffect, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import { useFilterStore } from "../stores/useFilterStore";

/**
 * Custom hook to manage real-time alert streaming via STOMP WebSockets.
 * Auto-connects on mount, re-subscribes when environment/serviceName filter changes.
 *
 * Signature: export function useAlerts(onAlert)
 * @param {function} onAlert - Callback invoked with a parsed alert object on each new message
 */
export function useAlerts(onAlert) {
    const { environment, serviceName } = useFilterStore();

    // Stable callback ref to avoid re-connecting on every render
    const handleAlert = useCallback(onAlert, [onAlert]);

    useEffect(() => {
        const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

        let wsUrl = "";
        if (apiBaseUrl) {
            wsUrl = apiBaseUrl.replace(/^http/, "ws") + "/ws";
        } else {
            const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
            const host = window.location.host;
            wsUrl = `${protocol}//${host}/ws`;
        }

        // Smart topic routing: mirrors useLogStream.js pattern
        const env = environment?.toLowerCase();
        const service = serviceName?.toLowerCase();

        let topic = "/topic/alerts";
        if (env && service) {
            topic = `/topic/alerts.${env}.${service}`;
        } else if (env) {
            topic = `/topic/alerts.${env}`;
        } else if (service) {
            topic = `/topic/alerts.${service}`;
        }

        console.log(`[useAlerts] Connecting to STOMP, topic: ${topic}`);

        let subscription = null;

        const client = new Client({
            brokerURL: wsUrl,
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => {
                console.log(`[useAlerts] STOMP connected. Subscribing to: ${topic}`);
                subscription = client.subscribe(topic, (message) => {
                    try {
                        const data = JSON.parse(message.body);
                        handleAlert(data);
                    } catch (err) {
                        console.error("[useAlerts] Failed to parse alert message:", err);
                    }
                });
            },

            onStompError: (frame) => {
                console.error("[useAlerts] STOMP error:", frame.headers["message"]);
            },

            onWebSocketError: () => {
                console.error("[useAlerts] WebSocket connection error.");
            },
        });

        client.activate();

        // Cleanup: unsubscribe + deactivate when filters change or component unmounts
        return () => {
            console.log(`[useAlerts] Cleaning up subscription to ${topic}`);
            if (subscription) {
                subscription.unsubscribe();
            }
            client.deactivate();
        };
    }, [environment, serviceName, handleAlert]);
}