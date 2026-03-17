import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Zap } from 'lucide-react';
import DashboardTabs from '../components/DashboardTabs';
import ThemeToggle from '../../../components/ui/ThemeToggle';
import FinancialDashboard from './FinancialDashboard';
import ActionPlanTab from './ActionPlanTab';
import InsuranceTab from './InsuranceTab';
import TaxPlanningTab from './TaxPlanningTab';

/**
 * Single-route dashboard wrapper.
 * Renders the shared sticky header + tab bar once,
 * then swaps content based on the active tab state — true SPA.
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
        <div className="min-h-screen bg-background-dark text-white">
            {/* Shared Sticky Header */}
            <div className="sticky top-0 z-50 bg-background-dark/80 backdrop-blur-xl border-b border-white/5">
                <div className="max-w-[1200px] mx-auto px-4 py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 bg-primary/20 rounded-lg flex items-center justify-center">
                            <Zap className="w-4 h-4 text-primary" />
                        </div>
                        <h1 className="font-bold text-lg tracking-wide">Financial Health</h1>
                    </div>
                    <div className="flex items-center gap-3">
                        <DashboardTabs activeTab={activeTab} onTabChange={setActiveTab} />
                        <ThemeToggle />
                        <button
                            onClick={() => navigate('/assessment/step-1')}
                            className="text-xs text-slate-400 hover:text-white transition-colors px-3 py-1.5 bg-surface-dark rounded-lg border border-white/5"
                        >
                            ↻ Retake
                        </button>
                    </div>
                </div>
            </div>

            {/* Tab Content — swapped without route change */}
            {renderContent()}
        </div>
    );
};

export default DashboardPage;
