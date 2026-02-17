import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Plus, X, Home, Car, GraduationCap, Plane, Heart, Briefcase, TrendingUp } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useGoalsQuery, useAddGoalMutation } from '../hooks/useGoals';

const GOAL_TYPES = [
    { id: 'home', label: 'Home', icon: Home, defaultCost: 5000000, defaultHorizon: 15 },
    { id: 'car', label: 'Car', icon: Car, defaultCost: 1500000, defaultHorizon: 5 },
    { id: 'education', label: 'Education', icon: GraduationCap, defaultCost: 2500000, defaultHorizon: 10 },
    { id: 'retirement', label: 'Retirement', icon: Briefcase, defaultCost: 50000000, defaultHorizon: 25 },
    { id: 'travel', label: 'Travel', icon: Plane, defaultCost: 500000, defaultHorizon: 1 },
    { id: 'wedding', label: 'Wedding', icon: Heart, defaultCost: 2000000, defaultHorizon: 3 },
];

const Step4FinancialGoals = () => {
    const navigate = useNavigate();
    const { goals, addGoal, removeGoal, updateGoal } = useAssessmentStore();

    // API Integration
    const { data: goalsData } = useGoalsQuery();
    const { mutateAsync: addGoalApi } = useAddGoalMutation();

    useEffect(() => {
        if (goalsData?.length) {
            useAssessmentStore.setState({ goals: goalsData });
        }
    }, [goalsData]);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);

    // Form State
    const [type, setType] = useState(GOAL_TYPES[0].id);
    const [name, setName] = useState('');
    const [cost, setCost] = useState('');
    const [horizon, setHorizon] = useState('');

    const openModal = (goalType = null, goalToEdit = null) => {
        if (goalToEdit) {
            setEditingId(goalToEdit.id);
            setType(goalToEdit.type);
            setName(goalToEdit.name);
            setCost(goalToEdit.cost);
            setHorizon(goalToEdit.horizon);
        } else {
            setEditingId(null);
            const template = GOAL_TYPES.find(t => t.id === goalType) || GOAL_TYPES[0];
            setType(template.id);
            setName(template.label);
            setCost(template.defaultCost);
            setHorizon(template.defaultHorizon);
        }
        setIsModalOpen(true);
    };

    const handleSave = async () => {
        const goalData = {
            type,
            name: name || GOAL_TYPES.find(t => t.id === type)?.label,
            cost: parseFloat(cost) || 0,
            horizon: parseInt(horizon) || 5,
            inflation: 6 // fixed for now
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

    // Calculations
    const calculateFutureCost = (cost, years, inflation = 0.06) => {
        return cost * Math.pow(1 + inflation, years);
    };

    const calculateSIP = (futureCost, years, rate = 0.12) => {
        const months = years * 12;
        const monthlyRate = rate / 12;
        if (monthlyRate === 0) return futureCost / months;
        return (futureCost * monthlyRate) / (Math.pow(1 + monthlyRate, months) - 1);
    };

    const totalSIPRequired = goals.reduce((sum, goal) => {
        const futureCost = calculateFutureCost(goal.cost, goal.horizon);
        return sum + calculateSIP(futureCost, goal.horizon);
    }, 0);

    return (
        <div className="flex flex-col h-full pb-32">
            {/* Carousel */}
            <div className="overflow-x-auto pb-4 -mx-4 px-4 scrollbar-hide">
                <div className="flex gap-4 w-max">
                    {GOAL_TYPES.map((t) => (
                        <button
                            key={t.id}
                            onClick={() => openModal(t.id)}
                            className="flex flex-col items-center gap-2 group min-w-[70px]"
                        >
                            <div className="w-16 h-16 rounded-2xl bg-surface-dark border border-white/5 shadow-sm flex items-center justify-center group-hover:border-primary/30 group-hover:bg-primary/10 transition-all">
                                <t.icon className="w-8 h-8 text-slate-400 group-hover:text-primary transition-colors" />
                            </div>
                            <span className="text-xs font-medium text-slate-400 group-hover:text-primary">{t.label}</span>
                        </button>
                    ))}
                </div>
            </div>

            {/* Goals List */}
            <div className="flex-1 space-y-4 overflow-y-auto pb-4">
                {goals.length === 0 && (
                    <div className="text-center py-10 text-slate-500">
                        <p>No goals added yet.</p>
                        <p className="text-sm">Select a goal above to get started.</p>
                    </div>
                )}
                {goals.map((goal) => {
                    const futureCost = calculateFutureCost(goal.cost, goal.horizon);
                    const sip = calculateSIP(futureCost, goal.horizon);
                    const Icon = GOAL_TYPES.find(t => t.id === goal.type)?.icon || Home;

                    return (
                        <div key={goal.id} className="bg-surface-dark rounded-2xl p-5 border border-white/5 shadow-sm relative overflow-hidden">
                            <div className="flex justify-between items-start mb-4">
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <Icon className="w-5 h-5 text-slate-400" />
                                        <h3 className="font-bold text-white">{goal.name}</h3>
                                    </div>
                                    <p className="text-xs text-slate-400">{goal.horizon} Years Horizon</p>
                                </div>
                                <button onClick={() => openModal(null, goal)} className="text-primary text-sm font-semibold hover:underline">
                                    Edit
                                </button>
                            </div>

                            <div className="grid grid-cols-2 gap-4 bg-background-dark rounded-xl p-3 border border-white/5">
                                <div>
                                    <p className="text-xs text-slate-400">Cost Today</p>
                                    <p className="font-bold text-white">₹ {(goal.cost / 100000).toFixed(1)} L</p>
                                </div>
                                <div>
                                    <p className="text-xs text-primary">Future Cost (+6%)</p>
                                    <p className="font-bold text-primary">₹ {(futureCost / 100000).toFixed(1)} L</p>
                                </div>
                            </div>

                            <div className="mt-4 flex items-center justify-between p-3 bg-primary/5 rounded-xl border border-primary/10">
                                <span className="text-xs font-bold text-slate-300 uppercase">Monthly SIP Required</span>
                                <span className="text-primary font-bold">₹ {Math.round(sip).toLocaleString()}</span>
                            </div>

                            <button onClick={() => removeGoal(goal.id)} className="absolute top-5 right-12 text-slate-500 hover:text-red-400 p-1">
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                    );
                })}
            </div>

            {/* Footer */}
            <div className="fixed bottom-0 left-0 w-full bg-gradient-to-t from-background-dark via-background-dark to-transparent pt-6 z-40">
                <div className="bg-surface-dark border-t border-white/5 p-5 rounded-t-3xl max-w-md mx-auto right-0 left-0">
                    <div className="flex flex-col gap-4">
                        {goals.length > 0 && (
                            <div className="flex justify-between items-center text-slate-300 text-sm px-1">
                                <span>Total Monthly Investment Needed:</span>
                                <span className="text-white font-bold text-lg">₹ {Math.round(totalSIPRequired).toLocaleString()}</span>
                            </div>
                        )}
                        <button
                            onClick={() => navigate('/assessment/step-5')}
                            className="w-full bg-primary text-background-dark font-bold text-base py-4 rounded-xl flex items-center justify-center gap-2 hover:bg-primary-dark transition-colors shadow-[0_0_20px_rgba(13,242,89,0.3)] active:scale-[0.98]"
                        >
                            Next: Insurance Coverage
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
                            <h3 className="text-xl font-bold text-white">{editingId ? 'Edit Goal' : 'Add Goal'}</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Goal Name</label>
                                <input
                                    type="text"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                />
                            </div>

                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Cost Today (₹)</label>
                                <input
                                    type="number"
                                    value={cost}
                                    onChange={(e) => setCost(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 font-bold text-lg text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                />
                            </div>

                            <div>
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Years to Goal</label>
                                <input
                                    type="number"
                                    value={horizon}
                                    onChange={(e) => setHorizon(e.target.value)}
                                    className="w-full bg-background-dark border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                />
                                <p className="text-xs text-slate-500 mt-1">Inflation assumed at 6%</p>
                            </div>

                            <button
                                onClick={handleSave}
                                className="w-full bg-primary hover:bg-primary-dark text-background-dark font-bold py-4 rounded-xl mt-4 shadow-[0_0_15px_rgba(13,242,89,0.3)] active:scale-[0.98] transition-all"
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
