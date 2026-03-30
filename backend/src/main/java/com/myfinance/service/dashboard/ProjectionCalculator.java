package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ProjectionCalculator {

    public ProjectionResultDTO calculate(UserFinancialData d) {
        double savings = d.getMonthlySavings();
        double savingsRate = d.getSavingsRate();

        // Smart optimization cap: if savings rate > 35%, use 10% boost instead of 20%
        int optimizationPct = savingsRate > 35 ? 10 : 20;
        double optimizedSavings = Math.round(savings * (1 + optimizationPct / 100.0));

        double annualRate = 0.12;
        double monthlyRate = annualRate / 12;
        int years = 30;

        List<YearPointDTO> points = new ArrayList<>();
        double currentCorpus = d.getNetWorth() > 0 ? d.getNetWorth() : 0;
        double optimizedCorpus = currentCorpus;

        for (int y = 0; y <= years; y++) {
            points.add(YearPointDTO.builder()
                    .year(y)
                    .current(Math.round(currentCorpus))
                    .optimized(Math.round(optimizedCorpus))
                    .build());

            if (y < years) {
                // SIP FV for 12 months
                for (int m = 0; m < 12; m++) {
                    currentCorpus = currentCorpus * (1 + monthlyRate) + savings;
                    optimizedCorpus = optimizedCorpus * (1 + monthlyRate) + optimizedSavings;
                }
            }
        }

        double currentEnd = points.get(years).getCurrent();
        double optimizedEnd = points.get(years).getOptimized();
        double extraGain = optimizedEnd - currentEnd;

        // Milestones
        List<MilestoneDTO> milestones = new ArrayList<>();
        double[] targets = {1000000, 5000000, 10000000, 50000000, 100000000}; // 10L, 50L, 1Cr, 5Cr, 10Cr
        String[] labels = {"₹10 Lakh", "₹50 Lakh", "₹1 Crore", "₹5 Crore", "₹10 Crore"};

        for (int t = 0; t < targets.length; t++) {
            for (int y = 0; y <= years; y++) {
                YearPointDTO pt = points.get(y);
                if (pt.getCurrent() >= targets[t]) {
                    milestones.add(MilestoneDTO.builder()
                            .label(labels[t])
                            .year(y)
                            .path("current")
                            .build());
                    break;
                }
            }
            for (int y = 0; y <= years; y++) {
                YearPointDTO pt = points.get(y);
                if (pt.getOptimized() >= targets[t]) {
                    milestones.add(MilestoneDTO.builder()
                            .label(labels[t])
                            .year(y)
                            .path("optimized")
                            .build());
                    break;
                }
            }
        }

        return ProjectionResultDTO.builder()
                .currentPath(points)
                .optimizedPath(points) // same list, different fields
                .currentEndValue((double) currentEnd)
                .optimizedEndValue((double) optimizedEnd)
                .extraGain(extraGain)
                .extraGainFormatted(fmt(extraGain))
                .optimizationPct(optimizationPct)
                .milestones(milestones)
                .build();
    }
}
