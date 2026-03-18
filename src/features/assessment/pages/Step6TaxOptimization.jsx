import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronDown, CheckCircle2, AlertTriangle, Loader2, Sparkles, Crown, TrendingUp, Users } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useTaxQuery, useTaxMutation } from '../hooks/useTax';
import { useTaxCalculationQuery } from '../hooks/useTaxCalculation';

const Step6TaxOptimization = () => {
    const navigate = useNavigate();
    const { taxRegime, setTaxRegime, investments80C, setInvestments80C } = useAssessmentStore();

    // API: existing CRUD for persisting tax choices
    const { data: taxData } = useTaxQuery();
    const { mutateAsync: saveTaxApi, isPending: isSaving } = useTaxMutation();

    useEffect(() => {
        if (taxData) {
            if (taxData.taxRegime) setTaxRegime(taxData.taxRegime);
            if (taxData.investments80C !== undefined) setInvestments80C(taxData.investments80C);
        }
    }, [taxData, setTaxRegime, setInvestments80C]);

    // ─── Manual Deduction Inputs ─────────────────────────────────────────────

    const [homeLoanPrincipal, setHomeLoanPrincipal] = useState(0);
    const [tuitionFees, setTuitionFees] = useState(0);
    const [nscFd, setNscFd] = useState(0);

    const [medSelfSpouse, setMedSelfSpouse] = useState(0);
    const [medParentsLt60, setMedParentsLt60] = useState(0);
    const [medParentsGt60, setMedParentsGt60] = useState(0);

    const [otherNps, setOtherNps] = useState(0);
    const [eduLoanInterest, setEduLoanInterest] = useState(0);
    const [homeLoanInterest, setHomeLoanInterest] = useState(0);
    const [donations, setDonations] = useState(0);

    // ─── Debounced deduction totals ──────────────────────────────────────────

    const [debouncedParams, setDebouncedParams] = useState({ deductions80C: 0, deductions80D: 0, otherDeductions: 0 });

    const { data: calcData, isLoading: calcLoading } = useTaxCalculationQuery(debouncedParams);

    const autoEpf = calcData?.autoEpf ?? 0;
    const autoPpf = calcData?.autoPpf ?? 0;
    const autoLifeIns = calcData?.autoLifeInsurance ?? 0;

    const total80CRaw = autoEpf + autoPpf + autoLifeIns + homeLoanPrincipal + tuitionFees + nscFd;
    const final80C = Math.min(total80CRaw, 150000);
    const unused80C = Math.max(0, 150000 - final80C);

    const final80D = Math.min(medSelfSpouse, 25000) + Math.min(medParentsLt60, 25000) + Math.min(medParentsGt60, 50000);
    const finalOtherDeductions = Math.min(otherNps, 50000) + eduLoanInterest + Math.min(homeLoanInterest, 200000) + donations;

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedParams({ deductions80C: final80C, deductions80D: final80D, otherDeductions: finalOtherDeductions });
        }, 300);
        return () => clearTimeout(timer);
    }, [final80C, final80D, finalOtherDeductions]);

    useEffect(() => { setInvestments80C(final80C); }, [final80C, setInvestments80C]);

    // ─── Backend data ────────────────────────────────────────────────────────

    const grossTotalIncome = calcData?.grossTotalIncome ?? 0;
    const incomeCategories = calcData?.incomeCategories ?? {};
    const hraExemption = calcData?.hraExemption ?? 0;
    const annualRentPaid = calcData?.annualRentPaid ?? 0;
    const annualBasic = calcData?.annualBasic ?? 0;
    const actualHraReceived = calcData?.actualHraReceived ?? 0;
    const oldRegime = calcData?.oldRegime ?? {};
    const newRegime = calcData?.newRegime ?? {};
    const recommendedRegime = calcData?.recommendedRegime ?? 'new';
    const savings = calcData?.savings ?? 0;

    useEffect(() => {
        if (!taxRegime && grossTotalIncome > 0) setTaxRegime(recommendedRegime);
    }, [grossTotalIncome, recommendedRegime, taxRegime, setTaxRegime]);

    const handleComplete = async () => {
        if (!taxRegime) {
            toast.error('Select your tax regime — Old or New — to complete', { id: 'step6-guide' });
            return;
        }
        try {
            await saveTaxApi({ taxRegime, investments80C: final80C });
        } catch (err) {
            console.warn('Tax API save failed:', err.message);
        }
        navigate('/assessment/complete');
    };

    const [openAccordion, setOpenAccordion] = useState('');
    const toggleAccordion = (id) => setOpenAccordion(openAccordion === id ? '' : id);

    const fmt = (n) => `₹${Math.round(n ?? 0).toLocaleString('en-IN')}`;

    // ─── Comparison table rows ───────────────────────────────────────────────

    const comparisonRows = [
        { label: 'Gross Income', old: oldRegime.grossIncome, new: newRegime.grossIncome },
        { label: 'Standard Deduction', old: oldRegime.standardDeduction, new: newRegime.standardDeduction },
        { label: 'Section 80C', old: oldRegime.deductions80C, new: newRegime.deductions80C },
        { label: 'Section 80D', old: oldRegime.deductions80D, new: newRegime.deductions80D },
        { label: 'HRA Exemption', old: oldRegime.hraExemption, new: newRegime.hraExemption },
        { label: 'Other Deductions', old: oldRegime.otherDeductions, new: newRegime.otherDeductions },
        { label: 'Net Taxable Income', old: oldRegime.netTaxable, new: newRegime.netTaxable, bold: true },
        { label: 'Tax Calculated', old: oldRegime.baseTax, new: newRegime.baseTax },
        { label: 'Cess (4%)', old: oldRegime.cess, new: newRegime.cess },
        { label: 'FINAL TAX', old: oldRegime.totalTax, new: newRegime.totalTax, final: true },
    ];

    return (
        <div className="flex flex-col h-full bg-background-dark pb-24">
            <div className="flex-1 space-y-6 overflow-y-auto w-full px-2 sm:px-4 hide-scrollbar">

                {/* Header */}
                <div className="mb-2">
                    <h1 className="text-3xl font-bold tracking-tight text-white mb-2">Tax Optimization <span className="text-slate-400 text-lg font-normal">(FY 2026-27)</span></h1>
                    <p className="text-slate-400 text-sm">Most Indians overpay few thousands to lakhs annually. Let's find the best regime for you and maximize deductions.</p>
                </div>

                {/* ═══════════════════ RECOMMENDATION BANNER (full-width) ═══════════════════ */}
                <div className={`rounded-2xl p-5 lg:p-6 border ${recommendedRegime === 'old' ? 'bg-primary/10 border-primary/30' : 'bg-primary/10 border-primary/30'}`}>
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                        <div>
                            <h3 className="font-bold text-lg mb-1 flex items-center gap-2 text-primary">
                                <Sparkles className="w-5 h-5" />
                                RECOMMENDATION: Choose <span className="text-white">{recommendedRegime === 'old' ? 'OLD' : 'NEW'} REGIME</span>
                            </h3>
                            <p className="text-white text-xl lg:text-2xl font-bold mb-2">
                                You SAVE {fmt(savings)}
                            </p>
                            <p className="text-slate-400 text-sm">
                                You save {fmt(savings)}, recommendation on tax optimization for amount. Choose {recommendedRegime === 'old' ? 'old' : 'new'} regime.
                            </p>
                        </div>
                        {/* Regime Toggle Buttons */}
                        <div className="flex items-center gap-0 bg-surface-dark rounded-xl border border-white/10 overflow-hidden flex-shrink-0">
                            <button
                                onClick={() => { setTaxRegime('old'); toast.success('Old Regime selected', { id: 'regime-select' }); }}
                                className={`px-5 py-2.5 text-sm font-bold transition-all ${taxRegime === 'old' ? 'bg-surface-active text-white' : 'text-slate-400 hover:text-white'}`}
                            >
                                Old Regime
                            </button>
                            <button
                                onClick={() => { setTaxRegime('new'); toast.success('New Regime selected', { id: 'regime-select' }); }}
                                className={`px-5 py-2.5 text-sm font-bold transition-all ${taxRegime === 'new' ? 'bg-primary text-background-dark' : 'text-slate-400 hover:text-white'}`}
                            >
                                New Regime
                            </button>
                        </div>
                    </div>
                </div>

                {/* ═══════════════════ COMPARISON TABLE (full-width) ═══════════════════ */}
                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg">
                    {/* Table */}
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            {/* Header */}
                            <thead>
                                <tr className="border-b border-white/10">
                                    <th className="text-left px-5 py-4 text-sm font-bold text-slate-400 w-[40%]"></th>
                                    <th
                                        onClick={() => { setTaxRegime('old'); toast.success('Old Regime selected', { id: 'regime-select' }); }}
                                        className={`text-center px-5 py-4 text-sm font-bold uppercase tracking-wider cursor-pointer transition-all w-[30%] ${
                                            taxRegime === 'old'
                                                ? 'text-primary bg-primary/5 border-b-2 border-primary'
                                                : recommendedRegime === 'old'
                                                    ? 'text-white hover:bg-white/5'
                                                    : 'text-slate-400 hover:bg-white/5'
                                        }`}
                                    >
                                        <div className="flex items-center justify-center gap-2">
                                            Old Regime
                                            {taxRegime === 'old' && <CheckCircle2 className="w-4 h-4 text-primary" />}
                                        </div>
                                    </th>
                                    <th
                                        onClick={() => { setTaxRegime('new'); toast.success('New Regime selected', { id: 'regime-select' }); }}
                                        className={`text-center px-5 py-4 text-sm font-bold uppercase tracking-wider cursor-pointer transition-all w-[30%] ${
                                            taxRegime === 'new'
                                                ? 'text-primary bg-primary/5 border-b-2 border-primary'
                                                : recommendedRegime === 'new'
                                                    ? 'text-white hover:bg-white/5'
                                                    : 'text-slate-400 hover:bg-white/5'
                                        }`}
                                    >
                                        <div className="flex items-center justify-center gap-2">
                                            New Regime
                                            {taxRegime === 'new' && <CheckCircle2 className="w-4 h-4 text-primary" />}
                                        </div>
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {comparisonRows.map((row, i) => (
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
                                                ? `text-lg font-black ${taxRegime === 'old' ? 'text-white' : 'text-slate-300'}`
                                                : row.bold
                                                    ? 'text-sm font-bold text-white'
                                                    : 'text-sm text-slate-300'
                                        } ${taxRegime === 'old' ? 'bg-primary/5' : ''}`}>
                                            {fmt(row.old)}
                                        </td>
                                        <td className={`text-center px-5 py-3 font-mono ${
                                            row.final
                                                ? `text-lg font-black ${taxRegime === 'new' ? 'text-primary' : 'text-slate-300'}`
                                                : row.bold
                                                    ? 'text-sm font-bold text-white'
                                                    : 'text-sm text-slate-300'
                                        } ${taxRegime === 'new' ? 'bg-primary/5' : ''}`}>
                                            {fmt(row.new)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    {calcLoading && (
                        <div className="flex items-center justify-center gap-2 py-3 text-slate-400 text-sm border-t border-white/5">
                            <Loader2 className="w-4 h-4 animate-spin" /> Recalculating...
                        </div>
                    )}
                </div>

                {/* ═══════════════════ 2-COLUMN: Income/Deductions | HRA/Premium ═══════════════════ */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 w-full pb-10">

                    {/* Left Column */}
                    <div className="space-y-6 w-full">

                        {/* Income Summary */}
                        <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg w-full">
                            <div className="bg-surface-active px-5 py-3 border-b border-white/5 flex items-center justify-between">
                                <h3 className="font-bold text-white text-sm tracking-wide">Income Summary</h3>
                                <span className="bg-primary/20 text-primary text-[10px] font-bold px-2 py-0.5 rounded uppercase">Auto-Populated</span>
                            </div>
                            <div className="p-5 font-mono text-sm space-y-3">
                                {Object.entries(incomeCategories).map(([source, amt]) => (
                                    <div key={source} className="flex justify-between items-center text-slate-300">
                                        <span>{source}</span>
                                        <span>{fmt(amt)}</span>
                                    </div>
                                ))}
                                {Object.keys(incomeCategories).length === 0 && (
                                    <div className="text-slate-500 text-center py-2">No income added in Step 2</div>
                                )}
                                <div className="border-t border-dashed border-white/20 pt-3 mt-3 flex justify-between items-center">
                                    <span className="text-white font-bold tracking-widest">GROSS TOTAL</span>
                                    <span className="text-primary text-lg font-bold">{fmt(grossTotalIncome)}</span>
                                </div>
                            </div>
                        </div>

                        {/* Deductions */}
                        <div className="w-full">
                            <div className="space-y-3 w-full">

                                {/* 80C */}
                                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg transition-all w-full">
                                    <button onClick={() => toggleAccordion('80c')} className="w-full px-5 py-4 flex items-center justify-between bg-surface-dark hover:bg-surface-active transition-colors text-left">
                                        <div>
                                            <h4 className="font-bold text-white">80C</h4>
                                            <p className="text-xs text-slate-400">(Max: ₹1,50,000)</p>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-primary font-bold font-mono">{fmt(final80C)}</span>
                                            <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${openAccordion === '80c' ? 'rotate-180' : ''}`} />
                                        </div>
                                    </button>
                                    {openAccordion === '80c' && (
                                        <div className="p-5 border-t border-white/5 bg-background-dark space-y-4">
                                            <div className="space-y-2">
                                                {autoEpf > 0 && <div className="flex justify-between text-sm"><span className="text-emerald-400 flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> EPF Contribution</span><span className="text-slate-300">{fmt(autoEpf)} (Auto)</span></div>}
                                                {autoPpf > 0 && <div className="flex justify-between text-sm"><span className="text-emerald-400 flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> PPF/NPS</span><span className="text-slate-300">{fmt(autoPpf)} (Auto)</span></div>}
                                                {autoLifeIns > 0 && <div className="flex justify-between text-sm"><span className="text-emerald-400 flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> Life Insurance</span><span className="text-slate-300">{fmt(autoLifeIns)} (Auto)</span></div>}
                                            </div>
                                            <div className="space-y-3 pt-3 border-t border-white/10">
                                                <div className="flex justify-between items-center text-sm">
                                                    <span className="text-slate-400">Home Loan Principal:</span>
                                                    <input type="number" value={homeLoanPrincipal || ''} onChange={(e) => setHomeLoanPrincipal(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                                </div>
                                                <div className="flex justify-between items-center text-sm">
                                                    <span className="text-slate-400">Tuition Fees:</span>
                                                    <input type="number" value={tuitionFees || ''} onChange={(e) => setTuitionFees(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                                </div>
                                                <div className="flex justify-between items-center text-sm">
                                                    <span className="text-slate-400">NSC/Tax-Saver FD:</span>
                                                    <input type="number" value={nscFd || ''} onChange={(e) => setNscFd(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                                </div>
                                            </div>
                                            <div className="pt-4 border-t border-white/10">
                                                <div className="flex justify-between text-sm font-bold">
                                                    <span className="text-white">TOTAL USED:</span>
                                                    <span className={`${final80C >= 150000 ? 'text-primary' : 'text-slate-300'}`}>{fmt(final80C)} / ₹1,50,000</span>
                                                </div>
                                                {unused80C > 0 && (
                                                    <div className="mt-4 bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-3 flex gap-3 items-start">
                                                        <AlertTriangle className="text-yellow-500 w-5 h-5 flex-shrink-0 mt-0.5" />
                                                        <div>
                                                            <span className="text-yellow-400 font-bold text-sm block">⚠️ You have {fmt(unused80C)} unused!</span>
                                                            <p className="text-yellow-400/80 text-xs">Invest by March 31 to save up to {fmt(unused80C * 0.312)} in taxes.</p>
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* 80D */}
                                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg transition-all w-full">
                                    <button onClick={() => toggleAccordion('80d')} className="w-full px-5 py-4 flex items-center justify-between bg-surface-dark hover:bg-surface-active transition-colors text-left">
                                        <div>
                                            <h4 className="font-bold text-white">80D</h4>
                                            <p className="text-xs text-slate-400">Medical Insurance</p>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-primary font-bold font-mono">{fmt(final80D)}</span>
                                            <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${openAccordion === '80d' ? 'rotate-180' : ''}`} />
                                        </div>
                                    </button>
                                    {openAccordion === '80d' && (
                                        <div className="p-5 border-t border-white/5 bg-background-dark space-y-3">
                                            <div className="flex justify-between items-center text-sm">
                                                <span className="text-slate-400">Self + Spouse Premium <br /><span className="text-[10px]">(max ₹25,000)</span></span>
                                                <input type="number" value={medSelfSpouse || ''} onChange={(e) => setMedSelfSpouse(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                            </div>
                                            <div className="flex justify-between items-center text-sm">
                                                <span className="text-slate-400">Parents Premium (&lt;60) <br /><span className="text-[10px]">(max ₹25,000)</span></span>
                                                <input type="number" value={medParentsLt60 || ''} onChange={(e) => setMedParentsLt60(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                            </div>
                                            <div className="flex justify-between items-center text-sm">
                                                <span className="text-slate-400">Parents Premium (≥60) <br /><span className="text-[10px]">(max ₹50,000)</span></span>
                                                <input type="number" value={medParentsGt60 || ''} onChange={(e) => setMedParentsGt60(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Others */}
                                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg transition-all w-full">
                                    <button onClick={() => toggleAccordion('other')} className="w-full px-5 py-4 flex items-center justify-between bg-surface-dark hover:bg-surface-active transition-colors text-left">
                                        <div>
                                            <h4 className="font-bold text-white">Others</h4>
                                            <p className="text-xs text-slate-400">NPS, Loans, Donations</p>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-primary font-bold font-mono">{fmt(finalOtherDeductions)}</span>
                                            <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${openAccordion === 'other' ? 'rotate-180' : ''}`} />
                                        </div>
                                    </button>
                                    {openAccordion === 'other' && (
                                        <div className="p-5 border-t border-white/5 bg-background-dark space-y-3">
                                            <div className="flex justify-between items-center text-sm">
                                                <span className="text-slate-400">Addt. NPS 80CCD(1B) <br /><span className="text-[10px]">(max ₹50,000)</span></span>
                                                <input type="number" value={otherNps || ''} onChange={(e) => setOtherNps(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                            </div>
                                            <div className="flex justify-between items-center text-sm">
                                                <span className="text-slate-400">Edu Loan Int. 80E <br /><span className="text-[10px]">(no limit)</span></span>
                                                <input type="number" value={eduLoanInterest || ''} onChange={(e) => setEduLoanInterest(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                            </div>
                                            <div className="flex justify-between items-center text-sm">
                                                <span className="text-slate-400">Home Loan Int. 24(b) <br /><span className="text-[10px]">(max ₹2,00,000)</span></span>
                                                <input type="number" value={homeLoanInterest || ''} onChange={(e) => setHomeLoanInterest(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                            </div>
                                            <div className="flex justify-between items-center text-sm">
                                                <span className="text-slate-400">Donations 80G <br /><span className="text-[10px]">(subject to limits)</span></span>
                                                <input type="number" value={donations || ''} onChange={(e) => setDonations(parseFloat(e.target.value) || 0)} placeholder="Enter" className="bg-surface text-right text-white px-3 py-1.5 rounded-lg border border-white/5 focus:border-primary focus:outline-none w-32" />
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>

                    </div>

                    {/* Right Column */}
                    <div className="space-y-6 w-full">

                        {/* HRA Exemption */}
                        <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg w-full">
                            <div className="bg-surface-active px-5 py-3 border-b border-white/5 flex items-center justify-between">
                                <h3 className="font-bold text-white text-sm tracking-wide">HRA Exemption</h3>
                                <span className="bg-primary/20 text-primary text-[10px] font-bold px-2 py-0.5 rounded uppercase">Auto-calculated</span>
                            </div>
                            <div className="p-5 font-mono text-sm space-y-2">
                                <div className="flex justify-between items-center text-slate-300">
                                    <span>Standa Exemption</span>
                                    <span>{fmt(actualHraReceived)}</span>
                                </div>
                                <div className="flex justify-between items-center text-slate-300">
                                    <span>Auto-calculated</span>
                                    <span>-</span>
                                </div>
                                <div className="border-t border-dashed border-white/20 pt-3 flex justify-between items-center">
                                    <span className="text-white font-bold tracking-widest">EXEMPTION</span>
                                    <span className="text-primary font-bold">{fmt(hraExemption)}</span>
                                </div>
                            </div>
                        </div>

                        {/* Premium Upsell Cards */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

                            {/* Tax Harvesting */}
                            <div className="relative group rounded-2xl p-[1px] overflow-hidden cursor-pointer shadow-lg hover:shadow-amber-500/10 transition-shadow duration-500">
                                <div className="absolute inset-0 bg-gradient-to-br from-amber-500/50 via-transparent to-orange-500/20 opacity-40 group-hover:opacity-100 transition-opacity duration-500 z-0"></div>
                                <div className="relative z-10 bg-surface-dark/40 backdrop-blur-2xl h-full rounded-2xl p-5 flex flex-col border border-white/10 group-hover:border-amber-500/40 transition-colors duration-300">
                                    <div className="bg-gradient-to-r from-amber-400 to-orange-500 text-black text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-widest w-fit mb-3">Premium</div>
                                    <h4 className="text-white font-bold text-base mb-2 group-hover:text-amber-400 transition-colors duration-300">Tax Harvesting</h4>
                                    <p className="text-xs text-slate-300 leading-relaxed group-hover:text-white transition-colors duration-300">Compare your <span className="text-amber-400 font-bold">portfolio</span> harvest of tax harvesting.</p>
                                </div>
                            </div>

                            {/* Family Income Distribution */}
                            <div className="relative group rounded-2xl p-[1px] overflow-hidden cursor-pointer shadow-lg hover:shadow-purple-500/10 transition-shadow duration-500">
                                <div className="absolute inset-0 bg-gradient-to-br from-purple-500/50 via-transparent to-pink-500/20 opacity-40 group-hover:opacity-100 transition-opacity duration-500 z-0"></div>
                                <div className="relative z-10 bg-surface-dark/40 backdrop-blur-2xl h-full rounded-2xl p-5 flex flex-col border border-white/10 group-hover:border-purple-500/40 transition-colors duration-300">
                                    <div className="bg-gradient-to-r from-purple-400 to-pink-500 text-white text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-widest w-fit mb-3">Premium</div>
                                    <h4 className="text-white font-bold text-base mb-2 group-hover:text-purple-400 transition-colors duration-300">Family Income Distribution</h4>
                                    <p className="text-xs text-slate-300 leading-relaxed group-hover:text-white transition-colors duration-300">Compare your account on <span className="text-purple-400 font-bold">Family Income</span> and bins distribution amounts.</p>
                                </div>
                            </div>

                        </div>
                    </div>
                </div>

            </div>

            {/* Bottom Navigation */}
            <div className="fixed bottom-0 left-0 right-0 bg-background-dark/80 backdrop-blur-lg border-t border-white/5 p-4 z-50">
                <div className="max-w-[1200px] mx-auto flex items-center justify-between">
                    <button
                        onClick={() => navigate('/assessment/step-5')}
                        className="px-6 py-3 bg-surface-dark hover:bg-surface-active text-white font-bold text-sm rounded-xl transition-all"
                    >
                        Back
                    </button>
                    <div className="flex items-center gap-3">
                        <div className="hidden sm:flex items-center gap-2 px-4 py-3 bg-surface-dark border border-white/10 rounded-xl">
                            <CheckCircle2 className="w-4 h-4 text-primary" />
                            <span className="text-sm font-semibold text-slate-400">Step 6/6</span>
                        </div>
                        <button
                            onClick={handleComplete}
                            disabled={isSaving}
                            className="px-8 py-3 bg-primary hover:bg-primary-dark active:scale-[0.98] text-background-dark font-bold text-sm rounded-xl flex items-center gap-2 transition-all shadow-[0_0_15px_rgba(13,242,89,0.25)] disabled:opacity-60"
                        >
                            {isSaving ? (
                                <><Loader2 className="w-4 h-4 animate-spin" /> Saving...</>
                            ) : (
                                <>Complete Assessment <ArrowRight className="w-4 h-4" /></>
                            )}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Step6TaxOptimization;
