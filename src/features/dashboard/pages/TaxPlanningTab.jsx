import React from 'react';
import {
    Receipt, Scale, Home, FileText, Building2,
    CheckCircle2, AlertTriangle, Info, ArrowDownRight,
    ArrowUpRight, Sparkles, TrendingDown,
} from 'lucide-react';
import { useTaxAnalysis } from '../../../hooks/useTaxAnalysis';
import { TaxSkeleton } from '../../../components/ui/DashboardSkeleton';
import SectionNav from '../components/SectionNav';

const TAX_SECTIONS = [
    { id: 'regime', label: 'Regime' },
    { id: 'tds', label: 'TDS' },
    { id: 'deductions', label: 'Deductions' },
    { id: 'nps', label: 'NPS' },
];



/* ═══════════════════════════════════════════════════════════════
   REUSABLE SUB-COMPONENTS
   ═══════════════════════════════════════════════════════════════ */

const SectionCard = ({ children, className = '', id }) => (
    <section id={id} className={`bg-surface-dark rounded-3xl border border-white/5 shadow-xl overflow-hidden ${className}`}>
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
                <h3 className="text-lg font-bold text-white">{title}</h3>
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{subtitle}</span>
            </div>
        </div>
    </div>
);

const RowItem = ({ label, value, muted, highlight, strikethrough }) => (
    <div className={`flex justify-between items-center gap-2 text-sm ${strikethrough ? 'opacity-40 line-through' : ''}`}>
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

    if (!regimeComparison) {
        return <TaxSkeleton />;
    }

    const { old: oldR, new: newR, recommended, savingsFormatted } = regimeComparison;

    return (
        <>

            <SectionNav sections={TAX_SECTIONS} />
            <div className="w-full max-w-6xl mx-auto px-4 py-6 pb-24 space-y-8">

                {/* Page Title */}
                <div>
                    <h2 className="text-2xl font-bold tracking-tight">Tax Planning</h2>
                    <p className="text-sm text-slate-500 mt-0.5">FY 2026-27 · Personalised tax analysis</p>
                </div>

                {/* ════════════════════════════════════════════════════
                   §5.1 TAX REGIME COMPARISON (Always)
                   ════════════════════════════════════════════════════ */}

                {/* Recommendation Banner */}
                <div id="regime" className="rounded-2xl p-5 lg:p-6 border bg-primary/10 border-primary/30">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                        <div>
                            <h3 className="font-bold text-lg mb-1 flex items-center gap-2 text-primary">
                                <Sparkles className="w-5 h-5" />
                                RECOMMENDATION: Choose <span className="text-white">{recommended === 'old' ? 'OLD' : 'NEW'} REGIME</span>
                            </h3>
                            <p className="text-white text-xl lg:text-2xl font-bold mb-2">
                                You SAVE {savingsFormatted}
                            </p>
                            <p className="text-slate-400 text-sm">
                                {recommended === 'old'
                                    ? 'Your deductions make Old Regime more beneficial. Review this each year if your investments change.'
                                    : 'With lower deductions, New Regime\'s wider slabs give you better savings.'}
                            </p>
                        </div>
                    </div>
                </div>

                {/* Comparison Table */}
                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                                <tr className="border-b border-white/10">
                                    <th className="text-left px-5 py-4 text-sm font-bold text-slate-400 w-[40%]"></th>
                                    <th className={`text-center px-5 py-4 text-sm font-bold uppercase tracking-wider w-[30%] ${
                                        recommended === 'old'
                                            ? 'text-primary bg-primary/5 border-b-2 border-primary'
                                            : 'text-slate-400'
                                    }`}>
                                        <div className="flex items-center justify-center gap-2">
                                            Old Regime
                                            {recommended === 'old' && <CheckCircle2 className="w-4 h-4 text-primary" />}
                                        </div>
                                    </th>
                                    <th className={`text-center px-5 py-4 text-sm font-bold uppercase tracking-wider w-[30%] ${
                                        recommended === 'new'
                                            ? 'text-primary bg-primary/5 border-b-2 border-primary'
                                            : 'text-slate-400'
                                    }`}>
                                        <div className="flex items-center justify-center gap-2">
                                            New Regime
                                            {recommended === 'new' && <CheckCircle2 className="w-4 h-4 text-primary" />}
                                        </div>
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {[
                                    { label: 'Gross Income', old: oldR.grossIncome, new: newR.grossIncome },
                                    { label: 'Standard Deduction', old: oldR.stdDeduction, new: newR.stdDeduction },
                                    { label: 'Section 80C', old: oldR.deductions80C, new: 0 },
                                    { label: 'NPS 80CCD', old: oldR.deductionsNps, new: 0 },
                                    { label: 'HRA Exemption', old: oldR.hraExemption ?? 0, new: 0 },
                                    { label: 'Other Deductions', old: oldR.otherDeductions ?? 0, new: 0 },
                                    { label: 'Net Taxable Income', old: oldR.taxableIncome, new: newR.taxableIncome, bold: true },
                                    { label: 'Tax Calculated', old: oldR.baseTax, new: newR.baseTax },
                                    { label: 'Cess (4%)', old: oldR.cess, new: newR.cess },
                                    { label: 'FINAL TAX', old: oldR.totalTax, new: newR.totalTax, final: true },
                                ].map((row) => (
                                    <tr
                                        key={row.label}
                                        className={`border-b border-white/5 transition-colors ${
                                            row.final ? 'bg-surface-active' : row.bold ? 'bg-white/[0.02]' : ''
                                        }`}
                                    >
                                        <td className={`px-5 py-3 text-sm ${row.final ? 'text-white font-black text-base' : row.bold ? 'text-white font-bold' : 'text-slate-300'}`}>
                                            {row.label}
                                        </td>
                                        <td className={`text-center px-5 py-3 font-mono ${
                                            row.final
                                                ? `text-lg font-black ${recommended === 'old' ? 'text-white' : 'text-slate-300'}`
                                                : row.bold
                                                    ? 'text-sm font-bold text-white'
                                                    : 'text-sm text-slate-300'
                                        } ${recommended === 'old' ? 'bg-primary/5' : ''}`}>
                                            {fmt(row.old)}
                                        </td>
                                        <td className={`text-center px-5 py-3 font-mono ${
                                            row.final
                                                ? `text-lg font-black ${recommended === 'new' ? 'text-primary' : 'text-slate-300'}`
                                                : row.bold
                                                    ? 'text-sm font-bold text-white'
                                                    : 'text-sm text-slate-300'
                                        } ${recommended === 'new' ? 'bg-primary/5' : ''}`}>
                                            {fmt(row.new)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* ════════════════════════════════════════════════════
                   §5.2  TDS RECONCILIATION (Always)
                   ════════════════════════════════════════════════════ */}
                <SectionCard id="tds">
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
                                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">Estimated Tax Liability</p>
                                <p className="text-lg font-black text-white">{tds.recommendedTaxFormatted}</p>
                                <p className="text-xs text-slate-500 mt-0.5">{recommended === 'old' ? 'Old Regime' : 'New Regime'}</p>
                            </div>
                            <div className="bg-background-dark rounded-xl border border-white/5 p-4 text-center">
                                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">TDS Deducted (Est.)</p>
                                <p className="text-lg font-black text-white">{tds.totalTDSFormatted}</p>
                                <p className="text-xs text-slate-500 mt-0.5">@10% of salary</p>
                            </div>
                        </div>

                        {/* Status message */}
                        {tds.status === 'refund' && (
                            <div className="flex items-start gap-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4">
                                <ArrowDownRight className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-sm font-bold text-emerald-400 mb-1">Refund Due: {tds.diffFormatted}</p>
                                    <p className="text-sm text-slate-400 leading-relaxed">
                                        You are entitled to a refund of {tds.diffFormatted}. File your ITR before 31 July to claim it.
                                    </p>
                                </div>
                            </div>
                        )}
                        {tds.status === 'due' && (
                            <div className="flex items-start gap-3 bg-red-500/10 border border-red-500/20 rounded-xl p-4">
                                <ArrowUpRight className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-sm font-bold text-red-400 mb-1">Additional Tax Due: {tds.diffFormatted}</p>
                                    <p className="text-sm text-slate-400 leading-relaxed">
                                        You have an additional tax demand of {tds.diffFormatted}. Pay this as Advance Tax before 15 March to avoid interest.
                                    </p>
                                </div>
                            </div>
                        )}
                        {tds.status === 'matched' && (
                            <div className="flex items-start gap-3 bg-sky-500/10 border border-sky-500/20 rounded-xl p-4">
                                <CheckCircle2 className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-sm font-bold text-sky-400 mb-1">TDS Perfectly Matched</p>
                                    <p className="text-sm text-slate-400 leading-relaxed">
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
                                <div className="flex justify-between items-center text-sm">
                                    <span className="text-slate-300">Gross Rental Income</span>
                                    <span className="font-bold text-white font-mono">{rental.grossFormatted}</span>
                                </div>
                                <div className="flex justify-between items-center text-sm">
                                    <span className="text-red-300">(-) 30% Standard Deduction</span>
                                    <span className="font-mono text-red-300">-{rental.stdDeductionFormatted}</span>
                                </div>
                                <div className="border-t border-dashed border-white/10 pt-2">
                                    <div className="flex justify-between items-center text-sm">
                                        <span className="font-bold text-white">Net Rental Added to Income</span>
                                        <span className="font-bold text-primary font-mono">{rental.netFormatted}</span>
                                    </div>
                                </div>
                            </div>
                            <div className="flex items-start gap-3 bg-amber-500/5 border border-amber-500/10 rounded-xl p-4">
                                <Info className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                                <p className="text-sm text-slate-400 leading-relaxed">
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
                <SectionCard id="deductions">
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
                                    <div className="grid grid-cols-[1fr_auto_auto] gap-x-6 px-4 py-2.5 border-b border-white/5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                                        <span>Deduction Type</span>
                                        <span className="text-right">Amount</span>
                                        <span className="text-right">Status</span>
                                    </div>
                                    {deductions.items.map((item, i) => (
                                        <div key={i} className="grid grid-cols-[1fr_auto_auto] gap-x-6 items-center px-4 py-3 border-b border-white/5 last:border-0">
                                            <div>
                                                <p className="text-sm font-bold text-white">{item.label}</p>
                                                <p className="text-xs text-slate-500">{item.sublabel}</p>
                                            </div>
                                            <span className="text-sm font-bold text-white font-mono text-right">
                                                {fmt(item.amount)} / {fmt(item.max)}
                                            </span>
                                            <div className="text-right">
                                                {item.status === 'full' && (
                                                    <span className="inline-flex items-center gap-1 text-sm font-bold text-emerald-400">
                                                        <CheckCircle2 className="w-3.5 h-3.5" /> Full
                                                    </span>
                                                )}
                                                {item.status === 'partial' && (
                                                    <span className="text-sm font-bold text-amber-400">
                                                        Gap: {fmt(item.gap)}
                                                    </span>
                                                )}
                                                {item.status === 'unused' && (
                                                    <span className="text-sm font-bold text-red-400">
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
                                        <p className="text-sm text-slate-400 leading-relaxed">
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
                                        <p className="text-sm text-slate-400 leading-relaxed">
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
                    <SectionCard id="nps">
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
                                        <p className="text-sm font-bold text-emerald-400 mb-1">Employer NPS Active</p>
                                        <p className="text-sm text-slate-400 leading-relaxed">
                                            Your employer contributes {employerNps.amountFormatted} to your NPS under Section 80CCD(2).
                                            This is tax-free and does not count against your ₹1.5L 80C or ₹50K NPS limits.
                                        </p>
                                    </div>
                                </div>
                            ) : (
                                <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                                    <TrendingDown className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                                    <div>
                                        <p className="text-sm font-bold text-amber-400 mb-1">Potential Tax Saving</p>
                                        <p className="text-sm text-slate-400 leading-relaxed">
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
        </>
    );
};

export default TaxPlanningTab;
