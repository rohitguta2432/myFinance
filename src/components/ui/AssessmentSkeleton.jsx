import React from 'react';

const Shimmer = ({ className = '' }) => (
    <div className={`animate-pulse rounded-lg bg-slate-500/15 ${className}`} />
);

/* ─── Step 1: Profile Skeleton ─── */
export const ProfileSkeleton = () => (
    <div className="w-full max-w-4xl mx-auto px-6 py-8 space-y-6">
        <div>
            <Shimmer className="h-8 w-64 mb-2" />
            <Shimmer className="h-4 w-96" />
        </div>
        {/* Form fields */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 space-y-5">
            {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="space-y-2">
                    <Shimmer className="h-3 w-28" />
                    <Shimmer className="h-11 w-full rounded-xl" />
                </div>
            ))}
        </div>
        {/* Risk questions */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 space-y-4">
            <Shimmer className="h-5 w-40 mb-3" />
            {[1, 2, 3].map((i) => (
                <div key={i} className="space-y-2">
                    <Shimmer className="h-4 w-72" />
                    <div className="flex gap-2">
                        {[1, 2, 3].map((j) => <Shimmer key={j} className="h-10 w-28 rounded-xl" />)}
                    </div>
                </div>
            ))}
        </div>
    </div>
);

/* ─── Step 2: Cash Flow Skeleton ─── */
export const CashFlowSkeleton = () => (
    <div className="w-full max-w-4xl mx-auto px-6 py-8 space-y-6">
        <div>
            <Shimmer className="h-8 w-72 mb-2" />
            <Shimmer className="h-4 w-80" />
        </div>
        {/* Income items */}
        <div className="space-y-3">
            {[1, 2].map((i) => (
                <div key={i} className="bg-surface-dark rounded-xl border border-white/5 p-4 flex items-center gap-3">
                    <Shimmer className="w-10 h-10 rounded-xl shrink-0" />
                    <div className="flex-1 space-y-1.5">
                        <Shimmer className="h-4 w-24" />
                        <Shimmer className="h-3 w-36" />
                    </div>
                    <Shimmer className="w-8 h-8 rounded-lg" />
                    <Shimmer className="w-8 h-8 rounded-lg" />
                </div>
            ))}
            <Shimmer className="h-12 w-full rounded-xl" />
        </div>
        {/* Expense items */}
        <div className="space-y-3">
            {[1, 2, 3].map((i) => (
                <div key={i} className="bg-surface-dark rounded-xl border border-white/5 p-4 flex items-center gap-3">
                    <Shimmer className="w-10 h-10 rounded-xl shrink-0" />
                    <div className="flex-1 space-y-1.5">
                        <Shimmer className="h-4 w-32" />
                        <Shimmer className="h-3 w-28" />
                    </div>
                    <Shimmer className="w-8 h-8 rounded-lg" />
                    <Shimmer className="w-8 h-8 rounded-lg" />
                </div>
            ))}
            <Shimmer className="h-12 w-full rounded-xl" />
        </div>
        {/* Summary */}
        <Shimmer className="h-32 w-full rounded-2xl" />
    </div>
);

/* ─── Step 3: Assets & Liabilities Skeleton ─── */
export const AssetsLiabilitiesSkeleton = () => (
    <div className="w-full max-w-4xl mx-auto px-6 py-8 space-y-6">
        <div>
            <Shimmer className="h-8 w-72 mb-2" />
            <Shimmer className="h-4 w-64" />
        </div>
        {/* Net Worth Card */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6">
            <div className="flex items-center justify-between">
                <div className="space-y-2">
                    <Shimmer className="h-3 w-28" />
                    <Shimmer className="h-10 w-40" />
                </div>
                <div className="flex gap-4">
                    <Shimmer className="h-16 w-28 rounded-xl" />
                    <Shimmer className="h-16 w-28 rounded-xl" />
                </div>
            </div>
        </div>
        {/* Portfolio Mix */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 space-y-4">
            <Shimmer className="h-5 w-36" />
            <div className="flex items-center gap-8">
                <Shimmer className="w-36 h-36 rounded-full shrink-0" />
                <div className="flex-1 space-y-3">
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} className="flex justify-between">
                            <Shimmer className="h-3 w-20" />
                            <Shimmer className="h-3 w-24" />
                        </div>
                    ))}
                </div>
            </div>
        </div>
        {/* Target table */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 space-y-3">
            <Shimmer className="h-5 w-56" />
            {[1, 2, 3, 4].map((i) => (
                <div key={i} className="flex justify-between py-2">
                    <Shimmer className="h-3 w-20" />
                    <Shimmer className="h-3 w-12" />
                    <Shimmer className="h-3 w-12" />
                    <Shimmer className="h-3 w-20" />
                </div>
            ))}
        </div>
    </div>
);

