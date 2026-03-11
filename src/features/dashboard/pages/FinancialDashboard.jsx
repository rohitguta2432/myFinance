import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield, Lock, Zap, TrendingUp, AlertTriangle, ChevronRight, Info, ArrowUpRight, CheckCircle2, XCircle, AlertCircle } from 'lucide-react';
import { useFinancialHealthScore } from '../../../hooks/useFinancialHealthScore';
import { useHookText } from '../../../hooks/useHookText';

/* ─── Score Ring SVG ─── */
const ScoreRing = ({ score, label, color }) => {
    const [animatedScore, setAnimatedScore] = useState(0);
    const radius = 80;
    const stroke = 10;
    const circumference = 2 * Math.PI * radius;
    const pct = Math.min(100, (animatedScore / 100) * 100);
    const offset = circumference - (pct / 100) * circumference;

    useEffect(() => {
        const timer = setTimeout(() => setAnimatedScore(score), 100);
        return () => clearTimeout(timer);
    }, [score]);

    return (
        <div className="relative flex items-center justify-center">
            <svg width="200" height="200" viewBox="0 0 200 200" className="transform -rotate-90">
                <circle cx="100" cy="100" r={radius} fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth={stroke} />
                <circle
                    cx="100" cy="100" r={radius} fill="none"
                    stroke={color}
                    strokeWidth={stroke}
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                    strokeLinecap="round"
                    className="transition-all duration-[1500ms] ease-out"
                    style={{ filter: `drop-shadow(0 0 8px ${color}40)` }}
                />
            </svg>
            <div className="absolute flex flex-col items-center">
                <span className="text-5xl font-black text-white tabular-nums tracking-tight"
                    style={{ textShadow: `0 0 20px ${color}30` }}
                >{Math.round(animatedScore)}</span>
                <span className="text-[11px] font-semibold uppercase tracking-[0.15em] mt-1" style={{ color }}>{label}</span>
                <span className="text-[10px] text-slate-500 mt-0.5">out of 100</span>
            </div>
        </div>
    );
};

/* ─── Pillar Bar ─── */
const PillarBar = ({ pillar, index, isWorst }) => {
    const pct = (pillar.score / pillar.maxScore) * 100;
    const barColor = pct <= 40 ? '#ef4444' : pct <= 65 ? '#f59e0b' : '#0DF259';

    return (
        <div className={`relative bg-surface-dark rounded-2xl p-4 border transition-all duration-300 ${isWorst ? 'border-red-500/30 shadow-[0_0_20px_rgba(239,68,68,0.1)]' : 'border-white/5 hover:border-white/10'}`}>
            {isWorst && (
                <div className="absolute -top-2.5 left-4 px-2 py-0.5 bg-red-500 text-[9px] font-bold text-white uppercase tracking-widest rounded-full">
                    Primary Risk
                </div>
            )}
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                    <span className="text-2xl">{pillar.icon}</span>
                    <div>
                        <h3 className="font-bold text-white text-sm tracking-wide">{pillar.name}</h3>
                        <p className="text-[11px] text-slate-500">{pillar.shortInsight}</p>
                    </div>
                </div>
                <div className="text-right">
                    <div className="flex items-baseline gap-0.5">
                        <span className="text-xl font-black tabular-nums" style={{ color: barColor }}>{pillar.score}</span>
                        <span className="text-xs text-slate-500">/{pillar.maxScore}</span>
                    </div>
                </div>
            </div>
            <div className="w-full h-2 bg-white/5 rounded-full overflow-hidden">
                <div
                    className="h-full rounded-full transition-all duration-1000 ease-out"
                    style={{
                        width: `${pct}%`,
                        background: `linear-gradient(90deg, ${barColor}cc, ${barColor})`,
                        boxShadow: `0 0 12px ${barColor}40`,
                    }}
                />
            </div>
        </div>
    );
};

