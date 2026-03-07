import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, ChevronDown, Check, CheckCircle2, TrendingUp, TrendingDown, DollarSign, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useFinancialsQuery, useAddIncomeMutation, useAddExpenseMutation, useDeleteIncomeMutation, useDeleteExpenseMutation } from '../hooks/useFinancials';

const Step2IncomeExpenses = () => {
    const navigate = useNavigate();
    const { incomes, addIncome, removeIncome, expenses, addExpense, removeExpense } = useAssessmentStore();

    // API Integration
    const { data: financialsData } = useFinancialsQuery();
    const { mutateAsync: addIncomeApi } = useAddIncomeMutation();
    const { mutateAsync: addExpenseApi } = useAddExpenseMutation();
    const { mutateAsync: deleteIncomeApi, isPending: isDeletingIncome } = useDeleteIncomeMutation();
    const { mutateAsync: deleteExpenseApi, isPending: isDeletingExpense } = useDeleteExpenseMutation();

    // Hydrate store from API
    useEffect(() => {
        if (financialsData) {
            if (financialsData.incomes?.length) {
                useAssessmentStore.setState({ incomes: financialsData.incomes });
            }
            if (financialsData.expenses?.length) {
                useAssessmentStore.setState({ expenses: financialsData.expenses });
            }
        }
    }, [financialsData]);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalType, setModalType] = useState('expense'); // 'income' or 'expense'

    // Form State
    const [category, setCategory] = useState('');
    const [amount, setAmount] = useState('');
    const [frequency, setFrequency] = useState('Monthly');
    const [isEssential, setIsEssential] = useState(true);
    const [taxDeducted, setTaxDeducted] = useState(false);
    const [tdsPercentage, setTdsPercentage] = useState(10);

    const openModal = (type) => {
        setModalType(type);
        setCategory(type === 'income' ? 'Salary' : 'Rent/Mortgage');
        setAmount('');
        setFrequency('Monthly');
        setTaxDeducted(false);
        setTdsPercentage(10);
        setIsModalOpen(true);
    };

    const handleSave = async () => {
        const newItem = {
            id: Date.now(),
            category,
            amount: parseFloat(amount) || 0,
            frequency,
            type: isEssential ? 'Essential' : 'Discretionary'
        };

        if (modalType === 'income') {
            const incomeItem = {
                ...newItem,
                source: category,
                taxDeducted,
                tdsPercentage: taxDeducted ? parseFloat(tdsPercentage) || 0 : 0
            };
            addIncome(incomeItem); // optimistic local update
            try { await addIncomeApi(incomeItem); } catch (e) { console.warn('Income API save failed:', e.message); }
        } else {
            addExpense(newItem); // optimistic local update
            try { await addExpenseApi(newItem); } catch (e) { console.warn('Expense API save failed:', e.message); }
        }
        setIsModalOpen(false);
    };

    // Derived Visuals
    const calculateMonthly = (item) => {
        if (item.frequency === 'Monthly') return item.amount;
        if (item.frequency === 'Quarterly') return item.amount / 3;
        if (item.frequency === 'Yearly') return item.amount / 12;
        return item.amount / 12; // One-time amortized
    };

    const totalMonthlyIncome = incomes.reduce((sum, item) => sum + calculateMonthly(item), 0);
    const totalMonthlyExpenses = expenses.reduce((sum, item) => sum + calculateMonthly(item), 0);
    const totalMonthlyEMIs = expenses
        .filter(exp => exp.category === 'EMIs (loan payments)' || exp.category.toUpperCase().includes('EMI'))
        .reduce((sum, item) => sum + calculateMonthly(item), 0);

    // We want the primary expenses listed as (Total Expenses - EMIs) in the card since it breaks out EMIs specifically.
    const nonEMIExpenses = totalMonthlyExpenses - totalMonthlyEMIs;

    const discretionaryExpensesList = expenses.filter(exp => exp.type === 'Discretionary');
    const totalDiscretionary = discretionaryExpensesList.reduce((sum, item) => sum + calculateMonthly(item), 0);

    const surplus = totalMonthlyIncome - totalMonthlyExpenses;
    const savingsRate = totalMonthlyIncome > 0 ? Math.round((surplus / totalMonthlyIncome) * 100) : 0;

    // Calculate hypothetical metrics if discretionary is reduced by 30%
    const hypotheticalTotalExpenses = totalMonthlyExpenses - (totalDiscretionary * 0.30);
    const hypotheticalSurplus = totalMonthlyIncome - hypotheticalTotalExpenses;
    const hypotheticalSavingsRate = totalMonthlyIncome > 0 ? Math.round((hypotheticalSurplus / totalMonthlyIncome) * 100) : 0;

    return (
        <div className="flex flex-col h-full pb-32">
            {/* Main Content */}
            <div className="flex-1 space-y-4 overflow-y-auto pb-4">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold tracking-tight text-white mb-2">Your Cash Flow Reality Check</h1>
                    <p className="text-slate-400 text-sm">Understanding your money flow is step one to controlling it.</p>
                </div>
                {/* List Incomes */}
                {incomes.map((inc) => (
                    <div key={inc.id} className="flex justify-between items-center bg-surface-dark p-4 rounded-xl border border-white/5 shadow-sm">
                        <div className="flex items-center gap-3">
                            <div className="bg-primary/10 p-2 rounded-lg text-primary">
                                <DollarSign className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="font-bold text-sm text-white">{inc.source}</p>
                                <p className="text-xs text-slate-400">
                                    {inc.frequency} • ₹ {inc.amount.toLocaleString()}
                                    {inc.taxDeducted && <span className="ml-2 text-primary">({inc.tdsPercentage}% TDS)</span>}
                                </p>
                            </div>
                        </div>
                        <button
                            onClick={async () => {
                                removeIncome(inc.id);
                                try { await deleteIncomeApi(inc.id); } catch (e) { console.warn('Delete API failed', e); }
                            }}
                            disabled={isDeletingIncome}
                            className="text-red-400 hover:text-red-500 disabled:opacity-50"
                        >
                            <X className="w-4 h-4" />
                        </button>
                    </div>
                ))}

                <button
                    onClick={() => openModal('income')}
                    className="w-full py-3 border border-dashed border-white/10 rounded-xl flex items-center justify-center gap-2 text-slate-400 font-medium hover:bg-surface-dark hover:border-primary/30 transition-colors"
                >
                    <Plus className="w-5 h-5" />
                    Add Income Source
                </button>

                {/* List Expenses */}
                {expenses.map((exp) => (
                    <div key={exp.id} className="flex justify-between items-center bg-surface-dark p-4 rounded-xl border border-white/5 shadow-sm">
                        <div className="flex items-center gap-3">
                            <div className="bg-red-500/10 p-2 rounded-lg text-red-400">
                                <TrendingDown className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="font-bold text-sm text-white">{exp.category} <span className="text-[10px] font-normal bg-surface-active px-1.5 py-0.5 rounded ml-1 text-slate-400">{exp.type}</span></p>
                                <p className="text-xs text-slate-400">{exp.frequency} • ₹ {exp.amount.toLocaleString()}</p>
                            </div>
                        </div>
                        <button
                            onClick={async () => {
                                removeExpense(exp.id);
                                try { await deleteExpenseApi(exp.id); } catch (e) { console.warn('Delete API failed', e); }
                            }}
                            disabled={isDeletingExpense}
                            className="text-red-400 hover:text-red-500 disabled:opacity-50"
                        >
                            <X className="w-4 h-4" />
                        </button>
                    </div>
                ))}

                <button
                    onClick={() => openModal('expense')}
                    className="w-full py-3 border border-dashed border-white/10 rounded-xl flex items-center justify-center gap-2 text-slate-400 font-medium hover:bg-surface-dark hover:border-primary/30 transition-colors"
                >
                    <Plus className="w-5 h-5" />
                    Add Expense
                </button>

                {/* Cash Flow Reality Check Card */}
                {incomes.length > 0 && expenses.length > 0 && (
                    <div className="mt-8 pt-8 border-t border-white/10 animate-fade-in">
                        <div className="bg-gradient-to-br from-surface-dark to-surface border border-white/10 rounded-2xl p-6 shadow-2xl relative overflow-hidden">
                            {/* Decorative Top Bar */}
                            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary to-blue-500"></div>

                            <div className="flex items-center justify-center gap-3 mb-6 opacity-80">
                                <div className="h-[1px] flex-1 bg-gradient-to-r from-transparent to-white/20"></div>
                                <h3 className="text-center font-semibold text-xs tracking-[0.2em] text-slate-300 uppercase">
                                    Your Monthly Cash Flow
                                </h3>
                                <div className="h-[1px] flex-1 bg-gradient-to-l from-transparent to-white/20"></div>
                            </div>

                            <div className="space-y-3 font-mono text-sm sm:text-base">
                                <div className="flex justify-between items-center">
                                    <span className="text-slate-300">Income</span>
                                    <span className="text-primary font-bold">₹{totalMonthlyIncome.toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-slate-300">Expenses</span>
                                    <span className="text-red-400">₹{nonEMIExpenses.toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
                                </div>
                                <div className="flex justify-between items-center text-slate-400">
                                    <span>EMIs (loans)</span>
                                    <span>₹{totalMonthlyEMIs.toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
                                </div>

                                <div className="border-t border-dashed border-white/20 my-3 pt-3 flex justify-between items-center text-lg">
                                    <span className="text-white font-bold">SURPLUS</span>
                                    <span className="text-white font-bold">₹{surplus.toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
                                </div>

                                <div className="pt-2 flex justify-between items-center">
                                    <span className="text-slate-300">Savings Rate:</span>
                                    <span className={`font-bold ${savingsRate >= 20 ? 'text-primary' : 'text-red-400'}`}>{savingsRate}%</span>
                                </div>
                            </div>

                            <div className="mt-6">
                                {savingsRate >= 20 ? (
                                    <div className="bg-primary/10 border border-primary/20 rounded-xl p-4 flex items-start gap-3">
                                        <CheckCircle2 className="w-5 h-5 text-primary shrink-0 mt-0.5" />
                                        <p className="text-sm text-primary font-medium">
                                            Excellent! You're saving more than 70% of Indians.
                                        </p>
                                    </div>
                                ) : (
                                    <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-4 space-y-3">
                                        <div className="flex gap-3">
                                            <span className="text-xl shrink-0 mt-0.5">🚨</span>
                                            <p className="text-sm text-red-200">
                                                <span className="font-bold text-red-400">Your savings rate is below the recommended 20%.</span>
                                            </p>
                                        </div>

                                        {discretionaryExpensesList.length > 0 && (
                                            <div className="pl-9 space-y-2">
                                                <p className="text-xs text-slate-400">Review non-essential expenses like:</p>
                                                <div className="space-y-1">
                                                    {discretionaryExpensesList.slice(0, 3).map(exp => (
                                                        <div key={exp.id} className="flex justify-between text-xs font-mono">
                                                            <span className="text-slate-300 truncate pr-2">{exp.category}:</span>
                                                            <span className="text-slate-400 shrink-0">₹{calculateMonthly(exp).toLocaleString(undefined, { maximumFractionDigits: 0 })}/mo</span>
                                                        </div>
                                                    ))}
                                                </div>
                                                <p className="text-xs font-medium text-white/80 pt-2 border-t border-red-500/20">
                                                    Reducing these by 30% would boost savings to <span className="text-primary font-bold">{hypotheticalSavingsRate}%</span>.
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* Bottom Navigation */}
            <div className="fixed bottom-0 left-0 right-0 bg-background-dark/80 backdrop-blur-lg border-t border-white/5 p-4 z-50">
                <div className="max-w-[1200px] mx-auto flex items-center justify-between">
                    <button
                        onClick={() => navigate('/assessment/step-1')}
                        className="px-6 py-3 bg-surface-dark hover:bg-surface-active text-white font-bold text-sm rounded-xl transition-all"
                    >
                        Back
                    </button>
                    <div className="flex items-center gap-3">
                        <div className="hidden sm:flex items-center gap-2 px-4 py-3 bg-surface-dark border border-white/10 rounded-xl">
                            <CheckCircle2 className="w-4 h-4 text-primary" />
                            <span className="text-sm font-semibold text-slate-400">Step 2/6</span>
                        </div>
                        <button
                            onClick={() => {
                                if (totalMonthlyIncome === 0 && totalMonthlyExpenses === 0) {
                                    toast.error('Add at least one income source and one expense to continue', { id: 'step2-guide' });
                                    return;
                                }
                                if (totalMonthlyIncome === 0) {
                                    toast.error('Add your income sources — salary, freelance, or investments', { id: 'step2-guide' });
                                    return;
                                }
                                if (totalMonthlyExpenses === 0) {
                                    toast('💡 Tip: Add your expenses for a better financial picture', { id: 'step2-guide' });
                                }
                                navigate('/assessment/step-3');
                            }}
                            className="px-6 py-3 bg-primary hover:bg-primary-dark active:scale-[0.98] text-background-dark font-bold text-sm rounded-xl flex items-center gap-2 transition-all shadow-[0_0_15px_rgba(13,242,89,0.25)]"
                        >
                            Next <ArrowRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={() => setIsModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-lg rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl border-t border-white/10 animate-slide-up">
                        <div className="w-12 h-1 bg-surface-active rounded-full mx-auto mb-6 sm:hidden"></div>
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-white">Add {modalType === 'income' ? 'Income' : 'Expense'}</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">{modalType === 'income' ? 'Type of Income' : 'Category'}</label>
                                <select
                                    value={category}
                                    onChange={(e) => setCategory(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                >
                                    {modalType === 'income' ? (
                                        <>
                                            <option>Salary</option>
                                            <option>Business Income</option>
                                            <option>Agriculture Income</option>
                                            <option>Freelancing</option>
                                            <option>Rental Income</option>
                                            <option>Dividend Income</option>
                                            <option>Interest Income</option>
                                            <option>Bonus/Incentive</option>
                                            <option>Capital Gains (from selling stocks/property)</option>
                                            <option>Pension</option>
                                            <option>Other</option>
                                        </>
                                    ) : (
                                        <>
                                            <option>Rent/Mortgage</option>
                                            <option>EMIs (loan payments)</option>
                                            <option>Utilities (electricity, water, gas)</option>
                                            <option>Transportation</option>
                                            <option>Food & Groceries</option>
                                            <option>Shopping (clothes, personal care)</option>
                                            <option>Entertainment (movies, dining out)</option>
                                            <option>Subscriptions (Netflix, gym, etc.)</option>
                                            <option>Healthcare</option>
                                            <option>Education</option>
                                            <option>Insurance Premiums</option>
                                            <option>Childcare</option>
                                            <option>Pet Care</option>
                                            <option>Travel & Vacations</option>
                                            <option>Gifts & Donations</option>
                                            <option>Other</option>
                                        </>
                                    )}
                                </select>
                            </div>

                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Amount (₹)</label>
                                <input
                                    type="number"
                                    value={amount}
                                    onChange={(e) => setAmount(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-lg text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                    placeholder="0"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Frequency</label>
                                    <select
                                        value={frequency}
                                        onChange={(e) => setFrequency(e.target.value)}
                                        className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                    >
                                        <option>Monthly</option>
                                        <option>Quarterly</option>
                                        <option>Yearly</option>
                                        <option>One-time</option>
                                    </select>
                                </div>
                                {modalType === 'expense' && (
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Type</label>
                                        <div
                                            onClick={() => setIsEssential(!isEssential)}
                                            className={`h-[46px] flex items-center justify-between px-3 rounded-lg border cursor-pointer transition-colors ${isEssential ? 'bg-primary/10 border-primary/20' : 'bg-background-dark border-white/10'}`}
                                        >
                                            <span className={`text-sm ${isEssential ? 'text-primary font-medium' : 'text-slate-400'}`}>{isEssential ? 'Essential' : 'Discretionary'}</span>
                                            <div className={`w-10 h-5 rounded-full p-0.5 flex transition-colors ${isEssential ? 'bg-primary' : 'bg-surface-active'}`}>
                                                <div className={`w-4 h-4 rounded-full bg-white shadow-sm transition-transform ${isEssential ? 'translate-x-full' : ''}`}></div>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {modalType === 'income' && (
                                <div className="p-4 bg-primary/5 border border-primary/20 rounded-xl space-y-4">
                                    <div className="flex items-center justify-between">
                                        <label className="text-sm font-medium text-white">Tax Already Deducted?</label>
                                        <div
                                            onClick={() => setTaxDeducted(!taxDeducted)}
                                            className={`w-12 h-6 rounded-full p-1 flex items-center cursor-pointer transition-colors ${taxDeducted ? 'bg-primary' : 'bg-surface-active'}`}
                                        >
                                            <div className={`w-4 h-4 rounded-full bg-white shadow-sm transition-transform ${taxDeducted ? 'translate-x-6' : ''}`}></div>
                                        </div>
                                    </div>
                                    {taxDeducted && (
                                        <div className="animate-fade-in">
                                            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">TDS Percentage (%)</label>
                                            <div className="relative">
                                                <input
                                                    type="number"
                                                    value={tdsPercentage}
                                                    onChange={(e) => setTdsPercentage(e.target.value)}
                                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 pr-10 font-bold text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                                    placeholder="10"
                                                    min="0"
                                                    max="100"
                                                    step="0.1"
                                                />
                                                <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-slate-400 font-bold">%</div>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}

                            <button
                                onClick={handleSave}
                                className="w-full bg-primary hover:bg-primary-dark text-background-dark font-bold py-4 rounded-xl mt-4 shadow-[0_0_15px_rgba(13,242,89,0.3)] active:scale-[0.98] transition-all"
                            >
                                Save {modalType === 'income' ? 'Income' : 'Expense'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Step2IncomeExpenses;
