import React, { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import api, { errorMessage } from "../Utills/api";
import { Button, Card, Empty, formatDate } from "../Utills/ui";

const NotificationsPanel = ({ onChange }) => {
  const [items, setItems] = useState([]);

  const load = useCallback(() => {
    api.get("/api/notifications", { params: { size: 50 } })
      .then(({ data }) => setItems(data.content))
      .catch((e) => toast.error(errorMessage(e, "Notifications are unavailable right now")));
  }, []);

  useEffect(load, [load]);

  const markRead = async (id) => {
    await api.post(`/api/notifications/${id}/read`).catch(() => {});
    load();
    onChange();
  };

  const markAll = async () => {
    await api.post("/api/notifications/read-all").catch(() => {});
    load();
    onChange();
  };

  return (
    <Card title="Notifications" action={<Button variant="secondary" onClick={markAll}>Mark all read</Button>}>
      {items.length === 0 ? <Empty>You're all caught up.</Empty> : (
        <ul className="divide-y">
          {items.map((n) => (
            <li key={n.id} className={`py-3 flex justify-between gap-4 ${n.read ? "opacity-60" : ""}`}>
              <div>
                <p className="font-medium text-gray-800">{n.title}</p>
                <p className="text-sm text-gray-600">{n.message}</p>
                <p className="text-xs text-gray-400 mt-1">{formatDate(n.createdAt)}</p>
              </div>
              {!n.read && <button className="text-xs text-indigo-600 whitespace-nowrap" onClick={() => markRead(n.id)}>Mark read</button>}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
};

export default NotificationsPanel;
