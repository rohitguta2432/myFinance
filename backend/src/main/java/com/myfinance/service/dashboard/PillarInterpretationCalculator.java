package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Migrated from frontend useHookText.js (357 lines).
 * Generates personalized interpretation text for each of the 5 health pillars
 * based on score tiers and computed financial data.
 *
 * INTERPRETATION RULES per pillar (from product spec):
 * - Survival: liquid=0 → CRITICAL; days capped at 180; months=0 → "Less than 1 week"
 * - Protection: cross-check life vs health sub-scores; city health benchmark tiers
 * - Debt: DTI safe threshold = 30%; DSCR<1 → force CRITICAL; {Y} never negative
 * - Wealth: 12% CAGR; equity=0% → force CRITICAL; {Z} in lakhs/crores
 * - Retirement: iterative solver for actual retirement age; don't show CRITICAL if early
 */
@Component
public class PillarInterpretationCalculator {

    public Map<String, PillarInterpretationDTO> calculate(List<PillarDTO> sortedPillars, RawDataDTO raw) {
        if (sortedPillars == null || raw == null) return Collections.emptyMap();

        Map<String, PillarInterpretationDTO> result = new LinkedHashMap<>();
        result.put("survival", computeSurvival(sortedPillars, raw));
        result.put("protection", computeProtection(sortedPillars, raw));
        result.put("debt", computeDebt(sortedPillars, raw));
        result.put("wealth", computeWealth(sortedPillars, raw));
        result.put("retirement", computeRetirement(sortedPillars, raw));
        return result;
    }

    // ── SURVIVAL ──────────────────────────────────────────────────────

