import axios from "axios";

export const API_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";

// Every request goes through the API gateway, never to a service directly.
const api = axios.create({ baseURL: API_URL, timeout: 15000 });

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && sessionStorage.getItem("accessToken")) {
      sessionStorage.clear();
      window.location.assign("/auth?expired=1");
    }
    return Promise.reject(error);
  }
);

/** Human-readable message from the platform's ApiError shape. */
export const errorMessage = (error, fallback = "Something went wrong") => {
  const data = error?.response?.data;
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length) {
    return Object.entries(data.fieldErrors).map(([field, msg]) => `${field} ${msg}`).join(", ");
  }
  if (!error?.response) {
    return "Cannot reach the bank right now. Check your connection and retry.";
  }
  return data?.message || fallback;
};

/** One key per user intent: retries of the same submission reuse it so money never moves twice. */
export const newIdempotencyKey = () =>
  window.crypto?.randomUUID
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

export default api;
