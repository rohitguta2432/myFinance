import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Star, TrendingUp, Scale, Flag, ArrowRight, BarChart3, Zap, Shield, Calculator, RefreshCw } from 'lucide-react';
import { useAssessmentStore } from '../../assessment/store/useAssessmentStore';
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

const Home = () => {
    const navigate = useNavigate();
    const { isComplete } = useAssessmentStore();
    const [activeTab, setActiveTab] = useState('summary');

    const renderTabContent = () => {
        switch (activeTab) {
            case 'actions': return <ActionPlanTab />;
            case 'insurance': return <InsuranceTab />;
            case 'tax': return <TaxPlanningTab />;
            case 'summary':
            default: return <FinancialDashboard />;
        }
    };

    return (
        <div className="flex-1 flex flex-col">
            {/* Content: Sidebar + Dashboard OR Landing */}
            <div className="flex-1 flex">
                {isComplete ? (
                    <>
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
                            {renderTabContent()}
                        </div>
                    </>
                ) : (
                    <div className="flex-1 flex flex-col">
                        {/* Hero Section */}
                        <div className="px-6 pt-8 pb-8 text-center">
                            <h1 className="text-white text-3xl lg:text-5xl leading-tight font-extrabold tracking-tight mb-4 max-w-4xl mx-auto">
                                Fix Your Finances Early, or Pay <span className="text-primary whitespace-nowrap">10× Later</span>
                            </h1>
                            <p className="text-white/70 text-base lg:text-lg font-normal leading-relaxed max-w-sm lg:max-w-2xl mx-auto">
                                Most professionals lose lakhs in opportunity cost by ignoring financial planning. Get your free Financial Position Summary in 10 minutes.
                            </p>
                        </div>

                        {/* Social Proof Pill */}
                        <div className="flex justify-center mb-8">
                            <div className="bg-surface-dark border border-white/10 rounded-full py-2 pl-2 pr-4 flex items-center gap-3">
                                <div className="flex -space-x-2">
                                    <div className="w-8 h-8 rounded-full border-2 border-surface-dark bg-gray-300 flex items-center justify-center text-surface-dark text-xs font-bold">A</div>
                                    <div className="w-8 h-8 rounded-full border-2 border-surface-dark bg-gray-400 flex items-center justify-center text-surface-dark text-xs font-bold">B</div>
                                    <div className="w-8 h-8 rounded-full border-2 border-surface-dark bg-gray-500 flex items-center justify-center text-surface-dark text-xs font-bold">C</div>
                                </div>
                                <div className="flex flex-col items-start">
                                    <div className="flex text-yellow-400 text-[10px] gap-0.5">
                                        {[...Array(5)].map((_, i) => (
                                            <Star key={i} className="w-3 h-3 fill-current" />
                                        ))}
                                    </div>
                                    <span className="text-white/80 text-xs font-medium">50,000+ Indians trust us</span>
                                </div>
                            </div>
                        </div>

                        {/* Feature Cards Grid */}
                        <h2 className="sr-only">Core Features</h2>
                        <div className="px-6 lg:px-8 space-y-4 lg:space-y-0 lg:grid lg:grid-cols-3 lg:items-start lg:gap-6 mb-8 max-w-4xl mx-auto">
                            <div className="bg-surface-dark rounded-xl p-5 border border-white/5 relative overflow-hidden group">
                                <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                                    <TrendingUp className="w-16 h-16 text-primary" />
                                </div>
                                <div className="flex items-start gap-4 relative z-10">
                                    <div className="bg-primary/10 p-3 rounded-lg shrink-0">
                                        <TrendingUp className="text-primary w-5 h-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-white font-bold text-lg mb-1">Post-Tax Return Analysis</h3>
                                        <p className="text-slate-400 text-sm leading-snug">See the real growth of your wealth after taxes and inflation eats into your returns.</p>
                                    </div>
                                </div>
                                <div className="mt-4 h-1 w-full bg-white/10 rounded-full overflow-hidden">
                                    <div className="h-full bg-primary w-3/4 rounded-full"></div>
                                </div>
                            </div>

                            <div className="bg-surface-dark rounded-xl p-5 border border-white/5 relative overflow-hidden group">
                                <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                                    <Scale className="w-16 h-16 text-primary" />
                                </div>
                                <div className="flex items-start gap-4 relative z-10">
                                    <div className="bg-primary/10 p-3 rounded-lg shrink-0">
                                        <Scale className="text-primary w-5 h-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-white font-bold text-lg mb-1">Tax Regime Optimizer</h3>
                                        <div className="inline-block bg-white/10 rounded px-2 py-0.5 mb-2">
                                            <span className="text-[10px] text-primary font-bold uppercase tracking-wider">FY 2026-27 Ready</span>
                                        </div>
                                        <p className="text-slate-400 text-sm leading-snug">Compare Old vs. New regime instantly tailored to your specific deductions.</p>
                                    </div>
                                </div>
                            </div>

                            <div className="bg-surface-dark rounded-xl p-5 border border-white/5 relative overflow-hidden group">
                                <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                                    <Flag className="w-16 h-16 text-primary" />
                                </div>
                                <div className="flex items-start gap-4 relative z-10">
                                    <div className="bg-primary/10 p-3 rounded-lg shrink-0">
                                        <Flag className="text-primary w-5 h-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-white font-bold text-lg mb-1">Goal-Based Roadmap</h3>
                                        <p className="text-slate-400 text-sm leading-snug">Map your savings to life events like buying a home, education, or retirement.</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Footer Note */}
                        <p className="text-center text-white/30 text-xs px-8 pb-32">
                            MyFinancial is a privacy-first tool. We do not sell your data to banks or insurance agents.
                        </p>

                        {/* Sticky Bottom CTA */}
                        <div className="fixed bottom-0 left-0 w-full bg-background-dark/80 backdrop-blur-xl border-t border-white/5 p-5 pb-8 z-40">
                            <div className="max-w-4xl mx-auto">
                                <button
                                    onClick={() => navigate('/assessment/step-1')}
                                    className="w-full bg-primary hover:bg-primary-dark active:scale-[0.98] transition-all text-background-dark font-bold text-lg py-4 rounded-xl flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(13,242,89,0.3)] cursor-pointer"
                                >
                                    Start Free Assessment
                                    <ArrowRight className="w-5 h-5 font-bold" />
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Home;
