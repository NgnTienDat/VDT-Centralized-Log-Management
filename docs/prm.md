
**Có nên lưu vào Elasticsearch không?**

**Nên.** Lý do:

Reload mất data là deal-breaker trong môi trường production. Người ops mở trang, refresh browser, không còn biết lần cuối rule này fire lúc nào và vào group nào — đó là thông tin quan trọng nhất.

Thêm nữa, ES đã có sẵn trong stack, thêm 1 index `alert-notifications` tốn thêm không đáng kể. Về sau có thể query theo time range, filter theo ruleId, aggregate tần suất firing — mọi thứ ES đã làm tốt.

---

**Kiến trúc đề xuất:**

```
Backend khi bắn thông báo:
  1. Lưu AlertNotificationPayload → index "alert-notifications" (MỚI)
  2. Gửi WebSocket → /topic/alerts (GIỮ NGUYÊN)

Frontend:
  Mount AlertRuleDetail
    → useQuery["alertNotifications", ruleId]
      → GET /api/v1/alerts/rules/{ruleId}/notifications?ruleId=xxx
      → load lịch sử từ ES
  
  WebSocket nhận event mới
    → queryClient.setQueryData(["alertNotifications", ruleId], (old) => [newEvent, ...old])
    → UI cập nhật realtime, KHÔNG dùng useState nữa
```

Cụ thể những gì cần làm ở từng tầng:

**Backend** (2 việc):
- Tạo `AlertNotificationRepository` (ES index `alert-notifications`)
- Trong `AlertSchedulerManager`, sau khi build payload và gửi WebSocket → save luôn payload vào ES
- Thêm endpoint `GET /api/v1/alerts/rules/{ruleId}/notifications?size=&page=`

**Frontend** (3 việc):

```js
// alertApi.js — thêm 1 hàm
export async function getNotificationsByRule(ruleId, { page = 0, size = 20 } = {}) {
    const response = await axiosClient.get(`/api/v1/alerts/rules/${ruleId}/notifications`, {
        params: { page, size },
    });
    return response.content || [];
}
```

```js
// hooks/useAlertNotifications.js — hook mới, tách riêng khỏi useAlerts
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getNotificationsByRule } from "../api/alertApi";

export const alertNotificationsKey = (ruleId) => ["alertNotifications", ruleId];

export function useAlertNotifications(ruleId) {
    return useQuery({
        queryKey: alertNotificationsKey(ruleId),
        queryFn: () => getNotificationsByRule(ruleId),
        enabled: !!ruleId,
        staleTime: 30_000,
    });
}

// Gọi hàm này từ WS callback để prepend notification mới vào cache
// mà không cần refetch toàn bộ
export function usePrependNotification() {
    const queryClient = useQueryClient();
    return (ruleId, newNotification) => {
        queryClient.setQueryData(alertNotificationsKey(ruleId), (old) => {
            if (!old) return [newNotification];
            // Tránh duplicate nếu vì lý do nào đó nhận 2 lần
            const alreadyExists = old.some((n) => n.timestamp === newNotification.timestamp && n.ruleId === newNotification.ruleId);
            if (alreadyExists) return old;
            return [newNotification, ...old];
        });
    };
}
```

```jsx
// AlertRuleDetail.jsx — thay notifications state bằng query
import { useAlertNotifications, usePrependNotification } from "../../hooks/useAlertNotifications";

// Bỏ: const [notifications, setNotifications] = useState([]);
const { data: notifications = [], isLoading: isLoadingNotifs } = useAlertNotifications(ruleId);
const prependNotification = usePrependNotification();

const onAlert = useCallback(
    (data) => {
        if (data.ruleId !== ruleId) return;
        prependNotification(ruleId, data); // cập nhật cache, không setState
    },
    [ruleId, prependNotification]
);
```

`AlertNotification.jsx` không cần sửa gì — nó vẫn nhận `notifications[]` từ props, không quan tâm data đến từ query hay state.