# India Tax Domain — FY 2025-26

Domain knowledge for Indian personal finance calculations used in myFinance.

## Tax Regimes

### New Regime (Default) — Section 115BAC
| Slab | Rate |
|------|------|
| ₹0 – ₹4L | 0% |
| ₹4L – ₹8L | 5% |
| ₹8L – ₹12L | 10% |
| ₹12L – ₹16L | 15% |
| ₹16L – ₹20L | 20% |
| ₹20L – ₹24L | 25% |
| Above ₹24L | 30% |

- **Section 87A Rebate**: If taxable income ≤ ₹12L → tax = ₹0
- **Standard Deduction**: ₹75,000
- **Cess**: 4% on total tax

### Old Regime
| Slab | Rate |
|------|------|
| ₹0 – ₹2.5L | 0% |
| ₹2.5L – ₹5L | 5% |
| ₹5L – ₹10L | 20% |
| Above ₹10L | 30% |

- **Standard Deduction**: ₹50,000
- **Cess**: 4% on total tax

## Deductions (Old Regime Only)

| Section | Limit | Components |
|---------|-------|------------|
| 80C | ₹1,50,000 | EPF + PPF + Life Insurance + ELSS + Tuition |
| 80CCD(1B) | ₹50,000 | Additional NPS contribution |
| 80D | ₹25,000/₹50,000 | Health insurance premium (self / senior citizen) |
| HRA | Variable | Based on metro/non-metro, rent paid, salary |

## Currency Formatting Rules

```javascript
// Always use this pattern
const fmt = (v) => {
    if (Math.abs(v) >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;  // Crore
    if (Math.abs(v) >= 100000)   return `₹${(v / 100000).toFixed(2)} L`;     // Lakh
    return `₹${Math.round(v).toLocaleString('en-IN')}`;
};
```

## Frequency Annualization

| Input Frequency | Multiplier |
|----------------|------------|
| Monthly | × 12 |
| Quarterly | × 4 |
| Yearly | × 1 |

## TDS Estimation
- Salaried income: Assume 10% TDS on gross salary
- Compare total TDS vs recommended tax → status: `refund`, `due`, or `matched`
- Threshold: ±₹100 considered "matched"

## Rental Income Treatment
- 30% Standard Deduction on gross rental income (Section 24)
- Net rental = Gross × 0.70

## Employer NPS (Section 80CCD(2))
- Show section if: employer NPS exists OR income > ₹15L
- Hide section if: income ≤ ₹10L AND no employer NPS
- Potential saving = salary × 10% × marginal rate

## Key Constants Used in Code
```
80C_LIMIT = 150000
NPS_80CCD1B_LIMIT = 50000
OLD_STD_DEDUCTION = 50000
NEW_STD_DEDUCTION = 75000
SECTION_87A_LIMIT = 1200000
CESS_RATE = 0.04
TDS_RATE = 0.10
RENTAL_STD_DEDUCTION = 0.30
```
