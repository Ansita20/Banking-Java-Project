import React from 'react';
import { Navigate } from 'react-router-dom';

const Protected = ({ children }) => {
  const token = sessionStorage.getItem("jwtToken");
  
  if (!token) {
    return <Navigate to="/auth" replace />;
  }
  
  return children;
};

export default Protected;