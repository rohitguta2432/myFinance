import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Shield, HeartPulse, Edit2, AlertTriangle, CheckCircle, CheckCircle2, Info, Loader2, Plus, X, Building2, Briefcase, FileText } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAssessmentStore } from '../store/useAssessmentStore';
import { useInsuranceQuery, useInsuranceMutation } from '../hooks/useInsurance';
import { useInsuranceGapQuery } from '../hooks/useInsuranceGap';
import { useProfileQuery } from '../hooks/useProfile';
import { InsuranceGapSkeleton } from '../../../components/ui/AssessmentSkeleton';
import SectionNav from '../../dashboard/components/SectionNav';

const STEP5_SECTIONS = [
    { id: 'corporate', label: 'Corporate' },
    { id: 'personal', label: 'Health & Life' },
    { id: 'checklist', label: 'Checklist' },
    { id: 'summary', label: 'Summary' },
];

const formatCurrency = (amount) => {
    if (amount >= 10000000) return `₹${(amount / 10000000).toFixed(2)} Cr`;
    if (amount >= 100000) return `₹${(amount / 100000).toFixed(2)} L`;
    return `₹${Math.round(amount).toLocaleString('en-IN')}`;
};

const Step5InsuranceGap = () => {
    const navigate = useNavigate();
    const {
        age,
        employmentType,
        insurance,
        updateInsurance,
        addPersonalHealth,
        removePersonalHealth,
        addPersonalLife,
        removePersonalLife,
        toggleChecklist
    } = useAssessmentStore();

    // Sync employmentType from backend (needed for corporate section visibility)
    const { data: profileData } = useProfileQuery();
    const setEmploymentType = useAssessmentStore((s) => s.setEmploymentType);
    useEffect(() => {
        if (profileData?.employmentType && !employmentType) {
            setEmploymentType(profileData.employmentType);
        }
    }, [profileData?.employmentType, employmentType, setEmploymentType]);

    // API Integration
    const { mutateAsync: saveInsuranceApi, isPending: isSaving } = useInsuranceMutation();
    const { data: gapData } = useInsuranceGapQuery();

    // Recommended covers from backend
    const recommendedLifeCover = gapData?.recommendedLifeCover ?? 0;
    const recommendedHealthCover = gapData?.recommendedHealthCover ?? 0;

    // Actual Covers
    const actualCorpHealth = parseFloat(insurance?.corporateHealth) || 0;
    const actualCorpLife = parseFloat(insurance?.corporateLife) || 0;

    const actualPersonalHealth = (insurance?.personalHealth || []).reduce((sum, p) => sum + (parseFloat(p.sumInsured) || 0), 0);
    const actualPersonalLife = (insurance?.personalLife || []).reduce((sum, p) => sum + (parseFloat(p.sumAssured) || 0), 0);

    const totalHealthCover = actualCorpHealth + actualPersonalHealth;
    const totalLifeCover = actualCorpLife + actualPersonalLife;

    const lifeGap = Math.max(0, recommendedLifeCover - totalLifeCover);
    const healthGap = Math.max(0, recommendedHealthCover - totalHealthCover);

    const lifeGapPercent = recommendedLifeCover > 0 ? (totalLifeCover / recommendedLifeCover) * 100 : 100;

    // Modals state
    const [isHealthModalOpen, setIsHealthModalOpen] = useState(false);
    const [isLifeModalOpen, setIsLifeModalOpen] = useState(false);

    // Health Form
    const [hType, setHType] = useState('Family Floater');
    const [hSum, setHSum] = useState('');
    const [hPremium, setHPremium] = useState('');
    const [hCopay, setHCopay] = useState('');

    // Life Form
    const [lType, setLType] = useState('Term Life');
    const [lSum, setLSum] = useState('');
    const [lPremium, setLPremium] = useState('');
    const [lSpouseAge, setLSpouseAge] = useState('');

    const handleSaveHealth = () => {
        if (!hSum) return;
        addPersonalHealth({
            id: Date.now(),
            type: hType,
            sumInsured: parseFloat(hSum),
            premium: parseFloat(hPremium) || 0,
            copay: parseFloat(hCopay) || 0
        });
        setIsHealthModalOpen(false);
        setHType('Family Floater'); setHSum(''); setHPremium(''); setHCopay('');
    };

    const handleSaveLife = () => {
        if (!lSum) return;
        addPersonalLife({
            id: Date.now(),
            type: lType,
            sumAssured: parseFloat(lSum),
            premium: parseFloat(lPremium) || 0,
            spouseAge: parseInt(lSpouseAge) || age
        });
        setIsLifeModalOpen(false);
        setLType('Term Life'); setLSum(''); setLPremium(''); setLSpouseAge('');
    };

    const handleNext = async () => {
        try {
            await saveInsuranceApi({
                life: totalLifeCover,
                health: totalHealthCover
            });
        } catch (e) { console.warn("API fail ignored", e); }
        navigate('/assessment/step-6');
    };

    return (
        <div className="flex flex-col h-full bg-background-dark text-white">
            {/* Headers */}
            <div className="mb-6 px-4 pt-4">
                <h1 className="text-2xl font-bold border-b border-primary/20 pb-2 inline-block text-white mb-2">Protecting What You've Built</h1>
                <p className="text-sm text-slate-400">Wealth without protection is a house of cards. One medical emergency can wipe out years of savings. Let's ensure you're covered.</p>
            </div>

            <SectionNav sections={STEP5_SECTIONS} />
            <div className="flex-1 space-y-8 overflow-y-auto px-4 pb-8">

                {/* SECTION A: Corporate */}
                <div id="corporate" />
                {employmentType === 'Salaried' && (
                    <section className="bg-surface-dark rounded-2xl p-5 border border-white/5 shadow-lg">
                        <div className="flex items-center gap-3 mb-4 border-b border-white/5 pb-3">
                            <div className="bg-blue-500/10 p-2 rounded-xl text-blue-400">
                                <Building2 className="w-5 h-5" />
                            </div>
                            <div>
                                <h3 className="font-bold text-white text-lg">Corporate Insurance</h3>
                                <p className="text-xs text-slate-400">Do you have employer-provided insurance?</p>
                            </div>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-400 block mb-1">Corporate Health Insurance coverage (₹)?</label>
                                <input
                                    type="number"
                                    placeholder="e.g. 500000"
                                    value={insurance?.corporateHealth || ''}
                                    onChange={e => updateInsurance({ corporateHealth: e.target.value })}
                                    className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:border-primary/50"
                                />
                            </div>
                            <div>
                                <label className="text-xs font-semibold text-slate-400 block mb-1">How many family members covered?</label>
                                <input
                                    type="number"
                                    placeholder="e.g. 4"
                                    value={insurance?.corporateHealthMembers || ''}
                                    onChange={e => updateInsurance({ corporateHealthMembers: e.target.value })}
                                    className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:border-primary/50"
                                />
                            </div>
                            <div>
                                <label className="text-xs font-semibold text-slate-400 block mb-1">Corporate Term Life Insurance coverage (₹)?</label>
                                <input
                                    type="number"
                                    placeholder="e.g. 1000000"
                                    value={insurance?.corporateLife || ''}
                                    onChange={e => updateInsurance({ corporateLife: e.target.value })}
                                    className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:border-primary/50"
                                />
                            </div>
                        </div>

                        {(actualCorpHealth > 0 || actualCorpLife > 0) && (
                            <div className="mt-5 border border-blue-500/20 bg-blue-500/5 rounded-xl p-4">
                                <h4 className="text-[10px] font-bold tracking-wider text-blue-400 uppercase mb-3">Corporate Coverage Summary</h4>
                                <div className="space-y-1 text-sm font-medium mb-3">
                                    <div className="flex justify-between">
                                        <span className="text-slate-400">Health:</span>
                                        <span className="text-white">{formatCurrency(actualCorpHealth)}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-400">Life:</span>
                                        <span className="text-white">{formatCurrency(actualCorpLife)}</span>
                                    </div>
                                </div>
                                <div className="flex items-start gap-2 text-xs text-amber-200/80 bg-amber-500/10 p-2 rounded-lg">
                                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                                    <p>Corporate coverage ends if you change jobs. Personal policies recommended.</p>
                                </div>
                            </div>
                        )}
                    </section>
                )}

                {/* SECTION B & C: Personal Health + Life — Side by Side */}
                <div id="personal" className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <section className="bg-surface-dark rounded-2xl border border-white/5 shadow-lg overflow-hidden flex flex-col">
                    <div className="p-5 border-b border-white/5 flex-1">
                        <div className="flex items-center justify-between mb-4">
                            <div className="flex items-center gap-3">
                                <div className="bg-teal-500/10 p-2 rounded-xl text-teal-400">
                                    <HeartPulse className="w-5 h-5" />
                                </div>
                                <h3 className="font-bold text-white text-lg">Personal Health</h3>
                            </div>
                            <button onClick={() => setIsHealthModalOpen(true)} className="text-xs flex items-center gap-1 bg-teal-500/10 text-teal-400 px-3 py-1.5 rounded-full font-bold hover:bg-teal-500/20 transition">
                                <Plus className="w-3 h-3" /> Add Policy
                            </button>
                        </div>

                        {!(insurance?.personalHealth && insurance.personalHealth.length > 0) ? (
                            <div className="text-center py-6 bg-background-dark rounded-xl border border-white/5 border-dashed">
                                <HeartPulse className="w-8 h-8 text-slate-600 mx-auto mb-2 opacity-50" />
                                <p className="text-sm text-slate-500">No personal health insurance added</p>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {insurance.personalHealth.map(p => (
                                    <div key={p.id} className="bg-background-dark p-3 rounded-xl border border-white/10 flex justify-between items-center">
                                        <div>
                                            <p className="font-bold text-sm text-white">{p.type}</p>
                                            <p className="text-xs text-slate-400">Sum: {formatCurrency(p.sumInsured)}{p.premium ? ` • Prem: ${formatCurrency(p.premium)}/yr` : ''}</p>
                                        </div>
                                        <button onClick={() => removePersonalHealth(p.id)} className="text-slate-500 hover:text-red-400 p-2"><X className="w-4 h-4" /></button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="p-5 bg-gradient-to-b from-surface-dark to-background-dark">
                        <h4 className="text-[10px] font-bold tracking-wider text-teal-400 uppercase mb-4 flex items-center gap-2">
                            <HeartPulse className="w-4 h-4" /> Health Coverage Analysis
                        </h4>

                        <div className="space-y-3 text-sm">
                            <div className="flex justify-between items-start border-b border-white/5 pb-2">
                                <div>
                                    <p className="text-slate-400 font-semibold mb-0.5">RECOMMENDED FOR YOU</p>
                                    <p className="text-[10px] text-slate-500">Based on family size & city</p>
                                </div>
                                <span className="font-bold text-lg text-white">{formatCurrency(recommendedHealthCover)}</span>
                            </div>

                            <div className="flex justify-between items-start border-b border-white/5 pb-2">
                                <div>
                                    <p className="text-slate-400 font-semibold mb-0.5">YOUR CURRENT COVERAGE</p>
                                    <p className="text-[10px] text-slate-500">Corporate: {formatCurrency(actualCorpHealth)} | Personal: {formatCurrency(actualPersonalHealth)}</p>
                                </div>
                                <span className="font-bold text-white">{formatCurrency(totalHealthCover)}</span>
                            </div>

                            {healthGap > 0 ? (
                                <div className="flex justify-between items-center text-amber-400 bg-amber-500/10 p-3 rounded-xl border border-amber-500/20 mt-2">
                                    <span className="font-bold flex items-center gap-2"><AlertTriangle className="w-4 h-4" /> COVERAGE GAP</span>
                                    <span className="font-bold text-lg">{formatCurrency(healthGap)}</span>
                                </div>
                            ) : (
                                <div className="flex justify-between items-center text-primary bg-primary/10 p-3 rounded-xl border border-primary/20 mt-2">
                                    <span className="font-bold flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> FULLY COVERED</span>
                                </div>
                            )}
                        </div>
                    </div>
                </section>

                <section className="bg-surface-dark rounded-2xl border border-white/5 shadow-lg overflow-hidden flex flex-col">
                    <div className="p-5 border-b border-white/5 flex-1">
                        <div className="flex items-center justify-between mb-4">
                            <div className="flex items-center gap-3">
                                <div className="bg-blue-500/10 p-2 rounded-xl text-blue-400">
                                    <Shield className="w-5 h-5" />
                                </div>
                                <h3 className="font-bold text-white text-lg">Personal Life</h3>
                            </div>
                            <button onClick={() => setIsLifeModalOpen(true)} className="text-xs flex items-center gap-1 bg-blue-500/10 text-blue-400 px-3 py-1.5 rounded-full font-bold hover:bg-blue-500/20 transition">
                                <Plus className="w-3 h-3" /> Add Policy
                            </button>
                        </div>

                        {!(insurance?.personalLife && insurance.personalLife.length > 0) ? (
                            <div className="text-center py-6 bg-background-dark rounded-xl border border-white/5 border-dashed">
                                <Shield className="w-8 h-8 text-slate-600 mx-auto mb-2 opacity-50" />
                                <p className="text-sm text-slate-500">No personal life insurance added</p>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {insurance.personalLife.map(p => (
                                    <div key={p.id} className="bg-background-dark p-3 rounded-xl border border-white/10 flex justify-between items-center">
                                        <div>
                                            <p className="font-bold text-sm text-white">{p.type}</p>
                                            <p className="text-xs text-slate-400">Sum: {formatCurrency(p.sumAssured)}{p.premium ? ` • Prem: ${formatCurrency(p.premium)}/yr` : ''}</p>
                                        </div>
                                        <button onClick={() => removePersonalLife(p.id)} className="text-slate-500 hover:text-red-400 p-2"><X className="w-4 h-4" /></button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="p-5 bg-gradient-to-b from-surface-dark to-background-dark">
                        <h4 className="text-[10px] font-bold tracking-wider text-blue-400 uppercase mb-4 flex items-center gap-2">
                            <Briefcase className="w-4 h-4" /> Life Coverage Analysis
                        </h4>

                        <div className="space-y-3 text-sm">
                            <div className="flex justify-between items-start border-b border-white/5 pb-2">
                                <div>
                                    <p className="text-slate-400 font-semibold mb-0.5">RECOMMENDED FOR YOU</p>
                                    <p className="text-[10px] text-slate-500">Based on Income, Goals & Liabilities</p>
                                </div>
                                <span className="font-bold text-lg text-white">{formatCurrency(recommendedLifeCover)}</span>
                            </div>

                            <div className="flex justify-between items-start border-b border-white/5 pb-2">
                                <div>
                                    <p className="text-slate-400 font-semibold mb-0.5">YOUR CURRENT COVERAGE</p>
                                    <p className="text-[10px] text-slate-500">Corporate: {formatCurrency(actualCorpLife)} | Personal: {formatCurrency(actualPersonalLife)}</p>
                                </div>
                                <span className="font-bold text-white">{formatCurrency(totalLifeCover)}</span>
                            </div>

                            {lifeGap > 0 ? (
                                <div className="bg-red-500/10 p-4 rounded-xl border border-red-500/20 mt-3">
                                    <div className="flex justify-between items-center mb-2">
                                        <span className="font-bold text-red-400 flex items-center gap-2"><X className="w-4 h-4" /> COVERAGE GAP</span>
                                        <span className="font-bold text-lg text-red-400">{formatCurrency(lifeGap)}</span>
                                    </div>
                                    <p className="text-[10px] font-semibold text-red-400/80 uppercase tracking-wide mb-3">
                                        (You're only {Math.round(lifeGapPercent)}% covered!)
                                    </p>
                                    <div className="border-t border-red-500/10 pt-3">
                                        <p className="text-xs text-red-200/70">Estimated premium to close gap:</p>
                                        <p className="font-bold text-white text-sm">₹{Math.round(lifeGap / 100000 * 20)}/month <span className="text-xs font-normal text-slate-400">({formatCurrency(lifeGap)} term plan)</span></p>
                                    </div>
                                </div>
                            ) : (
                                <div className="flex justify-between items-center text-primary bg-primary/10 p-3 rounded-xl border border-primary/20 mt-2">
                                    <span className="font-bold flex items-center gap-2"><CheckCircle2 className="w-4 h-4" /> FULLY COVERED</span>
                                </div>
                            )}
                        </div>
                    </div>
                </section>
                </div>

                {/* SECTION D: Quick Checklist */}
                <section id="checklist" className="bg-surface-dark rounded-2xl p-5 border border-white/5 shadow-lg">
                    <h3 className="font-bold text-white text-lg mb-1">Quick Checklist</h3>
                    <p className="text-xs text-slate-400 mb-4">Do you have these secondary protections? (Optional)</p>

                    <div className="space-y-2">
                        {[
                            { id: 'criticalIllness', label: 'Critical Illness Cover' },
                            { id: 'personalAccident', label: 'Personal Accident Cover' },
                            { id: 'disability', label: 'Disability Insurance' },
                            { id: 'maternity', label: 'Maternity Cover' }
                        ].map(item => (
                            <label key={item.id} className="flex items-center justify-between p-3 bg-background-dark border border-white/5 hover:border-white/10 rounded-xl cursor-pointer transition">
                                <span className="text-sm font-medium text-slate-300">{item.label}</span>
                                <div className={`w-10 h-6 rounded-full p-1 transition-colors duration-200 ease-in-out ${insurance?.checklist?.[item.id] ? 'bg-primary' : 'bg-white/10'}`}>
                                    <div className={`w-4 h-4 rounded-full bg-white shadow-md transform transition-transform duration-200 ease-in-out ${insurance?.checklist?.[item.id] ? 'translate-x-4' : 'translate-x-0'}`}></div>
                                </div>
                                {/* Hidden checkbox to trigger toggle */}
                                <input type="checkbox" className="hidden" checked={insurance?.checklist?.[item.id] || false} onChange={() => toggleChecklist(item.id)} />
                            </label>
                        ))}
                    </div>
                </section>

                {/* FINAL ACTION SUMMARY */}
                <section id="summary" className="bg-surface-dark border-2 border-primary/20 rounded-3xl shadow-2xl relative mt-10 p-6">
                    <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-primary text-background-dark font-black text-[10px] uppercase tracking-widest py-1 px-4 rounded-full shadow-lg">
                        Action Summary
                    </div>
                    <div className="h-full">
                        <h3 className="font-bold text-white text-center mb-6 mt-2 tracking-wide text-lg flex justify-center items-center gap-2">
                            <FileText className="w-5 h-5 text-primary" /> RECOMMENDED ACTIONS
                        </h3>

                        <div className="space-y-4">
                            {lifeGap > 0 && (
                                <div className="flex gap-4 items-start pb-4 border-b border-white/5">
                                    <div className="w-8 h-8 rounded-full bg-blue-500/20 text-blue-400 font-bold flex items-center justify-center shrink-0 border border-blue-500/20">1</div>
                                    <div>
                                        <p className="font-bold text-white mb-1">Buy {formatCurrency(lifeGap)} Term Life Plan</p>
                                        <p className="text-xs text-slate-400">Impact: Full family protection to cover liabilities and future goals.</p>
                                    </div>
                                </div>
                            )}

                            {healthGap > 0 && (
                                <div className="flex gap-4 items-start pb-4 border-b border-white/5">
                                    <div className="w-8 h-8 rounded-full bg-teal-500/20 text-teal-400 font-bold flex items-center justify-center shrink-0 border border-teal-500/20">{lifeGap > 0 ? '2' : '1'}</div>
                                    <div>
                                        <p className="font-bold text-white mb-1">Add {formatCurrency(healthGap)} Health Top-up</p>
                                        <p className="text-xs text-slate-400">Impact: Cover major medical emergencies beyond corporate limits.</p>
                                    </div>
                                </div>
                            )}

                            {lifeGap === 0 && healthGap === 0 && (
                                <div className="text-center py-4">
                                    <div className="w-12 h-12 bg-primary/20 text-primary rounded-full flex items-center justify-center mx-auto mb-3">
                                        <CheckCircle className="w-6 h-6" />
                                    </div>
                                    <p className="font-bold text-primary">Excellent job!</p>
                                    <p className="text-sm text-slate-400 mt-1">Your insurance covers all projected needs.</p>
                                </div>
                            )}
                        </div>
                    </div>
                </section>

            </div>

            {/* Bottom Navigation */}
            <div className="fixed bottom-0 left-0 right-0 bg-background-dark/80 backdrop-blur-lg border-t border-white/5 p-4 z-50">
                <div className="w-full px-6 flex items-center justify-between">
                    <button
                        onClick={() => navigate('/assessment/step-4')}
                        className="px-6 py-3 bg-surface-dark hover:bg-surface-active text-white font-bold text-sm rounded-xl transition-all"
                    >
                        Back
                    </button>
                    <div className="flex items-center gap-3">
                        <div className="hidden sm:flex items-center gap-2 px-4 py-3 bg-surface-dark border border-white/10 rounded-xl shadow-inner">
                            <CheckCircle2 className="w-4 h-4 text-primary" />
                            <span className="text-sm font-semibold text-slate-400">Step 5/6</span>
                        </div>
                        <button
                            onClick={handleNext}
                            disabled={isSaving}
                            className="px-6 py-3 bg-primary hover:bg-primary-dark active:scale-[0.98] text-background-dark font-bold text-sm rounded-xl flex items-center gap-2 transition-all shadow-[0_0_15px_rgba(13,242,89,0.25)] disabled:opacity-60"
                        >
                            {isSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Next'} <ArrowRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>

            {/* Health Modal */}
            {isHealthModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/80 backdrop-blur-sm" onClick={() => setIsHealthModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-sm rounded-t-3xl sm:rounded-3xl shadow-2xl border border-white/10 overflow-hidden animate-slide-up">
                        <div className="p-5 border-b border-white/5 bg-surface-dark sticky top-0 z-10 flex justify-between items-center">
                            <h3 className="font-bold text-white">Add Health Policy</h3>
                            <button onClick={() => setIsHealthModalOpen(false)} className="text-slate-400"><X className="w-5 h-5" /></button>
                        </div>
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-400 block mb-1">Policy Type</label>
                                <select value={hType} onChange={e => setHType(e.target.value)} className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white">
                                    <option value="Individual">Individual</option>
                                    <option value="Family Floater">Family Floater</option>
                                    <option value="Super Top-up">Super Top-up</option>
                                    <option value="Critical Illness">Critical Illness</option>
                                </select>
                            </div>
                            <div>
                                <label className="text-xs font-semibold text-slate-400 block mb-1">Sum Insured (₹)</label>
                                <input type="number" value={hSum} onChange={e => setHSum(e.target.value)} placeholder="1000000" className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white" />
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="text-xs font-semibold text-slate-400 block mb-1">Premium/yr (opt)</label>
                                    <input type="number" value={hPremium} onChange={e => setHPremium(e.target.value)} placeholder="15000" className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white" />
                                </div>
                                <div>
                                    <label className="text-xs font-semibold text-slate-400 block mb-1">Co-pay % (opt)</label>
                                    <input type="number" value={hCopay} onChange={e => setHCopay(e.target.value)} placeholder="20" className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white" />
                                </div>
                            </div>
                            <button onClick={handleSaveHealth} className="w-full bg-primary hover:bg-primary-dark text-background-dark font-bold py-3.5 rounded-xl shadow-[0_0_15px_rgba(13,242,89,0.3)] mt-4">Save Policy</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Life Modal */}
            {isLifeModalOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
                    <div className="absolute inset-0 bg-black/80 backdrop-blur-sm" onClick={() => setIsLifeModalOpen(false)}></div>
                    <div className="relative bg-surface-dark w-full max-w-sm rounded-t-3xl sm:rounded-3xl shadow-2xl border border-white/10 overflow-hidden animate-slide-up">
                        <div className="p-5 border-b border-white/5 bg-surface-dark sticky top-0 z-10 flex justify-between items-center">
                            <h3 className="font-bold text-white">Add Life Policy</h3>
                            <button onClick={() => setIsLifeModalOpen(false)} className="text-slate-400"><X className="w-5 h-5" /></button>
                        </div>
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="text-xs font-semibold text-slate-400 block mb-1">Policy Type</label>
                                <select value={lType} onChange={e => setLType(e.target.value)} className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white">
                                    <option value="Term Life">Term Life (recommended)</option>
                                    <option value="Whole Life">Whole Life</option>
                                    <option value="ULIP">ULIP</option>
                                    <option value="Endowment Plan">Endowment Plan</option>
                                </select>
                            </div>
                            <div>
                                <label className="text-xs font-semibold text-slate-400 block mb-1">Sum Assured (₹)</label>
                                <input type="number" value={lSum} onChange={e => setLSum(e.target.value)} placeholder="50000000" className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white" />
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="text-xs font-semibold text-slate-400 block mb-1">Premium/yr</label>
                                    <input type="number" value={lPremium} onChange={e => setLPremium(e.target.value)} placeholder="12000" className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white" />
                                </div>
                                <div>
                                    <label className="text-xs font-semibold text-slate-400 block mb-1">Spouse Age</label>
                                    <input type="number" value={lSpouseAge} onChange={e => setLSpouseAge(e.target.value)} placeholder="30" className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white" />
                                </div>
                            </div>
                            <button onClick={handleSaveLife} className="w-full bg-primary hover:bg-primary-dark text-background-dark font-bold py-3.5 rounded-xl shadow-[0_0_15px_rgba(13,242,89,0.3)] mt-4">Save Policy</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Step5InsuranceGap;
