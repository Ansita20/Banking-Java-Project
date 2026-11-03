import React, { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import api, { errorMessage } from "../Utills/api";
import { Button, Card, Empty, Field, formatDate, formatMoney, inputClass } from "../Utills/ui";

const PAGE_SIZE = 25;

const toCsv = (rows) => {
  const header = ["Date", "Type", "Description", "Debit", "Credit", "Balance", "Reference"];
  const escape = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
  const lines = rows.map((r) => [
    r.createdAt, r.entryType, r.description,
    r.direction === "DEBIT" ? r.amount : "", r.direction === "CREDIT" ? r.amount : "",
    r.balanceAfter, r.reference,
  ].map(escape).join(","));
  return [header.join(","), ...lines].join("\n");
};

const StatementPanel = ({ accounts }) => {
  const [account, setAccount] = useState(accounts[0]?.accountNumber || "");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (!account && accounts.length) setAccount(accounts[0].accountNumber);
  }, [accounts, account]);

  const load = useCallback(async () => {
    if (!account) return;
    try {
      const { data } = await api.get(`/api/accounts/${account}/statement`, {
        params: { from: from || undefined, to: to || undefined, page, size: PAGE_SIZE },
      });
      setResult(data);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  }, [account, from, to, page]);

  useEffect(() => {
    load();
  }, [load]);

  const download = () => {
    const blob = new Blob([toCsv(result?.content || [])], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `statement-${account}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  if (!accounts.length) return <Card title="Statements"><Empty>No accounts yet.</Empty></Card>;

  return (
    <Card title="Account statement" action={<Button variant="secondary" onClick={download} disabled={!result?.content?.length}>Download CSV</Button>}>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
        <Field label="Account">
          <select value={account} onChange={(e) => { setAccount(e.target.value); setPage(0); }} className={inputClass}>
            {accounts.map((a) => <option key={a.accountNumber} value={a.accountNumber}>{a.type} · {a.accountNumber}</option>)}
          </select>
        </Field>
        <Field label="From">
          <input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} className={inputClass} />
        </Field>
        <Field label="To">
          <input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} className={inputClass} />
        </Field>
      </div>

      {!result?.content?.length ? <Empty>No transactions in this period.</Empty> : (
        <>
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b">
                  <th className="py-2 pr-4">Date</th>
                  <th className="py-2 pr-4">Description</th>
                  <th className="py-2 pr-4 text-right">Debit</th>
                  <th className="py-2 pr-4 text-right">Credit</th>
                  <th className="py-2 text-right">Balance</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((r) => (
                  <tr key={r.id} className="border-b last:border-0">
                    <td className="py-2 pr-4 whitespace-nowrap">{formatDate(r.createdAt)}</td>
                    <td className="py-2 pr-4">
                      {r.description}
                      <span className="block text-xs text-gray-400">{r.entryType.replace(/_/g, " ")}</span>
                    </td>
                    <td className="py-2 pr-4 text-right text-red-600">{r.direction === "DEBIT" ? formatMoney(r.amount) : ""}</td>
                    <td className="py-2 pr-4 text-right text-green-700">{r.direction === "CREDIT" ? formatMoney(r.amount) : ""}</td>
                    <td className="py-2 text-right font-medium">{formatMoney(r.balanceAfter)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex justify-between items-center mt-4 text-sm">
            <span className="text-gray-500">Page {result.number + 1} of {Math.max(result.totalPages, 1)}</span>
            <div className="flex gap-2">
              <Button variant="secondary" disabled={result.first} onClick={() => setPage(page - 1)}>Previous</Button>
              <Button variant="secondary" disabled={result.last} onClick={() => setPage(page + 1)}>Next</Button>
            </div>
          </div>
        </>
      )}
    </Card>
  );
};

export default StatementPanel;
