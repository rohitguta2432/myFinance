import React, { useState } from 'react';
import { ChevronDown, ChevronUp, AlertTriangle, XCircle, CheckCircle2, Zap } from 'lucide-react';

/**
 * PillarInterpretationCard (collapsed-by-default)
 *
 * Header row (always visible):
 *   icon | name | status badge | score | [▼ Insight button]
 *   progress bar
 *
 * Expanded (on tap):
 *   hook text · dscr/equity warnings · "What Should I Do?" panel
 */

const STATUS_CONFIG = {
    critical: {
        bg: 'bg-red-500/8',
        border: 'border-red-500/25',
        glow: '0 0 20px rgba(239, 68, 68, 0.1)',
        chipBg: 'bg-red-500',
        chipText: 'text-white',
        textColor: 'text-red-400',
        barFrom: '#ef4444cc',
        barTo: '#ef4444',
        Icon: XCircle,
        label: 'CRITICAL',
    },
    warn: {
        bg: 'bg-amber-500/8',
        border: 'border-amber-500/20',
        glow: '0 0 20px rgba(245, 158, 11, 0.08)',
        chipBg: 'bg-amber-500',
        chipText: 'text-black',
        textColor: 'text-amber-400',
        barFrom: '#f59e0bcc',
        barTo: '#f59e0b',
        Icon: AlertTriangle,
        label: 'WARNING',
    },
    ok: {
        bg: 'bg-emerald-500/8',
        border: 'border-emerald-500/20',
        glow: '0 0 20px rgba(16, 185, 129, 0.08)',
        chipBg: 'bg-emerald-500',
        chipText: 'text-black',
        textColor: 'text-emerald-400',
        barFrom: '#10b981cc',
        barTo: '#10b981',
        Icon: CheckCircle2,
        label: 'ON TRACK',
    },
};

const PillarInterpretationCard = ({ pillar, hookData, index = 0, isWorst = false }) => {
    const [expanded, setExpanded] = useState(false);
    const [showAction, setShowAction] = useState(false);

    if (!pillar || !hookData) return null;

    const config = STATUS_CONFIG[hookData.tier] || STATUS_CONFIG.ok;
    const pct = ((pillar.score / pillar.maxScore) * 100).toFixed(0);

    return (
        <div
            className={`${config.bg} ${config.border} border rounded-2xl overflow-hidden transition-all duration-500`}
            style={{
                boxShadow: config.glow,
                animation: `fadeSlideUp 0.5s ease-out ${index * 0.08}s both`,
            }}
        >
            {/* ── Always-visible header ── */}
            <button
                onClick={() => {
                    setExpanded(!expanded);
                    if (expanded) setShowAction(false); // reset nested panel when collapsing
                }}
                className="w-full text-left px-4 pt-3 pb-3 focus:outline-none"
            >
                <div className="flex items-center justify-between mb-2.5">
                    <div className="flex items-center gap-2.5 min-w-0">
                        <span className="text-xl shrink-0">{pillar.icon}</span>
                        <h4 className="text-base font-bold text-white truncate">{pillar.name}</h4>
                        <span
                            className={`px-2 py-0.5 ${config.chipBg} ${config.chipText} text-xs font-bold uppercase tracking-widest rounded-full leading-none shrink-0`}
                        >
                            {config.label}
                        </span>
                        {isWorst && (
                            <span className="px-2 py-1 bg-red-500 text-xs font-bold text-white uppercase tracking-widest rounded-full leading-none shrink-0">
                                RISK
                            </span>
                        )}
                    </div>

                    <div className="flex items-center gap-2 shrink-0 ml-2">
                        <div className="flex items-baseline gap-0.5">
                            <span
                                className="text-xl font-bold tabular-nums"
                                style={{ color: config.barTo }}
                            >
                                {pillar.score}
                            </span>
                            <span className="text-xs text-slate-500">/{pillar.maxScore}</span>
                        </div>
                        <div className={`flex items-center gap-0.5 text-xs font-semibold ${config.textColor} transition-transform duration-200`}>
                            {expanded
                                ? <ChevronUp className="w-4 h-4" />
                                : <ChevronDown className="w-4 h-4" />
                            }
                        </div>
                    </div>
                </div>

                {/* Score bar */}
                <div className="w-full h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <div
                        className="h-full rounded-full transition-all duration-1000 ease-out"
                        style={{
                            width: `${pct}%`,
                            background: `linear-gradient(90deg, ${config.barFrom}, ${config.barTo})`,
                        }}
                    />
                </div>
            </button>

            {/* ── Expandable insight body ── */}
            <div
                className={`overflow-hidden transition-all duration-300 ease-out ${expanded ? 'max-h-[600px] opacity-100' : 'max-h-0 opacity-0'}`}
            >
                <div className="px-4 pb-4 space-y-3 border-t border-white/5 pt-3">
                    {/* Hook text */}
                    <p className="text-sm text-slate-300 leading-relaxed">
                        {hookData.text}
                    </p>

                    {/* DSCR Override Warning */}
                    {hookData.dscrOverride && (
                        <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">
                            <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                            <p className="text-xs text-red-300 leading-relaxed">
                                Your current EMIs exceed your disposable income — immediate restructuring is required.
                            </p>
                        </div>
                    )}

                    {/* Equity Override Warning */}
                    {hookData.equityOverride && (
                        <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">
                            <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                            <p className="text-xs text-red-300 leading-relaxed">
                                With zero equity, inflation is eroding the real value of your savings every year.
                            </p>
                        </div>
                    )}

                    {/* "What Should I Do?" toggle */}
                    <button
                        onClick={(e) => { e.stopPropagation(); setShowAction(!showAction); }}
                        className={`flex items-center gap-2 text-sm font-semibold transition-all duration-200 ${config.textColor} hover:opacity-80`}
                    >
                        <Zap className="w-4 h-4" />
                        {showAction ? 'Hide Action' : 'What Should I Do?'}
                        {showAction
                            ? <ChevronUp className="w-4 h-4" />
                            : <ChevronDown className="w-4 h-4" />
                        }
                    </button>

                    {/* Expandable action panel */}
                    <div
                        className={`overflow-hidden transition-all duration-300 ease-out ${showAction ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}
                    >
                        <div className="bg-white/5 rounded-xl px-4 py-3 border border-white/5">
                            <div className="flex items-start gap-2">
                                <span className="text-base mt-px">📌</span>
                                <p className="text-sm text-slate-300 leading-relaxed">
                                    {hookData.action}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PillarInterpretationCard;
