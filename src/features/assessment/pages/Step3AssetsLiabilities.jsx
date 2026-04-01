import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, Pencil, Wallet, CreditCard, Building, TrendingUp, CheckCircle2, AlertTriangle, Info } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useBalanceSheetQuery, useAddAssetMutation, useAddLiabilityMutation, useUpdateAssetMutation, useUpdateLiabilityMutation, useDeleteAssetMutation, useDeleteLiabilityMutation } from '../hooks/useBalanceSheet';
import { useRiskScoringQuery } from '../hooks/useRiskScoring';
import { usePortfolioAnalysisQuery } from '../hooks/usePortfolioAnalysis';
import { AssetsLiabilitiesSkeleton } from '../../../components/ui/AssessmentSkeleton';

const Step3AssetsLiabilities = () => {
    const navigate = useNavigate();
    const { incomes, expenses, assets, addAsset, removeAsset, liabilities, addLiability, removeLiability } = useAssessmentStore();

    // API Integration
    const { data: balanceData, isLoading: isFetchingBalance } = useBalanceSheetQuery();
    const { mutateAsync: addAssetApi } = useAddAssetMutation();
    const { mutateAsync: addLiabilityApi } = useAddLiabilityMutation();
    const { mutateAsync: updateAssetApi } = useUpdateAssetMutation();
    const { mutateAsync: updateLiabilityApi } = useUpdateLiabilityMutation();
    const { mutateAsync: deleteAssetApi, isPending: isDeletingAsset } = useDeleteAssetMutation();
    const { mutateAsync: deleteLiabilityApi, isPending: isDeletingLiability } = useDeleteLiabilityMutation();

    // Hydrate store from API
    useEffect(() => {
        if (balanceData) {
            if (balanceData.assets) useAssessmentStore.setState({ assets: balanceData.assets });
            if (balanceData.liabilities) useAssessmentStore.setState({ liabilities: balanceData.liabilities });
        }
    }, [balanceData]);

    // Fetch risk scoring (scores + target allocation) from backend
    const { data: riskData, isError: isRiskError, isLoading: isRiskLoading } = useRiskScoringQuery();
    const hasRiskData = !!riskData && !isRiskError && riskData.profileLabel != null;

    // Fetch portfolio analysis (classification, allocation %, DTI, etc.) from backend
    const { data: portfolio } = usePortfolioAnalysisQuery();

    const [activeTab, setActiveTab] = useState('assets'); // 'assets' or 'liabilities'
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingItem, setEditingItem] = useState(null); // null = add mode, object = edit mode

    // Form State
    const [category, setCategory] = useState('');
    const [subCategory, setSubCategory] = useState('');
    const [name, setName] = useState('');
    const [amount, setAmount] = useState('');

    // Asset Specific State
    const [purchaseValue, setPurchaseValue] = useState('');
    const [timeHorizon, setTimeHorizon] = useState('Short (0-2 years)');
    const [liquidity, setLiquidity] = useState('Immediate (instant access like savings account)');

    // Liability Specific State
    const [emi, setEmi] = useState('');
    const [interestRate, setInterestRate] = useState('');
    const [monthsLeft, setMonthsLeft] = useState('');
    const [moratoriumMonths, setMoratoriumMonths] = useState('');

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

    const liabilityCategories = [
        '🏠 Home Loan',
        '💰 Personal Loan',
        '💼 Business Loan',
        '💳 Credit Card Debt',
        '🚗 Vehicle Loan',
        '🪙 Gold Loan',
        '🎓 Education Loan',
        '🏢 Loan Against Property',
        '➕ Other'
    ];

    const openModal = () => {
        setEditingItem(null);
        if (activeTab === 'assets') {
            setCategory('Savings & Investments');
            setSubCategory(assetCategories['Savings & Investments'][0]);
        } else {
            setCategory(liabilityCategories[0]);
            setSubCategory('');
        }
        setName('');
        setAmount('');
        setPurchaseValue('');
        setTimeHorizon('Short (0-2 years)');
        setLiquidity('Immediate (instant access like savings account)');
        setEmi('');
        setInterestRate('');
        setMonthsLeft('');
        setMoratoriumMonths('');
        setIsModalOpen(true);
    };

    const openEditModal = (item) => {
        setEditingItem(item);
        if (activeTab === 'assets') {
            // Find which category group this subCategory belongs to
            const matchedCat = Object.entries(assetCategories).find(([, subs]) => subs.includes(item.subCategory || item.category));
            setCategory(matchedCat ? matchedCat[0] : 'Savings & Investments');
            setSubCategory(item.subCategory || item.category || '');
            setPurchaseValue(item.purchaseValue ?? '');
            setTimeHorizon(item.timeHorizon || 'Short (0-2 years)');
            setLiquidity(item.liquidity || 'Immediate (instant access like savings account)');
        } else {
            setCategory(item.category || liabilityCategories[0]);
            setEmi(item.emi ?? '');
            setInterestRate(item.interestRate ?? '');
            setMonthsLeft(item.monthsLeft ?? '');
            setMoratoriumMonths(item.moratoriumMonths ?? '');
        }
        setName(item.name || '');
        setAmount(item.amount ?? '');
        setIsModalOpen(true);
    };

    useEffect(() => {
        if (activeTab === 'assets' && assetCategories[category]) {
            setSubCategory(assetCategories[category][0]);
        }
    }, [category, activeTab]);

    const handleSave = async () => {
        const newItem = {
            category,
            subCategory: activeTab === 'assets' ? subCategory : undefined,
            name: name || (activeTab === 'assets' ? subCategory : category),
            amount: parseFloat(amount) || 0,
            purchaseValue: activeTab === 'assets' ? (parseFloat(purchaseValue) || 0) : undefined,
            timeHorizon: activeTab === 'assets' ? timeHorizon : undefined,
            liquidity: activeTab === 'assets' ? liquidity : undefined,
            emi: activeTab === 'liabilities' ? (parseFloat(emi) || 0) : undefined,
            interestRate: activeTab === 'liabilities' ? (parseFloat(interestRate) || 0) : undefined,
            monthsLeft: activeTab === 'liabilities' ? (parseInt(monthsLeft, 10) || 0) : undefined,
            moratoriumMonths: activeTab === 'liabilities' && category === '🎓 Education Loan' ? (parseInt(moratoriumMonths, 10) || 0) : undefined,
        };

        // ── Edit mode ──
        if (editingItem) {
            setIsModalOpen(false);
            try {
                if (activeTab === 'assets') {
                    await updateAssetApi({ id: editingItem.id, ...newItem });
                } else {
                    await updateLiabilityApi({ id: editingItem.id, ...newItem });
                }
                toast.success(`${activeTab === 'assets' ? 'Asset' : 'Liability'} updated successfully`);
            } catch {
                toast.error('Failed to update. Please try again.');
            }
            setEditingItem(null);
            return;
        }

        // ── Add mode ──
        const tempId = `temp-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
        const optimisticItem = { ...newItem, id: tempId };

        if (activeTab === 'assets') {
            addAsset(optimisticItem);
            setIsModalOpen(false);
            try {
                await addAssetApi(newItem);
                toast.success('Asset saved successfully');
            } catch {
                removeAsset(tempId);
                toast.error('Failed to save asset. Please try again.');
            }
        } else {
            addLiability(optimisticItem);
            setIsModalOpen(false);
            try {
                await addLiabilityApi(newItem);
                toast.success('Liability saved successfully');
            } catch {
                removeLiability(tempId);
                toast.error('Failed to save liability. Please try again.');
            }
        }
    };

    // ── Backend-computed portfolio analysis ──────────────────────────────
    const totalAssets = portfolio?.totalAssets ?? 0;
    const totalLiabilities = portfolio?.totalLiabilities ?? 0;
    const netWorth = portfolio?.netWorth ?? 0;
    const monthlyEmiTotal = portfolio?.monthlyEmiTotal ?? 0;
    const avgInterestRate = portfolio?.avgInterestRate ?? 0;
    const dtiRatio = portfolio?.dtiRatio ?? 0;
    const totalMonthlyIncome = portfolio?.monthlyIncome ?? 0;
    const cashFlowEMI = portfolio?.cashFlowEMI ?? 0;
    const emiMismatch = portfolio?.emiMismatch ?? false;

    const equityPct = portfolio?.equityPct ?? 0;
    const debtPct = portfolio?.debtPct ?? 0;
    const realEstatePct = portfolio?.realEstatePct ?? 0;
    const goldPct = portfolio?.goldPct ?? 0;
    const otherPct = portfolio?.otherPct ?? 0;

    const equityTotal = portfolio?.equityTotal ?? 0;
    const debtTotal = portfolio?.debtTotal ?? 0;
    const realEstateTotal = portfolio?.realEstateTotal ?? 0;
    const goldTotal = portfolio?.goldTotal ?? 0;
    const otherTotal = portfolio?.otherTotal ?? 0;

    // ── Visual helpers (stay in frontend) ────────────────────────────────
    const formatNetWorth = (amount) => {
        const absAmount = Math.abs(amount);
        let formatted;
        let label = '';

        if (absAmount >= 10000000) {
            formatted = (absAmount / 10000000).toFixed(2);
            label = 'Crores';
        } else if (absAmount >= 100000) {
            formatted = (absAmount / 100000).toFixed(2);
            label = 'Lakhs';
        } else {
            formatted = absAmount.toLocaleString();
        }

        const sign = amount < 0 ? '-' : '';
        const colorClass = amount < 0 ? 'text-red-400' : 'text-white';

        return {
            text: `${sign}₹ ${formatted} ${label}`.trim(),
            colorClass
        };
    };

    const netWorthFormat = formatNetWorth(netWorth);

    const conicGradient = totalAssets > 0 ? `conic-gradient(
        #3b82f6 0% ${equityPct}%,
        #10b981 ${equityPct}% ${equityPct + debtPct}%,
        #8b5cf6 ${equityPct + debtPct}% ${equityPct + debtPct + realEstatePct}%,
        #f59e0b ${equityPct + debtPct + realEstatePct}% ${equityPct + debtPct + realEstatePct + goldPct}%,
        #64748b ${equityPct + debtPct + realEstatePct + goldPct}% 100%
    )` : 'conic-gradient(#334155 0% 100%)';

    // Dynamic target allocation from backend scoring
    const targetEquity = riskData?.targetEquity ?? 50;
    const targetDebt = riskData?.targetDebt ?? 30;
    const targetGold = riskData?.targetGold ?? 5;
    const targetRealEstate = riskData?.targetRealEstate ?? 15;
    const profileLabel = riskData?.profileLabel ?? 'Moderate';

    const getMismatchAlert = () => {
        if (totalAssets === 0) return null;
        if (realEstatePct > targetRealEstate + 20) {
            return {
                title: `⚠️ Your Real Estate exposure is ${(realEstatePct - targetRealEstate).toFixed(0)}% above target.`,
                msg: `This concentrates risk. Consider gradually shifting ₹${((realEstatePct - targetRealEstate) / 100 * totalAssets / 100000).toFixed(1)} lakhs to equity over 2-3 years.`
            };
        }
        if (equityPct < targetEquity - 20) {
            return {
                title: `⚠️ Your Equity exposure is ${(targetEquity - equityPct).toFixed(0)}% below target.`,
                msg: `Consider gradually increasing your investments in mutual funds and stocks.`
            };
        }
        return null;
    };
    const alert = getMismatchAlert();

    if (isFetchingBalance) return <AssetsLiabilitiesSkeleton />;

    return (
        <div className="flex flex-col h-full overflow-y-auto pb-44">
            {/* Header Content */}
            <div className="mb-6">
                <h1 className="text-2xl font-bold tracking-tight text-white mb-2">Your Current Wealth Position</h1>
                <p className="text-slate-400 text-sm">Let's map what you own and what you owe.</p>
            </div>

            {/* Premium Net Worth Card */}
            <div className="relative overflow-hidden rounded-[2rem] p-[1px] mb-8 flex-shrink-0 group shadow-2xl">
                <div className="absolute inset-0 bg-gradient-to-br from-white/10 via-transparent to-white/5 z-0 rounded-[2rem]"></div>
                <div className="absolute -top-32 -right-32 w-72 h-72 bg-primary/30 rounded-full blur-[100px] z-0 group-hover:bg-primary/40 transition-colors duration-1000"></div>
                <div className="absolute -bottom-32 -left-32 w-72 h-72 bg-blue-500/20 rounded-full blur-[100px] z-0 group-hover:bg-blue-500/30 transition-colors duration-1000"></div>

                <div className="relative z-10 bg-surface-dark/40 backdrop-blur-2xl h-full w-full rounded-[2rem] border border-white/10 p-8 flex flex-col overflow-hidden">
                    <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-white/20 to-transparent"></div>

                    <div className="flex flex-col lg:flex-row lg:justify-between lg:items-end gap-8">
                        {/* Main Value */}
                        <div className="flex flex-col gap-3">
                            <div className="flex items-center gap-2.5">
                                <div className="w-2 h-2 rounded-full bg-primary animate-pulse shadow-[0_0_10px_#0ab842]"></div>
                                <p className="text-slate-400 text-xs font-bold tracking-widest uppercase">Total Net Worth</p>
                            </div>
                            <h1 className={`text-5xl md:text-6xl font-black tracking-tight ${netWorthFormat.colorClass} drop-shadow-lg`}>
                                {netWorthFormat.text}
                            </h1>
                        </div>

                        {/* Breakdown Pills */}
                        <div className="flex flex-col sm:flex-row items-center gap-4">
                            <div className="flex items-center gap-3 bg-white/5 hover:bg-white/10 transition-colors px-5 py-4 w-full sm:w-auto rounded-2xl border border-white/5 backdrop-blur-md">
                                <div className="p-2.5 bg-primary/20 rounded-xl">
                                    <TrendingUp className="w-5 h-5 text-primary" />
                                </div>
                                <div className="flex flex-col">
                                    <span className="text-slate-400 text-[10px] font-bold uppercase tracking-wider mb-0.5">Assets</span>
                                    <span className="text-white font-bold tracking-wide">{formatNetWorth(totalAssets).text}</span>
                                </div>
                            </div>

                            <div className="flex items-center gap-3 bg-white/5 hover:bg-white/10 transition-colors px-5 py-4 w-full sm:w-auto rounded-2xl border border-white/5 backdrop-blur-md">
                                <div className="p-2.5 bg-red-500/20 rounded-xl">
                                    <TrendingUp className="w-5 h-5 text-red-500 rotate-180" />
                                </div>
                                <div className="flex flex-col">
                                    <span className="text-slate-400 text-[10px] font-bold uppercase tracking-wider mb-0.5">Liabilities</span>
                                    <span className="text-white font-bold tracking-wide">{formatNetWorth(totalLiabilities).text}</span>
                                </div>
                            </div>
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
                            {otherTotal > 0 && (
                                <div className="flex justify-between text-sm">
                                    <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-slate-500"></div><span className="text-slate-300">Other</span></div>
                                    <span className="text-white font-semibold">{otherPct.toFixed(1)}% <span className="text-slate-500 font-normal ml-1">(₹{otherTotal.toLocaleString()})</span></span>
                                </div>
                            )}
                        </div>
                    </div>

                    {hasRiskData ? (
                        <>
                            <h4 className="text-white font-bold text-sm mb-3">Your Current vs Target ({profileLabel}):</h4>
                            <div className="bg-background-dark rounded-xl p-4 space-y-3 mb-4">
                                <div className="grid grid-cols-4 text-xs font-semibold text-slate-500 mb-1">
                                    <div className="col-span-1">Asset</div>
                                    <div className="text-center">You</div>
                                    <div className="text-center">Target</div>
                                    <div className="text-right">Status</div>
                                </div>
                                {[
                                    { label: 'Equity',      current: equityPct,     target: targetEquity },
                                    { label: 'Debt',         current: debtPct,       target: targetDebt },
                                    { label: 'Real Estate',  current: realEstatePct, target: targetRealEstate },
                                    { label: 'Gold',         current: goldPct,       target: targetGold },
                                ].map(row => {
                                    const diff = Math.round(row.current) - row.target;
                                    const threshold = row.target * 0.4; // 40% tolerance band
                                    let statusEl;
                                    if (diff > threshold) {
                                        statusEl = <span className="text-amber-400">⚠️ {Math.abs(diff)}% above</span>;
                                    } else if (diff < -threshold) {
                                        statusEl = <span className="text-amber-400">⚠️ {Math.abs(diff)}% below</span>;
                                    } else {
                                        statusEl = <span className="text-emerald-400">✓ On track</span>;
                                    }
                                    return (
                                        <div key={row.label} className="grid grid-cols-4 text-sm items-center">
                                            <div className="text-slate-300">{row.label}</div>
                                            <div className="text-center font-bold text-white">{row.current.toFixed(0)}%</div>
                                            <div className="text-center text-slate-400">{row.target}%</div>
                                            <div className="text-right text-xs">{statusEl}</div>
                                        </div>
                                    );
                                })}
                            </div>

                            {alert && (
                                <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                                    <h5 className="font-bold text-amber-400 text-sm mb-1">{alert.title}</h5>
                                    <p className="text-amber-400/80 text-xs">{alert.msg}</p>
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="bg-blue-500/10 border border-blue-500/20 rounded-xl p-4 flex items-start gap-3">
                            <Info className="w-5 h-5 text-blue-400 mt-0.5 shrink-0" />
                            <div>
                                <h5 className="text-blue-400 font-bold text-sm mb-1">
                                    {isRiskLoading ? 'Loading your risk profile…' : 'Target allocation unavailable'}
                                </h5>
                                <p className="text-slate-400 text-xs">
                                    {isRiskLoading
                                        ? 'We\'re calculating your personalized target based on your profile.'
                                        : 'Complete your profile and risk questionnaire (Step 1 & 4) so we can suggest a personalized target allocation for you.'}
                                </p>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* Liabilities Summary Card */}
            {totalLiabilities > 0 && activeTab === 'liabilities' && (
                <div className="bg-surface-dark border border-red-500/20 shadow-[0_0_15px_rgba(239,68,68,0.05)] rounded-2xl p-6 mb-6 animate-fade-in relative overflow-hidden">
                    <div className="absolute -left-10 -bottom-10 h-32 w-32 rounded-full bg-red-500/10 blur-3xl"></div>
                    <div className="relative z-10">
                        <h3 className="text-white font-bold mb-4 font-mono tracking-wider flex items-center justify-center border-b border-red-500/20 pb-4">
                            <span className="text-slate-500 mr-2">══</span>
                            LIABILITIES SUMMARY
                            <span className="text-slate-500 ml-2">══</span>
                        </h3>

                        <div className="space-y-4 font-mono text-sm">
                            <div className="flex justify-between items-center">
                                <span className="text-slate-300">Total Outstanding</span>
                                <span className="text-red-400 font-bold">₹ {totalLiabilities.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-slate-300">Monthly EMI Total</span>
                                <span className="text-white font-bold">₹ {monthlyEmiTotal.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-slate-300">Avg Interest Rate</span>
                                <span className="text-white font-bold">{avgInterestRate.toFixed(1)}%</span>
                            </div>
                        </div>

                        {/* EMI Mismatch Warning */}
                        {emiMismatch && (
                            <div className="mt-5 bg-red-500/10 border border-red-500/30 rounded-xl p-4 flex items-start gap-3 animate-fade-in">
                                <div className="shrink-0 mt-0.5 text-red-400">
                                    <AlertTriangle className="w-5 h-5" />
                                </div>
                                <div>
                                    <h5 className="text-red-400 font-bold text-sm mb-1.5">⚠️ EMI Mismatch Detected</h5>
                                    <p className="text-slate-300 text-xs leading-relaxed">
                                        Your <span className="font-bold text-white">Cash Flow</span> shows EMI expenses of <span className="font-bold text-amber-400">₹{cashFlowEMI.toLocaleString()}/mo</span>, but your <span className="font-bold text-white">Liabilities</span> add up to <span className="font-bold text-amber-400">₹{monthlyEmiTotal.toLocaleString()}/mo</span>.
                                    </p>
                                    <p className="text-slate-400 text-xs mt-2">
                                        Please update the EMI amount in <button onClick={() => navigate('/assessment/step-2')} className="underline text-primary font-semibold hover:text-primary-dark transition-colors">Cash Flow (Step 2)</button> or correct the EMI values in your liabilities above so both match.
                                    </p>
                                </div>
                            </div>
                        )}

                        {/* Debt-to-Income Gauge */}
                        {totalMonthlyIncome > 0 && (
                            <div className="mt-8 pt-6 border-t border-white/10">
                                <h4 className="text-slate-300 text-sm font-semibold mb-6 text-center">Debt-to-Income Ratio</h4>

                                <div className="relative h-2 bg-gradient-to-r from-emerald-500 via-amber-500 to-red-500 rounded-full mb-8 mx-4">
                                    <div
                                        className="absolute top-1/2 -translate-y-1/2 -ml-2 w-4 h-4 bg-white rounded-full shadow-lg border-2 border-surface-dark transition-all duration-1000"
                                        style={{ left: `${Math.min(Math.max(dtiRatio * 2, 0), 100)}%` }} // Scaling 0-50% across 0-100% width
                                    ></div>
                                    <div
                                        className="absolute -top-8 -translate-x-1/2 bg-surface-active px-2 py-1 rounded text-xs font-bold text-white transition-all duration-1000"
                                        style={{ left: `${Math.min(Math.max(dtiRatio * 2, 0), 100)}%` }}
                                    >
                                        {dtiRatio.toFixed(1)}%
                                    </div>
                                    <div className="absolute -bottom-6 left-0 text-xs text-slate-400 font-medium">0%</div>
                                    <div className="absolute -bottom-6 right-0 text-xs text-slate-400 font-medium">50%+</div>
                                </div>

                                <div className="bg-background-dark rounded-xl p-4 mt-2 border border-white/5">
                                    {dtiRatio < 30 ? (
                                        <div className="flex items-start gap-3">
                                            <CheckCircle2 className="w-5 h-5 text-emerald-500 mt-0.5 shrink-0" />
                                            <div>
                                                <h5 className="text-emerald-500 font-bold text-sm mb-1">Healthy (Below 30% threshold)</h5>
                                                <p className="text-slate-400 text-xs">Your debt burden is manageable and leaves room for savings and investments.</p>
                                            </div>
                                        </div>
                                    ) : dtiRatio <= 40 ? (
                                        <div className="flex items-start gap-3">
                                            <AlertTriangle className="w-5 h-5 text-amber-500 mt-0.5 shrink-0" />
                                            <div>
                                                <h5 className="text-amber-500 font-bold text-sm mb-1">Monitor: Your EMIs are {dtiRatio.toFixed(0)}% of income.</h5>
                                                <p className="text-slate-400 text-xs">Avoid taking more loans until this improves or your income increases.</p>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="flex items-start gap-3">
                                            <div className="shrink-0 text-red-500 mt-0.5">🚨</div>
                                            <div>
                                                <h5 className="text-red-500 font-bold text-sm mb-2">Risky: Your EMIs are {dtiRatio.toFixed(0)}% of income.</h5>
                                                <p className="text-slate-300 text-xs font-semibold mb-1">Consider:</p>
                                                <ul className="text-slate-400 text-xs space-y-1 list-disc pl-4">
                                                    <li>Debt consolidation at lower rates</li>
                                                    <li>Extending loan tenure to reduce EMI</li>
                                                    <li>Increasing income or reducing expenses</li>
                                                </ul>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
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
                                <p className="text-xs text-slate-400">
                                    {item.category} {item.subCategory ? `• ${item.subCategory}` : ''}
                                </p>
                                {activeTab === 'liabilities' && (
                                    <p className="text-xs text-red-400/80 mt-1">
                                        ₹{item.emi?.toLocaleString()}/mo @ {item.interestRate}%
                                        {item.monthsLeft ? ` • ${item.monthsLeft} mo left` : ''}
                                    </p>
                                )}
                            </div>
                        </div>
                        <div className="flex items-center gap-2">
                            <span className="font-bold text-white">₹ {item.amount.toLocaleString()}</span>
                            <button
                                onClick={() => openEditModal(item)}
                                className="text-slate-500 hover:text-primary transition-colors"
                                title="Edit"
                            >
                                <Pencil className="w-3.5 h-3.5" />
                            </button>
                            <button
                                onClick={async () => {
                                    if (activeTab === 'assets') {
                                        removeAsset(item.id);
                                        try { await deleteAssetApi(item.id); } catch (error) { console.warn('Asset API delete failed, removed locally:', error.message); }
                                    } else {
                                        removeLiability(item.id);
                                        try { await deleteLiabilityApi(item.id); } catch (error) { console.warn('Liability API delete failed, removed locally:', error.message); }
                                    }
                                    toast.success(`${activeTab === 'assets' ? 'Asset' : 'Liability'} deleted successfully`);
                                }}
                                disabled={isDeletingAsset || isDeletingLiability}
                                className="text-slate-500 hover:text-red-400 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
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

            {/* Bottom Navigation */}
            <div className="fixed bottom-0 left-0 right-0 bg-background-dark/80 backdrop-blur-lg border-t border-white/5 p-4 z-50">
                <div className="w-full px-6 flex items-center justify-between">
                    <button
                        onClick={() => navigate('/assessment/step-2')}
                        className="px-6 py-3 bg-surface-dark hover:bg-surface-active text-white font-bold text-sm rounded-xl transition-all"
                    >
                        Back
                    </button>
                    <div className="flex items-center gap-3">
                        <div className="hidden sm:flex items-center gap-2 px-4 py-3 bg-surface-dark border border-white/10 rounded-xl">
                            <CheckCircle2 className="w-4 h-4 text-primary" />
                            <span className="text-sm font-semibold text-slate-400">Step 3/6</span>
                        </div>
                        <button
                            onClick={() => {
                                const storeHasData = assets.length > 0 || liabilities.length > 0;
                                if (totalAssets === 0 && totalLiabilities === 0 && !storeHasData) {
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
                </div>
            </div>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={() => setIsModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-lg rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl border-t border-white/10 animate-slide-up max-h-[90vh] overflow-y-auto">
                        <div className="w-12 h-1 bg-surface-active rounded-full mx-auto mb-6 sm:hidden"></div>
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-white">{editingItem ? 'Edit' : 'Add'} {activeTab === 'assets' ? 'Asset' : 'Liability'}</h3>
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
                                            {liabilityCategories.map(cat => (
                                                <option key={cat}>{cat}</option>
                                            ))}
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
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">How much is still pending? (₹)</label>
                                        <input
                                            type="number"
                                            value={amount}
                                            onChange={(e) => setAmount(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-lg text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                            placeholder="e.g., 20,00,000"
                                        />
                                    </div>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Monthly EMI (₹)</label>
                                            <input
                                                type="number"
                                                value={emi}
                                                onChange={(e) => setEmi(e.target.value)}
                                                className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                                placeholder="e.g., 25,363"
                                            />
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-1 mb-2">
                                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Interest Rate (%)</label>
                                                <div className="group relative">
                                                    <Info className="w-3.5 h-3.5 text-slate-500" />
                                                    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 w-max bg-surface text-slate-300 text-[10px] py-1 px-2 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                                                        Check your loan statement
                                                    </div>
                                                </div>
                                            </div>
                                            <input
                                                type="number"
                                                value={interestRate}
                                                onChange={(e) => setInterestRate(e.target.value)}
                                                className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                                placeholder="e.g., 9"
                                                step="0.1"
                                            />
                                        </div>
                                    </div>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Months Left</label>
                                            <input
                                                type="number"
                                                value={monthsLeft}
                                                onChange={(e) => setMonthsLeft(e.target.value)}
                                                className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                                placeholder="e.g., 120"
                                            />
                                        </div>
                                        {category === '🎓 Education Loan' && (
                                            <div>
                                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Moratorium Months?</label>
                                                <input
                                                    type="number"
                                                    value={moratoriumMonths}
                                                    onChange={(e) => setMoratoriumMonths(e.target.value)}
                                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                                    placeholder="e.g., 6"
                                                />
                                            </div>
                                        )}
                                    </div>
                                </>
                            )}

                            <button
                                onClick={handleSave}
                                className="w-full bg-primary hover:bg-primary-dark text-background-dark font-bold py-4 rounded-xl mt-4 shadow-[0_0_15px_rgba(13,242,89,0.3)] active:scale-[0.98] transition-all"
                            >
                                {editingItem ? 'Update' : 'Save'} {activeTab === 'assets' ? 'Asset' : 'Liability'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Step3AssetsLiabilities;
