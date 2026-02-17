import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Shield, HeartPulse, Edit2, AlertTriangle, CheckCircle, Info } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';

const Step5InsuranceGap = () => {
    const navigate = useNavigate();
    const {
        incomes,
        dependents,
        cityTier,
        liabilities,
        insurance,
        setInsurance
    } = useAssessmentStore();

    const [isEditingLife, setIsEditingLife] = useState(false);
    const [isEditingHealth, setIsEditingHealth] = useState(false);

    // Calculate Needs
    const annualIncome = incomes.reduce((sum, item) => sum + (item.frequency === 'Monthly' ? item.amount * 12 : item.amount), 0);
    const totalDebt = liabilities.reduce((sum, item) => sum + item.amount, 0);
    const requiredLifeCover = dependents > 0 ? (annualIncome * 15) + totalDebt : totalDebt;
    const requiredHealthCover = cityTier === 'Tier 1' ? 1500000 : cityTier === 'Tier 2' ? 1000000 : 500000;

    // Gaps
    const lifeGap = Math.max(0, requiredLifeCover - insurance.life);
    const healthGap = Math.max(0, requiredHealthCover - insurance.health);

    const formatCurrency = (amount) => {
        if (amount >= 10000000) return `₹${(amount / 10000000).toFixed(1)} Cr`;
        if (amount >= 100000) return `₹${(amount / 100000).toFixed(1)} L`;
        return `₹${amount.toLocaleString()}`;
    };

    // Score Calculation
    const lifeScore = requiredLifeCover > 0 ? Math.min(1, insurance.life / requiredLifeCover) * 50 : 50;
    const healthScore = Math.min(1, insurance.health / requiredHealthCover) * 50;
    const totalScore = Math.round(lifeScore + healthScore);

    return (
        <div className="flex flex-col h-full pb-32">

            <div className="flex-1 space-y-6 overflow-y-auto pb-4">

                {/* Score Card */}
                <div className="bg-surface-dark rounded-2xl p-6 text-center relative overflow-hidden shadow-lg border border-white/5">
                    <div className="absolute top-0 left-1/2 -translate-x-1/2 w-64 h-32 bg-primary/20 blur-3xl rounded-full"></div>

                    <h3 className="text-slate-400 text-xs font-bold uppercase tracking-wider relative z-10 mb-2">Protection Score</h3>

                    <div className="relative z-10">
                        <span className={`text-5xl font-extrabold ${totalScore < 50 ? 'text-red-500' : totalScore < 80 ? 'text-yellow-500' : 'text-primary'}`}>
                            {totalScore}
                            <span className="text-lg text-slate-500">/100</span>
                        </span>
                    </div>

                    <div className={`mt-4 inline-flex items-center gap-2 px-3 py-1.5 rounded-full ${totalScore < 50 ? 'bg-red-500/10 text-red-400' : 'bg-primary/10 text-primary'} border border-white/5`}>
                        {totalScore < 50 ? <AlertTriangle className="w-4 h-4" /> : <CheckCircle className="w-4 h-4" />}
                        <span className="text-xs font-bold uppercase tracking-wide">
                            {totalScore < 50 ? 'Critical Gaps Detected' : 'Well Protected'}
                        </span>
                    </div>
                </div>

                {/* Life Insurance */}
                <div className="bg-surface-dark rounded-2xl p-5 border border-white/5">
                    <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                            <div className="bg-blue-500/10 p-2.5 rounded-xl text-blue-400">
                                <Shield className="w-6 h-6" />
                            </div>
                            <div>
                                <h3 className="font-bold text-white">Life Insurance</h3>
                                <p className="text-xs text-slate-400">Term plan coverage</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Required</p>
                            <p className="font-bold text-white">{formatCurrency(requiredLifeCover)}</p>
                        </div>
                    </div>

                    <div className="bg-background-dark rounded-xl p-4 border border-white/10 mb-3">
                        <div className="flex justify-between items-center">
                            <span className="text-sm font-medium text-slate-400">Current Cover</span>
                            {isEditingLife ? (
                                <div className="flex items-center gap-2">
                                    <span className="text-slate-500">₹</span>
                                    <input
                                        type="number"
                                        className="w-24 bg-surface-dark border border-white/10 rounded px-2 py-1 text-right font-bold text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        value={insurance.life}
                                        onChange={(e) => setInsurance('life', parseFloat(e.target.value) || 0)}
                                        onBlur={() => setIsEditingLife(false)}
                                        autoFocus
                                    />
                                </div>
                            ) : (
                                <button
                                    onClick={() => setIsEditingLife(true)}
                                    className="flex items-center gap-2 text-white font-bold hover:text-blue-400 transition-colors"
                                >
                                    {formatCurrency(insurance.life)}
                                    <Edit2 className="w-3 h-3 text-slate-500" />
                                </button>
                            )}
                        </div>
                    </div>

                    {lifeGap > 0 ? (
                        <div className="flex items-center gap-3 p-3 bg-red-500/10 text-red-400 rounded-xl text-sm font-medium border border-red-500/20">
                            <AlertTriangle className="w-5 h-5 flex-shrink-0" />
                            <span>Gap of <strong>{formatCurrency(lifeGap)}</strong> detected.</span>
                        </div>
                    ) : (
                        <div className="flex items-center gap-3 p-3 bg-primary/10 text-primary rounded-xl text-sm font-medium border border-primary/20">
                            <CheckCircle className="w-5 h-5 flex-shrink-0" />
                            <span>You are adequately insured!</span>
                        </div>
                    )}
                </div>

                {/* Health Insurance */}
                <div className="bg-surface-dark rounded-2xl p-5 border border-white/5">
                    <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                            <div className="bg-teal-500/10 p-2.5 rounded-xl text-teal-400">
                                <HeartPulse className="w-6 h-6" />
                            </div>
                            <div>
                                <h3 className="font-bold text-white">Health Insurance</h3>
                                <p className="text-xs text-slate-400">Family floater / Individual</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Required</p>
                            <p className="font-bold text-white">{formatCurrency(requiredHealthCover)}</p>
                        </div>
                    </div>

                    <div className="bg-background-dark rounded-xl p-4 border border-white/10 mb-3">
                        <div className="flex justify-between items-center">
                            <span className="text-sm font-medium text-slate-400">Current Cover</span>
                            {isEditingHealth ? (
                                <div className="flex items-center gap-2">
                                    <span className="text-slate-500">₹</span>
                                    <input
                                        type="number"
                                        className="w-24 bg-surface-dark border border-white/10 rounded px-2 py-1 text-right font-bold text-white focus:outline-none focus:ring-2 focus:ring-teal-500"
                                        value={insurance.health}
                                        onChange={(e) => setInsurance('health', parseFloat(e.target.value) || 0)}
                                        onBlur={() => setIsEditingHealth(false)}
                                        autoFocus
                                    />
                                </div>
                            ) : (
                                <button
                                    onClick={() => setIsEditingHealth(true)}
                                    className="flex items-center gap-2 text-white font-bold hover:text-teal-400 transition-colors"
                                >
                                    {formatCurrency(insurance.health)}
                                    <Edit2 className="w-3 h-3 text-slate-500" />
                                </button>
                            )}
                        </div>
                    </div>

                    {healthGap > 0 ? (
                        <div className="flex items-center gap-3 p-3 bg-red-500/10 text-red-400 rounded-xl text-sm font-medium border border-red-500/20">
                            <AlertTriangle className="w-5 h-5 flex-shrink-0" />
                            <span>Gap of <strong>{formatCurrency(healthGap)}</strong> detected.</span>
                        </div>
                    ) : (
                        <div className="flex items-center gap-3 p-3 bg-primary/10 text-primary rounded-xl text-sm font-medium border border-primary/20">
                            <CheckCircle className="w-5 h-5 flex-shrink-0" />
                            <span>You are adequately insured!</span>
                        </div>
                    )}
                </div>

                {/* Info Note */}
                <div className="flex gap-3 p-4 bg-blue-500/10 rounded-xl border border-blue-500/20">
                    <Info className="w-5 h-5 text-blue-400 flex-shrink-0" />
                    <p className="text-xs text-blue-300 leading-relaxed">
                        <strong>Why these numbers?</strong><br />
                        Life cover is calculated as 15x annual income + debt liabilities. Health cover is recommended based on your city tier ({cityTier}) to cover major medical emergencies.
                    </p>
                </div>

            </div>

            {/* Footer */}
            <div className="fixed bottom-0 left-0 w-full bg-surface-dark border-t border-white/10 p-5 z-40 rounded-t-3xl">
                <button
                    onClick={() => navigate('/assessment/step-6')}
                    className="w-full bg-primary text-slate-900 font-bold text-base py-4 rounded-xl flex items-center justify-center gap-2 hover:bg-primary-dark transition-colors shadow-lg active:scale-[0.98]"
                >
                    Next: Tax Optimization
                    <ArrowRight className="w-5 h-5" />
                </button>
            </div>
        </div>
    );
};

export default Step5InsuranceGap;
