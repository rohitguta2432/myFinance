import React, { useState } from 'react';
import { ChevronDown, ChevronUp, AlertTriangle, XCircle, CheckCircle2, Zap } from 'lucide-react';

/**
 * PillarInterpretationCard
 * 
 * Renders one pillar's score interpretation with:
 * - Status chip (CRITICAL / WARNING / OK)
 * - Personalised hook text with live variables
 * - Expandable action panel
 */

const STATUS_CONFIG = {
    critical: {
        bg: 'bg-red-500/8',
        border: 'border-red-500/25',
        glow: '0 0 20px rgba(239, 68, 68, 0.1)',
        chipBg: 'bg-red-500',
        chipText: 'text-white',
        textColor: 'text-red-400',
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
        Icon: CheckCircle2,
        label: 'ON TRACK',
    },
};

const PillarInterpretationCard = ({ pillar, hookData, index = 0 }) => {
    const [showAction, setShowAction] = useState(false);

    if (!pillar || !hookData) return null;

    const config = STATUS_CONFIG[hookData.tier] || STATUS_CONFIG.ok;
    const { Icon } = config;
    const pct = ((pillar.score / pillar.maxScore) * 100).toFixed(0);

    return (
        <div
            className={`${config.bg} ${config.border} border rounded-2xl p-4 transition-all duration-500 hover:border-opacity-50`}
            style={{
                boxShadow: config.glow,
                animation: `fadeSlideUp 0.5s ease-out ${index * 0.1}s both`,
            }}
        >
            {/* Header Row */}
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2.5">
                    <span className="text-xl">{pillar.icon}</span>
                    <h4 className="text-sm font-bold text-white">{pillar.name}</h4>
                    <span className={`px-2 py-0.5 ${config.chipBg} ${config.chipText} text-[9px] font-bold uppercase tracking-widest rounded-full leading-none`}>
                        {config.label}
                    </span>
                </div>
                <div className="flex items-baseline gap-1">
                    <span className="text-base font-black tabular-nums" style={{ color: hookData.tier === 'critical' ? '#ef4444' : hookData.tier === 'warn' ? '#f59e0b' : '#10b981' }}>
                        {pillar.score}
                    </span>
                    <span className="text-[10px] text-slate-500">/{pillar.maxScore}</span>
                </div>
            </div>

            {/* Score Bar */}
            <div className="w-full h-1.5 bg-white/5 rounded-full overflow-hidden mb-3">
                <div
                    className="h-full rounded-full transition-all duration-1000 ease-out"
                    style={{
                        width: `${pct}%`,
                        background: hookData.tier === 'critical'
                            ? 'linear-gradient(90deg, #ef4444cc, #ef4444)'
                            : hookData.tier === 'warn'
                                ? 'linear-gradient(90deg, #f59e0bcc, #f59e0b)'
                                : 'linear-gradient(90deg, #10b981cc, #10b981)',
                    }}
                />
            </div>

            {/* Hook Text */}
            <p className="text-[13px] text-slate-300 leading-relaxed mb-3">
                {hookData.text}
            </p>

            {/* DSCR Override Warning */}
            {hookData.dscrOverride && (
                <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2 mb-3">
                    <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                    <p className="text-xs text-red-300 leading-relaxed">
                        Your current EMIs exceed your disposable income — immediate restructuring is required.
                    </p>
                </div>
            )}

            {/* Equity Override Warning */}
            {hookData.equityOverride && (
                <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2 mb-3">
                    <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                    <p className="text-xs text-red-300 leading-relaxed">
                        With zero equity, inflation is eroding the real value of your savings every year.
                    </p>
                </div>
            )}

            {/* Action Toggle Button */}
            <button
                onClick={() => setShowAction(!showAction)}
                className={`flex items-center gap-2 text-xs font-semibold transition-all duration-200 ${config.textColor} hover:opacity-80`}
            >
                <Zap className="w-3.5 h-3.5" />
                {showAction ? 'Hide Action' : 'What Should I Do?'}
                {showAction
                    ? <ChevronUp className="w-3.5 h-3.5" />
                    : <ChevronDown className="w-3.5 h-3.5" />
                }
            </button>

            {/* Expandable Action Panel */}
            <div
                className={`overflow-hidden transition-all duration-300 ease-out ${showAction ? 'max-h-40 opacity-100 mt-3' : 'max-h-0 opacity-0'}`}
            >
                <div className="bg-white/5 rounded-xl px-4 py-3 border border-white/5">
                    <div className="flex items-start gap-2">
                        <span className="text-sm mt-px">📌</span>
                        <p className="text-xs text-slate-300 leading-relaxed">
                            {hookData.action}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PillarInterpretationCard;
