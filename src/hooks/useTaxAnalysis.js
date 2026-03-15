import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';

/* ═══════════════════════════════════════════════════════════════
   useTaxAnalysis — Premium Dashboard Tax Planning hook
   Computes all 5 tax sections from store data.
   ═══════════════════════════════════════════════════════════════ */

const fmt = (v) => {
    if (Math.abs(v) >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (Math.abs(v) >= 100000) return `₹${(v / 100000).toFixed(2)} L`;
    return `₹${Math.round(v).toLocaleString('en-IN')}`;
};

const calculateAnnual = (item) => {
    if (item.frequency === 'Monthly') return item.amount * 12;
    if (item.frequency === 'Quarterly') return item.amount * 4;
    if (item.frequency === 'Yearly') return item.amount;
    return item.amount;
};

export const useTaxAnalysis = () => {
    const {
        incomes, expenses, assets, insurance,
        taxRegime, investments80C, age,
    } = useAssessmentStore();

    return useMemo(() => {
        // ─── Income breakdown ───────────────────────────────────
        const incomeBySource = incomes.reduce((acc, inc) => {
            const cat = inc.source || 'Other';
            acc[cat] = (acc[cat] || 0) + calculateAnnual(inc);
            return acc;
        }, {});
        const salaryIncome = incomeBySource['Salary'] || 0;
        const grossRentalIncome = incomeBySource['Rental Income'] || 0;
        const grossTotalIncome = Object.values(incomeBySource).reduce((s, v) => s + v, 0);

        // ─── Rental Income Treatment (§5.3) ─────────────────────
        const rentalStdDeduction = grossRentalIncome * 0.30;
        const netRentalIncome = grossRentalIncome - rentalStdDeduction;
        const hasRentalIncome = grossRentalIncome > 0;

        // ─── Gross Taxable Income (after rental std deduction) ──
        const grossTaxableIncome = grossTotalIncome - rentalStdDeduction;

        // ─── Deduction components (from Step 6 logic) ───────────
        const autoEpf = assets.filter(a =>
            (a.subCategory || a.category)?.includes('EPF')
        ).reduce((sum, a) => sum + (parseFloat(a.amount) || 0), 0);

        const autoPpf = assets.filter(a =>
            (a.subCategory || a.category)?.includes('PPF')
        ).reduce((sum, a) => sum + (parseFloat(a.amount) || 0), 0);

        const autoNps = assets.filter(a =>
            (a.subCategory || a.category)?.includes('NPS')
        ).reduce((sum, a) => sum + (parseFloat(a.amount) || 0), 0);

        const autoLifeIns = (insurance?.personalLife || [])
            .reduce((sum, p) => sum + (parseFloat(p.premium) || 0), 0);

        const total80C = autoEpf + autoPpf + autoLifeIns;
        const final80C = Math.min(total80C, 150000);
        const unused80C = Math.max(0, 150000 - final80C);

        const npsContributed = autoNps;
        const finalNps = Math.min(npsContributed, 50000);
        const unusedNps = Math.max(0, 50000 - finalNps);

        // ─── Employer NPS detection ─────────────────────────────
        const employerNpsAssets = assets.filter(a =>
            ((a.subCategory || a.category) || '').toLowerCase().includes('employer') &&
            ((a.subCategory || a.category) || '').toLowerCase().includes('nps')
        );
        const employerNpsAmount = employerNpsAssets.reduce((sum, a) =>
            sum + (parseFloat(a.amount) || 0), 0
        );
        const hasEmployerNps = employerNpsAmount > 0;

        // ─── OLD REGIME calculation (§5.1) ──────────────────────
        const oldStdDeduction = 50000;
        const oldTotalDeductions = oldStdDeduction + final80C + finalNps;
        const oldTaxableIncome = Math.max(0, grossTaxableIncome - oldTotalDeductions);

        let oldBaseTax = 0;
        if (oldTaxableIncome > 1000000) {
            oldBaseTax += (oldTaxableIncome - 1000000) * 0.30;
            oldBaseTax += 112500; // 2.5-5L=12500, 5-10L=100000
        } else if (oldTaxableIncome > 500000) {
            oldBaseTax += (oldTaxableIncome - 500000) * 0.20;
            oldBaseTax += 12500;
        } else if (oldTaxableIncome > 250000) {
            oldBaseTax += (oldTaxableIncome - 250000) * 0.05;
        }
        const oldCess = oldBaseTax * 0.04;
        const oldTotalTax = oldBaseTax + oldCess;
        const oldEffectiveRate = grossTotalIncome > 0
            ? (oldTotalTax / grossTotalIncome) * 100 : 0;

        // ─── NEW REGIME calculation (§5.1 — updated slabs) ─────
        const newStdDeduction = 75000;
        const newTaxableIncome = Math.max(0, grossTaxableIncome - newStdDeduction);

        let newBaseTax = 0;
        // Section 87A rebate: taxable ≤ ₹12L → tax = ₹0
        if (newTaxableIncome <= 1200000) {
            newBaseTax = 0;
        } else {
            if (newTaxableIncome > 2400000) {
                newBaseTax += (newTaxableIncome - 2400000) * 0.30;
                newBaseTax += 290000;
            } else if (newTaxableIncome > 2000000) {
                newBaseTax += (newTaxableIncome - 2000000) * 0.25;
                newBaseTax += 190000;
            } else if (newTaxableIncome > 1600000) {
                newBaseTax += (newTaxableIncome - 1600000) * 0.20;
                newBaseTax += 110000;
            } else if (newTaxableIncome > 1200000) {
                newBaseTax += (newTaxableIncome - 1200000) * 0.15;
                newBaseTax += 60000;
            }
            // Slabs below 12L contribute 60000:
            // 4-8L = 5% * 4L = 20K; 8-12L = 10% * 4L = 40K → 60K total
        }
        const newCess = newBaseTax * 0.04;
        const newTotalTax = newBaseTax + newCess;
        const newEffectiveRate = grossTotalIncome > 0
            ? (newTotalTax / grossTotalIncome) * 100 : 0;

        const recommendedRegime = oldTotalTax <= newTotalTax ? 'old' : 'new';
        const savings = Math.abs(oldTotalTax - newTotalTax);
        // Dashboard always bases display on the recommended regime
        const selectedRegime = recommendedRegime;

        // ─── TDS Reconciliation (§5.2) ──────────────────────────
        // Estimate TDS as 10% of monthly salary * 12
        const monthlyEntries = incomes.filter(i => i.source === 'Salary' && i.frequency === 'Monthly');
        const tdsEstimate = monthlyEntries.reduce((sum, i) => {
            const gross = i.amount;
            // TDS amount = Gross × 10% (already deducted from take-home)
            return sum + (gross * 0.10 * 12);
        }, 0);
        // Also add yearly salary TDS
        const yearlyEntries = incomes.filter(i => i.source === 'Salary' && i.frequency === 'Yearly');
        const tdsYearly = yearlyEntries.reduce((sum, i) => sum + (i.amount * 0.10), 0);
        const totalTDS = tdsEstimate + tdsYearly;

        const recommendedTax = recommendedRegime === 'old' ? oldTotalTax : newTotalTax;
        const tdsDiff = totalTDS - recommendedTax;
        let tdsStatus = 'matched'; // 'refund', 'due', 'matched'
        if (tdsDiff > 100) tdsStatus = 'refund';
        else if (tdsDiff < -100) tdsStatus = 'due';

        // ─── 80C Deductions (§5.4) — conditional on regime ──────
        const isOldRegime = recommendedRegime === 'old';
        const potentialNpsSaving = unusedNps > 0
            ? Math.round(unusedNps * (oldTaxableIncome > 1000000 ? 0.312 : oldTaxableIncome > 500000 ? 0.208 : 0.052))
            : 0;

        // ─── Employer NPS (§5.5) — conditional ──────────────────
        const showEmployerNps = hasEmployerNps || grossTotalIncome > 1500000;
        const hideEmployerNps = grossTotalIncome <= 1000000 && !hasEmployerNps;
        const employerNpsPotentialSaving = Math.round(
            (salaryIncome * 0.10) * (oldTaxableIncome > 1000000 ? 0.312 : 0.208)
        );

        return {
            // Income summary
            grossTotalIncome,
            grossTotalIncomeFormatted: fmt(grossTotalIncome),
            incomeBySource,

            // §5.1 Regime Comparison
            regimeComparison: {
                old: {
                    grossIncome: grossTaxableIncome,
                    stdDeduction: oldStdDeduction,
                    deductions80C: final80C,
                    deductionsNps: finalNps,
                    totalDeductions: oldTotalDeductions,
                    taxableIncome: oldTaxableIncome,
                    baseTax: oldBaseTax,
                    cess: oldCess,
                    totalTax: oldTotalTax,
                    effectiveRate: oldEffectiveRate,
                },
                new: {
                    grossIncome: grossTaxableIncome,
                    stdDeduction: newStdDeduction,
                    taxableIncome: newTaxableIncome,
                    baseTax: newBaseTax,
                    cess: newCess,
                    totalTax: newTotalTax,
                    effectiveRate: newEffectiveRate,
                    rebateApplied: newTaxableIncome <= 1200000,
                },
                recommended: recommendedRegime,
                selected: selectedRegime,
                savings,
                savingsFormatted: fmt(savings),
            },

            // §5.2 TDS
            tds: {
                totalTDS,
                totalTDSFormatted: fmt(totalTDS),
                recommendedTax,
                recommendedTaxFormatted: fmt(recommendedTax),
                diff: Math.abs(tdsDiff),
                diffFormatted: fmt(Math.abs(tdsDiff)),
                status: tdsStatus,
            },

            // §5.3 Rental
            rental: {
                hasRentalIncome,
                grossRentalIncome,
                grossFormatted: fmt(grossRentalIncome),
                stdDeduction: rentalStdDeduction,
                stdDeductionFormatted: fmt(rentalStdDeduction),
                netRentalIncome,
                netFormatted: fmt(netRentalIncome),
            },

            // §5.4 Section 80C
            deductions: {
                isOldRegime,
                items: [
                    {
                        label: 'Section 80C',
                        sublabel: 'EPF + PPF + Life Insurance + Manual',
                        amount: final80C,
                        max: 150000,
                        gap: unused80C,
                        status: unused80C > 0 ? 'partial' : 'full',
                    },
                    {
                        label: 'NPS 80CCD(1B)',
                        sublabel: 'Additional NPS contribution',
                        amount: finalNps,
                        max: 50000,
                        gap: unusedNps,
                        status: npsContributed === 0 ? 'unused' : unusedNps > 0 ? 'partial' : 'full',
                        potentialSaving: potentialNpsSaving,
                    },
                    {
                        label: 'Standard Deduction',
                        sublabel: 'Flat deduction for salaried employees',
                        amount: oldStdDeduction,
                        max: 50000,
                        gap: 0,
                        status: 'full',
                    },
                ],
                totalDeductions: oldTotalDeductions,
                newRegimeDeduction: newStdDeduction,
            },

            // §5.5 Employer NPS
            employerNps: {
                show: showEmployerNps && !hideEmployerNps,
                hasEmployerNps,
                amount: employerNpsAmount,
                amountFormatted: fmt(employerNpsAmount),
                potentialSaving: employerNpsPotentialSaving,
                potentialSavingFormatted: fmt(employerNpsPotentialSaving),
                incomeAbove15L: grossTotalIncome > 1500000,
            },

            fmt,
        };
    }, [incomes, expenses, assets, insurance, taxRegime, investments80C, age]);
};
