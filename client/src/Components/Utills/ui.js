import React from "react";

export const formatMoney = (value) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(value || 0));

export const formatDate = (iso) =>
  iso ? new Date(iso).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "-";

export const maskAccount = (number) => (number ? `XXXX${number.slice(-4)}` : "");

export const inputClass =
  "block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 " +
  "focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:bg-gray-100";

const BADGE_COLORS = {
  ACTIVE: "bg-green-100 text-green-800",
  COMPLETED: "bg-green-100 text-green-800",
  PENDING_APPROVAL: "bg-yellow-100 text-yellow-800",
  PENDING: "bg-yellow-100 text-yellow-800",
  DEBITED: "bg-blue-100 text-blue-800",
  COMPENSATING: "bg-orange-100 text-orange-800",
  FROZEN: "bg-blue-100 text-blue-800",
  REVERSED: "bg-orange-100 text-orange-800",
  FAILED: "bg-red-100 text-red-800",
  REJECTED: "bg-red-100 text-red-800",
  CLOSED: "bg-gray-200 text-gray-700",
  DISABLED: "bg-red-100 text-red-800",
};

export const StatusBadge = ({ status }) => (
  <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${BADGE_COLORS[status] || "bg-gray-100 text-gray-700"}`}>
    {String(status).replace(/_/g, " ")}
  </span>
);

export const Card = ({ title, action, children, className = "" }) => (
  <div className={`bg-white rounded-xl shadow-sm border border-gray-100 p-6 ${className}`}>
    {(title || action) && (
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-800">{title}</h3>
        {action}
      </div>
    )}
    {children}
  </div>
);

const BUTTON_VARIANTS = {
  primary: "bg-indigo-600 text-white hover:bg-indigo-700",
  secondary: "bg-white text-gray-700 border border-gray-300 hover:bg-gray-50",
  danger: "bg-red-600 text-white hover:bg-red-700",
  success: "bg-green-600 text-white hover:bg-green-700",
};

export const Button = ({ variant = "primary", className = "", loading, children, ...props }) => (
  <button
    className={`inline-flex items-center justify-center px-4 py-2 rounded-md text-sm font-medium transition-colors
      disabled:opacity-50 disabled:cursor-not-allowed ${BUTTON_VARIANTS[variant]} ${className}`}
    disabled={loading || props.disabled}
    {...props}
  >
    {loading ? "Please wait..." : children}
  </button>
);

export const Field = ({ label, children, hint }) => (
  <label className="block">
    <span className="block text-sm font-medium text-gray-700 mb-1">{label}</span>
    {children}
    {hint && <span className="block text-xs text-gray-500 mt-1">{hint}</span>}
  </label>
);

export const Empty = ({ children }) => <p className="text-sm text-gray-500 py-6 text-center">{children}</p>;
