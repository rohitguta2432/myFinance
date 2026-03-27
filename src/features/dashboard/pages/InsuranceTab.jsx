import React, { useState } from 'react';
import {
    Shield, HeartPulse, Zap, ChevronDown, ChevronUp,
    CheckCircle2, AlertTriangle, Info, ArrowUpRight,
    Wallet, Building2, User, Check, X
} from 'lucide-react';
import { useInsuranceAnalysis } from '../../../hooks/useInsuranceAnalysis';
import { InsuranceSkeleton } from '../../../components/ui/DashboardSkeleton';



/* ── Expandable Do/Don't Panels ── */
const GuidancePanel = ({ items, type }) => {
    const [open, setOpen] = useState(false);
    const isDoList = type === 'do';
    const Icon = isDoList ? Check : X;
    const color = isDoList ? 'text-emerald-400' : 'text-red-400';
    const bg = isDoList ? 'bg-emerald-500/5 border-emerald-500/15' : 'bg-red-500/5 border-red-500/15';
    const headerBg = isDoList ? 'bg-emerald-500/10' : 'bg-red-500/10';
    const label = isDoList ? '✅ WHAT TO DO' : '❌ WHAT NOT TO DO';

    return (
        <div className={`border rounded-xl overflow-hidden ${bg}`}>
            <button
                onClick={() => setOpen(!open)}
                className={`w-full flex items-center justify-between px-4 py-3 ${headerBg} transition-colors hover:opacity-90`}
            >
                <span className={`text-xs font-bold uppercase tracking-wider ${color}`}>{label}</span>
                {open ? <ChevronUp className={`w-4 h-4 ${color}`} /> : <ChevronDown className={`w-4 h-4 ${color}`} />}
            </button>
            {open && (
                <div className="px-4 py-3 space-y-2.5">
                    {items.map((item, i) => (
                        <div key={i} className="flex items-start gap-2.5">
                            <div className={`w-5 h-5 rounded-full flex items-center justify-center shrink-0 mt-0.5 ${isDoList ? 'bg-emerald-500/20' : 'bg-red-500/20'}`}>
                                <Icon className={`w-3 h-3 ${color}`} />
                            </div>
                            <p className="text-xs text-slate-300 leading-relaxed">{item}</p>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

/* ── Cover Adequacy Bar ── */
const CoverBar = ({ pct, color, label, isAdequate }) => (
    <div className="mt-4">
        <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider" style={{ color }}>
                {label}
            </span>
            <span className="text-xs font-bold tabular-nums text-white">
                {Math.round(pct)}%
                {isAdequate && <CheckCircle2 className="w-3.5 h-3.5 inline ml-1 text-emerald-400" />}
            </span>
        </div>
        <div className="w-full h-2.5 bg-white/5 rounded-full overflow-hidden">
            <div
                className="h-full rounded-full transition-all duration-1000 ease-out"
                style={{
                    width: `${Math.min(100, pct)}%`,
                    background: `linear-gradient(90deg, ${color}cc, ${color})`,
                    boxShadow: `0 0 12px ${color}30`,
                }}
            />
        </div>
    </div>
);

/* ── Metric Row ── */
const MetricRow = ({ label, sublabel, value, icon }) => (
    <div className="flex items-center justify-between py-2.5 border-b border-white/5 last:border-0">
        <div className="flex items-center gap-2.5">
            {icon && <span className="text-sm">{icon}</span>}
            <div>
                <p className="text-xs font-semibold text-slate-300">{label}</p>
                {sublabel && <p className="text-[10px] text-slate-500">{sublabel}</p>}
            </div>
        </div>
        <span className="text-sm font-bold tabular-nums text-white">{value}</span>
    </div>
);

/* ── Additional Coverage Card ── */
const AdditionalCard = ({ card }) => (
    <div
        className="bg-surface-dark border border-white/5 rounded-2xl p-5 hover:border-primary/20 transition-all"
        style={{ animation: 'fadeSlideUp 0.5s ease-out both' }}
    >
        <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center text-xl">
                {card.icon}
            </div>
            <div>
                <h4 className="text-sm font-bold text-white">{card.title}</h4>
                <span className="text-[10px] text-amber-400 font-semibold uppercase tracking-wider">Recommended for you</span>
            </div>
        </div>
        <p className="text-xs text-slate-300 leading-relaxed mb-3">{card.explanation}</p>
        <div className="flex items-center gap-2 bg-white/5 rounded-lg px-3 py-2">
            <Wallet className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <span className="text-[11px] text-slate-400">
                Estimated: <span className="font-bold text-white">
                    {typeof card.estimatedPremium === 'number'
                        ? `₹${card.estimatedPremium.toLocaleString('en-IN')}/year`
                        : `${card.estimatedPremium}/year`
                    }
                </span>
            </span>
        </div>
    </div>
);

/* ═══════════════════════════════════════════════════════
   MAIN INSURANCE TAB
   ═══════════════════════════════════════════════════════ */
const InsuranceTab = () => {
    const data = useInsuranceAnalysis();
    const { termLife, healthInsurance, additionalCoverage, age, city, annualIncomeFormatted, totalEMI, totalEMIFormatted } = data;

    if (!termLife || !healthInsurance) {
        return <InsuranceSkeleton />;
    }

    // Do/Don't content — Term
    const termDo = [
        `Buy a pure term plan with cover extending to at least age 60. Look for: (a) claim settlement ratio > 98%, (b) individual death claim settlement ratio (not group), (c) cover amount adequate to close the gap of ${termLife.coverGapFormatted}.`,
        'Choose a cover period to at least the year your youngest dependent becomes financially independent.',
        'Disclose all existing health conditions and family medical history — accurate disclosure ensures your claim is honoured.',
        'Register your policy in an e-Insurance repository (NSDL or CDSL) so your nominee can track it digitally.',
        "Inform your nominee about the claim process — the insurer's call centre number, documents needed (death certificate, original policy, bank details).",
    ];
    const termDont = [
        'Do not buy a ULIP or endowment plan thinking it doubles as life insurance — the cover is inadequate and the costs are high.',
        'Do not choose a policy with a return-of-premium feature — this increases the premium by 50–100% for zero additional coverage benefit.',
        'Do not suppress any health information on the proposal form — claim rejection for non-disclosure is the most common failure.',
        'Do not rely on free group term cover from your employer as your primary protection — it disappears when employment ends.',
        'Do not choose a decreasing cover plan if you have fixed liabilities (flat EMI does not reduce).',
    ];

    // Do/Don't content — Health
    const healthDo = [
        `Buy a comprehensive individual or family floater with at least ${healthInsurance.cityBenchmarkFormatted} cover.`,
        'Key features to look for: No room rent sub-limit, no co-payment clause, restoration benefit, daycare procedures covered, pre-hospitalisation (30 days) and post-hospitalisation (90 days) expenses, cashless network including hospitals in your city.',
        `Add a ₹50L super top-up plan — it is the most cost-effective way to reach ${healthInsurance.totalWithTopUp} total cover.`,
        'Claim Section 80D deduction: ₹25,000 for self/family, additional ₹25,000 if parents are below 60, or ₹50,000 if parents are senior citizens.',
    ];
    const healthDont = [
        'Do not choose a policy based on premium alone — sub-limits can reduce your effective benefit by 50–70%.',
        'Do not ignore the co-payment clause — a 20% co-pay on an ₹8L claim means ₹1.6L out of your pocket.',
        'Do not let your policy lapse — pre-existing condition waiting periods restart from day one of a new policy.',
        "Do not count your employer's group health cover as your primary health insurance strategy — it ends the day you resign.",
        'Do not underestimate room rent capping — if the policy caps room rent at ₹3,000/day and you take a ₹6,000/day room, all associated costs are proportionally reduced.',
    ];

    const cityTierLabel = {
        metro: 'Metro',
        tier1: 'Tier-1',
        tier2: 'Tier-2 & below',
        tier3: 'Tier-2 & below',
    };

    return (
        <div className="w-full max-w-6xl mx-auto px-4 py-6 pb-24 space-y-8">

                {/* Page Title */}
                <div>
                    <h2 className="text-xl font-black tracking-tight">Insurance Analysis</h2>
                    <p className="text-xs text-slate-500 mt-0.5">
                        {city ? `📍 ${city}` : ''} · Personalised coverage assessment
                    </p>
                </div>

                {/* ════════════════════════════════════════════════
                   SECTION 1 — TERM LIFE INSURANCE
                   ════════════════════════════════════════════════ */}
                <section className="bg-surface-dark rounded-3xl border border-white/5 shadow-xl overflow-hidden">
                    {/* Section Header */}
                    <div className="bg-gradient-to-r from-blue-500/10 via-transparent to-transparent p-6 border-b border-white/5">
                        <div className="flex items-center gap-3 mb-2">
                            <div className="w-10 h-10 rounded-xl bg-blue-500/15 flex items-center justify-center">
                                <Shield className="w-5 h-5 text-blue-400" />
                            </div>
                            <div>
                                <h3 className="text-lg font-black text-white">Term Life Insurance</h3>
                                <span className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">Pillar of family security</span>
                            </div>
                        </div>
                    </div>

                    <div className="p-6 space-y-5">
                        {/* Importance Statement */}
                        <div className="bg-blue-500/5 border border-blue-500/10 rounded-xl p-4">
                            <h4 className="text-xs font-bold text-blue-400 uppercase tracking-wider mb-2 flex items-center gap-2">
                                <Info className="w-3.5 h-3.5" /> Why Term Insurance Matters
                            </h4>
                            <p className="text-xs text-slate-300 leading-relaxed">
                                Term insurance is the foundation of your family's financial security. If you are the primary income earner, your family's lifestyle, loan repayments, children's education, and retirement depend on your income. Term insurance replaces that income if you are no longer there to provide it. It is the only instrument that provides crore-level cover at a monthly cost of ₹500–2,000, making it the highest-leverage financial product available to you.
                            </p>
                        </div>

                        {/* Calculation Display */}
                        <div className="bg-background-dark rounded-xl border border-white/5 p-4">
                            <h4 className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-3">Your Numbers</h4>
                            <MetricRow
                                label="HLV Method"
                                sublabel={`Annual Salary × (${60} − ${age})`}
                                value={termLife.hlvFormatted}
                                icon="📊"
                            />
                            <MetricRow
                                label="Needs Analysis"
                                sublabel="Loans + (10 × Salary) + Goals"
                                value={termLife.needsAnalysisFormatted}
                                icon="📋"
                            />
                            <div className="flex items-center justify-between py-2.5 border-b border-white/5">
                                <div className="flex items-center gap-2.5">
                                    <span className="text-sm">🎯</span>
                                    <p className="text-xs font-bold text-white">Required Cover (Max)</p>
                                </div>
                                <span className="text-sm font-black text-primary tabular-nums">{termLife.requiredCoverFormatted}</span>
                            </div>
                            <MetricRow
                                label="Your Existing Cover"
                                sublabel={`Personal: ${new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(termLife.personalCover)} + Corporate: ${new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(termLife.corporateCover)}`}
                                value={termLife.existingCoverFormatted}
                                icon="🔒"
                            />
                            {!termLife.isAdequate && (
                                <div className="flex items-center justify-between py-2.5 mt-1 bg-red-500/10 rounded-lg px-3 border border-red-500/15">
                                    <div className="flex items-center gap-2">
                                        <AlertTriangle className="w-4 h-4 text-red-400" />
                                        <span className="text-xs font-bold text-red-400">COVER GAP</span>
                                    </div>
                                    <span className="text-sm font-black text-red-400 tabular-nums">{termLife.coverGapFormatted}</span>
                                </div>
                            )}
                        </div>

                        {/* Cover Bar */}
                        <CoverBar
                            pct={termLife.adequacyPct}
                            color={termLife.barColor}
                            label={termLife.label}
                            isAdequate={termLife.isAdequate}
                        />

                        {/* Do / Don't */}
                        <div className="space-y-3">
                            <GuidancePanel items={termDo} type="do" />
                            <GuidancePanel items={termDont} type="dont" />
                        </div>
                    </div>
                </section>

                {/* ════════════════════════════════════════════════
                   SECTION 2 — HEALTH INSURANCE
                   ════════════════════════════════════════════════ */}
                <section className="bg-surface-dark rounded-3xl border border-white/5 shadow-xl overflow-hidden">
                    <div className="bg-gradient-to-r from-teal-500/10 via-transparent to-transparent p-6 border-b border-white/5">
                        <div className="flex items-center gap-3 mb-2">
                            <div className="w-10 h-10 rounded-xl bg-teal-500/15 flex items-center justify-center">
                                <HeartPulse className="w-5 h-5 text-teal-400" />
                            </div>
                            <div>
                                <h3 className="text-lg font-black text-white">Health Insurance</h3>
                                <span className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">Medical protection</span>
                            </div>
                        </div>
                    </div>

                    <div className="p-6 space-y-5">
                        {/* Importance Statement */}
                        <div className="bg-teal-500/5 border border-teal-500/10 rounded-xl p-4">
                            <h4 className="text-xs font-bold text-teal-400 uppercase tracking-wider mb-2 flex items-center gap-2">
                                <Info className="w-3.5 h-3.5" /> Why Health Insurance Matters
                            </h4>
                            <p className="text-xs text-slate-300 leading-relaxed">
                                Medical costs in India have grown faster than inflation for a decade. A single ICU admission with surgery in a metro hospital costs ₹3–8 lakh. Without adequate health insurance, one hospitalisation can wipe out years of savings. Additionally, health insurance premiums qualify for a tax deduction under Section 80D — so being uninsured not only exposes you to financial risk but also costs you tax money.
                            </p>
                        </div>

                        {/* City Benchmark */}
                        <div className="bg-background-dark rounded-xl border border-white/5 p-4">
                            <h4 className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-3">City-Based Benchmark</h4>
                            <div className="grid grid-cols-3 gap-2 mb-3">
                                {[
                                    { tier: 'Metro', cover: '₹20 L', cities: 'Mumbai, Delhi, Bengaluru…', active: healthInsurance.cityTier === 'metro' },
                                    { tier: 'Tier-1', cover: '₹15 L', cities: 'Ahmedabad, Jaipur, Kochi…', active: healthInsurance.cityTier === 'tier1' },
                                    { tier: 'Tier-2+', cover: '₹10 L', cities: 'All other cities', active: healthInsurance.cityTier === 'tier2' || healthInsurance.cityTier === 'tier3' },
                                ].map((t, i) => (
                                    <div key={i} className={`rounded-lg p-3 text-center border ${t.active
                                        ? 'border-teal-500/30 bg-teal-500/10'
                                        : 'border-white/5 bg-white/[0.02]'
                                        }`}>
                                        <p className={`text-[10px] font-bold uppercase tracking-wider mb-1 ${t.active ? 'text-teal-400' : 'text-slate-500'}`}>{t.tier}</p>
                                        <p className={`text-sm font-black ${t.active ? 'text-white' : 'text-slate-600'}`}>{t.cover}</p>
                                        <p className="text-[9px] text-slate-600 mt-0.5">{t.cities}</p>
                                    </div>
                                ))}
                            </div>

                            <MetricRow
                                label="Recommended for You"
                                sublabel={`${cityTierLabel[healthInsurance.cityTier]} city benchmark`}
                                value={healthInsurance.cityBenchmarkFormatted}
                                icon="🎯"
                            />
                            <MetricRow
                                label="Your Effective Cover"
                                sublabel={`Personal: ${new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(healthInsurance.personalCover)} + Corporate: ${new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(healthInsurance.corporateCover)}`}
                                value={healthInsurance.effectiveCoverFormatted}
                                icon="🏥"
                            />
                            {!healthInsurance.isAdequate ? (
                                <div className="flex items-center justify-between py-2.5 mt-1 bg-amber-500/10 rounded-lg px-3 border border-amber-500/15">
                                    <div className="flex items-center gap-2">
                                        <AlertTriangle className="w-4 h-4 text-amber-400" />
                                        <span className="text-xs font-bold text-amber-400">COVER GAP</span>
                                    </div>
                                    <span className="text-sm font-black text-amber-400 tabular-nums">{healthInsurance.gapFormatted}</span>
                                </div>
                            ) : (
                                <div className="flex items-center gap-2 py-2.5 mt-1 bg-emerald-500/10 rounded-lg px-3 border border-emerald-500/15">
                                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                                    <span className="text-xs font-bold text-emerald-400">Health Cover Adequate</span>
                                </div>
                            )}
                        </div>

                        {/* Employer-Only Warning */}
                        {healthInsurance.isEmployerOnly && (
                            <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                                <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-xs font-bold text-amber-400 mb-1">Employer-Dependent Cover</p>
                                    <p className="text-xs text-slate-300 leading-relaxed">
                                        Your cover is employer-dependent — you need a personal policy. Corporate health insurance ends the day you change jobs, leaving your family exposed.
                                    </p>
                                </div>
                            </div>
                        )}

                        {/* Super Top-Up Recommendation */}
                        {healthInsurance.showSuperTopUpReco && (
                            <div className="flex items-start gap-3 bg-teal-500/10 border border-teal-500/20 rounded-xl p-4">
                                <ArrowUpRight className="w-5 h-5 text-teal-400 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-xs font-bold text-teal-400 mb-1">Super Top-Up Recommended</p>
                                    <p className="text-xs text-slate-300 leading-relaxed">
                                        With a deductible matching your base plan of {healthInsurance.baseCoverFormatted}, add a ₹50L super top-up to reach {healthInsurance.totalWithTopUp} total cover at minimal cost.
                                    </p>
                                </div>
                            </div>
                        )}

                        {/* Section 80D */}
                        <div className="bg-background-dark rounded-xl border border-white/5 p-4">
                            <h4 className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-2">Section 80D Tax Benefits</h4>
                            <div className="grid grid-cols-3 gap-3">
                                <div className="text-center">
                                    <p className="text-sm font-bold text-primary">₹25,000</p>
                                    <p className="text-[10px] text-slate-500">Self / Family</p>
                                </div>
                                <div className="text-center">
                                    <p className="text-sm font-bold text-primary">₹25,000</p>
                                    <p className="text-[10px] text-slate-500">Parents (&lt;60)</p>
                                </div>
                                <div className="text-center">
                                    <p className="text-sm font-bold text-primary">₹50,000</p>
                                    <p className="text-[10px] text-slate-500">Parents (60+)</p>
                                </div>
                            </div>
                        </div>

                        {/* Do / Don't */}
                        <div className="space-y-3">
                            <GuidancePanel items={healthDo} type="do" />
                            <GuidancePanel items={healthDont} type="dont" />
                        </div>
                    </div>
                </section>

                {/* ════════════════════════════════════════════════
                   SECTION 3 — ADDITIONAL COVERAGE
                   ════════════════════════════════════════════════ */}
                {additionalCoverage.length > 0 && (
                    <section>
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-8 h-8 rounded-lg bg-amber-500/15 flex items-center justify-center">
                                <Shield className="w-4 h-4 text-amber-400" />
                            </div>
                            <div>
                                <h3 className="text-lg font-black text-white">Additional Coverage</h3>
                                <p className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">Based on your profile</p>
                            </div>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                            {additionalCoverage.map((card, i) => (
                                <div key={card.id} style={{ animationDelay: `${i * 100}ms` }}>
                                    <AdditionalCard card={card} />
                                </div>
                            ))}
                        </div>
                    </section>
                )}

        </div>
    );
};

export default InsuranceTab;
