package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.myfinance.dto.*;
import com.myfinance.model.Expense;
import com.myfinance.model.Income;
import com.myfinance.model.enums.Frequency;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.IncomeRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CashFlowService")
class CashFlowServiceTest {

    @Mock
    private IncomeRepository incomeRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CashFlowService service;

    private static final Long USER_ID = 1L;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Income buildIncome(Long id, String source, Double amount, Frequency freq) {
        return Income.builder()
                .id(id)
                .userId(USER_ID)
                .sourceName(source)
                .amount(amount)
                .frequency(freq)
                .taxDeducted(false)
                .tdsPercentage(0.0)
                .build();
    }

    private Expense buildExpense(Long id, String category, Double amount, Frequency freq, Boolean essential) {
        return Expense.builder()
                .id(id)
                .userId(USER_ID)
                .category(category)
                .amount(amount)
                .frequency(freq)
                .isEssential(essential)
                .build();
    }

    // ─── getCashFlow ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCashFlow")
    class GetCashFlow {

        @Test
        @DisplayName("should return incomes and expenses mapped to DTOs")
        void returnsData() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense(1L, "Groceries", 5000.0, Frequency.MONTHLY, true)));

            FinancialsResponse result = service.getCashFlow(USER_ID);

            assertThat(result.getIncomes()).hasSize(1);
            assertThat(result.getIncomes().get(0).getSourceName()).isEqualTo("Salary");
            assertThat(result.getIncomes().get(0).getAmount()).isEqualTo(100000.0);
            assertThat(result.getExpenses()).hasSize(1);
            assertThat(result.getExpenses().get(0).getCategory()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("should return empty lists when no data")
        void emptyData() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            FinancialsResponse result = service.getCashFlow(USER_ID);

            assertThat(result.getIncomes()).isEmpty();
            assertThat(result.getExpenses()).isEmpty();
        }
    }

    // ─── getSummary ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("should compute monthly totals from monthly frequency")
        void monthlyTotals() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense(1L, "Rent", 30000.0, Frequency.MONTHLY, true)));

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalMonthlyIncome()).isCloseTo(100000.0, within(0.01));
            assertThat(result.getTotalMonthlyExpenses()).isCloseTo(30000.0, within(0.01));
            assertThat(result.getSurplus()).isCloseTo(70000.0, within(0.01));
        }

        @Test
        @DisplayName("should convert quarterly income to monthly (divide by 3)")
        void quarterlyIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Dividend", 30000.0, Frequency.QUARTERLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalMonthlyIncome()).isCloseTo(10000.0, within(0.01));
        }

        @Test
        @DisplayName("should convert yearly income to monthly (divide by 12)")
        void yearlyIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Bonus", 120000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalMonthlyIncome()).isCloseTo(10000.0, within(0.01));
        }

        @Test
        @DisplayName("should convert ONE_TIME to monthly (divide by 12)")
        void oneTimeIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Gift", 120000.0, Frequency.ONE_TIME)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalMonthlyIncome()).isCloseTo(10000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null amount as zero")
        void nullAmount() {
            Income income = Income.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .amount(null)
                    .frequency(Frequency.MONTHLY)
                    .build();
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(income));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalMonthlyIncome()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should treat null frequency as monthly (return amount as-is)")
        void nullFrequency() {
            Income income = Income.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .amount(50000.0)
                    .frequency(null)
                    .build();
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(income));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalMonthlyIncome()).isCloseTo(50000.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate savings rate as percentage of surplus over income")
        void savingsRate() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense(1L, "Rent", 70000.0, Frequency.MONTHLY, true)));

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            // surplus = 30000, rate = 30000/100000 * 100 = 30
            assertThat(result.getSavingsRate()).isEqualTo(30);
        }

        @Test
        @DisplayName("should return zero savings rate when no income")
        void zeroIncomeSavingsRate() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense(1L, "Rent", 30000.0, Frequency.MONTHLY, true)));

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getSavingsRate()).isEqualTo(0);
        }

        @Test
        @DisplayName("should calculate negative savings rate when expenses exceed income")
        void negativeSavingsRate() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Salary", 50000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense(1L, "Rent", 70000.0, Frequency.MONTHLY, true)));

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            // surplus = -20000, rate = -20000/50000 * 100 = -40
            assertThat(result.getSavingsRate()).isEqualTo(-40);
        }

        @Test
        @DisplayName("should filter EMI expenses correctly")
        void emiFiltering() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildExpense(1L, "EMIs (loan payments)", 20000.0, Frequency.MONTHLY, true),
                            buildExpense(2L, "Groceries", 10000.0, Frequency.MONTHLY, true)));

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalEMIs()).isCloseTo(20000.0, within(0.01));
        }

        @Test
        @DisplayName("should filter discretionary expenses (isEssential = false)")
        void discretionaryExpenses() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome(1L, "Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildExpense(1L, "Dining Out", 5000.0, Frequency.MONTHLY, false),
                            buildExpense(2L, "Shopping", 10000.0, Frequency.MONTHLY, false),
                            buildExpense(3L, "Rent", 30000.0, Frequency.MONTHLY, true)));

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getDiscretionaryTotal()).isCloseTo(15000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle empty lists returning zero totals")
        void emptyLists() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            CashFlowSummaryDTO result = service.getSummary(USER_ID);

            assertThat(result.getTotalMonthlyIncome()).isCloseTo(0.0, within(0.01));
            assertThat(result.getTotalMonthlyExpenses()).isCloseTo(0.0, within(0.01));
            assertThat(result.getSurplus()).isCloseTo(0.0, within(0.01));
            assertThat(result.getTotalEMIs()).isCloseTo(0.0, within(0.01));
            assertThat(result.getDiscretionaryTotal()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── addIncome ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addIncome")
    class AddIncome {

        @Test
        @DisplayName("should save income and return DTO")
        void addsIncome() {
            IncomeDTO dto = IncomeDTO.builder()
                    .sourceName("Salary")
                    .amount(100000.0)
                    .frequency("MONTHLY")
                    .taxDeducted(true)
                    .tdsPercentage(10.0)
                    .build();

            Income saved = Income.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .sourceName("Salary")
                    .amount(100000.0)
                    .frequency(Frequency.MONTHLY)
                    .taxDeducted(true)
                    .tdsPercentage(10.0)
                    .build();

            when(incomeRepo.save(any(Income.class))).thenReturn(saved);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("ADD_INCOME"), eq("income"), eq(1L), any());

            IncomeDTO result = service.addIncome(USER_ID, dto);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSourceName()).isEqualTo("Salary");
            assertThat(result.getAmount()).isEqualTo(100000.0);
            verify(incomeRepo).save(any(Income.class));
            verify(auditLogService).log(eq(USER_ID), eq("ADD_INCOME"), eq("income"), eq(1L), any());
        }
    }

    // ─── addExpense ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addExpense")
    class AddExpense {

        @Test
        @DisplayName("should save expense and return DTO")
        void addsExpense() {
            ExpenseDTO dto = ExpenseDTO.builder()
                    .category("Groceries")
                    .amount(5000.0)
                    .frequency("MONTHLY")
                    .isEssential(true)
                    .build();

            Expense saved = Expense.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .category("Groceries")
                    .amount(5000.0)
                    .frequency(Frequency.MONTHLY)
                    .isEssential(true)
                    .build();

            when(expenseRepo.save(any(Expense.class))).thenReturn(saved);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("ADD_EXPENSE"), eq("expense"), eq(1L), any());

            ExpenseDTO result = service.addExpense(USER_ID, dto);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCategory()).isEqualTo("Groceries");
            verify(expenseRepo).save(any(Expense.class));
        }
    }

    // ─── updateIncome ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateIncome")
    class UpdateIncome {

        @Test
        @DisplayName("should update existing income belonging to user")
        void updatesIncome() {
            Income existing = Income.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .sourceName("Salary")
                    .amount(80000.0)
                    .frequency(Frequency.MONTHLY)
                    .build();

            when(incomeRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(incomeRepo.save(any(Income.class))).thenReturn(existing);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("UPDATE_INCOME"), eq("income"), eq(1L), any());

            IncomeDTO dto = IncomeDTO.builder()
                    .sourceName("Salary Updated")
                    .amount(100000.0)
                    .frequency("MONTHLY")
                    .build();
            IncomeDTO result = service.updateIncome(USER_ID, 1L, dto);

            assertThat(result).isNotNull();
            verify(incomeRepo).save(any(Income.class));
        }

        @Test
        @DisplayName("should throw when income not found")
        void throwsWhenNotFound() {
            when(incomeRepo.findById(99L)).thenReturn(Optional.empty());

            IncomeDTO dto = IncomeDTO.builder()
                    .sourceName("Test")
                    .amount(100.0)
                    .frequency("MONTHLY")
                    .build();

            assertThatThrownBy(() -> service.updateIncome(USER_ID, 99L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Income not found or unauthorized");
        }

        @Test
        @DisplayName("should throw when income belongs to different user")
        void throwsWhenWrongUser() {
            Income otherUser = Income.builder()
                    .id(1L)
                    .userId(999L)
                    .sourceName("Other")
                    .amount(1000.0)
                    .build();
            when(incomeRepo.findById(1L)).thenReturn(Optional.of(otherUser));

            IncomeDTO dto = IncomeDTO.builder()
                    .sourceName("Test")
                    .amount(100.0)
                    .frequency("MONTHLY")
                    .build();

            assertThatThrownBy(() -> service.updateIncome(USER_ID, 1L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Income not found or unauthorized");
        }
    }

    // ─── updateExpense ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateExpense")
    class UpdateExpense {

        @Test
        @DisplayName("should update existing expense belonging to user")
        void updatesExpense() {
            Expense existing = Expense.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .category("Groceries")
                    .amount(5000.0)
                    .frequency(Frequency.MONTHLY)
                    .isEssential(true)
                    .build();

            when(expenseRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(expenseRepo.save(any(Expense.class))).thenReturn(existing);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("UPDATE_EXPENSE"), eq("expense"), eq(1L), any());

            ExpenseDTO dto = ExpenseDTO.builder()
                    .category("Food")
                    .amount(6000.0)
                    .frequency("MONTHLY")
                    .isEssential(true)
                    .build();
            ExpenseDTO result = service.updateExpense(USER_ID, 1L, dto);

            assertThat(result).isNotNull();
            verify(expenseRepo).save(any(Expense.class));
        }

        @Test
        @DisplayName("should throw when expense not found")
        void throwsWhenNotFound() {
            when(expenseRepo.findById(99L)).thenReturn(Optional.empty());

            ExpenseDTO dto = ExpenseDTO.builder()
                    .category("Test")
                    .amount(100.0)
                    .frequency("MONTHLY")
                    .build();

            assertThatThrownBy(() -> service.updateExpense(USER_ID, 99L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Expense not found or unauthorized");
        }

        @Test
        @DisplayName("should throw when expense belongs to different user")
        void throwsWhenWrongUser() {
            Expense otherUser = Expense.builder()
                    .id(1L)
                    .userId(999L)
                    .category("Other")
                    .amount(1000.0)
                    .build();
            when(expenseRepo.findById(1L)).thenReturn(Optional.of(otherUser));

            ExpenseDTO dto = ExpenseDTO.builder()
                    .category("Test")
                    .amount(100.0)
                    .frequency("MONTHLY")
                    .build();

            assertThatThrownBy(() -> service.updateExpense(USER_ID, 1L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Expense not found or unauthorized");
        }
    }

    // ─── deleteIncome ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteIncome")
    class DeleteIncome {

        @Test
        @DisplayName("should delete income belonging to user")
        void deletesIncome() {
            Income existing =
                    Income.builder().id(1L).userId(USER_ID).sourceName("Salary").build();
            when(incomeRepo.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(incomeRepo).delete(existing);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("DELETE_INCOME"), eq("income"), eq(1L), any());

            service.deleteIncome(USER_ID, 1L);

            verify(incomeRepo).delete(existing);
            verify(auditLogService).log(eq(USER_ID), eq("DELETE_INCOME"), eq("income"), eq(1L), any());
        }

        @Test
        @DisplayName("should throw when income not found for deletion")
        void throwsWhenNotFound() {
            when(incomeRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteIncome(USER_ID, 99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Income not found or unauthorized");
        }

        @Test
        @DisplayName("should throw when deleting income of different user")
        void throwsWhenWrongUser() {
            Income otherUser =
                    Income.builder().id(1L).userId(999L).sourceName("Other").build();
            when(incomeRepo.findById(1L)).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> service.deleteIncome(USER_ID, 1L)).isInstanceOf(RuntimeException.class);
        }
    }

    // ─── deleteExpense ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteExpense")
    class DeleteExpense {

        @Test
        @DisplayName("should delete expense belonging to user")
        void deletesExpense() {
            Expense existing =
                    Expense.builder().id(1L).userId(USER_ID).category("Rent").build();
            when(expenseRepo.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(expenseRepo).delete(existing);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("DELETE_EXPENSE"), eq("expense"), eq(1L), any());

            service.deleteExpense(USER_ID, 1L);

            verify(expenseRepo).delete(existing);
        }

        @Test
        @DisplayName("should throw when expense not found for deletion")
        void throwsWhenNotFound() {
            when(expenseRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteExpense(USER_ID, 99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Expense not found or unauthorized");
        }
    }
}
