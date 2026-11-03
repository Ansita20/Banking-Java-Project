import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useBankingSystem } from '../Context/UserContext';
import axios from '../Utills/AxiosWithJWT.js';
import { BsBank, BsBoxArrowRight, BsPeople, BsCreditCard, BsClipboardData, BsCheckCircleFill } from 'react-icons/bs';

const AdminPage = () => {
  const navigateTo = useNavigate();
  const { BASE_URL, userDetails } = useBankingSystem();
  
  // Navigation tabs
  const [activeTab, setActiveTab] = useState("requests"); // requests, accounts, transactions
  
  // Data states
  const [requests, setRequests] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  
  // Select state for each request's account type
  const [selectedAccTypes, setSelectedAccTypes] = useState({});

  const fetchRequests = async () => {
    try {
      const resp = await axios.get(`${BASE_URL}/account/getallreq`);
      setRequests(resp.data || []);
      // Initialize account type selection to Savings for all requests
      const initialTypes = {};
      (resp.data || []).forEach(req => {
        initialTypes[req.userId] = "Savings";
      });
      setSelectedAccTypes(initialTypes);
    } catch (error) {
      console.error(error);
      toast.error("Failed to fetch pending requests");
    }
  };

  const fetchAccounts = async () => {
    try {
      // Endpoint returns wrap list of users, all=2 returns all users
      const resp = await axios.get(`${BASE_URL}/account/accounts/2`);
      // The API wraps all users in an array inside an array or directly, let's flat it
      const data = resp.data?.[0] || resp.data || [];
      setAccounts(data);
    } catch (error) {
      console.error(error);
      toast.error("Failed to fetch accounts");
    }
  };

  const fetchTransactions = async () => {
    try {
      const resp = await axios.get(`${BASE_URL}/transactions/transaction`);
      setTransactions(resp.data || []);
    } catch (error) {
      console.error(error);
      toast.error("Failed to fetch transactions");
    }
  };

  useEffect(() => {
    if (!sessionStorage.getItem("jwtToken")) {
      navigateTo("/auth");
      return;
    }
    
    // Check if roles are populated
    if (userDetails) {
      if (userDetails.role !== "ADMIN") {
        navigateTo("/dashboard");
        return;
      }
      
      // Fetch data based on active tab
      if (activeTab === "requests") fetchRequests();
      if (activeTab === "accounts") fetchAccounts();
      if (activeTab === "transactions") fetchTransactions();
    }
  }, [userDetails, activeTab]);

  const handleApprove = async (userId) => {
    const accountType = selectedAccTypes[userId] || "Savings";
    try {
      await axios.post(`${BASE_URL}/account/create/${userId}`, { accountType });
      toast.success("Account Approved and Created Successfully!");
      fetchRequests();
    } catch (error) {
      console.error(error);
      toast.error(error.response?.data || "Failed to approve request");
    }
  };

  const handleSignOut = () => {
    sessionStorage.clear();
    navigateTo("/auth");
    toast.success("Signed Out Successfully!");
  };

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col">
      {/* Navigation Header */}
      <nav className="bg-white shadow-md px-6 py-4 flex justify-between items-center">
        <div className="flex items-center space-x-2 flex-1">
          <BsBank className="text-2xl text-indigo-600" />
          <h1 className="text-xl font-bold text-gray-800">Admin Control Panel</h1>
          <span className="bg-indigo-100 text-indigo-800 text-xs px-2 py-1 rounded ml-3 font-semibold">ADMIN</span>
        </div>
        <div className="flex items-center space-x-4">
          <button
            onClick={() => navigateTo("/dashboard")}
            className="text-gray-600 hover:text-indigo-600 font-semibold text-sm transition-colors mr-2"
          >
            User Dashboard View
          </button>
          <button
            onClick={handleSignOut}
            className="flex items-center space-x-2 bg-gray-100 hover:bg-gray-200 px-4 py-2 rounded-lg transition-colors text-sm font-semibold"
          >
            <BsBoxArrowRight />
            <span>Sign Out</span>
          </button>
        </div>
      </nav>

      {/* Main Admin Body */}
      <div className="flex-1 max-w-6xl w-full mx-auto p-6">
        {/* Navigation Tabs */}
        <div className="flex border-b border-gray-200 mb-6 bg-white rounded-lg shadow-sm p-1">
          <button
            onClick={() => setActiveTab("requests")}
            className={`flex-1 py-3 flex items-center justify-center space-x-2 rounded-md font-semibold transition-all ${
              activeTab === "requests"
                ? "bg-indigo-600 text-white shadow-md"
                : "text-gray-600 hover:text-gray-800"
            }`}
          >
            <BsPeople className="text-lg" />
            <span>Account Requests ({requests.length})</span>
          </button>
          <button
            onClick={() => setActiveTab("accounts")}
            className={`flex-1 py-3 flex items-center justify-center space-x-2 rounded-md font-semibold transition-all ${
              activeTab === "accounts"
                ? "bg-indigo-600 text-white shadow-md"
                : "text-gray-600 hover:text-gray-800"
            }`}
          >
            <BsCreditCard className="text-lg" />
            <span>User Accounts</span>
          </button>
          <button
            onClick={() => setActiveTab("transactions")}
            className={`flex-1 py-3 flex items-center justify-center space-x-2 rounded-md font-semibold transition-all ${
              activeTab === "transactions"
                ? "bg-indigo-600 text-white shadow-md"
                : "text-gray-600 hover:text-gray-800"
            }`}
          >
            <BsClipboardData className="text-lg" />
            <span>All Transactions ({transactions.length})</span>
          </button>
        </div>

        {/* Tab Contents */}
        {activeTab === "requests" && (
          <div className="bg-white rounded-xl shadow-md p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4 border-b pb-2">Pending Account Opening Requests</h2>
            {requests.length === 0 ? (
              <p className="text-gray-500 text-center py-8">No pending account requests at the moment.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User Details</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Documents Info</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Configure Account</th>
                      <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {requests.map((user) => (
                      <tr key={user.userId}>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-semibold text-gray-900">{user.firstname} {user.lastname}</div>
                          <div className="text-xs text-gray-500">{user.email}</div>
                          <div className="text-xs text-gray-500">Phone: {user.userdetails?.mobile}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-xs text-gray-900"><strong>Aadhar:</strong> {user.userdetails?.adhaar}</div>
                          <div className="text-xs text-gray-900"><strong>PAN:</strong> {user.userdetails?.pan}</div>
                          <div className="text-xs text-gray-500">Gender: {user.userdetails?.gender}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <select
                            value={selectedAccTypes[user.userId] || "Savings"}
                            onChange={(e) => setSelectedAccTypes({...selectedAccTypes, [user.userId]: e.target.value})}
                            className="text-sm px-2 py-1 border border-gray-300 rounded focus:ring-indigo-500"
                          >
                            <option value="Savings">Savings Account</option>
                            <option value="Current">Current Account</option>
                          </select>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          <button
                            onClick={() => handleApprove(user.userId)}
                            className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-1.5 px-4 rounded text-xs transition-colors"
                          >
                            Approve & Open
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === "accounts" && (
          <div className="bg-white rounded-xl shadow-md p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4 border-b pb-2">All Registered Bank Accounts</h2>
            {accounts.length === 0 ? (
              <p className="text-gray-500 text-center py-8">No registered users found.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User Details</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status & Role</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Bank Accounts</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {accounts.map((user) => (
                      <tr key={user.userId}>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-semibold text-gray-900">{user.firstname} {user.lastname}</div>
                          <div className="text-xs text-gray-500">{user.email}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-xs font-bold uppercase">{user.role}</div>
                          <div className="text-xs text-gray-500">
                            {user.emailVerified ? (
                              <span className="text-green-600 font-semibold">Verified</span>
                            ) : (
                              <span className="text-red-500 font-semibold">Unverified</span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          {user.accounts && user.accounts.length > 0 ? (
                            <div className="space-y-2">
                              {user.accounts.map((acc) => {
                                const accNo = acc.accountno;
                                return (
                                  <div key={accNo} className="border bg-gray-50 p-3 rounded flex flex-col space-y-2">
                                    <div className="flex justify-between items-center text-xs">
                                      <span><strong>No:</strong> {accNo} ({acc.accountType})</span>
                                      <span className="font-semibold text-green-700">₹{acc.balance?.toLocaleString()}</span>
                                    </div>
                                    <form 
                                      onSubmit={async (e) => {
                                        e.preventDefault();
                                        const amount = e.target.amount.value;
                                        if (!amount || amount <= 0) {
                                          toast.error("Please enter a valid amount");
                                          return;
                                        }
                                        try {
                                          await axios.post(`${BASE_URL}/deposit`, {
                                            accountno: accNo,
                                            amount: parseFloat(amount)
                                          });
                                          toast.success("Credited ₹" + amount + " successfully!");
                                          e.target.amount.value = "";
                                          fetchAccounts();
                                        } catch (err) {
                                          toast.error("Credit failed");
                                        }
                                      }} 
                                      className="flex items-center space-x-2 pt-1"
                                    >
                                      <input 
                                        type="number" 
                                        name="amount" 
                                        placeholder="Amount" 
                                        className="text-xs px-2 py-1 border rounded w-24 focus:outline-none"
                                        required 
                                      />
                                      <button 
                                        type="submit" 
                                        className="bg-green-600 hover:bg-green-700 text-white font-bold py-1 px-3 rounded text-xs transition-colors"
                                      >
                                        Deposit/Credit
                                      </button>
                                    </form>
                                  </div>
                                );
                              })}
                            </div>
                          ) : (
                            <span className="text-xs text-gray-400">No bank accounts opened</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === "transactions" && (
          <div className="bg-white rounded-xl shadow-md p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4 border-b pb-2">Global System Transactions</h2>
            {transactions.length === 0 ? (
              <p className="text-gray-500 text-center py-8">No transaction logs available.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">TX ID</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date & Time</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Sender / Receiver Acc</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Details</th>
                      <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {transactions.map((tx) => (
                      <tr key={tx.transactionId}>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-mono">
                          #{tx.transactionId}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-xs text-gray-900">{tx.transactionDate}</div>
                          <div className="text-xs text-gray-500">{tx.transactionTime}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-xs text-gray-900"><strong>From:</strong> {tx.fromAccount}</div>
                          <div className="text-xs text-gray-900"><strong>To:</strong> {tx.toAccount}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-xs font-medium text-gray-900">{tx.description}</div>
                          <div className="text-xs text-gray-500">
                            Status: <span className="text-green-600 font-semibold">{tx.transactionStatus}</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-bold text-gray-900">
                          ₹{tx.amount?.toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminPage;