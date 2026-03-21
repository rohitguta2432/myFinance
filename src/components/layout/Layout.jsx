import React from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, User, ShieldCheck, DollarSign, Wallet, Flag, Shield, Calculator, CheckCircle, LogOut } from 'lucide-react';
import ThemeToggle from '../ui/ThemeToggle';
import { useAuthStore } from '../../features/auth/store/useAuthStore';

const Layout = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const user = useAuthStore((s) => s.user);
    const logout = useAuthStore((s) => s.logout);

    // Step info based on current path
    const getStepInfo = () => {
        const path = location.pathname;
        if (path.includes('step-1')) return { step: 1, progress: 16, title: "Let's Understand Your Starting Point" };
        if (path.includes('step-2')) return { step: 2, progress: 33, title: 'Your Cash Flow Reality Check' };
        if (path.includes('step-3')) return { step: 3, progress: 50, title: 'Assets & Liabilities' };
        if (path.includes('step-4')) return { step: 4, progress: 66, title: 'Financial Goals' };
        if (path.includes('step-5')) return { step: 5, progress: 83, title: 'Insurance Gap' };
        if (path.includes('step-6')) return { step: 6, progress: 96, title: 'Tax Planning' };
        if (path.includes('complete')) return { step: 7, progress: 100, title: 'Complete' };
        return { step: 0, progress: 0, title: '' };
    };

    const { step, progress, title } = getStepInfo();
    const isHome = location.pathname === '/';
    const isDashboardRoute = location.pathname === '/dashboard';
    const isAssessment = step > 0 && step <= 6;
    const isComplete = location.pathname.includes('complete');

    // Steps config for sidebar
    const steps = [
        { num: 1, title: 'Personal Profile', icon: User, path: '/assessment/step-1' },
        { num: 2, title: 'Your Cash Flow Reality Check', icon: DollarSign, path: '/assessment/step-2' },
        { num: 3, title: 'Assets & Liabilities', icon: Wallet, path: '/assessment/step-3' },
        { num: 4, title: 'Financial Goals', icon: Flag, path: '/assessment/step-4' },
        { num: 5, title: 'Insurance Gap', icon: Shield, path: '/assessment/step-5' },
        { num: 6, title: 'Tax Planning', icon: Calculator, path: '/assessment/step-6' },
    ];

    return (
        <div className="min-h-screen bg-background-dark flex flex-col font-display text-white antialiased selection:bg-primary/30">
            {/* ═══ UNIFIED TOP HEADER ═══ */}
            <header className="sticky top-0 z-50 bg-primary/10 border-b border-primary/20 backdrop-blur-md">
                <div className="px-6 lg:px-10 py-2 flex items-center justify-between w-full">
                    {/* Left: brand + back button on assessment */}
                    <div className="flex items-center gap-3">
                        {isAssessment && (
                            <button
                                onClick={() => navigate(-1)}
                                className="text-white flex size-8 shrink-0 items-center justify-center rounded-full hover:bg-white/10 transition-colors"
                            >
                                <ArrowLeft className="w-5 h-5" />
                            </button>
                        )}
                        <div className="flex items-center gap-2">
                            <div className="bg-primary/20 p-1.5 rounded-lg flex items-center justify-center">
                                <ShieldCheck className="text-primary w-4 h-4" />
                            </div>
                            <span className="text-white text-base font-bold tracking-tight">MyFinancial</span>
                        </div>
                        {/* Step title shown on assessment pages */}
                        {isAssessment && (
                            <span className="hidden md:inline text-slate-400 text-sm font-medium ml-4">
                                — {title}
                            </span>
                        )}
                    </div>

                    {/* Right: theme + user + logout */}
                    <div className="flex items-center gap-3">
                        <ThemeToggle />
                        {user && (
                            <div className="flex items-center gap-2">
                                {user.pictureUrl ? (
                                    <img
                                        src={user.pictureUrl}
                                        alt={user.name}
                                        className="w-8 h-8 rounded-full border-2 border-primary/30 object-cover"
                                        referrerPolicy="no-referrer"
                                    />
                                ) : (
                                    <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
                                        <User className="w-4 h-4 text-primary" />
                                    </div>
                                )}
                                <span className="text-white/70 text-sm font-medium hidden lg:inline">{user.name}</span>
                                <button
                                    onClick={() => {
                                        logout();
                                        navigate('/login');
                                    }}
                                    className="text-white/40 hover:text-white/80 transition-colors p-1.5 rounded-lg hover:bg-white/5 cursor-pointer"
                                    title="Sign out"
                                >
                                    <LogOut className="w-4 h-4" />
                                </button>
                            </div>
                        )}
                    </div>
                </div>

                {/* Mobile Progress Bar — only on assessment steps */}
                {isAssessment && (
                    <div className="flex flex-col gap-2 px-6 pb-3 w-full lg:hidden">
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
                )}
            </header>

            {/* Content Area: Sidebar + Main */}
            <div className="flex-1 w-full flex">
                {/* Desktop Sidebar — step navigator (assessment only) */}
                {isAssessment && (
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
                                        className={`w-full flex items-center gap-3 px-3 py-3 rounded-xl text-left transition-all group ${isActive
                                            ? 'bg-primary/10 border border-primary/30'
                                            : isCompleted
                                                ? 'hover:bg-white/5 cursor-pointer'
                                                : 'opacity-40 cursor-default'
                                            }`}
                                        disabled={isFuture}
                                    >
                                        <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 transition-all ${isActive
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
                                            <p className={`text-xs font-bold uppercase tracking-wider ${isActive ? 'text-primary' : isCompleted ? 'text-slate-400' : 'text-slate-600'
                                                }`}>
                                                Step {s.num}
                                            </p>
                                            <p className={`text-sm font-medium truncate ${isActive ? 'text-white' : isCompleted ? 'text-slate-300' : 'text-slate-500'
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
                <main className={`flex-1 w-full animate-fade-in relative ${(isHome || isDashboardRoute) ? 'p-0' : 'max-w-6xl mx-auto p-4 md:p-6 lg:p-8'}`}>
                    <Outlet />
                </main>
            </div>

            {/* ═══ UNIFIED FOOTER ═══ */}
            {!isComplete && (
                <footer className="border-t border-white/5 py-4 px-6 lg:px-10">
                    <div className="flex items-center justify-between text-xs text-white/30 w-full">
                        <span>© 2026 MyFinancial. All rights reserved.</span>
                        <div className="flex items-center gap-4">
                            <a href="#" className="hover:text-white/50 transition-colors">Privacy</a>
                            <a href="#" className="hover:text-white/50 transition-colors">Terms</a>
                            <a href="#" className="hover:text-white/50 transition-colors">Help</a>
                        </div>
                    </div>
                </footer>
            )}
        </div>
    );
};

export default Layout;
