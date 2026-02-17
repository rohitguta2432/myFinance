package com.myfinance.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tax_planning")
public class TaxPlanning {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "epf_vpf_amount")
    private BigDecimal epfVpfAmount;

    @Column(name = "ppf_elss_amount")
    private BigDecimal ppfElssAmount;

    @Column(name = "tuition_fees_amount")
    private BigDecimal tuitionFeesAmount;

    @Column(name = "lic_premium_amount")
    private BigDecimal licPremiumAmount;

    @Column(name = "home_loan_principal")
    private BigDecimal homeLoanPrincipal;

    @Column(name = "health_insurance_premium")
    private BigDecimal healthInsurancePremium;

    @Column(name = "parents_health_insurance")
    private BigDecimal parentsHealthInsurance;

    @Column(name = "selected_regime")
    private String selectedRegime;

    @Column(name = "calculated_tax_old")
    private BigDecimal calculatedTaxOld;

    @Column(name = "calculated_tax_new")
    private BigDecimal calculatedTaxNew;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getEpfVpfAmount() {
        return epfVpfAmount;
    }

    public void setEpfVpfAmount(BigDecimal epfVpfAmount) {
        this.epfVpfAmount = epfVpfAmount;
    }

    public BigDecimal getPpfElssAmount() {
        return ppfElssAmount;
    }

    public void setPpfElssAmount(BigDecimal ppfElssAmount) {
        this.ppfElssAmount = ppfElssAmount;
    }

    public BigDecimal getTuitionFeesAmount() {
        return tuitionFeesAmount;
    }

    public void setTuitionFeesAmount(BigDecimal tuitionFeesAmount) {
        this.tuitionFeesAmount = tuitionFeesAmount;
    }

    public BigDecimal getLicPremiumAmount() {
        return licPremiumAmount;
    }

    public void setLicPremiumAmount(BigDecimal licPremiumAmount) {
        this.licPremiumAmount = licPremiumAmount;
    }

    public BigDecimal getHomeLoanPrincipal() {
        return homeLoanPrincipal;
    }

    public void setHomeLoanPrincipal(BigDecimal homeLoanPrincipal) {
        this.homeLoanPrincipal = homeLoanPrincipal;
    }

    public BigDecimal getHealthInsurancePremium() {
        return healthInsurancePremium;
    }

    public void setHealthInsurancePremium(BigDecimal healthInsurancePremium) {
        this.healthInsurancePremium = healthInsurancePremium;
    }

    public BigDecimal getParentsHealthInsurance() {
        return parentsHealthInsurance;
    }

    public void setParentsHealthInsurance(BigDecimal parentsHealthInsurance) {
        this.parentsHealthInsurance = parentsHealthInsurance;
    }

    public String getSelectedRegime() {
        return selectedRegime;
    }

    public void setSelectedRegime(String selectedRegime) {
        this.selectedRegime = selectedRegime;
    }

    public BigDecimal getCalculatedTaxOld() {
        return calculatedTaxOld;
    }

    public void setCalculatedTaxOld(BigDecimal calculatedTaxOld) {
        this.calculatedTaxOld = calculatedTaxOld;
    }

    public BigDecimal getCalculatedTaxNew() {
        return calculatedTaxNew;
    }

    public void setCalculatedTaxNew(BigDecimal calculatedTaxNew) {
        this.calculatedTaxNew = calculatedTaxNew;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
