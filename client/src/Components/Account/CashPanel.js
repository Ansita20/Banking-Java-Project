import React, { useEffect, useRef, useState } from "react";
import { toast } from "react-hot-toast";
import api, { errorMessage, newIdempotencyKey } from "../Utills/api";
import { Button, Card, Empty, Field, formatMoney, inputClass } from "../Utills/ui";

const CashPanel = ({ activeAccounts, reloadAccounts }) => {
  const [kind, setKind] = useState("deposits");
  const [account, setAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const idempotencyKey = useRef(newIdempotencyKey());

  useEffect(() => {
    if (!account && activeAccounts.length) setAccount(activeAccounts[0].accountNumber);
  }, [activeAccounts, account]);

  const changed = (setter) => (e) => {
    idempotencyKey.current = newIdempotencyKey();
    setter(e.target.value);
  };

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await api.post(`/api/accounts/${account}/${kind}`, { amount, description },
        { headers: { "Idempotency-Key": idempotencyKey.current } });
      toast.success(`${kind === "deposits" ? "Deposited" : "Withdrew"} ${formatMoney(data.amount)}. Balance ${formatMoney(data.balanceAfter)}`);
      idempotencyKey.current = newIdempotencyKey();
      setAmount("");
      setDescription("");
      reloadAccounts();
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  if (!activeAccounts.length) {
    return <Card title="Deposit / Withdraw"><Empty>You need an active account first.</Empty></Card>;
  }

  return (
    <Card title="Deposit / Withdraw" className="max-w-xl">
      <div className="flex bg-gray-100 rounded-lg p-1 text-sm mb-4">
        {[["deposits", "Deposit"], ["withdrawals", "Withdraw"]].map(([value, label]) => (
          <button key={value} type="button" onClick={() => setKind(value)}
                  className={`flex-1 py-1.5 rounded-md ${kind === value ? "bg-white shadow text-indigo-600" : "text-gray-500"}`}>
            {label}
          </button>
        ))}
      </div>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Account">
          <select value={account} onChange={changed(setAccount)} className={inputClass}>
            {activeAccounts.map((a) => (
              <option key={a.accountNumber} value={a.accountNumber}>{a.type} · {a.accountNumber} · {formatMoney(a.balance)}</option>
            ))}
          </select>
        </Field>
        <Field label="Amount (INR)" hint="Up to ₹2,00,000 per transaction">
          <input type="number" min="1" max="200000" step="0.01" required value={amount} onChange={changed(setAmount)}
                 className={inputClass} />
        </Field>
        <Field label="Remarks">
          <input maxLength={140} value={description} onChange={changed(setDescription)} className={inputClass} />
        </Field>
        <Button type="submit" variant={kind === "deposits" ? "success" : "primary"} className="w-full" loading={loading}>
          {kind === "deposits" ? "Deposit" : "Withdraw"}
        </Button>
      </form>
    </Card>
  );
};

export default CashPanel;
