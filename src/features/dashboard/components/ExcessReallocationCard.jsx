import { ArrowRight, TrendingUp, Shield, Clock, PiggyBank } from 'lucide-react';
import { useDashboardSummary } from '../../../hooks/useDashboardSummary';

const ExcessReallocationCard = () => {
    const { data } = useDashboardSummary();
    const realloc = data?.excessReallocation;

    if (!realloc?.hasExcess) return null;

    const {
        deployableSurplusFormatted,
        equityTransferFormatted,
        debtTransferFormatted,
        equityPct,
        debtPct,
        equityTransfer,
        debtTransfer,
        useStp,
        stpMonths,
        riskProfile,
        emergencyTargetMonths,
        bufferMonths,
        yearsToRetirement,
        reason,
    } = realloc;

    const riskLabel = riskProfile === 'conservative' ? 'Conservative' : riskProfile === 'aggressive' ? 'Aggressive' : 'Moderate';

    return (
        <div className="bg-surface-dark rounded-2xl border border-white/5 shadow-lg overflow-hidden">
            {/* Header */}
            <div className="bg-gradient-to-r from-primary/10 to-emerald-500/10 px-5 py-4">
                <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-lg bg-primary/15 flex items-center justify-center">
                        <TrendingUp className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                        <h3 className="font-bold text-white text-sm tracking-wide">EXCESS REALLOCATION</h3>
                        <p className="text-xs text-slate-400 mt-0.5">
                            Deploy idle liquid assets into retirement corpus
                        </p>
                    </div>
                </div>
            </div>

            {/* Protected vs Deployable */}
            <div className="px-5 py-4 border-b border-white/5">
                <div className="flex items-center gap-2 mb-3">
                    <Shield className="w-4 h-4 text-emerald-400" />
                    <p className="text-sm text-slate-400">
                        Keeping <span className="text-white font-semibold">{emergencyTargetMonths + bufferMonths} months</span> protected
                        <span className="text-slate-500 ml-1">({emergencyTargetMonths} emergency + {bufferMonths} buffer)</span>
                    </p>
                </div>
                <div className="bg-primary/5 border border-primary/20 rounded-xl p-4 text-center">
                    <p className="text-xs text-slate-400 uppercase tracking-wider font-medium mb-1">Deployable Surplus</p>
                    <p className="text-2xl font-bold text-primary">{deployableSurplusFormatted}</p>
                    <p className="text-xs text-slate-500 mt-1">100% → Retirement Corpus</p>
                </div>
            </div>

            {/* Allocation Split */}
            <div className="px-5 py-4 border-b border-white/5">
                <div className="flex items-center justify-between mb-3">
                    <p className="text-xs uppercase tracking-wider font-bold text-slate-500">Recommended Split</p>
                    <span className="text-xs text-slate-500">
                        {riskLabel} · {yearsToRetirement}y to retirement
                    </span>
                </div>

                <div className="grid grid-cols-2 gap-3">
                    {equityTransfer > 0 && (
                        <div className="bg-blue-500/5 border border-blue-500/15 rounded-xl p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <div className="w-2 h-2 rounded-full bg-blue-400" />
                                <p className="text-xs font-bold text-blue-400 uppercase tracking-wider">Equity</p>
                                <span className="text-xs text-slate-500 ml-auto">{equityPct}%</span>
                            </div>
                            <p className="text-lg font-bold text-white">{equityTransferFormatted}</p>
                            {useStp && (
                                <div className="flex items-center gap-1 mt-2">
                                    <Clock className="w-3 h-3 text-amber-400" />
                                    <p className="text-xs text-amber-400 font-medium">
                                        Via STP over {stpMonths} months
                                    </p>
                                </div>
                            )}
                        </div>
                    )}
                    {debtTransfer > 0 && (
                        <div className="bg-emerald-500/5 border border-emerald-500/15 rounded-xl p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <div className="w-2 h-2 rounded-full bg-emerald-400" />
                                <p className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Debt</p>
                                <span className="text-xs text-slate-500 ml-auto">{debtPct}%</span>
                            </div>
                            <p className="text-lg font-bold text-white">{debtTransferFormatted}</p>
                            <p className="text-xs text-slate-500 mt-2">Direct investment</p>
                        </div>
                    )}
                </div>

                {/* Equity-only case (< 10 years) */}
                {equityTransfer === 0 && debtTransfer > 0 && (
                    <p className="text-xs text-slate-500 mt-2 italic">
                        With less than 10 years to retirement, 100% is allocated to debt instruments for capital safety.
                    </p>
                )}
            </div>

            {/* Reason */}
            <div className="px-5 py-4">
                <div className="flex items-start gap-3 bg-white/[0.03] rounded-xl px-4 py-3">
                    <PiggyBank className="w-5 h-5 text-primary shrink-0 mt-0.5" />
                    <p className="text-sm text-slate-300 leading-relaxed">{reason}</p>
                </div>
            </div>
        </div>
    );
};

export default ExcessReallocationCard;
