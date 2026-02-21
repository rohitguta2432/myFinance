import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ShieldCheck, Minus, Plus, ChevronDown, CheckCircle, Circle, Check, Loader2, MapPin, AlertCircle } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useProfileQuery, useProfileMutation } from '../hooks/useProfile';

const Step1PersonalRisk = () => {
    const navigate = useNavigate();
    const {
        age, setAge,
        city, setCity,
        maritalStatus, setMaritalStatus,
        dependents, setDependents,
        childDependents, setChildDependents,
        employmentType, setEmploymentType,
        residencyStatus, setResidencyStatus,
        riskAnswers, setRiskAnswer,
        riskTolerance, setRiskTolerance
    } = useAssessmentStore();

    // API Integration
    const { data: profileData, isLoading: isFetching } = useProfileQuery();
    const { mutateAsync: saveProfileApi, isPending: isSaving } = useProfileMutation();

    // Hydrate Zustand from API on mount
    useEffect(() => {
        if (profileData) {
            if (profileData.age) setAge(profileData.age);
            if (profileData.city) setCity(profileData.city);
            if (profileData.maritalStatus) setMaritalStatus(profileData.maritalStatus);
            if (profileData.dependents !== undefined) setDependents(profileData.dependents);
            if (profileData.childDependents !== undefined) setChildDependents(profileData.childDependents);
            if (profileData.employmentType) setEmploymentType(profileData.employmentType);
            if (profileData.residencyStatus) setResidencyStatus(profileData.residencyStatus);
            if (profileData.riskAnswers) {
                Object.entries(profileData.riskAnswers).forEach(([qId, score]) => {
                    setRiskAnswer(parseInt(qId), score);
                });
            }
            if (profileData.riskTolerance) setRiskTolerance(profileData.riskTolerance);
        }
    }, [profileData]);

    // Local state for dropdowns and accordion
    const [openDropdown, setOpenDropdown] = useState(null);
    const [expandedQuestion, setExpandedQuestion] = useState(1);

    // Risk Questions Data — 5 scored questions
    const questions = [
        {
            id: 1,
            text: "If your investment dropped 20% tomorrow, would you:",
            options: [
                { id: 'panic', text: 'Panic and sell immediately', score: 1 },
                { id: 'hold', text: 'Hold steady and wait', score: 2 },
                { id: 'buy', text: 'Buy more at lower price', score: 3 }
            ]
        },
        {
            id: 2,
            text: "Your investment timeline for most goals:",
            options: [
                { id: 'short', text: 'Less than 3 years', score: 1 },
                { id: 'medium', text: '3 to 7 years', score: 2 },
                { id: 'long', text: '7+ years', score: 3 }
            ]
        },
        {
            id: 3,
            text: "What do you prioritize most:",
            options: [
                { id: 'protect', text: 'Capital protection first', score: 1 },
                { id: 'balanced', text: 'Balanced growth', score: 2 },
                { id: 'aggressive', text: 'Aggressive growth', score: 3 }
            ]
        },
        {
            id: 4,
            text: "Market volatility makes you:",
            options: [
                { id: 'anxious', text: 'Very anxious', score: 1 },
                { id: 'uncomfortable', text: 'Somewhat uncomfortable', score: 2 },
                { id: 'unfazed', text: 'Unfazed', score: 3 }
            ]
        },
        {
            id: 5,
            text: "Your investment knowledge level:",
            options: [
                { id: 'beginner', text: 'Beginner', score: 1 },
                { id: 'intermediate', text: 'Intermediate', score: 2 },
                { id: 'advanced', text: 'Advanced', score: 3 }
            ]
        }
    ];

    // Compute risk score (5–15 raw → mapped to 0–10 scale)
    const allRiskAnswered = Object.keys(riskAnswers).length === 5;
    const rawRiskScore = useMemo(() => {
        if (!allRiskAnswered) return 0;
        return Object.values(riskAnswers).reduce((sum, s) => sum + s, 0);
    }, [riskAnswers, allRiskAnswered]);

    // Map 5–15 → 0–10
    const mappedScore = useMemo(() => {
        if (!allRiskAnswered) return 0;
        return Math.round(((rawRiskScore - 5) / 10) * 10);
    }, [rawRiskScore, allRiskAnswered]);

    const riskProfile = useMemo(() => {
        if (!allRiskAnswered) return null;
        if (mappedScore <= 3) return {
            label: 'Conservative',
            equity: 20, debt: 60, gold: 10, reits: 10,
            color: 'text-blue-400'
        };
        if (mappedScore <= 6) return {
            label: 'Moderate',
            equity: 50, debt: 30, gold: 5, reits: 15,
            color: 'text-yellow-400'
        };
        return {
            label: 'Aggressive',
            equity: 70, debt: 15, gold: 5, reits: 10,
            color: 'text-red-400'
        };
    }, [mappedScore, allRiskAnswered]);

    // Update riskTolerance when profile is computed
    useEffect(() => {
        if (riskProfile) {
            setRiskTolerance(riskProfile.label.toLowerCase());
        }
    }, [riskProfile]);

    // Handle risk answer
    const handleRiskAnswer = (qId, option) => {
        setRiskAnswer(qId, option.score);
        // Auto-advance to next question
        if (qId < questions.length) {
            setTimeout(() => setExpandedQuestion(qId + 1), 300);
        } else {
            // Last question answered — collapse it to show result
            setTimeout(() => setExpandedQuestion(qId + 1), 300);
        }
    };

    // Mandatory fields validation
    const isFormValid = useMemo(() => {
        return (
            age && parseInt(age) >= 18 &&
            city.trim() !== '' &&
            maritalStatus !== '' &&
            employmentType !== '' &&
            residencyStatus !== '' &&
            allRiskAnswered
        );
    }, [age, city, maritalStatus, employmentType, residencyStatus, allRiskAnswered]);

    const handleNext = async () => {
        if (!isFormValid) return;
        try {
            await saveProfileApi({
                age, city, maritalStatus, dependents, childDependents,
                employmentType, residencyStatus, riskAnswers, riskTolerance
            });
        } catch (err) {
            console.warn('API save failed, continuing with local data:', err.message);
        }
        navigate('/assessment/step-2');
    };

    // Reset childDependents if dependents drops below it
    useEffect(() => {
        if (childDependents > dependents) {
            setChildDependents(Math.max(0, dependents));
        }
    }, [dependents]);

    return (
        <div className="flex flex-col gap-6 pb-24">
            {/* Section A: Personal Profile */}
            <section className="space-y-5">
                <div className="flex items-center gap-2 mb-2">
                    <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-xs font-bold border border-primary/30">A</span>
                    <h3 className="text-white text-lg font-bold leading-tight">Personal Profile</h3>
                </div>

                <div className="lg:grid lg:grid-cols-2 lg:gap-6 space-y-5 lg:space-y-0">

                    {/* Age Slider */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm">
                        <div className="flex justify-between items-center mb-4">
                            <label className="text-sm font-medium text-slate-400">Your Age <span className="text-red-400">*</span></label>
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

                    {/* Current City — free text input */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm space-y-3">
                        <label className="text-sm font-medium text-slate-400 block">Current City <span className="text-red-400">*</span></label>
                        <div className="relative">
                            <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                            <input
                                type="text"
                                value={city}
                                onChange={(e) => setCity(e.target.value)}
                                placeholder="e.g. Mumbai, Jaipur, Coimbatore..."
                                className="w-full bg-surface-active border border-white/10 rounded-lg py-3 pl-9 pr-4 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/30 transition-all"
                            />
                        </div>
                    </div>

                    {/* Marital Status — 4 options */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm space-y-3">
                        <label className="text-sm font-medium text-slate-400 block">Marital Status <span className="text-red-400">*</span></label>
                        <div className="grid grid-cols-2 gap-2">
                            {['Single', 'Married', 'Divorced', 'Widowed'].map((status) => (
                                <button
                                    key={status}
                                    onClick={() => setMaritalStatus(status.toLowerCase())}
                                    className={`py-2.5 px-3 rounded-lg font-semibold text-sm transition-all ${maritalStatus === status.toLowerCase()
                                        ? 'bg-primary text-background-dark shadow-[0_0_15px_rgba(13,242,89,0.3)] transform scale-[1.02]'
                                        : 'bg-surface-active text-slate-300 font-medium hover:bg-white/10 border border-transparent hover:border-white/10'
                                        }`}
                                >
                                    {status}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Dependents with Children sub-question */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm space-y-4">
                        <div className="flex items-center justify-between">
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
                        {/* Children sub-question — appears when dependents >= 1 */}
                        {parseInt(dependents || 0) >= 1 && (
                            <div className="pt-3 border-t border-white/5 flex items-center justify-between animate-fade-in">
                                <div>
                                    <label className="text-sm font-medium text-slate-400 block">How many are children?</label>
                                    <p className="text-xs text-slate-500 mt-0.5">Under 18 years old</p>
                                </div>
                                <div className="flex items-center gap-3 bg-surface-active rounded-lg p-1">
                                    <button
                                        onClick={() => setChildDependents(Math.max(0, childDependents - 1))}
                                        className="w-8 h-8 flex items-center justify-center rounded bg-background-dark text-slate-400 hover:text-primary shadow-sm transition-colors"
                                    >
                                        <Minus className="w-4 h-4" />
                                    </button>
                                    <span className="w-4 text-center font-mono font-bold text-lg text-white">{childDependents}</span>
                                    <button
                                        onClick={() => setChildDependents(Math.min(parseInt(dependents || 0), childDependents + 1))}
                                        className="w-8 h-8 flex items-center justify-center rounded bg-background-dark text-primary hover:text-primary shadow-sm transition-colors"
                                    >
                                        <Plus className="w-4 h-4" />
                                    </button>
                                </div>
                            </div>
                        )}
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
                            <label className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Employment <span className="text-red-400">*</span></label>
                            <div className="flex items-center justify-between">
                                <span className={`text-sm font-medium ${employmentType ? 'text-white' : 'text-slate-500'}`}>
                                    {employmentType || 'Select...'}
                                </span>
                                <ChevronDown className="w-4 h-4 text-slate-400" />
                            </div>
                        </div>
                        {openDropdown === 'employment' && (
                            <div className="absolute top-full left-0 w-full mt-2 bg-surface-dark rounded-xl shadow-xl border border-white/10 z-20 overflow-hidden">
                                {['Salaried', 'Self-Employed', 'Business', 'Retired', 'Unemployed'].map(type => (
                                    <div
                                        key={type}
                                        onClick={() => { setEmploymentType(type); setOpenDropdown(null); }}
                                        className={`p-3 text-sm cursor-pointer transition-colors ${employmentType === type
                                            ? 'bg-primary/10 text-primary font-semibold'
                                            : 'hover:bg-surface-active text-slate-300'
                                            }`}
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
                            <label className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Residency <span className="text-red-400">*</span></label>
                            <div className="flex items-center justify-between">
                                <span className={`text-sm font-medium ${residencyStatus ? 'text-white' : 'text-slate-500'}`}>
                                    {residencyStatus || 'Select...'}
                                </span>
                                <ChevronDown className="w-4 h-4 text-slate-400" />
                            </div>
                        </div>
                        {openDropdown === 'residency' && (
                            <div className="absolute top-full left-0 w-full mt-2 bg-surface-dark rounded-xl shadow-xl border border-white/10 z-20 overflow-hidden">
                                {['Resident', 'NRI', 'OCI'].map(status => (
                                    <div
                                        key={status}
                                        onClick={() => { setResidencyStatus(status); setOpenDropdown(null); }}
                                        className={`p-3 text-sm cursor-pointer transition-colors ${residencyStatus === status
                                            ? 'bg-primary/10 text-primary font-semibold'
                                            : 'hover:bg-surface-active text-slate-300'
                                            }`}
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
                <div className="flex items-center gap-2 mb-1">
                    <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-xs font-bold border border-primary/30">B</span>
                    <h3 className="text-white text-lg font-bold leading-tight">Risk Profile</h3>
                </div>
                <div className="space-y-1 mb-4">
                    <p className="text-sm text-primary font-semibold">Answer 5 quick questions to determine your ideal investment mix.</p>
                    <p className="text-xs text-slate-400 leading-relaxed">
                        These questions help us understand if you're comfortable with market ups and downs, or prefer safe but lower returns.
                    </p>
                </div>

                {/* Questions Accordion */}
                <div className="space-y-3">
                    {questions.map((q) => {
                        const isAnswered = riskAnswers[q.id] !== undefined;
                        const selectedScore = riskAnswers[q.id];
                        return (
                            <div key={q.id} className={`rounded-xl bg-surface-dark border overflow-hidden shadow-sm transition-all ${isAnswered && expandedQuestion !== q.id ? 'border-primary/20' : 'border-white/5'
                                }`}>
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
                                            {q.options.map((opt) => {
                                                const isSelected = selectedScore === opt.score;
                                                return (
                                                    <label
                                                        key={opt.id}
                                                        onClick={() => handleRiskAnswer(q.id, opt)}
                                                        className={`flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all border ${isSelected
                                                                ? 'bg-primary/10 border-primary/30'
                                                                : 'border-transparent hover:bg-surface-active hover:border-white/5'
                                                            } group`}
                                                    >
                                                        <div className={`relative flex items-center justify-center w-5 h-5 rounded-full border-2 transition-all ${isSelected
                                                                ? 'border-primary'
                                                                : 'border-slate-500 group-hover:border-primary'
                                                            }`}>
                                                            <div className={`w-2.5 h-2.5 rounded-full bg-primary transition-opacity ${isSelected ? 'opacity-100' : 'opacity-0 group-hover:opacity-50'
                                                                }`}></div>
                                                        </div>
                                                        <span className={`text-sm transition-colors ${isSelected ? 'text-white font-medium' : 'text-slate-300 group-hover:text-white'
                                                            }`}>{opt.text}</span>
                                                    </label>
                                                );
                                            })}
                                        </div>
                                    </div>
                                ) : (
                                    // Collapsed State
                                    <div
                                        onClick={() => setExpandedQuestion(q.id)}
                                        className={`flex items-center justify-between p-4 cursor-pointer transition-colors hover:bg-surface-active/20 ${isAnswered ? '' : q.id < expandedQuestion ? 'opacity-60' : ''
                                            }`}
                                    >
                                        <p className="text-sm text-slate-400 line-clamp-1">
                                            <span className="font-bold mr-2">Q{q.id}.</span> {q.text}
                                        </p>
                                        {isAnswered ? (
                                            <CheckCircle className="w-5 h-5 text-primary shrink-0" />
                                        ) : (
                                            <Circle className="w-5 h-5 text-slate-500 shrink-0" />
                                        )}
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>

                {/* Result Card — shown when all 5 questions answered */}
                {allRiskAnswered && riskProfile && (
                    <div className="relative mt-8 animate-fade-in-up">
                        <div className="absolute inset-0 bg-primary/20 blur-xl rounded-2xl"></div>
                        <div className="relative rounded-xl bg-gradient-to-br from-surface-dark to-black border border-primary/40 p-5 shadow-2xl">
                            <div className="flex items-start justify-between mb-4">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-background-dark shadow-[0_0_15px_rgba(13,242,89,0.4)]">
                                        <Check className="w-6 h-6 font-bold" />
                                    </div>
                                    <div>
                                        <p className="text-xs text-primary font-bold uppercase tracking-wide mb-0.5">Your Risk Profile</p>
                                        <h4 className="text-white text-lg font-bold">
                                            {riskProfile.label} Investor
                                        </h4>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <p className="text-xs text-slate-500 mb-0.5">Risk Score</p>
                                    <p className="text-2xl font-mono font-bold text-primary">{mappedScore}<span className="text-sm text-slate-400">/10</span></p>
                                </div>
                            </div>
                            <div className="h-px w-full bg-gradient-to-r from-transparent via-white/10 to-transparent my-4"></div>
                            <div className="space-y-3">
                                <p className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Recommended Asset Allocation</p>
                                {/* Stacked bar */}
                                <div className="flex h-3.5 w-full rounded-full overflow-hidden">
                                    <div className="bg-primary h-full transition-all duration-700" style={{ width: `${riskProfile.equity}%` }}></div>
                                    <div className="bg-blue-500 h-full transition-all duration-700" style={{ width: `${riskProfile.debt}%` }}></div>
                                    <div className="bg-yellow-500 h-full transition-all duration-700" style={{ width: `${riskProfile.gold}%` }}></div>
                                    <div className="bg-purple-500 h-full transition-all duration-700" style={{ width: `${riskProfile.reits}%` }}></div>
                                </div>
                                {/* Legend */}
                                <div className="grid grid-cols-2 gap-2 text-xs font-medium">
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-2.5 h-2.5 rounded-full bg-primary"></div>
                                        <span className="text-white">Equity {riskProfile.equity}%</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-2.5 h-2.5 rounded-full bg-blue-500"></div>
                                        <span className="text-white">Debt {riskProfile.debt}%</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-2.5 h-2.5 rounded-full bg-yellow-500"></div>
                                        <span className="text-white">Gold {riskProfile.gold}%</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-2.5 h-2.5 rounded-full bg-purple-500"></div>
                                        <span className="text-white">Real Estate (REITs) {riskProfile.reits}%</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </section>

            {/* Sticky Footer CTA */}
            <div className="fixed bottom-0 left-0 w-full bg-gradient-to-t from-background-dark via-background-dark to-transparent pt-10 pb-6 px-4 z-40 max-w-4xl mx-auto right-0">
                {!isFormValid && (
                    <div className="flex items-center gap-2 mb-3 justify-center">
                        <AlertCircle className="w-4 h-4 text-slate-500" />
                        <p className="text-xs text-slate-500">Please complete all mandatory fields to continue</p>
                    </div>
                )}
                <button
                    onClick={handleNext}
                    disabled={isSaving || !isFormValid}
                    className={`w-full font-bold text-base py-4 rounded-xl flex items-center justify-center gap-2 transition-all ${isFormValid
                            ? 'bg-primary hover:bg-primary-dark active:scale-[0.98] text-background-dark shadow-[0_0_20px_rgba(13,242,89,0.3)]'
                            : 'bg-slate-700 text-slate-400 cursor-not-allowed'
                        }`}
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
