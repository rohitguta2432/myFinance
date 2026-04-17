package com.myfinance.service;

import com.myfinance.dto.FeatureFlagDTO;
import com.myfinance.model.FeatureFlag;
import com.myfinance.model.FeatureFlagAudit;
import com.myfinance.model.User;
import com.myfinance.repository.FeatureFlagAuditRepository;
import com.myfinance.repository.FeatureFlagRepository;
import com.myfinance.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepo;
    private final FeatureFlagAuditRepository auditRepo;
    private final UserRepository userRepo;

    private static final List<FeatureFlag> SEED = List.of(
            FeatureFlag.builder().key("premium_advanced_investment").label("Advanced Investment Analysis")
                    .description("Scenario modeling + asset allocation deep-dive").category("premium").enabled(true).build(),
            FeatureFlag.builder().key("premium_tax_optimizer").label("Tax Optimization Reports")
                    .description("Section 80C/D planner + regime comparison").category("premium").enabled(false).build(),
            FeatureFlag.builder().key("premium_retirement_pro").label("Retirement Planning Pro")
                    .description("Monte Carlo simulation + FIRE calculator").category("premium").enabled(true).build(),
            FeatureFlag.builder().key("premium_export_tools").label("Export Tools (CSV/PDF)")
                    .description("Download full reports as CSV or PDF").category("premium").enabled(false).build(),
            FeatureFlag.builder().key("premium_kira_pro").label("Priority Chat (Kira Pro)")
                    .description("Unlimited Kira messages + advanced model").category("premium").enabled(true).build(),
            FeatureFlag.builder().key("premium_locked_insights").label("Locked Premium Insights Card")
                    .description("Show the dashboard teaser for locked premium insights").category("premium").enabled(true).build()
    );

    @PostConstruct
    public void seedFlags() {
        for (FeatureFlag seed : SEED) {
            if (!flagRepo.existsById(seed.getKey())) {
                flagRepo.save(seed);
                log.info("feature-flag.seeded key={} enabled={}", seed.getKey(), seed.getEnabled());
            }
        }
    }

    public Map<String, Boolean> getPublicFlagMap() {
        return flagRepo.findAll().stream()
                .collect(Collectors.toMap(FeatureFlag::getKey, FeatureFlag::getEnabled));
    }

    public List<FeatureFlagDTO> getAllFlags() {
        return flagRepo.findAll().stream()
                .sorted(Comparator.comparing(FeatureFlag::getCategory, Comparator.nullsLast(String::compareTo))
                        .thenComparing(FeatureFlag::getKey))
                .map(FeatureFlagDTO::from)
                .collect(Collectors.toList());
    }

    public Optional<FeatureFlagDTO> updateFlag(String key, Boolean enabled, Long actorUserId) {
        Optional<FeatureFlag> found = flagRepo.findById(key);
        if (found.isEmpty()) return Optional.empty();

        FeatureFlag flag = found.get();
        Boolean oldValue = flag.getEnabled();

        String actor = userRepo.findById(actorUserId)
                .map(User::getEmail).orElse("userId=" + actorUserId);

        flag.setEnabled(enabled);
        flag.setUpdatedBy(actor);
        flagRepo.save(flag);

        auditRepo.save(FeatureFlagAudit.builder()
                .flagKey(key)
                .oldValue(oldValue)
                .newValue(enabled)
                .changedBy(actor)
                .build());

        log.info("feature-flag.changed key={} old={} new={} by={}", key, oldValue, enabled, actor);
        return Optional.of(FeatureFlagDTO.from(flag));
    }

    public List<FeatureFlagAudit> getAuditLog() {
        return auditRepo.findTop100ByOrderByChangedAtDesc();
    }
}
