package com.myfinance.service;

import com.myfinance.dto.InsuranceDTO;
import com.myfinance.model.Insurance;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.repository.InsuranceRepository;
import com.myfinance.util.EnumUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceService {

    private final InsuranceRepository insuranceRepo;

    @Transactional(readOnly = true)
    public List<InsuranceDTO> getInsurance() {
        log.info("insurance.get started");
        var list = insuranceRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        log.info("insurance.get.success count={}", list.size());
        return list;
    }

    @Transactional
    public InsuranceDTO saveInsurance(InsuranceDTO dto) {
        log.info("insurance.save type={} policy={} coverage={}",
                dto.getInsuranceType(), dto.getPolicyName(), dto.getCoverageAmount());

        InsuranceType type = EnumUtils.safeEnum(InsuranceType.class, dto.getInsuranceType());

        // Upsert: find existing by type or create new
        Insurance insurance = (type != null)
                ? insuranceRepo.findByInsuranceType(type).orElse(new Insurance())
                : new Insurance();

        insurance.setInsuranceType(type);
        insurance.setPolicyName(dto.getPolicyName());
        insurance.setCoverageAmount(dto.getCoverageAmount());
        insurance.setPremiumAmount(dto.getPremiumAmount());
        insurance.setRenewalDate(dto.getRenewalDate());

        InsuranceDTO saved = toDTO(insuranceRepo.save(insurance));
        log.info("insurance.save.success id={}", saved.getId());
        return saved;
    }

    private InsuranceDTO toDTO(Insurance i) {
        return InsuranceDTO.builder()
                .id(i.getId())
                .insuranceType(EnumUtils.enumName(i.getInsuranceType()))
                .policyName(i.getPolicyName())
                .coverageAmount(i.getCoverageAmount())
                .premiumAmount(i.getPremiumAmount())
                .renewalDate(i.getRenewalDate())
                .build();
    }
}
