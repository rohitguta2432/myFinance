import React, { useState } from 'react';
import {
    AreaChart, Area, XAxis, YAxis, CartesianGrid,
    Tooltip, ResponsiveContainer, ReferenceLine,
} from 'recharts';
import { TrendingUp, Zap, Clock, Info } from 'lucide-react';
import { useProjection } from '../../../hooks/useProjection';

/* ── Format Y-axis ticks ── */
const formatYAxis = (v) => {
    if (v >= 10000000) return `${(v / 10000000).toFixed(1)}Cr`;
    if (v >= 100000) return `${(v / 100000).toFixed(0)}L`;
    if (v >= 1000) return `${Math.round(v / 1000)}K`;
    return String(v);
};

/* ── Custom Tooltip ── */
const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    const d = payload[0]?.payload;
    if (!d) return null;

    const fmt = (v) => {
        if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
        if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
        return `₹${Math.round(v).toLocaleString('en-IN')}`;
    };

    return (
        <div className="bg-white border border-slate-200 rounded-xl p-3 shadow-xl text-sm min-w-[180px]">
            <p className="text-slate-500 font-semibold mb-2">{d.year} · Age {d.age}</p>
            <div className="space-y-1.5">
                <div className="flex justify-between items-center">
                    <span className="flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-500" />
                        Optimized
                    </span>
                    <span className="font-bold text-emerald-600">{fmt(d.optimized)}</span>
                </div>
                <div className="flex justify-between items-center">
                    <span className="flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-blue-500" />
                        Current
                    </span>
                    <span className="font-bold text-blue-600">{fmt(d.currentPath)}</span>
                </div>
                <div className="flex justify-between items-center">
                    <span className="flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-amber-500" />
                        If started 5yr ago
                    </span>
                    <span className="font-bold text-amber-600">{fmt(d.earlyStart)}</span>
                </div>
            </div>
        </div>
    );
};

/* ── Legend Item ── */
const LegendItem = ({ color, label, value, icon: Icon }) => (
    <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded-sm shrink-0" style={{ backgroundColor: color }} />
        <div>
            <p className="text-xs text-slate-500 flex items-center gap-1">
                {Icon && <Icon className="w-4 h-4" />} {label}
            </p>
            <p className="text-base font-bold text-white">{value}</p>
        </div>
    </div>
);

