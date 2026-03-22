import React, { useState, useEffect } from 'react';
import { ChevronRight, Flame } from 'lucide-react';
import { useTimeMachine } from '../../../hooks/useTimeMachine';
import ProjectionChart from './ProjectionChart';

const FinancialTimeMachine = () => {
    const data = useTimeMachine();
    const [tickerCost, setTickerCost] = useState(0);

    // Animate the daily cost number counting up
    useEffect(() => {
        if (!data) return;
        const target = data.dailyCost;
        const duration = 1500;
        const steps = 40;
        const increment = target / steps;
        let current = 0;
        let step = 0;
        const timer = setInterval(() => {
            step++;
            current = Math.min(target, Math.round(increment * step));
            setTickerCost(current);
            if (step >= steps) clearInterval(timer);
        }, duration / steps);
        return () => clearInterval(timer);
    }, [data?.dailyCost]);

    if (!data || data.dailyCost <= 0) return null;

    const { missedWealthFormatted, totalDelayCostFormatted, oneYearPenaltyFormatted, streak, topAction } = data;

    return (
        <div className="relative rounded-2xl overflow-hidden"
            style={{
                background: 'linear-gradient(135deg, rgba(245,158,11,0.06) 0%, rgba(239,68,68,0.04) 100%)',
                border: '1px solid rgba(245,158,11,0.15)',
            }}
        >
            {/* Subtle glow */}
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-3/4 h-px"
                style={{ background: 'linear-gradient(90deg, transparent, rgba(245,158,11,0.4), transparent)' }}
            />

            <div className="p-5">
                {/* Header */}
                <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-2">
                        <span className="text-lg">⏳</span>
                        <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
                            Financial Time Machine
                        </h3>
                    </div>
                    {streak > 1 && (
                        <div className="flex items-center gap-1 px-2 py-1 bg-orange-500/10 border border-orange-500/20 rounded-full">
                            <Flame className="w-3 h-3 text-orange-400" />
                            <span className="text-[10px] font-bold text-orange-400">{streak}-day streak</span>
                        </div>
                    )}
                </div>

                {/* Hero ₹/day */}
                <div className="text-center mb-5">
                    <div className="inline-flex flex-col items-center px-8 py-4 rounded-xl"
                        style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)' }}
                    >
                        <p className="text-4xl font-black tabular-nums tracking-tight"
                            style={{
                                background: 'linear-gradient(135deg, #f59e0b, #ef4444)',
                                WebkitBackgroundClip: 'text',
                                WebkitTextFillColor: 'transparent',
                            }}
                        >
                            ₹{tickerCost.toLocaleString('en-IN')}/day
                        </p>
                        <p className="text-xs text-slate-500 mt-1">
                            is slipping away while you wait
                        </p>
                    </div>
                </div>

                {/* 3 Mini Stats */}
                <div className="grid grid-cols-3 gap-2 mb-5">
                    <div className="rounded-xl p-3 text-center"
                        style={{ background: 'rgba(16,185,129,0.08)', border: '1px solid rgba(16,185,129,0.2)' }}
                    >
                        <p className="text-sm font-black text-emerald-600 tabular-nums">{missedWealthFormatted}</p>
                        <p className="text-[9px] text-slate-500 mt-0.5 leading-tight">missed by not starting 5 yrs ago</p>
                    </div>
                    <div className="rounded-xl p-3 text-center"
                        style={{ background: 'rgba(245,158,11,0.08)', border: '1px solid rgba(245,158,11,0.2)' }}
                    >
                        <p className="text-sm font-black text-amber-600 tabular-nums">{totalDelayCostFormatted}</p>
                        <p className="text-[9px] text-slate-500 mt-0.5 leading-tight">total delay cost so far</p>
                    </div>
                    <div className="rounded-xl p-3 text-center"
                        style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.2)' }}
                    >
                        <p className="text-sm font-black text-red-600 tabular-nums">{oneYearPenaltyFormatted}</p>
                        <p className="text-[9px] text-slate-500 mt-0.5 leading-tight">more if you wait another year</p>
                    </div>
                </div>

                {/* Top Action */}
                {topAction && (
                    <div className="mb-5">
                        <p className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold mb-2">
                            Your #1 action to stop the bleed
                        </p>
                        <div className="flex items-center gap-3 p-3 rounded-xl bg-white/[0.03] border border-white/5 hover:border-primary/20 transition-colors cursor-pointer">
                            <span className="text-lg">{topAction.icon}</span>
                            <div className="flex-1 min-w-0">
                                <p className="text-xs font-bold text-slate-800 truncate">{topAction.title}</p>
                            </div>
                            <ChevronRight className="w-4 h-4 text-slate-600 shrink-0" />
                        </div>
                    </div>
                )}

                {/* Divider */}
                <div className="h-px bg-white/5 my-5" />

                {/* 30-Year Projection Chart */}
                <ProjectionChart />
            </div>
        </div>
    );
};

export default FinancialTimeMachine;

