import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, Home, Car, GraduationCap, Plane, Heart, Briefcase, TrendingUp, CheckCircle2, ShieldAlert, AlertTriangle, AlertCircle, Lock, Building2, Stethoscope, Briefcase as BusinessIcon, HelpCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useGoalsQuery, useAddGoalMutation, useDeleteGoalMutation } from '../hooks/useGoals';

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
    const { goals, addGoal, removeGoal, updateGoal, incomes, expenses, liabilities } = useAssessmentStore();

    // API Integration
    const { data: goalsData } = useGoalsQuery();
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
        } else {
            const newGoal = { ...goalData, id: Date.now() };
            addGoal(newGoal); // optimistic
            try { await addGoalApi(newGoal); } catch (e) { console.warn('Goal API save failed:', e.message); }
        }
        setIsModalOpen(false);
    };

    // --- Calculations for Projection ---
    const numericCost = parseFloat(cost) || 0;
    const numericHorizon = parseInt(horizon) || 0;
    const numericSavings = parseFloat(currentSavings) || 0;
    const numericInflation = parseFloat(inflation) / 100 || 0;

    // Future Cost = Today's Cost * (1 + Inflation Rate)^Years
    const futureCost = numericCost * Math.pow(1 + numericInflation, numericHorizon);

    // Buffered Cost = Future Cost * 1.20
    const bufferedCost = futureCost * 1.20;

    // Savings Growth = Current Savings * (1 + Return Rate)^Years
    const assumedReturnRate = 0.12;
    const savingsGrowth = numericSavings * Math.pow(1 + assumedReturnRate, numericHorizon);

    // Gap to Fill = Buffered Cost - Savings Growth
    const gapToFill = Math.max(0, bufferedCost - savingsGrowth);

    // Required SIP = Gap * (rate/12) / ((1 + rate/12)^(Years * 12) - 1)
    const calculateSIP = (gap, years, annualRate = 0.12) => {
        if (gap <= 0 || years <= 0) return 0;
        const months = years * 12;
        const monthlyRate = annualRate / 12;
        if (monthlyRate === 0) return gap / months;
        return (gap * monthlyRate) / (Math.pow(1 + monthlyRate, months) - 1);
    };
    const requiredSip = calculateSIP(gapToFill, numericHorizon, assumedReturnRate);

    // Required Lump Sum = Gap / (1 + Return Rate)^Years
    const requiredLumpSum = gapToFill / Math.pow(1 + assumedReturnRate, numericHorizon);

    // --- Formatters ---
    const formatToCrLakh = (value) => {
        if (value >= 10000000) return `₹${(value / 10000000).toFixed(2)} Cr`;
        if (value >= 100000) return `₹${(value / 100000).toFixed(2)} L`;
        return `₹${Math.round(value).toLocaleString('en-IN')}`;
    };

    // --- Monthly Surplus Calculation ---
    const calculateMonthlyIncome = () => {
        return incomes.reduce((sum, inc) => {
            const amount = parseFloat(inc.amount) || 0;
            switch (inc.frequency) {
                case 'monthly': return sum + amount;
                case 'yearly': return sum + (amount / 12);
                case 'weekly': return sum + (amount * 4.33);
                default: return sum + amount;
            }
        }, 0);
    };

    const calculateMonthlyExpenses = () => {
        return expenses.reduce((sum, exp) => {
            const amount = parseFloat(exp.amount) || 0;
            switch (exp.frequency) {
                case 'monthly': return sum + amount;
                case 'yearly': return sum + (amount / 12);
                case 'weekly': return sum + (amount * 4.33);
                default: return sum + amount;
            }
        }, 0);
    };

    const calculateMonthlyLiabilities = () => {
        return liabilities.reduce((sum, liab) => sum + (parseFloat(liab.emi) || 0), 0);
    };

    const totalMonthlyIncome = calculateMonthlyIncome();
    const totalMonthlyOutgo = calculateMonthlyExpenses() + calculateMonthlyLiabilities();
    const monthlySurplus = Math.max(0, totalMonthlyIncome - totalMonthlyOutgo);

    // --- All Goals Summary Data ---
    const totalGoals = goals.length;
    let totalAdjustedTarget = 0;
    let totalCurrentSavings = 0;
    let totalSIPRequiredAll = 0;

    goals.forEach(goal => {
        const goalFutureCost = goal.cost * Math.pow(1 + (goal.inflation / 100), goal.horizon);
        const goalBufferedCost = goalFutureCost * 1.20;
        const goalSavingsGrowth = (goal.currentSavings || 0) * Math.pow(1 + 0.12, goal.horizon);
        const goalGap = Math.max(0, goalBufferedCost - goalSavingsGrowth);

        totalAdjustedTarget += goalBufferedCost;
        totalCurrentSavings += (goal.currentSavings || 0);
        totalSIPRequiredAll += calculateSIP(goalGap, goal.horizon, 0.12);
    });

    const isAchievable = totalSIPRequiredAll <= monthlySurplus;
    const feasibilityRemainingBuffer = monthlySurplus - totalSIPRequiredAll;
    const feasibilityShortfall = totalSIPRequiredAll - monthlySurplus;


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
                            onClick={() => openModal(t.id)}
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

            {/* Goals List */}
            <div className="flex-1 space-y-4 overflow-y-auto pb-4 pt-2">
                {goals.length === 0 && (
                    <div className="text-center py-10 text-slate-500">
                        <p>No goals added yet.</p>
                        <p className="text-sm">Select a goal above to get started.</p>
                    </div>
                )}
                {goals.map((goal) => {
                    const gFutureCost = goal.cost * Math.pow(1 + (goal.inflation / 100), goal.horizon);
                    const gBufferedCost = gFutureCost * 1.20;
                    const gSavingsGrowth = (goal.currentSavings || 0) * Math.pow(1 + 0.12, goal.horizon);
                    const gGap = Math.max(0, gBufferedCost - gSavingsGrowth);
                    const sip = calculateSIP(gGap, goal.horizon, 0.12);
                    const Icon = GOAL_TYPES.find(t => t.id === goal.type)?.icon || Home;

                    const progressPercent = gBufferedCost > 0 ? ((goal.currentSavings || 0) / gBufferedCost) * 100 : 0;

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
                                    <span className="text-[10px] text-slate-300">{formatToCrLakh(gBufferedCost - (goal.currentSavings || 0))} to go</span>
                                </div>
                            </div>

                            <div className="mt-4 p-3 bg-background-dark/50 rounded-xl border border-white/5">
                                <p className="text-xs text-slate-400 mb-1">Required Monthly SIP</p>
                                <p className="font-bold text-white text-lg">₹ {Math.round(sip).toLocaleString('en-IN')}</p>
                                <div className="mt-2 text-xs">
                                    <p className="text-slate-500">Currently Investing: ₹0</p>
                                    <p className="text-amber-400 flex items-center gap-1 mt-0.5"><AlertTriangle className="w-3 h-3" /> Shortfall: ₹{Math.round(sip).toLocaleString('en-IN')}/month</p>
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
                    <div className="mt-8 bg-gradient-to-br from-surface-dark to-surface border border-primary/20 rounded-2xl overflow-hidden shadow-lg">
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
                                <div className="text-right font-bold text-white">{formatToCrLakh(totalCurrentSavings)}</div>

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
                <div className="max-w-[1200px] mx-auto flex items-center justify-between">
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
