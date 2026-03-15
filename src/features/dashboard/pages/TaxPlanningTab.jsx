import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
    Receipt, Scale, Home, FileText, Building2,
    CheckCircle2, AlertTriangle, Info, ArrowDownRight,
    ArrowUpRight, Sparkles, TrendingDown, Zap,
} from 'lucide-react';
import { useTaxAnalysis } from '../../../hooks/useTaxAnalysis';

/* ── Tab Navigation (shared across dashboard tabs) ── */
export const DashboardTabs = () => {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const tabs = [
        { label: 'Summary', path: '/dashboard' },
        { label: 'Insurance', path: '/dashboard/insurance' },
        { label: 'Tax', path: '/dashboard/tax' },
    ];
    const active = tabs.find(t =>
        t.path === '/dashboard'
            ? pathname === '/dashboard'
            : pathname.startsWith(t.path)
    )?.path || '/dashboard';

    return (
        <div className="flex gap-1 bg-surface-dark rounded-xl p-1 border border-white/5">
            {tabs.map(t => (
                <button
                    key={t.path}
                    onClick={() => navigate(t.path)}
                    className={`px-4 py-2 rounded-lg text-xs font-bold tracking-wide transition-all ${active === t.path
                        ? 'bg-primary/15 text-primary shadow-sm'
                        : 'text-slate-500 hover:text-slate-300'
                        }`}
                >
                    {t.label}
                </button>
            ))}
        </div>
    );
};

/* ═══════════════════════════════════════════════════════════════
   REUSABLE SUB-COMPONENTS
   ═══════════════════════════════════════════════════════════════ */

const SectionCard = ({ children, className = '' }) => (
    <section className={`bg-surface-dark rounded-3xl border border-white/5 shadow-xl overflow-hidden ${className}`}>
        {children}
    </section>
);

