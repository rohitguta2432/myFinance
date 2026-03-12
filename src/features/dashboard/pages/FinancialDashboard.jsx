import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield, Lock, Zap, TrendingUp, AlertTriangle, ChevronRight, Info, ArrowUpRight, CheckCircle2, XCircle, AlertCircle, Wallet, PiggyBank, BarChart3, RefreshCw } from 'lucide-react';
import { useFinancialHealthScore } from '../../../hooks/useFinancialHealthScore';
import { useHookText } from '../../../hooks/useHookText';
import { useRedFlags } from '../../../hooks/useRedFlags';
import { usePriorityActions } from '../../../hooks/usePriorityActions';
import { useAssessmentStore } from '../../assessment/store/useAssessmentStore';

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

/* ─── Pillar Bar (compact for inline display) ─── */
const PillarBar = ({ pillar, index, isWorst }) => {
    const pct = (pillar.score / pillar.maxScore) * 100;
    const barColor = pct <= 40 ? '#ef4444' : pct <= 65 ? '#f59e0b' : '#0DF259';

    return (
        <div className={`relative rounded-xl px-3 py-2.5 border transition-all duration-300 ${isWorst ? 'border-red-500/30 bg-red-500/5' : 'border-white/5 hover:border-white/10'}`}>
            <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2.5">
                    <span className="text-lg">{pillar.icon}</span>
                    <h3 className="font-bold text-white text-sm">{pillar.name}</h3>
                    {isWorst && (
                        <span className="px-1.5 py-0.5 bg-red-500 text-[8px] font-bold text-white uppercase tracking-widest rounded-full leading-none">Risk</span>
                    )}
                </div>
                <div className="flex items-baseline gap-0.5">
                    <span className="text-base font-black tabular-nums" style={{ color: barColor }}>{pillar.score}</span>
                    <span className="text-[10px] text-slate-500">/{pillar.maxScore}</span>
                </div>
            </div>
            <div className="w-full h-1.5 bg-white/5 rounded-full overflow-hidden">
                <div
                    className="h-full rounded-full transition-all duration-1000 ease-out"
                    style={{
                        width: `${pct}%`,
                        background: `linear-gradient(90deg, ${barColor}cc, ${barColor})`,
                        boxShadow: `0 0 8px ${barColor}30`,
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
    const { topFlags, hiddenCount: flagsHidden, totalTriggered: flagsTriggered } = useRedFlags();
    const { topActions, hiddenCount: actionsHidden, totalTriggered: actionsTriggered } = usePriorityActions();
    const { city } = useAssessmentStore();

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
        monthlySurplus = 0,
        grossIncome = 0,
    } = rawData || {};





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
                        onClick={() => navigate('/assessment/step-1')}
                        className="text-xs text-slate-400 hover:text-white transition-colors px-3 py-1.5 bg-surface-dark rounded-lg border border-white/5"
                    >
                        ↻ Retake Assessment
                    </button>
                </div>
            </div>

            <div className="max-w-[1200px] mx-auto px-4 py-6 pb-24 space-y-6">

                {/* ── Greeting + Date ── */}
                <div className="flex items-center justify-between">
                    <div>
                        <h2 className="text-xl font-black tracking-tight">Your Financial Snapshot</h2>
                        <p className="text-xs text-slate-500 mt-0.5">
                            {city ? `📍 ${city}` : ''} · Last assessed {new Date().toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                        </p>
                    </div>
                </div>

                {/* ── Quick Stats Strip ── */}
                <div className="grid grid-cols-3 gap-3">
                    {[
                        { label: 'Net Worth', value: formatInLakh(netWorth), icon: Wallet, color: netWorth > 0 ? 'text-primary' : 'text-red-400' },
                        { label: 'Monthly Surplus', value: formatInLakh(monthlySurplus), icon: PiggyBank, color: monthlySurplus > 0 ? 'text-primary' : 'text-red-400' },
                        { label: 'Savings Rate', value: `${savingsRate.toFixed(0)}%`, icon: BarChart3, color: savingsRate >= 20 ? 'text-primary' : savingsRate >= 10 ? 'text-amber-400' : 'text-red-400' },
                    ].map((stat, i) => {
                        const Icon = stat.icon;
                        return (
                            <div key={i} className="bg-surface-dark rounded-xl border border-white/5 p-3 flex items-center gap-3">
                                <div className="w-9 h-9 rounded-lg bg-white/5 flex items-center justify-center shrink-0">
                                    <Icon className={`w-4 h-4 ${stat.color}`} />
                                </div>
                                <div className="min-w-0">
                                    <p className="text-[10px] text-slate-500 uppercase tracking-wider font-medium">{stat.label}</p>
                                    <p className={`text-sm font-bold tabular-nums ${stat.color}`}>{stat.value}</p>
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* ── Score Overview — Ring + Pillars in One Block ── */}
                <div className="bg-surface-dark rounded-3xl p-6 border border-white/5 shadow-xl">
                    <div className="flex flex-col lg:flex-row gap-6">
                        {/* Left: Score Ring + Label */}
                        <div className="flex flex-col items-center lg:items-start gap-3 lg:w-[240px] shrink-0">
                            <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-500">Financial Health Score</p>
                            <ScoreRing score={totalScore} label={scoreLabel.label} color={scoreLabel.color} />
                            {mostCritical && (
                                <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 px-3 py-1.5 rounded-lg mt-1">
                                    <AlertTriangle className="w-3.5 h-3.5 text-red-400 shrink-0" />
                                    <span className="text-[10px] text-slate-300 leading-tight">
                                        <span className="text-red-400 font-bold">RISK:</span> {mostCritical.name}
                                    </span>
                                </div>
                            )}
                        </div>

                        {/* Divider */}
                        <div className="hidden lg:block w-px bg-white/5 self-stretch" />
                        <div className="lg:hidden h-px bg-white/5 w-full" />

                        {/* Right: Pillar Breakdown */}
                        <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-3">
                                <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">Score Breakdown by Pillar</h3>
                                <span className="text-[10px] text-slate-600">Sorted by priority</span>
                            </div>
                            <div className="grid gap-3">
                                {sortedPillars.map((p, i) => (
                                    <PillarBar key={p.id} pillar={p} index={i} isWorst={i === 0} />
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                {/* ── Emergency Fund Insight ── */}
                {(() => {
                    const targetMonths = 6;
                    const targetAmount = monthlyExpenses * targetMonths;
                    const shortfall = Math.max(0, targetAmount - liquidAssets);
                    const coverageDays = monthlyExpenses > 0 ? (liquidAssets / monthlyExpenses) * 30 : 0;
                    const coverageLabel = emergencyFundMonths < 1
                        ? `${Math.round(coverageDays)} Days`
                        : `${emergencyFundMonths.toFixed(1)} Months`;
                    const isHealthy = emergencyFundMonths >= targetMonths;
                    const borderColor = isHealthy ? 'border-primary/20' : emergencyFundMonths < 3 ? 'border-red-500/30' : 'border-amber-500/30';
                    const bgColor = isHealthy ? 'bg-primary/5' : emergencyFundMonths < 3 ? 'bg-red-500/5' : 'bg-amber-500/5';
                    const accentColor = isHealthy ? 'text-primary' : emergencyFundMonths < 3 ? 'text-red-400' : 'text-amber-400';

                    return (
                        <div className={`${bgColor} ${borderColor} border rounded-2xl p-5`}>
                            <div className="flex items-start gap-4">
                                <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-xl shrink-0">
                                    🛡️
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 flex-wrap mb-1">
                                        <h3 className="font-bold text-white text-sm">Emergency Fund:</h3>
                                        <span className={`font-black text-sm ${accentColor}`}>
                                            {coverageLabel} Only
                                        </span>
                                        {!isHealthy && (
                                            <span className="text-xs text-red-400 font-semibold">
                                                &amp; {formatInLakh(shortfall)} shortfall
                                            </span>
                                        )}
                                    </div>
                                    <p className="text-xs text-slate-400 leading-relaxed mb-2.5">
                                        You have <span className={`font-semibold ${accentColor}`}>{coverageLabel.toLowerCase()}</span> of
                                        expenses covered. Minimum safe level is <span className="text-white font-semibold">{targetMonths} months</span>.
                                    </p>
                                    {!isHealthy && (
                                        <div className="flex items-start gap-2 bg-white/5 rounded-lg px-3 py-2">
                                            <span className="text-sm mt-px">📌</span>
                                            <p className="text-xs text-slate-300 leading-relaxed">
                                                Park <span className="text-white font-bold">{formatInLakh(shortfall)}</span> more in a liquid MF or savings account.
                                            </p>
                                        </div>
                                    )}
                                    {isHealthy && (
                                        <div className="flex items-start gap-2 bg-white/5 rounded-lg px-3 py-2">
                                            <span className="text-sm mt-px">✅</span>
                                            <p className="text-xs text-slate-300 leading-relaxed">
                                                Your emergency fund covers <span className="text-primary font-bold">{coverageLabel.toLowerCase()}</span> — well above the 6-month minimum. Great job!
                                            </p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    );
                })()}

                {/* ── Red Flags ── */}
                {topFlags.length > 0 && (
                    <div>
                        <div className="flex items-center justify-between mb-3">
                            <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
                                Top {topFlags.length} Red Flags
                            </h3>
                            {flagsTriggered > 3 && (
                                <span className="text-[10px] text-slate-600">{flagsTriggered} total detected</span>
                            )}
                        </div>
                        <div className="space-y-3">
                            {topFlags.map((flag, i) => {
                                const sevColors = {
                                    CRITICAL: { bg: 'bg-red-500/8', border: 'border-red-500/25', badge: 'bg-red-500', text: 'text-red-400', icon: XCircle },
                                    WARNING: { bg: 'bg-amber-500/8', border: 'border-amber-500/20', badge: 'bg-amber-500', text: 'text-amber-400', icon: AlertTriangle },
                                    INFO: { bg: 'bg-blue-500/8', border: 'border-blue-500/20', badge: 'bg-blue-500', text: 'text-blue-400', icon: Info },
                                };
                                const s = sevColors[flag.severity] || sevColors.WARNING;
                                const Icon = s.icon;
                                return (
                                    <div key={flag.id} className={`${s.bg} ${s.border} border rounded-xl p-4`}>
                                        <div className="flex items-start gap-3">
                                            <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center shrink-0 mt-0.5">
                                                <Icon className={`w-4 h-4 ${s.text}`} />
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-center gap-2 flex-wrap mb-1">
                                                    <span className={`px-1.5 py-0.5 ${s.badge} text-[8px] font-bold text-white uppercase tracking-widest rounded-full leading-none`}>
                                                        {flag.severity}
                                                    </span>
                                                    <h4 className="text-sm font-bold text-white">{flag.title}</h4>
                                                </div>
                                                <p className="text-xs text-slate-400 leading-relaxed mb-2">
                                                    {flag.explanation}
                                                </p>
                                                <div className="flex items-start gap-2 bg-white/5 rounded-lg px-3 py-2">
                                                    <span className="text-sm mt-px">📌</span>
                                                    <p className="text-xs text-slate-300 leading-relaxed">
                                                        {flag.action}
                                                    </p>
                                                </div>
                                                {flag.impact > 0 && (
                                                    <p className="text-[10px] text-slate-600 mt-1.5">
                                                        Financial impact: <span className={`font-semibold ${s.text}`}>{formatInLakh(flag.impact)}</span>
                                                        {flag.urgency > 1 && <span className="ml-2">⚡ {flag.urgency}× urgency</span>}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                        {flagsHidden > 0 && (
                            <div className="mt-3 bg-surface-dark border border-white/5 rounded-xl p-3 flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <Lock className="w-3.5 h-3.5 text-slate-500" />
                                    <span className="text-[11px] text-slate-500">
                                        +{flagsHidden} more flag{flagsHidden > 1 ? 's' : ''} detected
                                    </span>
                                </div>
                                <span className="text-[10px] text-primary font-semibold">Unlock Premium →</span>
                            </div>
                        )}
                    </div>
                )}

                {topActions.length > 0 && (
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
                            Priority Actions
                        </h3>
                        {actionsTriggered > 3 && (
                            <span className="text-[10px] text-slate-600">{actionsTriggered} actions identified</span>
                        )}
                    </div>
                    <div className="space-y-3">
                        {topActions.map((act, i) => (
                            <div key={act.id} className="bg-surface-dark border border-white/5 rounded-xl p-4 hover:border-primary/20 transition-colors">
                                <div className="flex items-start gap-3">
                                    <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center text-lg shrink-0">
                                        {act.icon}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="px-1.5 py-0.5 bg-emerald-500/15 text-[8px] font-bold text-emerald-400 uppercase tracking-widest rounded-full leading-none">
                                                #{i + 1}
                                            </span>
                                            <h4 className="text-sm font-bold text-white">{act.title}</h4>
                                        </div>
                                        <p className="text-xs text-slate-400 leading-relaxed mb-2">
                                            {act.description}
                                        </p>
                                        <div className="flex items-center gap-3 text-[10px]">
                                            <span className="text-slate-500">
                                                Impact: <span className="text-emerald-400 font-semibold">{formatInLakh(act.impact)}</span>
                                            </span>
                                            {act.urgency > 1 && (
                                                <span className="text-amber-400">
                                                    ⚡ {act.urgency}× urgency
                                                </span>
                                            )}
                                            {act.feasibility < 1 && (
                                                <span className="text-rose-400">
                                                    ⚠ Low feasibility
                                                </span>
                                            )}
                                            {act.actionCost > 0 && (
                                                <span className="text-slate-600">
                                                    Cost: {formatInLakh(act.actionCost)}/mo
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <ChevronRight className="w-4 h-4 text-slate-600 shrink-0 mt-2" />
                                </div>
                            </div>
                        ))}
                    </div>
                    {actionsHidden > 0 && (
                        <div className="mt-3 bg-surface-dark border border-white/5 rounded-xl p-3 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <Lock className="w-3.5 h-3.5 text-slate-500" />
                                <span className="text-[11px] text-slate-500">
                                    +{actionsHidden} more action{actionsHidden > 1 ? 's' : ''} identified
                                </span>
                            </div>
                            <span className="text-[10px] text-primary font-semibold">Unlock Premium →</span>
                        </div>
                    )}
                </div>
                )}

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
