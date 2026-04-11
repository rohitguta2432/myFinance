import React, { useState, useEffect } from 'react';
import { Shield, Lock, Zap, TrendingUp, AlertTriangle, ChevronRight, Info, ArrowUpRight, CheckCircle2, XCircle, AlertCircle, Wallet, PiggyBank, BarChart3, RefreshCw } from 'lucide-react';
import { SummarySkeleton } from '../../../components/ui/DashboardSkeleton';
import { useFinancialHealthScore } from '../../../hooks/useFinancialHealthScore';
import { useHookText } from '../../../hooks/useHookText';
import { useRedFlags } from '../../../hooks/useRedFlags';
import { usePriorityActions } from '../../../hooks/usePriorityActions';
import { useAssessmentStore } from '../../assessment/store/useAssessmentStore';
import BenchmarkComparison from '../components/BenchmarkComparison';
import LockedPremiumInsights from '../components/LockedPremiumInsights';
import FinancialTimeMachine from '../components/FinancialTimeMachine';
import PillarInterpretationCard from '../components/PillarInterpretationCard';
import SectionNav from '../components/SectionNav';
import ExcessReallocationCard from '../components/ExcessReallocationCard';

const DASHBOARD_SECTIONS = [
    { id: 'snapshot', label: 'Snapshot' },
    { id: 'score', label: 'Health Score' },
    { id: 'projections', label: 'Projections' },
    { id: 'red-flags', label: 'Red Flags' },
    { id: 'actions', label: 'Actions' },
    { id: 'benchmarks', label: 'Benchmarks' },
    { id: 'premium', label: 'Premium' },
];

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
                <circle cx="100" cy="100" r={radius} fill="none" className="stroke-slate-500/10" strokeWidth={stroke} />
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
            <div className="absolute flex flex-col items-center w-[140px] text-center">
                <span className="text-5xl font-bold tabular-nums tracking-tight text-slate-800"
                    style={{ textShadow: `0 0 20px ${color}30` }}
                >{Math.round(animatedScore)}</span>
                <span className="text-xs font-semibold uppercase tracking-[0.1em] mt-1 leading-tight" style={{ color }}>{label}</span>
                <span className="text-xs text-slate-500 mt-0.5">out of 100</span>
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
                <p className="text-base font-semibold text-white">{title}</p>
                <div className="flex items-center gap-2 mt-1 text-sm">
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
                    <button className="px-5 py-2 bg-gradient-to-r from-amber-500 to-orange-500 text-xs font-bold text-black rounded-full uppercase tracking-wider shadow-lg hover:shadow-amber-500/20 transition-all active:scale-95">
                        Unlock Deep Insights
                    </button>
                </div>
            </div>
            <div className="relative z-0">
                <div className="flex items-center gap-2 mb-2">
                    <span className="text-xl">{pillar.icon}</span>
                    <h4 className="text-base font-bold text-white uppercase tracking-wider">{pillar.name} Insight</h4>
                </div>
                <p className="text-base text-slate-300 leading-relaxed blur-[3px] select-none">
                    {hookData.text}
                </p>
                <p className="text-xs text-slate-500 mt-2 italic blur-[2px] select-none">
                    {hookData.emotionalDriver}
                </p>
            </div>
        </div>
    );
};

