import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, ChevronDown, Check, TrendingUp, TrendingDown, DollarSign, Loader2 } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useFinancialsQuery, useAddIncomeMutation, useAddExpenseMutation } from '../hooks/useFinancials';

const Step2IncomeExpenses = () => {
    const navigate = useNavigate();
    const { incomes, addIncome, removeIncome, expenses, addExpense, removeExpense } = useAssessmentStore();

    // API Integration
    const { data: financialsData } = useFinancialsQuery();
    const { mutateAsync: addIncomeApi } = useAddIncomeMutation();
    const { mutateAsync: addExpenseApi } = useAddExpenseMutation();

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

    const openModal = (type) => {
        setModalType(type);
        setCategory(type === 'income' ? 'Salary' : 'Groceries');
        setAmount('');
        setFrequency('Monthly');
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
            const incomeItem = { ...newItem, source: category };
            addIncome(incomeItem); // optimistic local update
            try { await addIncomeApi(incomeItem); } catch (e) { console.warn('Income API save failed:', e.message); }
        } else {
            addExpense(newItem); // optimistic local update
            try { await addExpenseApi(newItem); } catch (e) { console.warn('Expense API save failed:', e.message); }
        }
        setIsModalOpen(false);
    };

    // Derived Visuals
    const totalMonthlyIncome = incomes.reduce((sum, item) => sum + (item.frequency === 'Monthly' ? item.amount : item.amount / 12), 0);
    const totalMonthlyExpenses = expenses.reduce((sum, item) => sum + (item.frequency === 'Monthly' ? item.amount : item.amount / 12), 0);
    const surplus = totalMonthlyIncome - totalMonthlyExpenses;
    const savingsRate = totalMonthlyIncome > 0 ? Math.round((surplus / totalMonthlyIncome) * 100) : 0;

    return (
        <div className="flex flex-col h-full pb-32">
            {/* Main Content */}
            <div className="flex-1 space-y-4 overflow-y-auto pb-4">
                {/* List Incomes */}
                {incomes.map((inc) => (
                    <div key={inc.id} className="flex justify-between items-center bg-surface-dark p-4 rounded-xl border border-white/5 shadow-sm">
                        <div className="flex items-center gap-3">
                            <div className="bg-primary/10 p-2 rounded-lg text-primary">
                                <DollarSign className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="font-bold text-sm text-white">{inc.source}</p>
                                <p className="text-xs text-slate-400">{inc.frequency} • ₹ {inc.amount.toLocaleString()}</p>
                            </div>
                        </div>
                        <button onClick={() => removeIncome(inc.id)} className="text-red-400 hover:text-red-500">
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
                        <button onClick={() => removeExpense(exp.id)} className="text-red-400 hover:text-red-500">
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
            </div>

            {/* Summary Footer */}
            <div className="fixed bottom-0 left-0 w-full bg-gradient-to-t from-background-dark via-background-dark to-transparent pt-6 z-40">
                <div className="bg-surface-dark border-t border-white/5 p-5 rounded-t-3xl max-w-md mx-auto right-0 left-0">
                    <div className="flex flex-col gap-4">
                        <div className="flex justify-between items-end border-b border-white/10 pb-4">
                            <div className="flex flex-col gap-1">
                                <span className="text-slate-400 text-xs font-medium uppercase tracking-wide">Monthly Cash Flow</span>
                                <div className="flex gap-4 text-xs text-slate-300">
                                    <span className="flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-primary"></span> In: ₹{(totalMonthlyIncome / 1000).toFixed(1)}k</span>
                                    <span className="flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-red-400"></span> Out: ₹{(totalMonthlyExpenses / 1000).toFixed(1)}k</span>
                                </div>
                            </div>
                            <div className="text-right">
                                <span className="text-xs text-slate-400 block mb-0.5">Surplus</span>
                                <span className="text-primary text-xl font-bold">₹ {surplus.toLocaleString()}</span>
                            </div>
                        </div>

                        {savingsRate > 20 && (
                            <div className="bg-primary/10 border border-primary/20 rounded-lg p-3 flex gap-3 items-start">
                                <Check className="text-primary w-5 h-5 mt-0.5" />
                                <p className="text-sm text-slate-200 leading-snug">
                                    <span className="text-primary font-bold">Good Job!</span> You're saving <span className="text-white font-bold">{savingsRate}%</span> of your income.
                                </p>
                            </div>
                        )}

                        <button
                            onClick={() => navigate('/assessment/step-3')}
                            className="w-full bg-primary text-background-dark font-bold text-base py-4 rounded-xl flex items-center justify-center gap-2 hover:bg-primary-dark transition-colors shadow-[0_0_20px_rgba(13,242,89,0.3)] active:scale-[0.98]"
                        >
                            Next: Assets & Liabilities
                            <ArrowRight className="w-5 h-5" />
                        </button>
                    </div>
                </div>
            </div>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={() => setIsModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-md rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl border-t border-white/10 animate-slide-up">
                        <div className="w-12 h-1 bg-surface-active rounded-full mx-auto mb-6 sm:hidden"></div>
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-white">Add {modalType === 'income' ? 'Income' : 'Expense'}</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">{modalType === 'income' ? 'Source' : 'Category'}</label>
                                <select
                                    value={category}
                                    onChange={(e) => setCategory(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                >
                                    {modalType === 'income' ? (
                                        <>
                                            <option>Salary</option>
                                            <option>Business</option>
                                            <option>Rental</option>
                                            <option>Other</option>
                                        </>
                                    ) : (
                                        <>
                                            <option>Groceries</option>
                                            <option>Rent/EMI</option>
                                            <option>Utilities</option>
                                            <option>Transport</option>
                                            <option>Entertainment</option>
                                            <option>Shopping</option>
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
