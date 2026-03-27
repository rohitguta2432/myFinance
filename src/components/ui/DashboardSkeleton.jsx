import React from 'react';

const Shimmer = ({ className = '' }) => (
    <div className={`animate-pulse rounded-lg bg-slate-500/15 ${className}`} />
);

/* ─── Summary Tab Skeleton ─── */
export const SummarySkeleton = () => (
    <div className="w-full max-w-6xl mx-auto px-6 lg:px-10 py-6 pb-24 space-y-6">
        <div>
            <Shimmer className="h-7 w-72 mb-2" />
            <Shimmer className="h-4 w-48" />
        </div>

        <div className="grid grid-cols-3 gap-3">
            {[1, 2, 3].map((i) => (
                <div key={i} className="bg-surface-dark rounded-xl border border-white/5 p-3 flex items-center gap-3">
                    <Shimmer className="w-9 h-9 rounded-lg shrink-0" />
                    <div className="flex-1 space-y-2">
                        <Shimmer className="h-3 w-20" />
                        <Shimmer className="h-5 w-24" />
                    </div>
                </div>
            ))}
        </div>

        <div className="bg-surface-dark rounded-3xl p-6 border border-white/5 shadow-xl">
            <div className="flex flex-col lg:flex-row gap-6">
                <div className="flex flex-col items-center lg:items-start gap-3 lg:w-[240px] shrink-0">
                    <Shimmer className="h-3 w-40 mb-2" />
                    <div className="relative flex items-center justify-center">
                        <svg width="200" height="200" viewBox="0 0 200 200" className="transform -rotate-90">
                            <circle cx="100" cy="100" r={80} fill="none" stroke="currentColor" className="text-slate-500/10" strokeWidth={10} />
                            <circle cx="100" cy="100" r={80} fill="none" stroke="currentColor" className="text-slate-500/15 animate-pulse" strokeWidth={10}
                                strokeDasharray={502} strokeDashoffset={326} strokeLinecap="round" className="animate-pulse" />
                        </svg>
                        <div className="absolute flex flex-col items-center gap-1">
                            <Shimmer className="h-10 w-16 rounded-lg" />
                            <Shimmer className="h-3 w-20" />
                        </div>
                    </div>
                </div>
                <div className="hidden lg:block w-px bg-white/5 self-stretch" />
                <div className="flex-1 min-w-0 space-y-3">
                    <div className="flex items-center justify-between mb-2">
                        <Shimmer className="h-3 w-28" />
                        <Shimmer className="h-3 w-32" />
                    </div>
                    {[1, 2, 3, 4, 5].map((i) => (
                        <div key={i} className="bg-slate-500/10 border border-white/5 rounded-xl p-3 flex items-center gap-3">
                            <Shimmer className="w-8 h-8 rounded-lg shrink-0" />
                            <div className="flex-1 space-y-2">
                                <div className="flex items-center gap-2">
                                    <Shimmer className="h-4 w-24" />
                                    <Shimmer className="h-4 w-16 rounded-full" />
                                </div>
                                <Shimmer className="h-1.5 w-full rounded-full" />
                            </div>
                            <Shimmer className="h-5 w-10" />
                        </div>
                    ))}
                </div>
            </div>
        </div>

        <Shimmer className="h-48 w-full rounded-3xl" />

        <div className="space-y-3">
            <div className="flex items-center justify-between">
                <Shimmer className="h-4 w-40" />
                <Shimmer className="h-3 w-24" />
            </div>
            {[1, 2, 3].map((i) => (
                <div key={i} className="bg-slate-500/8 border border-white/5 rounded-xl p-3 flex items-start gap-3">
                    <Shimmer className="w-5 h-5 rounded-full shrink-0 mt-0.5" />
                    <div className="flex-1 space-y-2">
                        <Shimmer className="h-4 w-56" />
                        <Shimmer className="h-3 w-40" />
                    </div>
                </div>
            ))}
        </div>
    </div>
);

/* ─── Action Plan Tab Skeleton ─── */
export const ActionPlanSkeleton = () => (
    <div className="w-full max-w-5xl mx-auto px-6 lg:px-10 py-6 pb-24 space-y-5">
        <div>
            <Shimmer className="h-7 w-56 mb-2" />
            <Shimmer className="h-4 w-72" />
        </div>
        <div className="flex gap-2 mb-2">
            {[1, 2, 3, 4].map((i) => <Shimmer key={i} className="h-8 w-24 rounded-full" />)}
        </div>
        {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-surface-dark rounded-2xl border border-white/5 p-5 space-y-3">
                <div className="flex items-center gap-3">
                    <Shimmer className="w-10 h-10 rounded-xl shrink-0" />
                    <div className="flex-1 space-y-2">
                        <Shimmer className="h-4 w-48" />
                        <Shimmer className="h-3 w-32" />
                    </div>
                    <Shimmer className="h-6 w-20 rounded-full" />
                </div>
                <Shimmer className="h-12 w-full rounded-lg" />
            </div>
        ))}
    </div>
);

/* ─── Insurance Tab Skeleton ─── */
export const InsuranceSkeleton = () => (
    <div className="w-full max-w-5xl mx-auto px-6 lg:px-10 py-6 pb-24 space-y-5">
        <div>
            <Shimmer className="h-7 w-52 mb-2" />
            <Shimmer className="h-4 w-64" />
        </div>
        {[1, 2, 3].map((i) => (
            <div key={i} className="bg-surface-dark rounded-3xl border border-white/5 overflow-hidden">
                <div className="p-6 border-b border-white/5 flex items-center gap-3">
                    <Shimmer className="w-10 h-10 rounded-xl" />
                    <div className="space-y-2">
                        <Shimmer className="h-5 w-40" />
                        <Shimmer className="h-3 w-28" />
                    </div>
                </div>
                <div className="p-6 space-y-3">
                    {[1, 2, 3].map((j) => (
                        <div key={j} className="flex justify-between">
                            <Shimmer className="h-3 w-32" />
                            <Shimmer className="h-3 w-24" />
                        </div>
                    ))}
                </div>
            </div>
        ))}
    </div>
);

/* ─── Tax Planning Tab Skeleton ─── */
export const TaxSkeleton = () => (
    <div className="w-full max-w-5xl mx-auto px-6 lg:px-10 py-6 pb-24 space-y-5">
        <div>
            <Shimmer className="h-7 w-44 mb-2" />
            <Shimmer className="h-4 w-60" />
        </div>
        <div className="grid grid-cols-2 gap-4">
            {[1, 2].map((i) => (
                <div key={i} className="bg-surface-dark rounded-3xl border border-white/5 p-6 space-y-4">
                    <div className="flex items-center gap-3">
                        <Shimmer className="w-10 h-10 rounded-xl" />
                        <Shimmer className="h-5 w-36" />
                    </div>
                    {[1, 2, 3, 4].map((j) => (
                        <div key={j} className="flex justify-between">
                            <Shimmer className="h-3 w-36" />
                            <Shimmer className="h-3 w-20" />
                        </div>
                    ))}
                    <Shimmer className="h-8 w-full rounded-xl mt-2" />
                </div>
            ))}
        </div>
        <div className="bg-surface-dark rounded-3xl border border-white/5 p-6 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="flex justify-between">
                    <Shimmer className="h-3 w-44" />
                    <Shimmer className="h-3 w-24" />
                </div>
            ))}
        </div>
    </div>
);
