import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BarChart3, Zap, Shield, Calculator, RefreshCw } from 'lucide-react';
import FinancialDashboard from './FinancialDashboard';
import ActionPlanTab from './ActionPlanTab';
import InsuranceTab from './InsuranceTab';
import TaxPlanningTab from './TaxPlanningTab';

const SIDEBAR_TABS = [
    { id: 'summary', label: 'Summary', icon: BarChart3 },
    { id: 'actions', label: 'Action Plan', icon: Zap },
    { id: 'insurance', label: 'Insurance', icon: Shield },
    { id: 'tax', label: 'Tax Planning', icon: Calculator },
];

/**
 * Dashboard wrapper — no header/footer (provided by Layout.jsx).
 * Just sidebar + tab content.
 */
const DashboardPage = () => {
    const [activeTab, setActiveTab] = useState('summary');
    const navigate = useNavigate();

    const renderContent = () => {
        switch (activeTab) {
            case 'actions':
                return <ActionPlanTab />;
            case 'insurance':
                return <InsuranceTab />;
            case 'tax':
                return <TaxPlanningTab />;
            case 'summary':
            default:
                return <FinancialDashboard />;
        }
    };

    return (
        <div className="flex-1 flex">
            {/* Left Sidebar Navigation */}
            <aside className="hidden lg:flex flex-col w-56 shrink-0 border-r border-white/5 bg-background-dark/50">
                <nav className="flex-1 p-4 space-y-1 pt-6">
                    <p className="text-[10px] uppercase tracking-widest font-bold text-slate-500 mb-4 px-3">Dashboard</p>
                    {SIDEBAR_TABS.map((tab) => {
                        const Icon = tab.icon;
                        const isActive = activeTab === tab.id;
                        return (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left transition-all cursor-pointer ${
                                    isActive
                                        ? 'bg-primary/10 border border-primary/30 text-primary'
                                        : 'text-slate-400 hover:text-white hover:bg-white/5'
                                }`}
                            >
                                <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-primary' : ''}`} />
                                <span className={`text-sm font-semibold ${isActive ? 'text-primary' : ''}`}>{tab.label}</span>
                            </button>
                        );
                    })}
                    {/* Retake Assessment */}
                    <div className="mt-4 pt-3 border-t border-white/5">
                        <button
                            onClick={() => navigate('/assessment/step-1')}
                            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left transition-all cursor-pointer text-amber-400/70 hover:text-amber-400 hover:bg-amber-400/5"
                        >
                            <RefreshCw className="w-4 h-4 shrink-0" />
                            <span className="text-sm font-semibold">Retake Assessment</span>
                        </button>
                    </div>
                </nav>
            </aside>

            {/* Mobile Tab Bar */}
            <div className="lg:hidden sticky top-[45px] z-40 bg-background-dark/90 backdrop-blur-md border-b border-white/5 w-full">
                <div className="flex gap-1 p-2 overflow-x-auto">
                    {SIDEBAR_TABS.map((tab) => {
                        const Icon = tab.icon;
                        const isActive = activeTab === tab.id;
                        return (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-bold tracking-wide transition-all whitespace-nowrap cursor-pointer ${
                                    isActive
                                        ? 'bg-primary/15 text-primary'
                                        : 'text-slate-500 hover:text-slate-300'
                                }`}
                            >
                                <Icon className="w-3.5 h-3.5" />
                                {tab.label}
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* Tab Content */}
            <div className="flex-1 min-w-0">
                {renderContent()}
            </div>
        </div>
    );
};

export default DashboardPage;
