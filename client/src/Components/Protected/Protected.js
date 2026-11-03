import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../Context/AuthContext";

const Protected = ({ children, role }) => {
  const { user } = useAuth();

  if (!sessionStorage.getItem("accessToken") || !user) {
    return <Navigate to="/auth" replace />;
  }
  if (role && user.role !== role) {
    return <Navigate to={user.role === "ADMIN" ? "/admin" : "/dashboard"} replace />;
  }
  return children;
};

export default Protected;