/* ─── Red Flag Card ─── */
const RedFlag = ({ title, current, benchmark, severity }) => {
    const Icon = severity === 'critical' ? XCircle : severity === 'warn' ? AlertTriangle : AlertCircle;
    const color = severity === 'critical' ? 'text-red-400' : severity === 'warn' ? 'text-amber-400' : 'text-blue-400';
    const bg = severity === 'critical' ? 'bg-red-500/5 border-red-500/10' : severity === 'warn' ? 'bg-amber-500/5 border-amber-500/10' : 'bg-blue-500/5 border-blue-500/10';

    return (
        <div className={`${bg} border rounded-xl p-3 flex items-start gap-3`}>
            <Icon className={`w-5 h-5 ${color} mt-0.5 shrink-0`} />
            <div className="flex-1 min-w-0">
                <p className="text-xs font-semibold text-white">{title}</p>
                <div className="flex items-center gap-2 mt-1 text-[11px]">
                    <span className={color}>You: {current}</span>
                    <span className="text-slate-600">|</span>
                    <span className="text-slate-400">Benchmark: {benchmark}</span>
                </div>
            </div>
        </div>
    );
};

/* ─── Locked Hook Card ─── */
const LockedHookCard = ({ pillar, hookData }) => {
    if (!hookData) return null;
    const tierBg = hookData.tier === 'critical'
        ? 'border-red-500/20 bg-red-500/5'
        : hookData.tier === 'warn'
            ? 'border-amber-500/20 bg-amber-500/5'
            : 'border-primary/20 bg-primary/5';

    return (
        <div className={`relative border rounded-2xl p-4 overflow-hidden ${tierBg}`}>
            <div className="absolute inset-0 backdrop-blur-[2px] z-10 flex items-center justify-center">
                <div className="flex flex-col items-center gap-2">
                    <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center backdrop-blur-md">
                        <Lock className="w-5 h-5 text-white/60" />
                    </div>
                    <button className="px-4 py-1.5 bg-gradient-to-r from-amber-500 to-orange-500 text-[10px] font-bold text-black rounded-full uppercase tracking-wider shadow-lg hover:shadow-amber-500/20 transition-all active:scale-95">
                        Unlock Deep Insights
                    </button>
                </div>
            </div>
            <div className="relative z-0">
                <div className="flex items-center gap-2 mb-2">
                    <span className="text-lg">{pillar.icon}</span>
                    <h4 className="text-xs font-bold text-white uppercase tracking-wider">{pillar.name} Insight</h4>
                </div>
                <p className="text-sm text-slate-300 leading-relaxed blur-[3px] select-none">
                    {hookData.text}
                </p>
                <p className="text-[10px] text-slate-500 mt-2 italic blur-[2px] select-none">
                    {hookData.emotionalDriver}
                </p>
            </div>
        </div>
    );
};

