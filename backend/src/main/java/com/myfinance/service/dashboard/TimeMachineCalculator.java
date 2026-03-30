package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import org.springframework.stereotype.Component;

@Component
public class TimeMachineCalculator {

    public TimeMachineDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        int age = d.getAge();
        double monthlySavings = d.getMonthlySavings();
        double annualSavings = Math.max(0, monthlySavings * 12);
        double currentCorpus = d.getNetWorth() > 0 ? d.getNetWorth() : 0;

        // Ideal start age = 22 (post-graduation assumption)
        double idealStartAge = 22;
        double actualStartAge = age;
        int delayYears = (int) Math.max(0, actualStartAge - idealStartAge);

        // Missed wealth = what corpus would be if started at idealStartAge
        double annualRate = 0.12;
        double monthlyRate = annualRate / 12;
        int missedMonths = delayYears * 12;

        // FV of SIP for missed months
        double missedWealth = 0;
        if (missedMonths > 0 && monthlySavings > 0) {
            missedWealth =
                    monthlySavings * ((Math.pow(1 + monthlyRate, missedMonths) - 1) / monthlyRate) * (1 + monthlyRate);
        }

        // Daily cost of inaction = missed wealth / (delay years * 365)
        double dailyCost = delayYears > 0 ? missedWealth / (delayYears * 365.0) : 0;

        // Cost of delay = difference between what you'd have vs what you have
        double costOfDelay = Math.max(0, missedWealth - currentCorpus);

        return TimeMachineDTO.builder()
                .missedWealth(missedWealth)
                .missedWealthFormatted(fmt(missedWealth))
                .dailyCostOfInaction(dailyCost)
                .dailyCostFormatted(fmt(dailyCost))
                .delayYears(delayYears)
                .idealStartAge(idealStartAge)
                .actualStartAge(actualStartAge)
                .costOfDelay(costOfDelay)
                .costOfDelayFormatted(fmt(costOfDelay))
                .build();
    }
}
