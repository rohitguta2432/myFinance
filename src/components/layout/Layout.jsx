import React from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

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

    return (
        <div className="min-h-screen bg-background-dark flex flex-col font-display text-white antialiased selection:bg-primary/30">
            {/* Sticky Header with Progress */}
            {showHeader && (
                <header className="sticky top-0 z-50 bg-background-dark/95 backdrop-blur-md border-b border-white/5">
                    <div className="flex items-center p-4 pb-2 justify-between max-w-md mx-auto">
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
                    {/* Progress Bar */}
                    <div className="flex flex-col gap-2 px-6 pb-4 max-w-md mx-auto">
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

            {/* Main Content */}
            <main className="flex-1 w-full max-w-md mx-auto p-4 md:p-6 animate-fade-in relative">
                <Outlet />
            </main>

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
