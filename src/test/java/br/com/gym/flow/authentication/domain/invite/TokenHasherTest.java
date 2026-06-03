package br.com.gym.flow.authentication.domain.invite;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    // SHA-256 as lowercase hex is always 64 characters; tokens are looked up by this hash,
    // so determinism (same raw -> same hash) is a real contract, not an implementation detail.
    private static final int SHA256_HEX_LENGTH = 64;

    @Test
    void givenSameRawToken_whenHashing_thenIsDeterministic() {
        // When
        var first = TokenHasher.hash("the-raw-token");
        var second = TokenHasher.hash("the-raw-token");

        // Then
        assertThat(first).isEqualTo(second).hasSize(SHA256_HEX_LENGTH).matches("[0-9a-f]+");
    }

    @Test
    void givenDifferentRawTokens_whenHashing_thenProducesDifferentHashes() {
        // When / Then
        assertThat(TokenHasher.hash("token-a")).isNotEqualTo(TokenHasher.hash("token-b"));
    }

    @Test
    void whenGeneratingRawTokens_thenEachIsUrlSafeAndUnique() {
        // When
        var a = TokenHasher.generateRawToken();
        var b = TokenHasher.generateRawToken();

        // Then — URL-safe base64 (no '+', '/', '=' padding) and not repeated
        assertThat(a).isNotBlank().doesNotContain("+", "/", "=");
        assertThat(a).isNotEqualTo(b);
    }
}
