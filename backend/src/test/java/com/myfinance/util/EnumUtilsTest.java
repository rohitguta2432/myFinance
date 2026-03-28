package com.myfinance.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfinance.model.enums.EmploymentType;
import com.myfinance.model.enums.MaritalStatus;
import com.myfinance.model.enums.RiskTolerance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnumUtilsTest {

    // ------------------------------------------------------------------ safeEnum
    @Nested
    @DisplayName("safeEnum")
    class SafeEnum {

        // ---- happy-path exact matches ----

        @Test
        @DisplayName("parses exact uppercase value")
        void parsesExactUppercase() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, "SALARIED")).isEqualTo(EmploymentType.SALARIED);
        }

        @Test
        @DisplayName("parses lowercase value by uppercasing")
        void parsesLowercase() {
            assertThat(EnumUtils.safeEnum(MaritalStatus.class, "married")).isEqualTo(MaritalStatus.MARRIED);
        }

        @Test
        @DisplayName("parses mixed-case value")
        void parsesMixedCase() {
            assertThat(EnumUtils.safeEnum(RiskTolerance.class, "Aggressive")).isEqualTo(RiskTolerance.AGGRESSIVE);
        }

        // ---- space / hyphen normalisation ----

        @Test
        @DisplayName("converts spaces to underscores before lookup")
        void spacesToUnderscores() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, "self employed"))
                    .isEqualTo(EmploymentType.SELF_EMPLOYED);
        }

        @Test
        @DisplayName("converts hyphens to underscores before lookup")
        void hyphensToUnderscores() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, "self-employed"))
                    .isEqualTo(EmploymentType.SELF_EMPLOYED);
        }

        @Test
        @DisplayName("handles mixed spaces, hyphens, and case")
        void mixedNormalisation() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, "Self Employed"))
                    .isEqualTo(EmploymentType.SELF_EMPLOYED);
        }

        // ---- null / blank inputs ----

        @Test
        @DisplayName("returns null for null value")
        void nullValue() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, null)).isNull();
        }

        @Test
        @DisplayName("returns null for empty string")
        void emptyString() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, "")).isNull();
        }

        @Test
        @DisplayName("returns null for blank (whitespace-only) string")
        void blankString() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, "   ")).isNull();
        }

        // ---- invalid / unrecognised values ----

        @Test
        @DisplayName("returns null for completely invalid value")
        void invalidValue() {
            assertThat(EnumUtils.safeEnum(EmploymentType.class, "FREELANCER")).isNull();
        }

        @Test
        @DisplayName("returns null for numeric string")
        void numericValue() {
            assertThat(EnumUtils.safeEnum(MaritalStatus.class, "123")).isNull();
        }

        @Test
        @DisplayName("returns null for special characters")
        void specialCharacters() {
            assertThat(EnumUtils.safeEnum(RiskTolerance.class, "@#$%")).isNull();
        }

        // ---- works across different enum types ----

        @Test
        @DisplayName("works with RiskTolerance enum")
        void riskToleranceEnum() {
            assertThat(EnumUtils.safeEnum(RiskTolerance.class, "conservative"))
                    .isEqualTo(RiskTolerance.CONSERVATIVE);
        }

        @Test
        @DisplayName("works with MaritalStatus enum")
        void maritalStatusEnum() {
            assertThat(EnumUtils.safeEnum(MaritalStatus.class, "DIVORCED")).isEqualTo(MaritalStatus.DIVORCED);
        }
    }

    // ------------------------------------------------------------------ enumName
    @Nested
    @DisplayName("enumName")
    class EnumName {

        @Test
        @DisplayName("returns name string for non-null enum")
        void nonNullEnum() {
            assertThat(EnumUtils.enumName(EmploymentType.SALARIED)).isEqualTo("SALARIED");
        }

        @Test
        @DisplayName("returns null for null enum")
        void nullEnum() {
            assertThat(EnumUtils.enumName(null)).isNull();
        }

        @Test
        @DisplayName("returns underscore-style name for multi-word enum")
        void multiWordEnum() {
            assertThat(EnumUtils.enumName(EmploymentType.SELF_EMPLOYED)).isEqualTo("SELF_EMPLOYED");
        }

        @Test
        @DisplayName("works with different enum types")
        void differentEnumTypes() {
            assertThat(EnumUtils.enumName(MaritalStatus.MARRIED)).isEqualTo("MARRIED");
            assertThat(EnumUtils.enumName(RiskTolerance.MODERATE)).isEqualTo("MODERATE");
        }
    }
}
