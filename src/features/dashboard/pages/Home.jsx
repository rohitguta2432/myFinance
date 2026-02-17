import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, User, Star, TrendingUp, Scale, Flag, ArrowRight, Menu } from 'lucide-react';

const Home = () => {
    const navigate = useNavigate();

    return (
        <div className="bg-background-dark text-white min-h-screen flex flex-col font-display selection:bg-primary selection:text-background-dark pb-20">
            {/* Sticky Trust Banner */}
            <div className="bg-primary/10 border-b border-primary/20 backdrop-blur-md sticky top-0 z-50 pt-safe-top">
                <div className="px-4 py-3 flex items-start justify-center gap-2 text-center">
                    <ShieldCheck className="text-primary w-4 h-4 shrink-0 translate-y-[2px]" />
                    <p className="text-primary text-xs font-medium leading-relaxed">
                        No Name/Phone/Email Required · Data Stays on Device · Export Anytime
                    </p>
                </div>
            </div>

            {/* Header / Nav */}
            <div className="flex items-center justify-between p-6">
                <div className="flex items-center gap-2">
                    <div className="bg-primary/20 p-2 rounded-lg flex items-center justify-center">
                        <ShieldCheck className="text-primary w-5 h-5" />
                    </div>
                    <h2 className="text-white text-lg font-bold tracking-tight">MyFinancial</h2>
                </div>
                {/* Optional Menu Icon */}
                <button className="text-white/60 hover:text-white transition-colors">
                    <Menu className="w-6 h-6" />
                </button>
            </div>

            {/* Hero Section */}
            <div className="px-6 pt-2 pb-8 text-center">
                <h1 className="text-white text-3xl leading-tight font-extrabold tracking-tight mb-4">
                    Fix Your Finances Early, or Pay <span className="text-primary">10× Later</span>
                </h1>
                <p className="text-white/70 text-base font-normal leading-relaxed max-w-sm mx-auto">
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
            <div className="px-6 space-y-4 mb-8">
                {/* Card 1: Post-Tax Return */}
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
                            <p className="text-white/60 text-sm leading-snug">See the real growth of your wealth after taxes and inflation eats into your returns.</p>
                        </div>
                    </div>
                    {/* Visual Decoration */}
                    <div className="mt-4 h-1 w-full bg-white/10 rounded-full overflow-hidden">
                        <div className="h-full bg-primary w-3/4 rounded-full"></div>
                    </div>
                </div>

                {/* Card 2: Tax Regime Optimizer */}
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
                            <p className="text-white/60 text-sm leading-snug">Compare Old vs. New regime instantly tailored to your specific deductions.</p>
                        </div>
                    </div>
                </div>

                {/* Card 3: Goal Roadmap */}
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
                            <p className="text-white/60 text-sm leading-snug">Map your savings to life events like buying a home, education, or retirement.</p>
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
                <button
                    onClick={() => navigate('/assessment/step-1')}
                    className="w-full bg-primary hover:bg-primary-dark active:scale-[0.98] transition-all text-background-dark font-bold text-lg py-4 rounded-xl flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(13,242,89,0.3)] cursor-pointer"
                >
                    Start Free Assessment
                    <ArrowRight className="w-5 h-5 font-bold" />
                </button>
            </div>
        </div>
    );
};

export default Home;
