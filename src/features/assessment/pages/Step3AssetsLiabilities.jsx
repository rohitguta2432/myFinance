import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, Wallet, CreditCard, Building, TrendingUp } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useBalanceSheetQuery, useAddAssetMutation, useAddLiabilityMutation } from '../hooks/useBalanceSheet';

const Step3AssetsLiabilities = () => {
    const navigate = useNavigate();
    const { assets, addAsset, removeAsset, liabilities, addLiability, removeLiability } = useAssessmentStore();

    // API Integration
    const { data: balanceData } = useBalanceSheetQuery();
    const { mutateAsync: addAssetApi } = useAddAssetMutation();
    const { mutateAsync: addLiabilityApi } = useAddLiabilityMutation();

    // Hydrate store from API
    useEffect(() => {
        if (balanceData) {
            if (balanceData.assets?.length) useAssessmentStore.setState({ assets: balanceData.assets });
            if (balanceData.liabilities?.length) useAssessmentStore.setState({ liabilities: balanceData.liabilities });
        }
    }, [balanceData]);

    const [activeTab, setActiveTab] = useState('assets'); // 'assets' or 'liabilities'
    const [isModalOpen, setIsModalOpen] = useState(false);

    // Form State
    const [category, setCategory] = useState('');
    const [name, setName] = useState('');
    const [amount, setAmount] = useState('');

    const openModal = () => {
        setCategory(activeTab === 'assets' ? 'Real Estate' : 'Home Loan');
        setName('');
        setAmount('');
        setIsModalOpen(true);
    };

    const handleSave = async () => {
        const newItem = {
            id: Date.now(),
            category,
            name: name || category,
            amount: parseFloat(amount) || 0,
        };

        if (activeTab === 'assets') {
            addAsset(newItem); // optimistic
            try { await addAssetApi(newItem); } catch (e) { console.warn('Asset API save failed:', e.message); }
        } else {
            addLiability(newItem); // optimistic
            try { await addLiabilityApi(newItem); } catch (e) { console.warn('Liability API save failed:', e.message); }
        }
        setIsModalOpen(false);
    };

    // Calculations
    const totalAssets = assets.reduce((sum, item) => sum + item.amount, 0);
    const totalLiabilities = liabilities.reduce((sum, item) => sum + item.amount, 0);
    const netWorth = totalAssets - totalLiabilities;

    return (
        <div className="flex flex-col h-full pb-32">
            {/* Net Worth Card */}
            <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-surface-dark to-black p-6 shadow-lg mb-6 border border-white/5">
                <div className="absolute -right-10 -top-10 h-32 w-32 rounded-full bg-primary/20 blur-3xl"></div>
                <div className="relative z-10 flex flex-col gap-1 text-center">
                    <p className="text-slate-400 text-sm font-medium tracking-wide uppercase">Total Net Worth</p>
                    <h1 className="text-white text-4xl font-bold tracking-tight py-2">₹ {(netWorth / 100000).toFixed(2)} Lakhs</h1>
                    <div className="flex items-center justify-center gap-4 mt-2">
                        <div className="text-xs">
                            <span className="text-slate-400 block">Assets</span>
                            <span className="text-primary font-bold">₹ {(totalAssets / 1000).toFixed(1)}k</span>
                        </div>
                        <div className="h-6 w-px bg-white/10"></div>
                        <div className="text-xs">
                            <span className="text-slate-400 block">Liabilities</span>
                            <span className="text-red-400 font-bold">₹ {(totalLiabilities / 1000).toFixed(1)}k</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Tabs */}
            <div className="bg-surface-active p-1 rounded-xl flex mb-6">
                <button
                    onClick={() => setActiveTab('assets')}
                    className={`flex-1 py-2.5 rounded-lg text-sm font-bold transition-all ${activeTab === 'assets' ? 'bg-background-dark text-primary shadow-sm' : 'text-slate-400 hover:text-white'}`}
                >
                    Assets
                </button>
                <button
                    onClick={() => setActiveTab('liabilities')}
                    className={`flex-1 py-2.5 rounded-lg text-sm font-bold transition-all ${activeTab === 'liabilities' ? 'bg-background-dark text-primary shadow-sm' : 'text-slate-400 hover:text-white'}`}
                >
                    Liabilities
                </button>
            </div>

            {/* List Content */}
            <div className="flex-1 space-y-4 overflow-y-auto pb-4">
                {(activeTab === 'assets' ? assets : liabilities).map((item) => (
                    <div key={item.id} className="flex justify-between items-center bg-surface-dark p-4 rounded-xl border border-white/5 shadow-sm">
                        <div className="flex items-center gap-3">
                            <div className={`p-2 rounded-lg ${activeTab === 'assets' ? 'bg-primary/10 text-primary' : 'bg-red-500/10 text-red-400'}`}>
                                {activeTab === 'assets' ? <TrendingUp className="w-5 h-5" /> : <CreditCard className="w-5 h-5" />}
                            </div>
                            <div>
                                <p className="font-bold text-sm text-white">{item.name}</p>
                                <p className="text-xs text-slate-400">{item.category}</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <span className="font-bold text-white">₹ {item.amount.toLocaleString()}</span>
                            <button onClick={() => activeTab === 'assets' ? removeAsset(item.id) : removeLiability(item.id)} className="text-slate-500 hover:text-red-400">
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                    </div>
                ))}

                <button
                    onClick={openModal}
                    className="w-full py-4 border border-dashed border-white/10 rounded-xl flex items-center justify-center gap-2 text-slate-400 font-medium hover:bg-surface-dark hover:border-primary/30 transition-colors"
                >
                    <Plus className="w-5 h-5" />
                    Add {activeTab === 'assets' ? 'Asset' : 'Liability'}
                </button>
            </div>

            {/* Footer */}
            <div className="fixed bottom-0 left-0 w-full bg-gradient-to-t from-background-dark via-background-dark to-transparent pt-10 pb-6 px-4 z-40 max-w-4xl mx-auto right-0">
                <button
                    onClick={() => navigate('/assessment/step-4')}
                    className="w-full bg-primary text-background-dark font-bold text-base py-4 rounded-xl flex items-center justify-center gap-2 hover:bg-primary-dark transition-colors shadow-[0_0_20px_rgba(13,242,89,0.3)] active:scale-[0.98]"
                >
                    Next: Financial Goals
                    <ArrowRight className="w-5 h-5" />
                </button>
            </div>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={() => setIsModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-lg rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl border-t border-white/10 animate-slide-up">
                        <div className="w-12 h-1 bg-surface-active rounded-full mx-auto mb-6 sm:hidden"></div>
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-white">Add {activeTab === 'assets' ? 'Asset' : 'Liability'}</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Category</label>
                                <select
                                    value={category}
                                    onChange={(e) => setCategory(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                >
                                    {activeTab === 'assets' ? (
                                        <>
                                            <option>Real Estate</option>
                                            <option>Stocks / Mutual Funds</option>
                                            <option>Gold</option>
                                            <option>Fixed Deposits</option>
                                            <option>Crypto</option>
                                            <option>Cash / Bank</option>
                                        </>
                                    ) : (
                                        <>
                                            <option>Home Loan</option>
                                            <option>Car Loan</option>
                                            <option>Personal Loan</option>
                                            <option>Credit Card Debt</option>
                                            <option>Other Debt</option>
                                        </>
                                    )}
                                </select>
                            </div>

                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Description / Name</label>
                                <input
                                    type="text"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                    placeholder={activeTab === 'assets' ? "e.g., Apartment in Mumbai" : "e.g., HDFC Home Loan"}
                                />
                            </div>

                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Current Value (₹)</label>
                                <input
                                    type="number"
                                    value={amount}
                                    onChange={(e) => setAmount(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-lg text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                    placeholder="0"
                                />
                            </div>

                            <button
                                onClick={handleSave}
                                className="w-full bg-primary hover:bg-primary-dark text-background-dark font-bold py-4 rounded-xl mt-4 shadow-[0_0_15px_rgba(13,242,89,0.3)] active:scale-[0.98] transition-all"
                            >
                                Save {activeTab === 'assets' ? 'Asset' : 'Liability'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Step3AssetsLiabilities;
