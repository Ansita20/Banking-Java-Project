import React from 'react';
import { NavLink } from 'react-router-dom';
import { BsBank, BsArrowRight } from "react-icons/bs";

const LandingPage = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      {/* Navigation */}
      <nav className="bg-white shadow-md px-6 py-4 flex justify-between items-center">
        <div className="flex items-center space-x-2">
          <BsBank className="text-2xl text-indigo-600" />
          <h1 className="text-xl font-bold text-gray-800">Online Banking</h1>
        </div>
        <div>
          <NavLink
            to="/auth"
            className="bg-indigo-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
          >
            Sign In / Sign Up
          </NavLink>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="flex flex-col items-center justify-center min-h-[80vh] px-6">
        <div className="text-center max-w-2xl">
          <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-6">
            Your Trusted Online Banking Solution
          </h1>
          <p className="text-lg text-gray-600 mb-8">
            Secure, fast, and easy banking. Transfer money, check balances, and manage your finances all in one place.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <NavLink
              to="/auth"
              className="inline-flex items-center justify-center space-x-2 bg-indigo-600 text-white px-8 py-3 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
            >
              <span>Get Started</span>
              <BsArrowRight className="text-lg" />
            </NavLink>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="bg-white py-12 px-6">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-2xl font-bold text-center text-gray-800 mb-8">Our Services</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center p-6">
              <div className="bg-indigo-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <BsBank className="text-2xl text-indigo-600" />
              </div>
              <h3 className="font-semibold text-gray-800 mb-2">Check Balance</h3>
              <p className="text-gray-600 text-sm">View your account balance anytime, anywhere</p>
            </div>
            <div className="text-center p-6">
              <div className="bg-green-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <BsArrowRight className="text-2xl text-green-600" />
              </div>
              <h3 className="font-semibold text-gray-800 mb-2">Transfer Money</h3>
              <p className="text-gray-600 text-sm">Send money to your beneficiaries instantly</p>
            </div>
            <div className="text-center p-6">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <BsBank className="text-2xl text-blue-600" />
              </div>
              <h3 className="font-semibold text-gray-800 mb-2">Transaction History</h3>
              <p className="text-gray-600 text-sm">View all your past transactions</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-800 text-white py-6 px-6">
        <div className="max-w-4xl mx-auto text-center">
          <p className="text-sm">&copy; 2024 Online Banking System. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;