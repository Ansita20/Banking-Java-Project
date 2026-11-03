import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import {
  BsBank, BsArrowLeftRight, BsCashCoin, BsPeople, BsReceipt, BsBell, BsPersonBadge, BsBoxArrowRight, BsGrid,
} from "react-icons/bs";
import api, { errorMessage } from "../Utills/api";
import { useAuth } from "../Context/AuthContext";
import Overview from "./Overview";
import TransferPanel from "./TransferPanel";
import CashPanel from "./CashPanel";
import BeneficiariesPanel from "./BeneficiariesPanel";
import StatementPanel from "./StatementPanel";
import NotificationsPanel from "./NotificationsPanel";
import ProfilePanel from "./ProfilePanel";

const TABS = [
  { id: "overview", label: "Overview", icon: BsGrid },
  { id: "transfer", label: "Transfer money", icon: BsArrowLeftRight },
  { id: "cash", label: "Deposit / Withdraw", icon: BsCashCoin },
  { id: "beneficiaries", label: "Beneficiaries", icon: BsPeople },
  { id: "statement", label: "Statements", icon: BsReceipt },
  { id: "notifications", label: "Notifications", icon: BsBell },
  { id: "profile", label: "Profile & KYC", icon: BsPersonBadge },
];

const Dashboard = () => {
  const navigate = useNavigate();
  const { user, logout, refreshUser } = useAuth();
  const [tab, setTab] = useState("overview");
  const [accounts, setAccounts] = useState([]);
  const [unread, setUnread] = useState(0);

  const loadAccounts = useCallback(async () => {
    try {
      const { data } = await api.get("/api/accounts");
      setAccounts(data);
    } catch (error) {
      toast.error(errorMessage(error, "Could not load accounts"));
    }
  }, []);

  const loadUnread = useCallback(async () => {
    try {
      const { data } = await api.get("/api/notifications/unread-count");
      setUnread(data.unread);
    } catch {
      // Notifications are non-critical; the rest of the app keeps working if the service is down.
    }
  }, []);

  useEffect(() => {
    refreshUser().catch(() => {});
    loadAccounts();
    loadUnread();
    const timer = setInterval(loadUnread, 15000);
    return () => clearInterval(timer);
  }, [refreshUser, loadAccounts, loadUnread]);

  const activeAccounts = accounts.filter((a) => a.status === "ACTIVE");
  const shared = { accounts, activeAccounts, reloadAccounts: loadAccounts, goTo: setTab };

  const signOut = () => {
    logout();
    navigate("/auth");
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="bg-white shadow-sm px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2 text-indigo-600">
          <BsBank className="text-2xl" />
          <span className="text-xl font-bold text-gray-900">CoreBank</span>
        </div>
        <div className="flex items-center gap-4">
          <button onClick={() => setTab("notifications")} className="relative text-gray-600 hover:text-indigo-600" title="Notifications">
            <BsBell className="text-xl" />
            {unread > 0 && (
              <span className="absolute -top-2 -right-2 bg-red-600 text-white text-[10px] rounded-full px-1.5">{unread}</span>
            )}
          </button>
          <span className="hidden sm:block text-sm text-gray-700">{user?.firstName} {user?.lastName}</span>
          <button onClick={signOut} className="flex items-center gap-1 text-sm text-gray-600 hover:text-red-600">
            <BsBoxArrowRight /> Sign out
          </button>
        </div>
      </header>

      <div className="flex flex-1 flex-col md:flex-row">
        <nav className="md:w-60 bg-white border-r border-gray-100 p-3 flex md:flex-col gap-1 overflow-x-auto">
          {TABS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex items-center gap-3 px-3 py-2 rounded-md text-sm whitespace-nowrap ${
                tab === id ? "bg-indigo-50 text-indigo-700 font-semibold" : "text-gray-600 hover:bg-gray-50"
              }`}
            >
              <Icon /> {label}
            </button>
          ))}
        </nav>

        <main className="flex-1 p-4 md:p-8 max-w-6xl">
          {tab === "overview" && <Overview {...shared} />}
          {tab === "transfer" && <TransferPanel {...shared} />}
          {tab === "cash" && <CashPanel {...shared} />}
          {tab === "beneficiaries" && <BeneficiariesPanel />}
          {tab === "statement" && <StatementPanel {...shared} />}
          {tab === "notifications" && <NotificationsPanel onChange={loadUnread} />}
          {tab === "profile" && <ProfilePanel />}
        </main>
      </div>
    </div>
  );
};

export default Dashboard;
