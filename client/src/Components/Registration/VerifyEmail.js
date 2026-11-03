import React, { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-hot-toast";
import api, { errorMessage } from "../Utills/api";
import { Button, Field, inputClass } from "../Utills/ui";

const VerifyEmail = () => {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState(params.get("email") || "");
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);

  const verify = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await api.post("/api/auth/verify-email", { email, code });
      toast.success(data.message);
      navigate("/auth");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    try {
      await api.post("/api/auth/resend-otp", { email });
      toast.success("A new code is on its way");
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center px-4">
      <div className="bg-white shadow-xl rounded-lg p-8 w-full max-w-md">
        <h2 className="text-2xl font-bold text-gray-900">Verify your email</h2>
        <p className="text-sm text-gray-600 mt-1 mb-6">
          We sent a 6-digit code to your inbox. It expires in 10 minutes.
          {process.env.NODE_ENV !== "production" && " (Local setup: open Mailpit at http://localhost:8025)"}
        </p>
        <form onSubmit={verify} className="space-y-5">
          <Field label="Email">
            <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass} />
          </Field>
          <Field label="Verification code">
            <input required pattern="\d{6}" maxLength={6} value={code} onChange={(e) => setCode(e.target.value)}
                   className={`${inputClass} text-center text-2xl tracking-[0.5em]`} />
          </Field>
          <Button type="submit" className="w-full" loading={loading}>Verify</Button>
        </form>
        <button onClick={resend} className="mt-4 w-full text-sm text-indigo-600 hover:underline">Resend code</button>
      </div>
    </div>
  );
};

export default VerifyEmail;
