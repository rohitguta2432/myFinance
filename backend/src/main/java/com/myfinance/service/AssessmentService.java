package com.myfinance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.*;
import com.myfinance.model.*;
import com.myfinance.model.enums.*;
import com.myfinance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final ProfileRepository profileRepo;
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;
    private final GoalRepository goalRepo;
    private final InsuranceRepository insuranceRepo;
    private final TaxRepository taxRepo;
    private final ObjectMapper objectMapper;

    // ─── Profile (Step 1) ───────────────────────────────────────────────────────

    public ProfileDTO getProfile() {
        return profileRepo.findAll().stream()
                .findFirst()
                .map(this::toProfileDTO)
                .orElse(new ProfileDTO());
    }

    public ProfileDTO saveProfile(ProfileDTO dto) {
        Profile profile = profileRepo.findAll().stream()
                .findFirst()
                .orElse(new Profile());

        profile.setAge(dto.getAge());
        profile.setCity(dto.getCity());
        profile.setMaritalStatus(safeEnum(MaritalStatus.class, dto.getMaritalStatus()));
        profile.setDependents(dto.getDependents());
        profile.setChildDependents(dto.getChildDependents());
        profile.setEmploymentType(safeEnum(EmploymentType.class, dto.getEmploymentType()));
        profile.setResidencyStatus(safeEnum(ResidencyStatus.class, dto.getResidencyStatus()));
        profile.setRiskTolerance(safeEnum(RiskTolerance.class, dto.getRiskTolerance()));
        profile.setRiskScore(dto.getRiskScore());
        profile.setRiskAnswers(toJson(dto.getRiskAnswers()));

        return toProfileDTO(profileRepo.save(profile));
    }

    private ProfileDTO toProfileDTO(Profile p) {
        return ProfileDTO.builder()
                .id(p.getId())
                .age(p.getAge())
                .city(p.getCity())
                .maritalStatus(enumName(p.getMaritalStatus()))
                .dependents(p.getDependents())
                .childDependents(p.getChildDependents())
                .employmentType(enumName(p.getEmploymentType()))
                .residencyStatus(enumName(p.getResidencyStatus()))
                .riskTolerance(enumName(p.getRiskTolerance()))
                .riskScore(p.getRiskScore())
                .riskAnswers(fromJson(p.getRiskAnswers()))
                .build();
    }

    // ─── Financials (Step 2) ────────────────────────────────────────────────────

    public FinancialsResponse getFinancials() {
        return FinancialsResponse.builder()
                .incomes(incomeRepo.findAll().stream().map(this::toIncomeDTO).collect(Collectors.toList()))
                .expenses(expenseRepo.findAll().stream().map(this::toExpenseDTO).collect(Collectors.toList()))
                .build();
    }

    public IncomeDTO addIncome(IncomeDTO dto) {
        Income income = Income.builder()
                .sourceName(dto.getSourceName())
                .amount(dto.getAmount())
                .frequency(safeEnum(Frequency.class, dto.getFrequency()))
                .build();
        return toIncomeDTO(incomeRepo.save(income));
    }

    public ExpenseDTO addExpense(ExpenseDTO dto) {
        Expense expense = Expense.builder()
                .category(dto.getCategory())
                .amount(dto.getAmount())
                .frequency(safeEnum(Frequency.class, dto.getFrequency()))
                .isEssential(dto.getIsEssential())
                .build();
        return toExpenseDTO(expenseRepo.save(expense));
    }

    private IncomeDTO toIncomeDTO(Income i) {
        return IncomeDTO.builder()
                .id(i.getId())
                .sourceName(i.getSourceName())
                .amount(i.getAmount())
                .frequency(enumName(i.getFrequency()))
                .build();
    }

    private ExpenseDTO toExpenseDTO(Expense e) {
        return ExpenseDTO.builder()
                .id(e.getId())
                .category(e.getCategory())
                .amount(e.getAmount())
                .frequency(enumName(e.getFrequency()))
                .isEssential(e.getIsEssential())
                .build();
    }

    // ─── Balance Sheet (Step 3) ─────────────────────────────────────────────────

    public BalanceSheetResponse getBalanceSheet() {
        return BalanceSheetResponse.builder()
                .assets(assetRepo.findAll().stream().map(this::toAssetDTO).collect(Collectors.toList()))
                .liabilities(liabilityRepo.findAll().stream().map(this::toLiabilityDTO).collect(Collectors.toList()))
                .build();
    }

    public AssetDTO addAsset(AssetDTO dto) {
        Asset asset = Asset.builder()
                .assetType(dto.getAssetType())
                .name(dto.getName())
                .currentValue(dto.getCurrentValue())
                .allocationPercentage(dto.getAllocationPercentage())
                .build();
        return toAssetDTO(assetRepo.save(asset));
    }

    public LiabilityDTO addLiability(LiabilityDTO dto) {
        Liability liability = Liability.builder()
                .liabilityType(dto.getLiabilityType())
                .name(dto.getName())
                .outstandingAmount(dto.getOutstandingAmount())
                .monthlyEmi(dto.getMonthlyEmi())
                .interestRate(dto.getInterestRate())
                .build();
        return toLiabilityDTO(liabilityRepo.save(liability));
    }

    private AssetDTO toAssetDTO(Asset a) {
        return AssetDTO.builder()
                .id(a.getId())
                .assetType(a.getAssetType())
                .name(a.getName())
                .currentValue(a.getCurrentValue())
                .allocationPercentage(a.getAllocationPercentage())
                .build();
    }

    private LiabilityDTO toLiabilityDTO(Liability l) {
        return LiabilityDTO.builder()
                .id(l.getId())
                .liabilityType(l.getLiabilityType())
                .name(l.getName())
                .outstandingAmount(l.getOutstandingAmount())
                .monthlyEmi(l.getMonthlyEmi())
                .interestRate(l.getInterestRate())
                .build();
    }

    // ─── Goals (Step 4) ─────────────────────────────────────────────────────────

    public List<GoalDTO> getGoals() {
        return goalRepo.findAll().stream().map(this::toGoalDTO).collect(Collectors.toList());
    }

    public GoalDTO addGoal(GoalDTO dto) {
        Goal goal = Goal.builder()
                .goalType(dto.getGoalType())
                .name(dto.getName())
                .targetAmount(dto.getTargetAmount())
                .currentCost(dto.getCurrentCost())
                .timeHorizonYears(dto.getTimeHorizonYears())
                .inflationRate(dto.getInflationRate())
                .build();
        return toGoalDTO(goalRepo.save(goal));
    }

    private GoalDTO toGoalDTO(Goal g) {
        return GoalDTO.builder()
                .id(g.getId())
                .goalType(g.getGoalType())
                .name(g.getName())
                .targetAmount(g.getTargetAmount())
                .currentCost(g.getCurrentCost())
                .timeHorizonYears(g.getTimeHorizonYears())
                .inflationRate(g.getInflationRate())
                .build();
    }

    // ─── Insurance (Step 5) ─────────────────────────────────────────────────────

    public List<InsuranceDTO> getInsurance() {
        return insuranceRepo.findAll().stream().map(this::toInsuranceDTO).collect(Collectors.toList());
    }

    public InsuranceDTO saveInsurance(InsuranceDTO dto) {
        InsuranceType type = safeEnum(InsuranceType.class, dto.getInsuranceType());

        // Upsert: find existing by type or create new
        Insurance insurance = (type != null)
                ? insuranceRepo.findByInsuranceType(type).orElse(new Insurance())
                : new Insurance();

        insurance.setInsuranceType(type);
        insurance.setPolicyName(dto.getPolicyName());
        insurance.setCoverageAmount(dto.getCoverageAmount());
        insurance.setPremiumAmount(dto.getPremiumAmount());
        insurance.setRenewalDate(dto.getRenewalDate());

        return toInsuranceDTO(insuranceRepo.save(insurance));
    }

    private InsuranceDTO toInsuranceDTO(Insurance i) {
        return InsuranceDTO.builder()
                .id(i.getId())
                .insuranceType(enumName(i.getInsuranceType()))
                .policyName(i.getPolicyName())
                .coverageAmount(i.getCoverageAmount())
                .premiumAmount(i.getPremiumAmount())
                .renewalDate(i.getRenewalDate())
                .build();
    }

    // ─── Tax (Step 6) ───────────────────────────────────────────────────────────

    public TaxDTO getTax() {
        return taxRepo.findAll().stream()
                .findFirst()
                .map(this::toTaxDTO)
                .orElse(new TaxDTO());
    }

    public TaxDTO saveTax(TaxDTO dto) {
        Tax tax = taxRepo.findAll().stream()
                .findFirst()
                .orElse(new Tax());

        tax.setSelectedRegime(safeEnum(TaxRegime.class, dto.getSelectedRegime()));
        tax.setPpfElssAmount(dto.getPpfElssAmount());
        tax.setEpfVpfAmount(dto.getEpfVpfAmount());
        tax.setTuitionFeesAmount(dto.getTuitionFeesAmount());
        tax.setLicPremiumAmount(dto.getLicPremiumAmount());
        tax.setHomeLoanPrincipal(dto.getHomeLoanPrincipal());
        tax.setHealthInsurancePremium(dto.getHealthInsurancePremium());
        tax.setParentsHealthInsurance(dto.getParentsHealthInsurance());
        tax.setCalculatedTaxOld(dto.getCalculatedTaxOld());
        tax.setCalculatedTaxNew(dto.getCalculatedTaxNew());

        return toTaxDTO(taxRepo.save(tax));
    }

    private TaxDTO toTaxDTO(Tax t) {
        return TaxDTO.builder()
                .id(t.getId())
                .selectedRegime(enumName(t.getSelectedRegime()))
                .ppfElssAmount(t.getPpfElssAmount())
                .epfVpfAmount(t.getEpfVpfAmount())
                .tuitionFeesAmount(t.getTuitionFeesAmount())
                .licPremiumAmount(t.getLicPremiumAmount())
                .homeLoanPrincipal(t.getHomeLoanPrincipal())
                .healthInsurancePremium(t.getHealthInsurancePremium())
                .parentsHealthInsurance(t.getParentsHealthInsurance())
                .calculatedTaxOld(t.getCalculatedTaxOld())
                .calculatedTaxNew(t.getCalculatedTaxNew())
                .build();
    }

    // ─── Utilities ──────────────────────────────────────────────────────────────

    private <E extends Enum<E>> E safeEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Enum.valueOf(clazz, value.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown enum value '{}' for {}", value, clazz.getSimpleName());
            return null;
        }
    }

    private String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    private String toJson(Map<String, Integer> map) {
        if (map == null || map.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize riskAnswers", e);
            return null;
        }
    }

    private Map<String, Integer> fromJson(String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize riskAnswers", e);
            return null;
        }
    }
}
