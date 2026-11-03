import React, { useState } from "react";
import { useNavigate, useSearchParams, NavLink } from "react-router-dom";
import { toast } from "react-hot-toast";
import { BsBank } from "react-icons/bs";
import api, { errorMessage } from "../Utills/api";
import { useAuth } from "../Context/AuthContext";
import { Button, Field, inputClass } from "../Utills/ui";

const MODES = { LOGIN: "login", REGISTER: "register", FORGOT: "forgot", RESET: "reset" };

const AuthPage = () => {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { login } = useAuth();
  const [mode, setMode] = useState(MODES.LOGIN);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "", code: "" });

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const run = async (action) => {
    setLoading(true);
    try {
      await action();
    } catch (error) {
      const code = error?.response?.data?.code;
      if (code === "EMAIL_NOT_VERIFIED") {
        toast.error("Please verify your email first");
        navigate(`/verify-email?email=${encodeURIComponent(form.email)}`);
        return;
      }
      toast.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const submitLogin = (e) => {
    e.preventDefault();
    run(async () => {
      const user = await login(form.email, form.password);
      toast.success(`Welcome back, ${user.firstName}`);
      navigate(user.role === "ADMIN" ? "/admin" : "/dashboard");
    });
  };

  const submitRegister = (e) => {
    e.preventDefault();
    run(async () => {
      const { firstName, lastName, email, password } = form;
      const { data } = await api.post("/api/auth/register", { firstName, lastName, email, password });
      toast.success(data.message);
      navigate(`/verify-email?email=${encodeURIComponent(email)}`);
    });
  };

  const submitForgot = (e) => {
    e.preventDefault();
    run(async () => {
      await api.post("/api/auth/forgot-password", { email: form.email });
      toast.success("If the email is registered, a reset code has been sent");
      setMode(MODES.RESET);
    });
  };

  const submitReset = (e) => {
    e.preventDefault();
    run(async () => {
      await api.post("/api/auth/reset-password", { email: form.email, code: form.code, newPassword: form.password });
      toast.success("Password updated, please log in");
      setForm({ ...form, password: "", code: "" });
      setMode(MODES.LOGIN);
    });
  };

  const tab = (value, label) => (
    <button
      type="button"
      onClick={() => setMode(value)}
      className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all ${
        mode === value ? "bg-white text-indigo-600 shadow" : "text-gray-500 hover:text-gray-700"
      }`}
    >
      {label}
    </button>
  );

  const emailField = (
    <Field label="Email address">
      <input name="email" type="email" required autoComplete="email" value={form.email} onChange={update}
             className={inputClass} />
    </Field>
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <NavLink to="/" className="inline-flex items-center gap-2 text-indigo-600">
          <BsBank className="text-4xl" />
          <span className="text-2xl font-bold text-gray-900">CoreBank</span>
        </NavLink>
        <p className="mt-2 text-sm text-gray-600">Secure online banking</p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow-xl rounded-lg sm:px-10">
          {params.get("expired") && mode === MODES.LOGIN && (
            <p className="mb-4 text-sm text-amber-700 bg-amber-50 rounded p-2">Your session expired. Please log in again.</p>
          )}

          {(mode === MODES.LOGIN || mode === MODES.REGISTER) && (
            <div className="flex mb-6 bg-gray-100 rounded-lg p-1">
              {tab(MODES.LOGIN, "Sign In")}
              {tab(MODES.REGISTER, "Open an account")}
            </div>
          )}

          {mode === MODES.LOGIN && (
            <form onSubmit={submitLogin} className="space-y-5">
              {emailField}
              <Field label="Password">
                <input name="password" type="password" required autoComplete="current-password"
                       value={form.password} onChange={update} className={inputClass} />
              </Field>
              <div className="text-right">
                <button type="button" className="text-sm text-indigo-600 hover:underline" onClick={() => setMode(MODES.FORGOT)}>
                  Forgot password?
                </button>
              </div>
              <Button type="submit" className="w-full" loading={loading}>Sign in</Button>
            </form>
          )}

          {mode === MODES.REGISTER && (
            <form onSubmit={submitRegister} className="space-y-5">
              <div className="grid grid-cols-2 gap-3">
                <Field label="First name">
                  <input name="firstName" required value={form.firstName} onChange={update} className={inputClass} />
                </Field>
                <Field label="Last name">
                  <input name="lastName" required value={form.lastName} onChange={update} className={inputClass} />
                </Field>
              </div>
              {emailField}
              <Field label="Password" hint="At least 8 characters, with a letter and a digit">
                <input name="password" type="password" required minLength={8} autoComplete="new-password"
                       value={form.password} onChange={update} className={inputClass} />
              </Field>
              <Button type="submit" className="w-full" loading={loading}>Create my profile</Button>
            </form>
          )}

          {mode === MODES.FORGOT && (
            <form onSubmit={submitForgot} className="space-y-5">
              <h3 className="font-semibold text-gray-800">Reset your password</h3>
              {emailField}
              <Button type="submit" className="w-full" loading={loading}>Send reset code</Button>
              <button type="button" className="w-full text-sm text-gray-500" onClick={() => setMode(MODES.LOGIN)}>Back to sign in</button>
            </form>
          )}

          {mode === MODES.RESET && (
            <form onSubmit={submitReset} className="space-y-5">
              <h3 className="font-semibold text-gray-800">Enter the code we emailed you</h3>
              {emailField}
              <Field label="6-digit code">
                <input name="code" required pattern="\d{6}" maxLength={6} value={form.code} onChange={update}
                       className={`${inputClass} tracking-widest`} />
              </Field>
              <Field label="New password" hint="At least 8 characters, with a letter and a digit">
                <input name="password" type="password" required minLength={8} value={form.password} onChange={update}
                       className={inputClass} />
              </Field>
              <Button type="submit" className="w-full" loading={loading}>Update password</Button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default AuthPage;