/* ─── Step 4: Goals Skeleton ─── */
export const GoalsSkeleton = () => (
    <div className="w-full max-w-4xl mx-auto px-6 py-8 space-y-6">
        <div>
            <Shimmer className="h-8 w-56 mb-2" />
            <Shimmer className="h-4 w-72" />
        </div>
        {/* Goal cards */}
        {[1, 2, 3].map((i) => (
            <div key={i} className="bg-surface-dark rounded-2xl border border-white/5 p-5 space-y-3">
                <div className="flex items-center gap-3">
                    <Shimmer className="w-10 h-10 rounded-xl shrink-0" />
                    <div className="flex-1 space-y-1.5">
                        <Shimmer className="h-5 w-36" />
                        <Shimmer className="h-3 w-48" />
                    </div>
                    <Shimmer className="w-8 h-8 rounded-lg" />
                </div>
                <div className="grid grid-cols-3 gap-3">
                    {[1, 2, 3].map((j) => (
                        <div key={j} className="space-y-1">
                            <Shimmer className="h-3 w-16" />
                            <Shimmer className="h-5 w-24" />
                        </div>
                    ))}
                </div>
            </div>
        ))}
        <Shimmer className="h-12 w-full rounded-xl" />
    </div>
);

/* ─── Step 5: Insurance Skeleton ─── */
export const InsuranceGapSkeleton = () => (
    <div className="w-full max-w-4xl mx-auto px-6 py-8 space-y-6">
        <div>
            <Shimmer className="h-8 w-56 mb-2" />
            <Shimmer className="h-4 w-80" />
        </div>
        {/* Term Life card */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 space-y-4">
            <div className="flex items-center gap-3">
                <Shimmer className="w-10 h-10 rounded-xl" />
                <Shimmer className="h-5 w-32" />
            </div>
            <Shimmer className="h-20 w-full rounded-xl" />
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                    <Shimmer className="h-3 w-28" />
                    <Shimmer className="h-11 w-full rounded-xl" />
                </div>
                <div className="space-y-2">
                    <Shimmer className="h-3 w-28" />
                    <Shimmer className="h-11 w-full rounded-xl" />
                </div>
            </div>
        </div>
        {/* Health card */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 space-y-4">
            <div className="flex items-center gap-3">
                <Shimmer className="w-10 h-10 rounded-xl" />
                <Shimmer className="h-5 w-36" />
            </div>
            <Shimmer className="h-20 w-full rounded-xl" />
            <div className="space-y-2">
                <Shimmer className="h-3 w-28" />
                <Shimmer className="h-11 w-full rounded-xl" />
            </div>
        </div>
    </div>
);

/* ─── Step 6: Tax Skeleton ─── */
export const TaxOptimizationSkeleton = () => (
    <div className="w-full max-w-4xl mx-auto px-6 py-8 space-y-6">
        <div>
            <Shimmer className="h-8 w-48 mb-2" />
            <Shimmer className="h-4 w-72" />
        </div>
        {/* Regime comparison */}
        <div className="grid grid-cols-2 gap-4">
            {[1, 2].map((i) => (
                <div key={i} className="bg-surface-dark rounded-2xl border border-white/5 p-5 space-y-3">
                    <Shimmer className="h-5 w-28" />
                    <Shimmer className="h-8 w-32" />
                    {[1, 2, 3].map((j) => (
                        <div key={j} className="flex justify-between">
                            <Shimmer className="h-3 w-28" />
                            <Shimmer className="h-3 w-20" />
                        </div>
                    ))}
                </div>
            ))}
        </div>
        {/* Deductions */}
        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 space-y-4">
            <Shimmer className="h-5 w-32" />
            {[1, 2, 3, 4].map((i) => (
                <div key={i} className="space-y-2">
                    <Shimmer className="h-3 w-36" />
                    <Shimmer className="h-11 w-full rounded-xl" />
                </div>
            ))}
        </div>
    </div>
);
