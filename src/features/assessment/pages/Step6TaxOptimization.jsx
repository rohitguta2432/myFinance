import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Wallet, Users, GraduationCap, CheckCircle, AlertTriangle, Loader2 } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useTaxQuery, useTaxMutation } from '../hooks/useTax';

const Step6TaxOptimization = () => {
    const navigate = useNavigate();
    const {
        incomes,
        investments80C,
        setInvestments80C,
        taxRegime,
        setTaxRegime
    } = useAssessmentStore();

    // API Integration
    const { data: taxData } = useTaxQuery();
    const { mutateAsync: saveTaxApi, isPending: isSaving } = useTaxMutation();

    useEffect(() => {
        if (taxData) {
            if (taxData.taxRegime) setTaxRegime(taxData.taxRegime);
            if (taxData.investments80C !== undefined) setInvestments80C(taxData.investments80C);
        }
    }, [taxData]);

    const handleComplete = async () => {
        try {
            await saveTaxApi({ taxRegime, investments80C, epf, licPpf });
        } catch (err) {
            console.warn('Tax API save failed:', err.message);
        }
        navigate('/assessment/complete');
    };

    // Local state for 80C breakdown
    const [epf, setEpf] = useState(60000);
    const [licPpf, setLicPpf] = useState(50000);

    // Sync total to store
    useEffect(() => {
        setInvestments80C(epf + licPpf);
    }, [epf, licPpf, setInvestments80C]);

    // Calculate Tax
    const annualIncome = incomes.reduce((sum, item) => sum + (item.frequency === 'Monthly' ? item.amount * 12 : item.amount), 0);

    const calculateTax = (income, regime) => {
        let taxable = income;
        let tax = 0;

        if (regime === 'old') {
            const stdDeduction = 50000;
            const deduction80C = Math.min(investments80C, 150000);
            taxable = Math.max(0, income - stdDeduction - deduction80C);

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
            const stdDeduction = 75000;
            taxable = Math.max(0, income - stdDeduction);

            if (taxable > 2400000) {
                tax += (taxable - 2400000) * 0.30;
                tax += 125000 + 100000;
            } else if (taxable > 1500000) {
                tax += (taxable - 1500000) * 0.30;
            } else if (taxable > 1200000) {
                tax = taxable * 0.15;
            } else if (taxable > 700000) {
                tax = (taxable - 300000) * 0.10;
            }
            tax = taxable * 0.15;
        }
        return Math.round(tax);
    };

    const taxOld = calculateTax(annualIncome, 'old');
    const taxNew = calculateTax(annualIncome, 'new');

    const recommendedRegime = taxOld < taxNew ? 'old' : 'new';
    const savings = Math.abs(taxOld - taxNew);

    // 80C Limit
    const limit80C = 150000;
    const current80C = Math.min(investments80C, limit80C);
    const unused80C = limit80C - current80C;
    const progress80C = (current80C / limit80C) * 100;

    return (
        <div className="flex flex-col h-full pb-32">

            <div className="flex-1 space-y-6 overflow-y-auto pb-4">

                {/* 80C + Inputs and Comparison Grid */}
                <div className="lg:grid lg:grid-cols-2 lg:gap-6 space-y-6 lg:space-y-0">

                    {/* Left Column: 80C Card + Alert + Inputs */}
                    <div className="space-y-6">
                        {/* 80C Card */}
                        <div className="bg-surface-dark rounded-2xl p-6 relative overflow-hidden shadow-lg border border-white/5">
                            <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 rounded-full blur-2xl -mr-10 -mt-10"></div>

                            <div className="flex justify-between items-end mb-4 relative z-10">
                                <div>
                                    <span className="text-slate-400 text-sm font-medium">80C Deductions</span>
                                    <div className="flex items-baseline gap-1 mt-1">
                                        <span className="text-2xl font-bold text-white">₹ {current80C.toLocaleString()}</span>
                                        <span className="text-sm text-slate-500">/ 1.5L</span>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <div className="text-primary font-bold text-lg">{Math.round(progress80C)}%</div>
                                </div>
                            </div>

                            <div className="w-full bg-background-dark rounded-full h-2">
                                <div
                                    className="bg-primary h-2 rounded-full shadow-[0_0_10px_rgba(34,197,94,0.5)] transition-all duration-1000"
                                    style={{ width: `${progress80C}%` }}
                                ></div>
                            </div>
                        </div>

                        {/* Alert */}
                        {unused80C > 0 && (
                            <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-4 flex gap-3 items-start">
                                <AlertTriangle className="text-yellow-500 w-5 h-5 flex-shrink-0 mt-0.5" />
                                <div>
                                    <span className="text-yellow-400 font-bold text-sm block">₹ {unused80C.toLocaleString()} unused limit!</span>
                                    <p className="text-yellow-400/70 text-xs">Invest by March 31 to save more tax.</p>
                                </div>
                            </div>
                        )}

                        {/* Inputs */}
                        <div className="bg-surface-dark rounded-2xl border border-white/5 divide-y divide-white/5">
                            <div className="p-4 flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className="bg-blue-500/10 p-2 rounded-lg text-blue-400">
                                        <Wallet className="w-5 h-5" />
                                    </div>
                                    <div>
                                        <p className="font-bold text-sm text-white">EPF / VPF</p>
                                        <p className="text-xs text-slate-500">Auto-fetched</p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-1">
                                    <span className="text-slate-500 text-sm">₹</span>
                                    <input
                                        type="number"
                                        value={epf}
                                        onChange={(e) => setEpf(parseFloat(e.target.value) || 0)}
                                        className="w-20 text-right font-bold text-white bg-transparent border-none focus:ring-0 p-0"
                                    />
                                </div>
                            </div>

                            <div className="p-4 flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className="bg-purple-500/10 p-2 rounded-lg text-purple-400">
                                        <Users className="w-5 h-5" />
                                    </div>
                                    <div>
                                        <p className="font-bold text-sm text-white">LIC / PPF / ELSS</p>
                                        <p className="text-xs text-slate-500">Investments</p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-1">
                                    <span className="text-slate-500 text-sm">₹</span>
                                    <input
                                        type="number"
                                        value={licPpf}
                                        onChange={(e) => setLicPpf(parseFloat(e.target.value) || 0)}
                                        className="w-20 text-right font-bold text-white bg-transparent border-none focus:ring-0 p-0"
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Right Column: Comparison */}
                    <div>
                        <h3 className="font-bold text-white mb-3">Regime Comparison</h3>
                        <div className="grid grid-cols-2 gap-4">
                            <div className={`rounded-xl p-4 border-2 transition-all ${recommendedRegime === 'old' ? 'bg-surface-active border-primary shadow-lg' : 'bg-surface-dark border-white/5 opacity-50'}`}>
                                {recommendedRegime === 'old' && <div className="text-[10px] bg-primary text-black font-bold px-2 py-0.5 rounded-full w-fit mb-2">WINNER</div>}
                                <p className={`text-xs font-bold uppercase tracking-wide ${recommendedRegime === 'old' ? 'text-slate-400' : 'text-slate-500'}`}>Old Regime</p>
                                <p className={`text-xl font-bold mt-1 text-white`}>₹ {taxOld.toLocaleString()}</p>
                                {recommendedRegime === 'old' && <p className="text-primary text-xs font-bold mt-2">Save ₹ {savings.toLocaleString()}</p>}
                            </div>

                            <div className={`rounded-xl p-4 border-2 transition-all ${recommendedRegime === 'new' ? 'bg-surface-active border-primary shadow-lg' : 'bg-surface-dark border-white/5 opacity-50'}`}>
                                {recommendedRegime === 'new' && <div className="text-[10px] bg-primary text-black font-bold px-2 py-0.5 rounded-full w-fit mb-2">WINNER</div>}
                                <p className={`text-xs font-bold uppercase tracking-wide ${recommendedRegime === 'new' ? 'text-slate-400' : 'text-slate-500'}`}>New Regime</p>
                                <p className={`text-xl font-bold mt-1 text-white`}>₹ {taxNew.toLocaleString()}</p>
                                {recommendedRegime === 'new' && <p className="text-primary text-xs font-bold mt-2">Save ₹ {savings.toLocaleString()}</p>}
                            </div>
                        </div>
                    </div>

                </div>

            </div>

            {/* Footer */}
            <div className="fixed bottom-0 left-0 w-full bg-surface-dark border-t border-white/10 p-5 z-40 rounded-t-3xl">
                <div className="max-w-4xl mx-auto">
                    <button
                        onClick={handleComplete}
                        disabled={isSaving}
                        className="w-full bg-primary text-background-dark font-bold text-base py-4 rounded-xl flex items-center justify-center gap-2 hover:bg-primary-dark transition-colors shadow-[0_0_20px_rgba(13,242,89,0.3)] active:scale-[0.98] disabled:opacity-60"
                    >
                        {isSaving ? (
                            <><Loader2 className="w-5 h-5 animate-spin" /> Saving...</>
                        ) : (
                            <>Complete Assessment <ArrowRight className="w-5 h-5" /></>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Step6TaxOptimization;
