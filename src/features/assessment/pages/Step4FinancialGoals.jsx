import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, Home, Car, GraduationCap, Plane, Heart, Briefcase, TrendingUp, CheckCircle2, ShieldAlert, AlertTriangle, AlertCircle, Lock, Building2, Stethoscope, Briefcase as BusinessIcon, HelpCircle, Crown, Clock, Target, Wallet, BarChart3, ArrowUpRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useGoalsQuery, useAddGoalMutation, useDeleteGoalMutation } from '../hooks/useGoals';
import { useGoalProjectionQuery } from '../hooks/useGoalProjection';
import { useRetirementAutoFillQuery } from '../hooks/useRetirementAutoFill';
import { GoalsSkeleton } from '../../../components/ui/AssessmentSkeleton';

const GOAL_TYPES = [
    { id: 'home', label: 'Home Purchase', icon: Home, defaultCost: 7500000, defaultHorizon: 15 },
    { id: 'education', label: 'Child Education', icon: GraduationCap, defaultCost: 2500000, defaultHorizon: 10 },
    { id: 'marriage', label: 'Child Marriage', icon: Heart, defaultCost: 2000000, defaultHorizon: 15 },
    { id: 'retirement', label: 'Retirement', icon: Briefcase, defaultCost: 50000000, defaultHorizon: 25 },
    { id: 'emergency', label: 'Emergency', icon: ShieldAlert, defaultCost: 500000, defaultHorizon: 1 },
    { id: 'business', label: 'Business', icon: Building2, defaultCost: 1000000, defaultHorizon: 5 },
    { id: 'car', label: 'Vehicle', icon: Car, defaultCost: 1500000, defaultHorizon: 5 },
    { id: 'custom', label: 'Custom', icon: HelpCircle, defaultCost: 1000000, defaultHorizon: 5 },
];

const IMPORTANCE_LEVELS = [
    { id: 'Critical', label: 'Critical' },
    { id: 'High', label: 'High' },
    { id: 'Medium', label: 'Medium' },
    { id: 'Low', label: 'Low' },
];

