import { useEffect, useCallback, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { useFilterStore } from "../stores/useFilterStore";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { createNewRule, getAllRules, getRuleById, updateRule, deleteRule } from "../api/alertApi";

const ALERT_RULES_QUERY_KEY = ["alertRules"];

/**
 * Signature: export function useAlerts(onAlert?)
 * @param {function} [onAlert] - Callback invoked với alert object mỗi khi có message mới qua WS.
 *                                Optional — nếu không truyền, hook vẫn giữ kết nối WS chạy nền
 *                                (phục vụ cho việc tái sử dụng AlertCard sau này) nhưng không làm gì với data.
 */
export function useAlerts(onAlert = () => { }) {
    const { environment, serviceName } = useFilterStore();
    const queryClient = useQueryClient();

    const onAlertRef = useRef(onAlert);
    useEffect(() => {
        onAlertRef.current = onAlert ?? (() => { });
    }, [onAlert]);

    const handleAlert = useCallback((data) => {
        onAlertRef.current(data);
    }, []);

    // ── WebSocket: nhận alert real-time qua STOMP topic /topic/alerts ──────────
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

        let topic = "/topic/alerts";

        let subscription = null;

        const client = new Client({
            brokerURL: wsUrl,
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => {
                subscription = client.subscribe(topic, (message) => {
                    try {
                        const data = JSON.parse(message.body);
                        handleAlert(data);
                        console.log("[useAlerts] Received alert:", data);
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

        return () => {
            if (subscription) {
                subscription.unsubscribe();
            }
            client.deactivate();
        };
    }, [handleAlert]);

    const invalidateRules = () =>
        queryClient.invalidateQueries({ queryKey: ALERT_RULES_QUERY_KEY });

    // ── Query: danh sách Alert Rules (GET /api/v1/alerts/rules) ────────────────
    const rulesQuery = useQuery({
        queryKey: ALERT_RULES_QUERY_KEY,
        queryFn: getAllRules,
    });

    // ── Mutation: tạo mới Alert Rule ────────────────────────────────────────────
    const createRuleMutation = useMutation({
        mutationFn: createNewRule,
        onSuccess: invalidateRules,
        onError: (err) => console.error("[useAlerts] Failed to create rule:", err),
    });

    // ── Mutation: cập nhật 1 phần Alert Rule (PATCH) ────────────────────────────
    const updateRuleMutation = useMutation({
        mutationFn: ({ ruleId, data }) => updateRule(ruleId, data),
        onSuccess: invalidateRules,
        onError: (err) => console.error("[useAlerts] Failed to update rule:", err),
    });

    // ── Mutation: xoá Alert Rule ─────────────────────────────────────────────────
    const deleteRuleMutation = useMutation({
        mutationFn: deleteRule,
        onSuccess: invalidateRules,
        onError: (err) => console.error("[useAlerts] Failed to delete rule:", err),
    });

    return {
        // rules list
        rules: rulesQuery.data ?? [],
        isLoadingRules: rulesQuery.isLoading,
        isRulesError: rulesQuery.isError,
        rulesError: rulesQuery.error,
        refetchRules: rulesQuery.refetch,

        // create
        createRule: createRuleMutation.mutate,
        createRuleAsync: createRuleMutation.mutateAsync,
        isCreatingRule: createRuleMutation.isPending,
        createRuleError: createRuleMutation.error,

        // update — gọi: updateRuleAsync({ ruleId, data: { isActive: false } })
        updateRule: updateRuleMutation.mutate,
        updateRuleAsync: updateRuleMutation.mutateAsync,
        isUpdatingRule: updateRuleMutation.isPending,
        updateRuleError: updateRuleMutation.error,

        // delete — gọi: deleteRuleAsync(ruleId)
        deleteRule: deleteRuleMutation.mutate,
        deleteRuleAsync: deleteRuleMutation.mutateAsync,
        isDeletingRule: deleteRuleMutation.isPending,
        deleteRuleError: deleteRuleMutation.error,
    };
}

/**
 * Hook phụ: lấy chi tiết 1 rule theo ID (dùng cho trang Edit Rule sau này).
 */
export function useAlertRule(ruleId) {
    return useQuery({
        queryKey: ["alertRules", ruleId],
        queryFn: () => getRuleById(ruleId),
        enabled: !!ruleId,
    });
}