import React from 'react';
import { Routes, Route, Navigate } from "react-router-dom";
import './App.css';
import LandingPage from './Components/LandingPage/LandingPage';
import AuthPage from './Components/Auth/AuthPage';
import Dashboard from './Components/Account/Dashboard';
import Protected from './Components/Protected/Protected';
import VerifyEmail from './Components/Registration/VerifyEmail';
import AdminPage from './Components/Admin/AdminPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/auth" element={<AuthPage />} />
      <Route path="/verify-email" element={<VerifyEmail />} />
      <Route path="/dashboard" element={
        <Protected role="USER">
          <Dashboard />
        </Protected>
      } />
      <Route path="/admin" element={
        <Protected role="ADMIN">
          <AdminPage />
        </Protected>
      } />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