const Step4FinancialGoals = () => {
    const navigate = useNavigate();
    const { goals, addGoal, removeGoal, updateGoal } = useAssessmentStore();

    // API Integration
    const { data: goalsData, isLoading: isFetchingGoals } = useGoalsQuery();
    const { mutateAsync: addGoalApi } = useAddGoalMutation();
    const { mutateAsync: deleteGoalApi, isPending: isDeletingGoal } = useDeleteGoalMutation();

    useEffect(() => {
        if (goalsData?.length) {
            useAssessmentStore.setState({ goals: goalsData });
        }
    }, [goalsData]);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isLimitModalOpen, setIsLimitModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);

    // Retirement Auto-Fill
    const [showRetirementPanel, setShowRetirementPanel] = useState(false);
    const { data: retirementData, isLoading: isRetirementLoading } = useRetirementAutoFillQuery();
    const [isPremium] = useState(true); // TODO: restore localStorage check after testing

    // Form State
    const [type, setType] = useState(GOAL_TYPES[0].id);
    const [name, setName] = useState('');
    const [cost, setCost] = useState('');
    const [horizon, setHorizon] = useState('');
    const [currentSavings, setCurrentSavings] = useState('');
    const [inflation, setInflation] = useState('6');
    const [importance, setImportance] = useState('High');

    const openModal = (goalType = null, goalToEdit = null) => {
        if (!goalToEdit && goals.length >= 2) {
            setIsLimitModalOpen(true);
            return;
        }

        if (goalToEdit) {
            setEditingId(goalToEdit.id);
            setType(goalToEdit.type);
            setName(goalToEdit.name);
            setCost(goalToEdit.cost);
            setHorizon(goalToEdit.horizon);
            setCurrentSavings(goalToEdit.currentSavings || '');
            setInflation(goalToEdit.inflation || '6');
            setImportance(goalToEdit.importance || 'High');
        } else {
            setEditingId(null);
            const template = GOAL_TYPES.find(t => t.id === goalType) || GOAL_TYPES[0];
            setType(template.id);
            setName(template.label);
            setCost(template.defaultCost);
            setHorizon(template.defaultHorizon);
            setCurrentSavings('');
            setInflation('6');
            setImportance('High');
        }
        setIsModalOpen(true);
    };

    const handleSave = async () => {
        const goalData = {
            type,
            name: name || GOAL_TYPES.find(t => t.id === type)?.label,
            cost: parseFloat(cost) || 0,
            horizon: parseInt(horizon) || 5,
            currentSavings: parseFloat(currentSavings) || 0,
            inflation: parseFloat(inflation) || 6,
            importance
        };

        if (editingId) {
            updateGoal(editingId, goalData);
            // Persist: delete old + re-add (no PUT endpoint for goals)
            try {
                await deleteGoalApi(editingId);
                const saved = await addGoalApi({ ...goalData, id: editingId });
                if (saved?.id) updateGoal(editingId, { ...goalData, id: saved.id });
            } catch (e) { console.warn('Goal update API failed:', e.message); }
        } else {
            const newGoal = { ...goalData, id: Date.now() };
            addGoal(newGoal); // optimistic
            try { await addGoalApi(newGoal); } catch (e) { console.warn('Goal API save failed:', e.message); }
        }
        setIsModalOpen(false);
    };

    // --- Calculations for Modal Projection Preview (before save) ---
    const numericCost = parseFloat(cost) || 0;
    const numericHorizon = parseInt(horizon) || 0;
    const numericSavings = parseFloat(currentSavings) || 0;
    const numericInflation = parseFloat(inflation) / 100 || 0;

    const futureCost = numericCost * Math.pow(1 + numericInflation, numericHorizon);
    const bufferedCost = futureCost * 1.20;
    const assumedReturnRate = 0.12;
    const savingsGrowth = numericSavings * Math.pow(1 + assumedReturnRate, numericHorizon);
    const gapToFill = Math.max(0, bufferedCost - savingsGrowth);

    const calculateSIP = (gap, years, annualRate = 0.12) => {
        if (gap <= 0 || years <= 0) return 0;
        const months = years * 12;
        const monthlyRate = annualRate / 12;
        if (monthlyRate === 0) return gap / months;
        return (gap * monthlyRate) / (Math.pow(1 + monthlyRate, months) - 1);
    };
    const requiredSip = calculateSIP(gapToFill, numericHorizon, assumedReturnRate);
    const requiredLumpSum = gapToFill / Math.pow(1 + assumedReturnRate, numericHorizon);

    // --- Formatters ---
    const formatToCrLakh = (value) => {
        if (value >= 10000000) return `₹${(value / 10000000).toFixed(2)} Cr`;
        if (value >= 100000) return `₹${(value / 100000).toFixed(2)} L`;
        return `₹${Math.round(value).toLocaleString('en-IN')}`;
    };

    // --- Backend Projection Data ---
    const { data: projection } = useGoalProjectionQuery();
    const totalGoals = projection?.totalGoals ?? goals.length;
    const totalAdjustedTarget = projection?.totalAdjustedTarget ?? 0;
    const totalCurrentSavingsAll = projection?.totalCurrentSavings ?? 0;
    const totalSIPRequiredAll = projection?.totalSipRequired ?? 0;
    const monthlySurplus = projection?.monthlySurplus ?? 0;
    const isAchievable = projection?.isAchievable ?? true;
    const feasibilityRemainingBuffer = projection?.remainingBuffer ?? 0;
    const feasibilityShortfall = projection?.shortfall ?? 0;

    // Build a lookup of backend-computed per-goal projections
    const goalProjectionMap = {};
    (projection?.goals || []).forEach(g => {
        goalProjectionMap[g.id] = g;
    });

    // --- Emergency Fund data from backend ---
    const efMonthlyExpenses = projection?.monthlyExpenses ?? 0;
    const efTargetMonths = projection?.emergencyTargetMonths ?? 6;
    const efTarget = projection?.emergencyFundTarget ?? 0;
    const efCurrent = projection?.emergencyFundCurrent ?? 0;
    const efGap = projection?.emergencyFundGap ?? 0;
    const efCoverage = projection?.emergencyCoverageMonths ?? 0;
    const efAggressive = projection?.emergencyAggressiveMonths ?? 0;
    const efConservative = projection?.emergencyConservativeMonths ?? 0;
    const efProgressPercent = efTarget > 0 ? Math.min(100, (efCurrent / efTarget) * 100) : 0;
    const efFullyCovered = efGap <= 0;

    if (isFetchingGoals) return <GoalsSkeleton />;

    return (
        <div className="flex flex-col h-full">
            {/* Headers */}
            <div className="mb-6">
                <h1 className="text-2xl font-bold border-b border-primary/20 pb-2 inline-block text-white mb-2">Step 4: Your Dreams, Quantified</h1>
                <p className="text-sm text-slate-400">Financial freedom isn't just about saving—it's about purposeful saving.</p>
            </div>

            {/* Carousel */}
            <div className="overflow-x-auto pb-4 -mx-4 px-4 scrollbar-hide">
                <div className="flex gap-4 w-max">
                    {GOAL_TYPES.map((t) => (
                        <button
                            key={t.id}
                            onClick={() => t.id === 'retirement' ? setShowRetirementPanel(prev => !prev) : openModal(t.id)}
                            className="flex flex-col items-center gap-2 group min-w-[80px]"
                        >
                            <div className="w-16 h-16 rounded-2xl bg-surface-dark border border-white/5 shadow-sm flex items-center justify-center group-hover:border-primary/30 group-hover:bg-primary/10 transition-all">
                                <t.icon className="w-8 h-8 text-slate-400 group-hover:text-primary transition-colors" />
                            </div>
                            <span className="text-xs font-medium text-slate-400 group-hover:text-primary whitespace-nowrap">{t.label}</span>
                        </button>
                    ))}
                </div>
            </div>

            {/* ═══ Retirement Auto-Fill Panel ═══ */}
            {showRetirementPanel && (
                <div className="mb-6 animate-fade-in">
                    {!isPremium ? (
                        /* ── Free User: Blurred Teaser ── */
                        <div className="relative bg-surface-dark rounded-2xl border border-white/5 shadow-lg overflow-hidden">
                            <div className="p-6 blur-[6px] select-none pointer-events-none">
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="bg-white/5 rounded-xl p-4"><p className="text-slate-500 text-sm">Corpus Required</p><p className="text-2xl font-bold text-white">₹11.7 Cr</p></div>
                                    <div className="bg-white/5 rounded-xl p-4"><p className="text-slate-500 text-sm">Monthly SIP</p><p className="text-2xl font-bold text-primary">₹75,000</p></div>
                                    <div className="bg-white/5 rounded-xl p-4"><p className="text-slate-500 text-sm">Gap</p><p className="text-2xl font-bold text-red-400">₹11.4 Cr</p></div>
                                    <div className="bg-white/5 rounded-xl p-4"><p className="text-slate-500 text-sm">On Track</p><p className="text-2xl font-bold text-amber-400">1.5%</p></div>
                                </div>
                            </div>
                            <div className="absolute inset-0 flex flex-col items-center justify-center bg-background-dark/60 backdrop-blur-sm">
                                <div className="w-14 h-14 bg-amber-500/10 rounded-full flex items-center justify-center mb-3 border border-amber-500/20">
                                    <Lock className="w-7 h-7 text-amber-500" />
                                </div>
                                <h3 className="text-lg font-bold text-white mb-1">Unlock Your Retirement Plan</h3>
                                <p className="text-sm text-slate-400 mb-4">See exact SIP & gap based on your real data</p>
                                <button className="px-6 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-background-dark font-bold rounded-xl shadow-[0_0_15px_rgba(245,158,11,0.3)] transition-all text-sm">
                                    <Crown className="w-4 h-4 inline mr-1.5 -mt-0.5" />
                                    Upgrade to Premium
                                </button>
                            </div>
                        </div>
                    ) : isRetirementLoading ? (
                        <div className="bg-surface-dark rounded-2xl border border-white/5 p-8 flex items-center justify-center">
                            <div className="animate-spin w-6 h-6 border-2 border-primary border-t-transparent rounded-full" />
                            <span className="ml-3 text-slate-400">Calculating your retirement plan...</span>
                        </div>
                    ) : retirementData ? (
                        /* ── Premium User: Full Retirement Analysis ── */
                        <div className="bg-surface-dark rounded-2xl border border-white/5 shadow-lg overflow-hidden">
                            {/* Header */}
                            <div className="bg-gradient-to-r from-primary/10 to-emerald-500/10 p-5">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <h3 className="font-bold text-white flex items-center gap-2 text-base">
                                            <Briefcase className="w-5 h-5 text-primary" />
                                            Retirement Goal Engine
                                        </h3>
                                        <p className="text-sm text-slate-400 mt-1">Auto-calculated from your financial data</p>
                                    </div>
                                    <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide ${
                                        retirementData.status === 'CRITICAL' ? 'bg-red-500/15 text-red-400' :
                                        retirementData.status === 'MODERATE' ? 'bg-amber-500/15 text-amber-400' :
                                        'bg-emerald-500/15 text-emerald-400'
                                    }`}>
                                        {retirementData.status === 'CRITICAL' ? '🔴 Critical Gap' :
                                         retirementData.status === 'MODERATE' ? '🟡 Moderate Gap' : '🟢 On Track'}
                                    </span>
                                </div>
                            </div>

                            <div className="p-5 space-y-4">
                                {/* Section 1: Context */}
                                <div className="grid grid-cols-3 gap-3">
                                    <div className="bg-white/5 rounded-xl p-3 text-center">
                                        <p className="text-xs text-slate-500 uppercase tracking-wide">Current Age</p>
                                        <p className="text-xl font-bold text-white mt-1">{retirementData.currentAge}</p>
                                    </div>
                                    <div className="bg-white/5 rounded-xl p-3 text-center">
                                        <p className="text-xs text-slate-500 uppercase tracking-wide">Retire At</p>
                                        <p className="text-xl font-bold text-white mt-1">{retirementData.retirementAge}</p>
                                    </div>
                                    <div className="bg-white/5 rounded-xl p-3 text-center">
                                        <p className="text-xs text-slate-500 uppercase tracking-wide">Years Left</p>
                                        <p className="text-xl font-bold text-primary mt-1">{retirementData.yearsToRetirement}</p>
                                    </div>
                                </div>

                                {/* Section 2: Future Expense */}
                                <div className="bg-white/5 rounded-xl p-4">
                                    <div className="flex items-center gap-2 mb-2">
                                        <Wallet className="w-4 h-4 text-slate-400" />
                                        <p className="text-sm font-semibold text-slate-300">Your Future Monthly Expense</p>
                                    </div>
                                    <div className="flex items-baseline gap-3">
                                        <span className="text-2xl font-bold text-white">{formatToCrLakh(retirementData.futureMonthlyExpense)}</span>
                                        <span className="text-xs text-slate-500">/ month</span>
                                    </div>
                                    <p className="text-xs text-slate-500 mt-1">
                                        {formatToCrLakh(retirementData.monthlyExpense)} today → {formatToCrLakh(retirementData.futureMonthlyExpense)} in {retirementData.yearsToRetirement} years at 6% inflation
                                    </p>
                                </div>

                                {/* Section 3: Corpus Required */}
                                <div className="bg-white/5 rounded-xl p-4">
                                    <div className="flex items-center gap-2 mb-2">
                                        <Target className="w-4 h-4 text-slate-400" />
                                        <p className="text-sm font-semibold text-slate-300">Retirement Corpus Required</p>
                                    </div>
                                    <p className="text-3xl font-black text-white">{formatToCrLakh(retirementData.corpusRequired)}</p>
                                    <p className="text-xs text-slate-500 mt-1">
                                        {formatToCrLakh(retirementData.futureMonthlyExpense)} × 12 ÷ 3% withdrawal rate
                                    </p>
                                </div>

                                {/* Section 4: Current Assets */}
                                <div className="bg-white/5 rounded-xl p-4">
                                    <div className="flex items-center gap-2 mb-2">
                                        <BarChart3 className="w-4 h-4 text-slate-400" />
                                        <p className="text-sm font-semibold text-slate-300">Your Retirement Assets (Projected)</p>
                                    </div>
                                    <div className="grid grid-cols-2 gap-3">
                                        <div>
                                            <p className="text-xs text-slate-500">Current (EPF+PPF+NPS)</p>
                                            <p className="text-lg font-bold text-white">{formatToCrLakh(retirementData.currentRetirementAssets)}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-500">Future Value @ 8%</p>
                                            <p className="text-lg font-bold text-primary">{formatToCrLakh(retirementData.projectedAssets)}</p>
                                        </div>
                                    </div>
                                </div>

                                {/* Section 5: Gap Analysis */}
                                <div className={`rounded-xl p-4 border ${
                                    retirementData.status === 'CRITICAL' ? 'bg-red-500/5 border-red-500/20' :
                                    retirementData.status === 'MODERATE' ? 'bg-amber-500/5 border-amber-500/20' :
                                    'bg-emerald-500/5 border-emerald-500/20'
                                }`}>
                                    <p className="text-sm font-semibold text-slate-300 mb-2">Gap Analysis</p>
                                    <p className={`text-2xl font-black ${
                                        retirementData.status === 'CRITICAL' ? 'text-red-400' :
                                        retirementData.status === 'MODERATE' ? 'text-amber-400' : 'text-emerald-400'
                                    }`}>
                                        {retirementData.gap > 0 ? `Short by ${formatToCrLakh(retirementData.gap)}` : 'Fully Covered!'}
                                    </p>
                                    <div className="mt-3">
                                        <div className="flex justify-between text-xs mb-1">
                                            <span className="text-slate-500">On Track</span>
                                            <span className="text-slate-300 font-bold">{retirementData.onTrackPercent.toFixed(1)}%</span>
                                        </div>
                                        <div className="w-full h-2 bg-white/10 rounded-full overflow-hidden">
                                            <div className={`h-full rounded-full transition-all ${
                                                retirementData.onTrackPercent >= 80 ? 'bg-emerald-500' :
                                                retirementData.onTrackPercent >= 20 ? 'bg-amber-500' : 'bg-red-500'
                                            }`} style={{ width: `${Math.min(100, retirementData.onTrackPercent)}%` }} />
                                        </div>
                                    </div>
                                </div>

                                {/* Section 6 & 7: SIP Options */}
                                {retirementData.gap > 0 && (
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                        {/* Flat SIP */}
                                        <div className="bg-white/5 rounded-xl p-4 border border-white/5">
                                            <p className="text-xs text-slate-500 uppercase tracking-wide mb-1">Flat SIP Required</p>
                                            <p className="text-2xl font-bold text-white">{formatToCrLakh(retirementData.sipFlat)}<span className="text-sm text-slate-500 font-normal">/mo</span></p>
                                            <p className="text-xs text-slate-500 mt-1">Same amount every month for {retirementData.yearsToRetirement} years</p>
                                        </div>
                                        {/* Step-up SIP */}
                                        <div className="bg-primary/5 rounded-xl p-4 border border-primary/20">
                                            <div className="flex items-center gap-1 mb-1">
                                                <p className="text-xs text-primary uppercase tracking-wide font-bold">Step-Up SIP</p>
                                                <span className="text-[10px] bg-primary/20 text-primary px-1.5 py-0.5 rounded-full font-bold">RECOMMENDED</span>
                                            </div>
                                            <p className="text-2xl font-bold text-white">
                                                {formatToCrLakh(retirementData.sipStepUpStart)}<span className="text-sm text-slate-500 font-normal">/mo</span>
                                            </p>
                                            <p className="text-xs text-slate-400 mt-1">
                                                Start lower, increase {retirementData.stepUpRate}% yearly
                                            </p>
                                        </div>
                                    </div>
                                )}

                                {/* Section 8: Delay Impact */}
                                {retirementData.sipIfDelayed > 0 && retirementData.sipFlat > 0 && (
                                    <div className="bg-red-500/5 border border-red-500/15 rounded-xl p-4">
                                        <div className="flex items-center gap-2 mb-2">
                                            <Clock className="w-4 h-4 text-red-400" />
                                            <p className="text-sm font-semibold text-red-400">Delay Impact</p>
                                        </div>
                                        <p className="text-sm text-slate-300">
                                            If you delay by <span className="text-red-400 font-bold">{retirementData.delayYears} years</span>, your SIP becomes{' '}
                                            <span className="text-red-400 font-bold">{formatToCrLakh(retirementData.sipIfDelayed)}/mo</span>
                                        </p>
                                        <p className="text-xs text-slate-500 mt-1">
                                            Delay cost: +{formatToCrLakh(retirementData.sipIfDelayed - retirementData.sipFlat)}/month
                                        </p>
                                    </div>
                                )}

                                {/* Add as Goal Button */}
                                <button
                                    onClick={async () => {
                                        if (goals.length >= 2 && !isPremium) {
                                            setIsLimitModalOpen(true);
                                            return;
                                        }
                                        const retGoal = {
                                            type: 'retirement',
                                            name: 'Retirement',
                                            cost: retirementData.corpusRequired / Math.pow(1.06, retirementData.yearsToRetirement),
                                            horizon: retirementData.yearsToRetirement,
                                            currentSavings: retirementData.currentRetirementAssets,
                                            inflation: '6',
                                            importance: 'Critical',
                                        };
                                        try {
                                            const saved = await addGoalApi(retGoal);
                                            addGoal(saved);
                                            toast.success('Retirement goal added!');
                                            setShowRetirementPanel(false);
                                        } catch {
                                            toast.error('Failed to add goal');
                                        }
                                    }}
                                    className="w-full py-3 bg-primary hover:bg-primary/90 text-background-dark font-bold rounded-xl transition-all flex items-center justify-center gap-2"
                                >
                                    <ArrowUpRight className="w-4 h-4" />
                                    Add Retirement Goal
                                </button>
                            </div>
                        </div>
                    ) : null}
                </div>
            )}

            {/* ═══ Emergency Fund Status Card ═══ */}
            {efMonthlyExpenses > 0 && (() => {
                const statusColor = efFullyCovered ? 'emerald' : efProgressPercent >= 50 ? 'amber' : 'red';
                const statusLabel = efFullyCovered ? 'Fully Covered' : efProgressPercent >= 50 ? 'Partially Covered' : 'Needs Attention';
                const formatMonths = (m) => {
                    const rounded = Math.ceil(m);
                    return `${rounded} ${rounded === 1 ? 'month' : 'months'}`;
                };

                return (
                <div className="mb-6 bg-surface-dark rounded-2xl border border-white/5 shadow-lg overflow-hidden">
                    {/* Header */}
                    <div className="bg-gradient-to-r from-emerald-500/10 to-teal-500/10 p-5">
                        <div className="flex items-center justify-between">
                            <div>
                                <h3 className="font-bold text-white flex items-center gap-2 text-base">
                                    <ShieldAlert className="w-5 h-5 text-emerald-400" />
                                    Emergency Fund Status
                                </h3>
                                <p className="text-sm text-slate-400 mt-1">
                                    You need <span className="text-white font-semibold">{efTargetMonths} months</span> of expenses
                                    <span className="text-slate-500 ml-1">
                                        ({efTargetMonths === 9 ? 'Business / Self-Employed' : 'Salaried / Retired'})
                                    </span>
                                </p>
                            </div>
                            {/* Status badge */}
                            <div className={`px-3 py-1.5 rounded-full text-xs font-bold border
                                ${statusColor === 'emerald' ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30' : ''}
                                ${statusColor === 'amber' ? 'bg-amber-500/15 text-amber-400 border-amber-500/30' : ''}
                                ${statusColor === 'red' ? 'bg-red-500/15 text-red-400 border-red-500/30' : ''}
                            `}>
                                {statusLabel}
                            </div>
                        </div>
                    </div>

                    {/* Key numbers */}
                    <div className="grid grid-cols-3 divide-x divide-white/5 border-y border-white/5">
                        <div className="p-4 text-center">
                            <p className="text-xs text-slate-400 mb-1">Monthly Expenses</p>
                            <p className="text-white font-bold text-base">₹{Math.round(efMonthlyExpenses).toLocaleString('en-IN')}</p>
                            <p className="text-[10px] text-slate-500 mt-0.5">from your cash flow</p>
                        </div>
                        <div className="p-4 text-center">
                            <p className="text-xs text-slate-400 mb-1">Target Corpus</p>
                            <p className="text-white font-bold text-base">{formatToCrLakh(efTarget)}</p>
                            <p className="text-[10px] text-slate-500 mt-0.5">{efTargetMonths} × monthly expenses</p>
                        </div>
                        <div className="p-4 text-center">
                            <p className="text-xs text-slate-400 mb-1">Liquid Assets</p>
                            <p className={`font-bold text-base ${efCurrent > 0 ? 'text-emerald-400' : 'text-red-400'}`}>{formatToCrLakh(efCurrent)}</p>
                            <p className="text-[10px] text-slate-500 mt-0.5">FDs, Savings, Debt MFs</p>
                        </div>
                    </div>

                    {/* Coverage progress */}
                    <div className="p-5">
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-sm text-slate-400">Coverage</span>
                            <span className="text-sm font-semibold text-white">
                                {efCoverage.toFixed(1)} / {efTargetMonths} months
                            </span>
                        </div>
                        <div className="h-3 w-full bg-background-dark rounded-full overflow-hidden">
                            <div
                                className={`h-full rounded-full transition-all duration-1000 ${efFullyCovered ? 'bg-emerald-500' : 'bg-gradient-to-r from-emerald-500 to-primary'}`}
                                style={{ width: `${efProgressPercent}%` }}
                            />
                        </div>
                        {!efFullyCovered && efGap > 0 && (
                            <p className="text-xs text-amber-400 mt-2 font-medium">
                                You need {formatToCrLakh(efGap)} more to reach your target
                            </p>
                        )}
                    </div>

                    {/* Scenarios */}
                    {efFullyCovered ? (
                        <div className="mx-5 mb-5 bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4">
                            <p className="text-emerald-400 font-bold text-sm flex items-center gap-2">
                                <CheckCircle2 className="w-5 h-5" />
                                Emergency fund fully covered! 🎉
                            </p>
                            <p className="text-sm text-emerald-400/80 mt-1.5 ml-7">
                                Your liquid assets cover {efCoverage.toFixed(1)} months of expenses — exceeding the {efTargetMonths}-month target.
                            </p>
                        </div>
                    ) : (
                        <div className="px-5 pb-5 space-y-3">
                            <p className="text-xs uppercase tracking-wider font-bold text-slate-500">
                                How to build it
                            </p>
                            {monthlySurplus > 0 ? (
                                <div className="grid grid-cols-2 gap-3">
                                    <div className="bg-emerald-500/5 rounded-xl p-4 border border-emerald-500/15">
                                        <p className="text-sm font-bold text-white">Aggressive — Save</p>
                                        <p className="text-xs text-slate-400 mt-1">
                                            Save ₹{Math.round(monthlySurplus).toLocaleString('en-IN')}/month
                                        </p>
                                        <p className="text-emerald-400 font-bold text-xs mt-1">
                                            → ready in {formatMonths(efAggressive)}
                                        </p>
                                    </div>
                                    <div className="bg-blue-500/5 rounded-xl p-4 border border-blue-500/15">
                                        <p className="text-sm font-bold text-white">Conservative — Save</p>
                                        <p className="text-xs text-slate-400 mt-1">
                                            Save ₹{Math.round(monthlySurplus * 0.5).toLocaleString('en-IN')}/month
                                        </p>
                                        <p className="text-blue-400 font-bold text-xs mt-1">
                                            → ready in {formatMonths(efConservative)}
                                        </p>
                                    </div>
                                </div>
                            ) : (
                                <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-4">
                                    <p className="text-sm text-red-400 flex items-start gap-2">
                                        <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                                        <span>Your monthly surplus is ₹0. Go back to <span className="font-bold text-white">Step 2</span> to increase income or reduce expenses to start building this fund.</span>
                                    </p>
                                </div>
                            )}
                        </div>
                    )}
                </div>
                );
            })()}

            {/* Goals List */}
            <div className="flex-1 space-y-4 overflow-y-auto pb-4 pt-2">
                {goals.length === 0 && (
                    <div className="text-center py-10 text-slate-500">
                        <p>No goals added yet.</p>
                        <p className="text-sm">Select a goal above to get started.</p>
                    </div>
                )}
                {goals.map((goal) => {
                    const proj = goalProjectionMap[goal.id];
                    const gBufferedCost = proj?.bufferedCost ?? 0;
                    const gSavings = proj?.currentSavings ?? (goal.currentSavings || 0);
                    const sip = proj?.requiredSip ?? 0;
                    const progressPercent = proj?.progressPercent ?? 0;
                    const Icon = GOAL_TYPES.find(t => t.id === goal.type)?.icon || Home;

                    return (
                        <div key={goal.id} className="bg-surface-dark rounded-2xl p-5 border border-white/5 shadow-sm relative overflow-hidden">
                            <div className="flex justify-between items-start mb-4">
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <Icon className="w-5 h-5 text-slate-400" />
                                        <h3 className="font-bold text-white uppercase text-sm tracking-wide">{goal.name}</h3>
                                        <span className="text-[10px] uppercase px-2 py-0.5 rounded bg-white/5 text-slate-400">[{goal.importance || 'High'} Priority]</span>
                                    </div>
                                    <p className="text-xs text-slate-400">Target: {formatToCrLakh(gBufferedCost)} ({new Date().getFullYear() + goal.horizon})</p>
                                    <p className="text-xs text-slate-400">Current: {formatToCrLakh(goal.currentSavings || 0)}</p>
                                </div>
                                <button onClick={() => openModal(null, goal)} className="text-primary text-sm font-semibold hover:underline bg-primary/10 px-3 py-1 rounded-full">
                                    Edit
                                </button>
                            </div>

                            <div className="mb-4">
                                <div className="h-2 w-full bg-background-dark rounded-full overflow-hidden">
                                    <div className="h-full bg-primary rounded-full" style={{ width: `${Math.min(100, progressPercent)}%` }}></div>
                                </div>
                                <div className="flex justify-between items-center mt-1">
                                    <span className="text-[10px] text-slate-500">Progress: {progressPercent.toFixed(1)}% complete</span>
                                    <span className="text-[10px] text-slate-300">{formatToCrLakh(gBufferedCost - gSavings)} to go</span>
                                </div>
                            </div>

                            <div className="mt-4 p-3 bg-background-dark/50 rounded-xl border border-white/5">
                                <p className="text-xs text-slate-400 mb-1">Required Monthly SIP</p>
                                <p className="font-bold text-white text-lg">₹ {Math.round(sip).toLocaleString('en-IN')}</p>
                                <div className="mt-2 text-xs">
                                    <p className="text-slate-500">Currently Saved: {formatToCrLakh(goal.currentSavings || 0)}</p>
                                    {sip > 0 && (
                                        <p className="text-amber-400 flex items-center gap-1 mt-0.5">
                                            <AlertTriangle className="w-3 h-3" /> Shortfall: ₹{Math.round(sip).toLocaleString('en-IN')}/month
                                        </p>
                                    )}
                                </div>
                            </div>

                            <button
                                onClick={async () => {
                                    try {
                                        await deleteGoalApi(goal.id);
                                        toast.success('Goal deleted successfully');
                                    } catch (error) {
                                        console.error('Failed to delete goal:', error);
                                        toast.error('Failed to delete goal');
                                    }
                                }}
                                disabled={isDeletingGoal}
                                className="absolute top-4 right-20 text-slate-500 hover:text-red-400 p-1 text-xs underline disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {isDeletingGoal ? 'Deleting...' : 'Delete'}
                            </button>
                        </div>
                    );
                })}

                {/* All Goals Summary */}
                {goals.length >= 2 && (
                    <div className="mt-8 bg-surface-dark border-2 border-primary/20 rounded-2xl overflow-hidden shadow-lg">
                        <div className="bg-primary/10 p-3 border-b border-primary/10">
                            <h3 className="font-bold text-primary text-center tracking-widest text-sm">ALL GOALS SUMMARY</h3>
                        </div>
                        <div className="p-5 space-y-4">
                            <div className="grid grid-cols-2 gap-4 text-sm">
                                <div className="text-slate-400">Total Goals</div>
                                <div className="text-right font-bold text-white">{totalGoals}</div>

                                <div className="text-slate-400">Total Target (Adjusted)</div>
                                <div className="text-right font-bold text-white">{formatToCrLakh(totalAdjustedTarget)}</div>

                                <div className="text-slate-400">Current Savings</div>
                                <div className="text-right font-bold text-white">{formatToCrLakh(totalCurrentSavingsAll)}</div>

                                <div className="text-slate-400">Total SIP Required</div>
                                <div className="text-right font-bold text-primary">₹ {Math.round(totalSIPRequiredAll).toLocaleString('en-IN')}</div>
                            </div>

                            <div className="border-t border-white/10 pt-4 mt-2">
                                <h4 className="font-bold text-white mb-3 text-xs tracking-wider">FEASIBILITY CHECK</h4>

                                <div className="space-y-2 text-sm mb-4">
                                    <div className="flex justify-between">
                                        <span className="text-slate-400">Your Monthly Surplus</span>
                                        <span className="font-bold text-white">₹ {Math.round(monthlySurplus).toLocaleString('en-IN')}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-400">Total SIP Needed</span>
                                        <span className="font-bold text-white">₹ {Math.round(totalSIPRequiredAll).toLocaleString('en-IN')}</span>
                                    </div>
                                    <div className="border-t border-white/5 pt-2 flex justify-between">
                                        <span className="text-slate-400">Remaining Buffer</span>
                                        <span className={`font-bold ${feasibilityRemainingBuffer >= 0 ? 'text-primary' : 'text-red-400'}`}>
                                            {feasibilityRemainingBuffer >= 0 ? `₹ ${Math.round(feasibilityRemainingBuffer).toLocaleString('en-IN')}` : `-₹ ${Math.round(Math.abs(feasibilityRemainingBuffer)).toLocaleString('en-IN')}`}
                                        </span>
                                    </div>
                                </div>

                                {isAchievable ? (
                                    <div className="bg-primary/10 border border-primary/20 rounded-lg p-3">
                                        <p className="font-bold text-primary flex items-start gap-2 text-sm">
                                            <CheckCircle2 className="w-4 h-4 mt-0.5 shrink-0" />
                                            Your goals are achievable!
                                        </p>
                                        <p className="text-xs text-primary/80 mt-1 ml-6">
                                            You can invest ₹{Math.round(totalSIPRequiredAll / 1000).toLocaleString('en-IN')}k/month and still have ₹{Math.round(feasibilityRemainingBuffer / 1000).toLocaleString('en-IN')}k for other needs.
                                        </p>
                                    </div>
                                ) : (
                                    <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-3">
                                        <p className="font-bold text-red-400 flex items-start gap-2 text-sm">
                                            <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                                            YOUR GOALS EXCEED CURRENT CAPACITY
                                        </p>
                                        <div className="text-xs text-red-400/80 mt-2 ml-6 space-y-1">
                                            <p>Shortfall: <span className="font-bold">₹{Math.round(feasibilityShortfall).toLocaleString('en-IN')}/month</span></p>
                                            <p className="mt-2 font-bold text-white">OPTIONS TO CONSIDER:</p>
                                            <ul className="list-disc pl-4 space-y-1 text-slate-300">
                                                <li>Extend timelines (e.g., buy home in 20 years instead of 15)</li>
                                                <li>Reduce target amounts</li>
                                                <li>Prioritize critical goals only (retirement, emergency fund)</li>
                                                <li>Increase income or reduce expenses</li>
                                            </ul>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* Bottom Navigation */}
            <div className="fixed bottom-0 left-0 right-0 bg-background-dark/80 backdrop-blur-lg border-t border-white/5 p-4 z-50">
                <div className="w-full px-6 flex items-center justify-between">
                    <button
                        onClick={() => navigate('/assessment/step-3')}
                        className="px-6 py-3 bg-surface-dark hover:bg-surface-active text-white font-bold text-sm rounded-xl transition-all"
                    >
                        Back
                    </button>
                    <div className="flex items-center gap-3">
                        <div className="hidden sm:flex items-center gap-2 px-4 py-3 bg-surface-dark border border-white/10 rounded-xl">
                            <CheckCircle2 className="w-4 h-4 text-primary" />
                            <span className="text-sm font-semibold text-slate-400">Step 4/6</span>
                        </div>
                        <button
                            onClick={() => {
                                if (goals.length === 0) {
                                    toast.error('Add at least one financial goal — home, retirement, education, etc.', { id: 'step4-guide' });
                                    return;
                                }
                                navigate('/assessment/step-5');
                            }}
                            className="px-6 py-3 bg-primary hover:bg-primary-dark active:scale-[0.98] text-background-dark font-bold text-sm rounded-xl flex items-center gap-2 transition-all shadow-[0_0_15px_rgba(13,242,89,0.25)]"
                        >
                            Next <ArrowRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>

            {/* Limit Reached Modal */}
            {isLimitModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/80 backdrop-blur-sm" onClick={() => setIsLimitModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-white/10 text-center animate-scale-in">
                        <div className="w-16 h-16 bg-amber-500/10 rounded-full flex items-center justify-center mx-auto mb-4 border border-amber-500/20">
                            <Lock className="w-8 h-8 text-amber-500" />
                        </div>
                        <h3 className="text-xl font-bold text-white mb-2 uppercase tracking-wide">Free Plan Limit Reached</h3>
                        <p className="text-slate-400 text-sm mb-6">
                            You've added {goals.length} goals (free plan maximum).<br />Want to add more goals?
                        </p>

                        <div className="space-y-3">
                            <button className="w-full bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-background-dark font-bold py-3 px-4 rounded-xl shadow-[0_0_15px_rgba(245,158,11,0.3)] transition-all">
                                Upgrade to Premium
                                <span className="block text-[10px] font-medium opacity-80">(₹ 999 /year for unlimited goals)</span>
                            </button>
                            <button
                                onClick={() => setIsLimitModalOpen(false)}
                                className="w-full bg-background-dark hover:bg-white/5 text-slate-400 font-medium py-3 px-4 rounded-xl transition-all"
                            >
                                Maybe Later
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Main Add/Edit Goal Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/80 backdrop-blur-sm" onClick={() => setIsModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-lg rounded-t-3xl sm:rounded-3xl shadow-2xl border border-white/10 animate-slide-up flex flex-col max-h-[90vh]">
                        {/* Modal Header */}
                        <div className="p-5 border-b border-white/5 shrink-0 flex justify-between items-center sticky top-0 bg-surface-dark rounded-t-3xl sm:rounded-3xl z-10">
                            <h3 className="text-lg font-bold text-white flex items-center gap-2">
                                {editingId ? 'Edit Goal' : 'Add Goal'}
                            </h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white bg-white/5 rounded-full p-1">
                                <X className="w-5 h-5" />
                            </button>
                        </div>

                        {/* Modal Body (Scrollable) */}
                        <div className="p-6 overflow-y-auto space-y-6">
                            {/* Inputs Grid */}
                            <div className="space-y-4">
                                <div>
                                    <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">Goal Name</label>
                                    <input
                                        type="text"
                                        value={name}
                                        onChange={(e) => setName(e.target.value)}
                                        className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:outline-none focus:border-primary/50 transition-colors"
                                    />
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">Cost Today (₹)</label>
                                        <input
                                            type="number"
                                            value={cost}
                                            onChange={(e) => setCost(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-xl p-3 font-semibold text-white focus:outline-none focus:border-primary/50 transition-colors"
                                        />
                                    </div>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">Years to Goal</label>
                                        <input
                                            type="number"
                                            value={horizon}
                                            onChange={(e) => setHorizon(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:outline-none focus:border-primary/50 transition-colors"
                                        />
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5 line-clamp-1">Current Savings (₹)</label>
                                        <input
                                            type="number"
                                            value={currentSavings}
                                            onChange={(e) => setCurrentSavings(e.target.value)}
                                            placeholder="0"
                                            className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:outline-none focus:border-primary/50 transition-colors"
                                        />
                                    </div>
                                    <div>
                                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5 line-clamp-1">Inflation Rate (%)</label>
                                        <input
                                            type="number"
                                            value={inflation}
                                            onChange={(e) => setInflation(e.target.value)}
                                            className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:outline-none focus:border-primary/50 transition-colors"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">How important is this goal?</label>
                                    <div className="grid grid-cols-4 gap-2">
                                        {IMPORTANCE_LEVELS.map(level => (
                                            <button
                                                key={level.id}
                                                onClick={() => setImportance(level.id)}
                                                className={`py-2 px-1 text-xs rounded-lg border transition-all ${importance === level.id
                                                    ? 'bg-primary/20 border-primary text-primary font-bold'
                                                    : 'bg-background-dark border-white/5 text-slate-400 hover:border-white/20'
                                                    }`}
                                            >
                                                {level.label}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {/* Projection Summary Card */}
                            {(numericCost > 0 && numericHorizon > 0) && (
                                <div className="mt-6 border border-white/10 rounded-2xl overflow-hidden bg-background-dark">
                                    <div className="bg-white/5 py-2 text-center border-b border-white/5">
                                        <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Projection Summary</span>
                                    </div>
                                    <div className="p-4 space-y-3 text-sm">
                                        <div className="flex justify-between items-center">
                                            <span className="text-slate-400 w-1/2">Today's Cost</span>
                                            <span className="text-white font-medium text-right w-1/2">{formatToCrLakh(numericCost)}</span>
                                        </div>
                                        <div className="flex justify-between items-start">
                                            <div className="w-1/2">
                                                <span className="text-slate-400 block">In {numericHorizon} years</span>
                                                <span className="text-[10px] text-slate-500">(with {numericInflation * 100}% inflation)</span>
                                            </div>
                                            <span className="text-white font-medium text-right w-1/2 mt-0.5">{formatToCrLakh(futureCost)}</span>
                                        </div>
                                        <div className="flex justify-between items-start">
                                            <div className="w-1/2">
                                                <span className="text-slate-400 block">With Buffer +20%</span>
                                                <span className="text-[10px] text-slate-500">(for taxes & fees)</span>
                                            </div>
                                            <span className="text-white font-medium text-right w-1/2 mt-0.5">{formatToCrLakh(bufferedCost)}</span>
                                        </div>
                                        {numericSavings > 0 && (
                                            <div className="flex justify-between items-start border-t border-white/5 pt-3">
                                                <div className="w-1/2">
                                                    <span className="text-slate-400 block">Your Current Savings</span>
                                                    <span className="text-[10px] text-slate-500">Will Grow To (at 12% returns)</span>
                                                </div>
                                                <div className="text-right w-1/2 mt-0.5">
                                                    <span className="text-white text-xs line-through block opacity-50">{formatToCrLakh(numericSavings)}</span>
                                                    <span className="text-primary font-medium">{formatToCrLakh(savingsGrowth)}</span>
                                                </div>
                                            </div>
                                        )}
                                        <div className="border-t border-white/10 pt-3 mt-1 flex justify-between items-center pb-2">
                                            <span className="text-slate-300 font-bold uppercase tracking-wide">Gap To Fill</span>
                                            <span className="text-white font-bold text-lg">{formatToCrLakh(gapToFill)}</span>
                                        </div>
                                    </div>

                                    <div className="bg-primary/5 border-t border-primary/20 p-4">
                                        <p className="text-[10px] font-bold text-primary uppercase tracking-wider mb-3 text-center">To achieve this goal, you need:</p>
                                        <div className="space-y-3">
                                            <div className="flex items-start gap-3">
                                                <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center shrink-0 mt-0.5">
                                                    <TrendingUp className="w-4 h-4 text-primary" />
                                                </div>
                                                <div>
                                                    <p className="font-bold text-white tracking-wide">₹ {Math.round(requiredSip).toLocaleString('en-IN')}/month <span className="text-xs text-slate-400 font-normal">(SIP)</span></p>
                                                    <p className="text-xs text-slate-400">for next {numericHorizon} years</p>
                                                </div>
                                            </div>

                                            <div className="relative py-2">
                                                <div className="absolute inset-0 flex items-center">
                                                    <div className="w-full border-t border-white/10"></div>
                                                </div>
                                                <div className="relative flex justify-center text-[10px] uppercase font-bold text-slate-500">
                                                    <span className="bg-primary/5 px-2 rounded">OR</span>
                                                </div>
                                            </div>

                                            <div className="flex items-start gap-3">
                                                <div className="w-8 h-8 rounded-full bg-amber-500/20 flex items-center justify-center shrink-0 mt-0.5">
                                                    <div className="text-amber-500 font-bold text-sm">₹</div>
                                                </div>
                                                <div>
                                                    <p className="font-bold text-white tracking-wide">₹ {Math.round(requiredLumpSum).toLocaleString('en-IN')} <span className="text-xs text-slate-400 font-normal">lump sum today</span></p>
                                                </div>
                                            </div>
                                            <p className="text-[10px] text-slate-500 text-center mt-3 pt-2 border-t border-white/5">
                                                Assumes 12% annual return (based on your Moderate risk)
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Modal Footer */}
                        <div className="p-4 border-t border-white/5 bg-surface-dark shrink-0 rounded-b-3xl sm:rounded-b-3xl z-10">
                            <button
                                onClick={handleSave}
                                className="w-full bg-primary hover:bg-primary-dark text-background-dark font-bold py-3.5 rounded-xl shadow-[0_0_15px_rgba(13,242,89,0.3)] active:scale-[0.98] transition-all"
                            >
                                Save Goal
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Step4FinancialGoals;
