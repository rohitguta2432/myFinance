import React, { useState } from 'react';
import {
    Shield, HeartPulse, Zap, ChevronDown, ChevronUp,
    CheckCircle2, AlertTriangle, Info, ArrowUpRight,
    Target, TrendingUp, Wallet, Building2, Check, X,
    ListChecks, Sparkles, Clock, BadgeAlert
} from 'lucide-react';

import { useActionPlan } from '../../../hooks/useActionPlan';
import { ActionPlanSkeleton } from '../../../components/ui/DashboardSkeleton';

/* ── Category / Pillar badge config ── */
const CATEGORY_CONFIG = {
    SRV: { label: 'Survival', bg: 'bg-red-500/15', text: 'text-red-400', border: 'border-red-500/20' },
    INS: { label: 'Protection', bg: 'bg-purple-500/15', text: 'text-purple-400', border: 'border-purple-500/20' },
    TAX: { label: 'Tax', bg: 'bg-amber-500/15', text: 'text-amber-400', border: 'border-amber-500/20' },
    RET: { label: 'Retirement', bg: 'bg-cyan-500/15', text: 'text-cyan-400', border: 'border-cyan-500/20' },
    WLT: { label: 'Wealth', bg: 'bg-blue-500/15', text: 'text-blue-400', border: 'border-blue-500/20' },
    DBT: { label: 'Debt', bg: 'bg-orange-500/15', text: 'text-orange-400', border: 'border-orange-500/20' },
};

const PILLAR_ICONS = {
    Survival: Shield,
    Protection: HeartPulse,
    Tax: Wallet,
    Retirement: TrendingUp,
    Wealth: Sparkles,
    Debt: Zap,
};

