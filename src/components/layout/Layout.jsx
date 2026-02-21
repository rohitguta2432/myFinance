import React from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, User, DollarSign, Wallet, Flag, Shield, Calculator, CheckCircle } from 'lucide-react';

const Layout = () => {
    const location = useLocation();
    const navigate = useNavigate();

    // Step info based on current path
    const getStepInfo = () => {
        const path = location.pathname;
        if (path.includes('step-1')) return { step: 1, progress: 16, title: 'Profile & Risk' };
        if (path.includes('step-2')) return { step: 2, progress: 33, title: 'Income & Expenses' };
        if (path.includes('step-3')) return { step: 3, progress: 50, title: 'Assets & Liabilities' };
        if (path.includes('step-4')) return { step: 4, progress: 66, title: 'Financial Goals' };
        if (path.includes('step-5')) return { step: 5, progress: 83, title: 'Insurance Gap' };
        if (path.includes('step-6')) return { step: 6, progress: 96, title: 'Tax Planning' };
        if (path.includes('complete')) return { step: 7, progress: 100, title: 'Complete' };
        return { step: 0, progress: 0, title: '' };
    };

    const { step, progress, title } = getStepInfo();
    const isHome = location.pathname === '/';
    const isComplete = location.pathname.includes('complete');
    const showHeader = step > 0 && step <= 6;

    // Steps config for sidebar
    const steps = [
        { num: 1, title: 'Profile & Risk', icon: User, path: '/assessment/step-1' },
        { num: 2, title: 'Income & Expenses', icon: DollarSign, path: '/assessment/step-2' },
        { num: 3, title: 'Assets & Liabilities', icon: Wallet, path: '/assessment/step-3' },
        { num: 4, title: 'Financial Goals', icon: Flag, path: '/assessment/step-4' },
        { num: 5, title: 'Insurance Gap', icon: Shield, path: '/assessment/step-5' },
        { num: 6, title: 'Tax Planning', icon: Calculator, path: '/assessment/step-6' },
    ];

    return (
        <div className="min-h-screen bg-background-dark flex flex-col font-display text-white antialiased selection:bg-primary/30">
            {/* Sticky Header with Progress */}
            {showHeader && (
                <header className="sticky top-0 z-50 bg-background-dark/95 backdrop-blur-md border-b border-white/5">
                    <div className="flex items-center p-4 pb-2 justify-between max-w-4xl mx-auto">
                        <button
                            onClick={() => navigate(-1)}
                            className="text-white flex size-10 shrink-0 items-center justify-center rounded-full hover:bg-white/10 transition-colors"
                        >
                            <ArrowLeft className="w-6 h-6" />
                        </button>
                        <h2 className="text-white text-lg font-bold leading-tight tracking-[-0.015em] flex-1 text-center pr-10">
                            {title}
                        </h2>
                    </div>
                    {/* Progress Bar — hidden on lg where sidebar shows */}
                    <div className="flex flex-col gap-2 px-6 pb-4 max-w-4xl mx-auto lg:hidden">
                        <div className="flex justify-between items-end">
                            <p className="text-primary text-xs font-semibold uppercase tracking-wider">
                                Step {step} of 6
                            </p>
                            <p className="text-slate-400 text-xs">
                                {progress}% Completed
                            </p>
                        </div>
                        <div className="h-1.5 w-full rounded-full bg-surface-active overflow-hidden">
                            <div
                                className="h-full bg-primary rounded-full shadow-[0_0_10px_rgba(13,242,89,0.5)] transition-all duration-500 ease-out"
                                style={{ width: `${progress}%` }}
                            ></div>
                        </div>
                    </div>
                </header>
            )}

            {/* Content Area: Sidebar + Main */}
            <div className="flex-1 w-full max-w-6xl mx-auto flex">
                {/* Desktop Sidebar — step navigator */}
                {showHeader && (
                    <aside className="hidden lg:flex flex-col w-64 shrink-0 p-6 pt-8 border-r border-white/5">
                        <p className="text-[10px] uppercase tracking-widest font-bold text-slate-500 mb-6">Assessment Progress</p>
                        <nav className="space-y-1">
                            {steps.map((s) => {
                                const Icon = s.icon;
                                const isActive = s.num === step;
                                const isCompleted = s.num < step;
                                const isFuture = s.num > step;
                                return (
                                    <button
                                        key={s.num}
                                        onClick={() => navigate(s.path)}
                                        className={`w-full flex items-center gap-3 px-3 py-3 rounded-xl text-left transition-all group ${
                                            isActive
                                                ? 'bg-primary/10 border border-primary/30'
                                                : isCompleted
                                                ? 'hover:bg-white/5 cursor-pointer'
                                                : 'opacity-40 cursor-default'
                                        }`}
                                        disabled={isFuture}
                                    >
                                        <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 transition-all ${
                                            isActive
                                                ? 'bg-primary text-background-dark shadow-[0_0_12px_rgba(13,242,89,0.4)]'
                                                : isCompleted
                                                ? 'bg-primary/20 text-primary'
                                                : 'bg-surface-active text-slate-500'
                                        }`}>
                                            {isCompleted ? (
                                                <CheckCircle className="w-4 h-4" />
                                            ) : (
                                                <Icon className="w-4 h-4" />
                                            )}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className={`text-xs font-bold uppercase tracking-wider ${
                                                isActive ? 'text-primary' : isCompleted ? 'text-slate-400' : 'text-slate-600'
                                            }`}>
                                                Step {s.num}
                                            </p>
                                            <p className={`text-sm font-medium truncate ${
                                                isActive ? 'text-white' : isCompleted ? 'text-slate-300' : 'text-slate-500'
                                            }`}>
                                                {s.title}
                                            </p>
                                        </div>
                                    </button>
                                );
                            })}
                        </nav>
                        {/* Overall progress at bottom */}
                        <div className="mt-auto pt-6 border-t border-white/5">
                            <div className="flex justify-between text-xs mb-2">
                                <span className="text-slate-500 font-medium">Overall</span>
                                <span className="text-primary font-bold">{progress}%</span>
                            </div>
                            <div className="h-1.5 w-full rounded-full bg-surface-active overflow-hidden">
                                <div
                                    className="h-full bg-primary rounded-full shadow-[0_0_10px_rgba(13,242,89,0.5)] transition-all duration-500"
                                    style={{ width: `${progress}%` }}
                                ></div>
                            </div>
                        </div>
                    </aside>
                )}

                {/* Main Content */}
                <main className="flex-1 w-full max-w-4xl mx-auto p-4 md:p-6 lg:p-8 animate-fade-in relative">
                    <Outlet />
                </main>
            </div>

            {/* Footer */}
            {!isComplete && (
                <footer className="p-6 text-center text-xs text-white/30">
                    © 2026 MyFinancial. Secure & Private.
                </footer>
            )}
        </div>
    );
};

export default Layout;
