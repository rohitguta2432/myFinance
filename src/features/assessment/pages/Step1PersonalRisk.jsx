import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, ShieldCheck, Minus, Plus, ChevronDown, CheckCircle, Circle, Check, Loader2 } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useProfileQuery, useProfileMutation } from '../hooks/useProfile';

const Step1PersonalRisk = () => {
    const navigate = useNavigate();
    const {
        age, setAge,
        cityTier, setCityTier,
        maritalStatus, setMaritalStatus,
        dependents, setDependents,
        employmentType, setEmploymentType,
        residencyStatus, setResidencyStatus,
        riskTolerance, setRiskTolerance
    } = useAssessmentStore();

    // API Integration
    const { data: profileData, isLoading: isFetching } = useProfileQuery();
    const { mutateAsync: saveProfileApi, isPending: isSaving } = useProfileMutation();

    // Hydrate Zustand from API on mount
    useEffect(() => {
        if (profileData) {
            if (profileData.age) setAge(profileData.age);
            if (profileData.cityTier) setCityTier(profileData.cityTier);
            if (profileData.maritalStatus) setMaritalStatus(profileData.maritalStatus);
            if (profileData.dependents !== undefined) setDependents(profileData.dependents);
            if (profileData.employmentType) setEmploymentType(profileData.employmentType);
            if (profileData.residencyStatus) setResidencyStatus(profileData.residencyStatus);
            if (profileData.riskTolerance) setRiskTolerance(profileData.riskTolerance);
        }
    }, [profileData]);

    const handleNext = async () => {
        try {
            await saveProfileApi({ age, cityTier, maritalStatus, dependents, employmentType, residencyStatus, riskTolerance });
        } catch (err) {
            console.warn('API save failed, continuing with local data:', err.message);
        }
        navigate('/assessment/step-2');
    };

    // Local state for dropdowns and accordion
    const [openDropdown, setOpenDropdown] = useState(null);
    const [expandedQuestion, setExpandedQuestion] = useState(1);

    // Risk Questions Data
    const questions = [
        {
            id: 1,
            text: "If your investment drops by 20% in 3 months, what would you do?",
            options: [
                { id: 'panic', text: 'Sell immediately to prevent further loss', riskScore: 'low' },
                { id: 'hold', text: 'Hold and wait for recovery', riskScore: 'medium' },
                { id: 'buy', text: 'Buy more at lower prices', riskScore: 'high' }
            ]
        },
        {
            id: 2,
            text: "What is your investment horizon for the majority of your funds?",
            options: [
                { id: 'short', text: 'Less than 3 years', riskScore: 'low' },
                { id: 'medium', text: '3-7 years', riskScore: 'medium' },
                { id: 'long', text: 'More than 7 years', riskScore: 'high' }
            ]
        },
        {
            id: 3,
            text: "What describes your primary financial goal?",
            options: [
                { id: 'Capital Preservation', text: 'Capital Preservation', riskScore: 'low' },
                { id: 'Balanced Growth', text: 'Balanced Growth', riskScore: 'medium' },
                { id: 'Aggressive Growth', text: 'Aggressive Growth', riskScore: 'high' }
            ]
        },
        {
            id: 4,
            text: "How would you describe your current income stability?",
            options: [
                { id: 'unstable', text: 'Variable / Freelance income', riskScore: 'low' },
                { id: 'moderate', text: 'Stable job, some uncertainty', riskScore: 'medium' },
                { id: 'stable', text: 'Very stable (govt/MNC) job', riskScore: 'high' }
            ]
        }
    ];

    // Helper to calculate risk score (simplified)
    const handleRiskAnswer = (qId, option) => {
        if (expandedQuestion < questions.length) {
            setExpandedQuestion(expandedQuestion + 1);
        }
        setRiskTolerance(option.riskScore);
    };

    return (
        <div className="flex flex-col gap-6 pb-24">
            {/* Section A: Demographics */}
            <section className="space-y-5">
                <div className="flex items-center gap-2 mb-2">
                    <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-xs font-bold border border-primary/30">A</span>
                    <h3 className="text-white text-lg font-bold leading-tight">Demographics</h3>
                </div>

                <div className="lg:grid lg:grid-cols-2 lg:gap-6 space-y-5 lg:space-y-0">

                    {/* Age Slider */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm">
                        <div className="flex justify-between items-center mb-4">
                            <label className="text-sm font-medium text-slate-400">Your Age</label>
                            <span className="text-2xl font-mono font-bold text-primary">{age || 30}</span>
                        </div>
                        <div className="relative w-full h-6 flex items-center">
                            <input
                                type="range"
                                min="18"
                                max="75"
                                value={age || 30}
                                onChange={(e) => setAge(e.target.value)}
                                className="z-10 relative w-full"
                            />
                        </div>
                        <div className="flex justify-between mt-1 text-[10px] text-slate-400 uppercase tracking-wider font-medium">
                            <span>18</span>
                            <span>75+</span>
                        </div>
                    </div>

                    {/* City Type */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm space-y-3">
                        <label className="text-sm font-medium text-slate-400 block">Current City Tier</label>
                        <div className="grid grid-cols-3 gap-2">
                            {['Metro', 'Tier 1', 'Tier 2'].map((tier) => (
                                <button
                                    key={tier}
                                    onClick={() => setCityTier(tier)}
                                    className={`flex items-center justify-center py-2.5 px-3 rounded-lg font-bold text-sm transition-all ${cityTier === tier
                                        ? 'bg-primary text-background-dark shadow-[0_0_15px_rgba(13,242,89,0.3)] transform scale-[1.02]'
                                        : 'bg-surface-active text-slate-300 font-medium hover:bg-white/10 border border-transparent hover:border-white/10'
                                        }`}
                                >
                                    {tier}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Marital Status */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm space-y-3">
                        <label className="text-sm font-medium text-slate-400 block">Marital Status</label>
                        <div className="flex p-1 bg-surface-active rounded-lg">
                            {['Single', 'Married'].map((status) => (
                                <button
                                    key={status}
                                    onClick={() => setMaritalStatus(status.toLowerCase())}
                                    className={`flex-1 py-2 rounded-md font-semibold text-sm transition-all ${maritalStatus === status.toLowerCase()
                                        ? 'bg-background-dark text-primary shadow-sm'
                                        : 'text-slate-400 hover:text-white'
                                        }`}
                                >
                                    {status}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Dependents */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm flex items-center justify-between">
                        <div>
                            <label className="text-sm font-medium text-slate-400 block">Dependents</label>
                            <p className="text-xs text-slate-500 mt-0.5">Parents, children, spouse</p>
                        </div>
                        <div className="flex items-center gap-3 bg-surface-active rounded-lg p-1">
                            <button
                                onClick={() => setDependents(Math.max(0, parseInt(dependents || 0) - 1))}
                                className="w-8 h-8 flex items-center justify-center rounded bg-background-dark text-slate-400 hover:text-primary shadow-sm transition-colors"
                            >
                                <Minus className="w-4 h-4" />
                            </button>
                            <span className="w-4 text-center font-mono font-bold text-lg text-white">{dependents || 0}</span>
                            <button
                                onClick={() => setDependents(parseInt(dependents || 0) + 1)}
                                className="w-8 h-8 flex items-center justify-center rounded bg-background-dark text-primary hover:text-primary shadow-sm transition-colors"
                            >
                                <Plus className="w-4 h-4" />
                            </button>
                        </div>
                    </div>
                </div>

                {/* Dropdowns: Employment & Residency */}
                <div className="grid grid-cols-2 gap-3">
                    {/* Employment */}
                    <div className="relative">
                        <div
                            onClick={() => setOpenDropdown(openDropdown === 'employment' ? null : 'employment')}
                            className="rounded-xl bg-surface-dark border border-white/5 p-4 shadow-sm cursor-pointer hover:border-primary/30 transition-colors"
                        >
                            <label className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Employment</label>
                            <div className="flex items-center justify-between">
                                <span className="text-sm font-medium text-white">{employmentType}</span>
                                <ChevronDown className="w-4 h-4 text-slate-400" />
                            </div>
                        </div>
                        {openDropdown === 'employment' && (
                            <div className="absolute top-full left-0 w-full mt-2 bg-surface-dark rounded-xl shadow-xl border border-white/10 z-20 overflow-hidden">
                                {['Salaried', 'Self-Employed', 'Business', 'Retired'].map(type => (
                                    <div
                                        key={type}
                                        onClick={() => { setEmploymentType(type); setOpenDropdown(null); }}
                                        className="p-3 text-sm hover:bg-surface-active cursor-pointer text-slate-300"
                                    >
                                        {type}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Residency */}
                    <div className="relative">
                        <div
                            onClick={() => setOpenDropdown(openDropdown === 'residency' ? null : 'residency')}
                            className="rounded-xl bg-surface-dark border border-white/5 p-4 shadow-sm cursor-pointer hover:border-primary/30 transition-colors"
                        >
                            <label className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Residency</label>
                            <div className="flex items-center justify-between">
                                <span className="text-sm font-medium text-white">{residencyStatus}</span>
                                <ChevronDown className="w-4 h-4 text-slate-400" />
                            </div>
                        </div>
                        {openDropdown === 'residency' && (
                            <div className="absolute top-full left-0 w-full mt-2 bg-surface-dark rounded-xl shadow-xl border border-white/10 z-20 overflow-hidden">
                                {['Resident', 'NRI', 'OCI'].map(status => (
                                    <div
                                        key={status}
                                        onClick={() => { setResidencyStatus(status); setOpenDropdown(null); }}
                                        className="p-3 text-sm hover:bg-surface-active cursor-pointer text-slate-300"
                                    >
                                        {status}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </section>

            <div className="h-px w-full bg-white/10 my-2"></div>

            {/* Section B: Risk Profile */}
            <section className="space-y-5">
                <div className="flex items-center gap-2 mb-2">
                    <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-xs font-bold border border-primary/30">B</span>
                    <h3 className="text-white text-lg font-bold leading-tight">Risk Assessment</h3>
                </div>

                {/* Questions Accordion */}
                <div className="space-y-3">
                    {questions.map((q) => (
                        <div key={q.id} className="rounded-xl bg-surface-dark border border-white/5 overflow-hidden shadow-sm transition-all">
                            {expandedQuestion === q.id ? (
                                // Expanded State
                                <div>
                                    <div className="p-4 border-b border-white/5 bg-surface-active/30">
                                        <p className="text-sm font-medium text-white leading-relaxed">
                                            <span className="text-primary font-bold mr-1">Q{q.id}.</span>
                                            {q.text}
                                        </p>
                                    </div>
                                    <div className="p-2 space-y-1">
                                        {q.options.map((opt) => (
                                            <label
                                                key={opt.id}
                                                onClick={() => handleRiskAnswer(q.id, opt)}
                                                className="flex items-center gap-3 p-3 rounded-lg hover:bg-surface-active cursor-pointer transition-colors border border-transparent hover:border-white/5 group"
                                            >
                                                <div className="relative flex items-center justify-center w-5 h-5 rounded-full border-2 border-slate-500 group-hover:border-primary">
                                                    <div className="w-2.5 h-2.5 rounded-full bg-primary opacity-0 group-hover:opacity-100 transition-opacity"></div>
                                                </div>
                                                <span className="text-sm text-slate-300 group-hover:text-white">{opt.text}</span>
                                            </label>
                                        ))}
                                    </div>
                                </div>
                            ) : (
                                // Collapsed State
                                <div
                                    onClick={() => setExpandedQuestion(q.id)}
                                    className={`flex items-center justify-between p-4 cursor-pointer ${q.id < expandedQuestion ? 'opacity-60' : ''}`}
                                >
                                    <p className="text-sm text-slate-400 line-clamp-1">
                                        <span className="font-bold mr-2">Q{q.id}.</span> {q.text}
                                    </p>
                                    {q.id < expandedQuestion ? (
                                        <CheckCircle className="w-5 h-5 text-primary" />
                                    ) : (
                                        <Circle className="w-5 h-5 text-slate-500" />
                                    )}
                                </div>
                            )}
                        </div>
                    ))}
                </div>

                {/* Result Card (Only show if all answered) */}
                {expandedQuestion > questions.length && (
                    <div className="relative mt-8 group animate-fade-in-up">
                        <div className="absolute inset-0 bg-primary/20 blur-xl rounded-2xl"></div>
                        <div className="relative rounded-xl bg-gradient-to-br from-surface-dark to-black border border-primary/40 p-5 shadow-2xl">
                            <div className="flex items-start justify-between mb-4">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-background-dark shadow-[0_0_15px_rgba(13,242,89,0.4)]">
                                        <Check className="w-6 h-6 font-bold" />
                                    </div>
                                    <div>
                                        <p className="text-xs text-primary font-bold uppercase tracking-wide mb-0.5">Assessment Complete</p>
                                        <h4 className="text-white text-lg font-bold">
                                            {riskTolerance === 'high' ? 'Aggressive' : riskTolerance === 'low' ? 'Conservative' : 'Moderate'} Risk Profile
                                        </h4>
                                    </div>
                                </div>
                            </div>
                            <div className="h-px w-full bg-gradient-to-r from-transparent via-white/10 to-transparent my-4"></div>
                            <div className="space-y-3">
                                <p className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Recommended Asset Mix</p>
                                <div className="flex h-3 w-full rounded-full overflow-hidden">
                                    <div className="bg-primary h-full" style={{ width: '50%' }}></div>
                                    <div className="bg-blue-500 h-full" style={{ width: '30%' }}></div>
                                    <div className="bg-yellow-500 h-full" style={{ width: '20%' }}></div>
                                </div>
                                <div className="flex justify-between text-xs font-medium">
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-2 h-2 rounded-full bg-primary"></div>
                                        <span className="text-white">Equity 50%</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-2 h-2 rounded-full bg-blue-500"></div>
                                        <span className="text-white">Debt 30%</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-2 h-2 rounded-full bg-yellow-500"></div>
                                        <span className="text-white">Gold 20%</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </section>

            {/* Sticky Footer CTA */}
            <div className="fixed bottom-0 left-0 w-full bg-gradient-to-t from-background-dark via-background-dark to-transparent pt-10 pb-6 px-4 z-40 max-w-4xl mx-auto right-0">
                <button
                    onClick={handleNext}
                    disabled={isSaving}
                    className="w-full bg-primary hover:bg-primary-dark active:scale-[0.98] transition-all text-background-dark font-bold text-base py-4 rounded-xl flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(13,242,89,0.3)] disabled:opacity-60"
                >
                    {isSaving ? (
                        <><Loader2 className="w-5 h-5 animate-spin" /> Saving...</>
                    ) : (
                        <>Next: Income & Expenses <ArrowRight className="w-5 h-5 font-bold" /></>
                    )}
                </button>
            </div>
        </div>
    );
};

export default Step1PersonalRisk;
