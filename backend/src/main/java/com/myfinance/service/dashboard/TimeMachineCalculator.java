package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TimeMachineCalculator {

    // Assumptions
    private static final double ANNUAL_RETURN = 0.12;         // 12% p.a.
    private static final double MONTHLY_RETURN = ANNUAL_RETURN / 12.0;
    private static final double INCOME_GROWTH = 0.06;         // 6% p.a.
    private static final int LOOKBACK_YEARS = 5;
    private static final int DEFAULT_RETIREMENT_AGE = 60;
    private static final int MIN_HORIZON_YEARS = 1;
    private static final double OPTIMISE_BUMP_PCT = 0.10;

    public TimeMachineDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        int age = d.getAge();
        double monthlySurplus = Math.max(0, d.getMonthlySavings());
        double currentCorpus = Math.max(0, d.getNetWorth());

        double idealStartAge = 22;
        int delayYears = (int) Math.max(0, age - idealStartAge);
        int lookbackYears = Math.min(delayYears, LOOKBACK_YEARS);

        // Lookback missed wealth — growing annuity FV (contributions grew 6%/yr)
        double missedWealth = computeLookbackFv(monthlySurplus, lookbackYears);

        // Forward horizon
        int horizonYears = Math.max(MIN_HORIZON_YEARS, DEFAULT_RETIREMENT_AGE - age);
        double baseCorpus = sipFv(monthlySurplus, horizonYears * 12);
        double delayedCorpus = sipFv(monthlySurplus, Math.max(0, (horizonYears - 1) * 12));
        double waitingPenalty = Math.max(0, baseCorpus - delayedCorpus);
        double dailyCost = waitingPenalty / 365.0;
        double costOfDelay = Math.max(0, missedWealth - currentCorpus);

        // Optimise scenario (+10% SIP)
        double monthlyBump = monthlySurplus * OPTIMISE_BUMP_PCT;
        double optimisedCorpus = sipFv(monthlySurplus + monthlyBump, horizonYears * 12);
        double extraCorpus = Math.max(0, optimisedCorpus - baseCorpus);

        String heroSubtitle = String.format(
                "in future retirement wealth (based on your %d-year investing horizon)",
                horizonYears);

        String explanation = monthlySurplus > 0
                ? String.format(
                        "If you start investing your %s monthly surplus today, your %d-year corpus is %s. "
                                + "If you wait just 1 more year, that corpus drops to %s — a gap of %s. "
                                + "Divide %s by 365 = %s/day.",
                        fmt(monthlySurplus), horizonYears, fmt(baseCorpus),
                        fmt(delayedCorpus), fmt(waitingPenalty),
                        fmt(waitingPenalty), fmt(dailyCost))
                : null;

        List<TimeMachineCardDTO> cards = monthlySurplus > 0
                ? buildCards(monthlySurplus, missedWealth, lookbackYears, waitingPenalty, dailyCost,
                        monthlyBump, extraCorpus)
                : null;

        return TimeMachineDTO.builder()
                .missedWealth(missedWealth)
                .missedWealthFormatted(fmt(missedWealth))
                .dailyCostOfInaction(dailyCost)
                .dailyCostFormatted(fmt(dailyCost))
                .delayYears(delayYears)
                .idealStartAge(idealStartAge)
                .actualStartAge((double) age)
                .costOfDelay(costOfDelay)
                .costOfDelayFormatted(fmt(costOfDelay))
                .horizonYears(horizonYears)
                .monthlySurplus(monthlySurplus)
                .baseCorpus(baseCorpus)
                .delayedCorpus(delayedCorpus)
                .waitingPenalty(waitingPenalty)
                .heroSubtitle(heroSubtitle)
                .explanation(explanation)
                .cards(cards)
                .build();
    }

    // Plain SIP future value: FV = P * ((1+r)^n - 1) / r * (1+r)
    private double sipFv(double monthly, int months) {
        if (monthly <= 0 || months <= 0) return 0;
        double r = MONTHLY_RETURN;
        return monthly * ((Math.pow(1 + r, months) - 1) / r) * (1 + r);
    }

    // Growing annuity: contributions start at P_start = current / (1+g)^years and grow g/yr.
    // Uses monthly growth approximation.
    private double computeLookbackFv(double currentMonthly, int years) {
        if (currentMonthly <= 0 || years <= 0) return 0;
        double pStart = currentMonthly / Math.pow(1 + INCOME_GROWTH, years);
        double r = MONTHLY_RETURN;
        double g = Math.pow(1 + INCOME_GROWTH, 1.0 / 12.0) - 1;
        int n = years * 12;
        if (Math.abs(r - g) < 1e-9) {
            return pStart * n * Math.pow(1 + r, n - 1);
        }
        return pStart * (Math.pow(1 + r, n) - Math.pow(1 + g, n)) / (r - g);
    }

    private List<TimeMachineCardDTO> buildCards(
            double monthlySurplus,
            double missedWealth,
            int lookbackYears,
            double waitingPenalty,
            double dailyCost,
            double monthlyBump,
            double extraCorpus) {
        List<TimeMachineCardDTO> cards = new ArrayList<>();

        if (lookbackYears > 0 && missedWealth > 0) {
            cards.add(TimeMachineCardDTO.builder()
                    .tone("danger")
                    .badge(String.format("IF YOU STARTED %d YRS AGO", lookbackYears))
                    .amountFormatted(fmt(missedWealth))
                    .heading("What you'd have built by now")
                    .body(String.format(
                            "If you had started a SIP %d years ago, with income & expenses inflation-adjusted at "
                                    + "%.0f%%/year (so your surplus was smaller back then), your portfolio would be "
                                    + "worth **%s today**.",
                            lookbackYears, INCOME_GROWTH * 100, fmt(missedWealth)))
                    .build());
        }

        cards.add(TimeMachineCardDTO.builder()
                .tone("danger")
                .badge("DAILY COST OF WAITING")
                .amountFormatted(fmt(dailyCost))
                .amountSuffix("/day")
                .heading(String.format("%s penalty ÷ 365", fmt(waitingPenalty)))
                .body(String.format(
                        "Waiting 1 more year before starting costs **%s** in retirement wealth. "
                                + "That's **%s lost** for every single day you delay.",
                        fmt(waitingPenalty), fmt(dailyCost)))
                .formula("(FV_today - FV_delayed1yr) ÷ 365")
                .build());

        double optimisedMonthly = monthlySurplus + monthlyBump;
        cards.add(TimeMachineCardDTO.builder()
                .tone("positive")
                .badge(String.format("OPTIMISE +%.0f%%", OPTIMISE_BUMP_PCT * 100))
                .amountPrefix("+")
                .amountFormatted(fmt(extraCorpus))
                .heading(String.format("Extra corpus from saving %s more/month", fmt(monthlyBump)))
                .body(String.format(
                        "Bumping your monthly SIP from %s to %s — just **%s extra** per month — earns "
                                + "you an extra **%s** at retirement with no change to your lifestyle.",
                        fmt(monthlySurplus), fmt(optimisedMonthly), fmt(monthlyBump), fmt(extraCorpus)))
                .build());

        return cards;
    }
}
