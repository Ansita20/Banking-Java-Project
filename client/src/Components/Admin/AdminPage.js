import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { BsBank, BsBoxArrowRight } from "react-icons/bs";
import api, { errorMessage } from "../Utills/api";
import { useAuth } from "../Context/AuthContext";
import { Button, Card, Empty, StatusBadge, formatDate, formatMoney, inputClass } from "../Utills/ui";

const TABS = [
  ["approvals", "Pending approvals"],
  ["accounts", "Accounts"],
  ["customers", "Customers"],
  ["transfers", "Transfers"],
];

const Table = ({ headers, children }) => (
  <div className="overflow-x-auto">
    <table className="min-w-full text-sm">
      <thead>
        <tr className="text-left text-gray-500 border-b">
          {headers.map((h) => <th key={h} className="py-2 pr-4 font-medium">{h}</th>)}
        </tr>
      </thead>
      <tbody>{children}</tbody>
    </table>
  </div>
);

const AdminPage = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [tab, setTab] = useState("approvals");
  const [summary, setSummary] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [transferStatus, setTransferStatus] = useState("");

  const load = useCallback(async () => {
    const safe = (p, setter) => p.then(({ data }) => setter(data)).catch((e) => toast.error(errorMessage(e)));
    await Promise.all([
      safe(api.get("/api/admin/accounts/summary"), setSummary),
      safe(api.get("/api/admin/accounts"), setAccounts),
      safe(api.get("/api/admin/users"), setCustomers),
      safe(api.get("/api/admin/transfers", { params: { status: transferStatus || undefined } }), (d) => setTransfers(d.content)),
    ]);
  }, [transferStatus]);

  useEffect(() => {
    load();
  }, [load]);

  const act = async (request, message) => {
    try {
      await request();
      toast.success(message);
      load();
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const withReason = (label, fn) => {
    const reason = window.prompt(`${label}: reason`);
    if (reason) fn(reason);
  };

  const accountAction = (no, action, body) => api.post(`/api/admin/accounts/${no}/${action}`, body);
  const pending = accounts.filter((a) => a.account.status === "PENDING_APPROVAL");

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2 text-indigo-600">
          <BsBank className="text-2xl" />
          <span className="text-xl font-bold text-gray-900">CoreBank · Back office</span>
        </div>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-gray-600">{user?.email}</span>
          <button onClick={() => { logout(); navigate("/auth"); }} className="flex items-center gap-1 text-gray-600 hover:text-red-600">
            <BsBoxArrowRight /> Sign out
          </button>
        </div>
      </header>

      <main className="p-4 md:p-8 space-y-6 max-w-7xl mx-auto">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            ["Customer deposits", summary ? formatMoney(summary.totalDeposits) : "-"],
            ["Active accounts", summary?.activeAccounts ?? "-"],
            ["Pending approvals", summary?.pendingApprovals ?? "-"],
            ["Frozen accounts", summary?.frozenAccounts ?? "-"],
          ].map(([label, value]) => (
            <Card key={label}>
              <p className="text-sm text-gray-500">{label}</p>
              <p className="text-2xl font-bold mt-1">{value}</p>
            </Card>
          ))}
        </div>

        <div className="flex gap-2 overflow-x-auto">
          {TABS.map(([id, label]) => (
            <button key={id} onClick={() => setTab(id)}
                    className={`px-4 py-2 rounded-md text-sm whitespace-nowrap ${tab === id ? "bg-indigo-600 text-white" : "bg-white text-gray-600 border"}`}>
              {label}{id === "approvals" && pending.length ? ` (${pending.length})` : ""}
            </button>
          ))}
        </div>

        {tab === "approvals" && (
          <Card title="Account applications">
            {pending.length === 0 ? <Empty>No applications waiting.</Empty> : (
              <Table headers={["Applicant", "Account", "Type", "Applied", ""]}>
                {pending.map(({ account: a, holderName, holderEmail }) => (
                  <tr key={a.accountNumber} className="border-b last:border-0">
                    <td className="py-2 pr-4">{holderName}<span className="block text-xs text-gray-500">{holderEmail}</span></td>
                    <td className="py-2 pr-4 font-mono">{a.accountNumber}</td>
                    <td className="py-2 pr-4">{a.type}</td>
                    <td className="py-2 pr-4">{formatDate(a.openedAt)}</td>
                    <td className="py-2 flex gap-2 justify-end">
                      <Button variant="success" onClick={() => act(() => accountAction(a.accountNumber, "approve"), "Account approved")}>Approve</Button>
                      <Button variant="danger" onClick={() => withReason("Reject", (reason) =>
                        act(() => accountAction(a.accountNumber, "reject", { reason }), "Application rejected"))}>Reject</Button>
                    </td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>
        )}

        {tab === "accounts" && (
          <Card title="All accounts">
            {accounts.length === 0 ? <Empty>No accounts.</Empty> : (
              <Table headers={["Account", "Holder", "Type", "Balance", "Status", ""]}>
                {accounts.map(({ account: a, holderName }) => (
                  <tr key={a.accountNumber} className="border-b last:border-0">
                    <td className="py-2 pr-4 font-mono">{a.accountNumber}</td>
                    <td className="py-2 pr-4">{holderName}</td>
                    <td className="py-2 pr-4">{a.type}</td>
                    <td className="py-2 pr-4">{formatMoney(a.balance)}</td>
                    <td className="py-2 pr-4"><StatusBadge status={a.status} />
                      {a.statusReason && <span className="block text-xs text-gray-500">{a.statusReason}</span>}</td>
                    <td className="py-2 text-right">
                      {a.status === "ACTIVE" && <Button variant="secondary" onClick={() => withReason("Freeze", (reason) =>
                        act(() => accountAction(a.accountNumber, "freeze", { reason }), "Account frozen"))}>Freeze</Button>}
                      {a.status === "FROZEN" && <Button variant="secondary" onClick={() =>
                        act(() => accountAction(a.accountNumber, "unfreeze"), "Account unfrozen")}>Unfreeze</Button>}
                    </td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>
        )}

        {tab === "customers" && (
          <Card title="Customers">
            <Table headers={["Name", "Email", "Verified", "KYC", "Joined", "Access", ""]}>
              {customers.filter((c) => c.role === "USER").map((c) => (
                <tr key={c.userId} className="border-b last:border-0">
                  <td className="py-2 pr-4">{c.firstName} {c.lastName}</td>
                  <td className="py-2 pr-4">{c.email}</td>
                  <td className="py-2 pr-4">{c.emailVerified ? "Yes" : "No"}</td>
                  <td className="py-2 pr-4"><StatusBadge status={c.kycCompleted ? "COMPLETED" : "PENDING"} /></td>
                  <td className="py-2 pr-4">{formatDate(c.createdAt)}</td>
                  <td className="py-2 pr-4"><StatusBadge status={c.enabled ? "ACTIVE" : "DISABLED"} /></td>
                  <td className="py-2 text-right">
                    <Button variant={c.enabled ? "danger" : "success"} onClick={() =>
                      act(() => api.patch(`/api/admin/users/${c.userId}/status`, { enabled: !c.enabled }),
                        c.enabled ? "Customer blocked" : "Customer unblocked")}>
                      {c.enabled ? "Block" : "Unblock"}
                    </Button>
                  </td>
                </tr>
              ))}
            </Table>
          </Card>
        )}

        {tab === "transfers" && (
          <Card title="Transfer monitor" action={
            <select value={transferStatus} onChange={(e) => setTransferStatus(e.target.value)} className={`${inputClass} w-48`}>
              <option value="">All statuses</option>
              {["PENDING", "DEBITED", "COMPLETED", "COMPENSATING", "FAILED", "REVERSED"].map((s) => <option key={s}>{s}</option>)}
            </select>
          }>
            {transfers.length === 0 ? <Empty>No transfers.</Empty> : (
              <Table headers={["When", "From", "To", "Amount", "Status", "Reference"]}>
                {transfers.map((t) => (
                  <tr key={t.transferId} className="border-b last:border-0">
                    <td className="py-2 pr-4 whitespace-nowrap">{formatDate(t.createdAt)}</td>
                    <td className="py-2 pr-4 font-mono">{t.fromAccount}</td>
                    <td className="py-2 pr-4 font-mono">{t.toAccount}</td>
                    <td className="py-2 pr-4">{formatMoney(t.amount)}</td>
                    <td className="py-2 pr-4"><StatusBadge status={t.status} />
                      {t.failureReason && <span className="block text-xs text-gray-500">{t.failureReason}</span>}</td>
                    <td className="py-2 font-mono text-xs text-gray-400">{t.transferId.slice(0, 8)}</td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>
        )}
      </main>
    </div>
  );
};

export default AdminPage;
