import React, { useState } from "react";
import { toast } from "react-hot-toast";
import api, { errorMessage } from "../Utills/api";
import { useAuth } from "../Context/AuthContext";
import { Button, Card, Field, StatusBadge, inputClass } from "../Utills/ui";

const emptyKyc = {
  phone: "", dateOfBirth: "", gender: "", addressLine: "", city: "", state: "", postalCode: "",
  panNumber: "", governmentId: "",
};

const ProfilePanel = () => {
  const { user, saveUser } = useAuth();
  const p = user?.profile || {};
  const [kyc, setKyc] = useState({
    ...emptyKyc,
    phone: p.phone || "", dateOfBirth: p.dateOfBirth || "", gender: p.gender || "", addressLine: p.addressLine || "",
    city: p.city || "", state: p.state || "", postalCode: p.postalCode || "",
  });
  const [passwords, setPasswords] = useState({ currentPassword: "", newPassword: "" });
  const [saving, setSaving] = useState(false);

  const update = (e) => setKyc({ ...kyc, [e.target.name]: e.target.name === "panNumber" ? e.target.value.toUpperCase() : e.target.value });

  const submitKyc = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const { data } = await api.put("/api/users/me/kyc", kyc);
      saveUser(data);
      setKyc({ ...kyc, panNumber: "", governmentId: "" });
      toast.success("KYC details saved");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const changePassword = async (e) => {
    e.preventDefault();
    try {
      await api.post("/api/users/me/change-password", passwords);
      setPasswords({ currentPassword: "", newPassword: "" });
      toast.success("Password changed");
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const input = (name, props = {}) => (
    <input name={name} value={kyc[name]} onChange={update} required className={inputClass} {...props} />
  );

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <Card title="KYC details" className="lg:col-span-2"
            action={<StatusBadge status={user?.kycCompleted ? "COMPLETED" : "PENDING"} />}>
        {user?.kycCompleted && (
          <p className="text-sm text-gray-500 mb-4">
            On file: PAN {p.panNumberMasked}, Aadhaar {p.governmentIdMasked}. Re-enter both to update your details.
          </p>
        )}
        <form onSubmit={submitKyc} className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Field label="Mobile number">{input("phone", { pattern: "[6-9]\\d{9}", maxLength: 10 })}</Field>
          <Field label="Date of birth">{input("dateOfBirth", { type: "date" })}</Field>
          <Field label="Gender">
            <select name="gender" value={kyc.gender} onChange={update} required className={inputClass}>
              <option value="">Select</option>
              <option value="FEMALE">Female</option>
              <option value="MALE">Male</option>
              <option value="OTHER">Other</option>
            </select>
          </Field>
          <Field label="PIN code">{input("postalCode", { pattern: "\\d{6}", maxLength: 6 })}</Field>
          <div className="md:col-span-2"><Field label="Address">{input("addressLine")}</Field></div>
          <Field label="City">{input("city")}</Field>
          <Field label="State">{input("state")}</Field>
          <Field label="PAN" hint="e.g. ABCDE1234F">{input("panNumber", { pattern: "[A-Z]{5}\\d{4}[A-Z]", maxLength: 10 })}</Field>
          <Field label="Aadhaar" hint="12 digits, stored encrypted">{input("governmentId", { pattern: "\\d{12}", maxLength: 12 })}</Field>
          <div className="md:col-span-2"><Button type="submit" loading={saving}>Save KYC</Button></div>
        </form>
      </Card>

      <Card title="Security">
        <p className="text-sm text-gray-600 mb-4">{user?.email}</p>
        <form onSubmit={changePassword} className="space-y-4">
          <Field label="Current password">
            <input type="password" required value={passwords.currentPassword}
                   onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} className={inputClass} />
          </Field>
          <Field label="New password" hint="At least 8 characters, with a letter and a digit">
            <input type="password" required minLength={8} value={passwords.newPassword}
                   onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} className={inputClass} />
          </Field>
          <Button type="submit" variant="secondary" className="w-full">Change password</Button>
        </form>
      </Card>
    </div>
  );
};

export default ProfilePanel;
