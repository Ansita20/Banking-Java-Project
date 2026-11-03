import React, { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import api, { errorMessage } from "../Utills/api";
import { Button, Card, Empty, Field, formatDate, inputClass } from "../Utills/ui";

const BeneficiariesPanel = () => {
  const [list, setList] = useState([]);
  const [nickname, setNickname] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [verified, setVerified] = useState(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(() => {
    api.get("/api/beneficiaries").then(({ data }) => setList(data)).catch((e) => toast.error(errorMessage(e)));
  }, []);

  useEffect(load, [load]);

  const verify = async () => {
    setVerified(null);
    try {
      const { data } = await api.get(`/api/beneficiaries/lookup/${accountNumber}`);
      setVerified(data);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const add = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.post("/api/beneficiaries", { nickname, accountNumber });
      toast.success("Beneficiary added");
      setNickname("");
      setAccountNumber("");
      setVerified(null);
      load();
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const remove = async (b) => {
    if (!window.confirm(`Remove ${b.nickname}?`)) return;
    try {
      await api.delete(`/api/beneficiaries/${b.id}`);
      load();
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card title="Add a beneficiary">
        <form onSubmit={add} className="space-y-4">
          <Field label="Nickname">
            <input required maxLength={60} value={nickname} onChange={(e) => setNickname(e.target.value)} className={inputClass} />
          </Field>
          <Field label="Account number" hint={verified ? `Verified: ${verified.holderName} (${verified.type})` : "We verify the account before saving it"}>
            <div className="flex gap-2">
              <input required pattern="\d{12}" maxLength={12} value={accountNumber}
                     onChange={(e) => { setAccountNumber(e.target.value); setVerified(null); }}
                     className={`${inputClass} font-mono`} />
              <Button type="button" variant="secondary" onClick={verify} disabled={!/^\d{12}$/.test(accountNumber)}>Verify</Button>
            </div>
          </Field>
          <Button type="submit" className="w-full" loading={loading}>Save beneficiary</Button>
        </form>
      </Card>

      <Card title="Saved beneficiaries">
        {list.length === 0 ? <Empty>No beneficiaries yet.</Empty> : (
          <ul className="divide-y">
            {list.map((b) => (
              <li key={b.id} className="py-3 flex justify-between items-center">
                <div>
                  <p className="font-medium">{b.nickname}</p>
                  <p className="text-sm text-gray-500 font-mono">{b.accountNumber} · {b.holderName}</p>
                  <p className="text-xs text-gray-400">Added {formatDate(b.createdAt)}</p>
                </div>
                <Button variant="secondary" onClick={() => remove(b)}>Remove</Button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
};

export default BeneficiariesPanel;