/* ── MAIN CHART COMPONENT ── */
const ProjectionChart = () => {
    const projection = useProjection();

    if (!projection || !projection.data?.length) return null;

    const {
        data, finalCurrentFormatted, finalOptimizedFormatted, finalEarlyFormatted,
        extraByOptimizingFormatted, projectionYears, retirementAge, milestones, fmt,
    } = projection;

    // Filter X-axis ticks to show every 5 years
    const tickYears = data
        .filter((d, i) => i === 0 || i === data.length - 1 || (data[0].year + i) % 5 === 0)
        .map(d => d.year);

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold uppercase tracking-[0.2em] text-slate-500">
                    {projectionYears}-Year Wealth Projection
                </h4>
                <span className="text-xs text-slate-600">
                    Retiring at {retirementAge}
                </span>
            </div>

            {/* Legend */}
            <div className="grid grid-cols-3 gap-3">
                <LegendItem color="#34d399" label={`Optimized (+${projection.optimizationPct}% savings)`} value={finalOptimizedFormatted} icon={Zap} />
                <LegendItem color="#60a5fa" label="Current Path" value={finalCurrentFormatted} icon={TrendingUp} />
                <LegendItem color="#fbbf24" label="If started 5yr ago" value={finalEarlyFormatted} icon={Clock} />
            </div>

            {/* Chart */}
            <div className="w-full h-[280px] -ml-2">
                <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                        <defs>
                            <linearGradient id="gradOptimized" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stopColor="#34d399" stopOpacity={0.25} />
                                <stop offset="100%" stopColor="#34d399" stopOpacity={0.02} />
                            </linearGradient>
                            <linearGradient id="gradCurrent" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stopColor="#60a5fa" stopOpacity={0.2} />
                                <stop offset="100%" stopColor="#60a5fa" stopOpacity={0.02} />
                            </linearGradient>
                            <linearGradient id="gradEarly" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stopColor="#fbbf24" stopOpacity={0.15} />
                                <stop offset="100%" stopColor="#fbbf24" stopOpacity={0.02} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid
                            strokeDasharray="3 3"
                            stroke="rgba(255,255,255,0.04)"
                            vertical={false}
                        />
                        <XAxis
                            dataKey="year"
                            tick={{ fontSize: 12, fill: '#64748b' }}
                            tickLine={false}
                            axisLine={{ stroke: 'rgba(255,255,255,0.06)' }}
                            ticks={tickYears}
                        />
                        <YAxis
                            tickFormatter={formatYAxis}
                            tick={{ fontSize: 12, fill: '#64748b' }}
                            tickLine={false}
                            axisLine={false}
                            width={50}
                        />
                        <Tooltip content={<CustomTooltip />} />

                        {/* Milestone lines */}
                        {milestones.map((m) => (
                            <ReferenceLine
                                key={m.amount}
                                y={parseInt(m.amount.replace(/[₹,CLr ]/g, '')) * (m.amount.includes('Cr') ? 10000000 : 100000)}
                                stroke="rgba(255,255,255,0.08)"
                                strokeDasharray="8 4"
                                label={{
                                    value: m.amount,
                                    position: 'right',
                                    style: { fontSize: 11, fill: '#475569' },
                                }}
                            />
                        ))}

                        {/* Areas — render order: back to front */}
                        <Area
                            type="monotone"
                            dataKey="earlyStart"
                            stroke="#fbbf24"
                            strokeWidth={1.5}
                            strokeDasharray="6 3"
                            fill="url(#gradEarly)"
                            dot={false}
                            activeDot={{ r: 4, stroke: '#fbbf24', strokeWidth: 2, fill: '#1a1a2e' }}
                        />
                        <Area
                            type="monotone"
                            dataKey="optimized"
                            stroke="#34d399"
                            strokeWidth={2}
                            fill="url(#gradOptimized)"
                            dot={false}
                            activeDot={{ r: 4, stroke: '#34d399', strokeWidth: 2, fill: '#1a1a2e' }}
                        />
                        <Area
                            type="monotone"
                            dataKey="currentPath"
                            stroke="#60a5fa"
                            strokeWidth={2}
                            fill="url(#gradCurrent)"
                            dot={false}
                            activeDot={{ r: 4, stroke: '#60a5fa', strokeWidth: 2, fill: '#1a1a2e' }}
                        />
                    </AreaChart>
                </ResponsiveContainer>
            </div>

            {/* Extra by optimizing callout */}
            <div className="flex items-center gap-3 p-3 rounded-xl bg-emerald-500/5 border border-emerald-500/15">
                <Zap className="w-5 h-5 text-emerald-400 shrink-0" />
                <p className="text-sm text-slate-300">
                    Optimizing your savings by just {projection.optimizationPct}% could earn you an extra{' '}
                    <span className="font-bold text-emerald-400">{extraByOptimizingFormatted}</span> by retirement.
                </p>
            </div>

            {/* Assumptions */}
            <div className="rounded-xl p-3 bg-white/[0.02] border border-white/5">
                <p className="text-[10px] font-semibold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <Info className="w-3 h-3" />
                    Assumptions
                </p>
                <ul className="space-y-1">
                    {[
                        '12% CAGR — based on Indian equity market long-term average (BSE Sensex)',
                        `Monthly SIP of ₹${projection.monthlySavings?.toLocaleString('en-IN')} — derived from your income minus expenses & EMIs`,
                        `Optimized scenario assumes ${projection.optimizationPct}% higher monthly savings (₹${projection.optimizedSavings?.toLocaleString('en-IN')}/mo)`,
                        'Returns compounded monthly using SIP future value formula',
                        'Does not account for inflation, taxation, or market volatility',
                        'Past performance does not guarantee future results',
                    ].map((text, i) => (
                        <li key={i} className="text-[10px] text-slate-500 leading-relaxed flex items-start gap-1.5">
                            <span className="text-slate-600 mt-0.5">•</span>
                            <span>{text}</span>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
};

export default ProjectionChart;