/* ─── MAIN DASHBOARD ─── */
const FinancialDashboard = () => {
    const navigate = useNavigate();
    const { totalScore, scoreLabel, sortedPillars, mostCritical, rawData } = useFinancialHealthScore();
    const hookTexts = useHookText(sortedPillars, rawData);

    const {
        emergencyFundMonths = 0,
        emiToIncomeRatio = 0,
        savingsRate = 0,
        dti = 0,
        liquidAssets = 0,
        monthlyExpenses = 0,
        netWorth = 0,
        lifeCoverRatio = 0,
        equityPct = 0,
        targetEquityPct = 50,
    } = rawData || {};

    // Build red flags from raw data
    const redFlags = [];
    if (emergencyFundMonths < 6) {
        redFlags.push({ title: 'Emergency Fund Below 6 Months', current: `${emergencyFundMonths.toFixed(1)} mo`, benchmark: '6 months', severity: emergencyFundMonths < 3 ? 'critical' : 'warn' });
    }
    if (emiToIncomeRatio > 30) {
        redFlags.push({ title: 'EMI-to-Income Ratio Elevated', current: `${emiToIncomeRatio.toFixed(0)}%`, benchmark: '<30%', severity: emiToIncomeRatio > 40 ? 'critical' : 'warn' });
    }
    if (lifeCoverRatio < 1) {
        redFlags.push({ title: 'Under-Insured Life Cover', current: `${(lifeCoverRatio * 100).toFixed(0)}%`, benchmark: '100%', severity: lifeCoverRatio < 0.5 ? 'critical' : 'warn' });
    }
    if (savingsRate < 20) {
        redFlags.push({ title: 'Savings Rate Below Target', current: `${savingsRate.toFixed(0)}%`, benchmark: '20-30%', severity: savingsRate < 10 ? 'critical' : 'warn' });
    }
    if (equityPct < targetEquityPct * 0.5) {
        redFlags.push({ title: 'Equity Exposure Gap', current: `${equityPct.toFixed(0)}%`, benchmark: `${targetEquityPct.toFixed(0)}%`, severity: 'warn' });
    }

    // Sort flags by severity — critical first
    const severityOrder = { critical: 0, warn: 1, info: 2 };
    redFlags.sort((a, b) => severityOrder[a.severity] - severityOrder[b.severity]);

    // Priority actions (top 3)
    const priorityActions = sortedPillars.slice(0, 3).map(p => {
        const gain = Math.round(p.deficit * 0.5);
        let action = '';
        switch (p.id) {
            case 'survival': action = `Build emergency fund to ${Math.max(6, Math.ceil(emergencyFundMonths))} months`; break;
            case 'protection': action = `Increase life cover to close the insurance gap`; break;
            case 'debt': action = `Reduce EMI burden from ${emiToIncomeRatio.toFixed(0)}% to under 30%`; break;
            case 'wealth': action = `Increase savings rate & rebalance equity allocation`; break;
            case 'retirement': action = `Start/increase retirement SIP contributions`; break;
            default: action = `Improve ${p.name} score`;
        }
        return { pillar: p, action, pointGain: gain };
    });

    // Format helpers
    const formatInLakh = (v) => {
        if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
        if (v >= 100000) return `₹${(v / 100000).toFixed(1)} L`;
        return `₹${Math.round(v).toLocaleString('en-IN')}`;
    };

    return (
        <div className="min-h-screen bg-background-dark text-white">
            {/* Header */}
            <div className="sticky top-0 z-50 bg-background-dark/80 backdrop-blur-xl border-b border-white/5">
                <div className="max-w-[1200px] mx-auto px-4 py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 bg-primary/20 rounded-lg flex items-center justify-center">
                            <Zap className="w-4 h-4 text-primary" />
                        </div>
                        <h1 className="font-bold text-lg tracking-wide">Financial Health</h1>
                    </div>
                    <button
                        onClick={() => navigate('/')}
                        className="text-xs text-slate-400 hover:text-white transition-colors px-3 py-1.5 bg-surface-dark rounded-lg border border-white/5"
                    >
                        ← Home
                    </button>
                </div>
            </div>

            <div className="max-w-[1200px] mx-auto px-4 py-6 pb-24 space-y-6">

                {/* ── Score Ring Section ── */}
                <div className="bg-surface-dark rounded-3xl p-6 border border-white/5 shadow-xl">
                    <div className="flex flex-col lg:flex-row items-center gap-6">
                        <ScoreRing score={totalScore} label={scoreLabel.label} color={scoreLabel.color} />
                        <div className="flex-1 text-center lg:text-left">
                            <h2 className="text-2xl font-black tracking-tight mb-1">Your Financial Health Score</h2>
                            <p className="text-sm text-slate-400 mb-4">
                                Assessed across 5 pillars — Survival, Protection, Debt, Wealth & Retirement
                            </p>
                            {mostCritical && (
                                <div className="inline-flex items-center gap-2 bg-red-500/10 border border-red-500/20 px-4 py-2 rounded-xl">
                                    <AlertTriangle className="w-4 h-4 text-red-400" />
                                    <span className="text-xs">
                                        <span className="text-red-400 font-bold">PRIMARY RISK:</span>{' '}
                                        <span className="text-slate-300">{mostCritical.name} — {mostCritical.longInsight}</span>
                                    </span>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* ── Pillar Scores ── */}
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">Pillar Scores</h3>
                        <span className="text-[10px] text-slate-600">Sorted by priority</span>
                    </div>
                    <div className="grid gap-3">
                        {sortedPillars.map((p, i) => (
                            <PillarBar key={p.id} pillar={p} index={i} isWorst={i === 0} />
                        ))}
                    </div>
                </div>

                {/* ── Red Flags ── */}
                {redFlags.length > 0 && (
                    <div>
                        <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500 mb-3">
                            Top Financial Red Flags
                        </h3>
                        <div className="space-y-2">
                            {redFlags.slice(0, 4).map((flag, i) => (
                                <RedFlag key={i} {...flag} />
                            ))}
                        </div>
                    </div>
                )}

                {/* ── Priority Actions ── */}
                <div>
                    <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500 mb-3">
                        Priority Actions
                    </h3>
                    <div className="space-y-2">
                        {priorityActions.map((item, i) => (
                            <div key={i} className="bg-surface-dark border border-white/5 rounded-xl p-4 flex items-center gap-4 hover:border-primary/20 transition-colors">
                                <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-lg shrink-0">
                                    {item.pillar.icon}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm text-white font-medium">{item.action}</p>
                                    <p className="text-[11px] text-slate-500 mt-0.5">
                                        Est. improvement: <span className="text-primary font-bold">+{item.pointGain} pts</span>
                                    </p>
                                </div>
                                <ChevronRight className="w-4 h-4 text-slate-600 shrink-0" />
                            </div>
                        ))}
                    </div>
                </div>

                {/* ── Your Numbers vs Benchmarks ── */}
                <div>
                    <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500 mb-3">
                        Your Numbers vs. Benchmarks
                    </h3>
                    <div className="bg-surface-dark rounded-2xl border border-white/5 overflow-hidden">
                        {[
                            { label: 'Emergency Fund', yours: `${emergencyFundMonths.toFixed(1)} mo`, bench: '6 months', pct: Math.min(100, (emergencyFundMonths / 6) * 100) },
                            { label: 'EMI / Income', yours: `${emiToIncomeRatio.toFixed(0)}%`, bench: '<30%', pct: Math.min(100, 100 - emiToIncomeRatio * 2.5) },
                            { label: 'Savings Rate', yours: `${savingsRate.toFixed(0)}%`, bench: '20-30%', pct: Math.min(100, (savingsRate / 30) * 100) },
                            { label: 'Net Worth', yours: formatInLakh(netWorth), bench: '—', pct: Math.min(100, Math.max(0, netWorth > 0 ? 70 : 20)) },
                            { label: 'Equity Allocation', yours: `${equityPct.toFixed(0)}%`, bench: `${targetEquityPct.toFixed(0)}%`, pct: Math.min(100, (equityPct / Math.max(1, targetEquityPct)) * 100) },
                        ].map((row, i) => {
                            const barC = row.pct <= 40 ? '#ef4444' : row.pct <= 65 ? '#f59e0b' : '#0DF259';
                            return (
                                <div key={i} className={`flex items-center gap-4 px-4 py-3 ${i > 0 ? 'border-t border-white/5' : ''}`}>
                                    <span className="text-xs text-slate-400 w-28 shrink-0">{row.label}</span>
                                    <div className="flex-1 relative">
                                        <div className="h-1.5 w-full bg-white/5 rounded-full overflow-hidden">
                                            <div className="h-full rounded-full transition-all duration-1000" style={{ width: `${row.pct}%`, background: barC }} />
                                        </div>
                                    </div>
                                    <span className="text-xs font-bold text-white w-16 text-right tabular-nums">{row.yours}</span>
                                    <span className="text-[10px] text-slate-600 w-16 text-right">{row.bench}</span>
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* ── Premium Insights / Upgrade Hooks ── */}
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
                            Premium Insights
                        </h3>
                        <div className="flex items-center gap-1 text-amber-500">
                            <Lock className="w-3 h-3" />
                            <span className="text-[10px] font-bold uppercase tracking-widest">Pro</span>
                        </div>
                    </div>
                    <div className="grid gap-3 sm:grid-cols-2">
                        {sortedPillars.slice(0, 4).map(p => (
                            <LockedHookCard key={p.id} pillar={p} hookData={hookTexts[p.id]} />
                        ))}
                    </div>
                </div>

                {/* ── Upgrade CTA ── */}
                <div className="bg-gradient-to-br from-amber-500/10 via-orange-500/10 to-red-500/10 border border-amber-500/20 rounded-3xl p-6 text-center relative overflow-hidden">
                    <div className="absolute inset-0 opacity-30"
                        style={{ background: 'radial-gradient(circle at 50% 0%, rgba(245,158,11,0.2), transparent 60%)' }}
                    />
                    <div className="relative z-10">
                        <h3 className="text-xl font-black mb-2">Unlock Your Full Financial Blueprint</h3>
                        <p className="text-sm text-slate-400 mb-5 max-w-md mx-auto">
                            Get personalized action plans, detailed pillar breakdowns, and step-by-step recommendations to reach 90+ score.
                        </p>
                        <button className="px-8 py-3.5 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-background-dark font-bold rounded-xl shadow-[0_0_25px_rgba(245,158,11,0.3)] transition-all active:scale-[0.97]">
                            Upgrade to Premium
                            <span className="block text-[10px] font-medium opacity-80 mt-0.5">₹999/year — Cancel anytime</span>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default FinancialDashboard;
