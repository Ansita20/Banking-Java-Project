import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useBankingSystem } from '../Context/UserContext';
import axios from '../Utills/AxiosWithJWT.js';
import { BsBank, BsArrowLeftRight, BsCashCoin, BsBoxArrowRight, BsPersonFill, BsCheckCircle } from 'react-icons/bs';

const AccountPage = () => {
  const navigateTo = useNavigate();
  const { BASE_URL, userDetails, gettingAUser, setUser: setUserDetails } = useBankingSystem();
  
  const [balance, setBalance] = useState(0);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [showTransfer, setShowTransfer] = useState(false);
  const [showCredit, setShowCredit] = useState(false);
  const [showTransactions, setShowTransactions] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [showThankYou, setShowThankYou] = useState(false);
  const [lastTransaction, setLastTransaction] = useState(null);
  
  // Transfer form state
  const [selectedBeneficiary, setSelectedBeneficiary] = useState("");
  const [transferAmount, setTransferAmount] = useState("");
  const [transferDescription, setTransferDescription] = useState("");
  
  // Beneficiary form state
  const [beneName, setBeneName] = useState("");
  const [beneAccountNo, setBeneAccountNo] = useState("");
  const [beneRelation, setBeneRelation] = useState("Friend");
  
  // Credit form state
  const [creditAmount, setCreditAmount] = useState("");

  // Profile form state
  const [profileData, setProfileData] = useState({
    address: "",
    city: "",
    state: "",
    pin: "",
    adhaar: "",
    pan: "",
    gender: "",
    mobile: "",
    dateOfBirth: ""
  });

  useEffect(() => {
    if (!sessionStorage.getItem("jwtToken")) {
      navigateTo("/auth");
    }
    gettingAUser();
  }, []);

  useEffect(() => {
    if (userDetails?.userdetails) {
      setProfileData({
        address: userDetails.userdetails.address || "",
        city: userDetails.userdetails.city || "",
        state: userDetails.userdetails.state || "",
        pin: userDetails.userdetails.pin || "",
        adhaar: userDetails.userdetails.adhaar || "",
        pan: userDetails.userdetails.pan || "",
        gender: userDetails.userdetails.gender || "",
        mobile: userDetails.userdetails.mobile || "",
        dateOfBirth: userDetails.userdetails.dateOfBirth || ""
      });
    }
  }, [userDetails]);

  // Redirect admin users to admin page
  useEffect(() => {
    if (userDetails?.role === "ADMIN") {
      navigateTo("/admin");
    }
  }, [userDetails, navigateTo]);

  const accountNo = userDetails?.accounts?.[0]?.accountno;
  const userName = userDetails ? `${userDetails.firstname} ${userDetails.lastname}` : "";

  // Fetch balance
  const fetchBalance = async () => {
    if (!accountNo) return;
    try {
      const resp = await axios.get(`${BASE_URL}/account/checkbal/${accountNo}`);
      setBalance(resp?.data?.[0]?.balance || 0);
    } catch (error) {
      console.log(error);
      toast.error("Failed to fetch balance");
    }
  };

  // Fetch beneficiaries
  const fetchBeneficiaries = async () => {
    if (!userDetails?.userId) return;
    try {
      const resp = await axios.get(`${BASE_URL}/beneficiaries/user/${userDetails?.userId}`);
      setBeneficiaries(resp.data || []);
    } catch (error) {
      console.log(error);
    }
  };

  // Fetch transactions
  const fetchTransactions = async () => {
    if (!accountNo) return;
    try {
      const resp = await axios.get(`${BASE_URL}/transactions/bankaccount/${accountNo}`);
      setTransactions(resp.data || []);
    } catch (error) {
      console.log(error);
    }
  };

  // Handle fund transfer
  const handleFundTransfer = async (e) => {
    e.preventDefault();
    
    if (!selectedBeneficiary || !transferAmount) {
      toast.error("Please fill all fields");
      return;
    }

    try {
      const resp = await axios.post(`${BASE_URL}/fund/transfer`, {
        accountno: accountNo
      }, {
        params: {
          toAccount: selectedBeneficiary,
          amount: transferAmount,
          description: transferDescription
        }
      });

      if (resp.status === 200) {
        setLastTransaction({
          type: 'Transfer',
          amount: transferAmount,
          to: selectedBeneficiary,
          description: transferDescription
        });
        setShowThankYou(true);
        setTransferAmount("");
        setTransferDescription("");
        fetchBalance();
        fetchTransactions();
      }
    } catch (error) {
      console.log(error);
      toast.error("Transfer failed");
    }
  };

  // Handle adding a new beneficiary
  const handleAddBeneficiary = async (e) => {
    e.preventDefault();
    if (!beneName || !beneAccountNo) {
      toast.error("Please fill all fields");
      return;
    }

    try {
      const resp = await axios.post(`${BASE_URL}/beneficiaries/create/${userDetails?.userId}`, {
        beneficiaryname: beneName,
        beneaccountno: parseInt(beneAccountNo),
        relation: beneRelation
      });

      // Backend returns either the created beneficiary, "Account does not Exists", or "This account cannot be added", or "Already Exists"
      if (resp.status === 200) {
        if (resp.data === "Account does not Exists") {
          toast.error("Beneficiary account number does not exist in our bank!");
        } else if (resp.data === "This account cannot be added") {
          toast.error("You cannot add your own account as a beneficiary!");
        } else if (resp.data === "Already Exists") {
          toast.error("This beneficiary is already in your list!");
        } else {
          toast.success("Beneficiary Added Successfully!");
          setBeneName("");
          setBeneAccountNo("");
          fetchBeneficiaries();
        }
      }
    } catch (error) {
      console.log(error);
      toast.error(error.response?.data || "Failed to add beneficiary");
    }
  };

  // Handle credit money
  const handleCreditMoney = async (e) => {
    e.preventDefault();
    
    if (!creditAmount) {
      toast.error("Please enter amount");
      return;
    }

    try {
      const resp = await axios.post(`${BASE_URL}/deposit`, {
        accountno: accountNo,
        amount: creditAmount
      });

      if (resp.status === 200) {
        setLastTransaction({
          type: 'Credit',
          amount: creditAmount
        });
        setShowThankYou(true);
        setCreditAmount("");
        fetchBalance();
        fetchTransactions();
      }
    } catch (error) {
      console.log(error);
      toast.error("Credit failed");
    }
  };

  // Handle account opening request
  const handleAccountOpenRequest = async () => {
    if (!userDetails?.userdetails?.adhaar || !userDetails?.userdetails?.pan || 
        !userDetails?.userdetails?.mobile || !userDetails?.userdetails?.gender) {
      toast.error("Please Update Profile First");
      return;
    }

    try {
      // 1. Submit the account opening request flag
      await axios.put(`${BASE_URL}/api/v1/user/acopreq/${userDetails?.userId}`);
      
      // 2. Instantly approve and create the Savings Account
      await axios.post(`${BASE_URL}/account/create/${userDetails?.userId}`, {
        accountType: "Savings"
      });
      
      // 3. Reload/get updated user details to refresh dashboard
      await gettingAUser();
      
      toast.success("Bank Account Opened Successfully with ₹10,000 balance!");
    } catch (error) {
      console.log(error);
      toast.error("Failed to automatically open bank account: " + (error.response?.data || error.message));
    }
  };

  // Handle profile update
  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    
    if (!profileData.adhaar || !profileData.pan || !profileData.mobile || !profileData.gender) {
      toast.error("Please fill all mandatory fields (Aadhar, PAN, Mobile, Gender)");
      return;
    }

    if (profileData.adhaar.length !== 12) {
      toast.error("Aadhar must be 12 digits!");
      return;
    }

    if (profileData.pan.length !== 10) {
      toast.error("PAN must be 10 characters!");
      return;
    }

    if (profileData.mobile.length !== 10) {
      toast.error("Mobile number must be 10 digits!");
      return;
    }

    const payload = {
      ...profileData,
      dateOfBirth: profileData.dateOfBirth ? profileData.dateOfBirth : null
    };

    try {
      const resp = await axios.put(`${BASE_URL}/api/v1/user/updateprofile/${userDetails.userId}`, payload);
      setUserDetails(resp.data);
      toast.success("Profile Updated Successfully!");
      setShowProfile(false);
    } catch (error) {
      console.log(error);
      toast.error("Failed to update profile: " + (error.response?.data || "Invalid inputs"));
    }
  };

  const handleSignOut = () => {
    sessionStorage.clear();
    navigateTo("/auth");
    toast.success("Signed Out Successfully!");
  };

  // Load data when account exists
  useEffect(() => {
    if (accountNo) {
      fetchBalance();
      fetchBeneficiaries();
      fetchTransactions();
    }
  }, [accountNo]);

  // If no account, show account opening request and profile form
  if (!accountNo) {
    return (
      <div className="min-h-screen bg-gray-100">
        <nav className="bg-white shadow-md px-6 py-4 flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <BsBank className="text-2xl text-indigo-600" />
            <h1 className="text-xl font-bold text-gray-800">Online Banking</h1>
          </div>
          <div className="flex items-center space-x-4">
            {userDetails?.role === "ADMIN" && (
              <button
                onClick={() => navigateTo("/admin")}
                className="flex items-center space-x-2 bg-indigo-100 hover:bg-indigo-200 text-indigo-800 px-4 py-2 rounded-lg transition-colors font-semibold"
              >
                <BsBank />
                <span>Admin Panel</span>
              </button>
            )}
            <button
              onClick={handleSignOut}
              className="flex items-center space-x-2 bg-gray-100 hover:bg-gray-200 px-4 py-2 rounded-lg transition-colors"
            >
              <BsBoxArrowRight />
              <span>Sign Out</span>
            </button>
          </div>
        </nav>

        <div className="max-w-4xl mx-auto py-8 px-4">
          <div className="bg-white rounded-xl shadow-md p-6 mb-6">
            <h2 className="text-2xl font-bold text-gray-800 mb-2">Welcome to Online Banking, {userDetails?.firstname}!</h2>
            <p className="text-gray-600 mb-4">
              To apply for a bank account, please complete your profile details below and submit your request.
            </p>
            <div className="border-t pt-4">
              <h3 className="font-semibold text-lg text-gray-800 mb-2">Account Request Status:</h3>
              {userDetails?.accountopenningreq ? (
                <div className="flex items-center space-x-2 text-yellow-600 bg-yellow-50 p-3 rounded-lg">
                  <span className="animate-pulse">●</span>
                  <span><strong>Pending Approval:</strong> Your request has been sent successfully. Please contact an admin or log in to the admin panel to approve your account.</span>
                </div>
              ) : (
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-gray-50 p-4 rounded-lg">
                  <span className="text-gray-600">Please make sure to fill out all mandatory (*) fields in the form below before submitting.</span>
                  <button
                    onClick={handleAccountOpenRequest}
                    className="bg-indigo-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-indigo-700 transition-colors whitespace-nowrap"
                  >
                    Apply for Account Opening
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6">
            <h3 className="text-xl font-bold text-gray-800 mb-4">Update Profile Details</h3>
            <form onSubmit={handleProfileUpdate} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                  <input
                    type="text"
                    value={profileData.address}
                    onChange={(e) => setProfileData({...profileData, address: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                  <input
                    type="text"
                    value={profileData.city}
                    onChange={(e) => setProfileData({...profileData, city: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
                  <input
                    type="text"
                    value={profileData.state}
                    onChange={(e) => setProfileData({...profileData, state: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">PIN Code</label>
                  <input
                    type="text"
                    value={profileData.pin}
                    onChange={(e) => setProfileData({...profileData, pin: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Aadhar Card Number *</label>
                  <input
                    type="text"
                    value={profileData.adhaar}
                    onChange={(e) => setProfileData({...profileData, adhaar: e.target.value})}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">PAN Card Number *</label>
                  <input
                    type="text"
                    value={profileData.pan}
                    onChange={(e) => setProfileData({...profileData, pan: e.target.value.toUpperCase()})}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Mobile Number *</label>
                  <input
                    type="tel"
                    value={profileData.mobile}
                    onChange={(e) => setProfileData({...profileData, mobile: e.target.value})}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
                  <input
                    type="date"
                    value={profileData.dateOfBirth}
                    onChange={(e) => setProfileData({...profileData, dateOfBirth: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Gender</label>
                <div className="flex space-x-4">
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="gender"
                      value="M"
                      checked={profileData.gender === 'M'}
                      onChange={(e) => setProfileData({...profileData, gender: e.target.value})}
                      className="mr-2"
                    />
                    Male
                  </label>
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="gender"
                      value="F"
                      checked={profileData.gender === 'F'}
                      onChange={(e) => setProfileData({...profileData, gender: e.target.value})}
                      className="mr-2"
                    />
                    Female
                  </label>
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="gender"
                      value="O"
                      checked={profileData.gender === 'O'}
                      onChange={(e) => setProfileData({...profileData, gender: e.target.value})}
                      className="mr-2"
                    />
                    Other
                  </label>
                </div>
              </div>

              <div className="flex space-x-4 pt-4">
                <button
                  type="submit"
                  className="w-full bg-indigo-600 text-white py-3 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
                >
                  Save Profile Details
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    );
  }

  // Thank You Page
  if (showThankYou) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="bg-white p-8 rounded-xl shadow-lg text-center max-w-md">
          <BsCheckCircle className="text-6xl text-green-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-800 mb-4">Transaction Successful!</h2>
          <div className="text-left bg-gray-50 p-4 rounded-lg mb-6">
            <p><strong>Type:</strong> {lastTransaction?.type}</p>
            <p><strong>Amount:</strong> ₹{lastTransaction?.amount}</p>
            {lastTransaction?.to && <p><strong>To Account:</strong> {lastTransaction?.to}</p>}
            {lastTransaction?.description && <p><strong>Description:</strong> {lastTransaction?.description}</p>}
          </div>
          <button
            onClick={() => setShowThankYou(false)}
            className="bg-indigo-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Navigation */}
      <nav className="bg-white shadow-md px-6 py-4 flex justify-between items-center">
        <div className="flex items-center space-x-2">
          <BsBank className="text-2xl text-indigo-600" />
          <h1 className="text-xl font-bold text-gray-800">Online Banking</h1>
        </div>
        <div className="flex items-center space-x-4">
          {userDetails?.role === "ADMIN" && (
            <button
              onClick={() => navigateTo("/admin")}
              className="flex items-center space-x-2 bg-indigo-100 hover:bg-indigo-200 text-indigo-800 px-4 py-2 rounded-lg transition-colors font-semibold"
            >
              <BsBank />
              <span>Admin Panel</span>
            </button>
          )}
          <button
            onClick={() => setShowProfile(true)}
            className="flex items-center space-x-2 bg-gray-100 hover:bg-gray-200 px-4 py-2 rounded-lg transition-colors"
          >
            <BsPersonFill />
            <span>Profile</span>
          </button>
          <button
            onClick={handleSignOut}
            className="flex items-center space-x-2 bg-gray-100 hover:bg-gray-200 px-4 py-2 rounded-lg transition-colors"
          >
            <BsBoxArrowRight />
            <span>Sign Out</span>
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <div className="p-6">
        {/* Welcome Section */}
        <div className="bg-white rounded-xl shadow-md p-6 mb-6">
          <h2 className="text-2xl font-bold text-gray-800">Welcome, {userName}</h2>
          <p className="text-gray-600">Account No: {accountNo}</p>
        </div>

        {/* Balance Card */}
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 rounded-xl shadow-md p-6 mb-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm opacity-80">Available Balance</p>
              <h3 className="text-3xl font-bold">₹{balance?.toLocaleString() || 0}</h3>
            </div>
            <BsBank className="text-4xl opacity-30" />
          </div>
        </div>

        {/* Action Buttons */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <button
            onClick={() => {
              setShowTransfer(true);
              setShowCredit(false);
              setShowTransactions(false);
              setShowProfile(false);
            }}
            className="bg-white rounded-xl shadow-md p-6 flex items-center space-x-4 hover:shadow-lg transition-shadow"
          >
            <div className="bg-indigo-100 p-3 rounded-full">
              <BsArrowLeftRight className="text-2xl text-indigo-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-800">Transfer Money</h3>
              <p className="text-sm text-gray-600">Send money to beneficiaries</p>
            </div>
          </button>

          <button
            onClick={() => {
              setShowCredit(true);
              setShowTransfer(false);
              setShowTransactions(false);
              setShowProfile(false);
            }}
            className="bg-white rounded-xl shadow-md p-6 flex items-center space-x-4 hover:shadow-lg transition-shadow"
          >
            <div className="bg-green-100 p-3 rounded-full">
              <BsCashCoin className="text-2xl text-green-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-800">Credit Money</h3>
              <p className="text-sm text-gray-600">Add money to your account</p>
            </div>
          </button>

          <button
            onClick={() => {
              setShowTransactions(true);
              setShowTransfer(false);
              setShowCredit(false);
              setShowProfile(false);
            }}
            className="bg-white rounded-xl shadow-md p-6 flex items-center space-x-4 hover:shadow-lg transition-shadow"
          >
            <div className="bg-blue-100 p-3 rounded-full">
              <BsBank className="text-2xl text-blue-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-800">Transactions</h3>
              <p className="text-sm text-gray-600">View transaction history</p>
            </div>
          </button>
        </div>

        {/* Profile Form */}
        {showProfile && (
          <div className="bg-white rounded-xl shadow-md p-6 mb-6">
            <h3 className="text-xl font-bold text-gray-800 mb-4">Update Profile</h3>
            <form onSubmit={handleProfileUpdate} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                  <input
                    type="text"
                    value={profileData.address}
                    onChange={(e) => setProfileData({...profileData, address: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                  <input
                    type="text"
                    value={profileData.city}
                    onChange={(e) => setProfileData({...profileData, city: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
                  <input
                    type="text"
                    value={profileData.state}
                    onChange={(e) => setProfileData({...profileData, state: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">PIN Code</label>
                  <input
                    type="text"
                    value={profileData.pin}
                    onChange={(e) => setProfileData({...profileData, pin: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Aadhar Card Number *</label>
                  <input
                    type="text"
                    value={profileData.adhaar}
                    onChange={(e) => setProfileData({...profileData, adhaar: e.target.value})}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">PAN Card Number *</label>
                  <input
                    type="text"
                    value={profileData.pan}
                    onChange={(e) => setProfileData({...profileData, pan: e.target.value.toUpperCase()})}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Mobile Number *</label>
                  <input
                    type="tel"
                    value={profileData.mobile}
                    onChange={(e) => setProfileData({...profileData, mobile: e.target.value})}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
                  <input
                    type="date"
                    value={profileData.dateOfBirth}
                    onChange={(e) => setProfileData({...profileData, dateOfBirth: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Gender</label>
                <div className="flex space-x-4">
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="gender"
                      value="M"
                      checked={profileData.gender === 'M'}
                      onChange={(e) => setProfileData({...profileData, gender: e.target.value})}
                      className="mr-2"
                    />
                    Male
                  </label>
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="gender"
                      value="F"
                      checked={profileData.gender === 'F'}
                      onChange={(e) => setProfileData({...profileData, gender: e.target.value})}
                      className="mr-2"
                    />
                    Female
                  </label>
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="gender"
                      value="O"
                      checked={profileData.gender === 'O'}
                      onChange={(e) => setProfileData({...profileData, gender: e.target.value})}
                      className="mr-2"
                    />
                    Other
                  </label>
                </div>
              </div>

              <div className="flex space-x-4">
                <button
                  type="submit"
                  className="flex-1 bg-indigo-600 text-white py-2 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
                >
                  Update Profile
                </button>
                <button
                  type="button"
                  onClick={() => setShowProfile(false)}
                  className="flex-1 bg-gray-200 text-gray-800 py-2 rounded-lg font-semibold hover:bg-gray-300 transition-colors"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Transfer & Beneficiary Forms */}
        {showTransfer && (
          <div className="bg-white rounded-xl shadow-md p-6 mb-6">
            <h3 className="text-xl font-bold text-gray-800 mb-6 border-b pb-2">Fund Transfers & Beneficiaries</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              {/* Add Beneficiary Column */}
              <div className="bg-gray-50 p-5 rounded-xl border border-gray-200">
                <h4 className="text-lg font-semibold text-gray-700 mb-4">Add New Beneficiary</h4>
                <form onSubmit={handleAddBeneficiary} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Beneficiary Name</label>
                    <input
                      type="text"
                      value={beneName}
                      onChange={(e) => setBeneName(e.target.value)}
                      required
                      placeholder="e.g. Jane Doe"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Account Number</label>
                    <input
                      type="number"
                      value={beneAccountNo}
                      onChange={(e) => setBeneAccountNo(e.target.value)}
                      required
                      placeholder="Enter bank account number"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Relation</label>
                    <select
                      value={beneRelation}
                      onChange={(e) => setBeneRelation(e.target.value)}
                      required
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    >
                      <option value="Self">Self</option>
                      <option value="Friend">Friend</option>
                      <option value="Family">Family</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                  <button
                    type="submit"
                    className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded-lg font-semibold transition-colors"
                  >
                    Add Beneficiary
                  </button>
                </form>
              </div>

              {/* Transfer Money Column */}
              <div className="bg-gray-50 p-5 rounded-xl border border-gray-200">
                <h4 className="text-lg font-semibold text-gray-700 mb-4">Send Money</h4>
                <form onSubmit={handleFundTransfer} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      From Account: <span className="font-mono text-gray-900 font-bold">{accountNo}</span>
                    </label>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      To Beneficiary
                    </label>
                    <select
                      value={selectedBeneficiary}
                      onChange={(e) => setSelectedBeneficiary(e.target.value)}
                      required
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    >
                      <option value="">Select Beneficiary</option>
                      {beneficiaries.map((b) => (
                        <option key={b.beneficiaryid} value={b.beneaccountno}>
                          {b.beneficiaryname} ({b.relation}) - {b.beneaccountno}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Amount
                    </label>
                    <input
                      type="number"
                      value={transferAmount}
                      onChange={(e) => setTransferAmount(e.target.value)}
                      required
                      min="1"
                      placeholder="Enter amount"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Description
                    </label>
                    <input
                      type="text"
                      value={transferDescription}
                      onChange={(e) => setTransferDescription(e.target.value)}
                      placeholder="Optional description"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                  </div>

                  <button
                    type="submit"
                    className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded-lg font-semibold transition-colors"
                  >
                    Transfer Funds
                  </button>
                </form>
              </div>
            </div>
          </div>
        )}

        {/* Credit Money Form */}
        {showCredit && (
          <div className="bg-white rounded-xl shadow-md p-6 mb-6">
            <h3 className="text-xl font-bold text-gray-800 mb-4">Credit Money</h3>
            <form onSubmit={handleCreditMoney} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  To Account: {accountNo}
                </label>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Amount
                </label>
                <input
                  type="number"
                  value={creditAmount}
                  onChange={(e) => setCreditAmount(e.target.value)}
                  required
                  min="1"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  placeholder="Enter amount to credit"
                />
              </div>

              <button
                type="submit"
                className="w-full bg-green-600 text-white py-2 rounded-lg font-semibold hover:bg-green-700 transition-colors"
              >
                Credit Amount
              </button>
            </form>
          </div>
        )}

        {/* Transactions List */}
        {showTransactions && (
          <div className="bg-white rounded-xl shadow-md p-6 mb-6">
            <h3 className="text-xl font-bold text-gray-800 mb-4">Recent Transactions</h3>
            {transactions.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Amount</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Balance</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {transactions.slice(0, 10).map((t) => (
                      <tr key={t.transactionId}>
                        <td className="px-4 py-2 text-sm text-gray-700">{t.transactionDate}</td>
                        <td className="px-4 py-2 text-sm">
                          <span className={`px-2 py-1 rounded text-xs ${
                            t.fromAccount === accountNo ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
                          }`}>
                            {t.fromAccount === accountNo ? 'Debit' : 'Credit'}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-sm font-medium">
                          {t.fromAccount === accountNo ? `-₹${t.amount}` : `+₹${t.amount}`}
                        </td>
                        <td className="px-4 py-2 text-sm text-gray-700">
                          ₹{t.fromAccount === accountNo ? t.senderBal : t.receiverBal}
                        </td>
                        <td className="px-4 py-2 text-sm">
                          <span className={`px-2 py-1 rounded text-xs ${
                            t.transactionStatus === 'Completed' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {t.transactionStatus}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-gray-600">No transactions found</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AccountPage;