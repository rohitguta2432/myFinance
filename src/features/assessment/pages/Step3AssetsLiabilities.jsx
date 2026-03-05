import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, Wallet, CreditCard, Building, TrendingUp, CheckCircle2 } from 'lucide-react';
import toast from 'react-hot-toast';
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
    const [subCategory, setSubCategory] = useState('');
    const [name, setName] = useState('');
    const [amount, setAmount] = useState('');

    // Asset Specific State
    const [purchaseValue, setPurchaseValue] = useState('');
    const [timeHorizon, setTimeHorizon] = useState('Short (0-2 years)');
    const [liquidity, setLiquidity] = useState('Immediate (instant access like savings account)');

    const assetCategories = {
        'Savings & Investments': [
            '🏦 Bank/Savings Account',
            '📊 Fixed Deposit (FD)',
            '💰 Recurring Deposit (RD)',
            '🏢 EPF (Provident Fund)',
            '📈 PPF (Public Provident Fund)',
            '🎯 NPS (National Pension System)',
            '📊 Mutual Funds — Equity',
            '📉 Mutual Funds — Debt',
            '📊 Mutual Funds — Hybrid',
            '📈 Stocks/Shares',
            '📄 Bonds/Debentures',
            '🏢REITs/InvITs'
        ],
        'Real Assets': [
            '🪙 Gold (Physical jewelry/bars)',
            '💎 Gold/ Silver (Digital/Sovereign Gold Bonds)',
            '⚪ Silver',
            '🏠 Real Estate (Residential)',
            '🏢 Real Estate (Commercial)',
            '🚗 Vehicle'
        ],
        'Alternative Investments': [
            '₿ Cryptocurrency',
            '💼 Business Equity',
            '📜 ESOPs (Employee Stock Options)',
            '🤝 P2P Lending',
            '🚀 Startup/Angel Investments',
            '➕ Other'
        ]
    };

    const openModal = () => {
        if (activeTab === 'assets') {
            setCategory('Savings & Investments');
            setSubCategory(assetCategories['Savings & Investments'][0]);
        } else {
            setCategory('Home Loan');
            setSubCategory('');
        }
        setName('');
        setAmount('');
        setPurchaseValue('');
        setTimeHorizon('Short (0-2 years)');
        setLiquidity('Immediate (instant access like savings account)');
        setIsModalOpen(true);
    };

    useEffect(() => {
        if (activeTab === 'assets' && assetCategories[category]) {
            setSubCategory(assetCategories[category][0]);
        }
    }, [category, activeTab]);

    const handleSave = async () => {
        const newItem = {
            id: Date.now(),
            category,
            subCategory: activeTab === 'assets' ? subCategory : undefined,
            name: name || (activeTab === 'assets' ? subCategory : category),
            amount: parseFloat(amount) || 0,
            purchaseValue: activeTab === 'assets' ? (parseFloat(purchaseValue) || 0) : undefined,
            timeHorizon: activeTab === 'assets' ? timeHorizon : undefined,
            liquidity: activeTab === 'assets' ? liquidity : undefined
        };

        if (activeTab === 'assets') {
            addAsset(newItem);
            try { await addAssetApi(newItem); } catch (e) { console.warn('Asset API save failed:', e.message); }
        } else {
            addLiability(newItem);
            try { await addLiabilityApi(newItem); } catch (e) { console.warn('Liability API save failed:', e.message); }
        }
        setIsModalOpen(false);
    };

    // Calculations
    const totalAssets = assets.reduce((sum, item) => sum + item.amount, 0);
    const totalLiabilities = liabilities.reduce((sum, item) => sum + item.amount, 0);
    const netWorth = totalAssets - totalLiabilities;

    // Asset Allocation
    const getAssetClass = (subcat) => {
        if (!subcat) return 'Debt';
        if (['Hybrid', 'Stocks/Shares', 'Mutual Funds — Equity'].some(s => subcat.includes(s))) return 'Equity';
        if (['Real Estate (Residential)', 'Real Estate (Commercial)', 'REITs/InvITs'].some(s => subcat.includes(s))) return 'Real Estate';
        if (['Gold/ Silver (Digital/Sovereign Gold Bonds)'].some(s => subcat.includes(s))) return 'Gold';
        return 'Debt'; // Catch-all for Bank, FD, RD, EPF, PPF, NPS, Bonds, etc. as requested
    };

    let equityTotal = 0, debtTotal = 0, realEstateTotal = 0, goldTotal = 0;
    assets.forEach(a => {
        const cls = getAssetClass(a.subCategory || a.category);
        if (cls === 'Equity') equityTotal += a.amount;
        else if (cls === 'Debt') debtTotal += a.amount;
        else if (cls === 'Real Estate') realEstateTotal += a.amount;
        else if (cls === 'Gold') goldTotal += a.amount;
    });

    const equityPct = totalAssets ? (equityTotal / totalAssets) * 100 : 0;
    const debtPct = totalAssets ? (debtTotal / totalAssets) * 100 : 0;
    const realEstatePct = totalAssets ? (realEstateTotal / totalAssets) * 100 : 0;
    const goldPct = totalAssets ? (goldTotal / totalAssets) * 100 : 0;

    const conicGradient = totalAssets > 0 ? `conic-gradient(
        #3b82f6 0% ${equityPct}%,
        #10b981 ${equityPct}% ${equityPct + debtPct}%,
        #8b5cf6 ${equityPct + debtPct}% ${equityPct + debtPct + realEstatePct}%,
        #f59e0b ${equityPct + debtPct + realEstatePct}% 100%
    )` : 'conic-gradient(#334155 0% 100%)';

    const getMismatchAlert = () => {
        if (totalAssets === 0) return null;
        if (realEstatePct > 15 + 20) {
            return {
                title: `⚠️ Your Real Estate exposure is ${(realEstatePct - 15).toFixed(0)}% above target.`,
                msg: `This concentrates risk. Consider gradually shifting ₹${((realEstatePct - 15) / 100 * totalAssets / 100000).toFixed(1)} lakhs to equity over 2-3 years.`
            };
        }
        if (equityPct < 50 - 20) {
            return {
                title: `⚠️ Your Equity exposure is ${(50 - equityPct).toFixed(0)}% below target.`,
                msg: `Consider gradually increasing your investments in mutual funds and stocks.`
            };
        }
        return null;
    };
    const alert = getMismatchAlert();

    return (
        <div className="flex flex-col h-full overflow-y-auto pb-44">
            {/* Header Content */}
            <div className="mb-6">
                <h1 className="text-3xl font-bold tracking-tight text-white mb-2">Your Current Wealth Position</h1>
                <p className="text-slate-400 text-sm">Let's map what you own and what you owe.</p>
            </div>

            {/* Net Worth Card */}
            <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-surface-dark to-black p-6 shadow-lg mb-6 border border-white/5 flex-shrink-0">
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

            {/* Asset Allocation Chart (Visible only if there are assets) */}
            {totalAssets > 0 && activeTab === 'assets' && (
                <div className="bg-surface-dark border border-white/5 rounded-2xl p-6 mb-6">
                    <h3 className="text-white font-bold mb-4">Your Portfolio Mix</h3>
                    <div className="flex flex-col md:flex-row items-center gap-8 mb-6">
                        <div className="w-32 h-32 rounded-full relative" style={{ background: conicGradient }}>
                            <div className="absolute inset-2 bg-surface-dark rounded-full"></div>
                        </div>
                        <div className="flex-1 space-y-2 w-full">
                            <div className="flex justify-between text-sm">
                                <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-blue-500"></div><span className="text-slate-300">Equity</span></div>
                                <span className="text-white font-semibold">{equityPct.toFixed(1)}% <span className="text-slate-500 font-normal ml-1">(₹{equityTotal.toLocaleString()})</span></span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-emerald-500"></div><span className="text-slate-300">Debt</span></div>
                                <span className="text-white font-semibold">{debtPct.toFixed(1)}% <span className="text-slate-500 font-normal ml-1">(₹{debtTotal.toLocaleString()})</span></span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-purple-500"></div><span className="text-slate-300">Real Estate</span></div>
                                <span className="text-white font-semibold">{realEstatePct.toFixed(1)}% <span className="text-slate-500 font-normal ml-1">(₹{realEstateTotal.toLocaleString()})</span></span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-amber-500"></div><span className="text-slate-300">Gold</span></div>
                                <span className="text-white font-semibold">{goldPct.toFixed(1)}% <span className="text-slate-500 font-normal ml-1">(₹{goldTotal.toLocaleString()})</span></span>
                            </div>
                        </div>
                    </div>

                    <h4 className="text-white font-bold text-sm mb-3">Your Current vs Target (Moderate Risk):</h4>
                    <div className="bg-background-dark rounded-xl p-4 space-y-3 mb-4">
                        <div className="grid grid-cols-4 text-xs font-semibold text-slate-500 mb-1">
                            <div className="col-span-1">Asset</div>
                            <div className="text-center">You</div>
                            <div className="text-center">Target</div>
                            <div className="text-right">Status</div>
                        </div>
                        <div className="grid grid-cols-4 text-sm items-center">
                            <div className="text-slate-300">Equity</div>
                            <div className="text-center font-bold text-white">{equityPct.toFixed(0)}%</div>
                            <div className="text-center text-slate-400">50%</div>
                            <div className="text-right text-xs">{equityPct < 30 ? <span className="text-amber-400">⚠️ {50 - Math.round(equityPct)}% below</span> : <span className="text-emerald-400">✓ Close enough</span>}</div>
                        </div>
                        <div className="grid grid-cols-4 text-sm items-center">
                            <div className="text-slate-300">Debt</div>
                            <div className="text-center font-bold text-white">{debtPct.toFixed(0)}%</div>
                            <div className="text-center text-slate-400">30%</div>
                            <div className="text-right text-xs"><span className="text-emerald-400">✓ Close enough</span></div>
                        </div>
                        <div className="grid grid-cols-4 text-sm items-center">
                            <div className="text-slate-300">Real Estate</div>
                            <div className="text-center font-bold text-white">{realEstatePct.toFixed(0)}%</div>
                            <div className="text-center text-slate-400">15%</div>
                            <div className="text-right text-xs">{realEstatePct > 35 ? <span className="text-amber-400">⚠️ {Math.round(realEstatePct) - 15}% above</span> : <span className="text-emerald-400">✓ Close enough</span>}</div>
                        </div>
                        <div className="grid grid-cols-4 text-sm items-center">
                            <div className="text-slate-300">Gold</div>
                            <div className="text-center font-bold text-white">{goldPct.toFixed(0)}%</div>
                            <div className="text-center text-slate-400">5%</div>
                            <div className="text-right text-xs"><span className="text-emerald-400">✓ Close enough</span></div>
                        </div>
                    </div>

                    {alert && (
                        <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                            <h5 className="font-bold text-amber-400 text-sm mb-1">{alert.title}</h5>
                            <p className="text-amber-400/80 text-xs">{alert.msg}</p>
                        </div>
                    )}
                </div>
            )}

            {/* Tabs */}
            <div className="bg-surface-active p-1 rounded-xl flex mb-6 flex-shrink-0">
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
            <div className="flex-1 space-y-4">
                {(activeTab === 'assets' ? assets : liabilities).map((item) => (
                    <div key={item.id} className="flex justify-between items-center bg-surface-dark p-4 rounded-xl border border-white/5 shadow-sm">
                        <div className="flex items-center gap-3">
                            <div className={`p-2 rounded-lg ${activeTab === 'assets' ? 'bg-primary/10 text-primary' : 'bg-red-500/10 text-red-400'}`}>
                                {activeTab === 'assets' ? <TrendingUp className="w-5 h-5" /> : <CreditCard className="w-5 h-5" />}
                            </div>
                            <div>
                                <p className="font-bold text-sm text-white">{item.name}</p>
                                <p className="text-xs text-slate-400">{item.category} {item.subCategory ? `• ${item.subCategory}` : ''}</p>
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

            {/* Inline Split Button — Left Aligned */}
            <div className="mt-8 mb-4 flex items-center gap-3 justify-end flex-shrink-0">
                <div className="flex items-center gap-2 px-4 py-3 bg-surface-dark border border-white/10 rounded-xl">
                    <CheckCircle2 className="w-4 h-4 text-primary" />
                    <span className="text-sm font-semibold text-slate-400">Step 3/6</span>
                </div>
                <button
                    onClick={() => {
                        if (totalAssets === 0 && totalLiabilities === 0) {
                            toast.error('Add your assets or liabilities — savings, FDs, loans, etc.', { id: 'step3-guide' });
                            return;
                        }
                        navigate('/assessment/step-4');
                    }}
                    className="px-6 py-3 bg-primary hover:bg-primary-dark active:scale-[0.98] text-background-dark font-bold text-sm rounded-xl flex items-center gap-2 transition-all shadow-[0_0_15px_rgba(13,242,89,0.25)]"
                >
                    Next <ArrowRight className="w-4 h-4" />
                </button>
            </div>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={() => setIsModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-lg rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl border-t border-white/10 animate-slide-up max-h-[90vh] overflow-y-auto">
                        <div className="w-12 h-1 bg-surface-active rounded-full mx-auto mb-6 sm:hidden"></div>
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-white">Add {activeTab === 'assets' ? 'Asset' : 'Liability'}</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            {activeTab === 'assets' ? (
                                <>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Category</label>
                                        <select
                                            value={category}
                                            onChange={(e) => setCategory(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                        >
                                            {Object.keys(assetCategories).map(cat => (
                                                <option key={cat}>{cat}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Sub Category</label>
                                        <select
                                            value={subCategory}
                                            onChange={(e) => setSubCategory(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                        >
                                            {assetCategories[category]?.map(sub => (
                                                <option key={sub}>{sub}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Purchase Value (₹)</label>
                                            <input
                                                type="number"
                                                value={purchaseValue}
                                                onChange={(e) => setPurchaseValue(e.target.value)}
                                                className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                                placeholder="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Current Value (₹)</label>
                                            <input
                                                type="number"
                                                value={amount}
                                                onChange={(e) => setAmount(e.target.value)}
                                                className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                                placeholder="0"
                                            />
                                        </div>
                                    </div>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Time Horizon</label>
                                        <select
                                            value={timeHorizon}
                                            onChange={(e) => setTimeHorizon(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                        >
                                            <option>Short (0-2 years)</option>
                                            <option>Medium (2-7 years)</option>
                                            <option>Long (7+ years)</option>
                                            <option>Retirement</option>
                                            <option>Never</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Liquidity</label>
                                        <select
                                            value={liquidity}
                                            onChange={(e) => setLiquidity(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                        >
                                            <option>Immediate (instant access like savings account)</option>
                                            <option>High (1-2 days like stocks, mutual funds)</option>
                                            <option>Medium (few days to weeks like FD)</option>
                                            <option>Low (months like real estate, gold)</option>
                                        </select>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Category</label>
                                        <select
                                            value={category}
                                            onChange={(e) => setCategory(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                        >
                                            <option>Home Loan</option>
                                            <option>Car Loan</option>
                                            <option>Personal Loan</option>
                                            <option>Credit Card Debt</option>
                                            <option>Other Debt</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Description / Name</label>
                                        <input
                                            type="text"
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                            placeholder="e.g., HDFC Home Loan"
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
                                </>
                            )}

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
