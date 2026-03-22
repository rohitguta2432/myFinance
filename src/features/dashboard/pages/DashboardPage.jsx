import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BarChart3, Zap, Shield, Calculator, RefreshCw, Lock, X, Crown, Check } from 'lucide-react';
import FinancialDashboard from './FinancialDashboard';
import ActionPlanTab from './ActionPlanTab';
import InsuranceTab from './InsuranceTab';
import TaxPlanningTab from './TaxPlanningTab';

const SIDEBAR_TABS = [
    { id: 'summary', label: 'Summary', icon: BarChart3, premium: false },
    { id: 'actions', label: 'Action Plan', icon: Zap, premium: true },
    { id: 'insurance', label: 'Insurance', icon: Shield, premium: true },
    { id: 'tax', label: 'Tax Planning', icon: Calculator, premium: true },
];

const PREMIUM_FEATURES = [
    'Personalized Action Plans — step-by-step guides to fix each red flag',
    'Insurance Gap Analysis — HLV method to calculate exact coverage needed',
    'Tax Regime Comparison — Old vs New with optimization recommendations',
    'Unlimited AI Advisor — ask Kira anything about your finances',
    'Priority Support — get answers within 24 hours',
];

/* ── Upgrade Modal ── */
const UpgradeModal = ({ isOpen, onClose, tabLabel }) => {
    if (!isOpen) return null;
    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
            {/* Backdrop */}
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
            {/* Modal */}
            <div className="relative w-full max-w-md bg-surface-dark border border-white/10 rounded-3xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
                {/* Glow effect */}
                <div className="absolute inset-0 opacity-40 pointer-events-none"
                    style={{ background: 'radial-gradient(circle at 50% 0%, rgba(245,158,11,0.25), transparent 60%)' }}
                />
                {/* Close */}
                <button onClick={onClose}
                    className="absolute top-4 right-4 z-20 w-8 h-8 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors cursor-pointer">
                    <X className="w-4 h-4 text-slate-400" />
                </button>
                {/* Content */}
                <div className="relative z-10 p-8 pt-10 text-center">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 border border-amber-500/30 flex items-center justify-center mx-auto mb-5">
                        <Lock className="w-7 h-7 text-amber-400" />
                    </div>
                    <h3 className="text-2xl font-black text-white mb-2">
                        {tabLabel} is a Premium Feature
                    </h3>
                    <p className="text-base text-slate-400 mb-6 leading-relaxed">
                        Upgrade to unlock detailed analysis and personalized recommendations for your financial health.
                    </p>
                    {/* Feature list */}
                    <div className="text-left space-y-2.5 mb-8">
                        {PREMIUM_FEATURES.map((feat, i) => (
                            <div key={i} className="flex items-start gap-2.5">
                                <Check className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                                <span className="text-sm text-slate-300 leading-relaxed">{feat}</span>
                            </div>
                        ))}
                    </div>
                    {/* CTA */}
                    <button className="w-full py-4 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-background-dark font-bold rounded-xl shadow-[0_0_30px_rgba(245,158,11,0.3)] transition-all active:scale-[0.97] cursor-pointer">
                        Upgrade to Premium
                        <span className="block text-sm font-medium opacity-80 mt-0.5">₹999/year — Cancel anytime</span>
                    </button>
                    <p className="text-xs text-slate-600 mt-3">No credit card required · 7-day free trial</p>
                </div>
            </div>
        </div>
    );
};

/**
 * Dashboard wrapper — sidebar + tab content.
 * Free users: only Summary tab accessible.
 * Premium users: all tabs unlocked.
 */
