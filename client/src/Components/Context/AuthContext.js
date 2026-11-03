import React, { createContext, useCallback, useContext, useState } from "react";
import api from "../Utills/api";

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

const readStoredUser = () => {
  try {
    return JSON.parse(sessionStorage.getItem("user"));
  } catch {
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(readStoredUser);

  const saveUser = useCallback((next) => {
    setUser(next);
    sessionStorage.setItem("user", JSON.stringify(next));
  }, []);

  const login = async (email, password) => {
    const { data } = await api.post("/api/auth/login", { email, password });
    sessionStorage.setItem("accessToken", data.accessToken);
    saveUser(data.user);
    return data.user;
  };

  const logout = () => {
    sessionStorage.clear();
    setUser(null);
  };

  const refreshUser = useCallback(async () => {
    const { data } = await api.get("/api/users/me");
    saveUser(data);
    return data;
  }, [saveUser]);

  return (
    <AuthContext.Provider value={{ user, login, logout, refreshUser, saveUser }}>
      {children}
    </AuthContext.Provider>
  );
};
