import React, { useEffect, useRef, useState } from "react";
import { toast } from "react-hot-toast";
import { BsCheckCircleFill, BsCircle, BsXCircleFill } from "react-icons/bs";
import api, { errorMessage, newIdempotencyKey } from "../Utills/api";
import { Button, Card, Empty, Field, StatusBadge, formatMoney, inputClass } from "../Utills/ui";

const FINAL = ["COMPLETED", "FAILED", "REVERSED"];

/** Visualises the saga: the debit and credit legs happen in account-service, asynchronously. */
const SagaProgress = ({ transfer }) => {
  const s = transfer.status;
  const debited = ["DEBITED", "COMPLETED", "COMPENSATING", "REVERSED"].includes(s);
  const steps = [
    { label: "Transfer accepted", done: true },
    { label: "Debited from your account", done: debited, failed: s === "FAILED" },
    { label: "Credited to beneficiary", done: s === "COMPLETED", failed: ["COMPENSATING", "REVERSED"].includes(s) },
  ];
  if (["COMPENSATING", "REVERSED"].includes(s)) {
    steps.push({ label: "Amount returned to you", done: s === "REVERSED" });
  }
  return (
    <div className="mt-4 border rounded-lg p-4 bg-gray-50">
      <div className="flex justify-between items-center mb-3">
        <span className="font-semibold">{formatMoney(transfer.amount)} to {transfer.toAccount}</span>
        <StatusBadge status={s} />
      </div>
      <ol className="space-y-2 text-sm">
        {steps.map((step) => (
          <li key={step.label} className="flex items-center gap-2">
            {step.failed ? <BsXCircleFill className="text-red-500" />
              : step.done ? <BsCheckCircleFill className="text-green-600" />
              : <BsCircle className="text-gray-400 animate-pulse" />}
            {step.label}
          </li>
        ))}
      </ol>
      {transfer.failureReason && <p className="text-sm text-red-600 mt-3">Reason: {transfer.failureReason}</p>}
      <p className="text-xs text-gray-400 mt-3 font-mono">Ref {transfer.transferId}</p>
    </div>
  );
};

const TransferPanel = ({ activeAccounts, reloadAccounts }) => {
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [mode, setMode] = useState("beneficiary");
  const [form, setForm] = useState({ fromAccount: "", beneficiaryId: "", toAccount: "", amount: "", description: "" });
  const [payee, setPayee] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [transfer, setTransfer] = useState(null);
  // The key survives retries of the same form submission and is renewed once it succeeds.
  const idempotencyKey = useRef(newIdempotencyKey());
  const pollTimer = useRef(null);

  useEffect(() => {
    api.get("/api/beneficiaries").then(({ data }) => setBeneficiaries(data)).catch(() => {});
    return () => clearTimeout(pollTimer.current);
  }, []);

  useEffect(() => {
    if (!form.fromAccount && activeAccounts.length) {
      setForm((f) => ({ ...f, fromAccount: activeAccounts[0].accountNumber }));
    }
  }, [activeAccounts, form.fromAccount]);

  const update = (e) => {
    idempotencyKey.current = newIdempotencyKey();
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const lookupPayee = async () => {
    setPayee(null);
    if (!/^\d{12}$/.test(form.toAccount)) return;
    try {
      const { data } = await api.get(`/api/beneficiaries/lookup/${form.toAccount}`);
      setPayee(data);
    } catch (error) {
      setPayee({ error: errorMessage(error) });
    }
  };

  const poll = (id, attempt = 0) => {
    pollTimer.current = setTimeout(async () => {
      try {
        const { data } = await api.get(`/api/transfers/${id}`);
        setTransfer(data);
        if (FINAL.includes(data.status)) {
          reloadAccounts();
          data.status === "COMPLETED" ? toast.success("Transfer completed") : toast.error(`Transfer ${data.status.toLowerCase()}`);
          return;
        }
      } catch {
        // keep polling; a transient gateway error should not lose track of the transfer
      }
      if (attempt < 30) poll(id, attempt + 1);
    }, 1000);
  };

  const submit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const body = {
        fromAccount: form.fromAccount,
        amount: form.amount,
        description: form.description,
        ...(mode === "beneficiary" ? { beneficiaryId: form.beneficiaryId } : { toAccount: form.toAccount }),
      };
      const { data } = await api.post("/api/transfers", body, { headers: { "Idempotency-Key": idempotencyKey.current } });
      setTransfer(data);
      idempotencyKey.current = newIdempotencyKey();
      setForm({ ...form, amount: "", description: "" });
      poll(data.transferId);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  if (!activeAccounts.length) {
    return <Card title="Transfer money"><Empty>You need an active account to make transfers.</Empty></Card>;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card title="Transfer money">
        <form onSubmit={submit} className="space-y-4">
          <Field label="From account">
            <select name="fromAccount" value={form.fromAccount} onChange={update} className={inputClass}>
              {activeAccounts.map((a) => (
                <option key={a.accountNumber} value={a.accountNumber}>
                  {a.type} · {a.accountNumber} · {formatMoney(a.balance)}
                </option>
              ))}
            </select>
          </Field>

          <div className="flex bg-gray-100 rounded-lg p-1 text-sm">
            {[["beneficiary", "Saved beneficiary"], ["account", "Account number"]].map(([value, label]) => (
              <button key={value} type="button" onClick={() => setMode(value)}
                      className={`flex-1 py-1.5 rounded-md ${mode === value ? "bg-white shadow text-indigo-600" : "text-gray-500"}`}>
                {label}
              </button>
            ))}
          </div>

          {mode === "beneficiary" ? (
            <Field label="Pay to">
              <select name="beneficiaryId" required value={form.beneficiaryId} onChange={update} className={inputClass}>
                <option value="">Select a beneficiary</option>
                {beneficiaries.map((b) => (
                  <option key={b.id} value={b.id}>{b.nickname} · {b.accountNumber}</option>
                ))}
              </select>
            </Field>
          ) : (
            <Field label="Destination account number"
                   hint={payee?.holderName ? `Account holder: ${payee.holderName}` : payee?.error}>
              <input name="toAccount" required pattern="\d{12}" maxLength={12} value={form.toAccount}
                     onChange={update} onBlur={lookupPayee} className={`${inputClass} font-mono`} />
            </Field>
          )}

          <Field label="Amount (INR)">
            <input name="amount" type="number" min="1" step="0.01" required value={form.amount} onChange={update}
                   className={inputClass} />
          </Field>
          <Field label="Remarks">
            <input name="description" maxLength={140} value={form.description} onChange={update} className={inputClass} />
          </Field>
          <Button type="submit" className="w-full" loading={submitting}>Send money</Button>
        </form>
      </Card>

      <Card title="Transfer status">
        {transfer ? <SagaProgress transfer={transfer} /> : <Empty>Your transfer's progress will appear here.</Empty>}
      </Card>
    </div>
  );
};

export default TransferPanel;