const DashboardPage = () => {
    const [activeTab, setActiveTab] = useState('summary');
    const [showUpgradeModal, setShowUpgradeModal] = useState(false);
    const [upgradeTabLabel, setUpgradeTabLabel] = useState('');
    const navigate = useNavigate();

    // Premium state (localStorage for now — will be backend-driven later)
    const [isPremium] = useState(() => {
        return localStorage.getItem('myfinancial_premium') === 'true';
    });

    const handleTabClick = (tab) => {
        if (tab.premium && !isPremium) {
            setUpgradeTabLabel(tab.label);
            setShowUpgradeModal(true);
            return;
        }
        setActiveTab(tab.id);
    };

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
            {/* Upgrade Modal */}
            <UpgradeModal
                isOpen={showUpgradeModal}
                onClose={() => setShowUpgradeModal(false)}
                tabLabel={upgradeTabLabel}
            />

            {/* Left Sidebar Navigation */}
            <aside className="hidden lg:flex flex-col w-56 shrink-0 border-r border-white/5 bg-background-dark/50">
                <nav className="flex-1 p-4 space-y-1 pt-6">
                    {/* Premium Badge or Free Plan Label */}
                    {isPremium ? (
                        <div className="flex items-center gap-2 px-3 mb-4">
                            <Crown className="w-5 h-5 text-amber-400" />
                            <span className="text-xs uppercase tracking-[0.2em] font-bold bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">Premium</span>
                        </div>
                    ) : (
                        <p className="text-xs uppercase tracking-widest font-bold text-slate-500 mb-4 px-3">Dashboard</p>
                    )}

                    {SIDEBAR_TABS.map((tab) => {
                        const Icon = tab.icon;
                        const isActive = activeTab === tab.id;
                        const isLocked = tab.premium && !isPremium;
                        return (
                            <button
                                key={tab.id}
                                onClick={() => handleTabClick(tab)}
                                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left transition-all cursor-pointer ${
                                    isActive
                                        ? 'bg-primary/10 border border-primary/30 text-primary'
                                        : isLocked
                                            ? 'text-slate-600 hover:text-slate-500 hover:bg-white/[0.02]'
                                            : 'text-slate-400 hover:text-white hover:bg-white/5'
                                }`}
                            >
                                <Icon className={`w-5 h-5 shrink-0 ${isActive ? 'text-primary' : isLocked ? 'text-slate-600' : ''}`} />
                                <span className={`text-base font-semibold flex-1 ${isActive ? 'text-primary' : isLocked ? 'text-slate-600' : ''}`}>{tab.label}</span>
                                {isLocked && <Lock className="w-4 h-4 text-amber-500/50 shrink-0" />}
                            </button>
                        );
                    })}

                    {/* Retake Assessment */}
                    <div className="mt-4 pt-3 border-t border-white/5">
                        <button
                            onClick={() => navigate('/assessment/step-1')}
                            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left transition-all cursor-pointer text-amber-400/70 hover:text-amber-400 hover:bg-amber-400/5"
                        >
                            <RefreshCw className="w-5 h-5 shrink-0" />
                            <span className="text-base font-semibold">Retake Assessment</span>
                        </button>
                    </div>
                </nav>

                {/* Sidebar Footer — Upgrade CTA for free users */}
                {!isPremium && (
                    <div className="p-4 border-t border-white/5">
                        <button
                            onClick={() => { setUpgradeTabLabel('Premium'); setShowUpgradeModal(true); }}
                            className="w-full py-3 bg-gradient-to-r from-amber-500/10 to-orange-500/10 border border-amber-500/20 rounded-xl text-center hover:border-amber-500/40 transition-all cursor-pointer group"
                        >
                            <span className="text-sm font-bold text-amber-400 group-hover:text-amber-300">🔓 Upgrade to Premium</span>
                            <span className="block text-[11px] text-slate-600 mt-0.5">Unlock all features</span>
                        </button>
                    </div>
                )}
            </aside>

            {/* Mobile Tab Bar */}
            <div className="lg:hidden sticky top-[45px] z-40 bg-background-dark/90 backdrop-blur-md border-b border-white/5 w-full">
                <div className="flex gap-1 p-2 overflow-x-auto">
                    {SIDEBAR_TABS.map((tab) => {
                        const Icon = tab.icon;
                        const isActive = activeTab === tab.id;
                        const isLocked = tab.premium && !isPremium;
                        return (
                            <button
                                key={tab.id}
                                onClick={() => handleTabClick(tab)}
                                className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-bold tracking-wide transition-all whitespace-nowrap cursor-pointer ${
                                    isActive
                                        ? 'bg-primary/15 text-primary'
                                        : isLocked
                                            ? 'text-slate-600'
                                            : 'text-slate-500 hover:text-slate-300'
                                }`}
                            >
                                <Icon className="w-3.5 h-3.5" />
                                {tab.label}
                                {isLocked && <Lock className="w-3 h-3 text-amber-500/50 ml-0.5" />}
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
