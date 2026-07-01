import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getNotificationsByRule } from "../api/alertApi";

export const alertNotificationsKey = (ruleId) => ["alertNotifications", ruleId];

export function useAlertNotifications(ruleId) {
    return useQuery({
        queryKey: alertNotificationsKey(ruleId),
        queryFn: () => getNotificationsByRule(ruleId),
        enabled: !!ruleId,
        staleTime: 30000,
    });
}

export function usePrependNotification() {
    const queryClient = useQueryClient();

    return (ruleId, newNotification) => {
        queryClient.setQueryData(alertNotificationsKey(ruleId), (old = []) => {
            // Check trùng lặp dựa trên timestamp + ruleId + alertState mới đồng nhất
            const alreadyExists = old.some(
                (n) =>
                    n.timestamp === newNotification.timestamp &&
                    n.ruleId === newNotification.ruleId &&
                    n.alertState === newNotification.alertState
            );

            if (alreadyExists) return old;
            return [newNotification, ...old];
        });
    };
}