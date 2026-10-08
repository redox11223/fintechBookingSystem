package com.redox.fintechBookingSystem.identity.token;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SecureTokenGeneratorTests {
  private static final int SHA_256_HASH_LENGTH_BYTES = 32;

  private final SecureTokenGenerator tokenGenerator =
      new SecureTokenGenerator();

  @Test
  void generatesUrlSafeTokenWithTwoHundredFiftySixBitsOfEntropy() {
    GeneratedSecureToken generatedToken = tokenGenerator.generate();

    assertThat(generatedToken.rawToken())
        .hasSize(43)
        .matches("[A-Za-z0-9_-]{43}")
        .doesNotContain("=");
    assertThat(Base64.getUrlDecoder().decode(generatedToken.rawToken())).hasSize(32);
  }

  @Test
  void returnsHashOfGeneratedRawToken() {
    GeneratedSecureToken generatedToken = tokenGenerator.generate();

    assertThat(generatedToken.tokenHash())
        .hasSize(SHA_256_HASH_LENGTH_BYTES)
        .containsExactly(tokenGenerator.hash(generatedToken.rawToken()));
  }

  @Test
  void generatesDifferentTokensOnSuccessiveCalls() {
    GeneratedSecureToken firstToken = tokenGenerator.generate();
    GeneratedSecureToken secondToken = tokenGenerator.generate();

    assertThat(firstToken.rawToken()).isNotEqualTo(secondToken.rawToken());
    assertThat(firstToken.tokenHash()).isNotEqualTo(secondToken.tokenHash());
  }

  @Test
  void hashesUtf8TokenWithSha256() {
    byte[] expectedHash = HexFormat.of().parseHex(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");

    assertThat(tokenGenerator.hash("abc"))
        .hasSize(SHA_256_HASH_LENGTH_BYTES)
        .containsExactly(expectedHash);
  }

  @Test
  void rejectsNullTokenBeforeHashing() {
    assertThatNullPointerException()
        .isThrownBy(() -> tokenGenerator.hash(null))
        .withMessage("Token must not be null");
  }

  @Test
  void generatedTokenRejectsInvalidState() {
    byte[] validHash = new byte[SHA_256_HASH_LENGTH_BYTES];

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneratedSecureToken(null, validHash));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneratedSecureToken(" \t", validHash));
    assertThatNullPointerException()
        .isThrownBy(() -> new GeneratedSecureToken("valid-token", null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneratedSecureToken("valid-token", new byte[31]));
  }

  @Test
  void protectsHashWithDefensiveCopies() {
    byte[] originalHash = new byte[SHA_256_HASH_LENGTH_BYTES];
    originalHash[0] = 42;
    GeneratedSecureToken generatedToken =
        new GeneratedSecureToken("valid-token", originalHash);

    originalHash[0] = 1;
    assertThat(generatedToken.tokenHash()[0]).isEqualTo((byte) 42);

    byte[] returnedHash = generatedToken.tokenHash();
    returnedHash[0] = 2;
    assertThat(generatedToken.tokenHash()[0]).isEqualTo((byte) 42);
  }

  @Test
  void invalidStateErrorDoesNotExposeRawToken() {
    String rawToken = "do-not-expose-this-token";

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneratedSecureToken(rawToken, new byte[31]))
        .withMessageNotContaining(rawToken);
  }

  @Test
  void doesNotExposeRawTokenInStringRepresentation() {
    String rawToken = "do-not-expose-this-token";
    GeneratedSecureToken token =
            new GeneratedSecureToken(rawToken, new byte[32]);

    assertThat(token.toString())
            .doesNotContain(rawToken)
            .contains("[REDACTED]");
  }

}
