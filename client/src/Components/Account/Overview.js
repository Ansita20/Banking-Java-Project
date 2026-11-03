import React, { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import api, { errorMessage } from "../Utills/api";
import { useAuth } from "../Context/AuthContext";
import { Button, Card, Empty, StatusBadge, formatDate, formatMoney, inputClass, maskAccount } from "../Utills/ui";

const Overview = ({ accounts, reloadAccounts, goTo }) => {
  const { user } = useAuth();
  const [type, setType] = useState("SAVINGS");
  const [opening, setOpening] = useState(false);
  const [transfers, setTransfers] = useState([]);
  const [limits, setLimits] = useState(null);

  useEffect(() => {
    api.get("/api/transfers", { params: { size: 5 } }).then(({ data }) => setTransfers(data.content)).catch(() => {});
    api.get("/api/transfers/limits").then(({ data }) => setLimits(data)).catch(() => {});
  }, []);

  const total = accounts.filter((a) => a.status === "ACTIVE" || a.status === "FROZEN")
    .reduce((sum, a) => sum + Number(a.balance), 0);

  const openAccount = async () => {
    setOpening(true);
    try {
      await api.post("/api/accounts", { type });
      toast.success("Application submitted. A bank officer will review it shortly.");
      reloadAccounts();
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setOpening(false);
    }
  };

  return (
    <div className="space-y-6">
      {!user?.kycCompleted && (
        <div className="bg-amber-50 border border-amber-200 text-amber-800 rounded-lg p-4 flex items-center justify-between">
          <span>Complete your KYC profile to open an account.</span>
          <Button variant="secondary" onClick={() => goTo("profile")}>Complete KYC</Button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <p className="text-sm text-gray-500">Total balance</p>
          <p className="text-3xl font-bold text-gray-900 mt-1">{formatMoney(total)}</p>
        </Card>
        <Card>
          <p className="text-sm text-gray-500">Transfer limit left today</p>
          <p className="text-3xl font-bold text-gray-900 mt-1">{limits ? formatMoney(limits.remainingToday) : "-"}</p>
          {limits && <p className="text-xs text-gray-500 mt-1">of {formatMoney(limits.dailyLimit)} daily</p>}
        </Card>
        <Card>
          <p className="text-sm text-gray-500 mb-2">Open a new account</p>
          <div className="flex gap-2">
            <select value={type} onChange={(e) => setType(e.target.value)} className={inputClass}>
              <option value="SAVINGS">Savings</option>
              <option value="CURRENT">Current</option>
            </select>
            <Button onClick={openAccount} loading={opening} disabled={!user?.kycCompleted}>Apply</Button>
          </div>
        </Card>
      </div>

      <Card title="Your accounts">
        {accounts.length === 0 ? (
          <Empty>No accounts yet.</Empty>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {accounts.map((a) => (
              <div key={a.accountNumber} className="border rounded-lg p-4 bg-gradient-to-br from-indigo-600 to-indigo-800 text-white">
                <div className="flex justify-between items-start">
                  <div>
                    <p className="text-xs uppercase opacity-75">{a.type} account</p>
                    <p className="font-mono text-lg tracking-wider">{a.accountNumber}</p>
                  </div>
                  <span className="bg-white rounded-full"><StatusBadge status={a.status} /></span>
                </div>
                <p className="text-2xl font-bold mt-4">{formatMoney(a.balance)}</p>
                {a.statusReason && <p className="text-xs mt-2 opacity-90">Note: {a.statusReason}</p>}
                <p className="text-xs opacity-75 mt-2">Opened {formatDate(a.openedAt)}</p>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card title="Recent transfers" action={<Button variant="secondary" onClick={() => goTo("transfer")}>New transfer</Button>}>
        {transfers.length === 0 ? (
          <Empty>No transfers yet.</Empty>
        ) : (
          <ul className="divide-y">
            {transfers.map((t) => (
              <li key={t.transferId} className="py-3 flex justify-between items-center text-sm">
                <div>
                  <p className="font-medium text-gray-800">{t.description}</p>
                  <p className="text-gray-500">{maskAccount(t.fromAccount)} → {maskAccount(t.toAccount)} · {formatDate(t.createdAt)}</p>
                </div>
                <div className="text-right">
                  <p className="font-semibold">{formatMoney(t.amount)}</p>
                  <StatusBadge status={t.status} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
};

export default Overview;
