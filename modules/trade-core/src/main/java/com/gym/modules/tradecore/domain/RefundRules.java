package com.gym.modules.tradecore.domain;
import java.math.BigInteger;
import java.util.Objects;
/** Exact normalized-unit arithmetic; no implicit calendar or compensation policy. */
public final class RefundRules {
 private RefundRules() {}
 public static BigInteger amount(BigInteger paid, BigInteger total, BigInteger remaining, BigInteger refunded) {
  Objects.requireNonNull(paid);Objects.requireNonNull(total);Objects.requireNonNull(remaining);Objects.requireNonNull(refunded);
  if(paid.signum()<0||total.signum()<=0||remaining.signum()<0||remaining.compareTo(total)>0||refunded.signum()<0||refunded.compareTo(paid)>0)
   throw new IllegalArgumentException("INVALID_NORMALIZED_REFUND_INPUT");
  BigInteger[] qr=paid.multiply(remaining).divideAndRemainder(total);
  return qr[0].add(qr[1].shiftLeft(1).compareTo(total)>=0?BigInteger.ONE:BigInteger.ZERO).min(paid.subtract(refunded));
 }
}