/* ─── MAIN DASHBOARD ─── */
const FinancialDashboard = ({ isPremium = false }) => {
    const { totalScore, scoreLabel, sortedPillars, mostCritical, rawData, isLoading } = useFinancialHealthScore();
    const hookTexts = useHookText(sortedPillars, rawData);
    const { allFlags, totalTriggered: flagsTriggered } = useRedFlags();
    const { allActions, totalTriggered: actionsTriggered } = usePriorityActions();

    // Free tier: 1 red flag visible, 3 priority actions visible
    const FREE_FLAGS_LIMIT = 1;
    const FREE_ACTIONS_LIMIT = 3;
    const visibleFlags = isPremium ? allFlags : allFlags.slice(0, FREE_FLAGS_LIMIT);
    const lockedFlags = isPremium ? [] : allFlags.slice(FREE_FLAGS_LIMIT);
    const visibleActions = isPremium ? allActions : allActions.slice(0, FREE_ACTIONS_LIMIT);
    const lockedActions = isPremium ? [] : allActions.slice(FREE_ACTIONS_LIMIT);
    const { city } = useAssessmentStore();

    // Inject city into rawData for hook text city health benchmark logic
    if (rawData) rawData.city = city;

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

    if (isLoading) return <SummarySkeleton />;

    return (
        <>

            <SectionNav sections={DASHBOARD_SECTIONS} />
            <div className="w-full max-w-6xl mx-auto px-6 lg:px-10 py-6 pb-24 space-y-6">

                {/* ── Greeting + Date ── */}
                <div id="snapshot" className="flex items-center justify-between">
                    <div>
                        <h2 className="text-2xl font-bold tracking-tight">Your Financial Snapshot</h2>
                        <p className="text-sm text-slate-400 mt-0.5">
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
                                    <p className="text-sm text-slate-500 uppercase tracking-wider font-medium">{stat.label}</p>
                                    <p className={`text-lg font-bold tabular-nums ${stat.color}`}>{stat.value}</p>
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* ── Score Overview — Ring + Pillars in One Block ── */}
                <div id="score" className="bg-surface-dark rounded-3xl p-6 border border-white/5 shadow-xl">
                    <div className="flex flex-col lg:flex-row gap-6">
                        {/* Left: Score Ring + Label */}
                        <div className="flex flex-col items-center lg:items-start gap-3 lg:w-[240px] shrink-0">
                            <p className="text-sm font-bold uppercase tracking-[0.2em] text-slate-500">Financial Health Score</p>
                            <ScoreRing score={totalScore} label={scoreLabel.label} color={scoreLabel.color} />
                            {mostCritical && (
                                <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 px-3 py-1.5 rounded-lg mt-1">
                                    <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
                                    <span className="text-sm text-slate-300 leading-tight">
                                        <span className="text-red-400 font-bold">RISK:</span> {mostCritical.name}
                                    </span>
                                </div>
                            )}
                        </div>

                        {/* Divider */}
                        <div className="hidden lg:block w-px bg-white/5 self-stretch" />
                        <div className="lg:hidden h-px bg-white/5 w-full" />

                        {/* Right: Pillar Breakdown (collapsible insights) */}
                        <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-3">
                                <h3 className="text-sm font-bold uppercase tracking-[0.2em] text-slate-500">Health Pillars</h3>
                                <span className="text-sm text-slate-600">Tap a pillar for insights</span>
                            </div>
                            <div className="grid gap-2">
                                {sortedPillars.map((p, i) => (
                                    <PillarInterpretationCard
                                        key={p.id}
                                        pillar={p}
                                        hookData={hookTexts[p.id]}
                                        index={i}
                                        isWorst={i === 0}
                                    />
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                {/* ── Financial Time Machine ── */}
                <div id="projections">
                    <FinancialTimeMachine isPremium={isPremium} />
                </div>

                {/* ── Red Flags ── */}
                {allFlags.length > 0 && (
                    <div id="red-flags">
                        <div className="flex items-center justify-between mb-3">
                            <h3 className="text-base font-bold uppercase tracking-[0.2em] text-slate-500">
                                Red Flags ({flagsTriggered})
                            </h3>
                            {!isPremium && flagsTriggered > FREE_FLAGS_LIMIT && (
                                <span className="text-xs text-amber-400 font-semibold">{flagsTriggered - FREE_FLAGS_LIMIT} locked</span>
                            )}
                        </div>
                        <div className="space-y-3">
                            {/* Visible flags — full detail */}
                            {visibleFlags.map((flag) => {
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
                                                    <span className={`px-2 py-1 ${s.badge} text-xs font-bold text-white uppercase tracking-widest rounded-full leading-none`}>
                                                        {flag.severity}
                                                    </span>
                                                    <h4 className="text-lg font-bold text-white">{flag.title}</h4>
                                                </div>
                                                <p className="text-base text-slate-400 leading-relaxed mb-2">
                                                    {flag.explanation}
                                                </p>
                                                <div className="flex items-start gap-2 bg-white/5 rounded-lg px-3 py-2">
                                                    <span className="text-base mt-px">📌</span>
                                                    <p className="text-sm text-slate-300 leading-relaxed">
                                                        {flag.action}
                                                    </p>
                                                </div>
                                                {flag.impact > 0 && (
                                                    <p className="text-sm text-slate-600 mt-2">
                                                        Financial impact: <span className={`font-semibold ${s.text}`}>{formatInLakh(flag.impact)}</span>
                                                        {flag.urgency > 1 && <span className="ml-2">⚡ {flag.urgency}× urgency</span>}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}

                            {/* Locked flags — title + severity visible, details blurred */}
                            {lockedFlags.map((flag) => {
                                const sevColors = {
                                    CRITICAL: { bg: 'bg-red-500/5', border: 'border-red-500/10', badge: 'bg-red-500', text: 'text-red-400', icon: XCircle },
                                    WARNING: { bg: 'bg-amber-500/5', border: 'border-amber-500/10', badge: 'bg-amber-500', text: 'text-amber-400', icon: AlertTriangle },
                                    INFO: { bg: 'bg-blue-500/5', border: 'border-blue-500/10', badge: 'bg-blue-500', text: 'text-blue-400', icon: Info },
                                };
                                const s = sevColors[flag.severity] || sevColors.WARNING;
                                const Icon = s.icon;
                                return (
                                    <div key={flag.id} className={`${s.bg} ${s.border} border rounded-xl p-4 relative overflow-hidden`}>
                                        <div className="flex items-start gap-3">
                                            <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center shrink-0 mt-0.5">
                                                <Icon className={`w-4 h-4 ${s.text}`} />
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-center gap-2 flex-wrap mb-1">
                                                    <span className={`px-2 py-1 ${s.badge} text-xs font-bold text-white uppercase tracking-widest rounded-full leading-none`}>
                                                        {flag.severity}
                                                    </span>
                                                    <h4 className="text-lg font-bold text-white">{flag.title}</h4>
                                                </div>
                                                <div className="blur-[6px] select-none pointer-events-none">
                                                    <p className="text-base text-slate-400 leading-relaxed mb-2">
                                                        {flag.explanation}
                                                    </p>
                                                    <div className="flex items-start gap-2 bg-white/5 rounded-lg px-3 py-2">
                                                        <span className="text-base mt-px">📌</span>
                                                        <p className="text-sm text-slate-300 leading-relaxed">{flag.action}</p>
                                                    </div>
                                                </div>
                                            </div>
                                            <Lock className="w-4 h-4 text-amber-500/60 shrink-0 mt-1" />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>

                        {/* Upgrade CTA for locked flags */}
                        {lockedFlags.length > 0 && (
                            <div className="mt-3 bg-gradient-to-r from-amber-500/5 to-orange-500/5 border border-amber-500/15 rounded-xl p-4 flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <Lock className="w-5 h-5 text-amber-400" />
                                    <span className="text-sm text-slate-400">
                                        Unlock all {flagsTriggered} red flags with detailed action plans
                                    </span>
                                </div>
                                <span className="text-sm text-amber-400 font-bold whitespace-nowrap">Upgrade →</span>
                            </div>
                        )}
                    </div>
                )}

                {allActions.length > 0 && (
                <div id="actions">
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="text-sm font-bold uppercase tracking-[0.2em] text-slate-500">
                            Priority Actions ({actionsTriggered})
                        </h3>
                        {!isPremium && actionsTriggered > FREE_ACTIONS_LIMIT && (
                            <span className="text-xs text-amber-400 font-semibold">{actionsTriggered - FREE_ACTIONS_LIMIT} locked</span>
                        )}
                    </div>
                    <div className="space-y-3">
                        {/* Visible actions — full detail */}
                        {visibleActions.map((act, i) => (
                            <div key={act.id} className="bg-surface-dark border border-white/5 rounded-xl p-4 hover:border-primary/20 transition-colors">
                                <div className="flex items-start gap-3">
                                    <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center text-lg shrink-0">
                                        {act.icon}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="px-2 py-1 bg-emerald-500/15 text-xs font-bold text-emerald-400 uppercase tracking-widest rounded-full leading-none">
                                                #{i + 1}
                                            </span>
                                            <h4 className="text-base font-bold text-white">{act.title}</h4>
                                        </div>
                                        <p className="text-sm text-slate-400 leading-relaxed mb-2">
                                            {act.description}
                                        </p>
                                        {act.howTo && (
                                            <div className="flex items-start gap-2 bg-white/5 rounded-lg px-3 py-2 mb-2">
                                                <span className="text-base mt-px">▶</span>
                                                <p className="text-sm text-slate-300 leading-relaxed">
                                                    {act.howTo}
                                                </p>
                                            </div>
                                        )}
                                        <div className="flex items-center gap-3 text-xs">
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

                        {/* Locked actions — title visible, details blurred */}
                        {lockedActions.map((act, i) => (
                            <div key={act.id} className="bg-surface-dark border border-white/5 rounded-xl p-4 relative overflow-hidden">
                                <div className="flex items-start gap-3">
                                    <div className="w-9 h-9 rounded-lg bg-white/5 flex items-center justify-center text-lg shrink-0 opacity-50">
                                        {act.icon}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="px-2 py-1 bg-slate-500/15 text-xs font-bold text-slate-400 uppercase tracking-widest rounded-full leading-none">
                                                #{FREE_ACTIONS_LIMIT + i + 1}
                                            </span>
                                            <h4 className="text-base font-bold text-white">{act.title}</h4>
                                        </div>
                                        <div className="blur-[6px] select-none pointer-events-none">
                                            <p className="text-sm text-slate-400 leading-relaxed mb-2">
                                                {act.description}
                                            </p>
                                            {act.howTo && (
                                                <div className="flex items-start gap-2 bg-white/5 rounded-lg px-3 py-2">
                                                    <span className="text-base mt-px">▶</span>
                                                    <p className="text-sm text-slate-300">{act.howTo}</p>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                    <Lock className="w-4 h-4 text-amber-500/60 shrink-0 mt-1" />
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Upgrade CTA for locked actions */}
                    {lockedActions.length > 0 && (
                        <div className="mt-3 bg-gradient-to-r from-amber-500/5 to-orange-500/5 border border-amber-500/15 rounded-xl p-4 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <Lock className="w-5 h-5 text-amber-400" />
                                <span className="text-sm text-slate-400">
                                    Unlock all {actionsTriggered} actions with step-by-step guides
                                </span>
                            </div>
                            <span className="text-sm text-amber-400 font-bold whitespace-nowrap">Upgrade →</span>
                        </div>
                    )}
                </div>
                )}

                {/* ── Your Numbers vs Benchmarks (Personalised) ── */}
                <div id="benchmarks">
                    <BenchmarkComparison />
                </div>

                {/* ── Excess Reallocation (surplus → retirement) ── */}
                <ExcessReallocationCard />

                {/* ── Premium Insights (Locked Cards) ── */}
                <div id="premium">
                    <LockedPremiumInsights />
                </div>

                {/* ── Upgrade CTA ── */}
                <div className="bg-gradient-to-br from-amber-500/10 via-orange-500/10 to-red-500/10 border border-amber-500/20 rounded-3xl p-6 text-center relative overflow-hidden">
                    <div className="absolute inset-0 opacity-30"
                        style={{ background: 'radial-gradient(circle at 50% 0%, rgba(245,158,11,0.2), transparent 60%)' }}
                    />
                    <div className="relative z-10">
                        <h3 className="text-2xl font-bold mb-2">Unlock Your Full Financial Blueprint</h3>
                        <p className="text-base text-slate-400 mb-5 max-w-lg mx-auto">
                            Get personalized action plans, detailed pillar breakdowns, and step-by-step recommendations to reach 90+ score.
                        </p>
                        <button className="px-8 py-3.5 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-background-dark font-bold rounded-xl shadow-[0_0_25px_rgba(245,158,11,0.3)] transition-all active:scale-[0.97]">
                            Upgrade to Premium
                            <span className="block text-xs font-medium opacity-80 mt-0.5">₹999/year — Cancel anytime</span>
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
};

export default FinancialDashboard;