const SectionHeader = ({ icon: Icon, iconColor, bgGradient, title, subtitle }) => (
    <div className={`bg-gradient-to-r ${bgGradient} via-transparent to-transparent p-6 border-b border-white/5`}>
        <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-xl ${iconColor} flex items-center justify-center`}>
                <Icon className="w-5 h-5" />
            </div>
            <div>
                <h3 className="text-lg font-black text-white">{title}</h3>
                <span className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">{subtitle}</span>
            </div>
        </div>
    </div>
);

const RowItem = ({ label, value, muted, highlight, strikethrough }) => (
    <div className={`flex justify-between items-center gap-2 text-xs ${strikethrough ? 'opacity-40 line-through' : ''}`}>
        <span className={muted ? 'text-slate-500' : 'text-slate-300'}>{label}</span>
        <span className={`font-mono text-right ${highlight ? 'font-bold text-primary' : muted ? 'text-slate-500' : 'text-slate-300'}`}>
            {value}
        </span>
    </div>
);

/* ═══════════════════════════════════════════════════════════════
   MAIN TAX PLANNING TAB
   ═══════════════════════════════════════════════════════════════ */
const TaxPlanningTab = () => {
    const data = useTaxAnalysis();
    const { regimeComparison, tds, rental, deductions, employerNps, fmt } = data;
    const { old: oldR, new: newR, recommended, savings, savingsFormatted } = regimeComparison;

    return (
        <div className="min-h-screen bg-background-dark text-white">
            {/* ── Header ── */}
            <div className="sticky top-0 z-50 bg-background-dark/80 backdrop-blur-xl border-b border-white/5">
                <div className="max-w-[1200px] mx-auto px-4 py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 bg-primary/20 rounded-lg flex items-center justify-center">
                            <Zap className="w-4 h-4 text-primary" />
                        </div>
                        <h1 className="font-bold text-lg tracking-wide">Financial Health</h1>
                    </div>
                    <DashboardTabs />
                </div>
            </div>

            <div className="max-w-[1200px] mx-auto px-4 py-6 pb-24 space-y-8">

                {/* Page Title */}
                <div>
                    <h2 className="text-xl font-black tracking-tight">Tax Planning</h2>
                    <p className="text-xs text-slate-500 mt-0.5">FY 2026-27 · Personalised tax analysis</p>
                </div>

                {/* ════════════════════════════════════════════════════
                   §5.1 TAX REGIME COMPARISON (Always)
                   ════════════════════════════════════════════════════ */}
                <SectionCard>
                    <SectionHeader
                        icon={Scale}
                        iconColor="bg-violet-500/15 text-violet-400"
                        bgGradient="from-violet-500/10"
                        title="Tax Regime Comparison"
                        subtitle="Old vs New — FY 2026-27"
                    />
                    <div className="p-6 space-y-5">

                        {/* Side-by-side cards */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {/* OLD REGIME */}
                            <div className={`rounded-2xl p-5 border-2 relative transition-all ${recommended === 'old'
                                ? 'border-emerald-500 bg-emerald-500/5 shadow-[0_0_20px_rgba(16,185,129,0.1)]'
                                : 'border-white/5 bg-white/[0.02]'
                                }`}>
                                {recommended === 'old' && (
                                    <div className="absolute -top-[2px] -right-[2px] bg-emerald-500 text-black font-black text-[9px] px-3 py-1.5 rounded-bl-xl rounded-tr-2xl tracking-widest">
                                        RECOMMENDED
                                    </div>
                                )}
                                <h4 className={`font-bold text-sm uppercase tracking-widest mb-4 ${recommended === 'old' ? 'text-emerald-400' : 'text-slate-400'}`}>
                                    Old Regime
                                </h4>
                                <div className="space-y-2 font-mono text-xs">
                                    <RowItem label="Gross Taxable" value={fmt(oldR.grossIncome)} />
                                    <RowItem label="(-) Std Deduction" value={`-${fmt(oldR.stdDeduction)}`} muted />
                                    <RowItem label="(-) 80C" value={`-${fmt(oldR.deductions80C)}`} muted />
                                    <RowItem label="(-) NPS 80CCD" value={`-${fmt(oldR.deductionsNps)}`} muted />
                                    <div className="border-t border-dashed border-white/10 pt-2 mt-2" />
                                    <RowItem label="Taxable Income" value={fmt(oldR.taxableIncome)} highlight />
                                    <RowItem label="Tax" value={fmt(oldR.baseTax)} />
                                    <RowItem label="(+) Cess 4%" value={fmt(oldR.cess)} muted />
                                </div>
                                <div className="border-t border-white/10 mt-4 pt-4">
                                    <div className="flex justify-between items-baseline">
                                        <span className="text-[10px] text-slate-500 font-bold">TOTAL TAX</span>
                                        <span className={`text-xl font-black ${recommended === 'old' ? 'text-white' : 'text-slate-400'}`}>
                                            {fmt(oldR.totalTax)}
                                        </span>
                                    </div>
                                    <p className="text-[10px] text-slate-600 text-right mt-0.5">
                                        Effective Rate: {oldR.effectiveRate.toFixed(1)}%
                                    </p>
                                </div>
                            </div>

                            {/* NEW REGIME */}
                            <div className={`rounded-2xl p-5 border-2 relative transition-all ${recommended === 'new'
                                ? 'border-emerald-500 bg-emerald-500/5 shadow-[0_0_20px_rgba(16,185,129,0.1)]'
                                : 'border-white/5 bg-white/[0.02]'
                                }`}>
                                {recommended === 'new' && (
                                    <div className="absolute -top-[2px] -right-[2px] bg-emerald-500 text-black font-black text-[9px] px-3 py-1.5 rounded-bl-xl rounded-tr-2xl tracking-widest">
                                        RECOMMENDED
                                    </div>
                                )}
                                <h4 className={`font-bold text-sm uppercase tracking-widest mb-4 ${recommended === 'new' ? 'text-emerald-400' : 'text-slate-400'}`}>
                                    New Regime
                                </h4>
                                <div className="space-y-2 font-mono text-xs">
                                    <RowItem label="Gross Taxable" value={fmt(newR.grossIncome)} />
                                    <RowItem label="(-) Std Deduction" value={`-${fmt(newR.stdDeduction)}`} muted />
                                    <RowItem label="(-) 80C / NPS / HRA" value="Not Allowed" strikethrough />
                                    <div className="border-t border-dashed border-white/10 pt-2 mt-2" />
                                    <RowItem label="Taxable Income" value={fmt(newR.taxableIncome)} highlight />
                                    {newR.rebateApplied && (
                                        <div className="flex items-center gap-1.5 bg-emerald-500/10 rounded-lg px-2 py-1.5 border border-emerald-500/15">
                                            <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                                            <span className="text-[10px] text-emerald-400 font-bold">Section 87A Rebate — Tax = ₹0</span>
                                        </div>
                                    )}
                                    <RowItem label="Tax" value={fmt(newR.baseTax)} />
                                    <RowItem label="(+) Cess 4%" value={fmt(newR.cess)} muted />
                                </div>
                                <div className="border-t border-white/10 mt-4 pt-4">
                                    <div className="flex justify-between items-baseline">
                                        <span className="text-[10px] text-slate-500 font-bold">TOTAL TAX</span>
                                        <span className={`text-xl font-black ${recommended === 'new' ? 'text-white' : 'text-slate-400'}`}>
                                            {fmt(newR.totalTax)}
                                        </span>
                                    </div>
                                    <p className="text-[10px] text-slate-600 text-right mt-0.5">
                                        Effective Rate: {newR.effectiveRate.toFixed(1)}%
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Savings Banner */}
                        <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-5">
                            <div className="flex items-center gap-2 mb-2">
                                <Sparkles className="w-5 h-5 text-emerald-400" />
                                <h4 className="font-black text-emerald-400">
                                    Choose {recommended === 'old' ? 'OLD' : 'NEW'} REGIME
                                </h4>
                            </div>
                            <p className="text-sm text-white">
                                You save <span className="font-black">{savingsFormatted}</span> by choosing {recommended === 'old' ? 'Old Regime' : 'New Regime'}.
                            </p>
                            <p className="text-xs text-slate-400 mt-1.5">
                                💡 {recommended === 'old'
                                    ? 'Your deductions make Old Regime more beneficial. Review this each year if your investments change.'
                                    : 'With lower deductions, New Regime\'s wider slabs give you better savings.'}
                            </p>
                        </div>
                    </div>
                </SectionCard>

                {/* ════════════════════════════════════════════════════
                   §5.2  TDS RECONCILIATION (Always)
                   ════════════════════════════════════════════════════ */}
                <SectionCard>
                    <SectionHeader
                        icon={Receipt}
                        iconColor="bg-sky-500/15 text-sky-400"
                        bgGradient="from-sky-500/10"
                        title="TDS Reconciliation"
                        subtitle="Tax deducted at source vs liability"
                    />
                    <div className="p-6 space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div className="bg-background-dark rounded-xl border border-white/5 p-4 text-center">
                                <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-1">Estimated Tax Liability</p>
                                <p className="text-lg font-black text-white">{tds.recommendedTaxFormatted}</p>
                                <p className="text-[10px] text-slate-600 mt-0.5">{recommended === 'old' ? 'Old Regime' : 'New Regime'}</p>
                            </div>
                            <div className="bg-background-dark rounded-xl border border-white/5 p-4 text-center">
                                <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-1">TDS Deducted (Est.)</p>
                                <p className="text-lg font-black text-white">{tds.totalTDSFormatted}</p>
                                <p className="text-[10px] text-slate-600 mt-0.5">@10% of salary</p>
                            </div>
                        </div>

                        {/* Status message */}
                        {tds.status === 'refund' && (
                            <div className="flex items-start gap-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4">
                                <ArrowDownRight className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-xs font-bold text-emerald-400 mb-1">Refund Due: {tds.diffFormatted}</p>
                                    <p className="text-xs text-slate-300 leading-relaxed">
                                        You are entitled to a refund of {tds.diffFormatted}. File your ITR before 31 July to claim it.
                                    </p>
                                </div>
                            </div>
                        )}
                        {tds.status === 'due' && (
                            <div className="flex items-start gap-3 bg-red-500/10 border border-red-500/20 rounded-xl p-4">
                                <ArrowUpRight className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-xs font-bold text-red-400 mb-1">Additional Tax Due: {tds.diffFormatted}</p>
                                    <p className="text-xs text-slate-300 leading-relaxed">
                                        You have an additional tax demand of {tds.diffFormatted}. Pay this as Advance Tax before 15 March to avoid interest.
                                    </p>
                                </div>
                            </div>
                        )}
                        {tds.status === 'matched' && (
                            <div className="flex items-start gap-3 bg-sky-500/10 border border-sky-500/20 rounded-xl p-4">
                                <CheckCircle2 className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-xs font-bold text-sky-400 mb-1">TDS Perfectly Matched</p>
                                    <p className="text-xs text-slate-300 leading-relaxed">
                                        Your TDS is perfectly matched with your tax liability. No additional payment required.
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>
                </SectionCard>

                {/* ════════════════════════════════════════════════════
                   §5.3  RENTAL INCOME (Only if > 0)
                   ════════════════════════════════════════════════════ */}
                {rental.hasRentalIncome && (
                    <SectionCard>
                        <SectionHeader
                            icon={Home}
                            iconColor="bg-amber-500/15 text-amber-400"
                            bgGradient="from-amber-500/10"
                            title="Rental Income Treatment"
                            subtitle="Section 24(a) — Standard deduction"
                        />
                        <div className="p-6 space-y-4">
                            <div className="bg-background-dark rounded-xl border border-white/5 p-4 space-y-2.5">
                                <div className="flex justify-between items-center text-xs">
                                    <span className="text-slate-300">Gross Rental Income</span>
                                    <span className="font-bold text-white font-mono">{rental.grossFormatted}</span>
                                </div>
                                <div className="flex justify-between items-center text-xs">
                                    <span className="text-red-300">(-) 30% Standard Deduction</span>
                                    <span className="font-mono text-red-300">-{rental.stdDeductionFormatted}</span>
                                </div>
                                <div className="border-t border-dashed border-white/10 pt-2">
                                    <div className="flex justify-between items-center text-xs">
                                        <span className="font-bold text-white">Net Rental Added to Income</span>
                                        <span className="font-bold text-primary font-mono">{rental.netFormatted}</span>
                                    </div>
                                </div>
                            </div>
                            <div className="flex items-start gap-3 bg-amber-500/5 border border-amber-500/10 rounded-xl p-4">
                                <Info className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                                <p className="text-xs text-slate-300 leading-relaxed">
                                    A flat 30% deduction is available on your rental income under Section 24(a), regardless of actual expenses.
                                    This reduces your taxable rental income from {rental.grossFormatted} to {rental.netFormatted}.
                                </p>
                            </div>
                        </div>
                    </SectionCard>
                )}

                {/* ════════════════════════════════════════════════════
                   §5.4  SECTION 80C DEDUCTIONS (Conditional on regime)
                   ════════════════════════════════════════════════════ */}
                <SectionCard>
                    <SectionHeader
                        icon={FileText}
                        iconColor="bg-indigo-500/15 text-indigo-400"
                        bgGradient="from-indigo-500/10"
                        title="Deductions"
                        subtitle={deductions.isOldRegime ? 'Old Regime — Section 80C / 80CCD' : 'New Regime — Limited deductions'}
                    />
                    <div className="p-6">
                        {deductions.isOldRegime ? (
                            <div className="space-y-3">
                                {/* Deductions table */}
                                <div className="bg-background-dark rounded-xl border border-white/5 overflow-hidden">
                                    <div className="grid grid-cols-[1fr_auto_auto] gap-x-6 px-4 py-2.5 border-b border-white/5 text-[10px] font-bold text-slate-500 uppercase tracking-wider">
                                        <span>Deduction Type</span>
                                        <span className="text-right">Amount</span>
                                        <span className="text-right">Status</span>
                                    </div>
                                    {deductions.items.map((item, i) => (
                                        <div key={i} className="grid grid-cols-[1fr_auto_auto] gap-x-6 items-center px-4 py-3 border-b border-white/5 last:border-0">
                                            <div>
                                                <p className="text-xs font-semibold text-white">{item.label}</p>
                                                <p className="text-[10px] text-slate-500">{item.sublabel}</p>
                                            </div>
                                            <span className="text-xs font-bold text-white font-mono text-right">
                                                {fmt(item.amount)} / {fmt(item.max)}
                                            </span>
                                            <div className="text-right">
                                                {item.status === 'full' && (
                                                    <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-400">
                                                        <CheckCircle2 className="w-3 h-3" /> Full
                                                    </span>
                                                )}
                                                {item.status === 'partial' && (
                                                    <span className="text-[10px] font-bold text-amber-400">
                                                        Gap: {fmt(item.gap)}
                                                    </span>
                                                )}
                                                {item.status === 'unused' && (
                                                    <span className="text-[10px] font-bold text-red-400">
                                                        Not utilised
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>

                                {/* NPS potential saving alert */}
                                {deductions.items[1]?.status !== 'full' && deductions.items[1]?.potentialSaving > 0 && (
                                    <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                                        <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                                        <p className="text-xs text-slate-300 leading-relaxed">
                                            NPS 80CCD(1B) not fully utilised — investing the gap could save you up to{' '}
                                            <span className="font-bold text-amber-400">{fmt(deductions.items[1].potentialSaving)}</span> in taxes.
                                        </p>
                                    </div>
                                )}
                            </div>
                        ) : (
                            /* New Regime — simplified message */
                            <div className="bg-background-dark rounded-xl border border-white/5 p-5">
                                <div className="flex items-start gap-3">
                                    <Info className="w-5 h-5 text-indigo-400 shrink-0 mt-0.5" />
                                    <div>
                                        <p className="text-xs text-slate-300 leading-relaxed">
                                            Deductions under 80C, 80CCD(1B), and HRA are <span className="font-bold text-white">not available</span> under
                                            the New Tax Regime. Only the ₹75,000 standard deduction applies.
                                        </p>
                                        <p className="text-xs text-slate-400 mt-2">
                                            Your total deduction: <span className="font-bold text-primary">{fmt(deductions.newRegimeDeduction)}</span>
                                        </p>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </SectionCard>

                {/* ════════════════════════════════════════════════════
                   §5.5  EMPLOYER NPS (Conditional)
                   ════════════════════════════════════════════════════ */}
                {employerNps.show && (
                    <SectionCard>
                        <SectionHeader
                            icon={Building2}
                            iconColor="bg-cyan-500/15 text-cyan-400"
                            bgGradient="from-cyan-500/10"
                            title="Employer NPS Deduction"
                            subtitle="Section 80CCD(2)"
                        />
                        <div className="p-6">
                            {employerNps.hasEmployerNps ? (
                                <div className="flex items-start gap-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4">
                                    <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                                    <div>
                                        <p className="text-xs font-bold text-emerald-400 mb-1">Employer NPS Active</p>
                                        <p className="text-xs text-slate-300 leading-relaxed">
                                            Your employer contributes {employerNps.amountFormatted} to your NPS under Section 80CCD(2).
                                            This is tax-free and does not count against your ₹1.5L 80C or ₹50K NPS limits.
                                        </p>
                                    </div>
                                </div>
                            ) : (
                                <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                                    <TrendingDown className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                                    <div>
                                        <p className="text-xs font-bold text-amber-400 mb-1">Potential Tax Saving</p>
                                        <p className="text-xs text-slate-300 leading-relaxed">
                                            Setting up employer NPS could save you{' '}
                                            <span className="font-bold text-amber-400">{employerNps.potentialSavingFormatted}</span>{' '}
                                            in taxes this year — contact your HR team to set up employer NPS contribution under Section 80CCD(2).
                                        </p>
                                    </div>
                                </div>
                            )}
                        </div>
                    </SectionCard>
                )}

            </div>
        </div>
    );
};

export default TaxPlanningTab;
