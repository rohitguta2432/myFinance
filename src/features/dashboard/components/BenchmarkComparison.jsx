import React from 'react';
import { usePersonalisedBenchmarks } from '../../../hooks/usePersonalisedBenchmarks';

const TRAFFIC_COLORS = {
    green: { bar: '#0DF259', bg: 'rgba(13,242,89,0.06)', border: 'rgba(13,242,89,0.15)', text: 'text-primary', glow: '0 0 12px rgba(13,242,89,0.2)' },
    amber: { bar: '#f59e0b', bg: 'rgba(245,158,11,0.06)', border: 'rgba(245,158,11,0.15)', text: 'text-amber-400', glow: '0 0 12px rgba(245,158,11,0.15)' },
    red:   { bar: '#ef4444', bg: 'rgba(239,68,68,0.06)',  border: 'rgba(239,68,68,0.15)',  text: 'text-red-400', glow: '0 0 12px rgba(239,68,68,0.15)' },
};

const BenchmarkRow = ({ bm, index }) => {
    const tc = TRAFFIC_COLORS[bm.trafficLight] || TRAFFIC_COLORS.amber;

    return (
        <div
            className={`flex items-center gap-3 px-4 py-3.5 transition-colors hover:bg-white/[0.02] ${index > 0 ? 'border-t border-white/5' : ''}`}
        >
            {/* Icon + Label */}
            <div className="w-8 shrink-0 text-center">
                <span className="text-base">{bm.icon}</span>
            </div>
            <div className="w-28 shrink-0">
                <span className="text-xs font-semibold text-slate-300 leading-tight block">{bm.label}</span>
                {bm.note && (
                    <span className="text-[9px] text-slate-600 leading-none block mt-0.5 truncate" title={bm.note}>
                        {bm.note}
                    </span>
                )}
            </div>

            {/* Bar */}
            <div className="flex-1 relative min-w-0">
                <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden relative">
                    {/* Target marker */}
                    <div
                        className="absolute top-0 bottom-0 w-px bg-white/20 z-10"
                        style={{ left: `${Math.min(100, (100 / 120) * 100)}%` }}
                        title={`Target: ${bm.benchTarget}`}
                    />
                    {/* User bar */}
                    <div
                        className="h-full rounded-full transition-all duration-1000 ease-out"
                        style={{
                            width: `${Math.min(100, (bm.barPercent / 120) * 100)}%`,
                            background: `linear-gradient(90deg, ${tc.bar}aa, ${tc.bar})`,
                            boxShadow: tc.glow,
                        }}
                    />
                </div>
            </div>

            {/* User Value */}
            <div className="w-16 text-right shrink-0">
                <span className={`text-xs font-bold tabular-nums ${tc.text}`}>
                    {bm.userValue}
                </span>
            </div>

            {/* Benchmark Target */}
            <div className="w-16 text-right shrink-0">
                <span className="text-[10px] text-slate-500 tabular-nums">
                    {bm.benchTarget}
                </span>
            </div>

            {/* Traffic light dot */}
            <div className="w-3 shrink-0 flex justify-center">
                <div
                    className="w-2 h-2 rounded-full"
                    style={{
                        backgroundColor: tc.bar,
                        boxShadow: `0 0 6px ${tc.bar}60`,
                    }}
                />
            </div>
        </div>
    );
};

const BenchmarkComparison = () => {
    const { benchmarks, cityTier, householdType } = usePersonalisedBenchmarks();

    if (!benchmarks || benchmarks.length === 0) return null;

    return (
        <div>
            <div className="flex items-center justify-between mb-3">
                <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
                    Your Numbers vs. Benchmarks
                </h3>
                <span className="text-[10px] text-slate-600">
                    Personalised for your profile
                </span>
            </div>
            <div className="bg-surface-dark rounded-2xl border border-white/5 overflow-hidden">
                {/* Column headers */}
                <div className="flex items-center gap-3 px-4 py-2 border-b border-white/5 bg-white/[0.02]">
                    <div className="w-8 shrink-0" />
                    <div className="w-28 shrink-0">
                        <span className="text-[9px] text-slate-600 uppercase tracking-wider font-bold">Metric</span>
                    </div>
                    <div className="flex-1">
                        <span className="text-[9px] text-slate-600 uppercase tracking-wider font-bold">Progress</span>
                    </div>
                    <div className="w-16 text-right shrink-0">
                        <span className="text-[9px] text-slate-600 uppercase tracking-wider font-bold">Yours</span>
                    </div>
                    <div className="w-16 text-right shrink-0">
                        <span className="text-[9px] text-slate-600 uppercase tracking-wider font-bold">Target</span>
                    </div>
                    <div className="w-3 shrink-0" />
                </div>

                {/* Rows */}
                {benchmarks.map((bm, i) => (
                    <BenchmarkRow key={bm.id} bm={bm} index={i} />
                ))}
            </div>

            {/* Legend */}
            <div className="flex items-center gap-4 mt-2 px-1">
                {[
                    { color: '#0DF259', label: '≥ Target' },
                    { color: '#f59e0b', label: 'Needs attention' },
                    { color: '#ef4444', label: 'Below minimum' },
                ].map((l) => (
                    <div key={l.label} className="flex items-center gap-1.5">
                        <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: l.color }} />
                        <span className="text-[9px] text-slate-600">{l.label}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default BenchmarkComparison;
