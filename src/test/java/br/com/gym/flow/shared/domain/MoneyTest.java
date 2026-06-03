package br.com.gym.flow.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void givenAmount_whenCreating_thenNormalizesToTwoDecimals() {
        // When
        var money = Money.brl("10.5");

        // Then
        assertThat(money.amount()).isEqualByComparingTo("10.50");
        assertThat(money.amount().scale()).isEqualTo(2);
    }

    @Test
    void givenNegativeAmount_whenCreating_thenThrows() {
        assertThatThrownBy(() -> Money.brl("-0.01")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenSameCurrency_whenAdding_thenSumsAmounts() {
        // Given
        var ten = Money.brl("10.00");
        var fiveFifty = Money.brl("5.50");

        // When / Then — 10.00 + 5.50
        assertThat(ten.add(fiveFifty).amount()).isEqualByComparingTo("15.50");
    }

    @Test
    void givenSameCurrency_whenSubtracting_thenReducesAmount() {
        // When / Then — 10.00 - 3.00
        assertThat(Money.brl("10.00").subtract(Money.brl("3.00")).amount()).isEqualByComparingTo("7.00");
    }

    @Test
    void givenSubtractionThatGoesNegative_whenSubtracting_thenThrows() {
        // Money is non-negative by invariant, so an overdraw is rejected
        assertThatThrownBy(() -> Money.brl("3.00").subtract(Money.brl("10.00")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNonNegativeFactor_whenMultiplying_thenScalesAmount() {
        // When / Then — 2.50 * 3
        assertThat(Money.brl("2.50").multiply(3).amount()).isEqualByComparingTo("7.50");
    }

    @Test
    void givenNegativeFactor_whenMultiplying_thenThrows() {
        assertThatThrownBy(() -> Money.brl("2.50").multiply(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDifferentCurrencies_whenAdding_thenThrowsCurrencyMismatch() {
        // Given
        var brl = Money.brl("10.00");
        var usd = Money.of(new BigDecimal("10.00"), USD);

        // When / Then
        assertThatThrownBy(() -> brl.add(usd))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("currency mismatch");
    }
}