    private PillarInterpretationDTO computeSurvival(List<PillarDTO> pillars, RawDataDTO raw) {
        double score = getPillarScore(pillars, "survival");
        double liquid = safe(raw.getLiquidAssets());
        double monthlyExp = safe(raw.getMonthlyExpenses());
        double efMonths = safe(raw.getEmergencyFundMonths());

        if (liquid == 0 || score <= 10) {
            int daysRunway = monthlyExp > 0 ? (int) Math.floor(liquid / (monthlyExp / 30)) : 0;
            String displayDays = daysRunway > 180 ? "180+" : String.valueOf(daysRunway);
            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("Your emergency fund lasts only " + displayDays
                            + " days — one income disruption triggers financial collapse.")
                    .action(
                            "Build an emergency fund of at least 6 months of essential expenses in a high-yield savings account. Start with ₹5,000/month SIP into a liquid fund.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        if (score <= 17) {
            String monthsDisplay = efMonths < 0.05 ? "Less than 1 week" : String.format("%.1f months", efMonths);
            double gap = Math.max(0, (6 * monthlyExp) - liquid);
            String monthlyTarget = formatRupees(Math.round(gap / 12));
            return PillarInterpretationDTO.builder()
                    .tier("warn")
                    .status("WARNING")
                    .text("You have " + monthsDisplay
                            + " of runway — below the 6-month safety net for your income profile.")
                    .action("Increase monthly contributions to your emergency fund. Target: ₹" + monthlyTarget
                            + "/month to reach 6-month buffer within a year.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        return PillarInterpretationDTO.builder()
                .tier("ok")
                .status("OK")
                .text(
                        "Your survival buffer is healthy — unlock to see if your asset allocation within liquid funds is optimal.")
                .action(
                        "Review your liquid fund allocation. Consider splitting between savings account (1-2 months) and liquid mutual funds (3-4 months) for better returns.")
                .dscrOverride(false)
                .equityOverride(false)
                .build();
    }

    // ── PROTECTION ────────────────────────────────────────────────────

    private PillarInterpretationDTO computeProtection(List<PillarDTO> pillars, RawDataDTO raw) {
        double score = getPillarScore(pillars, "protection");
        double lifeScoreVal = safe(raw.getLifeScore());
        double healthScoreVal = safe(raw.getHealthScore());
        double required = safe(raw.getRequiredCover());
        double existingTerm = safe(raw.getExistingTermCover());
        double existingHealth = safe(raw.getExistingHealthCover());
        double cityHealthBenchmark = getCityHealthBenchmark(raw.getCity());

        if (lifeScoreVal < 4 && healthScoreVal < 4) {
            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("Your family has ₹" + formatCr(Math.max(0, required - existingTerm))
                            + " Cr in uninsured exposure — we found the lowest-cost way to close this gap.")
                    .action(
                            "Get a pure term insurance plan immediately. At your age, a ₹1 Cr term plan costs approximately ₹700-1,200/month. Compare online-only plans for lowest premiums.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        if (lifeScoreVal >= 8 && healthScoreVal < 4) {
            return PillarInterpretationDTO.builder()
                    .tier("warn")
                    .status("WARNING")
                    .text("You are ₹" + formatLakh(Math.max(0, cityHealthBenchmark - existingHealth))
                            + " Lakh under-insured on health cover — one hospitalisation could cost you this.")
                    .action(
                            "Buy a top-up or super top-up health insurance plan to bridge the gap. These are significantly cheaper than standalone plans for the same coverage.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        if (score <= 8) {
            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("Your family has ₹" + formatCr(Math.max(0, required - existingTerm))
                            + " Cr in uninsured exposure — we found the lowest-cost way to close this gap.")
                    .action(
                            "Get a pure term insurance plan immediately. At your age, a ₹1 Cr term plan costs approximately ₹700-1,200/month. Compare online-only plans for lowest premiums.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        if (score <= 14) {
            return PillarInterpretationDTO.builder()
                    .tier("warn")
                    .status("WARNING")
                    .text("You are ₹" + formatLakh(Math.max(0, cityHealthBenchmark - existingHealth))
                            + " Lakh under-insured on health cover — one hospitalisation could cost you this.")
                    .action(
                            "Consider a super top-up health plan with ₹5-10L coverage. Also ensure your corporate health insurance covers your family and includes maternity if needed.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        return PillarInterpretationDTO.builder()
                .tier("ok")
                .status("OK")
                .text(
                        "Your insurance base is adequate — unlock to check if your riders cover the 3 most common claim scenarios.")
                .action(
                        "Review your policy riders: critical illness, accidental death, and waiver of premium are the top 3. Check if your existing plans include them.")
                .dscrOverride(false)
                .equityOverride(false)
                .build();
    }

    // ── DEBT ──────────────────────────────────────────────────────────

    private PillarInterpretationDTO computeDebt(List<PillarDTO> pillars, RawDataDTO raw) {
        double score = getPillarScore(pillars, "debt");
        double dscr = safe(raw.getDscr());
        double monthlyEMI = safe(raw.getMonthlyEMI());
        double dti = safe(raw.getDti());

        if (dscr < 1.0) {
            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("₹" + formatRupees(Math.round(monthlyEMI))
                            + "/month is trapped in EMIs — debt restructuring could free this up within 90 days.")
                    .action(
                            "Your current EMIs exceed your disposable income — immediate restructuring is required. Contact your lenders about loan tenure extension, balance transfer to lower-rate loans, or consolidation.")
                    .dscrOverride(true)
                    .equityOverride(false)
                    .build();
        }

        if (score <= 8) {
            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("₹" + formatRupees(Math.round(monthlyEMI))
                            + "/month is trapped in EMIs — debt restructuring could free this up within 90 days.")
                    .action(
                            "List all debts by interest rate. Pay minimum on all except the highest-rate debt — throw every extra rupee at that one. This avalanche method saves the most interest.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        if (score <= 14) {
            double dtiAbove = Math.max(0, dti - 30);
            if (dtiAbove <= 0) {
                return PillarInterpretationDTO.builder()
                        .tier("ok")
                        .status("OK")
                        .text(
                                "Your debt load is manageable — unlock to see if your home loan interest deduction is fully optimised.")
                        .action(
                                "Check if you are claiming full ₹2L deduction under Section 24(b) for home loan interest and ₹1.5L under Section 80C for principal repayment.")
                        .dscrOverride(false)
                        .equityOverride(false)
                        .build();
            }
            return PillarInterpretationDTO.builder()
                    .tier("warn")
                    .status("WARNING")
                    .text(
                            "Your DTI is " + String.format("%.0f", dti) + "% — " + String.format("%.0f", dtiAbove)
                                    + "% above the safe threshold — here is the payoff sequence that saves the most in interest.")
                    .action(
                            "Prioritise paying off high-interest loans (credit cards, personal loans) first. Consider balance transfer for home loans if rate differential exceeds 0.5%.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        return PillarInterpretationDTO.builder()
                .tier("ok")
                .status("OK")
                .text(
                        "Your debt load is manageable — unlock to see if your home loan interest deduction is fully optimised.")
                .action(
                        "Check if you are claiming full ₹2L deduction under Section 24(b) for home loan interest and ₹1.5L under Section 80C for principal repayment.")
                .dscrOverride(false)
                .equityOverride(false)
                .build();
    }

    // ── WEALTH ────────────────────────────────────────────────────────

    private PillarInterpretationDTO computeWealth(List<PillarDTO> pillars, RawDataDTO raw) {
        double score = getPillarScore(pillars, "wealth");
        double equityPct = safe(raw.getEquityPct());
        double targetEquityPct = safe(raw.getTargetEquityPct());
        double annualSavings = safe(raw.getAnnualSavings());
        double annualIncome = safe(raw.getAnnualIncome());
        int age = raw.getAge() != null ? raw.getAge() : 30;
        int retirementAge = raw.getRetirementAge() != null ? raw.getRetirementAge() : 60;

        if (equityPct == 0) {
            int yearsToRetirement = Math.max(1, retirementAge - age);
            double r = 0.12;
            double projected = annualSavings * ((Math.pow(1 + r, yearsToRetirement) - 1) / r);
            String projectedFormatted =
                    projected >= 10000000 ? "₹" + formatCr(projected) + " Cr" : "₹" + formatLakh(projected) + " Lakh";
            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("At your current savings rate you will accumulate " + projectedFormatted + " by retirement.")
                    .action(
                            "With zero equity, inflation is eroding the real value of your savings every year. Start a monthly SIP in a diversified equity fund (index fund or flexi-cap) with at least 20% of your monthly surplus.")
                    .dscrOverride(false)
                    .equityOverride(true)
                    .build();
        }

        if (score <= 8) {
            int yearsToRetirement = Math.max(1, retirementAge - age);
            double r = 0.12;
            double projected = annualSavings * ((Math.pow(1 + r, yearsToRetirement) - 1) / r);
            String projectedFormatted =
                    projected >= 10000000 ? "₹" + formatCr(projected) + " Cr" : "₹" + formatLakh(projected) + " Lakh";
            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("At your current savings rate you will accumulate " + projectedFormatted + " by retirement.")
                    .action(
                            "Increase your savings rate to at least 20% of gross income. Automate SIPs on salary day so investments happen before discretionary spending.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        if (score <= 14) {
            double r = 0.12;
            double annualEquityInvestment = equityPct > 0 ? (equityPct / 100) * annualIncome * 0.3 : 0;
            double tenYearGap = annualEquityInvestment * ((Math.pow(1 + r, 10) - 1) / r);
            String gapFormatted =
                    tenYearGap >= 5000000 ? "₹" + formatCr(tenYearGap) + " Cr" : "₹" + formatLakh(tenYearGap) + " Lakh";
            return PillarInterpretationDTO.builder()
                    .tier("warn")
                    .status("WARNING")
                    .text("Your equity exposure is " + String.format("%.0f", equityPct) + "% vs the "
                            + String.format("%.0f", targetEquityPct) + "% target for your age — this gap costs "
                            + gapFormatted + " in 10-year returns.")
                    .action(
                            "Gradually shift allocation towards equity via monthly SIPs. Increase equity SIP by 10% each year. Consider index funds (Nifty 50, Nifty Next 50) for core allocation.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        return PillarInterpretationDTO.builder()
                .tier("ok")
                .status("OK")
                .text("Your savings rate is strong — unlock to identify which funds in your portfolio are drag assets.")
                .action(
                        "Review your portfolio for funds that have consistently underperformed their benchmark over 3+ years. Consider consolidating into 3-5 core funds.")
                .dscrOverride(false)
                .equityOverride(false)
                .build();
    }

    // ── RETIREMENT ────────────────────────────────────────────────────

    private PillarInterpretationDTO computeRetirement(List<PillarDTO> pillars, RawDataDTO raw) {
        double score = getPillarScore(pillars, "retirement");
        int age = raw.getAge() != null ? raw.getAge() : 30;
        int retirementAge = raw.getRetirementAge() != null ? raw.getRetirementAge() : 60;
        double currentCorpus = safe(raw.getCurrentCorpus());
        double annualSavings = safe(raw.getAnnualSavings());
        double monthlyExpenses = safe(raw.getMonthlyExpenses());

        if (score <= 6) {
            double r = 0.09;
            double annualExpenses = monthlyExpenses * 12;
            double inflation = 0.07;
            double requiredCorpus = annualExpenses * Math.pow(1 + inflation, retirementAge - age) * 25;

            // Step 1: Iterative solver for actual retirement age X
            int X = retirementAge;
            for (int testAge = age + 1; testAge <= 80; testAge++) {
                int n = testAge - age;
                double projected = currentCorpus * Math.pow(1 + r, n) + annualSavings * ((Math.pow(1 + r, n) - 1) / r);
                if (projected >= requiredCorpus) {
                    X = testAge;
                    break;
                }
                X = testAge;
            }

            int Y = X - retirementAge;
            if (Y <= 0) {
                return PillarInterpretationDTO.builder()
                        .tier("ok")
                        .status("OK")
                        .text(
                                "You are on track for retirement — unlock to check if your NPS allocation is tax-optimised under the new regime.")
                        .action(
                                "Review your NPS Tier-1 allocation. Under the new tax regime, you can claim ₹50,000 additional deduction under Section 80CCD(1B).")
                        .dscrOverride(false)
                        .equityOverride(false)
                        .build();
            }

            int nR = retirementAge - age;
            double currentProjected =
                    currentCorpus * Math.pow(1 + r, nR) + annualSavings * ((Math.pow(1 + r, nR) - 1) / r);
            double S2 = annualSavings * 1.10;
            double newProjected = currentCorpus * Math.pow(1 + r, nR) + S2 * ((Math.pow(1 + r, nR) - 1) / r);
            double gap = Math.max(1, requiredCorpus - currentProjected);
            int Z = (int) Math.min(100, Math.round(((newProjected - currentProjected) / gap) * 100));
            long sipIncrease = Math.round((S2 - annualSavings) / 12);

            return PillarInterpretationDTO.builder()
                    .tier("critical")
                    .status("CRITICAL")
                    .text("At current pace you retire at age " + X + " — " + Y
                            + " years late. One change to your SIP fixes " + Z + "% of the gap.")
                    .action(
                            "Increase your monthly SIP by just 10% (₹" + formatRupees(sipIncrease)
                                    + "/month more). Also consider maximising your NPS and EPF voluntary contributions for tax-efficient retirement building.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        if (score <= 11) {
            int N = Math.max(1, retirementAge - age);
            double futureExpenses = (monthlyExpenses * 12) * Math.pow(1.07, N);
            double requiredCorpus = futureExpenses * 25;
            double r = 0.09;
            double fv1 = currentCorpus * Math.pow(1 + r, N);
            double gapAmount = Math.max(0, requiredCorpus - fv1);
            double additionalAnnual = gapAmount > 0 ? (gapAmount * r) / (Math.pow(1 + r, N) - 1) : 0;
            long additionalMonthly = Math.round(additionalAnnual / 12);

            return PillarInterpretationDTO.builder()
                    .tier("warn")
                    .status("WARNING")
                    .text("Your retirement corpus is ₹" + formatCr(gapAmount)
                            + " Cr short of target — monthly SIP needs to increase by ₹"
                            + formatRupees(additionalMonthly) + ".")
                    .action(
                            "Set up an additional SIP of ₹" + formatRupees(additionalMonthly)
                                    + "/month in a balanced advantage or flexi-cap fund. Auto-step-up by 10% annually for best results.")
                    .dscrOverride(false)
                    .equityOverride(false)
                    .build();
        }

        return PillarInterpretationDTO.builder()
                .tier("ok")
                .status("OK")
                .text(
                        "You are on track for retirement — unlock to check if your NPS allocation is tax-optimised under the new regime.")
                .action(
                        "Review your NPS Tier-1 allocation. Under the new tax regime, you can claim ₹50,000 additional deduction under Section 80CCD(1B).")
                .dscrOverride(false)
                .equityOverride(false)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private double getPillarScore(List<PillarDTO> pillars, String id) {
        return pillars.stream()
                .filter(p -> id.equals(p.getId()))
                .mapToDouble(p -> p.getScore() != null ? p.getScore() : 0)
                .findFirst()
                .orElse(0);
    }

    private double getCityHealthBenchmark(String cityName) {
        List<String> metros =
                List.of("mumbai", "delhi", "bangalore", "bengaluru", "chennai", "kolkata", "hyderabad", "pune");
        List<String> tier1 = List.of(
                "ahmedabad",
                "jaipur",
                "lucknow",
                "chandigarh",
                "kochi",
                "indore",
                "nagpur",
                "coimbatore",
                "visakhapatnam",
                "bhopal",
                "patna",
                "thiruvananthapuram",
                "gurgaon",
                "noida",
                "ghaziabad",
                "navi mumbai",
                "thane");
        String c = (cityName != null ? cityName : "").toLowerCase().trim();
        if (metros.stream().anyMatch(c::contains)) return 2000000;
        if (tier1.stream().anyMatch(c::contains)) return 1500000;
        return 1000000;
    }

    private String formatCr(double v) {
        return String.format("%.2f", v / 10000000);
    }

    private String formatLakh(double v) {
        return String.format("%.1f", v / 100000);
    }

    private String formatRupees(long v) {
        if (v < 0) return "-" + formatRupees(-v);
        return String.format("%,d", v);
    }

    private double safe(Double v) {
        return v != null ? v : 0;
    }
}