/* ── Expandable Do / Don't Panels (same pattern as InsuranceTab) ── */
const GuidancePanel = ({ items, type }) => {
    const [open, setOpen] = useState(false);
    const isDoList = type === 'do';
    const Icon = isDoList ? Check : X;
    const color = isDoList ? 'text-emerald-400' : 'text-red-400';
    const bg = isDoList ? 'bg-emerald-500/5 border-emerald-500/15' : 'bg-red-500/5 border-red-500/15';
    const headerBg = isDoList ? 'bg-emerald-500/10' : 'bg-red-500/10';
    const title = isDoList ? 'What To Do' : 'What Not To Do';

    return (
        <div className={`rounded-xl border ${bg} overflow-hidden`}>
            <button
                onClick={() => setOpen(!open)}
                className={`w-full flex items-center justify-between px-4 py-3 ${headerBg} hover:brightness-110 transition-all`}
            >
                <div className="flex items-center gap-2">
                    <Icon size={16} className={color} />
                    <span className={`text-sm font-bold tracking-wide ${color}`}>{title}</span>
                    <span className="text-xs text-slate-500 ml-1">{items.length} items</span>
                </div>
                {open ? <ChevronUp size={14} className="text-slate-500" /> : <ChevronDown size={14} className="text-slate-500" />}
            </button>
            {open && (
                <div className="px-4 py-3 space-y-2.5">
                    {items.map((item, idx) => (
                        <div key={idx} className="flex gap-2.5 items-start">
                            <Icon size={14} className={`${color} mt-0.5 shrink-0`} />
                            <span className="text-sm text-slate-300 leading-relaxed">{item}</span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

/* ── Steps Panel ── */
const StepsPanel = ({ steps }) => {
    const [open, setOpen] = useState(false);

    return (
        <div className="rounded-xl border bg-blue-500/5 border-blue-500/15 overflow-hidden">
            <button
                onClick={() => setOpen(!open)}
                className="w-full flex items-center justify-between px-4 py-3 bg-blue-500/10 hover:brightness-110 transition-all"
            >
                <div className="flex items-center gap-2">
                    <ListChecks size={16} className="text-blue-400" />
                    <span className="text-sm font-bold tracking-wide text-blue-400">Step-by-Step Guide</span>
                    <span className="text-xs text-slate-500 ml-1">{steps.length} steps</span>
                </div>
                {open ? <ChevronUp size={14} className="text-slate-500" /> : <ChevronDown size={14} className="text-slate-500" />}
            </button>
            {open && (
                <div className="px-4 py-3 space-y-3">
                    {steps.map((step, idx) => (
                        <div key={idx} className="flex gap-3 items-start">
                            <div className="w-6 h-6 rounded-full bg-blue-500/20 flex items-center justify-center shrink-0 mt-0.5">
                                <span className="text-xs font-bold text-blue-400">{idx + 1}</span>
                            </div>
                            <span className="text-sm text-slate-300 leading-relaxed">{step.text}</span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

/* ── Single Action Card ── */
const ActionCard = ({ action, rank }) => {
    const catConfig = CATEGORY_CONFIG[action.category] || CATEGORY_CONFIG.SRV;
    const PillarIcon = PILLAR_ICONS[action.pillar] || Shield;

    return (
        <div className="rounded-2xl border border-white/[0.06] bg-surface-dark/80 p-5 space-y-4 hover:border-white/10 transition-all">
            {/* Header: rank + badges + impact */}
            <div className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                    {/* Rank badge */}
                    <div className="w-10 h-10 rounded-lg bg-primary/15 flex items-center justify-center shrink-0">
                        <span className="text-base font-black text-primary">#{rank}</span>
                    </div>
                    <div className="space-y-1.5">
                        <div className="flex items-center gap-2 flex-wrap">
                            {/* Category badge */}
                            <span className={`px-2 py-0.5 rounded-md text-xs font-bold tracking-wider ${catConfig.bg} ${catConfig.text} border ${catConfig.border}`}>
                                {action.category}
                            </span>
                            {/* Pillar */}
                            <div className="flex items-center gap-1">
                                <PillarIcon size={12} className="text-slate-500" />
                                <span className="text-xs text-slate-500 font-medium">{action.pillar}</span>
                            </div>
                            {/* Scenario */}
                            <span className="text-xs text-slate-600 font-mono">{action.scenario}</span>
                        </div>
                        <h3 className="text-base font-bold text-white leading-snug">{action.title}</h3>
                        <p className="text-sm text-slate-400 leading-relaxed">{action.subtitle}</p>
                    </div>
                </div>
            </div>

            {/* Impact badge */}
            <div className="flex items-center gap-3 flex-wrap">
                <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-primary/10 border border-primary/20">
                    <ArrowUpRight size={14} className="text-primary" />
                    <span className="text-sm font-bold text-primary">Impact: {action.impact || 'HIGH'}</span>
                </div>
                {action.urgencyMultiplier >= 3 && (
                    <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-red-500/10 border border-red-500/20">
                        <Clock size={14} className="text-red-400" />
                        <span className="text-sm font-bold text-red-400">URGENT</span>
                    </div>
                )}
                {action.feasibilityFactor < 1 && (
                    <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-amber-500/10 border border-amber-500/20">
                        <BadgeAlert size={14} className="text-amber-400" />
                        <span className="text-sm font-bold text-amber-400">Build EF First</span>
                    </div>
                )}
            </div>

            {/* Expandable panels */}
            <div className="space-y-2">
                <GuidancePanel items={action.whatToDo} type="do" />
                <GuidancePanel items={action.whatNotToDo} type="dont" />
                <StepsPanel steps={action.steps} />
            </div>
        </div>
    );
};

/* ── Main Page ── */
export default function ActionPlanTab() {
    const { actions, count, fmt, isLoading } = useActionPlan();

    if (isLoading) return <ActionPlanSkeleton />;

    // FY label
    const now = new Date();
    const fyStart = now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1;
    const fyLabel = `FY ${fyStart}–${(fyStart + 1).toString().slice(-2)}`;

    return (
        <section className="min-h-screen pb-24">
            {/* Header */}
            <div className="w-full max-w-6xl mx-auto px-4 pt-10 space-y-6">


                {/* Page header */}
                <div className="space-y-2">
                    <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-primary/15 flex items-center justify-center">
                            <Target size={24} className="text-primary" />
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-white tracking-tight">Action Plan</h1>
                            <p className="text-sm text-slate-500">{fyLabel} · {count} action{count !== 1 ? 's' : ''} for your profile</p>
                        </div>
                    </div>

                    {count === 0 && (
                        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-6 text-center space-y-2">
                            <CheckCircle2 size={40} className="text-emerald-400 mx-auto" />
                            <p className="text-base font-bold text-emerald-400">All Clear!</p>
                            <p className="text-sm text-slate-400">No priority actions detected for your profile. You're doing great!</p>
                        </div>
                    )}

                    {count > 0 && (
                        <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-amber-500/5 border border-amber-500/15">
                            <AlertTriangle size={15} className="text-amber-400 shrink-0" />
                            <p className="text-sm text-amber-300/80">
                                Actions are ranked by <span className="font-bold">Financial Impact × Urgency × Feasibility</span>. Address #1 first.
                            </p>
                        </div>
                    )}
                </div>

                {/* Action cards */}
                <div className="space-y-4">
                    {actions.map((action, idx) => (
                        <ActionCard key={action.id} action={{ ...action, fmt }} rank={idx + 1} />
                    ))}
                </div>

                {/* Footer note */}
                {count > 0 && (
                    <div className="flex items-start gap-2 px-4 py-3 rounded-xl bg-white/[0.02] border border-white/[0.04]">
                        <Info size={15} className="text-slate-600 mt-0.5 shrink-0" />
                        <p className="text-sm text-slate-500 leading-relaxed">
                            These recommendations are based on the data you provided during assessment. They are not financial advice. 
                            Consult a SEBI-registered advisor for personalised planning.
                        </p>
                    </div>
                )}
            </div>
        </section>
    );
}
