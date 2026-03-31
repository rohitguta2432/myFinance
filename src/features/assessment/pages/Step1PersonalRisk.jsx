import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ShieldCheck, Minus, Plus, ChevronDown, CheckCircle, CheckCircle2, Circle, Check, Loader2, MapPin, AlertCircle, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useProfileQuery, useProfileMutation } from '../hooks/useProfile';
import { ProfileSkeleton } from '../../../components/ui/AssessmentSkeleton';
import SectionNav from '../../dashboard/components/SectionNav';

const STEP1_SECTIONS = [
    { id: 'profile', label: 'Profile' },
    { id: 'investor', label: 'Investor Style' },
];

const Step1PersonalRisk = () => {
    const navigate = useNavigate();
    const {
        age, setAge,
        state, setState,
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
            if (profileData.state) setState(profileData.state);
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

    // Location data from backend
    const [statesList, setStatesList] = useState([]);
    const [citiesList, setCitiesList] = useState([]);
    const [stateSearch, setStateSearch] = useState('');
    const [citySearch, setCitySearch] = useState('');
    const [loadingStates, setLoadingStates] = useState(false);
    const [loadingCities, setLoadingCities] = useState(false);
    const stateSearchRef = useRef(null);
    const citySearchRef = useRef(null);

    // Fetch states from backend on mount
    useEffect(() => {
        setLoadingStates(true);
        fetch('/api/v1/location/states')
            .then(res => res.json())
            .then(data => setStatesList(data || []))
            .catch(err => console.error('Failed to load states:', err))
            .finally(() => setLoadingStates(false));
    }, []);

    // Fetch cities when state changes
    useEffect(() => {
        if (!state) {
            setCitiesList([]);
            return;
        }
        setLoadingCities(true);
        fetch(`/api/v1/location/cities?state=${encodeURIComponent(state)}`)
            .then(res => res.json())
            .then(data => setCitiesList(data || []))
            .catch(err => console.error('Failed to load cities:', err))
            .finally(() => setLoadingCities(false));
    }, [state]);

    // Auto-focus search input when dropdown opens
    useEffect(() => {
        if (openDropdown === 'state') stateSearchRef.current?.focus();
        if (openDropdown === 'city') citySearchRef.current?.focus();
    }, [openDropdown]);

    const filteredStates = statesList.filter(s =>
        s.toLowerCase().includes(stateSearch.toLowerCase())
    );
    const filteredCities = citiesList.filter(c =>
        c.toLowerCase().includes(citySearch.toLowerCase())
    );
    const [expandedQuestion, setExpandedQuestion] = useState(1);

    // ── 7 New Risk Questions ─────────────────────────────────
    const questions = [
        {
            id: 1,
            text: "Your ₹1 lakh investment drops to ₹80,000 in a month. What do you do?",
            options: [
                { id: 'sell', text: 'I sell everything and move it to FD or savings', score: 1 },
                { id: 'hold', text: 'I do nothing and wait for it to come back', score: 2 },
                { id: 'buy', text: 'I invest more while prices are cheap', score: 3 }
            ]
        },
        {
            id: 2,
            text: "When are you most likely to need the money you are investing today?",
            options: [
                { id: 'short', text: 'Within the next 3 years', score: 1 },
                { id: 'medium', text: 'In 3 to 7 years', score: 2 },
                { id: 'long', text: '7 or more years from now', score: 3 }
            ]
        },
        {
            id: 3,
            text: "If you had ₹5 lakhs to invest today, what matters most to you?",
            options: [
                { id: 'safety', text: "That I don't lose any of it — growth can wait", score: 1 },
                { id: 'balanced', text: "Some growth is fine, but I don't want big swings", score: 2 },
                { id: 'growth', text: 'Maximum growth, even if it means big ups and downs', score: 3 }
            ]
        },
        {
            id: 4,
            text: "Imagine your portfolio crashes 40% in a recession. What do you do?",
            options: [
                { id: 'exit', text: "I exit completely — I can't handle this", score: 1 },
                { id: 'wait', text: 'I stay put and wait for recovery', score: 2 },
                { id: 'invest', text: 'I see this as a buying opportunity and invest more', score: 3 }
            ]
        },
        {
            id: 5,
            text: "You're offered: A guaranteed 7% return, or a chance at 15% but could be −5%. Pick one.",
            options: [
                { id: 'guaranteed', text: 'Give me the guaranteed 7%', score: 1 },
                { id: 'split', text: "I'd split — some safe, some risky", score: 2 },
                { id: 'chance', text: "I'll take the 15% chance — I can handle a bad year", score: 3 }
            ]
        },
        {
            id: 6,
            text: "If your EMI increases and money gets tight, what do you cut first?",
            options: [
                { id: 'investments', text: 'My investments — safety first', score: 1 },
                { id: 'adjust', text: 'I adjust both lifestyle and investments', score: 2 },
                { id: 'keep', text: "I don't touch my investments — I find other ways", score: 3 }
            ]
        },
        {
            id: 7,
            text: "Would you be comfortable putting money in crypto or startups?",
            options: [
                { id: 'no', text: 'Absolutely not — too risky', score: 1 },
                { id: 'small', text: 'Maybe a small percentage', score: 2 },
                { id: 'yes', text: 'Yes, I like high-risk high-reward bets', score: 3 }
            ]
        }
    ];

    // All 7 questions must be answered to proceed
    const allRiskAnswered = Object.keys(riskAnswers).length === 7;
    // Scoring is handled by the backend (GET /api/v1/risk-scoring)

    // Handle risk answer
    const handleRiskAnswer = (qId, option) => {
        setRiskAnswer(qId, option.score);
        // Auto-advance to next question
        if (qId < questions.length) {
            setTimeout(() => setExpandedQuestion(qId + 1), 300);
        }
    };

    // Mandatory fields validation
    const isFormValid = useMemo(() => {
        return (
            age && parseInt(age) >= 18 &&
            state.trim() !== '' &&
            city.trim() !== '' &&
            maritalStatus !== '' &&
            employmentType !== '' &&
            residencyStatus !== '' &&
            allRiskAnswered
        );
    }, [age, state, city, maritalStatus, employmentType, residencyStatus, allRiskAnswered]);

    const handleNext = async () => {
        if (!isFormValid) return;
        try {
            await saveProfileApi({
                age, state, city, maritalStatus, dependents, childDependents,
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

    if (isFetching) return <ProfileSkeleton />;

    return (
        <div className="flex flex-col gap-6 pb-24">
            <SectionNav sections={STEP1_SECTIONS} />
            {/* Section A: Personal Profile */}
            <section id="profile" className="space-y-5">
                <div className="flex items-center gap-2 mb-2">
                    <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-xs font-bold border border-primary/30">A</span>
                    <h3 className="text-white text-lg font-bold leading-tight">Personal Profile</h3>
                </div>

                <div className="lg:grid lg:grid-cols-2 lg:gap-6 space-y-5 lg:space-y-0">

                    {/* Age Slider */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm">
                        <div className="flex justify-between items-center mb-4">
                            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Your Age <span className="text-red-400">*</span></label>
                            <span className="text-2xl font-mono font-bold text-primary">{age}</span>
                        </div>
                        <div className="relative w-full h-6 flex items-center">
                            <input
                                type="range"
                                min="18"
                                max="75"
                                value={age}
                                onChange={(e) => setAge(e.target.value)}
                                className="z-10 relative w-full"
                            />
                        </div>
                        <div className="flex justify-between mt-1 text-[10px] text-slate-400 uppercase tracking-wider font-medium">
                            <span>18</span>
                            <span>75+</span>
                        </div>
                    </div>

                    {/* State & City — cascading searchable dropdowns */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm space-y-3">
                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Location <span className="text-red-400">*</span></label>
                        <div className="grid grid-cols-2 gap-3">
                            {/* State Dropdown */}
                            <div className="relative">
                                <div
                                    onClick={() => setOpenDropdown(openDropdown === 'state' ? null : 'state')}
                                    className={`bg-surface-active border rounded-lg py-3 px-3 text-sm cursor-pointer flex items-center gap-2 transition-all hover:border-primary/30 ${
                                        state ? 'border-primary/20 text-white' : 'border-white/10 text-slate-500'
                                    }`}
                                >
                                    <MapPin className="w-4 h-4 text-slate-500 shrink-0" />
                                    <span className="truncate flex-1">{state || 'Select State...'}</span>
                                    <ChevronDown className={`w-4 h-4 text-slate-400 shrink-0 transition-transform ${openDropdown === 'state' ? 'rotate-180' : ''}`} />
                                </div>
                                {openDropdown === 'state' && (
                                    <div className="absolute top-full left-0 w-full mt-2 bg-surface-dark rounded-xl shadow-xl border border-white/10 z-30 overflow-hidden">
                                        <div className="p-2 border-b border-white/5">
                                            <div className="relative">
                                                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
                                                <input
                                                    ref={stateSearchRef}
                                                    type="text"
                                                    value={stateSearch}
                                                    onChange={(e) => setStateSearch(e.target.value)}
                                                    placeholder="Search states..."
                                                    className="w-full bg-surface-active border border-white/5 rounded-lg py-2 pl-8 pr-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary/30"
                                                />
                                            </div>
                                        </div>
                                        <div className="max-h-48 overflow-y-auto">
                                            {loadingStates ? (
                                                <div className="p-3 text-xs text-slate-500 text-center flex items-center justify-center gap-2">
                                                    <Loader2 className="w-3 h-3 animate-spin" /> Loading states...
                                                </div>
                                            ) : filteredStates.length === 0 ? (
                                                <div className="p-3 text-xs text-slate-500 text-center">No states found</div>
                                            ) : (
                                                filteredStates.map(s => (
                                                    <div
                                                        key={s}
                                                        onClick={() => {
                                                            setState(s);
                                                            setCity('');
                                                            setStateSearch('');
                                                            setOpenDropdown(null);
                                                        }}
                                                        className={`px-3 py-2.5 text-sm cursor-pointer transition-colors ${
                                                            state === s
                                                                ? 'bg-primary/10 text-primary font-semibold'
                                                                : 'hover:bg-surface-active text-slate-300'
                                                        }`}
                                                    >
                                                        {s}
                                                    </div>
                                                ))
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* City Dropdown */}
                            <div className="relative">
                                <div
                                    onClick={() => {
                                        if (!state) {
                                            toast.error('Please select a state first', { id: 'state-first' });
                                            return;
                                        }
                                        setOpenDropdown(openDropdown === 'city' ? null : 'city');
                                    }}
                                    className={`bg-surface-active border rounded-lg py-3 px-3 text-sm cursor-pointer flex items-center gap-2 transition-all hover:border-primary/30 ${
                                        !state ? 'opacity-50 cursor-not-allowed' : city ? 'border-primary/20 text-white' : 'border-white/10 text-slate-500'
                                    }`}
                                >
                                    <MapPin className="w-4 h-4 text-slate-500 shrink-0" />
                                    <span className="truncate flex-1">{city || 'Select City...'}</span>
                                    <ChevronDown className={`w-4 h-4 text-slate-400 shrink-0 transition-transform ${openDropdown === 'city' ? 'rotate-180' : ''}`} />
                                </div>
                                {openDropdown === 'city' && state && (
                                    <div className="absolute top-full left-0 w-full mt-2 bg-surface-dark rounded-xl shadow-xl border border-white/10 z-30 overflow-hidden">
                                        <div className="p-2 border-b border-white/5">
                                            <div className="relative">
                                                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
                                                <input
                                                    ref={citySearchRef}
                                                    type="text"
                                                    value={citySearch}
                                                    onChange={(e) => setCitySearch(e.target.value)}
                                                    placeholder="Search cities..."
                                                    className="w-full bg-surface-active border border-white/5 rounded-lg py-2 pl-8 pr-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary/30"
                                                />
                                            </div>
                                        </div>
                                        <div className="max-h-48 overflow-y-auto">
                                            {loadingCities ? (
                                                <div className="p-3 text-xs text-slate-500 text-center flex items-center justify-center gap-2">
                                                    <Loader2 className="w-3 h-3 animate-spin" /> Loading cities...
                                                </div>
                                            ) : filteredCities.length === 0 ? (
                                                <div className="p-3 text-xs text-slate-500 text-center">No cities found</div>
                                            ) : (
                                                filteredCities.map(c => (
                                                    <div
                                                        key={c}
                                                        onClick={() => {
                                                            setCity(c);
                                                            setCitySearch('');
                                                            setOpenDropdown(null);
                                                        }}
                                                        className={`px-3 py-2.5 text-sm cursor-pointer transition-colors ${
                                                            city === c
                                                                ? 'bg-primary/10 text-primary font-semibold'
                                                                : 'hover:bg-surface-active text-slate-300'
                                                        }`}
                                                    >
                                                        {c}
                                                    </div>
                                                ))
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Marital Status — 4 options */}
                    <div className="rounded-xl bg-surface-dark border border-white/5 p-5 shadow-sm space-y-3">
                        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Marital Status <span className="text-red-400">*</span></label>
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
                                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Dependents</label>
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
                                    <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">How many are children?</label>
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

            {/* Section B: Know Your Investor Style */}
            <section id="investor" className="space-y-5">
                <div className="flex items-center gap-2 mb-1">
                    <span className="flex items-center justify-center w-6 h-6 rounded-full bg-primary/20 text-primary text-xs font-bold border border-primary/30">B</span>
                    <h3 className="text-white text-lg font-bold leading-tight">Know Your Investor Style</h3>
                </div>
                <div className="space-y-1 mb-4">
                    <p className="text-sm text-primary font-semibold">7 questions · Your profile tells us exactly how much risk your mind and your finances can handle.</p>
                    <p className="text-xs text-slate-400 leading-relaxed">
                        So your money is always working in a way you're genuinely comfortable with.
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

                {/* No result card — scoring is used internally for Step 3 comparison */}
            </section>

            {/* Bottom Navigation */}
            <div className="fixed bottom-0 left-0 right-0 bg-background-dark/80 backdrop-blur-lg border-t border-white/5 p-4 z-50">
                <div className="w-full px-6 flex items-center justify-end">
                    <div className="flex items-center gap-3">
                        <div className="hidden sm:flex items-center gap-2 px-4 py-3 bg-surface-dark border border-white/10 rounded-xl">
                            <CheckCircle2 className={`w-4 h-4 ${isFormValid ? 'text-primary' : 'text-slate-500'}`} />
                            <span className="text-sm font-semibold text-slate-400">Step 1/6</span>
                        </div>
                        <button
                            onClick={() => {
                                if (!isFormValid) {
                                    toast.error('Please complete all fields — age, city, employment, and risk questions', { id: 'validation' });
                                    return;
                                }
                                handleNext();
                            }}
                            disabled={isSaving}
                            className="px-6 py-3 bg-primary hover:bg-primary-dark active:scale-[0.98] text-background-dark font-bold text-sm rounded-xl flex items-center gap-2 transition-all shadow-[0_0_15px_rgba(13,242,89,0.25)] disabled:opacity-60"
                        >
                            {isSaving ? (
                                <><Loader2 className="w-4 h-4 animate-spin" /> Saving...</>
                            ) : (
                                <>Next <ArrowRight className="w-4 h-4" /></>
                            )}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Step1PersonalRisk;
