import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronDown, CheckCircle2, AlertTriangle, Loader2, Sparkles, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useTaxQuery, useTaxMutation } from '../hooks/useTax';

const Step6TaxOptimization = () => {
    const navigate = useNavigate();
    const {
        incomes,
        expenses,
        assets,
        insurance,
        taxRegime,
        setTaxRegime,
        investments80C,
        setInvestments80C
    } = useAssessmentStore();

    // API Integration
    const { data: taxData } = useTaxQuery();
    const { mutateAsync: saveTaxApi, isPending: isSaving } = useTaxMutation();

    useEffect(() => {
        if (taxData) {
            if (taxData.taxRegime) setTaxRegime(taxData.taxRegime);
            if (taxData.investments80C !== undefined) setInvestments80C(taxData.investments80C);
        }
    }, [taxData, setTaxRegime, setInvestments80C]);

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

    // --- Section A: Income Summary ---
    const calculateAnnual = (item) => {
        if (item.frequency === 'Monthly') return item.amount * 12;
        if (item.frequency === 'Quarterly') return item.amount * 4;
        if (item.frequency === 'Yearly') return item.amount;
        return item.amount; // One-time
    };

    const incomeCategories = incomes.reduce((acc, inc) => {
        const cat = inc.source || 'Other';
        acc[cat] = (acc[cat] || 0) + calculateAnnual(inc);
        return acc;
    }, {});
    const grossTotalIncome = Object.values(incomeCategories).reduce((sum, val) => sum + val, 0);

    // --- Section B: Deductions Data & Calculation ---

    // 80C Auto-fetches
    const autoEpf = assets.filter(a => (a.subCategory || a.category)?.includes('EPF')).reduce((sum, a) => sum + (parseFloat(a.amount) || 0), 0);
    const autoPpf = assets.filter(a => (a.subCategory || a.category)?.includes('PPF') || (a.subCategory || a.category)?.includes('NPS')).reduce((sum, a) => sum + (parseFloat(a.amount) || 0), 0);
    const autoLifeIns = (insurance?.personalLife || []).reduce((sum, p) => sum + (parseFloat(p.premium) || 0), 0);

    const [homeLoanPrincipal, setHomeLoanPrincipal] = useState(0);
    const [tuitionFees, setTuitionFees] = useState(0);
    const [nscFd, setNscFd] = useState(0);

    const total80C = autoEpf + autoPpf + autoLifeIns + homeLoanPrincipal + tuitionFees + nscFd;
    const final80C = Math.min(total80C, 150000);
    const unused80C = Math.max(0, 150000 - final80C);

    useEffect(() => {
        setInvestments80C(final80C);
    }, [final80C, setInvestments80C]);

    // 80D
    const [medSelfSpouse, setMedSelfSpouse] = useState(0);
    const [medParentsLt60, setMedParentsLt60] = useState(0);
    const [medParentsGt60, setMedParentsGt60] = useState(0);

    const final80D = Math.min(medSelfSpouse, 25000) + Math.min(medParentsLt60, 25000) + Math.min(medParentsGt60, 50000);

    // Other Deductions State
    const [otherNps, setOtherNps] = useState(0);
    const [eduLoanInterest, setEduLoanInterest] = useState(0);
    const [homeLoanInterest, setHomeLoanInterest] = useState(0);
    const [donations, setDonations] = useState(0);

    const finalOtherDeductions = Math.min(otherNps, 50000) + eduLoanInterest + Math.min(homeLoanInterest, 200000) + donations;

    // HRA Exemption Auto-Calculation
    const rentPaidMonthly = expenses.filter(e => e.category === 'Rent/Mortgage').reduce((sum, e) => {
        if (e.frequency === 'Monthly') return sum + e.amount;
        if (e.frequency === 'Yearly') return sum + e.amount / 12;
        return sum + e.amount;
    }, 0);
    const annualRentPaid = rentPaidMonthly * 12;
    // Assume Basic Salary is 50% of Total Salary if explicit basic isn't available. Assume actual HRA is 40% of Basic.
    const salaryIncome = incomeCategories['Salary'] || 0;
    const annualBasic = salaryIncome * 0.50;
    const actualHraReceived = annualBasic * 0.40;

    let finalHraExemption = 0;
    if (annualRentPaid > 0 && annualBasic > 0) {
        const rentMinus10PercentBasic = Math.max(0, annualRentPaid - (0.10 * annualBasic));
        const fiftyPercentBasic = 0.50 * annualBasic; // Assume Metro
        finalHraExemption = Math.min(actualHraReceived, fiftyPercentBasic, rentMinus10PercentBasic);
    }


    // --- Section C: Tax Calculations ---
    const calculateTax = (income, regime) => {
        let taxable = income;
        let tax = 0;

        if (regime === 'old') {
            const stdDeduction = 50000;
            taxable = Math.max(0, income - stdDeduction - final80C - final80D - finalHraExemption - finalOtherDeductions);

            // Calculate tax slabs (Old Regime)
            if (taxable > 1000000) {
                tax += (taxable - 1000000) * 0.30;
                tax += 112500;
            } else if (taxable > 500000) {
                tax += (taxable - 500000) * 0.20;
                tax += 12500;
            } else if (taxable > 250000) {
                tax += (taxable - 250000) * 0.05;
            }
        } else {
            // New Regime (FY 2024-25/2026-27 updated slabs)
            const stdDeduction = 75000;
            const employerNps = 0; // Assuming 0 for now unless user enters it
            taxable = Math.max(0, income - stdDeduction - employerNps);

            if (taxable <= 700000) {
                return { taxable, baseTax: 0, cess: 0, totalTax: 0, rate: 0 };
            }

            if (taxable > 1500000) {
                tax += (taxable - 1500000) * 0.30;
                tax += 150000;
            } else if (taxable > 1200000) {
                tax += (taxable - 1200000) * 0.20;
                tax += 90000;
            } else if (taxable > 1000000) {
                tax += (taxable - 1000000) * 0.15;
                tax += 60000;
            } else if (taxable > 700000) {
                tax += (taxable - 700000) * 0.10;
                tax += 30000;
            } else if (taxable > 300000) {
                tax += (taxable - 300000) * 0.05;
            }
            // Add marginal relief if applicable (simplified here)
        }

        const cess = tax * 0.04;
        const totalTax = tax + cess;
        const rate = income > 0 ? (totalTax / income) * 100 : 0;

        return { taxable, baseTax: tax, cess, totalTax, rate };
    };

    const oldTax = calculateTax(grossTotalIncome, 'old');
    const newTax = calculateTax(grossTotalIncome, 'new');

    const recommendedRegime = oldTax.totalTax <= newTax.totalTax ? 'old' : 'new';
    const savings = Math.abs(oldTax.totalTax - newTax.totalTax);

    // Auto-select regime if not selected
    useEffect(() => {
        if (!taxRegime && grossTotalIncome > 0) {
            setTaxRegime(recommendedRegime);
        }
    }, [grossTotalIncome, recommendedRegime, taxRegime, setTaxRegime]);


    // UI State
    const [openAccordion, setOpenAccordion] = useState(''); // '80c', '80d', 'other'
    const toggleAccordion = (id) => setOpenAccordion(openAccordion === id ? '' : id);

    return (
        <div className="flex flex-col h-full bg-background-dark pb-24">
            <div className="flex-1 space-y-6 overflow-y-auto w-full px-2 sm:px-4 hide-scrollbar">

                {/* Header */}
                <div className="mb-6">
                    <h1 className="text-3xl font-bold tracking-tight text-white mb-2">Tax Optimization <span className="text-slate-400 text-lg font-normal">(FY 2026-27)</span></h1>
                    <p className="text-slate-400 text-sm">Most Indians overpay few thousands to lakhs annually. Let's find the best regime for you and maximize deductions.</p>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 w-full pb-10">

                    {/* Left Column: Data & Deductions */}
                    <div className="space-y-6 w-full">

                        {/* Section A: Income Summary */}
                        <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg w-full">
                            <div className="bg-surface-active px-5 py-3 border-b border-white/5 flex items-center justify-between">
                                <h3 className="font-bold text-white text-sm tracking-wide">YOUR TOTAL INCOME (FY 2026-27)</h3>
                                <span className="bg-primary/20 text-primary text-[10px] font-bold px-2 py-0.5 rounded uppercase">Auto-Populated</span>
                            </div>
                            <div className="p-5 font-mono text-sm space-y-3">
                                {Object.entries(incomeCategories).map(([source, amt]) => (
                                    <div key={source} className="flex justify-between items-center text-slate-300">
                                        <span>{source}</span>
                                        <span>₹{amt.toLocaleString()}</span>
                                    </div>
                                ))}
                                {Object.keys(incomeCategories).length === 0 && (
                                    <div className="text-slate-500 text-center py-2">No income added in Step 2</div>
                                )}
                                <div className="border-t border-dashed border-white/20 pt-3 mt-3 flex justify-between items-center">
                                    <span className="text-white font-bold tracking-widest">GROSS TOTAL</span>
                                    <span className="text-primary text-lg font-bold">₹{grossTotalIncome.toLocaleString()}</span>
                                </div>
                            </div>
                        </div>

                        {/* Section B: Deductions */}
                        <div className="w-full">
                            <h3 className="text-slate-400 font-bold mb-3 uppercase tracking-wider text-sm flex items-center gap-2">
                                Deductions <span className="bg-slate-800 text-slate-300 px-2 py-0.5 rounded text-[10px]">Old Regime Only</span>
                            </h3>

                            <div className="space-y-3 w-full">
                                {/* 80C Accordion */}
                                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg transition-all w-full">
                                    <button
                                        onClick={() => toggleAccordion('80c')}
                                        className="w-full px-5 py-4 flex items-center justify-between bg-surface-dark hover:bg-surface-active transition-colors text-left"
                                    >
                                        <div>
                                            <h4 className="font-bold text-white">SECTION 80C DEDUCTIONS</h4>
                                            <p className="text-xs text-slate-400">(Max: ₹1,50,000)</p>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-primary font-bold font-mono">₹{final80C.toLocaleString()}</span>
                                            <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${openAccordion === '80c' ? 'rotate-180' : ''}`} />
                                        </div>
                                    </button>

                                    {openAccordion === '80c' && (
                                        <div className="p-5 border-t border-white/5 bg-background-dark space-y-4">
                                            {/* Auto Filled */}
                                            <div className="space-y-2">
                                                {autoEpf > 0 && <div className="flex justify-between text-sm"><span className="text-emerald-400 flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> EPF Contribution</span><span className="text-slate-300">₹{autoEpf.toLocaleString()} (Auto)</span></div>}
                                                {autoPpf > 0 && <div className="flex justify-between text-sm"><span className="text-emerald-400 flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> PPF/NPS</span><span className="text-slate-300">₹{autoPpf.toLocaleString()} (Auto)</span></div>}
                                                {autoLifeIns > 0 && <div className="flex justify-between text-sm"><span className="text-emerald-400 flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> Life Insurance</span><span className="text-slate-300">₹{autoLifeIns.toLocaleString()} (Auto)</span></div>}
                                            </div>

                                            {/* Manual Inputs */}
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
                                                    <span className={`${final80C >= 150000 ? 'text-primary' : 'text-slate-300'}`}>₹{final80C.toLocaleString()} / ₹1,50,000</span>
                                                </div>
                                                {unused80C > 0 && (
                                                    <div className="mt-4 bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-3 flex gap-3 items-start">
                                                        <AlertTriangle className="text-yellow-500 w-5 h-5 flex-shrink-0 mt-0.5" />
                                                        <div>
                                                            <span className="text-yellow-400 font-bold text-sm block">⚠️ You have ₹{unused80C.toLocaleString()} unused!</span>
                                                            <p className="text-yellow-400/80 text-xs">Invest by March 31 to save up to ₹{Math.round(unused80C * 0.312).toLocaleString()} in taxes.</p>
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* 80D Accordion */}
                                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg transition-all w-full">
                                    <button
                                        onClick={() => toggleAccordion('80d')}
                                        className="w-full px-5 py-4 flex items-center justify-between bg-surface-dark hover:bg-surface-active transition-colors text-left"
                                    >
                                        <div>
                                            <h4 className="font-bold text-white">SECTION 80D</h4>
                                            <p className="text-xs text-slate-400">Medical Insurance</p>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-primary font-bold font-mono">₹{final80D.toLocaleString()}</span>
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

                                {/* Other Deductions Accordion */}
                                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg transition-all w-full">
                                    <button
                                        onClick={() => toggleAccordion('other')}
                                        className="w-full px-5 py-4 flex items-center justify-between bg-surface-dark hover:bg-surface-active transition-colors text-left"
                                    >
                                        <div>
                                            <h4 className="font-bold text-white">OTHER DEDUCTIONS</h4>
                                            <p className="text-xs text-slate-400">NPS, Loans, Donations</p>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-primary font-bold font-mono">₹{finalOtherDeductions.toLocaleString()}</span>
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

                                {/* HRA Exemption Display */}
                                <div className="bg-surface-dark border border-white/5 rounded-2xl overflow-hidden shadow-lg w-full">
                                    <div className="bg-surface-active px-5 py-3 border-b border-white/5 flex items-center justify-between">
                                        <h3 className="font-bold text-white text-sm tracking-wide">HRA EXEMPTION</h3>
                                        <span className="bg-primary/20 text-primary text-[10px] font-bold px-2 py-0.5 rounded uppercase">Auto-Calculated</span>
                                    </div>
                                    <div className="p-5 font-mono text-sm space-y-2">
                                        <div className="flex justify-between items-center text-slate-300">
                                            <span>Actual HRA Received:</span>
                                            <span>₹{actualHraReceived.toLocaleString()}</span>
                                        </div>
                                        <div className="flex justify-between items-center text-slate-300">
                                            <span>Rent Paid:</span>
                                            <span>₹{annualRentPaid.toLocaleString()}</span>
                                        </div>
                                        <div className="flex justify-between items-center text-slate-300">
                                            <span>Basic Salary:</span>
                                            <span>₹{annualBasic.toLocaleString()}</span>
                                        </div>
                                        <div className="border-t border-dashed border-white/20 pt-3 flex justify-between items-center">
                                            <span className="text-white font-bold tracking-widest">EXEMPTION:</span>
                                            <span className="text-primary font-bold">₹{Math.round(finalHraExemption).toLocaleString()}</span>
                                        </div>
                                    </div>
                                </div>

                            </div>
                        </div>

                    </div>


                    {/* Right Column: The Big Comparison */}
                    <div className="space-y-6 w-full flex flex-col h-full">
                        <h3 className="text-slate-400 font-bold uppercase tracking-wider text-sm flex items-center gap-2">
                            The Big Comparison <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                        </h3>

                        <div className="grid grid-cols-2 gap-3 lg:gap-4 flex-grow">

                            {/* Old Regime Card */}
                            <div
                                onClick={() => setTaxRegime('old')}
                                className={`rounded-2xl p-4 lg:p-5 border-2 transition-all cursor-pointer relative overflow-hidden flex flex-col ${taxRegime === 'old' ? 'bg-surface-active border-primary shadow-[0_0_20px_rgba(34,197,94,0.15)]' : 'bg-surface-dark border-white/5 hover:border-white/20'}`}
                            >
                                {recommendedRegime === 'old' && (
                                    <div className="absolute top-0 right-0 bg-primary text-black font-bold text-[10px] px-3 py-1 rounded-bl-lg">RECOMMENDED</div>
                                )}
                                <div className="flex items-center justify-between mb-4">
                                    <h4 className={`font-bold uppercase tracking-widest text-sm ${taxRegime === 'old' ? 'text-primary' : 'text-slate-400'}`}>OLD REGIME</h4>
                                    {taxRegime === 'old' && <CheckCircle2 className="w-5 h-5 text-primary" />}
                                </div>

                                <div className="space-y-2 font-mono text-[11px] lg:text-xs text-slate-300 flex-grow">
                                    <div className="flex justify-between"><span>Gross Income</span><span>₹{grossTotalIncome.toLocaleString()}</span></div>
                                    <div className="flex justify-between text-red-300"><span>(-) Std Ded.</span><span>-₹50,000</span></div>
                                    <div className="flex justify-between text-red-300"><span>(-) 80C</span><span>-₹{final80C.toLocaleString()}</span></div>
                                    <div className="flex justify-between text-red-300"><span>(-) 80D</span><span>-₹{final80D.toLocaleString()}</span></div>
                                    <div className="flex justify-between text-red-300"><span>(-) HRA</span><span>-₹{Math.round(finalHraExemption).toLocaleString()}</span></div>
                                    <div className="flex justify-between text-red-300"><span>(-) Others</span><span>-₹{finalOtherDeductions.toLocaleString()}</span></div>

                                    <div className="border-t border-dashed border-white/20 my-2 pt-2 flex justify-between font-bold text-white">
                                        <span>Net Taxable</span><span>₹{oldTax.taxable.toLocaleString()}</span>
                                    </div>
                                    <div className="flex justify-between mt-4"><span>Tax Calculated</span><span>₹{Math.round(oldTax.baseTax).toLocaleString()}</span></div>
                                    <div className="flex justify-between"><span>(+) Cess (4%)</span><span>₹{Math.round(oldTax.cess).toLocaleString()}</span></div>
                                </div>

                                <div className="border-t border-white/10 mt-4 pt-4">
                                    <div className="flex justify-between items-baseline mb-1">
                                        <span className="text-xs text-slate-400 font-bold">FINAL TAX:</span>
                                        <span className={`text-xl lg:text-2xl font-bold ${recommendedRegime === 'old' ? 'text-white' : 'text-slate-300'}`}>₹{Math.round(oldTax.totalTax).toLocaleString()}</span>
                                    </div>
                                    <div className="text-[10px] text-right text-slate-500">Effective Rate: {oldTax.rate.toFixed(1)}%</div>
                                </div>
                            </div>

                            {/* New Regime Card */}
                            <div
                                onClick={() => setTaxRegime('new')}
                                className={`rounded-2xl p-4 lg:p-5 border-2 transition-all cursor-pointer relative overflow-hidden flex flex-col ${taxRegime === 'new' ? 'bg-surface-active border-blue-500 shadow-[0_0_20px_rgba(59,130,246,0.15)]' : 'bg-surface-dark border-white/5 hover:border-white/20'}`}
                            >
                                {recommendedRegime === 'new' && (
                                    <div className="absolute top-0 right-0 bg-blue-500 text-white font-bold text-[10px] px-3 py-1 rounded-bl-lg">RECOMMENDED</div>
                                )}
                                <div className="flex items-center justify-between mb-4">
                                    <h4 className={`font-bold uppercase tracking-widest text-sm ${taxRegime === 'new' ? 'text-blue-400' : 'text-slate-400'}`}>NEW REGIME</h4>
                                    {taxRegime === 'new' && <CheckCircle2 className="w-5 h-5 text-blue-500" />}
                                </div>

                                <div className="space-y-2 font-mono text-[11px] lg:text-xs text-slate-300 flex-grow">
                                    <div className="flex justify-between"><span>Gross Income</span><span>₹{grossTotalIncome.toLocaleString()}</span></div>
                                    <div className="flex justify-between text-red-300"><span>(-) Std Ded.</span><span>-₹75,000</span></div>
                                    <div className="flex justify-between text-red-300 opacity-50"><span className="line-through">(-) 80C/80D/HRA</span><span>Not Allowed</span></div>

                                    <div className="border-t border-dashed border-white/20 my-2 pt-2 flex justify-between font-bold text-white mt-10">
                                        <span>Net Taxable</span><span>₹{newTax.taxable.toLocaleString()}</span>
                                    </div>
                                    <div className="flex justify-between mt-4"><span>Tax Calculated</span><span>₹{Math.round(newTax.baseTax).toLocaleString()}</span></div>
                                    <div className="flex justify-between"><span>(+) Cess (4%)</span><span>₹{Math.round(newTax.cess).toLocaleString()}</span></div>
                                </div>

                                <div className="border-t border-white/10 mt-4 pt-4">
                                    <div className="flex justify-between items-baseline mb-1">
                                        <span className="text-xs text-slate-400 font-bold">FINAL TAX:</span>
                                        <span className={`text-xl lg:text-2xl font-bold ${recommendedRegime === 'new' ? 'text-white' : 'text-slate-300'}`}>₹{Math.round(newTax.totalTax).toLocaleString()}</span>
                                    </div>
                                    <div className="text-[10px] text-right text-slate-500">Effective Rate: {newTax.rate.toFixed(1)}%</div>
                                </div>
                            </div>
                        </div>

                        {/* Recommendation Banner */}
                        <div className={`mt-6 rounded-2xl p-5 lg:p-6 border ${recommendedRegime === 'old' ? 'bg-primary/10 border-primary/30' : 'bg-blue-500/10 border-blue-500/30'}`}>
                            <h3 className={`font-bold text-lg mb-2 flex items-center gap-2 ${recommendedRegime === 'old' ? 'text-primary' : 'text-blue-400'}`}>
                                <Sparkles className="w-5 h-5" />
                                RECOMMENDATION: Choose {recommendedRegime === 'old' ? 'OLD' : 'NEW'} REGIME
                            </h3>
                            <p className="text-white text-base lg:text-lg mb-3">
                                You SAVE <span className="font-bold">₹{Math.round(savings).toLocaleString()}</span> by choosing {recommendedRegime === 'old' ? 'Old Regime over New Regime' : 'New Regime over Old Regime'}.
                            </p>
                            <p className="text-slate-400 text-sm flex items-start gap-2">
                                <span className="mt-0.5">💡</span>
                                {recommendedRegime === 'old'
                                    ? `With your high deductions (₹${((final80C + final80D + finalHraExemption + finalOtherDeductions) / 100000).toFixed(2)}L total), Old Regime is better. If you have fewer deductions next year, re-check.`
                                    : 'Since your planned deductions are lower, New Regime offers better tax savings due to its wider slabs and zero tax up to ₹7L.'}
                            </p>
                        </div>

                        {/* Upsell Cards */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-8">
                            <div className="bg-gradient-to-br from-surface-dark to-black border border-amber-500/30 rounded-xl p-5 relative overflow-hidden group cursor-pointer hover:border-amber-400/50 transition-all">
                                <div className="absolute -right-4 -top-4 w-16 h-16 bg-amber-500/10 rounded-full blur-xl group-hover:bg-amber-500/20 transition-all"></div>
                                <div className="bg-amber-500 text-black text-[10px] font-bold px-2 py-0.5 rounded w-fit mb-2 uppercase tracking-wide">Premium</div>
                                <h4 className="text-white font-bold text-sm mb-1">Tax Harvesting</h4>
                                <p className="text-xs text-slate-400">Save up to ₹10,000 extra by booking 1L LTCG tax-free annually.</p>
                            </div>
                            <div className="bg-gradient-to-br from-surface-dark to-black border border-purple-500/30 rounded-xl p-5 relative overflow-hidden group cursor-pointer hover:border-purple-400/50 transition-all">
                                <div className="absolute -right-4 -top-4 w-16 h-16 bg-purple-500/10 rounded-full blur-xl group-hover:bg-purple-500/20 transition-all"></div>
                                <div className="bg-purple-500 text-white text-[10px] font-bold px-2 py-0.5 rounded w-fit mb-2 uppercase tracking-wide">Premium</div>
                                <h4 className="text-white font-bold text-sm mb-1">Family Income Distribution</h4>
                                <p className="text-xs text-slate-400">Restructure investments among family members to stay in lower tax brackets.</p>
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
