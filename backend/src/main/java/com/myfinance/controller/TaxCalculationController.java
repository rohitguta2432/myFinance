package com.myfinance.controller;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.service.TaxCalculationService;
import com.myfinance.service.TaxCalculationService.DeductionInputs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-calculation")
@RequiredArgsConstructor
@Tag(name = "Tax Calculation", description = "Real-time tax computation")
public class TaxCalculationController {

    private final TaxCalculationService taxCalculationService;

    @GetMapping
    @Operation(summary = "Calculate tax for given granular deduction inputs")
    public TaxCalculationDTO calculate(
            @RequestAttribute("userId") Long userId,
            // 80C components
            @RequestParam(defaultValue = "0") double ppfNps,
            @RequestParam(defaultValue = "0") double homeLoanPrincipal,
            @RequestParam(defaultValue = "0") double tuitionFees,
            @RequestParam(defaultValue = "0") double nscFd,
            // 80D medical
            @RequestParam(defaultValue = "0") double medSelfSpouse,
            @RequestParam(defaultValue = "0") double medParentsLt60,
            @RequestParam(defaultValue = "0") double medParentsGt60,
            // Other deductions
            @RequestParam(defaultValue = "0") double additionalNps,
            @RequestParam(defaultValue = "0") double homeLoanInterest,
            @RequestParam(defaultValue = "0") double educationLoanInterest,
            @RequestParam(defaultValue = "0") double donations) {

        DeductionInputs in = DeductionInputs.builder()
                .ppfNps(ppfNps)
                .homeLoanPrincipal(homeLoanPrincipal)
                .tuitionFees(tuitionFees)
                .nscFd(nscFd)
                .medSelfSpouse(medSelfSpouse)
                .medParentsLt60(medParentsLt60)
                .medParentsGt60(medParentsGt60)
                .additionalNps(additionalNps)
                .homeLoanInterest(homeLoanInterest)
                .educationLoanInterest(educationLoanInterest)
                .donations(donations)
                .build();

        return taxCalculationService.calculate(userId, in);
    }
}
