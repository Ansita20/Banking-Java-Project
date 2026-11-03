import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from "axios";
import { toast } from 'react-hot-toast';
import { useBankingSystem } from '../Context/UserContext';
import banklogo from "../../assets/images/cblogo.png";

const AuthPage = () => {
  const navigateTo = useNavigate();
  const { BASE_URL, gettingAUser } = useBankingSystem();
  const [isLogin, setIsLogin] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  
  // Login form state
  const [loginData, setLoginData] = useState({
    email: "",
    password: ""
  });
  
  // Signup form state
  const [signupData, setSignupData] = useState({
    firstname: "",
    lastname: "",
    email: "",
    password: ""
  });

  const TOKEN_EXPIRY_DURATION = 15 * 60 * 1000;

  const handleLoginChange = (e) => {
    setLoginData({ ...loginData, [e.target.name]: e.target.value });
  };

  const handleSignupChange = (e) => {
    setSignupData({ ...signupData, [e.target.name]: e.target.value });
  };

  const submitLogin = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    
    try {
      const resp = await axios.post(`${BASE_URL}/api/v1/login`, loginData);
      
      if (resp.data.user.emailVerified === false) {
        toast.error("Email is not verified!");
        navigateTo("/signup/otp");
        setIsLoading(false);
        return;
      }

      sessionStorage.setItem("jwtToken", resp.data.jwtToken);
      sessionStorage.setItem("userId", resp.data.user.userId);
      
      setTimeout(() => {
        sessionStorage.clear();
        navigateTo("/auth");
        toast.error("Session timed out, please re-login");
      }, TOKEN_EXPIRY_DURATION);

      if (resp.status === 200) {
        gettingAUser();
        navigateTo("/dashboard");
        toast.success("Login Successful!");
      }
    } catch (error) {
      console.log(error);
      if (error.response?.data?.message) {
        toast.error(error.response.data.message);
      } else if (error.response?.status === 401) {
        toast.error("Invalid email or password");
      } else {
        toast.error("Login failed. Please check your connection and try again.");
      }
    } finally {
      setIsLoading(false);
    }
  };
  
  const handleForgotPassword = async () => {
    // For now, just show a message that this feature is not available
    toast.error("Password reset is not available in this simplified version. Please contact support.");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const { firstname, lastname, email, password } = signupData;

    if (!firstname || !lastname || !email || !password) {
      toast.error("Please fill all fields");
      return;
    }

    if (password.length < 8) {
      toast.error("Password should be at least 8 characters!");
      return;
    }
    
    setIsLoading(true);
    
    try {
      const resp = await axios.post(`${BASE_URL}/api/v1/signup`, {
        firstname,
        lastname,
        email,
        password
      });

      sessionStorage.setItem("userId", resp.data.userId);

      if (resp.status === 200) {
        navigateTo("/signup/otp");
        toast.success("Registration Successful, Please Verify Email!");
      }
    } catch (error) {
      console.log(error);
      if (error.response?.status === 409) {
        toast.error("This email is already registered!");
      } else {
        toast.error("Registration failed. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <img className="h-16 w-auto" src={banklogo} alt="Bank Logo" />
        </div>
        <h2 className="mt-6 text-center text-3xl font-bold tracking-tight text-gray-900">
          {isLogin ? "Welcome Back" : "Create Your Account"}
        </h2>
        <p className="mt-2 text-center text-sm text-gray-600">
          {isLogin ? "Sign in to your account" : "Join our banking system"}
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow-xl rounded-lg sm:px-10">
          {/* Toggle between Login and Signup */}
          <div className="flex mb-6 bg-gray-100 rounded-lg p-1">
            <button
              onClick={() => setIsLogin(true)}
              className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all ${
                isLogin 
                  ? 'bg-indigo-600 text-white shadow-md' 
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Login
            </button>
            <button
              onClick={() => setIsLogin(false)}
              className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all ${
                !isLogin 
                  ? 'bg-indigo-600 text-white shadow-md' 
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Sign Up
            </button>
          </div>

          {isLogin ? (
            // Login Form
            <form onSubmit={submitLogin} className="space-y-6">
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                  Email address
                </label>
                <div className="mt-1">
                  <input
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    value={loginData.email}
                    onChange={handleLoginChange}
                    required
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="you@example.com"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                  Password
                </label>
                <div className="mt-1">
                  <input
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    value={loginData.password}
                    onChange={handleLoginChange}
                    required
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="••••••••"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end">
                <div className="text-sm">
                  <button
                    type="button"
                    onClick={() => navigateTo("/auth")}
                    className="font-medium text-indigo-600 hover:text-indigo-500"
                  >
                    Back to Sign In
                  </button>
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
                >
                  {isLoading ? "Signing in..." : "Sign in"}
                </button>
              </div>
            </form>
          ) : (
            // Signup Form
            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="firstname" className="block text-sm font-medium text-gray-700">
                    First Name
                  </label>
                  <div className="mt-1">
                    <input
                      id="firstname"
                      name="firstname"
                      type="text"
                      required
                      value={signupData.firstname}
                      onChange={handleSignupChange}
                      className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      placeholder="John"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="lastname" className="block text-sm font-medium text-gray-700">
                    Last Name
                  </label>
                  <div className="mt-1">
                    <input
                      id="lastname"
                      name="lastname"
                      type="text"
                      required
                      value={signupData.lastname}
                      onChange={handleSignupChange}
                      className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      placeholder="Doe"
                    />
                  </div>
                </div>
              </div>

              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                  Email address
                </label>
                <div className="mt-1">
                  <input
                    id="email"
                    name="email"
                    type="email"
                    required
                    value={signupData.email}
                    onChange={handleSignupChange}
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="you@example.com"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                  Password
                </label>
                <div className="mt-1">
                  <input
                    id="password"
                    name="password"
                    type="password"
                    required
                    value={signupData.password}
                    onChange={handleSignupChange}
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="•••••••• (min 8 characters)"
                  />
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
                >
                  {isLoading ? "Creating account..." : "Create account"}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default AuthPage;