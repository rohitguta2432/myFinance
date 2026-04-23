package com.myfinance.model;

import com.myfinance.model.enums.TaxRegime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    private TaxRegime selectedRegime;

    // ── 80C (combined cap ₹1.5L) ────────────────────────────
    private Double ppfElssAmount;
    private Double epfVpfAmount;
    private Double tuitionFeesAmount;
    private Double licPremiumAmount;
    private Double homeLoanPrincipal;
    private Double nscFdAmount;

    // ── 80D medical insurance ───────────────────────────────
    private Double healthInsurancePremium;          // self + spouse (₹25K cap)
    private Double parentsHealthInsurance;          // parents <60 (₹25K cap)
    private Double parentsHealthInsuranceSenior;    // parents ≥60 (₹50K cap)

    // ── Other deductions ────────────────────────────────────
    private Double additionalNpsAmount;             // 80CCD(1B) — ₹50K cap
    private Double homeLoanInterest;                // 24(b)     — ₹2L cap
    private Double educationLoanInterest;           // 80E       — no cap
    private Double donationsAmount;                 // 80G       — subject to limits

    // ── Server-computed totals (regenerated on save) ────────
    private Double calculatedTaxOld;
    private Double calculatedTaxNew;
}
